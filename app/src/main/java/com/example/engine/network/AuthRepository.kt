package com.example.engine.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.engine.storage.PlayerIdentity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
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
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AuthRepository @JvmOverloads constructor(
    private val context: Context,
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull(),
    private val httpClient: OkHttpClient = OkHttpClientProvider.client,
    apiKey: String = FirestoreLeaderboardApi.resolveDefaultApiKey(context)
) {
    private val apiKey: String = FirestoreLeaderboardApi.sanitizeApiKey(apiKey)
    private val androidCertSha1: String? = FirestoreLeaderboardApi.resolveSigningCertSha1(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE)
    private val authMutex = Mutex()

    private val _isAuthenticated = MutableStateFlow(
        auth?.currentUser != null || !prefs.getString(KEY_REST_ID_TOKEN, null).isNullOrBlank()
    )
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        auth?.currentUser?.uid?.let { uid ->
            prefs.edit().putString(KEY_CACHED_UID, uid).apply()
            _isAuthenticated.value = true
        }
    }

    val currentUserId: String?
        get() = auth?.currentUser?.uid
            ?: prefs.getString(KEY_CACHED_UID, null)
            ?: PlayerIdentity.getOrCreateDeviceUuid(context)

    suspend fun ensureAuthenticated(): String? = withContext(Dispatchers.IO) {
        authMutex.withLock {
            val existing = auth?.currentUser?.uid
            if (existing != null) {
                _isAuthenticated.value = true
                prefs.edit().putString(KEY_CACHED_UID, existing).apply()
                return@withLock existing
            }

            if (auth != null) {
                try {
                    val task = auth.signInAnonymously()
                    val result = Tasks.await(task, 10, TimeUnit.SECONDS)
                    val newUid = result.user?.uid
                    if (newUid != null) {
                        _isAuthenticated.value = true
                        prefs.edit().putString(KEY_CACHED_UID, newUid).apply()
                        Log.d(TAG, "Anonymous auth successful: uid=$newUid")
                        return@withLock newUid
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Native anonymous sign-in failed: ${e.message}. Falling back to Identity Toolkit REST.")
                }
            }

            if (apiKey.isNotBlank()) {
                val cachedUid = prefs.getString(KEY_CACHED_UID, null)
                val cachedRefresh = prefs.getString(KEY_REST_REFRESH_TOKEN, null)
                if (!cachedUid.isNullOrBlank() && !cachedRefresh.isNullOrBlank()) {
                    val token = getIdTokenLocked(forceRefresh = false)
                    if (!token.isNullOrBlank()) {
                        _isAuthenticated.value = true
                        return@withLock prefs.getString(KEY_CACHED_UID, cachedUid)
                    }
                }

                val restUid = signInWithRestIdentityToolkit()
                if (!restUid.isNullOrBlank()) {
                    _isAuthenticated.value = true
                    return@withLock restUid
                }
            }

            val fallbackUid = prefs.getString(KEY_CACHED_UID, null)
                ?: PlayerIdentity.getOrCreateDeviceUuid(context)
            fallbackUid
        }
    }

    fun getCachedIdTokenIfValid(): String? {
        if (auth?.currentUser != null) return null
        val cachedToken = prefs.getString(KEY_REST_ID_TOKEN, null)
        val expiryMs = prefs.getLong(KEY_REST_TOKEN_EXPIRY_MS, 0L)
        return if (!cachedToken.isNullOrBlank() && System.currentTimeMillis() < expiryMs) cachedToken else null
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        authMutex.withLock {
            getIdTokenLocked(forceRefresh)
        }
    }

    private suspend fun getIdTokenLocked(forceRefresh: Boolean): String? {
        val user = auth?.currentUser
        if (user != null) {
            try {
                val task = user.getIdToken(forceRefresh)
                val tokenResult = Tasks.await(task, 10, TimeUnit.SECONDS)
                val token = tokenResult.token
                if (!token.isNullOrBlank()) {
                    _isAuthenticated.value = true
                    prefs.edit().putString(KEY_CACHED_UID, user.uid).apply()
                    return token
                }
            } catch (e: Exception) {
                Log.w(TAG, "Native getIdToken failed: ${e.message}.")
            }
            // Do not mint a separate REST Identity Toolkit UID while native FirebaseAuth has an active user,
            // otherwise currentUserId (auth.currentUser.uid) and the Bearer token's uid will mismatch (403).
            return null
        }

        if (apiKey.isNotBlank()) {
            val cachedToken = prefs.getString(KEY_REST_ID_TOKEN, null)
            val refreshToken = prefs.getString(KEY_REST_REFRESH_TOKEN, null)
            val expiryMs = prefs.getLong(KEY_REST_TOKEN_EXPIRY_MS, 0L)

            if (!forceRefresh && !cachedToken.isNullOrBlank() && System.currentTimeMillis() < expiryMs) {
                return cachedToken
            }

            if (!refreshToken.isNullOrBlank()) {
                val refreshed = refreshRestIdToken(refreshToken)
                if (!refreshed.isNullOrBlank()) {
                    return refreshed
                }
            }

            val newUid = signInWithRestIdentityToolkit()
            if (!newUid.isNullOrBlank()) {
                return prefs.getString(KEY_REST_ID_TOKEN, null)
            }
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
                    Log.w(TAG, "[AuthRepository] REST signUp HTTP ${response.code}: $err")
                    return null
                }
                val respStr = response.body?.string().orEmpty()
                if (respStr.isBlank()) return null
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
                        .apply()
                    _isAuthenticated.value = true
                    Log.i(TAG, "[AuthRepository] REST Identity Toolkit anonymous auth succeeded: uid=$localId")
                    localId
                } else {
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
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
                    Log.w(TAG, "[AuthRepository] REST token refresh HTTP ${response.code}: $err")
                    if (response.code in 400..403) {
                        prefs.edit()
                            .remove(KEY_REST_ID_TOKEN)
                            .remove(KEY_REST_REFRESH_TOKEN)
                            .remove(KEY_REST_TOKEN_EXPIRY_MS)
                            .apply()
                        _isAuthenticated.value = false
                    }
                    return null
                }
                val respStr = response.body?.string().orEmpty()
                if (respStr.isBlank()) return null
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
                    editor.apply()
                    _isAuthenticated.value = true
                    Log.d(TAG, "[AuthRepository] REST token refresh succeeded")
                    newIdToken
                } else {
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "[AuthRepository] REST token refresh failed: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_CACHED_UID = "cached_auth_uid"
        private const val KEY_REST_ID_TOKEN = "cached_rest_id_token"
        private const val KEY_REST_REFRESH_TOKEN = "cached_rest_refresh_token"
        private const val KEY_REST_TOKEN_EXPIRY_MS = "cached_rest_token_expiry_ms"
    }
}
