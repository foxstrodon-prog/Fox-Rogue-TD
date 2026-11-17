package com.example.engine.network

import android.content.Context
import com.example.BuildConfig
import com.example.audio.CustomAssetRecord
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Spark-Safe Firebase Telemetry Sync for Sound & Skin Votes.
 * Batches all Like (+1), Dislike (-1), and Star ratings into a SINGLE Firestore document
 * per authenticated user (`asset_votes/{userId}`) using full-document overwrite `.set(payload)`
 * (never `SetOptions.merge()`) so reset-to-neutral votes are cleanly removed from Firestore.
 */
class AssetVoteSyncManager(
    context: Context,
    private val authRepository: AuthRepository
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("asset_vote_sync_prefs", Context.MODE_PRIVATE)

    @Volatile
    private var lastSyncEpochMs: Long = 0L

    private val MIN_SYNC_COOLDOWN_MS = 10_000L

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

    fun syncVotesToFirestore(
        scope: CoroutineScope,
        records: List<CustomAssetRecord>,
        force: Boolean = false,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncEpochMs < MIN_SYNC_COOLDOWN_MS) {
            onResult(false, "Sync cooldown active (Spark guard)")
            return
        }

        val hasVotedItems = records.any { it.voteState != 0 || it.isFavoriteStar }
        val hadCloudVotes = prefs.getBoolean(KEY_HAD_CLOUD_VOTES, false)
        if (!hasVotedItems && !hadCloudVotes) {
            onResult(true, "No votes to sync")
            return
        }

        lastSyncEpochMs = now

        scope.launch(Dispatchers.IO) {
            try {
                if (FirebaseApp.getApps(appContext).isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Saved locally (Firebase unconfigured)")
                    }
                    return@launch
                }

                val uid = authRepository.ensureAuthenticated() ?: authRepository.currentUserId
                if (uid.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Saved locally (Auth pending)")
                    }
                    return@launch
                }

                val payload = buildVotePayload(uid, records, now)
                // Full overwrite (NO SetOptions.merge) so votes reset to Neutral (0) are removed from Firestore.
                FirebaseFirestore.getInstance()
                    .collection("asset_votes")
                    .document(uid)
                    .set(payload)
                    .addOnSuccessListener {
                        prefs.edit().putBoolean(KEY_HAD_CLOUD_VOTES, hasVotedItems).apply()
                        val msg = if (hasVotedItems) {
                            val count = (payload["votes"] as? Map<*, *>)?.size ?: 0
                            "Synced $count ratings to Firebase"
                        } else {
                            "Cleared cloud ratings on Firebase"
                        }
                        onResult(true, msg)
                    }
                    .addOnFailureListener { e ->
                        onResult(false, "Saved locally (${e.localizedMessage ?: "Offline"})")
                    }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Saved locally (Firebase unavailable)")
                }
            }
        }
    }

    internal fun resetCooldownForTest() {
        lastSyncEpochMs = 0L
    }

    companion object {
        private const val KEY_HAD_CLOUD_VOTES = "had_cloud_votes"
    }
}
