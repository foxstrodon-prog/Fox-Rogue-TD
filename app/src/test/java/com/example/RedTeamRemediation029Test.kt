package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.audio.MusicTrack
import com.example.audio.SoundManager
import com.example.engine.*
import com.example.engine.coordinator.TowerPlacementCoordinator
import com.example.engine.coordinator.WaveProgressionCoordinator
import com.example.engine.network.AuthRepository
import com.example.engine.network.FirestoreLeaderboardApi
import com.example.engine.repository.LeaderboardRepository
import com.example.engine.storage.LeaderboardStore
import com.example.engine.worker.ScoreSubmissionWorker
import com.example.model.*
import com.example.ui.GameUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RedTeamRemediation029Test {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("rogue_td_telemetry", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("rogue_td_player_identity", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun `GameUIState toHudState propagates activeEnemyCount from enemies list`() {
    val enemy1 = EnemyInstance(id = "e1", type = EnemyType.DRONE, laneIndex = 0, maxHp = 100f, currentHp = 100f, speed = 2f)
    val enemy2 = EnemyInstance(id = "e2", type = EnemyType.SOLDIER, laneIndex = 1, maxHp = 200f, currentHp = 150f, speed = 1.5f)
    val state = GameUIState(enemies = listOf(enemy1, enemy2))

    val hudState = state.toHudState()
    assertEquals(2, hudState.activeEnemyCount)
    assertTrue(hudState.enemies.isEmpty())
  }

  @Test
  fun `CombatSimulationEngine returns non-aliased snapshot on zero deltaSec and skips dead enemies`() {
    val engine = CombatSimulationEngine()
    val grid = engine.spatialGrid

    val deadEnemy = EnemyInstance(id = "dead", type = EnemyType.DRONE, laneIndex = 0, maxHp = 100f, currentHp = 0f, speed = 2f, worldX = 3f, worldY = 3f)
    val liveEnemy = EnemyInstance(id = "live", type = EnemyType.DRONE, laneIndex = 0, maxHp = 100f, currentHp = 80f, speed = 2f, worldX = 3f, worldY = 3f)
    grid.rebuild(listOf(deadEnemy, liveEnemy))

    val nearby = ArrayList<EnemyInstance>()
    grid.queryNearby(3f, 3f, 2f, nearby)
    assertEquals(1, nearby.size)
    assertEquals("live", nearby[0].id)

    val input = mutableListOf(
      Projectile(
        id = "p1",
        startX = 1f,
        startY = 1f,
        currentX = 1f,
        currentY = 1f,
        targetX = 5f,
        targetY = 5f,
        speed = 5f,
        damage = 10f
      )
    )
    val snapshot = engine.updateProjectiles(
      projectiles = input,
      survivingEnemies = listOf(deadEnemy, liveEnemy),
      deltaSec = 0f,
      applyDamage = { _, _, _, _, _, _, _ -> },
      addExplosion = { _, _, _, _, _ -> }
    )
    assertNotSame(input, snapshot)
    input.clear()
    assertEquals(1, snapshot.size)
  }

  @Test
  fun `DamageCalculator and EnemyInstance guard against NaN and Infinity propagation`() {
    val result = DamageCalculator.DamageResult()
    DamageCalculator.calculateDamage(
      baseDamage = Float.NaN,
      towerShape = EnemyShape.CIRCLE,
      towerAffinity = ElementAffinity.RED,
      enemyShape = EnemyShape.SQUARE,
      enemyAffinity = ElementAffinity.GREEN,
      enemyArmor = Float.NaN,
      out = result
    )
    assertTrue(result.finalDamage.isFinite())
    assertEquals(1.0f, result.finalDamage, 0.001f)

    val nanEnemy = EnemyInstance(id = "e2", type = EnemyType.DRONE, laneIndex = 0, maxHp = 100f, currentHp = Float.NaN, speed = 2f, slowFactor = Float.NaN, slowTimerMs = 500L)
    assertTrue(nanEnemy.isDead)
    assertTrue(nanEnemy.effectiveSpeed.isFinite())
    assertTrue(nanEnemy.effectiveSpeed > 0f)
  }

  @Test
  fun `WaveGenerator preserves Boss modifier labels on waves 10 and 20 and applies speedScaleMultiplier`() {
    val wave10 = WaveGenerator.generateWave(10, difficulty = GameDifficulty.HARD)
    val wave20 = WaveGenerator.generateWave(20, difficulty = GameDifficulty.HARD)
    val wave1Hard = WaveGenerator.generateWave(1, difficulty = GameDifficulty.HARD)

    assertEquals("[HARD] CRITICAL THREAT: APEX TITAN & ESCORTS", wave10.modifierLabel)
    assertEquals("[HARD] EXTINCTION OVERLORD: TOTAL SIEGE", wave20.modifierLabel)
    assertEquals("CRITICAL THREAT: APEX TITAN & ESCORTS", WaveGenerator.generateWave(10).modifierLabel)
    assertEquals(GameDifficulty.HARD.speedScaleMultiplier, wave1Hard.spawnList.first().speedScale, 0.001f)
  }

  @Test
  fun `WaveProgressionCoordinator deployWave is a no-op after victory`() {
    val level = MapGenerator.generate(seed = 123L)
    val stateFlow = MutableStateFlow(
      GameUIState(
        level = level,
        isVictory = true,
        isWaveActive = false,
        waveNumber = 30
      )
    )
    val coord = WaveProgressionCoordinator(
      uiStateFlow = stateFlow,
      remoteConfigManager = RemoteConfigManager(null),
      telemetryManager = TelemetryManager(context),
      nextId = { "id_$it" },
      addFloatingText = { _, _, _, _, _ -> },
      onRecordRunCompletion = { _, _ -> }
    )

    coord.deployWave()
    assertFalse(stateFlow.value.isWaveActive)
    assertFalse(coord.hasPendingSpawns())
  }

  @Test
  fun `TowerPlacementCoordinator changeSelectedTowerPriority emits immutable copy in towers list and selectedTower`() {
    val soundManager = SoundManager(context)
    val tower = TowerInstance(id = "t1", gridPos = GridPos(2, 2), targetPriority = TargetPriority.FIRST)
    val stateFlow = MutableStateFlow(GameUIState(towers = listOf(tower), selectedTower = tower))
    val placement = TowerPlacementCoordinator(
      uiStateFlow = stateFlow,
      soundManager = soundManager,
      telemetryManager = TelemetryManager(context),
      nextId = { "n_$it" },
      addFloatingText = { _, _, _, _, _ -> },
      onSynergyDirty = {}
    )

    placement.changeSelectedTowerPriority(TargetPriority.STRONGEST)
    assertEquals(TargetPriority.STRONGEST, stateFlow.value.selectedTower?.targetPriority)
    assertEquals(TargetPriority.STRONGEST, stateFlow.value.towers.first().targetPriority)
    assertEquals(TargetPriority.FIRST, tower.targetPriority)
    soundManager.release()
  }

  @Test
  fun `SoundManager updates effective music volume immediately on volume and mute toggles even while paused`() {
    val soundManager = SoundManager(context)
    soundManager.activeMusicTrack = MusicTrack.CYBER_AMBIENT
    soundManager.isPaused = true
    soundManager.setMutedState(false)
    soundManager.setMasterVolumeLevel(0.8f)
    soundManager.setMusicVolumeLevel(0.5f)

    assertEquals(0.20f, soundManager.calculateEffectiveMusicVolume(), 0.001f)
    soundManager.toggleMute()
    assertEquals(0f, soundManager.calculateEffectiveMusicVolume(), 0.001f)
    soundManager.toggleMute()
    assertEquals(0.20f, soundManager.calculateEffectiveMusicVolume(), 0.001f)
    soundManager.release()
  }

  @Test
  fun `LeaderboardStore serializes concurrent enqueuePending across separate instances`() = runBlocking {
    val storeA = LeaderboardStore(context)
    val storeB = LeaderboardStore(context)

    coroutineScope {
      val jobs = (1..8).map { idx ->
        async(Dispatchers.IO) {
          val s = if (idx % 2 == 0) storeA else storeB
          s.enqueuePending(
            PendingSubmission(
              runId = "run_$idx",
              callsign = "Cmdr$idx",
              score = idx * 100,
              wavesCleared = idx,
              scrapEarned = idx * 10,
              totalDamage = idx * 500L,
              timestampMs = System.currentTimeMillis() + idx,
              clientVersion = BuildConfig.VERSION_NAME
            )
          )
        }
      }
      jobs.forEach { it.await() }
    }

    assertEquals(8, storeA.getPendingQueue().size)
  }

  @Test
  fun `AuthRepository preserves cached UID and refresh token across 5xx refresh failures and provides atomic session`() = runBlocking {
    val prefs = context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE)
    prefs.edit()
      .putString("cached_auth_uid", "stable_uid_1")
      .putString("cached_rest_id_token", "expired_tok")
      .putString("cached_rest_refresh_token", "valid_refresh_tok")
      .putLong("cached_rest_token_expiry_ms", 0L)
      .commit()

    var signUpCalls = 0
    val client = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val url = chain.request().url.toString()
        if (url.contains("identitytoolkit")) {
          signUpCalls++
        }
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(503)
          .message("Service Unavailable")
          .body("""{"error":{"code":503,"message":"BACKEND_UNAVAILABLE"}}""".toResponseBody(null))
          .build()
      }
      .build()

    val authRepo = AuthRepository(context = context, auth = null, httpClient = client, apiKey = "k")
    val (uid, token) = authRepo.getAuthenticatedSession()
    assertEquals("stable_uid_1", uid)
    assertNull(token)
    assertEquals("5xx token refresh failure must NOT mint a brand-new anonymous account", 0, signUpCalls)
    assertEquals(FailureKind.SERVER_ERROR, authRepo.lastAuthFailure?.kind)
  }

  @Test
  fun `ScoreSubmissionWorker processes healthy items after transient item failure and avoids security burn on offline refresh`() = runBlocking {
    val store = LeaderboardStore(context)
    val api = FirestoreLeaderboardApi(projectId = "test-project", apiKey = "test-key")

    store.enqueuePending(
      PendingSubmission(
        runId = "run_good",
        callsign = "GoodPilot",
        score = 3000,
        wavesCleared = 6,
        scrapEarned = 200,
        totalDamage = 6000L,
        timestampMs = System.currentTimeMillis(),
        clientVersion = BuildConfig.VERSION_NAME
      )
    )
    store.enqueuePending(
      PendingSubmission(
        runId = "run_flaky",
        callsign = "FlakyPilot",
        score = 2000,
        wavesCleared = 4,
        scrapEarned = 100,
        totalDamage = 3000L,
        timestampMs = System.currentTimeMillis() + 10,
        clientVersion = BuildConfig.VERSION_NAME
      )
    )

    val authPrefs = context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE)
    authPrefs.edit()
      .putString("cached_auth_uid", "worker_uid")
      .putString("cached_rest_id_token", "valid_tok")
      .putString("cached_rest_refresh_token", "ref_tok")
      .putLong("cached_rest_token_expiry_ms", System.currentTimeMillis() + 600_000L)
      .commit()

    val mockClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val url = chain.request().url.toString()
        when {
          url.contains("documentId=run_flaky") -> Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(503)
            .message("Unavailable")
            .body("".toResponseBody(null))
            .build()
          url.contains("documentId=run_good") -> Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody(null))
            .build()
          else -> Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody(null))
            .build()
        }
      }
      .build()

    val mockApi = FirestoreLeaderboardApi(client = mockClient, projectId = "test-project", apiKey = "test-key", context = context)
    val authRepo = AuthRepository(context = context, auth = null, httpClient = mockClient, apiKey = "test-key")
    val factory = object : WorkerFactory() {
      override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
        ScoreSubmissionWorker(appContext, workerParameters, mockApi, store, authRepo)
    }
    val worker = TestListenableWorkerBuilder<ScoreSubmissionWorker>(context)
      .setWorkerFactory(factory)
      .build()

    val result = worker.doWork()
    assertTrue("Worker must retry for remaining transient failure", result is ListenableWorker.Result.Retry)
    val remaining = store.getPendingQueue()
    assertEquals(1, remaining.size)
    assertEquals("run_flaky", remaining[0].runId)

    // Now test 403 + offline refresh does NOT burn attempts toward 5-drop limit
    store.clearPendingQueue()
    store.enqueuePending(
      PendingSubmission(
        runId = "run_sec",
        callsign = "SecPilot",
        score = 1500,
        wavesCleared = 3,
        scrapEarned = 80,
        totalDamage = 2500L,
        timestampMs = System.currentTimeMillis(),
        clientVersion = BuildConfig.VERSION_NAME
      )
    )
    val offlineRefreshClient = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val url = chain.request().url.toString()
        if (url.contains("securetoken")) {
          throw UnknownHostException("offline")
        }
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(403)
          .message("Forbidden")
          .body("""{"error":{"status":"PERMISSION_DENIED","message":"Missing permissions"}}""".toResponseBody(null))
          .build()
      }
      .build()
    val api403 = FirestoreLeaderboardApi(client = offlineRefreshClient, projectId = "test-project", apiKey = "test-key", context = context)
    val authOfflineRefresh = AuthRepository(context = context, auth = null, httpClient = offlineRefreshClient, apiKey = "test-key")
    val factory2 = object : WorkerFactory() {
      override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
        ScoreSubmissionWorker(appContext, workerParameters, api403, store, authOfflineRefresh)
    }
    val res2 = TestListenableWorkerBuilder<ScoreSubmissionWorker>(context)
      .setWorkerFactory(factory2)
      .build()
      .doWork()
    assertTrue(res2 is ListenableWorker.Result.Retry)
    assertEquals(0, store.getPendingQueue().first().attempts)
    assertNotNull(api)
  }

  @Test
  fun `TelemetryManager isolates display seed fallback from persisted tower stats and preserves double damage precision`() {
    val tm = TelemetryManager(context)
    tm.recordRun(
      RunRecord(
        id = "r1",
        seed = 1L,
        sectorName = "Alpha",
        playerName = "Maverick",
        wavesCleared = 50,
        enemiesKilled = 500,
        totalDamage = 1.0e12f,
        scrapEarned = 10000,
        ascendedTowersCount = 2,
        mvpTower = "Stellar",
        isVictory = true,
        timestamp = System.currentTimeMillis()
      )
    )
    assertEquals(1.0e12, tm.getLifetimeTelemetry().totalDamageOutput, 1.0e5)

    val realTower = TowerInstance(id = "t1", gridPos = GridPos(1, 1), towerClass = TowerClass.KINETIC_BALLISTIC, totalDamage = 1234f, kills = 12)
    tm.recordTowerSession(listOf(realTower))
    val perf = tm.getTowerPerformance()
    assertEquals(1, perf.size)
    assertEquals(1234.0, perf[0].totalDamage, 0.01)
  }

  @Test
  fun `AppUpdateManager rejects non-HTTPS APK download URLs`() {
    val updater = AppUpdateManager(context)
    updater.startDownload(
      UpdateInfo(
        isUpdateAvailable = true,
        latestVersion = "0.0.1.030",
        currentVersion = BuildConfig.VERSION_NAME,
        apkUrl = "http://insecure.example.com/update.apk",
        apkFileName = "../../evil.apk"
      )
    )
    assertEquals(UpdateDownloadStatus.FAILED, updater.updateState.value.status)
    updater.unregisterReceiver()
  }
}
