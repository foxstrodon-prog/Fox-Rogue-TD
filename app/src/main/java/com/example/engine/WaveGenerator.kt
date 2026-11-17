package com.example.engine

import com.example.model.EnemyRole
import com.example.model.EnemyType
import com.example.model.GameDifficulty

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
    val isBossWave: Boolean,
    /** Which visual-grammar roles (Attack / Defense / Support) appear in this wave. */
    val rolesPresent: Set<EnemyRole> = emptySet()
)

object WaveGenerator {

    /** Share (0..100) of procedural spawn slots reserved for Support-role units at a given wave. */
    private fun supportShare(waveNumber: Int): Int = when {
        waveNumber < 6 -> 0
        waveNumber < 10 -> 8
        waveNumber < 20 -> 12
        else -> 16
    }

    /**
     * Picks the enemy archetype for procedural slot [i] of wave [waveNumber].
     * Visual grammar: Circle swarm / Triangle striker / Square bruiser / Star stellar;
     * Attack (crimson/orange), Defense (cobalt/cyan), Support (emerald/amber).
     */
    private fun pickEnemyType(i: Int, waveNumber: Int): EnemyType {
        val roll = (i * 13 + waveNumber * 7) % 100
        val supportPct = supportShare(waveNumber)
        if (supportPct > 0 && roll >= 100 - supportPct) {
            return when ((i + waveNumber) % 4) {
                0 -> EnemyType.MEDIC
                1 -> EnemyType.RALLIER
                2 -> if (waveNumber >= 12) EnemyType.COSMIC_WEAVER else EnemyType.MEDIC
                else -> if (waveNumber >= 10) EnemyType.DISRUPTOR else EnemyType.MEDIC
            }
        }
        return when {
            roll < 24 -> EnemyType.DRONE
            roll < 42 -> EnemyType.SOLDIER
            roll < 60 -> EnemyType.SPEEDSTER
            roll < 76 && waveNumber >= 6 -> EnemyType.STELLAR_PROBE
            waveNumber >= 15 && roll >= 88 -> EnemyType.ASTRAL_COLOSSUS
            waveNumber >= 4 -> EnemyType.BRUISER
            else -> EnemyType.SOLDIER
        }
    }

    private fun spawnDelayFor(type: EnemyType): Long = when (type) {
        EnemyType.DRONE -> 450L
        EnemyType.SPEEDSTER -> 550L
        EnemyType.SOLDIER -> 700L
        EnemyType.DISRUPTOR -> 850L
        EnemyType.BRUISER -> 1200L
        EnemyType.BOSS -> 2000L
        EnemyType.MEDIC -> 800L
        EnemyType.RALLIER -> 800L
        EnemyType.STELLAR_PROBE -> 600L
        EnemyType.COSMIC_WEAVER -> 900L
        EnemyType.ASTRAL_COLOSSUS -> 1400L
    }

