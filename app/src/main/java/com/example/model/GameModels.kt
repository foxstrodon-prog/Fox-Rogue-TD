package com.example.model

import com.example.engine.TowerUpgradeManager

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
    val enemyCountMultiplier: Float,
    val scrapIncomeMultiplier: Float = 1.0f
) {
    EASY("easy", "EASY", 30, "Reconnaissance: Wave 30 Milestone (-20% HP, -10% Speed)", 0.80f, 0.90f, 0.85f, 1.45f),
    MEDIUM("medium", "MEDIUM", 50, "Standard Defense: Wave 50 Milestone (Calibrated Baseline)", 1.00f, 1.00f, 1.00f, 1.20f),
    HARD("hard", "HARD", 100, "Endgame Gauntlet: Wave 100 Milestone (True Exponential Past W30)", 1.25f, 1.05f, 1.15f, 1.05f),
    INFINITE("infinite", "INFINITE", Int.MAX_VALUE, "Endless Incursion: Infinite Scaling (1.08^W Past W50)", 1.00f, 1.00f, 1.00f, 1.00f)
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
    val easyTargetOpenSlots: Int = 15,
    val mediumTargetOpenSlots: Int = 15,
    val hardTargetOpenSlots: Int = 15,
    val minLaneOverlap: Int = 4,
    val maxLaneOverlap: Int = 12,
    val mapColumns: Int = 10,
    val mapRows: Int = 20,
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
    fun getTargetOpenSlotsForDifficulty(difficulty: GameDifficulty): Int {
        return when (difficulty) {
            GameDifficulty.EASY -> easyTargetOpenSlots
            GameDifficulty.MEDIUM -> mediumTargetOpenSlots
            GameDifficulty.HARD, GameDifficulty.INFINITE -> hardTargetOpenSlots
        }
    }

    fun getTowerKey(type: TowerType, branchTier1: String? = null, finalFormCode: String? = null): String {
        return when {
            finalFormCode != null -> "${type.name}_$finalFormCode"
            branchTier1 != null -> "${type.name}_$branchTier1"
            else -> type.name
        }
    }

    fun getTowerBalance(type: TowerType): TowerUnitBalance {
        return customTowerConfigs[type.name] ?: DEFAULT_TOWER_BALANCES[type.name]!!
    }

    fun findCustomTowerBalance(tower: TowerInstance): TowerUnitBalance? {
        val arch = tower.archetype
        val ffCode = tower.finalFormCode
        val b1 = tower.branchTier1
        return when {
            arch != null -> customTowerConfigs[arch.name]
            ffCode != null -> customTowerConfigs["${tower.type.name}_$ffCode"]
            b1 != null -> {
                val classBaseType = mapClassToType(tower.towerClass)
                customTowerConfigs["${tower.type.name}_$b1"]
                    ?: if (classBaseType != tower.type) customTowerConfigs["${classBaseType.name}_$b1"] else null
            }
            else -> {
                val classBaseType = mapClassToType(tower.towerClass)
                customTowerConfigs[tower.type.name]
                    ?: if (classBaseType != tower.type) customTowerConfigs[classBaseType.name] else null
            }
        }
    }

    fun getEffectiveTowerBalance(
        type: TowerType,
        branchTier1: String? = null,
        finalFormCode: String? = null
    ): TowerUnitBalance {
        val key = getTowerKey(type, branchTier1, finalFormCode)
        customTowerConfigs[key]?.let { return it }
        DEFAULT_TOWER_BALANCES[key]?.let { return it }
        return getTowerBalance(type)
    }

    fun getEffectiveTowerBalance(descriptor: TowerBalanceDescriptor): TowerUnitBalance {
        customTowerConfigs[descriptor.key]?.let { return it }
        DEFAULT_TOWER_BALANCES[descriptor.key]?.let { return it }
        return getEffectiveTowerBalance(descriptor.family, descriptor.branchTier1, descriptor.finalFormCode)
    }

    companion object {
        val DEFAULT_TOWER_BALANCES: Map<String, TowerUnitBalance> = buildMap {
            for (type in TowerType.values()) {
                val specialVal = when {
                    type.isSplash -> type.splashRadius
                    type.slowPercent > 0f -> type.slowPercent
                    type.chains > 0 -> type.chains.toFloat()
                    else -> 0f
                }
                val ownerClass = TowerClass.values().firstOrNull { mapClassToType(it) == type }
                put(
                    type.name,
                    TowerUnitBalance(
                        customName = null,
                        damage = type.baseDamage,
                        range = type.baseRange,
                        fireRate = type.baseFireRate,
                        cost = ownerClass?.baseCost ?: type.baseCost,
                        specialEffectValue = specialVal
                    )
                )
                for (t1 in TowerUpgradeManager.getTier1Options(type)) {
                    put(
                        "${type.name}_${t1.branchKey}",
                        TowerUnitBalance(
                            customName = t1.subName,
                            damage = type.baseDamage * t1.damageMultiplier,
                            range = type.baseRange * t1.rangeMultiplier,
                            fireRate = type.baseFireRate * t1.fireRateMultiplier,
                            cost = ownerClass?.let { TowerUpgradeManager.getTier1VectorCost(it) } ?: t1.cost,
                            specialEffectValue = specialVal
                        )
                    )
                }
                for (ff in TowerUpgradeManager.get9FinalForms(type)) {
                    put(
                        "${type.name}_${ff.code}",
                        TowerUnitBalance(
                            customName = ff.subName,
                            damage = type.baseDamage * ff.damageMultiplier,
                            range = type.baseRange * ff.rangeMultiplier,
                            fireRate = type.baseFireRate * ff.fireRateMultiplier,
                            cost = ff.cost,
                            specialEffectValue = specialVal
                        )
                    )
                }
            }
            for (arch in TowerArchetype.values()) {
                val specialVal = when {
                    arch.isSplash -> arch.splashRadius
                    arch.slowPercent > 0f -> arch.slowPercent
                    arch.chains > 0 -> arch.chains.toFloat()
                    else -> 0f
                }
                put(
                    arch.name,
                    TowerUnitBalance(
                        customName = null,
                        damage = arch.baseDamage,
                        range = arch.baseRange,
                        fireRate = arch.baseFireRate,
                        cost = arch.baseCost,
                        specialEffectValue = specialVal
                    )
                )
            }
        }

        private val ALL_52_DESCRIPTORS: List<TowerBalanceDescriptor> = buildList {
            for (cls in TowerClass.values()) {
                val baseType = mapClassToType(cls)
                val baseSpecial = when {
                    baseType.isSplash -> baseType.splashRadius
                    baseType.slowPercent > 0f -> baseType.slowPercent
                    baseType.chains > 0 -> baseType.chains.toFloat()
                    else -> 0f
                }
                add(
                    TowerBalanceDescriptor(
                        key = baseType.name,
                        displayName = cls.displayName,
                        level = 1,
                        family = baseType,
                        defaultDamage = baseType.baseDamage,
                        defaultRange = baseType.baseRange,
                        defaultFireRate = baseType.baseFireRate,
                        defaultCost = cls.baseCost,
                        specialEffectValue = baseSpecial,
                        towerClass = cls
                    )
                )
                for (vec in TowerVector.values()) {
                    val branchKey = when (vec) {
                        TowerVector.OFFENSIVE -> "A"
                        TowerVector.DEFENSIVE -> "B"
                        TowerVector.SUPPORT -> "C"
                    }
                    val t1Opt = TowerUpgradeManager.getTier1Form(baseType, branchKey)
                    add(
                        TowerBalanceDescriptor(
                            key = "${baseType.name}_$branchKey",
                            displayName = "${cls.displayName} - ${vec.displayName}",
                            level = 2,
                            family = baseType,
                            branchTier1 = branchKey,
                            defaultDamage = baseType.baseDamage * (t1Opt?.damageMultiplier ?: 1.0f),
                            defaultRange = baseType.baseRange * (t1Opt?.rangeMultiplier ?: 1.0f),
                            defaultFireRate = baseType.baseFireRate * (t1Opt?.fireRateMultiplier ?: 1.0f),
                            defaultCost = TowerUpgradeManager.getTier1VectorCost(cls),
                            specialEffectValue = baseSpecial,
                            towerClass = cls,
                            vector = vec
                        )
                    )
                }
                for (arch in TowerArchetype.values().filter { it.towerClass == cls }) {
                    val branchKey = when (arch.vector) {
                        TowerVector.OFFENSIVE -> "A"
                        TowerVector.DEFENSIVE -> "B"
                        TowerVector.SUPPORT -> "C"
                    }
                    val specialVal = when {
                        arch.isSplash -> arch.splashRadius
                        arch.slowPercent > 0f -> arch.slowPercent
                        arch.chains > 0 -> arch.chains.toFloat()
                        else -> 0f
                    }
                    add(
                        TowerBalanceDescriptor(
                            key = arch.name,
                            displayName = "${cls.displayName} - ${arch.displayName}",
                            level = 3,
                            family = mapArchetypeToType(arch),
                            branchTier1 = branchKey,
                            defaultDamage = arch.baseDamage,
                            defaultRange = arch.baseRange,
                            defaultFireRate = arch.baseFireRate,
                            defaultCost = arch.baseCost,
                            specialEffectValue = specialVal,
                            towerClass = cls,
                            vector = arch.vector,
                            archetype = arch
                        )
                    )
                }
            }
        }

        private val ALL_39_DESCRIPTORS: List<TowerBalanceDescriptor> =
            ALL_52_DESCRIPTORS.filter { it.towerClass != TowerClass.STELLAR_COSMIC }

        val STATIC_ENEMY_BALANCES: Map<EnemyType, EnemyUnitBalance> = EnemyType.values().associateWith { type ->
            EnemyUnitBalance(
                customName = null,
                hp = type.baseHp,
                speed = type.baseSpeed,
                armor = type.baseArmor,
                scrapValue = type.scrapValue
            )
        }

        fun getAll52TowerDescriptors(): List<TowerBalanceDescriptor> = ALL_52_DESCRIPTORS

        fun getAll39TowerDescriptors(): List<TowerBalanceDescriptor> = ALL_39_DESCRIPTORS
    }

    fun getEnemyBalance(type: EnemyType): EnemyUnitBalance {
        return customEnemyConfigs[type.name] ?: STATIC_ENEMY_BALANCES[type]!!
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
        if (easyTargetOpenSlots != default.easyTargetOpenSlots) return true
        if (mediumTargetOpenSlots != default.mediumTargetOpenSlots) return true
        if (hardTargetOpenSlots != default.hardTargetOpenSlots) return true
        if (minLaneOverlap != default.minLaneOverlap) return true
        if (maxLaneOverlap != default.maxLaneOverlap) return true
        if (mapColumns != default.mapColumns) return true
        if (mapRows != default.mapRows) return true
        if (enemiesPerRoundMultiplier != default.enemiesPerRoundMultiplier) return true

        if (customTowerConfigs.isNotEmpty()) {
            val allDescriptors = getAll52TowerDescriptors()
            val descriptorKeys = HashSet<String>(allDescriptors.size)
            for (desc in allDescriptors) {
                descriptorKeys.add(desc.key)
                val curr = getEffectiveTowerBalance(desc)
                val defTower = default.getEffectiveTowerBalance(desc)
                if (curr.cost != defTower.cost ||
                    Math.abs(curr.damage - defTower.damage) > 0.01f ||
                    Math.abs(curr.fireRate - defTower.fireRate) > 0.01f ||
                    Math.abs(curr.range - defTower.range) > 0.01f ||
                    Math.abs(curr.specialEffectValue - defTower.specialEffectValue) > 0.01f) {
                    return true
                }
            }
            for ((key, curr) in customTowerConfigs) {
                if (key !in descriptorKeys) {
                    val defTower = DEFAULT_TOWER_BALANCES[key] ?: return true
                    if (curr.cost != defTower.cost ||
                        Math.abs(curr.damage - defTower.damage) > 0.01f ||
                        Math.abs(curr.fireRate - defTower.fireRate) > 0.01f ||
                        Math.abs(curr.range - defTower.range) > 0.01f ||
                        Math.abs(curr.specialEffectValue - defTower.specialEffectValue) > 0.01f) {
                        return true
                    }
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
     * Generates a structured JSON string containing appName, versionName, versionCode,
     * and a "deltas" object containing ONLY values that differ from the stock baseline defaults.
     */
    fun exportDiffJson(
        appName: String = "Dual Front TD",
        versionName: String = com.example.BuildConfig.VERSION_NAME,
        versionCode: Int = com.example.BuildConfig.VERSION_CODE
    ): String {
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
        if (easyTargetOpenSlots != default.easyTargetOpenSlots) diffMap["easyTargetOpenSlots"] = easyTargetOpenSlots
        if (mediumTargetOpenSlots != default.mediumTargetOpenSlots) diffMap["mediumTargetOpenSlots"] = mediumTargetOpenSlots
        if (hardTargetOpenSlots != default.hardTargetOpenSlots) diffMap["hardTargetOpenSlots"] = hardTargetOpenSlots
        if (minLaneOverlap != default.minLaneOverlap) diffMap["minLaneOverlap"] = minLaneOverlap
        if (maxLaneOverlap != default.maxLaneOverlap) diffMap["maxLaneOverlap"] = maxLaneOverlap
        if (mapColumns != default.mapColumns) diffMap["mapColumns"] = mapColumns
        if (mapRows != default.mapRows) diffMap["mapRows"] = mapRows
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

        // Tower diffs across all 52 units (L1 Base, L2 Vectors, L3 Archetypes) + any custom keys
        val towerDiffs = mutableMapOf<String, Map<String, Any>>()
        val allDescriptors = getAll52TowerDescriptors()
        val seenKeys = HashSet<String>(allDescriptors.size)
        for (desc in allDescriptors) {
            seenKeys.add(desc.key)
            val curr = getEffectiveTowerBalance(desc)
            val defTower = default.getEffectiveTowerBalance(desc)
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
        for ((key, curr) in customTowerConfigs) {
            if (key !in seenKeys) {
                val defTower = DEFAULT_TOWER_BALANCES[key]
                val tDiff = mutableMapOf<String, Any>()
                if (!curr.customName.isNullOrBlank() && curr.customName != defTower?.customName) {
                    tDiff["customName"] = curr.customName
                }
                if (defTower == null || Math.abs(curr.damage - defTower.damage) > 0.01f) tDiff["damage"] = curr.damage
                if (defTower == null || Math.abs(curr.range - defTower.range) > 0.01f) tDiff["range"] = curr.range
                if (defTower == null || Math.abs(curr.fireRate - defTower.fireRate) > 0.01f) tDiff["fireRate"] = curr.fireRate
                if (defTower == null || curr.cost != defTower.cost) tDiff["cost"] = curr.cost
                if (defTower == null || Math.abs(curr.specialEffectValue - defTower.specialEffectValue) > 0.01f) {
                    tDiff["specialEffectValue"] = curr.specialEffectValue
                }
                if (tDiff.isNotEmpty()) towerDiffs[key] = tDiff
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

        // Serialize to clean formatted JSON string with appName, versionName, versionCode, and deltas
        val sb = StringBuilder("{\n")
        sb.append("  \"appName\": \"").append(jsonEscape(appName)).append("\",\n")
        sb.append("  \"versionName\": \"").append(jsonEscape(versionName)).append("\",\n")
        sb.append("  \"versionCode\": ").append(versionCode).append(",\n")
        if (diffMap.isEmpty()) {
            sb.append("  \"deltas\": {}\n")
            sb.append("}")
            return sb.toString()
        }
        sb.append("  \"deltas\": {\n")
        val entries = diffMap.entries.toList()
        entries.forEachIndexed { i, (k, v) ->
            sb.append("    \"").append(jsonEscape(k)).append("\": ")
            when (v) {
                is Number, is Boolean -> sb.append(v)
                is String -> sb.append("\"").append(jsonEscape(v)).append("\"")
                is Map<*, *> -> {
                    sb.append("{\n")
                    val subEntries = v.entries.toList()
                    subEntries.forEachIndexed { j, sub ->
                        sb.append("      \"").append(jsonEscape(sub.key.toString())).append("\": ")
                        val subVal = sub.value
                        if (subVal is Map<*, *>) {
                            sb.append("{\n")
                            val leafEntries = subVal.entries.toList()
                            leafEntries.forEachIndexed { kLeaf, leaf ->
                                sb.append("        \"").append(jsonEscape(leaf.key.toString())).append("\": ")
                                val leafVal = leaf.value
                                if (leafVal is String) sb.append("\"").append(jsonEscape(leafVal)).append("\"")
                                else sb.append(leafVal)
                                if (kLeaf < leafEntries.size - 1) sb.append(",")
                                sb.append("\n")
                            }
                            sb.append("      }")
                        } else if (subVal is String) {
                            sb.append("\"").append(jsonEscape(subVal)).append("\"")
                        } else {
                            sb.append(subVal)
                        }
                        if (j < subEntries.size - 1) sb.append(",")
                        sb.append("\n")
                    }
                    sb.append("    }")
                }
                else -> sb.append("\"").append(jsonEscape(v.toString())).append("\"")
            }
            if (i < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  }\n")
        sb.append("}")
        return sb.toString()
    }

    private fun jsonEscape(value: String): String = buildString(value.length + 4) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
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
