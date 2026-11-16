package com.example.engine.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.engine.storage.PlayerIdentity
import com.example.model.FailureKind
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AuthRepository private constructor(
    private val context: Context,
    private val httpClient: OkHttpClient,
    apiKey: String,
    private val nativeClient: NativeAuthClient?,
    @Suppress("UNUSED_PARAMETER") marker: Unit
) {
    @JvmOverloads
    constructor(
        context: Context,
        auth: FirebaseAuth? = defaultFirebaseAuth(),
        httpClient: OkHttpClient = OkHttpClientProvider.client,
        apiKey: String = FirestoreLeaderboardApi.resolveDefaultApiKey(context)
    ) : this(
        context = context,
        httpClient = httpClient,
        apiKey = apiKey,
        nativeClient = auth?.let(::FirebaseNativeAuthClient),
        marker = Unit
    )

    internal constructor(
        context: Context,
        nativeClient: NativeAuthClient?,
        httpClient: OkHttpClient = OkHttpClientProvider.client,
        apiKey: String = FirestoreLeaderboardApi.resolveDefaultApiKey(context)
    ) : this(
        context = context,
        httpClient = httpClient,
        apiKey = apiKey,
        nativeClient = nativeClient,
        marker = Unit
    )

    internal interface NativeAuthClient {
        val hasCurrentUser: Boolean
        val currentUid: String?
        fun getIdToken(forceRefresh: Boolean): String?
        fun signInAnonymously(): String?
        fun signOut()
    }

    private class FirebaseNativeAuthClient(private val auth: FirebaseAuth) : NativeAuthClient {
        override val hasCurrentUser: Boolean
            get() = auth.currentUser != null

        override val currentUid: String?
            get() = runCatching { auth.currentUser?.uid }.getOrNull()

        override fun getIdToken(forceRefresh: Boolean): String? {
            val user = auth.currentUser ?: return null
            val tokenResult = Tasks.await(
                user.getIdToken(forceRefresh),
                GMS_TASK_TIMEOUT_SEC,
                TimeUnit.SECONDS
            )
            return tokenResult.token
        }

        override fun signInAnonymously(): String? {
            val result = Tasks.await(
                auth.signInAnonymously(),
                GMS_TASK_TIMEOUT_SEC,
                TimeUnit.SECONDS
            )
            return result.user?.uid
        }

        override fun signOut() {
            auth.signOut()
        }
    }

    data class AuthFailure(
        val kind: FailureKind,
        val message: String
    )

    private val apiKey: String = FirestoreLeaderboardApi.sanitizeApiKey(apiKey)
    private val androidCertSha1: String? = FirestoreLeaderboardApi.resolveSigningCertSha1(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE)

    @Volatile
    private var nativeAuthBypassed: Boolean = false

    @Volatile
    var lastAuthFailure: AuthFailure? = null
        private set

    private val activeNativeClient: NativeAuthClient?
        get() = if (nativeAuthBypassed) null else nativeClient

    private val _isAuthenticated = MutableStateFlow(
        nativeClient?.currentUid != null || !prefs.getString(KEY_REST_ID_TOKEN, null).isNullOrBlank()
    )
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        nativeClient?.currentUid?.let { uid ->
            prefs.edit().putString(KEY_CACHED_UID, uid).apply()
            _isAuthenticated.value = true
        }
    }

    val currentUserId: String?
        get() = activeNativeClient?.currentUid
            ?: prefs.getString(KEY_CACHED_UID, null)
            ?: PlayerIdentity.getOrCreateDeviceUuid(context)

    suspend fun ensureAuthenticated(): String? = withContext(Dispatchers.IO) {
        sharedAuthMutex.withLock {
            val existingUid = activeNativeClient?.currentUid
            if (existingUid != null) {
                val verifiedToken = getIdTokenLocked(forceRefresh = false)
                if (!verifiedToken.isNullOrBlank()) {
                    return@withLock activeNativeClient?.currentUid
                        ?: prefs.getString(KEY_CACHED_UID, null)
                        ?: existingUid
                }
                val stillNativeUid = activeNativeClient?.currentUid
                if (stillNativeUid != null) {
                    prefs.edit().putString(KEY_CACHED_UID, stillNativeUid).commit()
                    return@withLock stillNativeUid
                }
                val recoveredUid = prefs.getString(KEY_CACHED_UID, null)
                if (!recoveredUid.isNullOrBlank() && _isAuthenticated.value) {
                    return@withLock recoveredUid
                }
            }

            if (activeNativeClient != null) {
                val nativeUid = signInAnonymouslyNativeLocked()
                if (!nativeUid.isNullOrBlank()) {
                    return@withLock nativeUid
                }
            }

            if (apiKey.isNotBlank()) {
                val cachedUid = prefs.getString(KEY_CACHED_UID, null)
                val cachedToken = prefs.getString(KEY_REST_ID_TOKEN, null)
                val cachedRefresh = prefs.getString(KEY_REST_REFRESH_TOKEN, null)
                if (!cachedUid.isNullOrBlank() && (!cachedToken.isNullOrBlank() || !cachedRefresh.isNullOrBlank())) {
                    val token = getIdTokenLocked(forceRefresh = false)
                    if (!token.isNullOrBlank()) {
                        if (nativeClient != null) {
                            nativeAuthBypassed = true
                        }
                        _isAuthenticated.value = true
                        return@withLock prefs.getString(KEY_CACHED_UID, cachedUid)
                    }
                }

                val restUid = signInWithRestIdentityToolkit()
                if (!restUid.isNullOrBlank()) {
                    if (nativeClient != null) {
                        nativeAuthBypassed = true
                    }
                    _isAuthenticated.value = true
                    return@withLock restUid
                }
            } else if (activeNativeClient == null && lastAuthFailure == null) {
                lastAuthFailure = AuthFailure(
                    FailureKind.UNCONFIGURED,
                    "Leaderboard API key is not configured"
                )
            }

            prefs.getString(KEY_CACHED_UID, null)
                ?: PlayerIdentity.getOrCreateDeviceUuid(context)
        }
    }

    fun getCachedIdTokenIfValid(): String? {
        if (activeNativeClient?.hasCurrentUser == true) return null
        val cachedToken = prefs.getString(KEY_REST_ID_TOKEN, null)
        val expiryMs = prefs.getLong(KEY_REST_TOKEN_EXPIRY_MS, 0L)
        return if (!cachedToken.isNullOrBlank() && System.currentTimeMillis() < expiryMs) cachedToken else null
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        sharedAuthMutex.withLock {
            getIdTokenLocked(forceRefresh)
        }
    }

    @JvmOverloads
    suspend fun getAuthenticatedSession(forceRefresh: Boolean = false): Pair<String?, String?> = withContext(Dispatchers.IO) {
        sharedAuthMutex.withLock {
            val token = getIdTokenLocked(forceRefresh)
            currentUserId to token
        }
    }

    private fun signInAnonymouslyNativeLocked(): String? {
        val client = activeNativeClient ?: return null
        return try {
            val newUid = client.signInAnonymously()
            if (!newUid.isNullOrBlank()) {
                lastAuthFailure = null
                _isAuthenticated.value = true
                prefs.edit().putString(KEY_CACHED_UID, newUid).commit()
                Log.d(TAG, "Anonymous auth successful: uid=$newUid")
                newUid
            } else {
                lastAuthFailure = AuthFailure(
                    FailureKind.SECURITY_REJECTED,
                    "Native anonymous sign-in returned no user ID"
                )
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val failure = classifyNativeAuthException(e)
            lastAuthFailure = failure
            Log.w(TAG, "Native anonymous sign-in failed (${failure.kind}): ${failure.message}. Falling back to Identity Toolkit REST.")
            null
        }
    }

    private suspend fun getIdTokenLocked(forceRefresh: Boolean): String? {
        val client = activeNativeClient
        val existingUid = client?.currentUid
        if (client != null && existingUid != null) {
            try {
                val token = client.getIdToken(forceRefresh)
                if (!token.isNullOrBlank()) {
                    lastAuthFailure = null
                    _isAuthenticated.value = true
                    prefs.edit().putString(KEY_CACHED_UID, existingUid).commit()
                    return token
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val failure = classifyNativeAuthException(e)
                lastAuthFailure = failure
                Log.w(TAG, "Native getIdToken failed (${failure.kind}): ${failure.message}.")

                // Preserve active native user on transient network/timeout/rate-limit errors so we don't mint
                // a conflicting REST UID while offline.
                if (failure.kind == FailureKind.NETWORK_UNAVAILABLE ||
                    failure.kind == FailureKind.TIMEOUT ||
                    failure.kind == FailureKind.RATE_LIMITED
                ) {
                    return null
                }

                // Native user is stale/deleted/invalid or GMS failed non-transiently: clear native session
                // cleanly before attempting re-authentication so currentUserId and token UID stay aligned.
                runCatching { client.signOut() }
                if (prefs.getString(KEY_CACHED_UID, null) == existingUid) {
                    prefs.edit().remove(KEY_CACHED_UID).commit()
                }
                _isAuthenticated.value = false

                if (failure.kind != FailureKind.UNCONFIGURED) {
                    val reAuthUid = signInAnonymouslyNativeLocked()
                    if (!reAuthUid.isNullOrBlank()) {
                        try {
                            val freshToken = client.getIdToken(false)
                            if (!freshToken.isNullOrBlank()) {
                                lastAuthFailure = null
                                _isAuthenticated.value = true
                                return freshToken
                            }
                        } catch (re: CancellationException) {
                            throw re
                        } catch (re: Exception) {
                            lastAuthFailure = classifyNativeAuthException(re)
                        }
                    }
                }

                if (apiKey.isNotBlank()) {
                    nativeAuthBypassed = true
                } else {
                    return null
                }
            }
            if (!nativeAuthBypassed && client.currentUid != null) {
                // Do not mint a separate REST Identity Toolkit UID while native FirebaseAuth has an active user,
                // otherwise currentUserId and the Bearer token's uid will mismatch (403).
                return null
            }
        }

        if (apiKey.isNotBlank()) {
            val cachedToken = prefs.getString(KEY_REST_ID_TOKEN, null)
            val refreshToken = prefs.getString(KEY_REST_REFRESH_TOKEN, null)
            val expiryMs = prefs.getLong(KEY_REST_TOKEN_EXPIRY_MS, 0L)

            if (!forceRefresh && !cachedToken.isNullOrBlank() && System.currentTimeMillis() < expiryMs) {
                lastAuthFailure = null
                _isAuthenticated.value = true
                return cachedToken
            }

            if (!refreshToken.isNullOrBlank()) {
                val refreshed = refreshRestIdToken(refreshToken)
                if (!refreshed.isNullOrBlank()) {
                    return refreshed
                }
                val refreshFailureKind = lastAuthFailure?.kind
                if (refreshFailureKind == FailureKind.NETWORK_UNAVAILABLE ||
                    refreshFailureKind == FailureKind.TIMEOUT ||
                    refreshFailureKind == FailureKind.RATE_LIMITED ||
                    refreshFailureKind == FailureKind.SERVER_ERROR ||
                    refreshFailureKind == FailureKind.MALFORMED_PAYLOAD ||
                    refreshFailureKind == FailureKind.UNCONFIGURED
                ) {
                    return null
                }
            }

            val currentClient = activeNativeClient
            if (currentClient != null) {
                val nativeUid = signInAnonymouslyNativeLocked()
                if (!nativeUid.isNullOrBlank()) {
                    try {
                        val token = currentClient.getIdToken(false)
                        if (!token.isNullOrBlank()) {
                            lastAuthFailure = null
                            _isAuthenticated.value = true
                            return token
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastAuthFailure = classifyNativeAuthException(e)
                    }
                }
            }

            val newUid = signInWithRestIdentityToolkit()
            if (!newUid.isNullOrBlank()) {
                if (nativeClient != null) {
                    nativeAuthBypassed = true
                }
                return prefs.getString(KEY_REST_ID_TOKEN, null)
            }
        } else if (lastAuthFailure == null) {
            lastAuthFailure = AuthFailure(
                FailureKind.UNCONFIGURED,
                "Leaderboard API key is not configured"
            )
        }

        return null
    }

    private suspend fun signInWithRestIdentityToolkit(): String? {
        if (apiKey.isBlank()) return null
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
        val bodyJson = JSONObject().put("returnSecureToken", true).toString()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Goog-Api-Key", apiKey)
            .apply {
                if (!androidCertSha1.isNullOrBlank()) {
                    addHeader("X-Android-Package", context.packageName)
                    addHeader("X-Android-Cert", androidCertSha1)
                }
            }
            .post(bodyJson.toRequestBody(mediaType))
            .build()

        return try {
            httpClient.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string().orEmpty()
                    val parsed = parseRestAuthError(response.code, err)
                    lastAuthFailure = parsed
                    Log.w(TAG, "[AuthRepository] REST signUp HTTP ${response.code} (${parsed.kind}): ${parsed.message} | raw=$err")
                    return null
                }
                val respStr = response.body?.string().orEmpty()
                if (respStr.isBlank()) {
                    lastAuthFailure = AuthFailure(FailureKind.SERVER_ERROR, "Empty response from Firebase Auth REST")
                    return null
                }
                val json = JSONObject(respStr)
                val localId = json.optString("localId", "")
                val idToken = json.optString("idToken", "")
                val refreshToken = json.optString("refreshToken", "")
                val expiresInSec = json.optString("expiresIn", "3600").toLongOrNull() ?: 3600L

                if (localId.isNotBlank() && idToken.isNotBlank()) {
                    val expiryMs = System.currentTimeMillis() + (expiresInSec - 120L).coerceAtLeast(60L) * 1000L
                    prefs.edit()
                        .putString(KEY_CACHED_UID, localId)
                        .putString(KEY_REST_ID_TOKEN, idToken)
                        .putString(KEY_REST_REFRESH_TOKEN, refreshToken)
                        .putLong(KEY_REST_TOKEN_EXPIRY_MS, expiryMs)
                        .commit()
                    lastAuthFailure = null
                    _isAuthenticated.value = true
                    Log.i(TAG, "[AuthRepository] REST Identity Toolkit anonymous auth succeeded: uid=$localId")
                    localId
                } else {
                    lastAuthFailure = AuthFailure(FailureKind.MALFORMED_PAYLOAD, "Firebase Auth REST response missing idToken or localId")
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            lastAuthFailure = AuthFailure(FailureKind.TIMEOUT, "Authentication connection timed out")
            Log.w(TAG, "[AuthRepository] REST signUp timed out: ${e.message}")
            null
        } catch (e: UnknownHostException) {
            lastAuthFailure = AuthFailure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable during authentication")
            Log.w(TAG, "[AuthRepository] REST signUp DNS/network unavailable: ${e.message}")
            null
        } catch (e: IOException) {
            lastAuthFailure = AuthFailure(FailureKind.NETWORK_UNAVAILABLE, "Network I/O error during authentication")
            Log.w(TAG, "[AuthRepository] REST signUp I/O failed: ${e.message}")
            null
        } catch (e: Exception) {
            lastAuthFailure = AuthFailure(FailureKind.SERVER_ERROR, "Authentication failed: ${e.message}")
            Log.w(TAG, "[AuthRepository] REST signUp failed: ${e.message}")
            null
        }
    }

    private suspend fun refreshRestIdToken(refreshToken: String): String? {
        if (apiKey.isBlank() || refreshToken.isBlank()) return null
        val url = "https://securetoken.googleapis.com/v1/token?key=$apiKey"
        val bodyJson = JSONObject()
            .put("grant_type", "refresh_token")
            .put("refresh_token", refreshToken)
            .toString()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Goog-Api-Key", apiKey)
            .apply {
                if (!androidCertSha1.isNullOrBlank()) {
                    addHeader("X-Android-Package", context.packageName)
                    addHeader("X-Android-Cert", androidCertSha1)
                }
            }
            .post(bodyJson.toRequestBody(mediaType))
            .build()

        return try {
            httpClient.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string().orEmpty()
                    val parsed = parseRestAuthError(response.code, err)
                    lastAuthFailure = parsed
                    Log.w(TAG, "[AuthRepository] REST token refresh HTTP ${response.code} (${parsed.kind}): ${parsed.message} | raw=$err")
                    if (response.code in 400..403) {
                        prefs.edit()
                            .remove(KEY_REST_ID_TOKEN)
                            .remove(KEY_REST_REFRESH_TOKEN)
                            .remove(KEY_REST_TOKEN_EXPIRY_MS)
                            .commit()
                        _isAuthenticated.value = false
                    }
                    return null
                }
                val respStr = response.body?.string().orEmpty()
                if (respStr.isBlank()) {
                    lastAuthFailure = AuthFailure(FailureKind.SERVER_ERROR, "Empty response from Firebase Auth token refresh")
                    return null
                }
                val json = JSONObject(respStr)
                val newIdToken = json.optString("id_token").ifBlank { json.optString("idToken") }
                val newRefreshToken = json.optString("refresh_token").ifBlank { json.optString("refreshToken", refreshToken) }
                val userId = json.optString("user_id").ifBlank { json.optString("localId") }
                val expiresInSec = json.optString("expires_in", "3600").toLongOrNull() ?: 3600L

                if (newIdToken.isNotBlank()) {
                    val expiryMs = System.currentTimeMillis() + (expiresInSec - 120L).coerceAtLeast(60L) * 1000L
                    val editor = prefs.edit()
                        .putString(KEY_REST_ID_TOKEN, newIdToken)
                        .putString(KEY_REST_REFRESH_TOKEN, newRefreshToken)
                        .putLong(KEY_REST_TOKEN_EXPIRY_MS, expiryMs)
                    if (userId.isNotBlank()) {
                        editor.putString(KEY_CACHED_UID, userId)
                    }
                    editor.commit()
                    lastAuthFailure = null
                    _isAuthenticated.value = true
                    Log.d(TAG, "[AuthRepository] REST token refresh succeeded")
                    newIdToken
                } else {
                    lastAuthFailure = AuthFailure(FailureKind.MALFORMED_PAYLOAD, "Token refresh response missing id_token")
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            lastAuthFailure = AuthFailure(FailureKind.TIMEOUT, "Token refresh timed out")
            Log.w(TAG, "[AuthRepository] REST token refresh timed out: ${e.message}")
            null
        } catch (e: UnknownHostException) {
            lastAuthFailure = AuthFailure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable during token refresh")
            Log.w(TAG, "[AuthRepository] REST token refresh DNS/network unavailable: ${e.message}")
            null
        } catch (e: IOException) {
            lastAuthFailure = AuthFailure(FailureKind.NETWORK_UNAVAILABLE, "Network I/O error during token refresh")
            Log.w(TAG, "[AuthRepository] REST token refresh I/O failed: ${e.message}")
            null
        } catch (e: Exception) {
            lastAuthFailure = AuthFailure(FailureKind.SERVER_ERROR, "Token refresh failed: ${e.message}")
            Log.w(TAG, "[AuthRepository] REST token refresh failed: ${e.message}")
            null
        }
    }

    private fun classifyNativeAuthException(error: Throwable): AuthFailure {
        val cause = (error as? ExecutionException)?.cause ?: error
        return when (cause) {
            is TimeoutException, is SocketTimeoutException -> {
                AuthFailure(FailureKind.TIMEOUT, "Firebase Auth timed out")
            }
            is FirebaseNetworkException, is UnknownHostException, is IOException -> {
                AuthFailure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable during Firebase Auth")
            }
            is FirebaseAuthInvalidUserException -> {
                AuthFailure(FailureKind.SECURITY_REJECTED, "Cached anonymous user session expired (${cause.errorCode})")
            }
            is FirebaseAuthException -> {
                when (cause.errorCode) {
                    "ERROR_OPERATION_NOT_ALLOWED", "ERROR_ADMIN_ONLY_OPERATION" -> AuthFailure(
                        FailureKind.UNCONFIGURED,
                        "Anonymous Auth is disabled in Firebase Console (Authentication -> Sign-in method -> Anonymous)"
                    )
                    "ERROR_API_KEY_NOT_VALID", "ERROR_INVALID_API_KEY", "ERROR_APP_NOT_AUTHORIZED" -> AuthFailure(
                        FailureKind.UNCONFIGURED,
                        "Firebase API key or app authorization invalid (${cause.errorCode})"
                    )
                    "ERROR_TOO_MANY_REQUESTS" -> AuthFailure(
                        FailureKind.RATE_LIMITED,
                        "Firebase Auth rate limit reached (${cause.errorCode})"
                    )
                    else -> AuthFailure(
                        FailureKind.SECURITY_REJECTED,
                        "Firebase Auth error (${cause.errorCode}): ${cause.message ?: "unknown"}"
                    )
                }
            }
            else -> AuthFailure(
                FailureKind.SECURITY_REJECTED,
                cause.message?.takeIf { it.isNotBlank() } ?: "Anonymous authentication failed"
            )
        }
    }

    private fun parseRestAuthError(code: Int, body: String): AuthFailure {
        if (code == 429) {
            return AuthFailure(FailureKind.RATE_LIMITED, "Firebase Auth rate limit reached (429)")
        }
        if (code in 500..599) {
            return AuthFailure(FailureKind.SERVER_ERROR, "Firebase Auth server error ($code)")
        }
        if (body.isBlank()) {
            return AuthFailure(FailureKind.SECURITY_REJECTED, "Firebase Auth request rejected ($code)")
        }
        return try {
            val trimmed = body.trim()
            val json = if (trimmed.startsWith("[")) {
                JSONArray(trimmed).optJSONObject(0) ?: JSONObject()
            } else {
                JSONObject(trimmed)
            }
            val err = json.optJSONObject("error")
            val msg = err?.optString("message", "") ?: ""
            val details = err?.optJSONArray("details")
            var reason = ""
            if (details != null) {
                for (i in 0 until details.length()) {
                    val r = details.optJSONObject(i)?.optString("reason", "") ?: ""
                    if (r.isNotBlank()) reason = r
                }
            }
            when {
                msg.contains("ADMIN_ONLY_OPERATION", ignoreCase = true) ||
                    msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) -> {
                    AuthFailure(
                        FailureKind.UNCONFIGURED,
                        "Anonymous Auth is disabled in Firebase Console (Authentication -> Sign-in method -> Anonymous)"
                    )
                }
                msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) -> {
                    AuthFailure(
                        FailureKind.UNCONFIGURED,
                        "Firebase Authentication is not initialized in Firebase Console"
                    )
                }
                reason == "API_KEY_ANDROID_APP_BLOCKED" ||
                    msg.contains("Requests from this Android client application", ignoreCase = true) -> {
                    AuthFailure(
                        FailureKind.UNCONFIGURED,
                        "Firebase API Key Android app restriction blocked (${context.packageName}). Check SHA-1 in Google Cloud Console."
                    )
                }
                reason == "API_KEY_SERVICE_BLOCKED" ||
                    reason == "SERVICE_DISABLED" ||
                    msg.contains("identitytoolkit", ignoreCase = true) && msg.contains("blocked", ignoreCase = true) ||
                    msg.contains("is disabled", ignoreCase = true) -> {
                    AuthFailure(
                        FailureKind.UNCONFIGURED,
                        "Identity Toolkit API is disabled or blocked for this Firebase API Key"
                    )
                }
                reason == "API_KEY_INVALID" ||
                    msg.contains("API_KEY_INVALID", ignoreCase = true) ||
                    msg.contains("API key not valid", ignoreCase = true) -> {
                    AuthFailure(
                        FailureKind.UNCONFIGURED,
                        "Firebase API Key invalid or expired ($code). Verify google-services.json or LEADERBOARD_API_KEY."
                    )
                }
                msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER", ignoreCase = true) -> {
                    AuthFailure(FailureKind.RATE_LIMITED, "Firebase Auth rate limit reached (429)")
                }
                else -> AuthFailure(
                    FailureKind.SECURITY_REJECTED,
                    if (msg.isNotBlank()) "Firebase Auth rejected ($code): $msg" else "Firebase Auth rejected ($code)"
                )
            }
        } catch (_: Exception) {
            AuthFailure(FailureKind.SECURITY_REJECTED, "Firebase Auth rejected ($code)")
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val GMS_TASK_TIMEOUT_SEC = 15L
        private const val KEY_CACHED_UID = "cached_auth_uid"
        private const val KEY_REST_ID_TOKEN = "cached_rest_id_token"
        private const val KEY_REST_REFRESH_TOKEN = "cached_rest_refresh_token"
        private const val KEY_REST_TOKEN_EXPIRY_MS = "cached_rest_token_expiry_ms"
        private val sharedAuthMutex = Mutex()

        private fun defaultFirebaseAuth(): FirebaseAuth? =
            try {
                FirebaseAuth.getInstance()
            } catch (_: Exception) {
                null
            }
    }
}
