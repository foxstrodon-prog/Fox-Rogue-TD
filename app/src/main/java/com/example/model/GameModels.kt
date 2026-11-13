package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

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

data class GridPos(val x: Int, val y: Int)

enum class TileType {
    EMPTY,               // Buildable
    PATH_ALPHA,          // Creep path lane 1
    PATH_BETA,           // Creep path lane 2
    PATH_MERGED,         // Brio train-track merged shared rail segment!
    PATH_INTERSECTION,   // Intersection crossover tile
    OBSTACLE,            // Unbuildable terrain/rock/crystal
    SPAWNER_ALPHA,       // Spawn portal Lane 1
    SPAWNER_BETA,        // Spawn portal Lane 2
    CORE                 // Nexus Core to protect
}

enum class TileBuff(
    val label: String,
    val desc: String,
    val badgeColor: Long,
    val vectorCategory: TowerVector? = null,
    val incomeBonusMultiplier: Float = 0f,
    val damageMultiplier: Float = 1.0f,
    val rangeMultiplier: Float = 1.0f,
    val speedMultiplier: Float = 1.0f
) {
    NONE("", "", 0x00000000, null, 0f, 1.0f, 1.0f, 1.0f),
    AMPLIFIER("GREEN MATRIX (+30% DMG)", "Power Grid: +30% Raw Damage to any turret placed here", 0xFF00E676, TowerVector.OFFENSIVE, 0.10f, 1.30f, 1.0f, 1.0f),
    HIGH_GROUND("TEAL MATRIX (+35% RNG)", "Sensor Perch: +35% Targeting Range to any turret placed here", 0xFF00E5FF, TowerVector.DEFENSIVE, 0.10f, 1.0f, 1.35f, 1.0f),
    CONDUIT("ORANGE MATRIX (+35% SPD)", "Accelerator Node: +35% Attack Speed & +25% Scrap Yield", 0xFFFF9100, TowerVector.SUPPORT, 0.25f, 1.0f, 1.0f, 1.35f)
}

data class MapTile(
    val pos: GridPos,
    val type: TileType,
    val buff: TileBuff = TileBuff.NONE,
    /**
     * Clearance-masked buffer tile (inner corner of a lane turn). These are emitted as
     * [TileType.OBSTACLE] with isClearance = true so towers never visually clip walking creeps.
     */
    val isClearance: Boolean = false
) {
    /** True only for ordinary empty tiles that are not clearance-masked. */
    val isBuildable: Boolean get() = type == TileType.EMPTY && !isClearance
}

data class Biome(
    val name: String,
    val description: String,
    val groundColor: Color,
    val groundSecondary: Color,
    val pathColor: Color,
    val accentColor: Color,
    val secondaryConduitColor: Color,
    val obstacleColor: Color
)

