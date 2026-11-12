package com.example.engine.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.engine.storage.PlayerIdentity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AuthRepository @JvmOverloads constructor(
    private val context: Context,
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE)

    private val _isAuthenticated = MutableStateFlow(auth?.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var restIdToken: String? = prefs.getString(KEY_REST_ID_TOKEN, null)
    private var restTokenExpiryMs: Long = prefs.getLong(KEY_REST_TOKEN_EXPIRY, 0L)
    private var restUid: String? = prefs.getString(KEY_REST_UID, null)

    init {
        auth?.currentUser?.uid?.let { uid ->
            prefs.edit().putString(KEY_CACHED_UID, uid).apply()
            _isAuthenticated.value = true
        }
    }

    val currentUserId: String?
        get() = auth?.currentUser?.uid
            ?: (if (auth != null) restUid else null)
            ?: prefs.getString(KEY_CACHED_UID, null)
            ?: PlayerIdentity.getOrCreateDeviceUuid(context)

    suspend fun ensureAuthenticated(): String? = withContext(Dispatchers.IO) {
        val existing = auth?.currentUser?.uid
        if (existing != null) {
            _isAuthenticated.value = true
            prefs.edit().putString(KEY_CACHED_UID, existing).apply()
            return@withContext existing
        }

        if (auth == null) {
            val fallbackUid = prefs.getString(KEY_CACHED_UID, null)
                ?: PlayerIdentity.getOrCreateDeviceUuid(context)
            return@withContext fallbackUid
        }

        try {
            val task = auth.signInAnonymously()
            val result = Tasks.await(task, 15, TimeUnit.SECONDS)
            val newUid = result.user?.uid
            if (newUid != null) {
                _isAuthenticated.value = true
                prefs.edit().putString(KEY_CACHED_UID, newUid).apply()
                Log.d(TAG, "Anonymous auth successful: uid=$newUid")
                return@withContext newUid
            }
        } catch (e: Exception) {
            Log.w(TAG, "Native anonymous sign-in failed: ${e.message}. Attempting REST Identity Toolkit fallback.")
            val restUidResult = requestRestAnonymousAuth()
            if (!restUidResult.isNullOrBlank()) {
                return@withContext restUidResult
            }
        }

        val fallbackUid = prefs.getString(KEY_CACHED_UID, null)
            ?: PlayerIdentity.getOrCreateDeviceUuid(context)
        fallbackUid
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (auth == null) return@withContext null

        val user = auth.currentUser
        if (user != null) {
            try {
                val task = user.getIdToken(forceRefresh)
                val tokenResult = Tasks.await(task, 15, TimeUnit.SECONDS)
                val token = tokenResult.token
                if (!token.isNullOrBlank()) return@withContext token
            } catch (e: Exception) {
                Log.w(TAG, "Native getIdToken failed: ${e.message}. Falling back to REST token.")
            }
        }

        val now = System.currentTimeMillis()
        if (!forceRefresh && !restIdToken.isNullOrBlank() && now < restTokenExpiryMs) {
            return@withContext restIdToken
        }

        requestRestAnonymousAuth()
        restIdToken
    }

    private suspend fun requestRestAnonymousAuth(): String? {
        val apiKey = com.example.BuildConfig.LEADERBOARD_API_KEY
        if (apiKey.isBlank()) return null
        return try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
            val json = JSONObject().put("returnSecureToken", true)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Android-Package", "com.aistudio.roguetd.xtkp")
                .addHeader("X-Android-Cert", FirestoreLeaderboardApi.ANDROID_CERT_SHA1)
                .post(json.toString().toRequestBody(mediaType))
                .build()

            OkHttpClientProvider.client.newCall(request).awaitResponse().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val resJson = JSONObject(body)
                    val token = resJson.optString("idToken", "")
                    val uid = resJson.optString("localId", "")
                    val expiresIn = resJson.optLong("expiresIn", 3600L)
                    if (token.isNotBlank() && uid.isNotBlank()) {
                        restIdToken = token
                        restUid = uid
                        restTokenExpiryMs = System.currentTimeMillis() + (expiresIn * 1000L) - 60_000L
                        prefs.edit()
                            .putString(KEY_REST_ID_TOKEN, token)
                            .putString(KEY_REST_UID, uid)
                            .putLong(KEY_REST_TOKEN_EXPIRY, restTokenExpiryMs)
                            .putString(KEY_CACHED_UID, uid)
                            .apply()
                        _isAuthenticated.value = true
                        Log.i(TAG, "[AuthRepository] REST Anonymous Auth successful: uid=$uid")
                        uid
                    } else null
                } else {
                    Log.w(TAG, "[AuthRepository] REST Anonymous Auth failed with code ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[AuthRepository] REST Anonymous Auth exception: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_CACHED_UID = "cached_auth_uid"
        private const val KEY_REST_ID_TOKEN = "rest_id_token"
        private const val KEY_REST_TOKEN_EXPIRY = "rest_token_expiry"
        private const val KEY_REST_UID = "rest_uid"
    }
}
