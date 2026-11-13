package com.example.model

enum class TowerClass(
    val classId: String,
    val displayName: String,
    val subtitle: String,
    val baseCost: Int,
    val themeColor: Long,
    val description: String
) {
    KINETIC_BALLISTIC(
        classId = "kinetic",
        displayName = "Kinetic Ballistics",
        subtitle = "Basic Munitions & High-Cyclic Turrets",
        baseCost = 80,
        themeColor = 0xFF00F0FF,
        description = "Standard projectile, flak, rotary cannon, and ballistic sentry tech."
    ),
    ELEMENTAL_VOID(
        classId = "elemental",
        displayName = "Elemental Void",
        subtitle = "Cryo Stasis, Arc Shock & Singularity",
        baseCost = 140,
        themeColor = 0xFFA855F7,
        description = "Unlocked after reaching Tier 3. Channels sub-zero freeze, gigawatt arcs, and gravitational singularities."
    ),
    HEAVY_SIEGE(
        classId = "heavy",
        displayName = "Heavy Siege",
        subtitle = "Armor Melter, Anti-Colossus & Artillery",
        baseCost = 210,
        themeColor = 0xFFFF7A00,
        description = "Unlocked after 2 Tier 3 towers. Specializes in destroying high-armor bruisers and boss colossi."
    ),
    STELLAR_COSMIC(
        classId = "stellar",
        displayName = "Stellar Star",
        subtitle = "Cosmic Flare, Singularity & Gravity Warp",
        baseCost = 180,
        themeColor = 0xFFFFD700,
        description = "Unlocked after 3 Tier 3 towers. Manipulates cosmic gravity, supernovas, and event horizons."
    );

    val color: Long get() = themeColor
    val shapeSymbol: String get() = when (this) {
        KINETIC_BALLISTIC -> "●"
        ELEMENTAL_VOID -> "▲"
        HEAVY_SIEGE -> "■"
        STELLAR_COSMIC -> "★"
    }
    val shapeName: String get() = when (this) {
        KINETIC_BALLISTIC -> "Circle"
        ELEMENTAL_VOID -> "Triangle"
        HEAVY_SIEGE -> "Square"
        STELLAR_COSMIC -> "Star"
    }
}

enum class ElementAffinity(val label: String, val colorHex: Long) {
    RED("Red", 0xFFFF2A55),
    GREEN("Green", 0xFF00E676),
    YELLOW("Yellow", 0xFFFFD700);

    fun counters(other: ElementAffinity): Boolean =
        (this == RED && other == GREEN) ||
        (this == GREEN && other == YELLOW) ||
        (this == YELLOW && other == RED)

    fun isWeakTo(other: ElementAffinity): Boolean = other.counters(this)
}

enum class TowerVector(
    val idName: String,
    val displayName: String,
    val role: String,
    val color: Long,
    val description: String
) {
    OFFENSIVE("offensive", "Offensive", "Heavy firepower & burst damage", 0xFFFF2A55, "Focuses on massive damage, rapid cannons, and heavy ordnance"),
    DEFENSIVE("defensive", "Defensive", "Crowd control, stasis & area denial", 0xFF00E676, "Focuses on slowing, trapping, and electrical arcs"),
    SUPPORT("support", "Support", "Targeting uplinks, armor shred & economy", 0xFFFFD700, "Focuses on range uplinks, stripping enemy armor, and credit yields");

    val affinity: ElementAffinity get() = when (this) {
        OFFENSIVE -> ElementAffinity.RED
        DEFENSIVE -> ElementAffinity.GREEN
        SUPPORT -> ElementAffinity.YELLOW
    }
}

/**
 * Returns a distinct unique theme color for each of the 12 Tier 1 vector specializations
 * ensuring zero color collisions across all 52 towers in Fox-Rogue-TD.
 */
fun getTier1VectorColor(towerClass: TowerClass, vector: TowerVector): Long {
    return when (towerClass) {
        TowerClass.KINETIC_BALLISTIC -> when (vector) {
            TowerVector.OFFENSIVE -> 0xFF38BDF8 // Sky Blue
            TowerVector.DEFENSIVE -> 0xFF2DD4BF // Turquoise
            TowerVector.SUPPORT -> 0xFF67E8F9   // Light Aqua
        }
        TowerClass.ELEMENTAL_VOID -> when (vector) {
            TowerVector.OFFENSIVE -> 0xFFD946EF // Neon Magenta
            TowerVector.DEFENSIVE -> 0xFF8B5CF6 // Bright Violet
            TowerVector.SUPPORT -> 0xFFC084FC   // Light Orchid
        }
        TowerClass.HEAVY_SIEGE -> when (vector) {
            TowerVector.OFFENSIVE -> 0xFFEF4444 // Vivid Red
            TowerVector.DEFENSIVE -> 0xFFEA580C // Rust Orange
            TowerVector.SUPPORT -> 0xFFF97316   // Amber Orange
        }
        TowerClass.STELLAR_COSMIC -> when (vector) {
            TowerVector.OFFENSIVE -> 0xFFFBBF24 // Sun Gold
            TowerVector.DEFENSIVE -> 0xFFEAB308 // Deep Yellow
            TowerVector.SUPPORT -> 0xFFFACC15   // Bright Lemon
        }
    }
}

