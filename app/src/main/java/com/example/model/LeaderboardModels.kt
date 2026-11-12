package com.example.model

enum class FailureKind {
  NETWORK_UNAVAILABLE,
  TIMEOUT,
  RATE_LIMITED,
  SERVER_ERROR,
  MALFORMED_PAYLOAD,
  SECURITY_REJECTED,
  UNCONFIGURED
}

sealed interface LeaderboardResult<out T> {
  data class Success<T>(val data: T) : LeaderboardResult<T>
  data class Failure(
    val kind: FailureKind,
    val message: String,
    val cause: Throwable? = null
  ) : LeaderboardResult<Nothing>
}

data class OnlineScoreEntry(
  val id: String,
  val rank: Int,
  val callsign: String,
  val score: Int,
  val wavesCleared: Int,
  val scrapEarned: Int,
  val timestampMs: Long,
  val isLocalPlayer: Boolean = false,
  val userId: String = ""
) {
  val name: String get() = callsign
  val username: String get() = callsign
  val highScore: Int get() = score
  val maxWave: Int get() = wavesCleared
  val timestamp: Long get() = timestampMs
  val isDevTuned: Boolean get() = false
  val date: String get() = if (timestampMs > 0) java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestampMs)) else ""
}

data class PendingSubmission(
  val runId: String,
  val callsign: String,
  val score: Int,
  val wavesCleared: Int,
  val scrapEarned: Int,
  val totalDamage: Long,
  val timestampMs: Long,
  val clientVersion: String,
  val attempts: Int = 0,
  val lastAttemptMs: Long = 0L,
  val userId: String = ""
) {
  val username: String get() = callsign
  val highScore: Int get() = score
  val maxWave: Int get() = wavesCleared
  val timestamp: Long get() = timestampMs
}

data class LeaderboardUiState(
  val entries: List<OnlineScoreEntry> = emptyList(),
  val status: GlobalBoardStatus = GlobalBoardStatus.IDLE,
  val failureKind: FailureKind? = null,
  val isRefreshing: Boolean = false,
  val pendingCount: Int = 0,
  val lastFetchMs: Long = 0L
)