data class TowerInstance(
    val id: String,
    var type: TowerType = TowerType.GATLING,
    val gridPos: GridPos,
    var towerClass: TowerClass = TowerClass.KINETIC_BALLISTIC,
    var tier: Int = 0, // 0 = Base Placement ("Basic Turret"), 1 = Vector ("Offensive Turret"), 2 = Identity Lock ("Vulcan Gatling"), 3 = Global Aura ("Vulcan Gatling [Global Speed Aura]")
    var vector: TowerVector? = null,
    var archetype: TowerArchetype? = null,
    var isTier3GlobalActive: Boolean = false,
    var selectedAuraIndex: Int = 0, // 0 = Primary Aura, 1 = Secondary Aura (2 possible auras per archetype!)
    var branchTier1: String? = null, // Compatibility: "A", "B", or "C"
    var finalFormCode: String? = null, // Compatibility: "A.a".."C.c"
    var kills: Int = 0,
    var totalDamage: Float = 0f,
    var targetPriority: TargetPriority = TargetPriority.FIRST,
    var lastShotTimeMs: Long = 0L,
    var targetEnemyId: String? = null,
    var rotationAngleDeg: Float = 0f,
    var isCapstone: Boolean = false,
    var generation: Int = 0,
    val synergyVault: MutableList<BankedSynergyVault> = mutableListOf()
) {
    val isBase: Boolean get() = tier == 0 && branchTier1 == null
    val isTier1: Boolean get() = tier == 1 || (branchTier1 != null && finalFormCode == null && tier == 0)
    val isTier2: Boolean get() = tier == 2
    val isTier3: Boolean get() = tier == 3 || isTier3GlobalActive
    val isFinalForm: Boolean get() = finalFormCode != null || tier == 3 || isTier3GlobalActive
    val isMaxed: Boolean get() = isTier3 || finalFormCode != null
    val isAscended: Boolean get() = isTier3 || finalFormCode != null

    fun getInvestedScrapValue(): Int {
        val base = type.baseCost
        val t1 = if (tier >= 1 || vector != null || branchTier1 != null) (base * 0.8f).toInt() else 0
        val t2 = if (tier >= 2 || archetype != null || finalFormCode != null) (base * 1.5f).toInt() else 0
        val t3 = if (tier >= 3 || isTier3GlobalActive) 280 else 0
        return base + t1 + t2 + t3
    }

    val shape: EnemyShape get() = when (archetype?.towerClass ?: towerClass) {
        TowerClass.KINETIC_BALLISTIC -> EnemyShape.CIRCLE
        TowerClass.ELEMENTAL_VOID -> EnemyShape.TRIANGLE
        TowerClass.HEAVY_SIEGE -> EnemyShape.SQUARE
        TowerClass.STELLAR_COSMIC -> EnemyShape.STAR
    }

    val affinity: ElementAffinity get() = when (archetype?.vector ?: vector) {
        TowerVector.OFFENSIVE -> ElementAffinity.RED
        TowerVector.DEFENSIVE -> ElementAffinity.GREEN
        TowerVector.SUPPORT -> ElementAffinity.YELLOW
        null -> when (branchTier1) {
            "A" -> ElementAffinity.RED
            "B" -> ElementAffinity.GREEN
            "C" -> ElementAffinity.YELLOW
            else -> when (type) {
                TowerType.GATLING, TowerType.MORTAR -> ElementAffinity.RED
                TowerType.CRYO, TowerType.VOID -> ElementAffinity.GREEN
                TowerType.TESLA, TowerType.LASER, TowerType.STELLAR -> ElementAffinity.YELLOW
            }
        }
    }

    fun getEffectiveCombatRange(
        tileBuff: TileBuff = TileBuff.NONE,
        globalRangeBonus: Float = 0f,
        balanceRange: Float? = null
    ): Float {
        val t1Def = if (branchTier1 != null) com.example.engine.TowerUpgradeManager.getTier1Options(type).firstOrNull { it.branchKey == branchTier1 } else null
        val ffDef = if (finalFormCode != null) com.example.engine.TowerUpgradeManager.getFinalForm(type, finalFormCode!!) else null
        val towerRngMul = ffDef?.rangeMultiplier ?: t1Def?.rangeMultiplier ?: (if (isTier3) 1.35f else if (isTier2) 1.20f else if (branchTier1 == "C" || vector == TowerVector.SUPPORT) 1.35f else 1.0f)
        val baseRng = archetype?.baseRange ?: balanceRange ?: type.baseRange
        val tileRngBonus = if (tileBuff == TileBuff.HIGH_GROUND) 0.35f else 0.0f
        return baseRng * towerRngMul * (1.0f + tileRngBonus + globalRangeBonus)
    }

    fun getEffectiveDamage(
        balanceConfig: DevBalanceConfig = DevBalanceConfig(),
        tileBuff: TileBuff = TileBuff.NONE,
        globalDamageBonus: Float = 0f
    ): Float {
        val bal = balanceConfig.getEffectiveTowerBalance(type, branchTier1, finalFormCode)
        val tileDmgBonus = if (tileBuff == TileBuff.AMPLIFIER) 0.30f else 0.0f
        val apexBonus = if (isFinalForm) 0.30f else 0.0f
        val classDmgMul = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> balanceConfig.kineticDamageMultiplier
            TowerClass.ELEMENTAL_VOID -> balanceConfig.elementalDamageMultiplier
            TowerClass.HEAVY_SIEGE -> balanceConfig.heavyDamageMultiplier
            TowerClass.STELLAR_COSMIC -> 1.0f
        }
        return bal.damage * classDmgMul * (1.0f + tileDmgBonus + globalDamageBonus + apexBonus)
    }

    fun getEffectiveFireRate(
        balanceConfig: DevBalanceConfig = DevBalanceConfig(),
        tileBuff: TileBuff = TileBuff.NONE,
        globalFireRateBonus: Float = 0f
    ): Float {
        val bal = balanceConfig.getEffectiveTowerBalance(type, branchTier1, finalFormCode)
        val tileSpdBonus = if (tileBuff == TileBuff.CONDUIT) 0.35f else 0.0f
        return bal.fireRate * balanceConfig.globalTowerFireRateMultiplier * (1.0f + tileSpdBonus + globalFireRateBonus)
    }

    fun getEffectiveDps(
        balanceConfig: DevBalanceConfig = DevBalanceConfig(),
        tileBuff: TileBuff = TileBuff.NONE,
        globalDamageBonus: Float = 0f,
        globalFireRateBonus: Float = 0f
    ): Float {
        return getEffectiveDamage(balanceConfig, tileBuff, globalDamageBonus) * getEffectiveFireRate(balanceConfig, tileBuff, globalFireRateBonus)
    }

    val activeAura: AuraOption? get() {
        val arch = archetype ?: return null
        return if (selectedAuraIndex == 1) arch.secondaryAura else arch.primaryAura
    }

    // Fallback helpers for tests/compatibility
    val pathTiers: IntArray get() = intArrayOf(
        if (branchTier1 == "A" || vector == TowerVector.OFFENSIVE) (if (isFinalForm) 2 else 1) else 0,
        if (branchTier1 == "B" || vector == TowerVector.DEFENSIVE) (if (isFinalForm) 2 else 1) else 0,
        if (branchTier1 == "C" || vector == TowerVector.SUPPORT) (if (isFinalForm) 2 else 1) else 0
    )

    fun getSellValue(): Int = com.example.engine.TowerUpgradeManager.getSellValue(this)

    fun getDisplayName(): String {
        return when {
            tier == 3 || isTier3GlobalActive -> {
                val arch = archetype ?: mapTypeToArchetype(type)
                val aura = if (selectedAuraIndex == 1) arch.secondaryAura.name else arch.primaryAura.name
                "${arch.displayName} [$aura]"
            }
            tier == 2 -> {
                val arch = archetype ?: mapTypeToArchetype(type)
                arch.displayName
            }
            tier == 1 -> {
                val v = vector ?: when (branchTier1) {
                    "A" -> TowerVector.OFFENSIVE
                    "B" -> TowerVector.DEFENSIVE
                    else -> TowerVector.SUPPORT
                }
                if (towerClass == TowerClass.KINETIC_BALLISTIC) "${v.displayName} Turret" else "${v.displayName} ${towerClass.displayName} Turret"
            }
            finalFormCode != null -> {
                val ff = com.example.engine.TowerUpgradeManager.getFinalForm(type, finalFormCode!!)
                if (ff != null) "${type.displayName} - ${ff.subName}" else type.displayName
            }
            branchTier1 != null -> {
                val t1 = com.example.engine.TowerUpgradeManager.getTier1Options(type).firstOrNull { it.branchKey == branchTier1 }
                if (t1 != null) "${type.displayName} - ${t1.subName}" else type.displayName
            }
            else -> when (towerClass) {
                TowerClass.KINETIC_BALLISTIC -> "Basic Turret"
                TowerClass.ELEMENTAL_VOID -> "Elemental Conduit"
                TowerClass.HEAVY_SIEGE -> "Heavy Siege Platform"
                TowerClass.STELLAR_COSMIC -> "Stellar Star Node"
            }
        }
    }
}