    fun generateWave(
        waveNumber: Int,
        enemyCountMultiplier: Float = 1.0f,
        difficulty: GameDifficulty = GameDifficulty.MEDIUM
    ): WaveData {
        val countMul = enemyCountMultiplier * difficulty.enemyCountMultiplier
        val isBossWave = (waveNumber % 10 == 0) || (waveNumber == 5) || (waveNumber == 45) || (waveNumber == 50) || (waveNumber == 52)
        val name = when {
            waveNumber == 5 -> "Sector Vanguard: Dual Elite Scout"
            waveNumber == 10 -> "Sector Apex: Titan Colossus Prime"
            waveNumber == 20 -> "Extinction Event: Dual Overlord Incursion"
            waveNumber == 35 -> "Signature Milestone: Dual-Front Bullet Hell"
            waveNumber == 40 -> "Signature Milestone: Booster Warp Incursion"
            waveNumber == 42 -> "Signature Milestone: Prismatic Void Shatter"
            waveNumber == 45 -> "Signature Milestone: Kinetic Vulcan Overclock"
            waveNumber == 50 -> "Apex Milestone: Astral Colossus Annihilation"
            waveNumber == 52 -> "Ascension Gauntlet: Total Evolution Siege"
            isBossWave -> "Colossus Dual Breach #$waveNumber"
            else -> "Dual Assault Wave #$waveNumber"
        }

        val modifier = when {
            waveNumber == 1 -> "Dual-Front Recon Swarm"
            waveNumber == 2 -> "Pincer Kinetic Probes"
            waveNumber == 3 -> "Flanking Speedster Vanguard"
            waveNumber == 4 -> "Heavy Armor Dual Outriders"
            waveNumber == 5 -> "Warning: Dual Elite Mini-Boss"
            waveNumber == 35 -> "Twin-Lane Full-Screen Firestorm (Cinematic 2)"
            waveNumber == 40 -> "Instant Dual-Front Tactical Swaps (Cinematic 3)"
            waveNumber == 42 -> "Cryo Freeze & Singularity Collapse (Cinematic 4)"
            waveNumber == 45 -> "Hyper-Velocity Ballistic Shred (Cinematic 5)"
            waveNumber == 50 -> "Apex Stellar Star vs Void Colossus (Cinematic 1)"
            waveNumber == 52 -> "Tier 1 to Apex Final Forms Gauntlet (Cinematic 6)"
            waveNumber == 10 -> "CRITICAL THREAT: APEX TITAN & ESCORTS"
            waveNumber == 20 -> "EXTINCTION OVERLORD: TOTAL SIEGE"
            waveNumber % 4 == 0 -> "Heavily Armored Converging Column"
            waveNumber % 3 == 0 -> "Bifurcated High-Speed Blitz"
            waveNumber > 20 -> "Endless Void Dual-Front Scaling"
            else -> "Hostile Dual-Lane Convergence"
        }

        val spawnList = mutableListOf<EnemySpawnEntry>()
        val baseHpScale: Float
        val baseSpeedScale: Float

        when (difficulty) {
            GameDifficulty.EASY -> {
                val post12 = if (waveNumber > 12) (waveNumber - 12) * 0.16f else 0f
                val post20Exp = if (waveNumber > 20) Math.pow(1.055, (waveNumber - 20).toDouble()).toFloat() else 1.0f
                baseHpScale = ((1.0f + (waveNumber - 1) * 0.20f + post12) * post20Exp) * difficulty.hpScaleMultiplier
                baseSpeedScale = (1.0f + minOf(0.35f, (waveNumber - 1) * 0.018f)) * difficulty.speedScaleMultiplier
            }
            GameDifficulty.MEDIUM -> {
                val post10 = if (waveNumber > 10) (waveNumber - 10) * 0.22f else 0f
                val post22Exp = if (waveNumber > 22) Math.pow(1.06, (waveNumber - 22).toDouble()).toFloat() else 1.0f
                baseHpScale = ((1.0f + (waveNumber - 1) * 0.24f + post10) * post22Exp) * difficulty.hpScaleMultiplier
                baseSpeedScale = (1.0f + minOf(0.45f, (waveNumber - 1) * 0.022f)) * difficulty.speedScaleMultiplier
            }
            GameDifficulty.HARD -> {
                val pre30Base = 1.0f + (waveNumber - 1) * 0.28f + if (waveNumber > 8) (waveNumber - 8) * 0.24f else 0f
                val midExp = if (waveNumber > 16) Math.pow(1.04, minOf(waveNumber - 16, 14).toDouble()).toFloat() else 1.0f
                if (waveNumber <= 30) {
                    baseHpScale = pre30Base * midExp * difficulty.hpScaleMultiplier
                } else {
                    val w30Value = (1.0f + 29 * 0.28f + 22 * 0.24f) * Math.pow(1.04, 14.0).toFloat() * difficulty.hpScaleMultiplier
                    val post30 = (waveNumber - 30).toDouble()
                    val expCurve = Math.pow(1.065, post30).toFloat()
                    baseHpScale = w30Value * expCurve
                }
                baseSpeedScale = (1.0f + minOf(0.55f, (waveNumber - 1) * 0.028f)) * difficulty.speedScaleMultiplier
            }
            GameDifficulty.INFINITE -> {
                val w50Base = 1.0f + 49 * 0.24f + 40 * 0.20f
                if (waveNumber <= 50) {
                    baseHpScale = (1.0f + (waveNumber - 1) * 0.24f + if (waveNumber > 10) (waveNumber - 10) * 0.20f else 0f) * difficulty.hpScaleMultiplier
                    baseSpeedScale = (1.0f + minOf(0.45f, (waveNumber - 1) * 0.022f)) * difficulty.speedScaleMultiplier
                } else {
                    val post50 = (waveNumber - 50).toDouble()
                    val safePost50 = minOf(post50, 300.0)
                    val expCurve = Math.pow(1.08, safePost50).toFloat()
                    baseHpScale = minOf(1.0e12f, (w50Base * difficulty.hpScaleMultiplier) * expCurve)
                    baseSpeedScale = minOf(2.2f, (1.45f + (waveNumber - 50) * 0.015f) * difficulty.speedScaleMultiplier)
                }
            }
        }

        when (waveNumber) {
            1 -> {
                // Wave 1: Drones on Alpha and Beta (alternating, scaled by countMul)
                val count = (12 * countMul).toInt().coerceAtLeast(4)
                for (i in 0 until count) {
                    val lane = i % 2
                    spawnList.add(EnemySpawnEntry(EnemyType.DRONE, delayMs = if (i == 0) 300L else 750L, laneIndex = lane, hpScale = 0.9f * difficulty.hpScaleMultiplier, speedScale = baseSpeedScale))
                }
            }
            2 -> {
                // Wave 2: Drones and Automatons on both lanes
                val droneCount = (8 * countMul).toInt().coerceAtLeast(2)
                val soldierCount = (6 * countMul).toInt().coerceAtLeast(2)
                for (i in 0 until droneCount) {
                    spawnList.add(EnemySpawnEntry(EnemyType.DRONE, delayMs = 650L, laneIndex = i % 2, hpScale = baseHpScale, speedScale = baseSpeedScale))
                }
                for (i in 0 until soldierCount) {
                    spawnList.add(EnemySpawnEntry(EnemyType.SOLDIER, delayMs = 900L, laneIndex = (i + 1) % 2, hpScale = baseHpScale, speedScale = baseSpeedScale))
                }
            }
            3 -> {
                // Wave 3: Speedsters flank on Beta, Drones on Alpha
                val count = (8 * countMul).toInt().coerceAtLeast(2)
                for (i in 0 until count) {
                    spawnList.add(EnemySpawnEntry(EnemyType.DRONE, delayMs = 500L, laneIndex = 0, hpScale = baseHpScale, speedScale = baseSpeedScale))
                    spawnList.add(EnemySpawnEntry(EnemyType.SPEEDSTER, delayMs = 600L, laneIndex = 1, hpScale = baseHpScale, speedScale = 1.05f * baseSpeedScale))
                }
            }
            4 -> {
                // Wave 4: Golems leading each lane
                spawnList.add(EnemySpawnEntry(EnemyType.BRUISER, delayMs = 400L, laneIndex = 0, hpScale = baseHpScale, speedScale = baseSpeedScale))
                spawnList.add(EnemySpawnEntry(EnemyType.BRUISER, delayMs = 500L, laneIndex = 1, hpScale = baseHpScale, speedScale = baseSpeedScale))
                val soldierCount = (10 * countMul).toInt().coerceAtLeast(4)
                for (i in 0 until soldierCount) {
                    spawnList.add(EnemySpawnEntry(EnemyType.SOLDIER, delayMs = 700L, laneIndex = i % 2, hpScale = baseHpScale, speedScale = baseSpeedScale))
                }
            }
            5 -> {
                // Wave 5: Mini-Boss on Alpha, Fast Disruptors on Beta
                spawnList.add(
                    EnemySpawnEntry(
                        EnemyType.BOSS,
                        delayMs = 800L,
                        laneIndex = 0,
                        hpScale = baseHpScale * 1.5f,
                        speedScale = baseSpeedScale,
                        isBoss = true
                    )
                )
                val count = (8 * countMul).toInt().coerceAtLeast(3)
                for (i in 0 until count) {
                    spawnList.add(EnemySpawnEntry(EnemyType.SPEEDSTER, delayMs = 650L, laneIndex = 1, hpScale = baseHpScale, speedScale = baseSpeedScale))
                }
                spawnList.add(
                    EnemySpawnEntry(
                        EnemyType.BRUISER,
                        delayMs = 1200L,
                        laneIndex = 1,
                        hpScale = baseHpScale * 2.5f,
                        speedScale = baseSpeedScale,
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
                        speedScale = baseSpeedScale,
                        isBoss = true
                    )
                )
                val disruptorCount = (8 * countMul).toInt().coerceAtLeast(2)
                val speedsterCount = (12 * countMul).toInt().coerceAtLeast(4)
                for (i in 0 until disruptorCount) {
                    spawnList.add(EnemySpawnEntry(EnemyType.DISRUPTOR, delayMs = 700L, laneIndex = 1, hpScale = baseHpScale, speedScale = baseSpeedScale))
                }
                for (i in 0 until speedsterCount) {
                    spawnList.add(EnemySpawnEntry(EnemyType.SPEEDSTER, delayMs = 600L, laneIndex = i % 2, hpScale = baseHpScale, speedScale = baseSpeedScale))
                }
            }
            35 -> {
                // Wave 35: Dual-Front Bullet Hell (Cinematic 2) - 80-unit high-speed swarm across twin lanes
                val swarmCount = (80 * countMul).toInt().coerceAtLeast(40)
                for (i in 0 until swarmCount) {
                    val lane = i % 2
                    val type = if (i % 3 == 0) EnemyType.SPEEDSTER else EnemyType.DRONE
                    spawnList.add(
                        EnemySpawnEntry(
                            type = type,
                            delayMs = if (i == 0) 250L else 200L,
                            laneIndex = lane,
                            hpScale = baseHpScale * 0.70f,
                            speedScale = 1.35f
                        )
                    )
                }
            }
            40 -> {
                // Wave 40: Booster Teleportation Maneuver (Cinematic 3) - 50 armored soldiers demanding cross-lane turret warping
                val soldierCount = (50 * countMul).toInt().coerceAtLeast(25)
                for (i in 0 until soldierCount) {
                    val lane = i % 2
                    spawnList.add(
                        EnemySpawnEntry(
                            type = EnemyType.SOLDIER,
                            delayMs = if (i == 0) 300L else 350L,
                            laneIndex = lane,
                            hpScale = baseHpScale * 1.15f,
                            speedScale = 0.95f
                        )
                    )
                }
            }
            42 -> {
                // Wave 42: Prismatic Void Shatter (Cinematic 4) - 60 cluster units with Bruisers & Soldiers
                val count = (60 * countMul).toInt().coerceAtLeast(30)
                for (i in 0 until count) {
                    val lane = i % 2
                    val type = if (i % 5 == 0) EnemyType.BRUISER else EnemyType.SOLDIER
                    spawnList.add(
                        EnemySpawnEntry(
                            type = type,
                            delayMs = if (i == 0) 250L else 300L,
                            laneIndex = lane,
                            hpScale = baseHpScale * 1.25f,
                            speedScale = 0.90f
                        )
                    )
                }
            }
            45 -> {
                // Wave 45: Kinetic Vulcan Overclock (Cinematic 5) - Triple Armored Behemoths on Alpha and Beta
                for (i in 0 until 3) {
                    spawnList.add(
                        EnemySpawnEntry(
                            type = EnemyType.BRUISER,
                            delayMs = if (i == 0) 400L else 1400L,
                            laneIndex = 0,
                            hpScale = baseHpScale * 2.8f,
                            isBoss = true
                        )
                    )
                    spawnList.add(
                        EnemySpawnEntry(
                            type = EnemyType.BRUISER,
                            delayMs = 1200L,
                            laneIndex = 1,
                            hpScale = baseHpScale * 2.8f,
                            isBoss = true
                        )
                    )
                }
                val escorts = (20 * countMul).toInt().coerceAtLeast(10)
                for (i in 0 until escorts) {
                    spawnList.add(
                        EnemySpawnEntry(
                            type = EnemyType.SPEEDSTER,
                            delayMs = 450L,
                            laneIndex = i % 2,
                            hpScale = baseHpScale * 0.85f
                        )
                    )
                }
            }
            50 -> {
                // Wave 50: Astral Colossus Annihilation (Cinematic 1) - Apex Boss on Alpha with full drone escort
                spawnList.add(
                    EnemySpawnEntry(
                        type = EnemyType.BOSS,
                        delayMs = 600L,
                        laneIndex = 0,
                        hpScale = baseHpScale * 4.2f,
                        speedScale = 0.5f,
                        isBoss = true
                    )
                )
                val droneEscort = (30 * countMul).toInt().coerceAtLeast(15)
                for (i in 0 until droneEscort) {
                    spawnList.add(
                        EnemySpawnEntry(
                            type = EnemyType.DRONE,
                            delayMs = 320L,
                            laneIndex = if (i % 2 == 0) 0 else 1,
                            hpScale = baseHpScale * 0.85f,
                            speedScale = 1.15f
                        )
                    )
                }
            }
            52 -> {
                // Wave 52: Ascension Gauntlet (Cinematic 6) - Total Evolution Siege
                val totalEnemies = (52 * countMul).toInt().coerceAtLeast(26)
                for (i in 0 until totalEnemies) {
                    val type = when {
                        i % 12 == 0 -> EnemyType.ASTRAL_COLOSSUS
                        i % 4 == 0 -> EnemyType.BRUISER
                        i % 2 == 0 -> EnemyType.SPEEDSTER
                        else -> EnemyType.SOLDIER
                    }
                    spawnList.add(
                        EnemySpawnEntry(
                            type = type,
                            delayMs = 360L,
                            laneIndex = i % 2,
                            hpScale = baseHpScale * 1.4f,
                            speedScale = 1.0f,
                            isBoss = (i % 12 == 0)
                        )
                    )
                }
            }
            else -> {
                // Procedural mixed waves attacking both lanes
                val baseCount = 16 + waveNumber * 3
                val enemyCount = (baseCount * countMul).toInt().coerceAtLeast(6)
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
                    val type = pickEnemyType(i, waveNumber)
                    val delay = spawnDelayFor(type)

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

        // Calibrated difficulty-aware wave completion scrap bonus curbing late-game runaway while keeping full-board Singularity viable
        val baseBonusScrap = 40 + minOf(waveNumber, 40) * 6 + if (isBossWave) 70 else 0
        val bonusScrap = (baseBonusScrap * difficulty.scrapIncomeMultiplier).toInt()

        val displayModifier = if (difficulty != GameDifficulty.MEDIUM) {
            "[${difficulty.label}] $modifier"
        } else {
            modifier
        }

        return WaveData(
            waveNumber = waveNumber,
            name = name,
            modifierLabel = displayModifier,
            totalEnemies = spawnList.size,
            spawnList = spawnList,
            completionBonusScrap = bonusScrap,
            isBossWave = isBossWave,
            rolesPresent = spawnList.mapTo(mutableSetOf()) { it.type.role }
        )
    }
}
