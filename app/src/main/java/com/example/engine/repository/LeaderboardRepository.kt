package com.example.engine.repository

import android.os.SystemClock
import android.util.Log
import com.example.engine.network.AuthRepository
import com.example.engine.network.FirestoreLeaderboardApi
import com.example.engine.storage.LeaderboardStore
import com.example.engine.worker.LeaderboardWorkScheduler
import com.example.model.FailureKind
import com.example.model.GlobalBoardStatus
import com.example.model.LeaderboardResult
import com.example.model.LeaderboardUiState
import com.example.model.PendingSubmission
import com.example.model.RunRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LeaderboardRepository @JvmOverloads constructor(
  private val api: FirestoreLeaderboardApi = FirestoreLeaderboardApi(),
  private val store: LeaderboardStore,
  private val scheduler: LeaderboardWorkScheduler,
  private val authRepo: AuthRepository? = null
) {

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val _boardState = MutableStateFlow(LeaderboardUiState())
  val boardState: StateFlow<LeaderboardUiState> = _boardState.asStateFlow()

  private val fetchMutex = Mutex()
  private var lastFetchElapsedRealtime: Long = 0L

  suspend fun initialize() = withContext(Dispatchers.IO) {
    scope.launch {
      syncEventFlow.collect {
        val pending = store.getPendingQueue()
        Log.d(TAG, "[LeaderboardRepository] syncEventFlow triggered: pendingCount=${pending.size}. Triggering fresh fetch.")
        _boardState.update { it.copy(pendingCount = pending.size) }
        fetchLeaderboard(forceRefresh = true)
      }
    }
    val cached = store.getCachedLeaderboard()
    val pending = store.getPendingQueue()
    val initialStatus = if (!api.isConfigured) {
      GlobalBoardStatus.NOT_CONFIGURED
    } else if (cached.isNotEmpty()) {
      GlobalBoardStatus.OFFLINE_CACHED
    } else {
      GlobalBoardStatus.IDLE
    }
    Log.d(TAG, "[LeaderboardRepository] Initialized: status=$initialStatus, cachedEntries=${cached.size}, pendingCount=${pending.size}")
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
      Log.w(TAG, "[LeaderboardRepository] fetchLeaderboard: Unconfigured API. Setting status=NOT_CONFIGURED, pendingCount=${pending.size}")
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
      Log.d(TAG, "[LeaderboardRepository] fetchLeaderboard: Throttled (within 30s interval)")
      return@withContext
    }
    if (forceRefresh && (now - lastFetchElapsedRealtime < 3_000L)) {
      Log.d(TAG, "[LeaderboardRepository] fetchLeaderboard: Throttled (within 3s force-refresh interval)")
      return@withContext
    }

    fetchMutex.withLock {
      val lockNow = SystemClock.elapsedRealtime()
      if (!forceRefresh && (lockNow - lastFetchElapsedRealtime < 30_000L)) {
        return@withContext
      }

      Log.d(TAG, "[LeaderboardRepository] fetchLeaderboard: Initiating fetch. Transitioning status=LOADING if empty")
      _boardState.update {
        it.copy(
          isRefreshing = true,
          status = if (it.entries.isEmpty()) GlobalBoardStatus.LOADING else it.status
        )
      }

      var result = api.fetchTopScores()
      if (result is LeaderboardResult.Failure &&
          (result.kind == FailureKind.NETWORK_UNAVAILABLE || result.kind == FailureKind.TIMEOUT) &&
          _boardState.value.entries.isEmpty()) {
        Log.d(TAG, "[LeaderboardRepository] Cold-start network handshake pending, retrying in 1500ms...")
        kotlinx.coroutines.delay(1500L)
        result = api.fetchTopScores()
      }
      when (result) {
        is LeaderboardResult.Success -> {
          lastFetchElapsedRealtime = SystemClock.elapsedRealtime()
          val myUserId = authRepo?.currentUserId
          val processed = if (myUserId.isNullOrBlank()) {
            result.data
          } else {
            result.data.map { entry ->
              if (entry.userId == myUserId) entry.copy(isLocalPlayer = true) else entry
            }
          }
          store.saveCachedLeaderboard(processed)
          val pendingCount = store.getPendingQueue().size
          Log.i(TAG, "[LeaderboardRepository] fetchLeaderboard: SUCCESS. Transitioning status=ONLINE. Fetched ${processed.size} entries. Pending count: $pendingCount")
          _boardState.update {
            it.copy(
              entries = processed,
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
          val status = if (cached.isNotEmpty()) {
            GlobalBoardStatus.OFFLINE_CACHED
          } else if (result.kind == FailureKind.UNCONFIGURED) {
            GlobalBoardStatus.NOT_CONFIGURED
          } else {
            GlobalBoardStatus.ERROR
          }
          Log.w(TAG, "[LeaderboardRepository] fetchLeaderboard: FAILURE (kind=${result.kind}). Transitioning status=$status. Pending count: $pendingCount. Message: ${result.message}")
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

  suspend fun enqueueRun(
    run: RunRecord,
    callsign: String,
    isDevTuned: Boolean,
    userId: String? = null
  ) = withContext(Dispatchers.IO) {
    if (isDevTuned || run.isDevTuned) {
      Log.d(TAG, "[LeaderboardRepository] enqueueRun: Skipping dev tuned run ${run.id}")
      return@withContext
    }

    if (store.isAlreadySubmitted(run.id)) {
      Log.d(TAG, "[LeaderboardRepository] enqueueRun: Skipping already submitted run ${run.id}")
      return@withContext
    }

    val effectiveUserId = userId ?: authRepo?.currentUserId.orEmpty()

    val submission = PendingSubmission(
      runId = run.id,
      callsign = callsign,
      score = run.score,
      wavesCleared = run.wavesCleared,
      scrapEarned = run.scrapEarned,
      totalDamage = run.totalDamage.toLong(),
      timestampMs = run.timestamp,
      clientVersion = com.example.BuildConfig.VERSION_NAME,
      userId = effectiveUserId
    )

    store.enqueuePending(submission)
    val pendingCount = store.getPendingQueue().size
    Log.d(TAG, "[LeaderboardRepository] enqueueRun: Enqueued runId=${run.id}, score=${run.score}, waves=${run.wavesCleared}. Pending count: $pendingCount")
    _boardState.update { it.copy(pendingCount = pendingCount) }

    scheduler.triggerImmediateSync()
  }

  fun retryPendingSubmissions() {
    Log.d(TAG, "[LeaderboardRepository] retryPendingSubmissions: Triggering immediate WorkManager sync")
    scheduler.triggerImmediateSync()
  }

  companion object {
    private const val TAG = "LeaderboardRepository"
    val syncEventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun notifySubmissionComplete() {
      syncEventFlow.tryEmit(Unit)
    }
  }
}