data class TrackParticle(
    val id: String,
    val laneIndex: Int,
    var segmentIndex: Int,
    var progress: Float,
    val speed: Float,
    val color: Long,
    val size: Float
)

data class EnemyInstance(
    val id: String,
    val type: EnemyType,
    val laneIndex: Int, // 0 = Lane Alpha, 1 = Lane Beta
    var currentPathSegmentIndex: Int = 0,
    var segmentProgress: Float = 0f,
    val maxHp: Float,
    var currentHp: Float,
    val speed: Float,
    var slowTimerMs: Long = 0L,
    var slowFactor: Float = 1.0f,
    var shockTimerMs: Long = 0L,
    var worldX: Float = 0f,
    var worldY: Float = 0f,
    val isBoss: Boolean = false,
    /** Direction of travel in radians (atan2(dy, dx) in grid space); used to orient triangles/squares. */
    var headingRad: Float = 0f,
    /** Per-tick transient: speed multiplier granted by an adjacent HASTE support unit (1 = none). */
    var auraSpeedMul: Float = 1f,
    /** Per-tick transient: fractional damage reduction granted by an adjacent AEGIS support unit. */
    var auraDamageReduction: Float = 0f,
    /** Per-tick transient: true while inside any friendly support aura (for rendering). */
    var isAuraBuffed: Boolean = false
) {
    val isDead: Boolean get() = currentHp <= 0f
    val effectiveSpeed: Float
        get() = (if (slowTimerMs > 0L) speed * maxOf(0.15f, slowFactor) else speed) * auraSpeedMul
}

