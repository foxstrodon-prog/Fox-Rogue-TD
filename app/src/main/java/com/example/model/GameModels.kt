package com.example.model

enum class CinematicScenario(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val thumbnailTimestampSec: Float,
    val targetWave: Int
) {
    CIN_01_BOSS_MELT(
        id = "cin_01",
        title = "Astral Colossus Annihilation",
        subtitle = "Apex Stellar Star vs Void Colossus",
        description = "Level 4 Radiant Apex Corona channels focused cosmic beam energy through a booster pad, vaporizing the armored void colossus.",
        thumbnailTimestampSec = 23.40f,
        targetWave = 50
    ),
    CIN_02_BULLET_HELL(
        id = "cin_02",
        title = "Dual-Front Swarm Saturation",
        subtitle = "Twin-Lane Full-Screen Firestorm",
        description = "Coordinated flak barriers and rapid-fire vulcan cannons saturating twin lanes against advancing swarms.",
        thumbnailTimestampSec = 18.20f,
        targetWave = 35
    ),
    CIN_03_BOOSTER_WARP(
        id = "cin_03",
        title = "Booster Teleportation Maneuver",
        subtitle = "Instant Dual-Front Tactical Swaps",
        description = "High-speed tactical turret teleportation across linked booster pads, instantly shifting heavy firepower between lanes.",
        thumbnailTimestampSec = 15.30f,
        targetWave = 40
    ),
    CIN_04_ELEMENTAL_CASCADE(
        id = "cin_04",
        title = "Prismatic Void Shatter",
        subtitle = "Cryo Freeze & Singularity Collapse",
        description = "Sub-zero frost nova freezing the advance while a gravitational void singularity collapses the cluster in a prismatic chain explosion.",
        thumbnailTimestampSec = 21.10f,
        targetWave = 42
    ),
    CIN_05_KINETIC_GATLING(
        id = "cin_05",
        title = "Kinetic Vulcan Overclock",
        subtitle = "Hyper-Velocity Ballistic Shred",
        description = "Heavy Gatling barrel spooling to maximum velocity, blasting armored behemoths with sustained tracer fire and muzzle bloom.",
        thumbnailTimestampSec = 16.50f,
        targetWave = 45
    ),
    CIN_06_EVOLUTION_MONTAGE(
        id = "cin_06",
        title = "Ascension Showcase",
        subtitle = "Tier 1 to Apex Final Forms",
        description = "Time-lapse transformation from basic starter turrets up through specialized evolution branches into radiant Apex forms.",
        thumbnailTimestampSec = 25.00f,
        targetWave = 52
    );

    val cinematicNumber: Int
        get() = when (this) {
            CIN_01_BOSS_MELT -> 1
            CIN_02_BULLET_HELL -> 2
            CIN_03_BOOSTER_WARP -> 3
            CIN_04_ELEMENTAL_CASCADE -> 4
            CIN_05_KINETIC_GATLING -> 5
            CIN_06_EVOLUTION_MONTAGE -> 6
        }

    fun getTargetFilename(version: String = "v001004"): String = "${version}_Cinematic${cinematicNumber}.mp4"
}

data class ActiveCinematicState(
    val scenario: CinematicScenario,
    val currentFrame: Int = 0,
    val totalFrames: Int = 1800 // Exactly 30.00 seconds at 60 FPS
) {
    val progress: Float get() = (currentFrame.toFloat() / totalFrames).coerceIn(0f, 1f)
    val elapsedSeconds: Float get() = currentFrame / 60f
    val remainingSeconds: Float get() = maxOf(0f, (totalFrames - currentFrame) / 60f)
    val isComplete: Boolean get() = currentFrame >= totalFrames
}

enum class GameSpeed(val multiplier: Float, val label: String) {
    PAUSED(0f, "PAUSE"),
    NORMAL(1f, "1X"),
    FAST(2f, "2X"),
    ULTRA(3.5f, "3.5X"),
    MAX(5.0f, "5X"),
    HYPER(10.0f, "10X")
}

