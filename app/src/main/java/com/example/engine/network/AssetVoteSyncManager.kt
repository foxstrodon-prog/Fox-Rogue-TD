package com.example.engine.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.audio.CustomAssetRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.isActive

/**
 * Spark-Safe Firebase Telemetry Sync for Sound & Skin Votes.
 * Batches all Like (+1), Dislike (-1), and Star ratings into a SINGLE Firestore document
 * per authenticated user (`asset_votes/{userId}`) over Firestore REST using full-document
 * overwrite PATCH (no `updateMask`, never `SetOptions.merge()`) so reset-to-neutral votes
 * are cleanly removed from Firestore and REST Identity Toolkit fallback sessions work reliably.
 */
class AssetVoteSyncManager @JvmOverloads constructor(
    context: Context,
    private val authRepository: AuthRepository,
    private val httpClient: OkHttpClient = OkHttpClientProvider.client,
    projectId: String = FirestoreLeaderboardApi.resolveProjectId(BuildConfig.LEADERBOARD_PROJECT_ID, context),
    apiKey: String = FirestoreLeaderboardApi.resolveDefaultApiKey(context)
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("asset_vote_sync_prefs", Context.MODE_PRIVATE)
    private val resolvedProjectId: String = projectId.trim()
        .ifBlank { FirestoreLeaderboardApi.resolveProjectId("", appContext) }
    private val resolvedApiKey: String = FirestoreLeaderboardApi.sanitizeApiKey(apiKey)
        .ifBlank { FirestoreLeaderboardApi.resolveDefaultApiKey(appContext) }
    private val androidCertSha1: String? = FirestoreLeaderboardApi.resolveSigningCertSha1(appContext)

    @Volatile
    private var lastSyncEpochMs: Long = 0L

    private val isSyncInFlight = AtomicBoolean(false)

    private val MIN_SYNC_COOLDOWN_MS = 10_000L

    val isConfigured: Boolean
        get() = resolvedProjectId.isNotBlank() && resolvedApiKey.isNotBlank()

    fun buildVotePayload(
        userId: String,
        records: List<CustomAssetRecord>,
        nowMs: Long = System.currentTimeMillis()
    ): Map<String, Any> {
        val votedItems = records
            .filter { it.voteState != 0 || it.isFavoriteStar }
            .take(200)

        val votesMap = linkedMapOf<String, Any>()
        val likedIds = mutableListOf<String>()
        val dislikedIds = mutableListOf<String>()
        val starredIds = mutableListOf<String>()

        for (item in votedItems) {
            if (item.voteState == 1) likedIds.add(item.id)
            if (item.voteState == -1) dislikedIds.add(item.id)
            if (item.isFavoriteStar) starredIds.add(item.id)

            votesMap[item.id] = mapOf(
                "name" to item.name,
                "category" to item.category,
                "vote" to item.voteState,
                "star" to item.isFavoriteStar,
                "equipped" to item.isEquipped,
                "updatedAt" to item.updatedAtEpochMs
            )
        }

        return mapOf(
            "userId" to userId,
            "versionName" to BuildConfig.VERSION_NAME,
            "versionCode" to BuildConfig.VERSION_CODE,
            "syncedAtEpochMs" to nowMs,
            "likedCount" to likedIds.size,
            "dislikedCount" to dislikedIds.size,
            "starredCount" to starredIds.size,
            "likedIds" to likedIds,
            "dislikedIds" to dislikedIds,
            "starredIds" to starredIds,
            "votes" to votesMap
        )
    }

    internal fun buildFirestoreDocumentJson(payload: Map<String, Any>): JSONObject {
        @Suppress("UNCHECKED_CAST")
        val votesMap = payload["votes"] as? Map<String, Map<String, Any?>> ?: emptyMap()
        val voteEntryFields = JSONObject()
        for ((assetId, entry) in votesMap) {
            val entryFields = JSONObject()
                .put("name", JSONObject().put("stringValue", entry["name"]?.toString().orEmpty()))
                .put("category", JSONObject().put("stringValue", entry["category"]?.toString().orEmpty()))
                .put("vote", JSONObject().put("integerValue", ((entry["vote"] as? Number)?.toInt() ?: 0).toString()))
                .put("star", JSONObject().put("booleanValue", entry["star"] as? Boolean ?: false))
                .put("equipped", JSONObject().put("booleanValue", entry["equipped"] as? Boolean ?: false))
                .put("updatedAt", JSONObject().put("integerValue", ((entry["updatedAt"] as? Number)?.toLong() ?: 0L).toString()))
            voteEntryFields.put(assetId, JSONObject().put("mapValue", JSONObject().put("fields", entryFields)))
        }

        fun stringArrayValue(key: String): JSONObject {
            val list = payload[key] as? List<*> ?: emptyList<Any>()
            val values = JSONArray()
            for (item in list) {
                values.put(JSONObject().put("stringValue", item?.toString().orEmpty()))
            }
            return JSONObject().put("arrayValue", JSONObject().put("values", values))
        }

        val fields = JSONObject()
            .put("userId", JSONObject().put("stringValue", payload["userId"]?.toString().orEmpty()))
            .put("versionName", JSONObject().put("stringValue", payload["versionName"]?.toString().orEmpty()))
            .put("versionCode", JSONObject().put("integerValue", ((payload["versionCode"] as? Number)?.toInt() ?: 0).toString()))
            .put("syncedAtEpochMs", JSONObject().put("integerValue", ((payload["syncedAtEpochMs"] as? Number)?.toLong() ?: 0L).toString()))
            .put("likedCount", JSONObject().put("integerValue", ((payload["likedCount"] as? Number)?.toInt() ?: 0).toString()))
            .put("dislikedCount", JSONObject().put("integerValue", ((payload["dislikedCount"] as? Number)?.toInt() ?: 0).toString()))
            .put("starredCount", JSONObject().put("integerValue", ((payload["starredCount"] as? Number)?.toInt() ?: 0).toString()))
            .put("likedIds", stringArrayValue("likedIds"))
            .put("dislikedIds", stringArrayValue("dislikedIds"))
            .put("starredIds", stringArrayValue("starredIds"))
            .put("votes", JSONObject().put("mapValue", JSONObject().put("fields", voteEntryFields)))

        return JSONObject().put("fields", fields)
    }

    fun syncVotesToFirestore(
        scope: CoroutineScope,
        records: List<CustomAssetRecord>,
        force: Boolean = false,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncEpochMs < MIN_SYNC_COOLDOWN_MS) {
            prefs.edit().putBoolean(KEY_HAS_PENDING_SYNC, true).apply()
            onResult(false, "Saved locally • Pending cloud sync (cooldown active)")
            return
        }

        val hasVotedItems = records.any { it.voteState != 0 || it.isFavoriteStar }
        val hadCloudVotes = prefs.getBoolean(KEY_HAD_CLOUD_VOTES, false)
        val hasPendingSync = prefs.getBoolean(KEY_HAS_PENDING_SYNC, false)
        if (!hasVotedItems && !hadCloudVotes && !hasPendingSync) {
            onResult(true, "No votes to sync")
            return
        }

        if (!scope.isActive) {
            onResult(false, "Saved locally (Sync scope inactive)")
            return
        }

        if (!isSyncInFlight.compareAndSet(false, true)) {
            onResult(false, "Sync already in progress")
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                if (!isConfigured) {
                    prefs.edit().putBoolean(KEY_HAS_PENDING_SYNC, true).apply()
                    withContext(Dispatchers.Main) {
                        onResult(false, "Saved locally (Firebase unconfigured)")
                    }
                    return@launch
                }

                authRepository.ensureAuthenticated()
                var (uid, idToken) = authRepository.getAuthenticatedSession()
                if (uid.isNullOrBlank() || idToken.isNullOrBlank()) {
                    prefs.edit().putBoolean(KEY_HAS_PENDING_SYNC, true).apply()
                    val authErr = authRepository.lastAuthFailure?.message ?: "Auth unavailable"
                    withContext(Dispatchers.Main) {
                        onResult(false, "Saved locally ($authErr)")
                    }
                    return@launch
                }

                var payload = buildVotePayload(uid, records, System.currentTimeMillis())
                var result = patchVoteDocument(uid, idToken, payload)

                if (result is PatchOutcome.HttpFailure && (result.code == 401 || result.code == 403)) {
                    val (refreshedUid, refreshedToken) = authRepository.getAuthenticatedSession(forceRefresh = true)
                    if (!refreshedUid.isNullOrBlank() && !refreshedToken.isNullOrBlank()) {
                        uid = refreshedUid
                        idToken = refreshedToken
                        payload = buildVotePayload(uid, records, System.currentTimeMillis())
                        result = patchVoteDocument(uid, idToken, payload)
                    }
                }

                when (result) {
                    is PatchOutcome.Success -> {
                        lastSyncEpochMs = System.currentTimeMillis()
                        prefs.edit()
                            .putBoolean(KEY_HAD_CLOUD_VOTES, hasVotedItems)
                            .putBoolean(KEY_HAS_PENDING_SYNC, false)
                            .apply()
                        val msg = if (hasVotedItems) {
                            val count = (payload["votes"] as? Map<*, *>)?.size ?: 0
                            "Synced $count ratings to Firebase"
                        } else {
                            "Cleared cloud ratings on Firebase"
                        }
                        withContext(Dispatchers.Main) { onResult(true, msg) }
                    }
                    is PatchOutcome.HttpFailure -> {
                        lastSyncEpochMs = System.currentTimeMillis()
                        prefs.edit().putBoolean(KEY_HAS_PENDING_SYNC, true).apply()
                        val msg = formatFirestoreHttpError(result.code, result.body)
                        Log.w(TAG, "asset_votes sync HTTP ${result.code}: $msg | raw=${result.body}")
                        withContext(Dispatchers.Main) { onResult(false, "Saved locally ($msg)") }
                    }
                    is PatchOutcome.NetworkFailure -> {
                        prefs.edit().putBoolean(KEY_HAS_PENDING_SYNC, true).apply()
                        Log.w(TAG, "asset_votes sync network error: ${result.message}")
                        withContext(Dispatchers.Main) { onResult(false, "Saved locally (${result.message})") }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "asset_votes sync unexpected failure: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "Saved locally (${e.localizedMessage ?: "Firebase unavailable"})")
                }
            } finally {
                isSyncInFlight.set(false)
            }
        }
    }

    private sealed interface PatchOutcome {
        data object Success : PatchOutcome
        data class HttpFailure(val code: Int, val body: String) : PatchOutcome
        data class NetworkFailure(val message: String) : PatchOutcome
    }

    private suspend fun patchVoteDocument(
        userId: String,
        idToken: String,
        payload: Map<String, Any>
    ): PatchOutcome {
        val encodedUid = Uri.encode(userId)
        val url = "https://firestore.googleapis.com/v1/projects/$resolvedProjectId/databases/(default)/documents/asset_votes/$encodedUid?key=$resolvedApiKey"
        val docJson = buildFirestoreDocumentJson(payload).toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Goog-Api-Key", resolvedApiKey)
            .addHeader("Authorization", "Bearer $idToken")
            .apply {
                if (!androidCertSha1.isNullOrBlank()) {
                    addHeader("X-Android-Package", appContext.packageName)
                    addHeader("X-Android-Cert", androidCertSha1)
                }
            }
            .patch(docJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            httpClient.newCall(request).awaitResponse().use { response ->
                val code = response.code
                if (code in 200..299) {
                    PatchOutcome.Success
                } else {
                    PatchOutcome.HttpFailure(code, response.body?.string().orEmpty())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: SocketTimeoutException) {
            PatchOutcome.NetworkFailure("Connection timed out")
        } catch (_: UnknownHostException) {
            PatchOutcome.NetworkFailure("Offline")
        } catch (_: IOException) {
            PatchOutcome.NetworkFailure("Network error")
        }
    }

    private fun formatFirestoreHttpError(code: Int, body: String): String {
        if (body.isBlank()) return "HTTP $code"
        return try {
            val trimmed = body.trim()
            val json = if (trimmed.startsWith("[")) {
                JSONArray(trimmed).optJSONObject(0) ?: JSONObject()
            } else {
                JSONObject(trimmed)
            }
            val err = json.optJSONObject("error")
            val msg = err?.optString("message", "").orEmpty()
            val status = err?.optString("status", "").orEmpty()
            val details = err?.optJSONArray("details")
            var reason = ""
            if (details != null) {
                for (i in 0 until details.length()) {
                    val r = details.optJSONObject(i)?.optString("reason", "").orEmpty()
                    if (r.isNotBlank()) reason = r
                }
            }
            when {
                reason == "API_KEY_ANDROID_APP_BLOCKED" || msg.contains("Requests from this Android client application", ignoreCase = true) ->
                    "API key Android SHA-1 blocked"
                reason == "SERVICE_DISABLED" || msg.contains("is disabled", ignoreCase = true) ->
                    "Cloud Firestore API disabled"
                status == "PERMISSION_DENIED" || code == 403 ->
                    "PERMISSION_DENIED (deploy firestore.rules or enable Anonymous Auth)"
                code == 404 || status == "NOT_FOUND" ->
                    "Firestore database not found"
                code == 429 ->
                    "Rate limited (429)"
                msg.isNotBlank() -> msg
                else -> "HTTP $code"
            }
        } catch (_: Exception) {
            "HTTP $code"
        }
    }

    internal fun resetCooldownForTest() {
        lastSyncEpochMs = 0L
    }

    companion object {
        private const val TAG = "AssetVoteSyncManager"
        private const val KEY_HAD_CLOUD_VOTES = "had_cloud_votes"
        private const val KEY_HAS_PENDING_SYNC = "has_pending_sync"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