/**
 * Pooled, mutable projectile. Instances are owned and recycled by the ViewModel simulation;
 * UI code must treat them as read-only and must not retain references across frames.
 */
data class Projectile(
    val id: String = UUID.randomUUID().toString(),
    val startX: Float = 0f,
    val startY: Float = 0f,
    var currentX: Float = 0f,
    var currentY: Float = 0f,
    val targetEnemyId: String? = null,
    val targetX: Float = 0f,
    val targetY: Float = 0f,
    val speed: Float = 1f,
    val damage: Float = 0f,
    val color: Long = 0xFFFFFFFF,
    val isSplash: Boolean = false,
    val splashRadius: Float = 0f,
    val isMortarArc: Boolean = false,
    val totalDistance: Float = 1f,
    var traveledDistance: Float = 0f,
    val isCrit: Boolean = false,
    val hasClusterSplit: Boolean = false,
    val towerClassIndex: Int = 0,
    val sourceShape: EnemyShape = EnemyShape.CIRCLE,
    val sourceAffinity: ElementAffinity = ElementAffinity.RED
)

data class LaserBeam(
    val id: String = UUID.randomUUID().toString(),
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val color: Long = 0xFFFFFFFF,
    var lifeMs: Long = 120L
)

data class LightningArc(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Pair<Float, Float>> = emptyList(),
    val color: Long = 0xFFB026FF,
    var lifeMs: Long = 180L
)

data class ExplosionEffect(
    val id: String = UUID.randomUUID().toString(),
    val x: Float = 0f,
    val y: Float = 0f,
    val maxRadius: Float = 1f,
    var currentRadius: Float = 0f,
    val color: Long = 0xFFFFFFFF,
    var lifeMs: Long = 250L,
    val maxLifeMs: Long = 250L
)

data class ShockwaveRing(
    val id: String = UUID.randomUUID().toString(),
    val x: Float = 0f,
    val y: Float = 0f,
    val maxRadius: Float = 2f,
    val color: Long = 0xFFFFFFFF,
    var lifeMs: Long = 600L,
    val maxLifeMs: Long = 600L
) {
    val progress: Float get() = if (maxLifeMs <= 0L) 1f else (1f - lifeMs.toFloat() / maxLifeMs.toFloat()).coerceIn(0f, 1f)
}

data class FloatingText(
    val id: String = UUID.randomUUID().toString(),
    var x: Float = 0f,
    var y: Float = 0f,
    val text: String = "",
    val color: Long = 0xFFFFFFFF,
    var lifeMs: Long = 750L,
    val maxLifeMs: Long = 750L,
    val isCrit: Boolean = false
)

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

enum class TowerDrawStyle { NEON_GLOW, BLUEPRINT_LINE, SOLID_MILITARY, VOID_RING, PIXEL_BLOCK }
enum class ProjectileStyle { ORB, TRACER, SHELL, DIAMOND, PIXEL }
enum class HudStyle { GLASS, OUTLINE, PANEL, VOID_GLOW, PIXEL }
enum class MapStyle { GLOW_CONDUIT, BLUEPRINT, ROAD, VOID_RIBBON, PIXEL_TILES }

/**
 * Full procedural palette + draw-style definition for one theme.
 * Covers (a) UI HUD, (b) map grid/paths, (c) Tower 1 Kinetic, (d) Tower 2 Elemental, (e) Tower 3 Siege.
 * All colours are packed ARGB longs (use Color(value)).
 */
