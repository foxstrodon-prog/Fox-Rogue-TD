package com.example.engine.worker

import android.content.Context
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
    if (runAttemptCount > 10) return@withContext Result.failure()

    val pendingList = store.getPendingQueue()
    if (pendingList.isEmpty()) return@withContext Result.success()

    authRepo.ensureAuthenticated()
    var idToken = authRepo.getIdToken()
    val effectiveUserId = authRepo.currentUserId.orEmpty()

    if (idToken.isNullOrBlank() || effectiveUserId.isBlank()) {
      return@withContext Result.retry()
    }

    for (item in pendingList) {
      val itemWithUser = item.copy(userId = effectiveUserId)
      var result = api.submitScore(itemWithUser, idToken = idToken)
      if (result is LeaderboardResult.Failure && result.kind == FailureKind.SECURITY_REJECTED) {
        val refreshed = authRepo.getIdToken(forceRefresh = true)
        if (!refreshed.isNullOrBlank() && refreshed != idToken) {
          idToken = refreshed
          result = api.submitScore(itemWithUser, idToken = idToken)
        }
      }
      when (result) {
        is LeaderboardResult.Success -> {
          store.removePending(item.runId)
          store.recordSubmittedId(item.runId)
        }
        is LeaderboardResult.Failure -> {
          when (result.kind) {
            FailureKind.SECURITY_REJECTED -> {
              store.incrementAttempt(item.runId)
              if (item.attempts >= 5) {
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
              store.removePending(item.runId)
            }
          }
        }
      }
    }

    Result.success()
  }
}
