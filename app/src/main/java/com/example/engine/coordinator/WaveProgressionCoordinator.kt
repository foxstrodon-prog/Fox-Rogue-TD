package com.example.engine.coordinator

import com.example.engine.EnemySpawnEntry
import com.example.engine.RemoteConfigManager
import com.example.engine.TelemetryManager
import com.example.engine.WaveGenerator
import com.example.model.*
import com.example.ui.GameUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

/**
 * Domain coordinator responsible for dual-lane spawn timers, queue popping, boss multiplier
 * calculations, wave deployment, and wave completion reward payouts (Task D-01.iii).
 */
class WaveProgressionCoordinator(
    private val uiStateFlow: MutableStateFlow<GameUIState>,
    private val remoteConfigManager: RemoteConfigManager,
    private val telemetryManager: TelemetryManager,
    private val nextId: (String) -> String,
    private val addFloatingText: (Float, Float, String, Long, Boolean) -> Unit,
    private val onRecordRunCompletion: (Boolean, Int) -> Unit
) {
    private val pendingSpawns = ArrayList<EnemySpawnEntry>(64)
    private var nextSpawnTimeMs: Long = 0L
    var waveHasStartedSpawning: Boolean = false
        private set

    fun clearPendingSpawns() {
        pendingSpawns.clear()
        waveHasStartedSpawning = false
        nextSpawnTimeMs = 0L
    }

    fun hasPendingSpawns(): Boolean = pendingSpawns.isNotEmpty()

    /**
     * Deploys the next wave and initializes dual-lane spawn queues.
     */
    fun deployWave() {
        val state = uiStateFlow.value
        if (state.isWaveActive || state.isGameOver) return

        telemetryManager.logWaveStart(state.waveNumber)

        val waveData = WaveGenerator.generateWave(
            state.waveNumber,
            state.balanceConfig.enemiesPerRoundMultiplier,
            state.difficulty
        )
        pendingSpawns.clear()
        pendingSpawns.addAll(waveData.spawnList)
        nextSpawnTimeMs = 300L
        waveHasStartedSpawning = false

        uiStateFlow.update {
            it.copy(
                isWaveActive = true,
                currentWaveData = waveData
            )
        }
    }

    /**
     * Ticks dual-lane spawn timer and appends ready enemies to [targetEnemies] in-place without intermediate lists.
     */
    fun tickWaveSpawning(
        state: GameUIState,
        level: GeneratedLevel,
        deltaMs: Long,
        targetEnemies: MutableList<EnemyInstance>
    ) {
        if (!state.isWaveActive || pendingSpawns.isEmpty()) return

        nextSpawnTimeMs -= deltaMs
        if (nextSpawnTimeMs <= 0L) {
            val spawn = pendingSpawns.removeAt(0)
            waveHasStartedSpawning = true

            val path = if (spawn.laneIndex == 0) level.laneAlpha else level.laneBeta
            val start = path.first()
            val customEnemyBal = state.balanceConfig.getEnemyBalance(spawn.type)
            val bossMul = if (spawn.isBoss) remoteConfigManager.bossHealthMultiplier else 1.0f
            val hpMultiplier = spawn.hpScale * state.balanceConfig.enemyHpMultiplier * remoteConfigManager.waveHpMultiplier * bossMul
            val effectiveMaxHp = customEnemyBal.hp * hpMultiplier

            val newEnemy = EnemyInstance(
                id = nextId("en_"),
                type = spawn.type,
                laneIndex = spawn.laneIndex,
                currentPathSegmentIndex = 0,
                segmentProgress = 0f,
                maxHp = effectiveMaxHp,
                currentHp = effectiveMaxHp,
                speed = customEnemyBal.speed * spawn.speedScale,
                worldX = start.x.toFloat(),
                worldY = start.y.toFloat(),
                isBoss = spawn.isBoss
            )
            targetEnemies.add(newEnemy)

            nextSpawnTimeMs = if (pendingSpawns.isNotEmpty()) pendingSpawns[0].delayMs else 0L
        }
    }

    /**
     * Evaluates wave completion, calculates scrap completion payouts and tower bonuses, and advances the wave counter.
     */
    fun evaluateWaveCompletion(
        state: GameUIState,
        level: GeneratedLevel,
        towers: List<TowerInstance>,
        remainingAlive: List<EnemyInstance>,
        currentCoreHp: Int
    ): WaveCompletionEvaluation {
        var waveActive = state.isWaveActive
        var currentWave = state.waveNumber
        var isVictory = state.isVictory
        var bonusScrap = 0
        var updatedCoreHp = currentCoreHp

        if (waveActive && waveHasStartedSpawning && pendingSpawns.isEmpty() && remainingAlive.isEmpty()) {
            waveActive = false
            waveHasStartedSpawning = false

            var completionReward = ((state.currentWaveData?.completionBonusScrap ?: 100) *
                state.balanceConfig.waveBonusMultiplier *
                state.balanceConfig.globalIncomeMultiplier).roundToInt().coerceAtLeast(10)

            var hasMortarCc = false
            var hasLaserCc = false
            var hasGatlingCc = false
            for (i in towers.indices) {
                val t = towers[i]
                if (t.isFinalForm && t.finalFormCode == "C.c") {
                    when (t.type) {
                        TowerType.MORTAR -> hasMortarCc = true
                        TowerType.LASER -> hasLaserCc = true
                        TowerType.GATLING -> hasGatlingCc = true
                        else -> {}
                    }
                }
            }

            if (hasMortarCc) completionReward = (completionReward * 1.4f).toInt()
            if (hasLaserCc) completionReward += 50
            if (hasGatlingCc) updatedCoreHp = minOf(state.maxCoreHp, updatedCoreHp + 1)

            bonusScrap += completionReward
            telemetryManager.logWaveComplete(currentWave, completionReward)

            val corePos = level.tiles.firstOrNull { it.type == TileType.CORE }?.pos ?: GridPos(level.width / 2, level.height - 1)
            val clearMessage = if (currentWave % 5 == 0) "★ Milestone Wave $currentWave Cleared! +$completionReward ★" else "Wave $currentWave Cleared! +$completionReward"
            addFloatingText(corePos.x.toFloat(), corePos.y.toFloat() - 1f, clearMessage, if (currentWave % 5 == 0) 0xFFFFD700 else 0xFF00F0FF, false)

            if (currentWave >= state.difficulty.maxWave) {
                isVictory = true
                onRecordRunCompletion(true, currentWave)
                addFloatingText(corePos.x.toFloat(), corePos.y.toFloat() - 2f, "👑 ${state.difficulty.label} VICTORY ACHIEVED! 👑", 0xFFFFD700, false)
            } else {
                currentWave++
            }
        }

        return WaveCompletionEvaluation(
            isWaveActive = waveActive,
            waveNumber = currentWave,
            isVictory = isVictory,
            bonusScrap = bonusScrap,
            updatedCoreHp = updatedCoreHp
        )
    }

    /**
     * Spawns a custom test formation into the active map lanes (Tessa's Sandbox).
     */
    fun spawnSandboxWave(enemyType: EnemyType, count: Int) {
        val level = uiStateFlow.value.level ?: return
        val baseBal = uiStateFlow.value.balanceConfig.getEnemyBalance(enemyType)
        val newEnemies = ArrayList<EnemyInstance>(count)
        for (i in 0 until count) {
            val laneIndex = i % 2
            val path = if (laneIndex == 0) level.laneAlpha else level.laneBeta
            val start = path.first()
            val newEnemy = EnemyInstance(
                id = nextId("sb_"),
                type = enemyType,
                laneIndex = laneIndex,
                currentPathSegmentIndex = 0,
                segmentProgress = -(i * 0.35f),
                maxHp = baseBal.hp,
                currentHp = baseBal.hp,
                speed = baseBal.speed,
                worldX = start.x.toFloat(),
                worldY = start.y.toFloat(),
                isBoss = (enemyType == EnemyType.BOSS)
            )
            newEnemies.add(newEnemy)
        }
        uiStateFlow.update { current ->
            current.copy(
                enemies = current.enemies + newEnemies,
                isWaveActive = true
            )
        }
    }
}

data class WaveCompletionEvaluation(
    val isWaveActive: Boolean,
    val waveNumber: Int,
    val isVictory: Boolean,
    val bonusScrap: Int,
    val updatedCoreHp: Int
)
