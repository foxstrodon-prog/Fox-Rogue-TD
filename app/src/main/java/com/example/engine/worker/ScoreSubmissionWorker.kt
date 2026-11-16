package com.example.engine.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.engine.network.AuthRepository
import com.example.engine.network.FirestoreLeaderboardApi
import com.example.engine.repository.LeaderboardRepository
import com.example.engine.storage.LeaderboardStore
import com.example.model.FailureKind
import com.example.model.LeaderboardResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScoreSubmissionWorker @JvmOverloads constructor(
  context: Context,
  params: WorkerParameters,
  private val api: FirestoreLeaderboardApi = FirestoreLeaderboardApi(context = context),
  private val store: LeaderboardStore = LeaderboardStore(context),
  private val authRepo: AuthRepository = AuthRepository(
    context = context,
    apiKey = FirestoreLeaderboardApi.resolveDefaultApiKey(context)
  )
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    Log.d(TAG, "[ScoreSubmissionWorker] Triggered: runAttemptCount=$runAttemptCount")
    if (runAttemptCount > 10) {
      val msg = "Score submission aborted after $runAttemptCount attempts"
      Log.e(TAG, "[ScoreSubmissionWorker] runAttemptCount ($runAttemptCount) exceeded threshold (> 10). Aborting worker.")
      val pendingOnAbort = store.getPendingQueue()
      for (item in pendingOnAbort) {
        LeaderboardRepository.notifySubmissionFailed(
          runId = item.runId,
          kind = FailureKind.SERVER_ERROR,
          message = msg,
          dropped = false
        )
      }
      return@withContext Result.failure()
    }

    val pendingList = store.getPendingQueue()
    Log.d(TAG, "[ScoreSubmissionWorker] Pending submission queue size: ${pendingList.size}")
    if (pendingList.isEmpty()) {
      Log.d(TAG, "[ScoreSubmissionWorker] Pending queue empty. Nothing to submit.")
      return@withContext Result.success()
    }

    if (!api.isConfigured) {
      val msg = "Leaderboard project ID or API key is not configured"
      Log.w(TAG, "[ScoreSubmissionWorker] $msg. Aborting submission.")
      for (item in pendingList) {
        LeaderboardRepository.notifySubmissionFailed(
          runId = item.runId,
          kind = FailureKind.UNCONFIGURED,
          message = msg,
          dropped = false
        )
      }
      return@withContext Result.failure()
    }

    authRepo.ensureAuthenticated()
    var (sessionUid, idToken) = authRepo.getAuthenticatedSession()
    var effectiveUserId = sessionUid.orEmpty()
    Log.d(TAG, "[ScoreSubmissionWorker] Authenticated user context: uid=$effectiveUserId, hasIdToken=${!idToken.isNullOrBlank()}")

    if (idToken.isNullOrBlank() || effectiveUserId.isBlank()) {
      val authFailure = authRepo.lastAuthFailure
      val failureKind = authFailure?.kind ?: FailureKind.SECURITY_REJECTED
      val msg = authFailure?.message ?: "Anonymous authentication unavailable (no Firebase ID token)"
      Log.w(TAG, "[ScoreSubmissionWorker] Auth unavailable (kind=$failureKind): $msg")
      for (item in pendingList) {
        LeaderboardRepository.notifySubmissionFailed(
          runId = item.runId,
          kind = failureKind,
          message = msg,
          dropped = false
        )
      }
      return@withContext if (failureKind == FailureKind.UNCONFIGURED) Result.failure() else Result.retry()
    }

    var needsRetry = false
    var hasUnconfigured = false

    for (item in pendingList) {
      var itemWithUser = item.copy(userId = effectiveUserId)
      Log.d(TAG, "[ScoreSubmissionWorker] Submitting score for runId=${item.runId}, callsign=${item.callsign}, score=${item.score}, waves=${item.wavesCleared}, attempt=${item.attempts + 1}")
      var result = api.submitScore(itemWithUser, idToken = idToken)
      if (result is LeaderboardResult.Failure && result.kind == FailureKind.SECURITY_REJECTED) {
        Log.w(TAG, "[ScoreSubmissionWorker] Score submission returned SECURITY_REJECTED for runId=${item.runId}. Attempting forced token refresh.")
        val (refreshedUid, refreshedToken) = authRepo.getAuthenticatedSession(forceRefresh = true)
        if (!refreshedToken.isNullOrBlank() && !refreshedUid.isNullOrBlank()) {
          idToken = refreshedToken
          effectiveUserId = refreshedUid
          itemWithUser = item.copy(userId = effectiveUserId)
          result = api.submitScore(itemWithUser, idToken = idToken)
        } else {
          val refreshFailure = authRepo.lastAuthFailure
          if (refreshFailure != null && (
              refreshFailure.kind == FailureKind.NETWORK_UNAVAILABLE ||
              refreshFailure.kind == FailureKind.TIMEOUT ||
              refreshFailure.kind == FailureKind.RATE_LIMITED ||
              refreshFailure.kind == FailureKind.SERVER_ERROR
            )
          ) {
            result = LeaderboardResult.Failure(refreshFailure.kind, refreshFailure.message)
          }
        }
      }
      when (result) {
        is LeaderboardResult.Success -> {
          Log.i(TAG, "[ScoreSubmissionWorker] Confirmed score submission for runId=${item.runId}. Removing from pending queue.")
          store.removePending(item.runId)
          store.recordSubmittedId(item.runId)
          LeaderboardRepository.notifySubmissionComplete(item.runId)
        }
        is LeaderboardResult.Failure -> {
          Log.w(TAG, "[ScoreSubmissionWorker] Submission failed for runId=${item.runId}: kind=${result.kind}, message=${result.message}")
          when (result.kind) {
            FailureKind.SECURITY_REJECTED -> {
              store.incrementAttempt(item.runId)
              if (item.attempts + 1 >= 5) {
                Log.e(TAG, "[ScoreSubmissionWorker] RunId=${item.runId} exceeded max security attempts (5). Dropping from queue.")
                store.removePending(item.runId)
                LeaderboardRepository.notifySubmissionFailed(item.runId, result.kind, result.message, dropped = true)
              } else {
                LeaderboardRepository.notifySubmissionFailed(item.runId, result.kind, result.message, dropped = false)
                needsRetry = true
              }
            }
            FailureKind.UNCONFIGURED -> {
              LeaderboardRepository.notifySubmissionFailed(item.runId, result.kind, result.message, dropped = false)
              hasUnconfigured = true
            }
            FailureKind.RATE_LIMITED,
            FailureKind.TIMEOUT,
            FailureKind.SERVER_ERROR,
            FailureKind.NETWORK_UNAVAILABLE -> {
              LeaderboardRepository.notifySubmissionFailed(item.runId, result.kind, result.message, dropped = false)
              needsRetry = true
            }
            FailureKind.MALFORMED_PAYLOAD -> {
              Log.e(TAG, "[ScoreSubmissionWorker] RunId=${item.runId} failed with MALFORMED_PAYLOAD. Dropping from queue.")
              store.removePending(item.runId)
              LeaderboardRepository.notifySubmissionFailed(item.runId, result.kind, result.message, dropped = true)
            }
          }
        }
      }
    }

    return@withContext when {
      hasUnconfigured && !needsRetry -> Result.failure()
      needsRetry -> Result.retry()
      else -> {
        Log.i(TAG, "[ScoreSubmissionWorker] All pending items processed successfully.")
        Result.success()
      }
    }
  }

  companion object {
    private const val TAG = "ScoreSubmissionWorker"
  }
}
