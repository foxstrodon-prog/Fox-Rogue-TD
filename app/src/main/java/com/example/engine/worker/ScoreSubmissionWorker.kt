package com.example.engine.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.engine.network.AuthRepository
import com.example.engine.network.FirestoreLeaderboardApi
import com.example.engine.storage.LeaderboardStore
import com.example.model.FailureKind
import com.example.model.LeaderboardResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScoreSubmissionWorker @JvmOverloads constructor(
  context: Context,
  params: WorkerParameters,
  private val api: FirestoreLeaderboardApi = FirestoreLeaderboardApi(),
  private val store: LeaderboardStore = LeaderboardStore(context),
  private val authRepo: AuthRepository = AuthRepository(context)
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    Log.d(TAG, "[ScoreSubmissionWorker] Triggered: runAttemptCount=$runAttemptCount")
    if (runAttemptCount > 10) {
      Log.e(TAG, "[ScoreSubmissionWorker] runAttemptCount ($runAttemptCount) exceeded threshold (> 10). Aborting worker.")
      return@withContext Result.failure()
    }

    val pendingList = store.getPendingQueue()
    Log.d(TAG, "[ScoreSubmissionWorker] Pending submission queue size: ${pendingList.size}")
    if (pendingList.isEmpty()) {
      Log.d(TAG, "[ScoreSubmissionWorker] Pending queue empty. Nothing to submit.")
      return@withContext Result.success()
    }

    authRepo.ensureAuthenticated()
    var idToken = authRepo.getIdToken()
    val effectiveUserId = authRepo.currentUserId.orEmpty()
    Log.d(TAG, "[ScoreSubmissionWorker] Authenticated user context: uid=$effectiveUserId, hasIdToken=${!idToken.isNullOrBlank()}")

    if (idToken.isNullOrBlank() || effectiveUserId.isBlank()) {
      Log.w(TAG, "[ScoreSubmissionWorker] Missing auth credentials (idToken or effectiveUserId). Requesting retry.")
      return@withContext Result.retry()
    }

    for (item in pendingList) {
      val itemWithUser = item.copy(userId = effectiveUserId)
      Log.d(TAG, "[ScoreSubmissionWorker] Submitting score for runId=${item.runId}, callsign=${item.callsign}, score=${item.score}, waves=${item.wavesCleared}, attempt=${item.attempts + 1}")
      var result = api.submitScore(itemWithUser, idToken = idToken)
      if (result is LeaderboardResult.Failure && result.kind == FailureKind.SECURITY_REJECTED) {
        Log.w(TAG, "[ScoreSubmissionWorker] Score submission returned SECURITY_REJECTED for runId=${item.runId}. Attempting forced token refresh.")
        val refreshed = authRepo.getIdToken(forceRefresh = true)
        if (!refreshed.isNullOrBlank() && refreshed != idToken) {
          idToken = refreshed
          result = api.submitScore(itemWithUser, idToken = idToken)
        }
      }
      when (result) {
        is LeaderboardResult.Success -> {
          Log.i(TAG, "[ScoreSubmissionWorker] Confirmed score submission for runId=${item.runId}. Removing from pending queue.")
          store.removePending(item.runId)
          store.recordSubmittedId(item.runId)
          com.example.engine.repository.LeaderboardRepository.notifySubmissionComplete()
        }
        is LeaderboardResult.Failure -> {
          Log.w(TAG, "[ScoreSubmissionWorker] Submission failed for runId=${item.runId}: kind=${result.kind}, message=${result.message}")
          when (result.kind) {
            FailureKind.SECURITY_REJECTED -> {
              store.incrementAttempt(item.runId)
              if (item.attempts >= 5) {
                Log.e(TAG, "[ScoreSubmissionWorker] RunId=${item.runId} exceeded max security attempts (5). Dropping from queue.")
                store.removePending(item.runId)
              } else {
                return@withContext Result.retry()
              }
            }
            FailureKind.RATE_LIMITED,
            FailureKind.TIMEOUT,
            FailureKind.SERVER_ERROR,
            FailureKind.NETWORK_UNAVAILABLE,
            FailureKind.UNCONFIGURED -> {
              store.incrementAttempt(item.runId)
              return@withContext Result.retry()
            }
            FailureKind.MALFORMED_PAYLOAD -> {
              Log.e(TAG, "[ScoreSubmissionWorker] RunId=${item.runId} failed with MALFORMED_PAYLOAD. Dropping from queue.")
              store.removePending(item.runId)
            }
          }
        }
      }
    }

    Log.i(TAG, "[ScoreSubmissionWorker] All pending items processed successfully.")
    Result.success()
  }

  companion object {
    private const val TAG = "ScoreSubmissionWorker"
  }
}
