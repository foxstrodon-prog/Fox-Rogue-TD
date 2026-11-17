package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.engine.CombatSimulationEngine
import com.example.engine.MapGenerator
import com.example.engine.RemoteConfigManager
import com.example.engine.TelemetryManager
import com.example.engine.coordinator.SimulationCoordinator
import com.example.engine.coordinator.WaveProgressionCoordinator
import com.example.model.*
import com.example.ui.CinematicController
import com.example.ui.CinematicHost
import com.example.ui.GameUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SpeedScalingInvarianceTest {

    private data class SimHarness(
        val stateFlow: MutableStateFlow<GameUIState>,
        val waveCoordinator: WaveProgressionCoordinator,
        val simCoordinator: SimulationCoordinator,
        val soundManager: SoundManager
    )

    private fun buildHarness(initialState: GameUIState): SimHarness {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val stateFlow = MutableStateFlow(initialState)
        var idCounter = 0L
        val nextId: (String) -> String = { prefix -> "$prefix${idCounter++}" }
        val remoteConfig = RemoteConfigManager(null)
        val telemetry = TelemetryManager(app)
        val sm = SoundManager(app)
        val waveCoordinator = WaveProgressionCoordinator(
            uiStateFlow = stateFlow,
            remoteConfigManager = remoteConfig,
            telemetryManager = telemetry,
            nextId = nextId,
            addFloatingText = { _, _, _, _, _ -> },
            onRecordRunCompletion = { _, _ -> }
        )
        val cinematicHost = object : CinematicHost {
            override val uiStateFlow: MutableStateFlow<GameUIState> get() = stateFlow
            override val soundManager: SoundManager get() = sm
            override fun clearPendingSpawns() = waveCoordinator.clearPendingSpawns()
            override fun markSynergyDirty() {}
            override fun addFloatingText(x: Float, y: Float, text: String, color: Long, isCrit: Boolean) {}
            override fun swapBoosterPads() {}
            override fun deployWave() = waveCoordinator.deployWave()
        }
        val simCoordinator = SimulationCoordinator(
            uiStateFlow = stateFlow,
            combatEngine = CombatSimulationEngine(),
            waveCoordinator = waveCoordinator,
            soundManager = sm,
            remoteConfigManager = remoteConfig,
            cinematicController = CinematicController(cinematicHost),
            nextId = nextId,
            onRecordRunCompletion = { _, _ -> },
            critRandom = Random(42L)
        )
        return SimHarness(stateFlow, waveCoordinator, simCoordinator, sm)
    }

    @Test
    fun `WaveProgressionCoordinator tickWaveSpawning is strictly invariant across 20ms vs 100ms vs 200ms steps`() = runBlocking {
        val level = MapGenerator.generate(seed = 12345L)
        val results = listOf(20L to 100, 100L to 20, 200L to 10).map { (stepMs, count) ->
            val h = buildHarness(GameUIState(level = level, waveNumber = 35, difficulty = GameDifficulty.MEDIUM))
            h.waveCoordinator.deployWave()
            val spawned = mutableListOf<EnemyInstance>()
            repeat(count) {
                h.waveCoordinator.tickWaveSpawning(h.stateFlow.value, level, stepMs, spawned)
            }
            h.soundManager.release()
            h.soundManager.awaitPendingIoForTest()
            spawned.size to spawned.map { it.type to it.laneIndex }
        }

        // At 2000ms total elapsed on Wave 35 (initial 300ms + 250ms first entry + 200ms cadence):
        // All step granularities must spawn the exact same enemy count & sequence
        assertEquals("20ms and 100ms steps must spawn identical enemy count", results[0].first, results[1].first)
        assertEquals("20ms and 200ms steps must spawn identical enemy count", results[0].first, results[2].first)
        assertEquals("20ms and 200ms steps must spawn identical enemy sequence", results[0].second, results[2].second)
        assertTrue("Expected at least 8 enemies spawned in 2000ms of Wave 35", results[0].first >= 8)
    }

    @Test
    fun `1X vs 10X batched fixed-timestep simulation produces identical combat parity on loaded tower board`() = runBlocking {
        val level = MapGenerator.generate(seed = 777L)
        val buildablePads = level.tiles.filter { it.isBuildable }.take(8)
        assertEquals(8, buildablePads.size)

        fun createTowers(): List<TowerInstance> {
            val types = listOf(
                TowerType.GATLING to TowerClass.KINETIC_BALLISTIC,
                TowerType.MORTAR to TowerClass.HEAVY_SIEGE,
                TowerType.CRYO to TowerClass.ELEMENTAL_VOID,
                TowerType.VOID to TowerClass.ELEMENTAL_VOID,
                TowerType.GATLING to TowerClass.KINETIC_BALLISTIC,
                TowerType.TESLA to TowerClass.ELEMENTAL_VOID,
                TowerType.LASER to TowerClass.KINETIC_BALLISTIC,
                TowerType.STELLAR to TowerClass.STELLAR_COSMIC
            )
            return buildablePads.mapIndexed { idx, tile ->
                val (tType, tClass) = types[idx]
                TowerInstance(
                    id = "t_$idx",
                    type = tType,
                    gridPos = tile.pos,
                    towerClass = tClass,
                    tier = 2,
                    lastShotTimeMs = -10_000L
                )
            }
        }

        // 1X cadence: 300 frames * 1 step of 20ms (publishUiState = true every tick)
        val h1x = buildHarness(
            GameUIState(
                level = level,
                waveNumber = 12,
                difficulty = GameDifficulty.HARD,
                towers = createTowers(),
                coreHp = 30,
                maxCoreHp = 30
            )
        )
        h1x.waveCoordinator.deployWave()
        for (i in 1..300) {
            h1x.simCoordinator.tickSimulation(0.020f, 20L, nowMs = i * 20L, publishUiState = true)
        }

        // 10X cadence: 20 frames * 15 batched steps of 20ms (publishUiState = false until final step of frame)
        val h10x = buildHarness(
            GameUIState(
                level = level,
                waveNumber = 12,
                difficulty = GameDifficulty.HARD,
                towers = createTowers(),
                coreHp = 30,
                maxCoreHp = 30
            )
        )
        h10x.waveCoordinator.deployWave()
        repeat(20) { frameIdx ->
            val frameNowMs = (frameIdx + 1) * 300L
            for (sub in 0 until 15) {
                h10x.simCoordinator.tickSimulation(
                    deltaSec = 0.020f,
                    deltaMs = 20L,
                    nowMs = frameNowMs,
                    publishUiState = (sub == 14)
                )
            }
        }

        val s1 = h1x.stateFlow.value
        val s10 = h10x.stateFlow.value
        assertEquals(6000L, h1x.simCoordinator.virtualSimTimeMs)
        assertEquals(6000L, h10x.simCoordinator.virtualSimTimeMs)
        assertEquals("1X and 10X must yield identical kills", s1.totalKills, s10.totalKills)
        assertEquals("1X and 10X must yield identical scrap", s1.scrap, s10.scrap)
        assertEquals("1X and 10X must yield identical core HP", s1.coreHp, s10.coreHp)
        assertEquals("1X and 10X must yield identical surviving enemy count", s1.enemies.size, s10.enemies.size)
        assertEquals("1X and 10X must yield identical total damage", s1.totalDamageDealt, s10.totalDamageDealt, 0.01f)
        assertEquals("1X and 10X must yield identical wave number", s1.waveNumber, s10.waveNumber)

        h1x.soundManager.release(); h1x.soundManager.awaitPendingIoForTest()
        h10x.soundManager.release(); h10x.soundManager.awaitPendingIoForTest()
    }

    @Test
    fun `tickSimulation with publishUiState false holds uiStateFlow emissions until final publish`() = runBlocking {
        val level = MapGenerator.generate(seed = 555L)
        val pad = level.tiles.first { it.isBuildable }
        val tower = TowerInstance(
            id = "t_batch",
            type = TowerType.LASER,
            gridPos = pad.pos,
            towerClass = TowerClass.KINETIC_BALLISTIC,
            tier = 2,
            lastShotTimeMs = -10_000L
        )
        val h = buildHarness(
            GameUIState(
                level = level,
                waveNumber = 1,
                towers = listOf(tower),
                scrap = 500
            )
        )
        h.waveCoordinator.deployWave()

        // 25 unpublished 20ms steps (= 500ms virtual time, enough for Wave 1 first spawn at 300ms)
        repeat(25) { idx ->
            h.simCoordinator.tickSimulation(0.020f, 20L, nowMs = (idx + 1) * 20L, publishUiState = false)
        }
        assertEquals(500L, h.simCoordinator.virtualSimTimeMs)
        assertEquals("uiStateFlow must not publish intermediate batch enemies", 0, h.stateFlow.value.enemies.size)
        assertEquals("uiStateFlow animTimeMs must remain unpublished", 0L, h.stateFlow.value.animTimeMs)

        // Final step publishes accumulated state atomically
        h.simCoordinator.tickSimulation(0.020f, 20L, nowMs = 520L, publishUiState = true)
        assertEquals(520L, h.simCoordinator.virtualSimTimeMs)
        assertEquals(520L, h.stateFlow.value.animTimeMs)
        assertTrue("Published state must contain spawned/engaged wave state", h.stateFlow.value.enemies.isNotEmpty() || h.stateFlow.value.totalKills > 0)

        h.soundManager.release()
        h.soundManager.awaitPendingIoForTest()
    }
}