data class ThemeSkinConfig(
    val id: String,
    val displayName: String,
    val description: String,
    // (b) Map grid / paths
    /** When true the per-run Biome palette is used for ground/path colours (Cyberpunk Neon only). */
    val useBiomePalette: Boolean,
    val mapStyle: MapStyle,
    val ground: Long,
    val groundAlt: Long,
    val gridLine: Long,
    val pathBase: Long,
    val laneAlpha: Long,
    val laneBeta: Long,
    val laneMerged: Long,
    val obstacle: Long,
    val clearanceMark: Long,
    // (a) HUD
    val hudStyle: HudStyle,
    val hudCornerDp: Int,
    val hudBackground: Long,
    val hudSurface: Long,
    val hudBorder: Long,
    val hudAccent: Long,
    val hudText: Long,
    val hudTextDim: Long,
    // (c)(d)(e) Towers
    /** When false, towers keep their per-type/archetype colours (Cyberpunk Neon). */
    val overrideTowerColors: Boolean,
    val kineticColor: Long,
    val elementalColor: Long,
    val siegeColor: Long,
    val kineticStyle: TowerDrawStyle,
    val elementalStyle: TowerDrawStyle,
    val siegeStyle: TowerDrawStyle,
    // Juice
    val projectileStyle: ProjectileStyle,
    val shockwaveColor: Long,
    /** Square (pixel) shockwave instead of round ring. */
    val shockwaveSquare: Boolean = false
) {
    fun towerColor(cls: TowerClass): Long = when (cls) {
        TowerClass.KINETIC_BALLISTIC -> kineticColor
        TowerClass.ELEMENTAL_VOID -> elementalColor
        TowerClass.HEAVY_SIEGE -> siegeColor
        TowerClass.STELLAR_COSMIC -> 0xFFFFD700
    }

    fun towerStyle(cls: TowerClass): TowerDrawStyle = when (cls) {
        TowerClass.KINETIC_BALLISTIC -> kineticStyle
        TowerClass.ELEMENTAL_VOID -> elementalStyle
        TowerClass.HEAVY_SIEGE -> siegeStyle
        TowerClass.STELLAR_COSMIC -> kineticStyle
    }
}

