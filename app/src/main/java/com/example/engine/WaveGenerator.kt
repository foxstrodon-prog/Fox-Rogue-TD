package com.example.engine

import com.example.model.EnemyType

data class EnemySpawnEntry(
    val type: EnemyType,
    val delayMs: Long,
    val laneIndex: Int = 0, // 0 = Lane Alpha, 1 = Lane Beta
    val hpScale: Float = 1.0f,
    val speedScale: Float = 1.0f,
    val isBoss: Boolean = false
)

data class WaveData(
    val waveNumber: Int,
    val name: String,
    val modifierLabel: String,
    val totalEnemies: Int,
    val spawnList: List<EnemySpawnEntry>,
    val completionBonusScrap: Int,
    val isBossWave: Boolean
)

object WaveGenerator {

    fun generateWave(waveNumber: Int): WaveData {
        val isBossWave = (waveNumber % 10 == 0) || (waveNumber == 5)
        val name = when {
            waveNumber == 5 -> "Sector Vanguard: Dual Elite Scout"
            waveNumber == 10 -> "Sector Apex: Titan Colossus Prime"
            waveNumber == 20 -> "Extinction Event: Dual Overlord Incursion"
            isBossWave -> "Colossus Dual Breach #$waveNumber"
            else -> "Dual Assault Wave #$waveNumber"
        }

        val modifier = when {
            waveNumber == 1 -> "Dual-Front Recon Swarm"
            waveNumber == 2 -> "Pincer Kinetic Probes"
            waveNumber == 3 -> "Flanking Speedster Vanguard"
            waveNumber == 4 -> "Heavy Armor Dual Outriders"
            waveNumber == 5 -> "Warning: Dual Elite Mini-Boss"
            waveNumber % 4 == 0 -> "Heavily Armored Converging Column"
            waveNumber % 3 == 0 -> "Bifurcated High-Speed Blitz"
            waveNumber == 10 -> "CRITICAL THREAT: APEX TITAN & ESCORTS"
            waveNumber == 20 -> "EXTINCTION OVERLORD: TOTAL SIEGE"
            waveNumber > 20 -> "Endless Void Dual-Front Scaling"
            else -> "Hostile Dual-Lane Convergence"
        }

        val spawnList = mutableListOf<EnemySpawnEntry>()
        val baseHpScale = 1.0f + (waveNumber - 1) * 0.20f + if (waveNumber > 10) (waveNumber - 10) * 0.12f else 0f
        val baseSpeedScale = 1.0f + minOf(0.35f, (waveNumber - 1) * 0.02f)

        when (waveNumber) {
            1 -> {
                // Wave 1: 6 Drones on Alpha, 6 Drones on Beta (alternating!)
                for (i in 0 until 12) {
                    val lane = i % 2
                    spawnList.add(EnemySpawnEntry(EnemyType.DRONE, delayMs = if (i == 0) 300L else 750L, laneIndex = lane, hpScale = 0.9f))
                }
            }
            2 -> {
                // Wave 2: Drones and Automatons on both lanes
                for (i in 0 until 8) {
                    spawnList.add(EnemySpawnEntry(EnemyType.DRONE, delayMs = 650L, laneIndex = i % 2, hpScale = baseHpScale))
                }
                for (i in 0 until 6) {
                    spawnList.add(EnemySpawnEntry(EnemyType.SOLDIER, delayMs = 900L, laneIndex = (i + 1) % 2, hpScale = baseHpScale))
                }
            }
            3 -> {
                // Wave 3: Speedsters flank on Beta, Drones on Alpha
                for (i in 0 until 8) {
                    spawnList.add(EnemySpawnEntry(EnemyType.DRONE, delayMs = 500L, laneIndex = 0, hpScale = baseHpScale))
                    spawnList.add(EnemySpawnEntry(EnemyType.SPEEDSTER, delayMs = 600L, laneIndex = 1, hpScale = baseHpScale, speedScale = 1.05f))
                }
            }
            4 -> {
                // Wave 4: Golems leading each lane
                spawnList.add(EnemySpawnEntry(EnemyType.BRUISER, delayMs = 400L, laneIndex = 0, hpScale = baseHpScale))
                spawnList.add(EnemySpawnEntry(EnemyType.BRUISER, delayMs = 500L, laneIndex = 1, hpScale = baseHpScale))
                for (i in 0 until 10) {
                    spawnList.add(EnemySpawnEntry(EnemyType.SOLDIER, delayMs = 700L, laneIndex = i % 2, hpScale = baseHpScale))
                }
            }
            5 -> {
                // Wave 5: Mini-Boss on Alpha, Fast Disruptors on Beta
                spawnList.add(
                    EnemySpawnEntry(
                        EnemyType.BRUISER,
                        delayMs = 800L,
                        laneIndex = 0,
                        hpScale = baseHpScale * 2.8f,
                        isBoss = true
                    )
                )
                for (i in 0 until 8) {
                    spawnList.add(EnemySpawnEntry(EnemyType.SPEEDSTER, delayMs = 650L, laneIndex = 1, hpScale = baseHpScale))
                }
                spawnList.add(
                    EnemySpawnEntry(
                        EnemyType.BRUISER,
                        delayMs = 1200L,
                        laneIndex = 1,
                        hpScale = baseHpScale * 2.5f,
                        isBoss = true
                    )
                )
            }
            10 -> {
                // Boss Wave: Titan Boss on Alpha + Heavy Escorts on Beta
                spawnList.add(
                    EnemySpawnEntry(
                        EnemyType.BOSS,
                        delayMs = 1500L,
                        laneIndex = 0,
                        hpScale = baseHpScale * 1.6f,
                        isBoss = true
                    )
                )
                for (i in 0 until 8) {
                    spawnList.add(EnemySpawnEntry(EnemyType.DISRUPTOR, delayMs = 700L, laneIndex = 1, hpScale = baseHpScale))
                }
                for (i in 0 until 12) {
                    spawnList.add(EnemySpawnEntry(EnemyType.SPEEDSTER, delayMs = 600L, laneIndex = i % 2, hpScale = baseHpScale))
                }
            }
            else -> {
                // Procedural mixed waves attacking both lanes
                val enemyCount = 16 + waveNumber * 3
                val canHaveBruisers = waveNumber >= 4
                val canHaveDisruptors = waveNumber >= 6
                val canHaveBoss = isBossWave

                if (canHaveBoss) {
                    spawnList.add(
                        EnemySpawnEntry(
                            EnemyType.BOSS,
                            delayMs = 1200L,
                            laneIndex = 0,
                            hpScale = baseHpScale * 1.8f,
                            isBoss = true
                        )
                    )
                    spawnList.add(
                        EnemySpawnEntry(
                            EnemyType.BRUISER,
                            delayMs = 1400L,
                            laneIndex = 1,
                            hpScale = baseHpScale * 2.0f,
                            isBoss = true
                        )
                    )
                }

                for (i in 0 until enemyCount) {
                    val roll = (i * 13 + waveNumber * 7) % 100
                    val type = when {
                        roll < 30 -> EnemyType.DRONE
                        roll < 55 -> EnemyType.SOLDIER
                        roll < 75 -> EnemyType.SPEEDSTER
                        roll < 88 && canHaveDisruptors -> EnemyType.DISRUPTOR
                        canHaveBruisers -> EnemyType.BRUISER
                        else -> EnemyType.SOLDIER
                    }

                    val delay = when (type) {
                        EnemyType.DRONE -> 450L
                        EnemyType.SPEEDSTER -> 550L
                        EnemyType.SOLDIER -> 700L
                        EnemyType.DISRUPTOR -> 850L
                        EnemyType.BRUISER -> 1200L
                        EnemyType.BOSS -> 2000L
                    }

                    val lane = (i + (if (i % 3 == 0) 1 else 0)) % 2

                    spawnList.add(
                        EnemySpawnEntry(
                            type = type,
                            delayMs = delay,
                            laneIndex = lane,
                            hpScale = baseHpScale,
                            speedScale = baseSpeedScale
                        )
                    )
                }
            }
        }

        val bonusScrap = 90 + waveNumber * 20 + if (isBossWave) 150 else 0

        return WaveData(
            waveNumber = waveNumber,
            name = name,
            modifierLabel = modifier,
            totalEnemies = spawnList.size,
            spawnList = spawnList,
            completionBonusScrap = bonusScrap,
            isBossWave = isBossWave
        )
    }
}
