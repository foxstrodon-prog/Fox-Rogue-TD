package com.example.engine.repository

import android.os.SystemClock
import com.example.engine.network.FirestoreLeaderboardApi
import com.example.engine.storage.LeaderboardStore
import com.example.engine.worker.LeaderboardWorkScheduler
import com.example.model.GlobalBoardStatus
import com.example.model.LeaderboardResult
import com.example.model.LeaderboardUiState
import com.example.model.PendingSubmission
import com.example.model.RunRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LeaderboardRepository(
  private val api: FirestoreLeaderboardApi = FirestoreLeaderboardApi(),
  private val store: LeaderboardStore,
  private val scheduler: LeaderboardWorkScheduler
) {

  private val _boardState = MutableStateFlow(LeaderboardUiState())
  val boardState: StateFlow<LeaderboardUiState> = _boardState.asStateFlow()

  private val fetchMutex = Mutex()
  private var lastFetchElapsedRealtime: Long = 0L

  suspend fun initialize() = withContext(Dispatchers.IO) {
    val cached = store.getCachedLeaderboard()
    val pending = store.getPendingQueue()
    val initialStatus = if (!api.isConfigured) {
      GlobalBoardStatus.NOT_CONFIGURED
    } else if (cached.isNotEmpty()) {
      GlobalBoardStatus.OFFLINE_CACHED
    } else {
      GlobalBoardStatus.IDLE
    }
    _boardState.update {
      it.copy(
        entries = cached,
        status = initialStatus,
        pendingCount = pending.size
      )
    }
  }

  suspend fun fetchLeaderboard(forceRefresh: Boolean = false) = withContext(Dispatchers.IO) {
    if (!api.isConfigured) {
      val cached = store.getCachedLeaderboard()
      val pending = store.getPendingQueue()
      _boardState.update {
        it.copy(
          entries = cached,
          status = GlobalBoardStatus.NOT_CONFIGURED,
          pendingCount = pending.size
        )
      }
      return@withContext
    }

    val now = SystemClock.elapsedRealtime()
    if (!forceRefresh && (now - lastFetchElapsedRealtime < 30_000L)) {
      return@withContext
    }
    if (forceRefresh && (now - lastFetchElapsedRealtime < 3_000L)) {
      return@withContext
    }

    fetchMutex.withLock {
      val lockNow = SystemClock.elapsedRealtime()
      if (!forceRefresh && (lockNow - lastFetchElapsedRealtime < 30_000L)) {
        return@withContext
      }

      _boardState.update {
        it.copy(
          isRefreshing = true,
          status = if (it.entries.isEmpty()) GlobalBoardStatus.LOADING else it.status
        )
      }

      val result = api.fetchTopScores()
      when (result) {
        is LeaderboardResult.Success -> {
          lastFetchElapsedRealtime = SystemClock.elapsedRealtime()
          store.saveCachedLeaderboard(result.data)
          val pendingCount = store.getPendingQueue().size
          _boardState.update {
            it.copy(
              entries = result.data,
              status = GlobalBoardStatus.ONLINE,
              failureKind = null,
              isRefreshing = false,
              pendingCount = pendingCount,
              lastFetchMs = System.currentTimeMillis()
            )
          }
        }
        is LeaderboardResult.Failure -> {
          val cached = store.getCachedLeaderboard()
          val pendingCount = store.getPendingQueue().size
          val status = if (cached.isNotEmpty()) GlobalBoardStatus.OFFLINE_CACHED else GlobalBoardStatus.ERROR
          _boardState.update {
            it.copy(
              entries = if (cached.isNotEmpty()) cached else it.entries,
              status = status,
              failureKind = result.kind,
              isRefreshing = false,
              pendingCount = pendingCount
            )
          }
        }
      }
    }
  }

  suspend fun enqueueRun(run: RunRecord, callsign: String, isDevTuned: Boolean) = withContext(Dispatchers.IO) {
    if (isDevTuned || run.isDevTuned) {
      return@withContext
    }

    if (store.isAlreadySubmitted(run.id)) {
      return@withContext
    }

    val submission = PendingSubmission(
      runId = run.id,
      callsign = callsign,
      score = run.score,
      wavesCleared = run.wavesCleared,
      scrapEarned = run.scrapEarned,
      totalDamage = run.totalDamage.toLong(),
      timestampMs = run.timestamp,
      clientVersion = com.example.BuildConfig.VERSION_NAME
    )

    store.enqueuePending(submission)
    val pendingCount = store.getPendingQueue().size
    _boardState.update { it.copy(pendingCount = pendingCount) }

    scheduler.triggerImmediateSync()
  }

  fun retryPendingSubmissions() {
    scheduler.triggerImmediateSync()
  }
}
