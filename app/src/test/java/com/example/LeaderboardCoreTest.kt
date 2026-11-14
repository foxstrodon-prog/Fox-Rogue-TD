package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.RemoteConfigManager
import com.example.engine.TelemetryManager
import com.example.engine.network.AuthRepository
import com.example.engine.network.FirestoreLeaderboardApi
import com.example.engine.storage.LeaderboardStore
import com.example.engine.storage.PlayerIdentity
import com.example.model.FailureKind
import com.example.model.LeaderboardResult
import com.example.model.OnlineScoreEntry
import com.example.model.PendingSubmission
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LeaderboardCoreTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
    val baseDir = File(context.filesDir, "leaderboard")
    baseDir.deleteRecursively()
  }

  @Test
  fun `PlayerIdentity creates persistent 36 char UUID and valid callsign`() {
    val uuid1 = PlayerIdentity.getOrCreateDeviceUuid(context)
    assertEquals(36, uuid1.length)
    assertTrue(uuid1.contains("-"))

    val uuid2 = PlayerIdentity.getOrCreateDeviceUuid(context)
    assertEquals(uuid1, uuid2)

    val callsign = PlayerIdentity.getPlayerCallsign(context)
    assertTrue(callsign.startsWith("Commander-"))

    PlayerIdentity.setPlayerCallsign(context, "Ghost_Fox")
    assertEquals("Ghost_Fox", PlayerIdentity.getPlayerCallsign(context))
  }

  @Test
  fun `FirestoreLeaderboardApi sanitizeCallsign adheres to rules`() {
    assertEquals("Commander", FirestoreLeaderboardApi.sanitizeCallsign(""))
    assertEquals("Commander", FirestoreLeaderboardApi.sanitizeCallsign("  a  "))
    assertEquals("Fox_Rogue_TD", FirestoreLeaderboardApi.sanitizeCallsign("Fox_Rogue_TD"))
    assertEquals("Commander", FirestoreLeaderboardApi.sanitizeCallsign("$$$%"))
    assertEquals("SuperLongCallsig", FirestoreLeaderboardApi.sanitizeCallsign("SuperLongCallsignThatExceeds16Chars"))
  }

  @Test
  fun `FirestoreLeaderboardApi normalizeCollection routes correctly`() {
    assertEquals("leaderboards", FirestoreLeaderboardApi.normalizeCollection("leaderboard"))
    assertEquals("leaderboards", FirestoreLeaderboardApi.normalizeCollection("leaderboards"))
    assertEquals("leaderboards", FirestoreLeaderboardApi.normalizeCollection("  LEADERBOARD  "))
    assertEquals("leaderboards", FirestoreLeaderboardApi.normalizeCollection(""))
    assertEquals("custom_board", FirestoreLeaderboardApi.normalizeCollection("custom_board"))
  }

  @Test
  fun `FirestoreLeaderboardApi returns UNCONFIGURED when keys are blank`() = runBlocking {
    val unconfiguredApi = FirestoreLeaderboardApi(
      projectId = "",
      apiKey = ""
    )
    assertFalse(unconfiguredApi.isConfigured)

    val pending = PendingSubmission(
      runId = "test-run-1",
      callsign = "Pilot",
      score = 5000,
      wavesCleared = 12,
      scrapEarned = 350,
      totalDamage = 45000L,
      timestampMs = 1000L,
      clientVersion = "0.0.0.008"
    )

    val submitResult = unconfiguredApi.submitScore(pending)
    assertTrue(submitResult is LeaderboardResult.Failure)
    assertEquals(FailureKind.UNCONFIGURED, (submitResult as LeaderboardResult.Failure).kind)

    val fetchResult = unconfiguredApi.fetchTopScores()
    assertTrue(fetchResult is LeaderboardResult.Failure)
    assertEquals(FailureKind.UNCONFIGURED, (fetchResult as LeaderboardResult.Failure).kind)
  }

  @Test
  fun `LeaderboardStore saves and retrieves cached leaderboard`() = runBlocking {
    val store = LeaderboardStore(context)
    val entries = listOf(
      OnlineScoreEntry("run-1", 1, "Ace", 15000, 20, 1200, 1000L, true),
      OnlineScoreEntry("run-2", 2, "Fox", 12000, 18, 950, 1005L, false)
    )

    store.saveCachedLeaderboard(entries)
    val cached = store.getCachedLeaderboard()

    assertEquals(2, cached.size)
    assertEquals("Ace", cached[0].callsign)
    assertEquals(15000, cached[0].score)
    assertTrue(cached[0].isLocalPlayer)
    assertEquals("Fox", cached[1].callsign)
    assertFalse(cached[1].isLocalPlayer)
  }

  @Test
  fun `LeaderboardStore manages pending submission queue and caps at 20`() = runBlocking {
    val store = LeaderboardStore(context)
    val now = System.currentTimeMillis()

    for (i in 1..25) {
      val item = PendingSubmission(
        runId = "run-$i",
        callsign = "Player-$i",
        score = i * 100,
        wavesCleared = i,
        scrapEarned = i * 10,
        totalDamage = i * 1000L,
        timestampMs = now,
        clientVersion = "0.0.0.008"
      )
      store.enqueuePending(item)
    }

    val pending = store.getPendingQueue()
    assertEquals(20, pending.size)
    // Most recently added is at index 0
    assertEquals("run-25", pending[0].runId)

    store.incrementAttempt("run-25")
    val updated = store.getPendingQueue()
    assertEquals(1, updated[0].attempts)
    assertTrue(updated[0].lastAttemptMs > 0L)

    store.removePending("run-25")
    val afterRemove = store.getPendingQueue()
    assertEquals(19, afterRemove.size)
    assertFalse(afterRemove.any { it.runId == "run-25" })
  }

  @Test
  fun `LeaderboardStore tracks submitted runIds`() = runBlocking {
    val store = LeaderboardStore(context)

    assertFalse(store.isAlreadySubmitted("run-abc"))
    store.recordSubmittedId("run-abc")
    assertTrue(store.isAlreadySubmitted("run-abc"))
  }

  @Test
  fun `LeaderboardRepository handles dev tuned exclusion and unconfigured status`() = runBlocking {
    val store = LeaderboardStore(context)
    val scheduler = com.example.engine.worker.LeaderboardWorkScheduler(context)
    val unconfiguredApi = FirestoreLeaderboardApi(projectId = "", apiKey = "")
    val repo = com.example.engine.repository.LeaderboardRepository(
      api = unconfiguredApi,
      store = store,
      scheduler = scheduler
    )

    repo.initialize()
    assertEquals(com.example.model.GlobalBoardStatus.NOT_CONFIGURED, repo.boardState.value.status)

    val devRun = com.example.model.RunRecord(
      id = "dev_run_1",
      sectorName = "Sector Alpha",
      wavesCleared = 10,
      enemiesKilled = 100,
      totalDamage = 20000f,
      scrapEarned = 1000,
      mvpTower = "Vulcan Gatling",
      isVictory = false,
      timestamp = System.currentTimeMillis(),
      isDevTuned = true
    )
    repo.enqueueRun(devRun, "TestCommander", isDevTuned = true)
    assertTrue("Dev tuned runs must not be enqueued", store.getPendingQueue().isEmpty())

    val legitRun = devRun.copy(id = "legit_run_1", isDevTuned = false)
    repo.enqueueRun(legitRun, "TestCommander", isDevTuned = false)
    val pending = store.getPendingQueue()
    assertEquals(1, pending.size)
    assertEquals("legit_run_1", pending[0].runId)
    assertEquals("TestCommander", pending[0].callsign)
  }

  @Test
  fun `AuthRepository falls back to persistent UUID when unauthenticated`() = runBlocking {
    val authRepo = AuthRepository(context, auth = null)
    val uid = authRepo.currentUserId
    assertTrue(!uid.isNullOrBlank())
    assertEquals(36, uid!!.length)

    val ensured = authRepo.ensureAuthenticated()
    assertEquals(uid, ensured)
    assertFalse(authRepo.isAuthenticated.value)
  }

  @Test
  fun `RemoteConfigManager provides standard fallback multipliers`() {
    val rcm = RemoteConfigManager(remoteConfig = null)
    assertEquals(1.0f, rcm.waveHpMultiplier, 0.01f)
    assertEquals(1.0f, rcm.scrapBountyMultiplier, 0.01f)
    assertEquals(1.0f, rcm.bossHealthMultiplier, 0.01f)
    assertEquals(0.20f, rcm.harmonicGridDamageBoost, 0.01f)
  }

  @Test
  fun `TelemetryManager logs churn telemetry and tutorial events without throwing`() {
    val tm = TelemetryManager(context, analytics = null)
    tm.logTutorialStart()
    tm.logTutorialComplete()
    tm.logWaveStart(1)
    tm.logWaveComplete(1, 150)
    tm.logLevelFail(waveFailed = 15, towersBuilt = 6, currencyRemaining = 45, mvpTowerClass = "HEAVY_SIEGE")
  }

  @Test
  fun `LeaderboardStore preserves userId across pending queue and cached entries`() = runBlocking {
    val store = LeaderboardStore(context)
    val item = PendingSubmission(
      runId = "run-auth-1",
      callsign = "Vanguard",
      score = 4200,
      wavesCleared = 8,
      scrapEarned = 260,
      totalDamage = 18000L,
      timestampMs = 1234567L,
      clientVersion = "0.0.1.020",
      userId = "firebase_anon_uid_999"
    )
    store.enqueuePending(item)

    val pending = store.getPendingQueue()
    assertEquals(1, pending.size)
    assertEquals("firebase_anon_uid_999", pending[0].userId)

    val entry = OnlineScoreEntry(
      id = "run-auth-1",
      rank = 1,
      callsign = "Vanguard",
      score = 4200,
      wavesCleared = 8,
      scrapEarned = 260,
      timestampMs = 1234567L,
      userId = "firebase_anon_uid_999"
    )
    store.saveCachedLeaderboard(listOf(entry))
    val cached = store.getCachedLeaderboard()
    assertEquals(1, cached.size)
    assertEquals("firebase_anon_uid_999", cached[0].userId)
    assertEquals(4200, cached[0].highScore)
    assertEquals(8, cached[0].maxWave)
  }
}
