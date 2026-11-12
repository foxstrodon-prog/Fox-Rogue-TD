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
import java.util.concurrent.TimeUnit

class AuthRepository @JvmOverloads constructor(
    private val context: Context,
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE)

    private val _isAuthenticated = MutableStateFlow(auth?.currentUser != null)
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
                newUid
            } else {
                prefs.getString(KEY_CACHED_UID, null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Anonymous sign-in failed or offline: ${e.message}")
            prefs.getString(KEY_CACHED_UID, null)
                ?: PlayerIdentity.getOrCreateDeviceUuid(context)
        }
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val user = auth?.currentUser ?: return@withContext null
        try {
            val task = user.getIdToken(forceRefresh)
            val tokenResult = Tasks.await(task, 15, TimeUnit.SECONDS)
            tokenResult.token
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve Firebase ID token: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_CACHED_UID = "cached_auth_uid"
    }
}
