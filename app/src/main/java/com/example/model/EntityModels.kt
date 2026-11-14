package com.example.model

import java.util.UUID

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
    val synergyVault: MutableList<BankedSynergyVault> = mutableListOf(),
    var investedScrap: Int = 0,
    var cachedBalance: TowerUnitBalance? = null
) {
    val isBase: Boolean get() = tier == 0 && branchTier1 == null
    val isTier1: Boolean get() = tier == 1 || (branchTier1 != null && finalFormCode == null && tier == 0)
    val isTier2: Boolean get() = tier == 2
    val isTier3: Boolean get() = tier == 3 || isTier3GlobalActive
    val isFinalForm: Boolean get() = finalFormCode != null || tier == 3 || isTier3GlobalActive
    val isMaxed: Boolean get() = isTier3 || finalFormCode != null
    val isAscended: Boolean get() = isTier3 || finalFormCode != null

    fun invalidateBalanceCache() {
        cachedBalance = null
    }

    fun resolveBalance(balanceConfig: DevBalanceConfig = DevBalanceConfig()): TowerUnitBalance {
        val current = cachedBalance
        if (current != null && balanceConfig.customTowerConfigs.isEmpty()) return current
        val resolved = balanceConfig.getEffectiveTowerBalance(type, branchTier1, finalFormCode)
        if (balanceConfig.customTowerConfigs.isEmpty()) {
            cachedBalance = resolved
        }
        return resolved
    }

    fun getInvestedScrapValue(): Int {
        if (investedScrap > 0) return investedScrap
        return computeFallbackInvestedScrap()
    }

    private fun computeFallbackInvestedScrap(): Int {
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
        val baseRng = balanceRange ?: archetype?.baseRange ?: type.baseRange
        val towerRngMul = if (archetype != null || vector != null || tier > 0) {
            when {
                isTier3 -> 1.35f
                isTier2 -> 1.20f
                vector == TowerVector.SUPPORT || branchTier1 == "C" -> 1.35f
                else -> 1.0f
            }
        } else {
            val ffDef = if (finalFormCode != null) com.example.engine.TowerUpgradeManager.getFinalForm(type, finalFormCode!!) else null
            val t1Def = if (branchTier1 != null) com.example.engine.TowerUpgradeManager.getTier1Form(type, branchTier1!!) else null
            ffDef?.rangeMultiplier ?: t1Def?.rangeMultiplier ?: 1.0f
        }
        val tileRngBonus = if (tileBuff == TileBuff.HIGH_GROUND) 0.35f else 0.0f
        return baseRng * towerRngMul * (1.0f + tileRngBonus + globalRangeBonus)
    }

    fun getEffectiveDamage(
        balanceConfig: DevBalanceConfig = DevBalanceConfig(),
        tileBuff: TileBuff = TileBuff.NONE,
        globalDamageBonus: Float = 0f
    ): Float {
        val tileDmgBonus = if (tileBuff == TileBuff.AMPLIFIER) 0.30f else 0.0f
        val apexBonus = if (isFinalForm) 0.30f else 0.0f
        val arch = archetype
        val (baseDmg, tierDmgBonus) = if (arch != null) {
            val customDmg = balanceConfig.customTowerConfigs[arch.name]?.damage
                ?: balanceConfig.customTowerConfigs[type.name]?.damage
            val resolvedBase = customDmg ?: arch.baseDamage
            val t3Mul = if (tier >= 3 || isTier3GlobalActive) 1.25f else 1.0f
            resolvedBase to t3Mul
        } else {
            val bal = resolveBalance(balanceConfig)
            val tMul = if (branchTier1 == null && finalFormCode == null && (vector != null || tier > 0)) {
                when (tier) {
                    3 -> 1.60f
                    2 -> 1.35f
                    1 -> 1.15f
                    else -> 1.0f
                }
            } else 1.0f
            bal.damage to tMul
        }
        val classDmgMul = when (arch?.towerClass ?: towerClass) {
            TowerClass.KINETIC_BALLISTIC -> balanceConfig.kineticDamageMultiplier
            TowerClass.ELEMENTAL_VOID -> balanceConfig.elementalDamageMultiplier
            TowerClass.HEAVY_SIEGE -> balanceConfig.heavyDamageMultiplier
            TowerClass.STELLAR_COSMIC -> 1.15f
        }
        return baseDmg * tierDmgBonus * classDmgMul * (1.0f + tileDmgBonus + globalDamageBonus + apexBonus)
    }

    fun getEffectiveFireRate(
        balanceConfig: DevBalanceConfig = DevBalanceConfig(),
        tileBuff: TileBuff = TileBuff.NONE,
        globalFireRateBonus: Float = 0f
    ): Float {
        val tileSpdBonus = if (tileBuff == TileBuff.CONDUIT) 0.35f else 0.0f
        val arch = archetype
        val (baseSpd, tierSpdBonus) = if (arch != null) {
            val customSpd = balanceConfig.customTowerConfigs[arch.name]?.fireRate
                ?: balanceConfig.customTowerConfigs[type.name]?.fireRate
            val resolvedBase = customSpd ?: arch.baseFireRate
            val t3Mul = if (tier >= 3 || isTier3GlobalActive) 1.20f else 1.0f
            resolvedBase to t3Mul
        } else {
            val bal = resolveBalance(balanceConfig)
            val tMul = if (branchTier1 == null && finalFormCode == null && (vector != null || tier > 0)) {
                when (tier) {
                    3 -> 1.45f
                    2 -> 1.25f
                    1 -> 1.15f
                    else -> 1.0f
                }
            } else 1.0f
            bal.fireRate to tMul
        }
        return baseSpd * tierSpdBonus * balanceConfig.globalTowerFireRateMultiplier * (1.0f + tileSpdBonus + globalFireRateBonus)
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
    val pathTiers: IntArray = intArrayOf(0, 0, 0)
        get() {
            if (field[0] == 0 && field[1] == 0 && field[2] == 0) {
                if (branchTier1 == "A" || vector == TowerVector.OFFENSIVE) field[0] = if (isFinalForm) 2 else 1
                if (branchTier1 == "B" || vector == TowerVector.DEFENSIVE) field[1] = if (isFinalForm) 2 else 1
                if (branchTier1 == "C" || vector == TowerVector.SUPPORT) field[2] = if (isFinalForm) 2 else 1
            }
            return field
        }

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
                val t1 = com.example.engine.TowerUpgradeManager.getTier1Form(type, branchTier1!!)
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
