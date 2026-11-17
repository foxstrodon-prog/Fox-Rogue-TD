package com.example

import android.app.Application
import android.content.Context
import android.content.pm.Signature
import android.content.pm.SigningInfo
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.engine.RemoteConfigManager
import com.example.engine.TelemetryManager
import com.example.engine.network.AuthRepository
import com.example.engine.network.FirestoreLeaderboardApi
import com.example.engine.repository.LeaderboardRepository
import com.example.engine.storage.LeaderboardStore
import com.example.engine.storage.PlayerIdentity
import com.example.engine.worker.LeaderboardWorkScheduler
import com.example.engine.worker.ScoreSubmissionWorker
import com.example.model.FailureKind
import com.example.model.GlobalBoardStatus
import com.example.model.LeaderboardResult
import com.example.model.OnlineScoreEntry
import com.example.model.PendingSubmission
import com.example.model.RunRecord
import com.example.model.RunSubmissionState
import com.example.ui.RogueTDViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.lang.reflect.Modifier
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LeaderboardCoreTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    WorkManagerTestInitHelper.initializeTestWorkManager(context)
    File(context.filesDir, "leaderboard").deleteRecursively()
    context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("rogue_td_player_identity", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("rogue_td_telemetry", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @After
  fun tearDown() {
    WorkManager.getInstance(context).cancelAllWork()
    WorkManagerImpl.getInstance(context)?.workDatabase?.close()
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
    val offlineClient = OkHttpClient.Builder()
      .addInterceptor { throw UnknownHostException("offline") }
      .build()
    val authRepo = AuthRepository(context, auth = null, httpClient = offlineClient)
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
      clientVersion = "0.0.1.022",
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

  @Test
  fun `FirestoreLeaderboardApi does not failover to alternateCollection on HTTP 403 SECURITY_REJECTED`() = runBlocking {
    var requestCount = 0
    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        requestCount++
        okhttp3.Response.Builder()
          .request(chain.request())
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(403)
          .message("Forbidden")
          .body(
            """{"error":{"code":403,"message":"Missing or insufficient permissions.","status":"PERMISSION_DENIED"}}"""
              .toByteArray()
              .let { okhttp3.ResponseBody.create(null, it) }
          )
          .build()
      }
      .build()

    val api = FirestoreLeaderboardApi(
      client = mockClient,
      projectId = "test-project",
      apiKey = "test-key",
      collection = "leaderboards"
    )
    val item = PendingSubmission(
      runId = "run-403",
      callsign = "Fox",
      score = 1000,
      wavesCleared = 10,
      scrapEarned = 500,
      totalDamage = 5000L,
      timestampMs = 1000L,
      clientVersion = "0.0.1.022"
    )

    val submitRes = api.submitScore(item)
    assertTrue(submitRes is LeaderboardResult.Failure)
    assertEquals(FailureKind.SECURITY_REJECTED, (submitRes as LeaderboardResult.Failure).kind)
    assertEquals("HTTP 403 on submitScore must fail fast after 1 request without cross-path failover", 1, requestCount)

    requestCount = 0
    val fetchRes = api.fetchTopScores(10)
    assertTrue(fetchRes is LeaderboardResult.Failure)
    assertEquals(FailureKind.SECURITY_REJECTED, (fetchRes as LeaderboardResult.Failure).kind)
    assertEquals("HTTP 403 on fetchTopScores must fail fast after 1 request without cross-path failover", 1, requestCount)
  }

  @Test
  fun `AuthRepository REST Identity Toolkit fallback authenticates and caches idToken when native FirebaseAuth fails`() = runBlocking {
    var capturedUrl = ""
    var capturedApiKeyHeader: String? = null
    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        val req = chain.request()
        capturedUrl = req.url.toString()
        capturedApiKeyHeader = req.header("X-Goog-Api-Key")
        val responseJson = """{"localId":"rest_anon_uid_777","idToken":"jwt_token_xyz","refreshToken":"refresh_token_abc","expiresIn":"3600"}"""
        okhttp3.Response.Builder()
          .request(req)
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body(okhttp3.ResponseBody.create(null, responseJson.toByteArray()))
          .build()
      }
      .build()

    context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE).edit().clear().commit()

    val authRepo = AuthRepository(
      context = context,
      auth = null,
      httpClient = mockClient,
      apiKey = "key=REST_TEST_KEY_456"
    )

    val uid = authRepo.ensureAuthenticated()
    assertEquals("rest_anon_uid_777", uid)
    assertTrue(authRepo.isAuthenticated.value)
    assertTrue(capturedUrl.contains("identitytoolkit.googleapis.com/v1/accounts:signUp?key=REST_TEST_KEY_456"))
    assertEquals("REST_TEST_KEY_456", capturedApiKeyHeader)

    val token = authRepo.getIdToken()
    assertEquals("jwt_token_xyz", token)
  }

  @Test
  fun `LeaderboardRepository bypassThrottle allows immediate refresh after score submission syncEventFlow`() = runBlocking {
    var fetchCount = 0
    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        fetchCount++
        val responseJson = """[{"document":{"name":"projects/p/databases/(default)/documents/leaderboards/run_$fetchCount","fields":{"username":{"stringValue":"Ace"},"highScore":{"integerValue":"5000"},"maxWave":{"integerValue":"10"},"timestamp":{"integerValue":"1000"},"isDevTuned":{"booleanValue":false}}}}]"""
        okhttp3.Response.Builder()
          .request(chain.request())
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body(okhttp3.ResponseBody.create(null, responseJson.toByteArray()))
          .build()
      }
      .build()

    val api = FirestoreLeaderboardApi(
      client = mockClient,
      projectId = "test-project",
      apiKey = "test-key",
      collection = "leaderboards"
    )
    val store = LeaderboardStore(context)
    val scheduler = com.example.engine.worker.LeaderboardWorkScheduler(context)
    val repo = com.example.engine.repository.LeaderboardRepository(
      api = api,
      store = store,
      scheduler = scheduler
    )

    repo.initialize()
    repo.fetchLeaderboard(forceRefresh = true)
    assertEquals(1, fetchCount)

    // Standard forceRefresh within 3s window is throttled
    repo.fetchLeaderboard(forceRefresh = true, bypassThrottle = false)
    assertEquals(1, fetchCount)

    // bypassThrottle = true immediately fetches even within 3s window
    repo.fetchLeaderboard(forceRefresh = true, bypassThrottle = true)
    assertEquals(2, fetchCount)
  }

  @Test
  fun `FirestoreLeaderboardApi requires non-blank apiKey and parses API_KEY_INVALID error`() = runBlocking {
    val missingKeyApi = FirestoreLeaderboardApi(projectId = "test-project", apiKey = "")
    assertFalse("Missing API key must mark API unconfigured", missingKeyApi.isConfigured)

    val invalidKeyClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        okhttp3.Response.Builder()
          .request(chain.request())
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(400)
          .message("Bad Request")
          .body(
            okhttp3.ResponseBody.create(
              null,
              """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}""".toByteArray()
            )
          )
          .build()
      }
      .build()

    val api = FirestoreLeaderboardApi(
      client = invalidKeyClient,
      projectId = "test-project",
      apiKey = "bad-key",
      collection = "leaderboards"
    )
    val res = api.fetchTopScores(10)
    assertTrue(res is LeaderboardResult.Failure)
    val failure = res as LeaderboardResult.Failure
    assertEquals(FailureKind.UNCONFIGURED, failure.kind)
    assertTrue("Error message must identify invalid API key, got: ${failure.message}", failure.message.contains("API Key invalid", ignoreCase = true))
  }

  @Test
  fun `LeaderboardStore updatePendingCallsign and clearPendingQueue work reliably`() = runBlocking {
    val store = LeaderboardStore(context)
    val item = PendingSubmission(
      runId = "run-edit-1",
      callsign = "OldName",
      score = 1200,
      wavesCleared = 3,
      scrapEarned = 150,
      totalDamage = 4000L,
      timestampMs = System.currentTimeMillis(),
      clientVersion = "0.0.1.026"
    )
    store.enqueuePending(item)
    assertTrue(store.updatePendingCallsign("run-edit-1", "Maverick"))
    assertEquals("Maverick", store.getPendingQueue().first().callsign)

    store.clearPendingQueue()
    assertTrue(store.getPendingQueue().isEmpty())
  }

  @Test
  fun `LeaderboardRepository tracks RunSubmissionState and surfaces failure messages`() = runBlocking {
    val failingClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        okhttp3.Response.Builder()
          .request(chain.request())
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(403)
          .message("Forbidden")
          .body(
            okhttp3.ResponseBody.create(
              null,
              """{"error":{"code":403,"message":"Missing or insufficient permissions.","status":"PERMISSION_DENIED"}}""".toByteArray()
            )
          )
          .build()
      }
      .build()

    val api = FirestoreLeaderboardApi(
      client = failingClient,
      projectId = "test-project",
      apiKey = "test-key",
      collection = "leaderboards"
    )
    val store = LeaderboardStore(context)
    val scheduler = com.example.engine.worker.LeaderboardWorkScheduler(context)
    val repo = com.example.engine.repository.LeaderboardRepository(
      api = api,
      store = store,
      scheduler = scheduler
    )

    repo.initialize()
    repo.fetchLeaderboard(forceRefresh = true)
    assertEquals(com.example.model.GlobalBoardStatus.ERROR, repo.boardState.value.status)
    assertEquals(FailureKind.SECURITY_REJECTED, repo.boardState.value.failureKind)
    assertTrue(repo.boardState.value.errorMessage?.contains("Security Rules", ignoreCase = true) == true)

    val run = com.example.model.RunRecord(
      id = "run-state-1",
      sectorName = "Sector Alpha",
      wavesCleared = 4,
      enemiesKilled = 30,
      totalDamage = 5000f,
      scrapEarned = 300,
      mvpTower = "Vulcan Minigun",
      isVictory = false,
      timestamp = System.currentTimeMillis(),
      isDevTuned = false
    )
    repo.enqueueRun(run, "Maverick", isDevTuned = false, userId = "uid_123")
    assertEquals(RunSubmissionState.PENDING, repo.boardState.value.lastSubmissionState)
    assertEquals("run-state-1", repo.boardState.value.lastRunId)
  }

  @Test
  fun `AuthRepository resolves default apiKey and attempts REST auth when FirebaseAuth is null`() = runBlocking {
    val hadFirebaseApp = FirebaseApp.getApps(context).isNotEmpty()
    FirebaseApp.clearInstancesForTest()
    try {
      FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setProjectId("test-isolated-project")
          .setApiKey("AIzaSy" + "0123456789".repeat(3) + "abc")
          .setApplicationId("1:1234567890:android:abcdef0123456789")
          .build()
      )
      var restUrl = ""
      val restClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
          restUrl = chain.request().url.toString()
          val body = """{"localId":"rest_default_key_uid","idToken":"tok_default_1","refreshToken":"ref_default_1","expiresIn":"3600"}"""
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody(null))
            .build()
        }
        .build()

      val authRepo = AuthRepository(context = context, auth = null, httpClient = restClient)
      val uid = authRepo.ensureAuthenticated()
      assertEquals("rest_default_key_uid", uid)
      assertTrue(restUrl.contains("identitytoolkit.googleapis.com/v1/accounts:signUp?key="))
      assertTrue(authRepo.isAuthenticated.value)
    } finally {
      FirebaseApp.clearInstancesForTest()
      if (hadFirebaseApp) {
        runCatching { FirebaseApp.initializeApp(context) }
      }
    }
  }

  @Test
  fun `ScoreSubmissionWorker aborts on runAttemptCount gt 10 and notifies repository`() = runBlocking {
    val store = LeaderboardStore(context)
    val scheduler = LeaderboardWorkScheduler(context)
    val api = FirestoreLeaderboardApi(projectId = "test-project", apiKey = "test-key")
    val repo = LeaderboardRepository(api = api, store = store, scheduler = scheduler)
    repo.initialize()

    val run = RunRecord(
      id = "run-abort-10",
      sectorName = "Sector Zero",
      wavesCleared = 5,
      enemiesKilled = 25,
      totalDamage = 4000f,
      scrapEarned = 200,
      mvpTower = "Vulcan Minigun",
      isVictory = false,
      timestamp = System.currentTimeMillis(),
      isDevTuned = false
    )
    repo.enqueueRun(run, "Pilot", isDevTuned = false, userId = "uid-1")
    assertEquals(RunSubmissionState.PENDING, repo.boardState.value.lastSubmissionState)

    val authRepo = AuthRepository(context, auth = null, apiKey = "")
    val worker = buildWorker(api, store, authRepo, attemptCount = 11)
    val result = worker.doWork()
    assertTrue(result is ListenableWorker.Result.Failure)

    delay(150)
    assertEquals("run-abort-10", repo.boardState.value.lastRunId)
    assertTrue(repo.boardState.value.lastSubmissionError?.contains("aborted", ignoreCase = true) == true)
    assertEquals(FailureKind.SERVER_ERROR, repo.boardState.value.failureKind)
  }

  @Test
  fun `ScoreSubmissionWorker fails fast on unconfigured api or UNCONFIGURED server response`() = runBlocking {
    val store = LeaderboardStore(context)
    val scheduler = LeaderboardWorkScheduler(context)
    val unconfiguredApi = FirestoreLeaderboardApi(projectId = "", apiKey = "")
    val repo = LeaderboardRepository(api = unconfiguredApi, store = store, scheduler = scheduler)
    repo.initialize()

    store.enqueuePending(
      PendingSubmission(
        runId = "run-unconf-1",
        callsign = "Pilot",
        score = 1000,
        wavesCleared = 3,
        scrapEarned = 100,
        totalDamage = 1500L,
        timestampMs = System.currentTimeMillis(),
        clientVersion = "0.0.1.026"
      )
    )

    val authRepo = AuthRepository(context, auth = null, apiKey = "")
    val worker1 = buildWorker(unconfiguredApi, store, authRepo)
    assertTrue(worker1.doWork() is ListenableWorker.Result.Failure)

    val restClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val url = chain.request().url.toString()
        val body = if (url.contains("identitytoolkit")) {
          """{"localId":"uid-2","idToken":"tok-2","refreshToken":"ref-2","expiresIn":"3600"}"""
        } else {
          """{"error":{"code":400,"message":"API key not valid.","status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}"""
        }
        val code = if (url.contains("identitytoolkit")) 200 else 400
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(code)
          .message(if (code == 200) "OK" else "Bad Request")
          .body(body.toResponseBody(null))
          .build()
      }
      .build()
    val configuredButRejectedApi = FirestoreLeaderboardApi(
      client = restClient,
      projectId = "test-project",
      apiKey = "bad-key"
    )
    val configuredAuth = AuthRepository(context, auth = null, httpClient = restClient, apiKey = "bad-key")
    val events = mutableListOf<LeaderboardRepository.SubmissionEvent>()
    val collectJob = launch { LeaderboardRepository.syncEventFlow.collect { events.add(it) } }
    delay(50)

    val worker2 = buildWorker(configuredButRejectedApi, store, configuredAuth)
    assertTrue(worker2.doWork() is ListenableWorker.Result.Failure)
    delay(100)
    collectJob.cancel()

    val unconfEvent = events.filterIsInstance<LeaderboardRepository.SubmissionEvent.Failed>()
      .lastOrNull { it.runId == "run-unconf-1" }
    assertNotNull(unconfEvent)
    assertEquals(FailureKind.UNCONFIGURED, unconfEvent!!.kind)
  }

  @Test
  fun `ScoreSubmissionWorker refreshes token on SECURITY_REJECTED and drops at attempt 5`() = runBlocking {
    val store = LeaderboardStore(context)
    context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE).edit()
      .putString("cached_auth_uid", "uid-refresh")
      .putString("cached_rest_id_token", "stale_token")
      .putString("cached_rest_refresh_token", "refresh_token_1")
      .putLong("cached_rest_token_expiry_ms", System.currentTimeMillis() + 600_000L)
      .commit()

    val seenAuthHeaders = mutableListOf<String?>()
    val refreshSuccessClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val req = chain.request()
        val url = req.url.toString()
        if (url.contains("securetoken.googleapis.com")) {
          Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"id_token":"fresh_token","refresh_token":"refresh_token_2","user_id":"uid-refresh","expires_in":"3600"}""".toResponseBody(null))
            .build()
        } else {
          val authHeader = req.header("Authorization")
          seenAuthHeaders.add(authHeader)
          if (authHeader == "Bearer stale_token") {
            Response.Builder()
              .request(req)
              .protocol(Protocol.HTTP_1_1)
              .code(403)
              .message("Forbidden")
              .body("""{"error":{"code":403,"message":"Missing or insufficient permissions.","status":"PERMISSION_DENIED"}}""".toResponseBody(null))
              .build()
          } else {
            Response.Builder()
              .request(req)
              .protocol(Protocol.HTTP_1_1)
              .code(200)
              .message("OK")
              .body("{}".toResponseBody(null))
              .build()
          }
        }
      }
      .build()

    store.enqueuePending(
      PendingSubmission(
        runId = "run-refresh-1",
        callsign = "Ace",
        score = 3000,
        wavesCleared = 6,
        scrapEarned = 200,
        totalDamage = 8000L,
        timestampMs = System.currentTimeMillis(),
        clientVersion = "0.0.1.026"
      )
    )

    val events = mutableListOf<LeaderboardRepository.SubmissionEvent>()
    val collectJob = launch { LeaderboardRepository.syncEventFlow.collect { events.add(it) } }
    delay(50)

    val api1 = FirestoreLeaderboardApi(client = refreshSuccessClient, projectId = "test-project", apiKey = "k")
    val auth1 = AuthRepository(context, auth = null, httpClient = refreshSuccessClient, apiKey = "k")
    val res1 = buildWorker(api1, store, auth1).doWork()
    assertTrue(res1 is ListenableWorker.Result.Success)
    assertEquals(listOf("Bearer stale_token", "Bearer fresh_token"), seenAuthHeaders)
    assertTrue(store.getPendingQueue().isEmpty())
    assertTrue(store.isAlreadySubmitted("run-refresh-1"))
    delay(100)
    assertTrue(events.any { it is LeaderboardRepository.SubmissionEvent.Completed && it.runId == "run-refresh-1" })

    // Now test dropping at attempts + 1 >= 5 when SECURITY_REJECTED persists
    val always403Client = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val req = chain.request()
        val url = req.url.toString()
        if (url.contains("securetoken.googleapis.com")) {
          Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"id_token":"still_rejected","refresh_token":"r2","user_id":"uid-refresh","expires_in":"3600"}""".toResponseBody(null))
            .build()
        } else {
          Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(403)
            .message("Forbidden")
            .body("""{"error":{"code":403,"message":"Permission denied","status":"PERMISSION_DENIED"}}""".toResponseBody(null))
            .build()
        }
      }
      .build()
    store.enqueuePending(
      PendingSubmission(
        runId = "run-drop-5",
        callsign = "Ace",
        score = 1200,
        wavesCleared = 4,
        scrapEarned = 100,
        totalDamage = 2000L,
        timestampMs = System.currentTimeMillis(),
        clientVersion = "0.0.1.026",
        attempts = 4
      )
    )
    val api2 = FirestoreLeaderboardApi(client = always403Client, projectId = "test-project", apiKey = "k")
    val auth2 = AuthRepository(context, auth = null, httpClient = always403Client, apiKey = "k")
    val res2 = buildWorker(api2, store, auth2).doWork()
    assertTrue(res2 is ListenableWorker.Result.Success)
    assertTrue("Pending item reaching 5th attempt must be dropped", store.getPendingQueue().isEmpty())
    delay(100)
    collectJob.cancel()

    val droppedEvent = events.filterIsInstance<LeaderboardRepository.SubmissionEvent.Failed>()
      .lastOrNull { it.runId == "run-drop-5" }
    assertNotNull(droppedEvent)
    assertTrue(droppedEvent!!.dropped)
  }

  @Test
  fun `LeaderboardWorkScheduler appends while running and replaces on explicit retry`() {
    val wm = WorkManager.getInstance(context)
    val scheduler = LeaderboardWorkScheduler(context)

    scheduler.triggerImmediateSync(replaceExisting = false)
    val firstInfos = wm.getWorkInfosForUniqueWork(LeaderboardWorkScheduler.WORK_NAME).get()
    assertEquals(1, firstInfos.size)
    val firstId = firstInfos[0].id

    // Simulate active execution of the first work item, then enqueue another without replacing
    val db = WorkManagerImpl.getInstance(context).workDatabase
    db.workSpecDao().setState(WorkInfo.State.RUNNING, firstId.toString())

    scheduler.triggerImmediateSync(replaceExisting = false)
    val afterAppend = wm.getWorkInfosForUniqueWork(LeaderboardWorkScheduler.WORK_NAME).get()
    assertEquals("APPEND_OR_REPLACE must schedule follow-up work while active work is RUNNING", 2, afterAppend.size)
    assertTrue(afterAppend.any { it.id == firstId && it.state == WorkInfo.State.RUNNING })
    assertTrue(afterAppend.any { it.id != firstId && (it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED) })

    // Explicit manual retry with replaceExisting = true replaces active/backed-off chain with a fresh ENQUEUED work
    scheduler.triggerImmediateSync(replaceExisting = true)
    val afterReplace = wm.getWorkInfosForUniqueWork(LeaderboardWorkScheduler.WORK_NAME).get()
    assertTrue(afterReplace.any { it.id != firstId && it.state == WorkInfo.State.ENQUEUED })
    assertNotEquals(firstId, afterReplace.first { it.state == WorkInfo.State.ENQUEUED }.id)
  }

  @Test
  fun `LeaderboardRepository deduplicates already submitted runs and scopes submission events to lastRunId`() = runBlocking {
    val okClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body("[]".toResponseBody(null))
          .build()
      }
      .build()
    val api = FirestoreLeaderboardApi(client = okClient, projectId = "test-project", apiKey = "k")
    val store = LeaderboardStore(context)
    val scheduler = LeaderboardWorkScheduler(context)
    val repo = LeaderboardRepository(api = api, store = store, scheduler = scheduler)
    repo.initialize()

    // Already submitted run is skipped by enqueueRun
    store.recordSubmittedId("run-dup-1")
    val dupRun = RunRecord(
      id = "run-dup-1",
      sectorName = "Sector Zero",
      wavesCleared = 7,
      enemiesKilled = 40,
      totalDamage = 9000f,
      scrapEarned = 350,
      mvpTower = "Vulcan Minigun",
      isVictory = false,
      timestamp = System.currentTimeMillis(),
      isDevTuned = false
    )
    repo.enqueueRun(dupRun, "Ace", isDevTuned = false, userId = "u1")
    assertTrue("Already submitted run must not be re-enqueued", store.getPendingQueue().isEmpty())
    assertEquals(RunSubmissionState.SUBMITTED, repo.boardState.value.lastSubmissionState)

    // Stale events for other runIds or when lastRunId is null must not overwrite current run submission state
    repo.resetRunSubmissionState()
    assertNull(repo.boardState.value.lastRunId)
    assertEquals(RunSubmissionState.NONE, repo.boardState.value.lastSubmissionState)

    LeaderboardRepository.notifySubmissionComplete("stale-run-id")
    delay(100)
    assertEquals("Completed event for stale runId must not change NONE state when lastRunId is null", RunSubmissionState.NONE, repo.boardState.value.lastSubmissionState)

    val currentRun = dupRun.copy(id = "run-current-2")
    repo.enqueueRun(currentRun, "Ace", isDevTuned = false, userId = "u1")
    assertEquals(RunSubmissionState.PENDING, repo.boardState.value.lastSubmissionState)

    LeaderboardRepository.notifySubmissionFailed("stale-run-id", FailureKind.SERVER_ERROR, "Stale error", dropped = true)
    delay(100)
    assertEquals(RunSubmissionState.PENDING, repo.boardState.value.lastSubmissionState)
    assertNull(repo.boardState.value.lastSubmissionError)

    LeaderboardRepository.notifySubmissionComplete("run-current-2")
    delay(100)
    assertEquals(RunSubmissionState.SUBMITTED, repo.boardState.value.lastSubmissionState)
  }

  @Test
  fun `FirestoreLeaderboardApi does not failover on API_KEY_INVALID API_KEY_ANDROID_APP_BLOCKED or SERVICE_DISABLED`() = runBlocking {
    val errorBodies = listOf(
      400 to """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}""",
      403 to """{"error":{"code":403,"message":"Requests from this Android client application are blocked.","status":"PERMISSION_DENIED","details":[{"reason":"API_KEY_ANDROID_APP_BLOCKED"}]}}""",
      403 to """{"error":{"code":403,"message":"Cloud Firestore API has not been used in project before or it is disabled.","status":"PERMISSION_DENIED","details":[{"reason":"SERVICE_DISABLED"}]}}"""
    )

    val item = PendingSubmission(
      runId = "run-nofailover",
      callsign = "Fox",
      score = 1000,
      wavesCleared = 5,
      scrapEarned = 100,
      totalDamage = 2000L,
      timestampMs = 1000L,
      clientVersion = "0.0.1.026"
    )

    for ((code, body) in errorBodies) {
      var requestCount = 0
      val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
          requestCount++
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Error")
            .body(body.toResponseBody(null))
            .build()
        }
        .build()

      val api = FirestoreLeaderboardApi(client = client, projectId = "test-project", apiKey = "bad-key")

      val submitRes = api.submitScore(item)
      assertTrue(submitRes is LeaderboardResult.Failure)
      assertEquals(FailureKind.UNCONFIGURED, (submitRes as LeaderboardResult.Failure).kind)
      assertEquals("submitScore must not failover to alternateCollection for $body", 1, requestCount)

      requestCount = 0
      val fetchRes = api.fetchTopScores(10)
      assertTrue(fetchRes is LeaderboardResult.Failure)
      assertEquals(FailureKind.UNCONFIGURED, (fetchRes as LeaderboardResult.Failure).kind)
      assertEquals("fetchTopScores must not failover to alternateCollection for $body", 1, requestCount)
    }
  }

  @Test
  fun `FirestoreLeaderboardApi resolveSigningCertSha1 prefers apkContentsSigners over signingCertificateHistory`() {
    val activeSig = Signature("01020304")
    val rotatedHistorySig = Signature("0a0b0c0d")
    val signingInfo = SigningInfo()
    Shadows.shadowOf(signingInfo).apply {
      setSignatures(arrayOf(activeSig))
      setPastSigningCertificates(arrayOf(rotatedHistorySig, activeSig))
    }
    assertEquals(1, signingInfo.apkContentsSigners?.size)
    Shadows.shadowOf(context.packageManager)
      .getInternalMutablePackageInfo(context.packageName)
      .signingInfo = signingInfo

    val expectedDigest = java.security.MessageDigest.getInstance("SHA-1")
      .digest(activeSig.toByteArray())
      .joinToString("") { "%02x".format(it) }
    val actualDigest = FirestoreLeaderboardApi.resolveSigningCertSha1(context)
    assertEquals(expectedDigest, actualDigest)
  }

  @Test
  fun `LeaderboardRepository fetchLeaderboard sends Bearer token and getCachedIdTokenIfValid respects native user`() = runBlocking {
    val expiry = System.currentTimeMillis() + 600_000L
    context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE).edit()
      .putString("cached_auth_uid", "rest_uid_11")
      .putString("cached_rest_id_token", "rest_bearer_token_11")
      .putString("cached_rest_refresh_token", "rest_refresh_11")
      .putLong("cached_rest_token_expiry_ms", expiry)
      .commit()

    var capturedAuthHeader: String? = null
    val mockClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        capturedAuthHeader = chain.request().header("Authorization")
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body("[]".toResponseBody(null))
          .build()
      }
      .build()

    val authRepo = AuthRepository(context, auth = null, httpClient = mockClient, apiKey = "k")
    val api = FirestoreLeaderboardApi(client = mockClient, projectId = "test-project", apiKey = "k")
    val repo = LeaderboardRepository(api = api, store = LeaderboardStore(context), scheduler = LeaderboardWorkScheduler(context), authRepo = authRepo)

    repo.fetchLeaderboard(forceRefresh = true)
    assertEquals("Bearer rest_bearer_token_11", capturedAuthHeader)

    // When native FirebaseAuth has an active currentUser, getCachedIdTokenIfValid must return null
    val hadFirebaseApp = FirebaseApp.getApps(context).isNotEmpty()
    val firebaseApp = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(
      context,
      FirebaseOptions.Builder()
        .setProjectId("test-isolated-project")
        .setApiKey("AIzaSy" + "0123456789".repeat(3) + "abc")
        .setApplicationId("1:1234567890:android:abcdef0123456789")
        .build()
    )
    val firebaseAuth = FirebaseAuth.getInstance(firebaseApp)
    val userField = FirebaseAuth::class.java.declaredFields.first { it.type == FirebaseUser::class.java }
    userField.isAccessible = true
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val unsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
    val unsafe = unsafeField.get(null)
    val allocateMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
    val defaultUserClass = (('a'..'z').asSequence().map { "zz$it" } +
      ('a'..'z').asSequence().flatMap { first -> ('a'..'z').asSequence().map { second -> "zz$first$second" } })
      .mapNotNull { name ->
        runCatching { Class.forName("com.google.firebase.auth.internal.$name") }.getOrNull()
      }
      .first { FirebaseUser::class.java.isAssignableFrom(it) && !Modifier.isAbstract(it.modifiers) }
    val fakeUser = allocateMethod.invoke(unsafe, defaultUserClass) as FirebaseUser
    val originalUser = userField.get(firebaseAuth)
    val nativeAuthRepo = AuthRepository(context, auth = firebaseAuth, httpClient = mockClient, apiKey = "k")
    try {
      userField.set(firebaseAuth, fakeUser)
      assertNull(
        "getCachedIdTokenIfValid must not return REST token while native FirebaseUser is signed in",
        nativeAuthRepo.getCachedIdTokenIfValid()
      )
    } finally {
      userField.set(firebaseAuth, originalUser)
      if (!hadFirebaseApp) {
        FirebaseApp.clearInstancesForTest()
      }
    }
  }

  @Test
  fun `RogueTDViewModel setPlayerName updates pending callsign after run completion including short sanitized names`() = runBlocking {
    val app = context.applicationContext as Application
    val okClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val url = chain.request().url.toString()
        val body = if (url.contains("identitytoolkit")) {
          """{"localId":"vm_uid_1","idToken":"vm_tok_1","refreshToken":"vm_ref_1","expiresIn":"3600"}"""
        } else {
          "[]"
        }
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body(body.toResponseBody(null))
          .build()
      }
      .build()
    val authRepo = AuthRepository(context, auth = null, httpClient = okClient, apiKey = "k")
    val store = LeaderboardStore(context)
    val scheduler = LeaderboardWorkScheduler(context)
    val api = FirestoreLeaderboardApi(client = okClient, projectId = "test-project", apiKey = "k")
    val repo = LeaderboardRepository(api = api, store = store, scheduler = scheduler, authRepo = authRepo)

    val vmStore = androidx.lifecycle.ViewModelStore()
    val vm = androidx.lifecycle.ViewModelProvider(
      vmStore,
      object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
          RogueTDViewModel(
            application = app,
            authRepository = authRepo,
            remoteConfigManager = RemoteConfigManager(null),
            leaderboardRepository = repo
          ) as T
      }
    )[RogueTDViewModel::class.java]
    try {
      // Simulate a wave completed and end run confirmed
      vm._uiState.value = vm._uiState.value.copy(waveNumber = 3, isWaveActive = false, totalKills = 12, totalDamageDealt = 1500f)
      vm.confirmEndRun()
      vm.setPlayerName("Ax")
      val deadline1 = System.currentTimeMillis() + 5_000L
      while (store.getPendingQueue().isEmpty() && System.currentTimeMillis() < deadline1) {
        delay(25)
      }

      val queueAfterShort = store.getPendingQueue()
      assertEquals(1, queueAfterShort.size)
      assertEquals("Commander", queueAfterShort.first().callsign)

      vm.setPlayerName("Viper")
      val deadline2 = System.currentTimeMillis() + 5_000L
      while (store.getPendingQueue().firstOrNull()?.callsign != "Viper" && System.currentTimeMillis() < deadline2) {
        delay(25)
      }
      assertEquals("Viper", store.getPendingQueue().first().callsign)

      vm.startNewRun()
      assertEquals(RunSubmissionState.NONE, vm.uiState.value.runSubmissionState)
    } finally {
      vmStore.clear()
    }
  }

  @Test
  fun `FirestoreLeaderboardApi sanitizeCallsign never falls below 3 chars after space truncation`() {
    assertEquals("Commander", FirestoreLeaderboardApi.sanitizeCallsign("A               B"))
    assertEquals("Commander", FirestoreLeaderboardApi.sanitizeCallsign("AB              XYZ"))
    val longValid = FirestoreLeaderboardApi.sanitizeCallsign("  Alpha_Bravo_12345  ")
    assertTrue(longValid.length in 3..16)
  }

  @Test
  fun `AuthRepository serializes concurrent REST authentication calls`() = runBlocking {
    var signUpCalls = 0
    val mockClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        if (chain.request().url.toString().contains("identitytoolkit")) {
          signUpCalls++
          Thread.sleep(60)
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"localId":"serial_uid_1","idToken":"serial_tok_1","refreshToken":"serial_ref_1","expiresIn":"3600"}""".toResponseBody(null))
            .build()
        } else {
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("[]".toResponseBody(null))
            .build()
        }
      }
      .build()

    val authRepo = AuthRepository(context = context, auth = null, httpClient = mockClient, apiKey = "k")
    val (uid, token) = coroutineScope {
      val d1 = async(Dispatchers.IO) { authRepo.ensureAuthenticated() }
      val d2 = async(Dispatchers.IO) { authRepo.getIdToken() }
      d1.await() to d2.await()
    }

    assertEquals("serial_uid_1", uid)
    assertEquals("serial_tok_1", token)
    assertEquals("Concurrent ensureAuthenticated and getIdToken must mint only 1 anonymous account", 1, signUpCalls)
  }

  @Test
  fun `LeaderboardRepository enqueueRun with isCurrentRun false queues without clobbering active run state`() = runBlocking {
    val api = FirestoreLeaderboardApi(projectId = "test-project", apiKey = "k")
    val store = LeaderboardStore(context)
    val scheduler = LeaderboardWorkScheduler(context)
    val repo = LeaderboardRepository(api = api, store = store, scheduler = scheduler)
    repo.initialize()
    repo.resetRunSubmissionState()

    val staleRun = RunRecord(
      id = "stale-run-99",
      sectorName = "Sector Zero",
      wavesCleared = 5,
      enemiesKilled = 40,
      totalDamage = 8000f,
      scrapEarned = 300,
      mvpTower = "Vulcan Minigun",
      isVictory = false,
      timestamp = System.currentTimeMillis(),
      isDevTuned = false
    )
    repo.enqueueRun(staleRun, "Ace", isDevTuned = false, userId = "u1", isCurrentRun = false)

    assertEquals(1, store.getPendingQueue().size)
    assertNull(repo.boardState.value.lastRunId)
    assertEquals(RunSubmissionState.NONE, repo.boardState.value.lastSubmissionState)
  }

  @Test
  fun `RogueTDViewModel confirmEndRun on pre-wave-1 zero activity does not record phantom score`() = runBlocking {
    val app = context.applicationContext as Application
    val authRepo = AuthRepository(context, auth = null, apiKey = "")
    val store = LeaderboardStore(context)
    val scheduler = LeaderboardWorkScheduler(context)
    val api = FirestoreLeaderboardApi(projectId = "", apiKey = "")
    val repo = LeaderboardRepository(api = api, store = store, scheduler = scheduler, authRepo = authRepo)

    val vmStore = androidx.lifecycle.ViewModelStore()
    val vm = androidx.lifecycle.ViewModelProvider(
      vmStore,
      object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
          RogueTDViewModel(
            application = app,
            authRepository = authRepo,
            remoteConfigManager = RemoteConfigManager(null),
            leaderboardRepository = repo
          ) as T
      }
    )[RogueTDViewModel::class.java]
    try {
      vm.confirmEndRun()
      delay(150)

      assertTrue("Zero-activity pre-wave-1 end run must not enqueue a score", store.getPendingQueue().isEmpty())
      assertNull(vm.uiState.value.lastRunRecord)
    } finally {
      vmStore.clear()
    }
  }

  @Test
  fun `AuthRepository serializes concurrent REST authentication across separate instances`() = runBlocking {
    var signUpCalls = 0
    val mockClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        if (chain.request().url.toString().contains("identitytoolkit")) {
          signUpCalls++
          Thread.sleep(60)
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"localId":"shared_uid_1","idToken":"shared_tok_1","refreshToken":"shared_ref_1","expiresIn":"3600"}""".toResponseBody(null))
            .build()
        } else {
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("[]".toResponseBody(null))
            .build()
        }
      }
      .build()

    val repoA = AuthRepository(context = context, auth = null, httpClient = mockClient, apiKey = "k")
    val repoB = AuthRepository(context = context, auth = null, httpClient = mockClient, apiKey = "k")

    val (uidA, tokenB) = coroutineScope {
      val d1 = async(Dispatchers.IO) { repoA.ensureAuthenticated() }
      val d2 = async(Dispatchers.IO) { repoB.getIdToken() }
      d1.await() to d2.await()
    }

    assertEquals("shared_uid_1", uidA)
    assertEquals("shared_tok_1", tokenB)
    assertEquals("shared_uid_1", repoB.currentUserId)
    assertEquals("Separate AuthRepository instances must share lock and mint only 1 anonymous account", 1, signUpCalls)
  }

  @Test
  fun `ScoreSubmissionWorker surfaces specific Identity Toolkit UNCONFIGURED and NETWORK errors instead of generic no-token`() = runBlocking {
    val store = LeaderboardStore(context)
    val api = FirestoreLeaderboardApi(projectId = "test-project", apiKey = "test-key")

    // 1. ADMIN_ONLY_OPERATION (Anonymous Auth disabled in Console) -> UNCONFIGURED + Result.failure()
    store.enqueuePending(
      PendingSubmission(
        runId = "run-auth-disabled",
        callsign = "Pilot",
        score = 2000,
        wavesCleared = 5,
        scrapEarned = 120,
        totalDamage = 3000L,
        timestampMs = System.currentTimeMillis(),
        clientVersion = "0.0.1.028"
      )
    )
    val adminOnlyClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(400)
          .message("Bad Request")
          .body("""{"error":{"code":400,"message":"ADMIN_ONLY_OPERATION"}}""".toResponseBody(null))
          .build()
      }
      .build()
    val events = mutableListOf<LeaderboardRepository.SubmissionEvent>()
    val collectJob = launch { LeaderboardRepository.syncEventFlow.collect { events.add(it) } }
    delay(50)

    val authDisabled = AuthRepository(context, auth = null, httpClient = adminOnlyClient, apiKey = "test-key")
    val res1 = buildWorker(api, store, authDisabled).doWork()
    assertTrue("UNCONFIGURED auth failure must fail fast", res1 is ListenableWorker.Result.Failure)
    delay(80)
    val ev1 = events.filterIsInstance<LeaderboardRepository.SubmissionEvent.Failed>()
      .lastOrNull { it.runId == "run-auth-disabled" }
    assertNotNull(ev1)
    assertEquals(FailureKind.UNCONFIGURED, ev1!!.kind)
    assertTrue(ev1.message.contains("Anonymous Auth is disabled", ignoreCase = true))

    // 2. UnknownHostException (offline) -> NETWORK_UNAVAILABLE + Result.retry()
    val offlineClient = OkHttpClient.Builder()
      .addInterceptor { throw UnknownHostException("offline") }
      .build()
    val authOffline = AuthRepository(context, auth = null, httpClient = offlineClient, apiKey = "test-key")
    val res2 = buildWorker(api, store, authOffline).doWork()
    assertTrue("Offline auth failure must request WorkManager retry", res2 is ListenableWorker.Result.Retry)
    delay(80)
    collectJob.cancel()

    val ev2 = events.filterIsInstance<LeaderboardRepository.SubmissionEvent.Failed>()
      .lastOrNull { it.runId == "run-auth-disabled" }
    assertNotNull(ev2)
    assertEquals(FailureKind.NETWORK_UNAVAILABLE, ev2!!.kind)
    assertTrue(ev2.message.contains("Network unavailable", ignoreCase = true))
  }

  @Test
  fun `AuthRepository recovers via REST Identity Toolkit when stale native user fails non-transiently and preserves native user when offline`() = runBlocking {
    val mockClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body("""{"localId":"recovered_rest_uid","idToken":"recovered_rest_tok","refreshToken":"recovered_ref","expiresIn":"3600"}""".toResponseBody(null))
          .build()
      }
      .build()

    // 1. Stale native user fails non-transiently -> signs out, retries native sign-in (also fails), then recovers via REST cleanly
    var signedOut = false
    val staleNativeClient = object : AuthRepository.NativeAuthClient {
      override val hasCurrentUser: Boolean get() = !signedOut
      override val currentUid: String? get() = if (signedOut) null else "stale_native_uid"
      override fun getIdToken(forceRefresh: Boolean): String? {
        throw com.google.firebase.auth.FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "User deleted")
      }
      override fun signInAnonymously(): String? {
        throw IllegalStateException("GMS unavailable")
      }
      override fun signOut() {
        signedOut = true
      }
    }

    val authRepo = AuthRepository(
      context = context,
      nativeClient = staleNativeClient,
      httpClient = mockClient,
      apiKey = "test-key"
    )
    val uid = authRepo.ensureAuthenticated()
    val token = authRepo.getIdToken()
    assertTrue("Stale native user must be signed out before REST recovery", signedOut)
    assertEquals("recovered_rest_uid", uid)
    assertEquals("recovered_rest_tok", token)
    assertEquals("recovered_rest_uid", authRepo.currentUserId)
    assertNull(authRepo.lastAuthFailure)

    // 2. Active native user failing with network error -> preserves native UID and does NOT mint conflicting REST user
    var offlineSignedOut = false
    var restCalls = 0
    val countingClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        restCalls++
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body("""{"localId":"unexpected_rest_uid","idToken":"unexpected_tok","refreshToken":"r","expiresIn":"3600"}""".toResponseBody(null))
          .build()
      }
      .build()
    val offlineNativeClient = object : AuthRepository.NativeAuthClient {
      override val hasCurrentUser: Boolean get() = !offlineSignedOut
      override val currentUid: String? get() = if (offlineSignedOut) null else "valid_native_uid"
      override fun getIdToken(forceRefresh: Boolean): String? {
        throw java.util.concurrent.ExecutionException(UnknownHostException("offline"))
      }
      override fun signInAnonymously(): String? = null
      override fun signOut() {
        offlineSignedOut = true
      }
    }

    val offlineAuthRepo = AuthRepository(
      context = context,
      nativeClient = offlineNativeClient,
      httpClient = countingClient,
      apiKey = "test-key"
    )
    assertEquals("valid_native_uid", offlineAuthRepo.ensureAuthenticated())
    assertNull(offlineAuthRepo.getIdToken())
    assertFalse("Offline error must NOT sign out valid native user", offlineSignedOut)
    assertEquals("Offline native getIdToken failure must NOT mint conflicting REST UID", 0, restCalls)
    assertEquals(FailureKind.NETWORK_UNAVAILABLE, offlineAuthRepo.lastAuthFailure?.kind)
  }

  private fun buildWorker(
    api: FirestoreLeaderboardApi,
    store: LeaderboardStore,
    authRepo: AuthRepository,
    attemptCount: Int = 0
  ): ScoreSubmissionWorker {
    val factory = object : WorkerFactory() {
      override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
      ): ListenableWorker = ScoreSubmissionWorker(appContext, workerParameters, api, store, authRepo)
    }
    return TestListenableWorkerBuilder<ScoreSubmissionWorker>(context)
      .setWorkerFactory(factory)
      .setRunAttemptCount(attemptCount)
      .build()
  }
}