data class AuraOption(
    val name: String,
    val description: String,
    val shortBadge: String
)

enum class TowerType(
    val displayName: String,
    val role: String,
    val baseCost: Int,
    val baseRange: Float, // Grid cell units
    val baseDamage: Float,
    val baseFireRate: Float, // Attacks per second
    val color: Long,
    val isBeam: Boolean = false,
    val isSplash: Boolean = false,
    val splashRadius: Float = 0f,
    val chains: Int = 0,
    val slowPercent: Float = 0f
) {
    GATLING(
        displayName = "Vulcan Minigun",
        role = "Rapid Kinetic Rotary Cannons",
        baseCost = 100,
        baseRange = 2.5f,
        baseDamage = 48f,
        baseFireRate = 1.2f,
        color = 0xFF00F0FF
    ),
    CRYO(
        displayName = "Cryo Glacier",
        role = "Sub-Zero Freezing Laser Spire",
        baseCost = 130,
        baseRange = 2.4f,
        baseDamage = 36f,
        baseFireRate = 1.6f,
        color = 0xFF38BDF8,
        isBeam = true,
        slowPercent = 0.50f
    ),
    TESLA(
        displayName = "Tesla Stormcoil",
        role = "Chaining High-Voltage Lightning Web",
        baseCost = 160,
        baseRange = 2.2f,
        baseDamage = 100f,
        baseFireRate = 0.7f,
        color = 0xFFB026FF,
        chains = 3
    ),
    MORTAR(
        displayName = "Hellfire Mortar",
        role = "Heavy Incendiary Ballistic Artillery",
        baseCost = 200,
        baseRange = 4.2f,
        baseDamage = 210f,
        baseFireRate = 0.40f,
        color = 0xFFFF7A00,
        isSplash = true,
        splashRadius = 1.8f
    ),
    LASER(
        displayName = "Solar Death Ray",
        role = "High-Intensity Thermal Piercing Beam",
        baseCost = 240,
        baseRange = 3.0f,
        baseDamage = 175f,
        baseFireRate = 1.8f,
        color = 0xFFFF0055,
        isBeam = true
    ),
    VOID(
        displayName = "Void Singularity",
        role = "Graviton Vortex & Armor Shredder",
        baseCost = 280,
        baseRange = 2.5f,
        baseDamage = 175f,
        baseFireRate = 0.6f,
        color = 0xFF8B5CF6,
        isSplash = true,
        splashRadius = 2.5f
    ),
    STELLAR(
        displayName = "Stellar Star",
        role = "Cosmic Flare & Gravitational Warp",
        baseCost = 180,
        baseRange = 3.6f,
        baseDamage = 130f,
        baseFireRate = 0.9f,
        color = 0xFFFFD700,
        isSplash = true,
        splashRadius = 2.0f
    )
}

data class Tier1FormDef(
    val branchKey: String, // "A", "B", or "C"
    val subName: String,
    val description: String,
    val cost: Int,
    val damageMultiplier: Float,
    val fireRateMultiplier: Float,
    val rangeMultiplier: Float
)

data class FinalFormDef(
    val code: String, // "A.a", "A.b", "A.c", "B.a", ... "C.c"
    val subName: String,
    val description: String,
    val cost: Int,
    val globalBuffName: String,
    val globalBuffDescription: String,
    val damageMultiplier: Float,
    val fireRateMultiplier: Float,
    val rangeMultiplier: Float,
    val specialTag: String = ""
)

data class TowerUnitBalance(
    val customName: String? = null,
    val damage: Float,
    val range: Float,
    val fireRate: Float,
    val cost: Int,
    val specialEffectValue: Float = 0f // splash radius, slow %, or chains depending on type
)

data class TowerBalanceDescriptor(
    val key: String,
    val displayName: String,
    val level: Int, // 1 = Base (L1), 2 = Specialization (L2), 3 = Apex Final Form (L3)
    val family: TowerType,
    val branchTier1: String? = null,
    val finalFormCode: String? = null,
    val defaultDamage: Float,
    val defaultRange: Float,
    val defaultFireRate: Float,
    val defaultCost: Int,
    val specialEffectValue: Float = 0f
)