enum class SkinTheme(val config: ThemeSkinConfig) {
    CYBERPUNK_NEON(
        ThemeSkinConfig(
            id = "cyberpunk_neon", displayName = "Cyberpunk Neon",
            description = "Glowing conduits, biome-driven ground, neon towers.",
            useBiomePalette = true, mapStyle = MapStyle.GLOW_CONDUIT,
            ground = 0xFF090E17, groundAlt = 0xFF111C2E, gridLine = 0xFF111C2E, pathBase = 0xFF1E293B,
            laneAlpha = 0xFF00F0FF, laneBeta = 0xFFFFB800, laneMerged = 0xFFC084FC,
            obstacle = 0xFF1E3A5F, clearanceMark = 0xFF1E293B,
            hudStyle = HudStyle.GLASS, hudCornerDp = 10,
            hudBackground = 0xFF070B14, hudSurface = 0xFF161F30, hudBorder = 0xFF1E293B,
            hudAccent = 0xFF00F0FF, hudText = 0xFFFFFFFF, hudTextDim = 0xFF94A3B8,
            overrideTowerColors = false,
            kineticColor = 0xFF00F0FF, elementalColor = 0xFFA855F7, siegeColor = 0xFFFF7A00,
            kineticStyle = TowerDrawStyle.NEON_GLOW, elementalStyle = TowerDrawStyle.NEON_GLOW, siegeStyle = TowerDrawStyle.NEON_GLOW,
            projectileStyle = ProjectileStyle.ORB, shockwaveColor = 0xFF00F0FF
        )
    ),
    MONOCHROME_BLUEPRINT(
        ThemeSkinConfig(
            id = "monochrome_blueprint", displayName = "Monochrome Blueprint",
            description = "Technical drawing: white linework on blueprint blue.",
            useBiomePalette = false, mapStyle = MapStyle.BLUEPRINT,
            ground = 0xFF0B2545, groundAlt = 0xFF13315C, gridLine = 0xFF3E6A9E, pathBase = 0xFF134074,
            laneAlpha = 0xFFEEF4ED, laneBeta = 0xFFB7C9E2, laneMerged = 0xFFFFFFFF,
            obstacle = 0xFF1B4079, clearanceMark = 0xFF3E6A9E,
            hudStyle = HudStyle.OUTLINE, hudCornerDp = 2,
            hudBackground = 0xFF0B2545, hudSurface = 0xFF13315C, hudBorder = 0xFF8DA9C4,
            hudAccent = 0xFFEEF4ED, hudText = 0xFFEEF4ED, hudTextDim = 0xFF8DA9C4,
            overrideTowerColors = true,
            kineticColor = 0xFFEEF4ED, elementalColor = 0xFFB7C9E2, siegeColor = 0xFF8DA9C4,
            kineticStyle = TowerDrawStyle.BLUEPRINT_LINE, elementalStyle = TowerDrawStyle.BLUEPRINT_LINE, siegeStyle = TowerDrawStyle.BLUEPRINT_LINE,
            projectileStyle = ProjectileStyle.TRACER, shockwaveColor = 0xFFEEF4ED
        )
    ),
    SOLARIZED_MILITARY(
        ThemeSkinConfig(
            id = "solarized_military", displayName = "Solarized Military",
            description = "Olive drab terrain, dirt roads, brass and rust hardware.",
            useBiomePalette = false, mapStyle = MapStyle.ROAD,
            ground = 0xFF1F2A1C, groundAlt = 0xFF2A3826, gridLine = 0xFF3C4B30, pathBase = 0xFF5C4A32,
            laneAlpha = 0xFFD9A441, laneBeta = 0xFF8FB339, laneMerged = 0xFFE8D8A0,
            obstacle = 0xFF4B5D3A, clearanceMark = 0xFF3C4B30,
            hudStyle = HudStyle.PANEL, hudCornerDp = 4,
            hudBackground = 0xFF1B2318, hudSurface = 0xFF2A3826, hudBorder = 0xFF6B7F4F,
            hudAccent = 0xFFD9A441, hudText = 0xFFEDE6C8, hudTextDim = 0xFFA3A98A,
            overrideTowerColors = true,
            kineticColor = 0xFFC8B560, elementalColor = 0xFF5FA8A0, siegeColor = 0xFFB5651D,
            kineticStyle = TowerDrawStyle.SOLID_MILITARY, elementalStyle = TowerDrawStyle.SOLID_MILITARY, siegeStyle = TowerDrawStyle.SOLID_MILITARY,
            projectileStyle = ProjectileStyle.SHELL, shockwaveColor = 0xFFD9A441
        )
    ),
    DEEP_VOID(
        ThemeSkinConfig(
            id = "deep_void", displayName = "Deep Void",
            description = "Starless dark matter ribbons with orbiting ring turrets.",
            useBiomePalette = false, mapStyle = MapStyle.VOID_RIBBON,
            ground = 0xFF05030D, groundAlt = 0xFF0B0720, gridLine = 0xFF1B1140, pathBase = 0xFF120B2E,
            laneAlpha = 0xFFA855F7, laneBeta = 0xFF22D3EE, laneMerged = 0xFFF0ABFC,
            obstacle = 0xFF2E1065, clearanceMark = 0xFF1B1140,
            hudStyle = HudStyle.VOID_GLOW, hudCornerDp = 18,
            hudBackground = 0xFF05030D, hudSurface = 0xFF120B2E, hudBorder = 0xFF4C1D95,
            hudAccent = 0xFFC084FC, hudText = 0xFFEDE9FE, hudTextDim = 0xFF8B7FB8,
            overrideTowerColors = true,
            kineticColor = 0xFF7DD3FC, elementalColor = 0xFFE879F9, siegeColor = 0xFFFB7185,
            kineticStyle = TowerDrawStyle.VOID_RING, elementalStyle = TowerDrawStyle.VOID_RING, siegeStyle = TowerDrawStyle.VOID_RING,
            projectileStyle = ProjectileStyle.DIAMOND, shockwaveColor = 0xFFC084FC
        )
    ),
    RETRO_ARCADE(
        ThemeSkinConfig(
            id = "retro_arcade", displayName = "Retro Arcade",
            description = "8-bit palette, blocky pixel tiles and square bullets.",
            useBiomePalette = false, mapStyle = MapStyle.PIXEL_TILES,
            ground = 0xFF0A0A12, groundAlt = 0xFF14142B, gridLine = 0xFF2A2A5A, pathBase = 0xFF1C1C3C,
            laneAlpha = 0xFF39FF14, laneBeta = 0xFFFF3EA5, laneMerged = 0xFFFFFF00,
            obstacle = 0xFF3A3A7A, clearanceMark = 0xFF2A2A5A,
            hudStyle = HudStyle.PIXEL, hudCornerDp = 0,
            hudBackground = 0xFF000000, hudSurface = 0xFF111122, hudBorder = 0xFF39FF14,
            hudAccent = 0xFFFFFF00, hudText = 0xFFFFFFFF, hudTextDim = 0xFF9999CC,
            overrideTowerColors = true,
            kineticColor = 0xFF39FF14, elementalColor = 0xFFFF3EA5, siegeColor = 0xFFFFFF00,
            kineticStyle = TowerDrawStyle.PIXEL_BLOCK, elementalStyle = TowerDrawStyle.PIXEL_BLOCK, siegeStyle = TowerDrawStyle.PIXEL_BLOCK,
            projectileStyle = ProjectileStyle.PIXEL, shockwaveColor = 0xFFFFFF00, shockwaveSquare = true
        )
    );

