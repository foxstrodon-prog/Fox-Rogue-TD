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
import com.example.model.RunSubmissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

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
  private val initialized = AtomicBoolean(false)
  @Volatile
  private var lastFetchElapsedRealtime: Long = 0L

  fun close() {
    scope.cancel()
  }

  suspend fun initialize() = withContext(Dispatchers.IO) {
    if (initialized.compareAndSet(false, true)) {
      scope.launch {
      syncEventFlow.collect { event ->
        val pending = store.getPendingQueue()
        when (event) {
          is SubmissionEvent.Completed -> {
            Log.d(TAG, "[LeaderboardRepository] Submission completed for runId=${event.runId}: pendingCount=${pending.size}. Triggering fresh fetch.")
            _boardState.update { prev ->
              val isCurrentRun = prev.lastRunId == event.runId
              prev.copy(
                pendingCount = pending.size,
                lastSubmissionState = if (isCurrentRun) RunSubmissionState.SUBMITTED else prev.lastSubmissionState,
                lastSubmissionError = if (isCurrentRun) null else prev.lastSubmissionError
              )
            }
            fetchLeaderboard(forceRefresh = true, bypassThrottle = true)
          }
          is SubmissionEvent.Failed -> {
            Log.w(TAG, "[LeaderboardRepository] Submission failed for runId=${event.runId}: kind=${event.kind}, dropped=${event.dropped}, msg=${event.message}")
            _boardState.update { prev ->
              val isCurrentRun = prev.lastRunId == event.runId
              val nextSubmissionState = when {
                !isCurrentRun -> prev.lastSubmissionState
                event.dropped || event.kind == FailureKind.UNCONFIGURED -> RunSubmissionState.NONE
                else -> RunSubmissionState.PENDING
              }
              prev.copy(
                pendingCount = pending.size,
                lastSubmissionState = nextSubmissionState,
                lastSubmissionError = if (isCurrentRun) event.message else prev.lastSubmissionError,
                failureKind = event.kind,
                errorMessage = event.message,
                status = if (event.kind == FailureKind.UNCONFIGURED && prev.entries.isEmpty()) {
                  GlobalBoardStatus.NOT_CONFIGURED
                } else {
                  prev.status
                }
              )
            }
          }
          }
        }
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
        errorMessage = if (!api.isConfigured) "Leaderboard backend project ID or API key is not configured" else null,
        pendingCount = pending.size
      )
    }
  }

  suspend fun fetchLeaderboard(forceRefresh: Boolean = false, bypassThrottle: Boolean = false) = withContext(Dispatchers.IO) {
    if (!api.isConfigured) {
      val cached = store.getCachedLeaderboard()
      val pending = store.getPendingQueue()
      Log.w(TAG, "[LeaderboardRepository] fetchLeaderboard: Unconfigured API. Setting status=NOT_CONFIGURED, pendingCount=${pending.size}")
      _boardState.update {
        it.copy(
          entries = cached,
          status = GlobalBoardStatus.NOT_CONFIGURED,
          failureKind = FailureKind.UNCONFIGURED,
          errorMessage = "Leaderboard backend project ID or API key is not configured",
          pendingCount = pending.size
        )
      }
      return@withContext
    }

    val now = SystemClock.elapsedRealtime()
    if (!bypassThrottle && lastFetchElapsedRealtime > 0L) {
      if (!forceRefresh && (now - lastFetchElapsedRealtime < 30_000L)) {
        Log.d(TAG, "[LeaderboardRepository] fetchLeaderboard: Throttled (within 30s interval)")
        return@withContext
      }
      if (forceRefresh && (now - lastFetchElapsedRealtime < 3_000L)) {
        Log.d(TAG, "[LeaderboardRepository] fetchLeaderboard: Throttled (within 3s force-refresh interval)")
        return@withContext
      }
    }

    fetchMutex.withLock {
      val lockNow = SystemClock.elapsedRealtime()
      if (!bypassThrottle && lastFetchElapsedRealtime > 0L && !forceRefresh && (lockNow - lastFetchElapsedRealtime < 30_000L)) {
        return@withContext
      }

      Log.d(TAG, "[LeaderboardRepository] fetchLeaderboard: Initiating fetch. Transitioning status=LOADING if empty")
      _boardState.update {
        it.copy(
          isRefreshing = true,
          status = if (it.entries.isEmpty()) GlobalBoardStatus.LOADING else it.status
        )
      }

      try {
        val (myUserId, idToken) = authRepo?.getAuthenticatedSession() ?: (null to null)
        var result = api.fetchTopScores(idToken = idToken)
        if (result is LeaderboardResult.Failure &&
            (result.kind == FailureKind.NETWORK_UNAVAILABLE || result.kind == FailureKind.TIMEOUT) &&
            _boardState.value.entries.isEmpty()) {
          Log.d(TAG, "[LeaderboardRepository] Cold-start network handshake pending, retrying in 1500ms...")
          kotlinx.coroutines.delay(1500L)
          result = api.fetchTopScores(idToken = idToken)
        }
        when (result) {
          is LeaderboardResult.Success -> {
            lastFetchElapsedRealtime = maxOf(1L, SystemClock.elapsedRealtime())
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
                errorMessage = null,
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
                errorMessage = result.message,
                isRefreshing = false,
                pendingCount = pendingCount
              )
            }
          }
        }
      } finally {
        _boardState.update { it.copy(isRefreshing = false) }
      }
    }
  }

  @JvmOverloads
  suspend fun enqueueRun(
    run: RunRecord,
    callsign: String,
    isDevTuned: Boolean,
    userId: String? = null,
    isCurrentRun: Boolean = true
  ) = withContext(Dispatchers.IO) {
    if (isDevTuned || run.isDevTuned) {
      Log.d(TAG, "[LeaderboardRepository] enqueueRun: Skipping dev tuned run ${run.id}")
      if (isCurrentRun) {
        _boardState.update {
          it.copy(
            lastRunId = run.id,
            lastSubmissionState = RunSubmissionState.EXCLUDED_DEV_TUNED,
            lastSubmissionError = null
          )
        }
      }
      return@withContext
    }

    if (store.isAlreadySubmitted(run.id)) {
      Log.d(TAG, "[LeaderboardRepository] enqueueRun: Skipping already submitted run ${run.id}")
      if (isCurrentRun) {
        _boardState.update {
          it.copy(
            lastRunId = run.id,
            lastSubmissionState = RunSubmissionState.SUBMITTED,
            lastSubmissionError = null
          )
        }
      }
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

    if (!api.isConfigured) {
      Log.d(TAG, "[LeaderboardRepository] enqueueRun: Backend unconfigured, cached locally without sync for ${run.id}")
      _boardState.update {
        it.copy(
          pendingCount = pendingCount,
          lastRunId = if (isCurrentRun) run.id else it.lastRunId,
          lastSubmissionState = if (isCurrentRun) RunSubmissionState.LOCAL_ONLY else it.lastSubmissionState,
          lastSubmissionError = if (isCurrentRun) "Leaderboard backend project ID or API key is not configured" else it.lastSubmissionError
        )
      }
      return@withContext
    }

    Log.d(TAG, "[LeaderboardRepository] enqueueRun: Enqueued runId=${run.id}, score=${run.score}, waves=${run.wavesCleared}. Pending count: $pendingCount")
    _boardState.update {
      it.copy(
        pendingCount = pendingCount,
        lastRunId = if (isCurrentRun) run.id else it.lastRunId,
        lastSubmissionState = if (isCurrentRun) RunSubmissionState.PENDING else it.lastSubmissionState,
        lastSubmissionError = if (isCurrentRun) null else it.lastSubmissionError
      )
    }

    scheduler.triggerImmediateSync(replaceExisting = false)
  }

  suspend fun updatePendingCallsign(runId: String, callsign: String) = withContext(Dispatchers.IO) {
    if (store.updatePendingCallsign(runId, callsign)) {
      Log.d(TAG, "[LeaderboardRepository] Updated pending callsign for runId=$runId")
    }
  }

  suspend fun clearPendingQueue() = withContext(Dispatchers.IO) {
    store.clearPendingQueue()
    _boardState.update {
      it.copy(
        pendingCount = 0,
        lastSubmissionState = if (it.lastSubmissionState == RunSubmissionState.PENDING) RunSubmissionState.NONE else it.lastSubmissionState,
        lastSubmissionError = null
      )
    }
    Log.i(TAG, "[LeaderboardRepository] Pending submission queue cleared")
  }

  fun retryPendingSubmissions(replaceExisting: Boolean = false) {
    Log.d(TAG, "[LeaderboardRepository] retryPendingSubmissions: Triggering immediate WorkManager sync (replace=$replaceExisting)")
    scheduler.triggerImmediateSync(replaceExisting = replaceExisting)
  }

  fun resetRunSubmissionState() {
    _boardState.update {
      it.copy(
        lastRunId = null,
        lastSubmissionState = RunSubmissionState.NONE,
        lastSubmissionError = null
      )
    }
  }

  sealed interface SubmissionEvent {
    data class Completed(val runId: String) : SubmissionEvent
    data class Failed(
      val runId: String,
      val kind: FailureKind,
      val message: String,
      val dropped: Boolean
    ) : SubmissionEvent
  }

  companion object {
    private const val TAG = "LeaderboardRepository"
    val syncEventFlow = MutableSharedFlow<SubmissionEvent>(extraBufferCapacity = 8)

    fun notifySubmissionComplete(runId: String = "") {
      syncEventFlow.tryEmit(SubmissionEvent.Completed(runId))
    }

    fun notifySubmissionFailed(runId: String, kind: FailureKind, message: String, dropped: Boolean) {
      syncEventFlow.tryEmit(SubmissionEvent.Failed(runId, kind, message, dropped))
    }
  }
}