enum class GameDifficulty(
    val id: String,
    val label: String,
    val maxWave: Int,
    val description: String,
    val hpScaleMultiplier: Float,
    val speedScaleMultiplier: Float,
    val enemyCountMultiplier: Float
) {
    EASY("easy", "EASY", 30, "Reconnaissance: Wave 30 Milestone (-20% HP, -10% Speed)", 0.80f, 0.90f, 0.85f),
    MEDIUM("medium", "MEDIUM", 50, "Standard Defense: Wave 50 Milestone (Calibrated Baseline)", 1.00f, 1.00f, 1.00f),
    HARD("hard", "HARD", 100, "Endgame Gauntlet: Wave 100 Milestone (True Exponential Past W30)", 1.25f, 1.05f, 1.15f),
    INFINITE("infinite", "INFINITE", Int.MAX_VALUE, "Endless Incursion: Infinite Scaling (1.08^W Past W50)", 1.00f, 1.00f, 1.00f)
}

enum class TargetPriority(val label: String) {
    FIRST("First"),
    CLOSEST("Closest"),
    STRONGEST("Strongest"),
    WEAKEST("Weakest")
}

data class RunRecord(
    val id: String,
    val seed: Long = 0L,
    val sectorName: String,
    val playerName: String = "Operative",
    val wavesCleared: Int,
    val enemiesKilled: Int,
    val totalDamage: Float,
    val scrapEarned: Int,
    val ascendedTowersCount: Int = 0,
    val mvpTower: String,
    val isVictory: Boolean,
    val timestamp: Long,
    /** True if any dev-console balance value deviated from baseline during the run (DEV/SANDBOX). */
    val isDevTuned: Boolean = false
) {
    val score: Int get() = (wavesCleared * 1000) + (enemiesKilled * 15) + (totalDamage * 0.1f).toInt()
}

/** One row of the global (online) leaderboard. */
data class LeaderboardEntry(
    val rank: Int = 1,
    val name: String,
    val score: Int = 0,
    val wavesCleared: Int = 0,
    val scrapEarned: Int = 0,
    val isDevTuned: Boolean = false,
    val date: String = "",
    val deviceUuid: String = ""
)

/** One row of the global (online) leaderboard. */
data class GlobalScoreEntry(
    val rank: Int,
    val playerName: String,
    val wavesCleared: Int,
    val score: Int,
    val timestamp: Long = 0L,
    val isLocalPlayer: Boolean = false
)

enum class GlobalBoardStatus {
    IDLE,            // Nothing requested yet
    LOADING,
    ONLINE,          // Fresh data from the backend
    OFFLINE_CACHED,  // Network failed; showing last cached global board (or empty)
    NOT_CONFIGURED,  // No backend configured: local-only fallback
    ERROR            // Network or server error with failure details
}

enum class RunSubmissionState {
    NONE,
    EXCLUDED_DEV_TUNED,  // Run carries the DEV/SANDBOX watermark: never submitted
    LOCAL_ONLY,          // Backend not configured
    PENDING,             // Queued; will retry when online
    SUBMITTED
}

data class LifetimeTelemetry(
    val totalRuns: Int = 0,
    val totalVictories: Int = 0,
    val totalHostilesKilled: Int = 0,
    val totalScrapMined: Long = 0L,
    val totalDamageOutput: Double = 0.0,
    val bestWaveEver: Int = 0,
    val mostUsedTower: String = "Vulcan Minigun"
)