    val displayName: String get() = config.displayName

    companion object {
        fun fromId(id: String?): SkinTheme = values().firstOrNull { it.config.id == id } ?: CYBERPUNK_NEON
    }
}

/** 5 selectable visual skins for turrets (1 Default + 4 Candidates) */
enum class TurretSkinSlot(val slot: Int, val displayName: String, val description: String) {
    SLOT_1_DEFAULT(1, "Vector Cyber (Default)", "Sleek glowing vector geometry and rotary/crystal emitters"),
    SLOT_2_BLUEPRINT(2, "Monochrome Blueprint", "Technical drafting lines, measurement crosshairs, and white schematics"),
    SLOT_3_MILITARY(3, "Solarized Military", "Armored composite plating, heavy bolted rivets, and reinforced brass barrels"),
    SLOT_4_VOID(4, "Deep Void", "Ethereal dark-matter chassis, floating halo rings, and cosmic void motes"),
    SLOT_5_PIXEL(5, "Retro Pixel Arcade", "Crisp 8-bit faceted pixel geometry and high-contrast arcade borders");

    companion object {
        fun fromSlot(slot: Int): TurretSkinSlot = values().firstOrNull { it.slot == slot } ?: SLOT_1_DEFAULT
    }
}

/** 5 selectable environment themes for map grid and lane pathways (1 Default + 4 Candidates) */
enum class MapThemeSlot(val slot: Int, val displayName: String, val description: String) {
    SLOT_1_DEFAULT(1, "Cyberpunk Grid (Default)", "Dark slate terrain with glowing neon cyan/amber energy conduits"),
    SLOT_2_BLUEPRINT(2, "Blueprint Schematic", "Technical blueprint deep navy with crisp white drafting pathways"),
    SLOT_3_OUTPOST(3, "Solarized Outpost", "Tactical olive terrain with dirt-road asphalt pathways and amber hazard lines"),
    SLOT_4_VOID(4, "Void Rift", "Starless dark-matter ground with radiant violet/magenta dimensional pathways"),
    SLOT_5_MATRIX(5, "Retro Matrix", "Arcade midnight black surface with glowing green/yellow phosphor conduits");

    companion object {
        fun fromSlot(slot: Int): MapThemeSlot = values().firstOrNull { it.slot == slot } ?: SLOT_1_DEFAULT
    }
}

/** 5 selectable gunfire audio variants per archetype (1 Default + 4 Candidates) */
enum class TurretSfxSlot(val slot: Int, val kineticName: String, val elementalName: String, val siegeName: String) {
    SLOT_1_DEFAULT(1, "Rotary Gunfire (Default)", "Phase Beam (Default)", "Artillery Blast (Default)"),
    SLOT_2_VARIANT_A(2, "Heavy Autocannon", "Plasma Burn", "Sonic Detonator"),
    SLOT_3_VARIANT_B(3, "Hyper Pulse", "Ion Discharge", "Flak Burst"),
    SLOT_4_VARIANT_C(4, "Gauss Needle", "Void Pulse", "Concussion Mortar"),
    SLOT_5_VARIANT_D(5, "Rail Crack", "Arc Flare", "Plasma Howitzer");

    companion object {
        fun fromSlot(slot: Int): TurretSfxSlot = values().firstOrNull { it.slot == slot } ?: SLOT_1_DEFAULT
    }
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