data class DevBalanceConfig(
    val startingScrap: Int = 220,             // Default reduced early income
    val globalIncomeMultiplier: Float = 1.0f,  // 0.2x to 3.0x slider
    val globalTowerFireRateMultiplier: Float = 1.0f, // 0.2x to 3.0x overall tower fire rate slider
    val enemyHpMultiplier: Float = 1.0f,       // 0.5x to 3.0x enemy health difficulty
    val enemySpeedMultiplier: Float = 1.0f,    // 0.2x to 3.0x enemy movement speed slider
    val kineticDamageMultiplier: Float = 1.0f, // 0.2x to 3.0x per class/tower sliders
    val elementalDamageMultiplier: Float = 1.0f,
    val heavyDamageMultiplier: Float = 1.0f,
    val waveBonusMultiplier: Float = 1.0f,     // 0.2x to 2.5x end of wave bonus
    val buildCostMultiplier: Float = 1.0f,     // 0.5x to 2.0x tower build costs
    val minLaneLength: Int = 16,              // 8 to 35 slider
    val maxLaneLength: Int = 32,              // 15 to 50 slider
    val enemiesPerRoundMultiplier: Float = 1.0f, // 0.5x to 3.0x enemies per round
    val masterVolume: Float = 0.7f,
    val musicVolume: Float = 0.45f,
    val sfxVolume: Float = 0.8f,
    val upgradeVolume: Float = 0.85f,
    val isMuted: Boolean = false,
    val musicTrackName: String = "ROLLING_PLAYLIST",
    val kineticSfxSlot: Int = 1,
    val elementalSfxSlot: Int = 1,
    val siegeSfxSlot: Int = 1,
    val turretSkinSlot: Int = 1,
    val mapThemeSlot: Int = 1,
    val customTowerConfigs: Map<String, TowerUnitBalance> = emptyMap(),
    val customEnemyConfigs: Map<String, EnemyUnitBalance> = emptyMap()
) {
    fun getTowerKey(type: TowerType, branchTier1: String? = null, finalFormCode: String? = null): String {
        return when {
            finalFormCode != null -> "${type.name}_$finalFormCode"
            branchTier1 != null -> "${type.name}_$branchTier1"
            else -> type.name
        }
    }

    fun getTowerBalance(type: TowerType): TowerUnitBalance {
        return customTowerConfigs[type.name] ?: TowerUnitBalance(
            customName = null,
            damage = type.baseDamage,
            range = type.baseRange,
            fireRate = type.baseFireRate,
            cost = type.baseCost,
            specialEffectValue = when {
                type.isSplash -> type.splashRadius
                type.slowPercent > 0f -> type.slowPercent
                type.chains > 0 -> type.chains.toFloat()
                else -> 0f
            }
        )
    }

    fun getEffectiveTowerBalance(
        type: TowerType,
        branchTier1: String? = null,
        finalFormCode: String? = null
    ): TowerUnitBalance {
        val key = getTowerKey(type, branchTier1, finalFormCode)
        val custom = customTowerConfigs[key]
        if (custom != null) return custom

        return when {
            finalFormCode != null -> {
                val ff = com.example.engine.TowerUpgradeManager.getFinalForm(type, finalFormCode)
                val dmgMul = ff?.damageMultiplier ?: 1.0f
                val spdMul = ff?.fireRateMultiplier ?: 1.0f
                val rngMul = ff?.rangeMultiplier ?: 1.0f
                val cost = ff?.cost ?: (type.baseCost * 2)
                TowerUnitBalance(
                    customName = ff?.subName,
                    damage = type.baseDamage * dmgMul,
                    range = type.baseRange * rngMul,
                    fireRate = type.baseFireRate * spdMul,
                    cost = cost,
                    specialEffectValue = when {
                        type.isSplash -> type.splashRadius
                        type.slowPercent > 0f -> type.slowPercent
                        type.chains > 0 -> type.chains.toFloat()
                        else -> 0f
                    }
                )
            }
            branchTier1 != null -> {
                val t1 = com.example.engine.TowerUpgradeManager.getTier1Options(type).firstOrNull { it.branchKey == branchTier1 }
                val dmgMul = t1?.damageMultiplier ?: 1.0f
                val spdMul = t1?.fireRateMultiplier ?: 1.0f
                val rngMul = t1?.rangeMultiplier ?: 1.0f
                val cost = t1?.cost ?: (type.baseCost * 1.5f).toInt()
                TowerUnitBalance(
                    customName = t1?.subName,
                    damage = type.baseDamage * dmgMul,
                    range = type.baseRange * rngMul,
                    fireRate = type.baseFireRate * spdMul,
                    cost = cost,
                    specialEffectValue = when {
                        type.isSplash -> type.splashRadius
                        type.slowPercent > 0f -> type.slowPercent
                        type.chains > 0 -> type.chains.toFloat()
                        else -> 0f
                    }
                )
            }
            else -> getTowerBalance(type)
        }
    }

    companion object {
        fun getAll52TowerDescriptors(): List<TowerBalanceDescriptor> {
            val list = mutableListOf<TowerBalanceDescriptor>()
            val baseTypes = listOf(TowerType.GATLING, TowerType.CRYO, TowerType.TESLA, TowerType.STELLAR)

            for (type in baseTypes) {
                // L1 Base (4 towers)
                list.add(
                    TowerBalanceDescriptor(
                        key = type.name,
                        displayName = type.displayName,
                        level = 1,
                        family = type,
                        defaultDamage = type.baseDamage,
                        defaultRange = type.baseRange,
                        defaultFireRate = type.baseFireRate,
                        defaultCost = type.baseCost,
                        specialEffectValue = when {
                            type.isSplash -> type.splashRadius
                            type.slowPercent > 0f -> type.slowPercent
                            type.chains > 0 -> type.chains.toFloat()
                            else -> 0f
                        }
                    )
                )

                // L2 Tier 1 Options (12 towers: 3 per base)
                val t1Options = com.example.engine.TowerUpgradeManager.getTier1Options(type)
                for (opt in t1Options) {
                    list.add(
                        TowerBalanceDescriptor(
                            key = "${type.name}_${opt.branchKey}",
                            displayName = "${type.displayName} - ${opt.subName}",
                            level = 2,
                            family = type,
                            branchTier1 = opt.branchKey,
                            defaultDamage = type.baseDamage * opt.damageMultiplier,
                            defaultRange = type.baseRange * opt.rangeMultiplier,
                            defaultFireRate = type.baseFireRate * opt.fireRateMultiplier,
                            defaultCost = opt.cost,
                            specialEffectValue = 0f
                        )
                    )
                }

                // L3 Final Forms (36 towers: 9 per base)
                val finalForms = com.example.engine.TowerUpgradeManager.get9FinalForms(type)
                for (ff in finalForms) {
                    list.add(
                        TowerBalanceDescriptor(
                            key = "${type.name}_${ff.code}",
                            displayName = "${type.displayName} - ${ff.subName}",
                            level = 3,
                            family = type,
                            branchTier1 = ff.code.substringBefore("."),
                            finalFormCode = ff.code,
                            defaultDamage = type.baseDamage * ff.damageMultiplier,
                            defaultRange = type.baseRange * ff.rangeMultiplier,
                            defaultFireRate = type.baseFireRate * ff.fireRateMultiplier,
                            defaultCost = ff.cost,
                            specialEffectValue = 0f
                        )
                    )
                }
            }
            return list
        }

        fun getAll39TowerDescriptors(): List<TowerBalanceDescriptor> {
            return getAll52TowerDescriptors().filter { it.family != TowerType.STELLAR }
        }
    }

    fun getEnemyBalance(type: EnemyType): EnemyUnitBalance {
        return customEnemyConfigs[type.name] ?: EnemyUnitBalance(
            customName = null,
            hp = type.baseHp,
            speed = type.baseSpeed,
            armor = type.baseArmor,
            scrapValue = type.scrapValue
        )
    }

    /**
     * True when gameplay balance sliders (scrap, multipliers, tower/enemy overrides, lane lengths)
     * differ from standard baseline. Audio, volume, music track, skins, and cosmetic themes
     * do NOT constitute gameplay modifications and will never disqualify a run.
     */
    fun isGameplayModified(): Boolean {
        val default = DevBalanceConfig()
        if (startingScrap != default.startingScrap) return true
        if (globalIncomeMultiplier != default.globalIncomeMultiplier) return true
        if (globalTowerFireRateMultiplier != default.globalTowerFireRateMultiplier) return true
        if (enemyHpMultiplier != default.enemyHpMultiplier) return true
        if (enemySpeedMultiplier != default.enemySpeedMultiplier) return true
        if (kineticDamageMultiplier != default.kineticDamageMultiplier) return true
        if (elementalDamageMultiplier != default.elementalDamageMultiplier) return true
        if (heavyDamageMultiplier != default.heavyDamageMultiplier) return true
        if (waveBonusMultiplier != default.waveBonusMultiplier) return true
        if (buildCostMultiplier != default.buildCostMultiplier) return true
        if (minLaneLength != default.minLaneLength) return true
        if (maxLaneLength != default.maxLaneLength) return true
        if (enemiesPerRoundMultiplier != default.enemiesPerRoundMultiplier) return true

        if (customTowerConfigs.isNotEmpty()) {
            val allDescriptors = getAll39TowerDescriptors()
            for (desc in allDescriptors) {
                val curr = getEffectiveTowerBalance(desc.family, desc.branchTier1, desc.finalFormCode)
                val defTower = default.getEffectiveTowerBalance(desc.family, desc.branchTier1, desc.finalFormCode)
                if (curr.cost != defTower.cost ||
                    Math.abs(curr.damage - defTower.damage) > 0.01f ||
                    Math.abs(curr.fireRate - defTower.fireRate) > 0.01f ||
                    Math.abs(curr.range - defTower.range) > 0.01f ||
                    Math.abs(curr.specialEffectValue - defTower.specialEffectValue) > 0.01f) {
                    return true
                }
            }
        }

        if (customEnemyConfigs.isNotEmpty()) {
            for (type in EnemyType.values()) {
                val curr = getEnemyBalance(type)
                if (curr.hp != type.baseHp ||
                    curr.speed != type.baseSpeed ||
                    curr.armor != type.baseArmor ||
                    curr.scrapValue != type.scrapValue) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * True when every balance slider / per-unit override matches the shipped baseline.
     * Anything else marks a run as DEV/SANDBOX (isDevTuned) and excludes it from ranked submissions.
     */
    val isBaseline: Boolean get() = !isGameplayModified()

    /**
     * Generates a strict delta / diff-only JSON string containing ONLY values
     * that differ from the stock baseline defaults.
     */
    fun exportDiffJson(): String {
        val diffMap = mutableMapOf<String, Any>()
        val default = DevBalanceConfig()

        if (startingScrap != default.startingScrap) diffMap["startingScrap"] = startingScrap
        if (globalIncomeMultiplier != default.globalIncomeMultiplier) diffMap["globalIncomeMultiplier"] = globalIncomeMultiplier
        if (globalTowerFireRateMultiplier != default.globalTowerFireRateMultiplier) diffMap["globalTowerFireRateMultiplier"] = globalTowerFireRateMultiplier
        if (enemyHpMultiplier != default.enemyHpMultiplier) diffMap["enemyHpMultiplier"] = enemyHpMultiplier
        if (enemySpeedMultiplier != default.enemySpeedMultiplier) diffMap["enemySpeedMultiplier"] = enemySpeedMultiplier
        if (kineticDamageMultiplier != default.kineticDamageMultiplier) diffMap["kineticDamageMultiplier"] = kineticDamageMultiplier
        if (elementalDamageMultiplier != default.elementalDamageMultiplier) diffMap["elementalDamageMultiplier"] = elementalDamageMultiplier
        if (heavyDamageMultiplier != default.heavyDamageMultiplier) diffMap["heavyDamageMultiplier"] = heavyDamageMultiplier
        if (waveBonusMultiplier != default.waveBonusMultiplier) diffMap["waveBonusMultiplier"] = waveBonusMultiplier
        if (buildCostMultiplier != default.buildCostMultiplier) diffMap["buildCostMultiplier"] = buildCostMultiplier
        if (minLaneLength != default.minLaneLength) diffMap["minLaneLength"] = minLaneLength
        if (maxLaneLength != default.maxLaneLength) diffMap["maxLaneLength"] = maxLaneLength
        if (enemiesPerRoundMultiplier != default.enemiesPerRoundMultiplier) diffMap["enemiesPerRoundMultiplier"] = enemiesPerRoundMultiplier

        // Audio & Visual Settings Diffs
        if (Math.abs(masterVolume - default.masterVolume) > 0.01f) diffMap["masterVolume"] = masterVolume
        if (Math.abs(musicVolume - default.musicVolume) > 0.01f) diffMap["musicVolume"] = musicVolume
        if (Math.abs(sfxVolume - default.sfxVolume) > 0.01f) diffMap["sfxVolume"] = sfxVolume
        if (Math.abs(upgradeVolume - default.upgradeVolume) > 0.01f) diffMap["upgradeVolume"] = upgradeVolume
        if (isMuted != default.isMuted) diffMap["isMuted"] = isMuted
        if (musicTrackName != default.musicTrackName) diffMap["musicTrackName"] = musicTrackName
        if (kineticSfxSlot != default.kineticSfxSlot) diffMap["kineticSfxSlot"] = kineticSfxSlot
        if (elementalSfxSlot != default.elementalSfxSlot) diffMap["elementalSfxSlot"] = elementalSfxSlot
        if (siegeSfxSlot != default.siegeSfxSlot) diffMap["siegeSfxSlot"] = siegeSfxSlot
        if (turretSkinSlot != default.turretSkinSlot) diffMap["turretSkinSlot"] = turretSkinSlot
        if (mapThemeSlot != default.mapThemeSlot) diffMap["mapThemeSlot"] = mapThemeSlot

        // Tower diffs across all 39 units (L1 Base, L2 Specializations, L3 Final Forms)
        val towerDiffs = mutableMapOf<String, Map<String, Any>>()
        val allDescriptors = getAll39TowerDescriptors()
        for (desc in allDescriptors) {
            val curr = getEffectiveTowerBalance(desc.family, desc.branchTier1, desc.finalFormCode)
            val defTower = default.getEffectiveTowerBalance(desc.family, desc.branchTier1, desc.finalFormCode)
            val tDiff = mutableMapOf<String, Any>()

            if (!curr.customName.isNullOrBlank() && curr.customName != defTower.customName) {
                tDiff["customName"] = curr.customName
            }
            if (Math.abs(curr.damage - defTower.damage) > 0.01f) tDiff["damage"] = curr.damage
            if (Math.abs(curr.range - defTower.range) > 0.01f) tDiff["range"] = curr.range
            if (Math.abs(curr.fireRate - defTower.fireRate) > 0.01f) tDiff["fireRate"] = curr.fireRate
            if (curr.cost != defTower.cost) tDiff["cost"] = curr.cost
            if (Math.abs(curr.specialEffectValue - defTower.specialEffectValue) > 0.01f) {
                tDiff["specialEffectValue"] = curr.specialEffectValue
            }

            if (tDiff.isNotEmpty()) {
                towerDiffs[desc.key] = tDiff
            }
        }
        if (towerDiffs.isNotEmpty()) diffMap["customTowers"] = towerDiffs

        // Enemy diffs
        val enemyDiffs = mutableMapOf<String, Map<String, Any>>()
        for (type in EnemyType.values()) {
            val curr = getEnemyBalance(type)
            val defEnemy = default.getEnemyBalance(type)
            val eDiff = mutableMapOf<String, Any>()

            if (!curr.customName.isNullOrBlank() && curr.customName != type.displayName) {
                eDiff["customName"] = curr.customName
            }
            if (Math.abs(curr.hp - defEnemy.hp) > 0.01f) eDiff["hp"] = curr.hp
            if (Math.abs(curr.speed - defEnemy.speed) > 0.01f) eDiff["speed"] = curr.speed
            if (Math.abs(curr.armor - defEnemy.armor) > 0.01f) eDiff["armor"] = curr.armor
            if (curr.scrapValue != defEnemy.scrapValue) eDiff["scrapValue"] = curr.scrapValue

            if (eDiff.isNotEmpty()) {
                enemyDiffs[type.name] = eDiff
            }
        }
        if (enemyDiffs.isNotEmpty()) diffMap["customEnemies"] = enemyDiffs
        if (diffMap.isEmpty()) return "{}"

        // Serialize to clean formatted JSON string
        val sb = StringBuilder("{\n")
        val entries = diffMap.entries.toList()
        entries.forEachIndexed { i, (k, v) ->
            sb.append("  \"").append(k).append("\": ")
            when (v) {
                is Number, is Boolean -> sb.append(v)
                is String -> sb.append("\"").append(v).append("\"")
                is Map<*, *> -> {
                    sb.append("{\n")
                    val subEntries = v.entries.toList()
                    subEntries.forEachIndexed { j, sub ->
                        sb.append("    \"").append(sub.key).append("\": ")
                        val subVal = sub.value
                        if (subVal is Map<*, *>) {
                            sb.append("{\n")
                            val leafEntries = subVal.entries.toList()
                            leafEntries.forEachIndexed { kLeaf, leaf ->
                                sb.append("      \"").append(leaf.key).append("\": ")
                                if (leaf.value is String) sb.append("\"").append(leaf.value).append("\"")
                                else sb.append(leaf.value)
                                if (kLeaf < leafEntries.size - 1) sb.append(",")
                                sb.append("\n")
                            }
                            sb.append("    }")
                        } else if (subVal is String) {
                            sb.append("\"").append(subVal).append("\"")
                        } else {
                            sb.append(subVal)
                        }
                        if (j < subEntries.size - 1) sb.append(",")
                        sb.append("\n")
                    }
                    sb.append("  }")
                }
                else -> sb.append("\"").append(v.toString()).append("\"")
            }
            if (i < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("}")
        return sb.toString()
    }
}

data class TowerPerformanceRecord(
    val towerName: String,
    val towerClass: TowerClass,
    val totalDamage: Double = 0.0,
    val totalKills: Long = 0L,
    val totalBuilt: Int = 0,
    val totalCostInvested: Long = 0L
) {
    val dpsCostEfficiency: Double get() = if (totalCostInvested > 0) totalDamage / totalCostInvested.toDouble() else 0.0
    val killsCostEfficiency: Double get() = if (totalCostInvested > 0) (totalKills * 1000.0) / totalCostInvested.toDouble() else 0.0
}
