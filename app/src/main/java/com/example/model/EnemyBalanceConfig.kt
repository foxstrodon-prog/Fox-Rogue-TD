package com.example.model

/** Visual-grammar role of an enemy: drives colour coding and (for SUPPORT) aura behaviour. */
enum class EnemyRole(val label: String, val primaryColor: Long, val accentColor: Long, val tag: String) {
    ATTACK("Attack", 0xFFFF3B30, 0xFFFF8A00, "ATK"),      // Aggressive crimson / orange
    DEFENSE("Defense", 0xFF2F6BFF, 0xFF22D3EE, "DEF"),    // Cobalt / shielded cyan
    SUPPORT("Support", 0xFF10B981, 0xFFFBBF24, "SUP")     // Emerald / amber
}

/** Geometric archetype of an enemy: Circle = swarm, Triangle = striker, Square = bruiser, Star = stellar. */
enum class EnemyShape(val label: String, val hitboxScale: Float) {
    CIRCLE("Swarm", 1.00f),
    TRIANGLE("Striker", 0.90f),
    SQUARE("Bruiser", 1.20f),
    STAR("Stellar", 1.10f)
}

/** Aura behaviour projected by SUPPORT-role enemies onto allies in range. */
enum class SupportKind { NONE, HEAL, HASTE, AEGIS }

enum class EnemyType(
    val displayName: String,
    val baseHp: Float,
    val baseSpeed: Float,
    val baseArmor: Float,
    val scrapValue: Int,
    val color: Long,
    val sizeRatio: Float,
    val role: EnemyRole = EnemyRole.ATTACK,
    val shape: EnemyShape = EnemyShape.CIRCLE,
    val elementAffinity: ElementAffinity = ElementAffinity.RED,
    val supportKind: SupportKind = SupportKind.NONE,
    val auraRadius: Float = 0f
) {
    DRONE(
        displayName = "Scuttle Drone",
        baseHp = 45f,
        baseSpeed = 1.60f,
        baseArmor = 0f,
        scrapValue = 5,
        color = 0xFFFF3B30,
        sizeRatio = 0.55f,
        role = EnemyRole.ATTACK,
        shape = EnemyShape.CIRCLE,
        elementAffinity = ElementAffinity.RED
    ),
    SOLDIER(
        displayName = "Automaton",
        baseHp = 115f,
        baseSpeed = 1.15f,
        baseArmor = 0.10f,
        scrapValue = 8,
        color = 0xFF22D3EE,
        sizeRatio = 0.75f,
        role = EnemyRole.DEFENSE,
        shape = EnemyShape.CIRCLE,
        elementAffinity = ElementAffinity.GREEN
    ),
    SPEEDSTER(
        displayName = "Phase Scout",
        baseHp = 75f,
        baseSpeed = 2.05f,
        baseArmor = 0.05f,
        scrapValue = 10,
        color = 0xFFFF8A00,
        sizeRatio = 0.60f,
        role = EnemyRole.ATTACK,
        shape = EnemyShape.TRIANGLE,
        elementAffinity = ElementAffinity.YELLOW
    ),
    BRUISER(
        displayName = "Siege Golem",
        baseHp = 420f,
        baseSpeed = 0.72f,
        baseArmor = 0.35f,
        scrapValue = 20,
        color = 0xFF2F6BFF,
        sizeRatio = 0.95f,
        role = EnemyRole.DEFENSE,
        shape = EnemyShape.SQUARE,
        elementAffinity = ElementAffinity.GREEN
    ),
    DISRUPTOR(
        displayName = "Shield Disruptor",
        baseHp = 240f,
        baseSpeed = 0.98f,
        baseArmor = 0.20f,
        scrapValue = 18,
        color = 0xFF10B981,
        sizeRatio = 0.85f,
        role = EnemyRole.SUPPORT,
        shape = EnemyShape.SQUARE,
        elementAffinity = ElementAffinity.GREEN,
        supportKind = SupportKind.AEGIS,
        auraRadius = 1.6f
    ),
    BOSS(
        displayName = "Vanguard Titan",
        baseHp = 2000f,
        baseSpeed = 0.58f,
        baseArmor = 0.30f,
        scrapValue = 90,
        color = 0xFF2F6BFF,
        sizeRatio = 1.40f,
        role = EnemyRole.DEFENSE,
        shape = EnemyShape.SQUARE,
        elementAffinity = ElementAffinity.RED
    ),
    MEDIC(
        displayName = "Repair Wisp",
        baseHp = 150f,
        baseSpeed = 1.05f,
        baseArmor = 0.05f,
        scrapValue = 15,
        color = 0xFF10B981,
        sizeRatio = 0.70f,
        role = EnemyRole.SUPPORT,
        shape = EnemyShape.CIRCLE,
        elementAffinity = ElementAffinity.GREEN,
        supportKind = SupportKind.HEAL,
        auraRadius = 1.7f
    ),
    RALLIER(
        displayName = "Haste Beacon",
        baseHp = 130f,
        baseSpeed = 1.20f,
        baseArmor = 0.05f,
        scrapValue = 15,
        color = 0xFFFBBF24,
        sizeRatio = 0.70f,
        role = EnemyRole.SUPPORT,
        shape = EnemyShape.TRIANGLE,
        elementAffinity = ElementAffinity.YELLOW,
        supportKind = SupportKind.HASTE,
        auraRadius = 1.9f
    ),
    STELLAR_PROBE(
        displayName = "Stellar Probe",
        baseHp = 85f,
        baseSpeed = 1.45f,
        baseArmor = 0.15f,
        scrapValue = 12,
        color = 0xFFFFD700,
        sizeRatio = 0.65f,
        role = EnemyRole.ATTACK,
        shape = EnemyShape.STAR,
        elementAffinity = ElementAffinity.YELLOW
    ),
    COSMIC_WEAVER(
        displayName = "Cosmic Weaver",
        baseHp = 210f,
        baseSpeed = 1.05f,
        baseArmor = 0.25f,
        scrapValue = 20,
        color = 0xFFF59E0B,
        sizeRatio = 0.85f,
        role = EnemyRole.SUPPORT,
        shape = EnemyShape.STAR,
        elementAffinity = ElementAffinity.RED,
        supportKind = SupportKind.HASTE,
        auraRadius = 1.8f
    ),
    ASTRAL_COLOSSUS(
        displayName = "Astral Colossus",
        baseHp = 680f,
        baseSpeed = 0.65f,
        baseArmor = 0.40f,
        scrapValue = 50,
        color = 0xFFA855F7,
        sizeRatio = 1.05f,
        role = EnemyRole.DEFENSE,
        shape = EnemyShape.STAR,
        elementAffinity = ElementAffinity.GREEN
    );

    /** Collision / targeting radius in grid cells (shape-aware, distinct per archetype). */
    val hitRadius: Float get() = sizeRatio * 0.35f * shape.hitboxScale
}

data class EnemyUnitBalance(
    val customName: String? = null,
    val hp: Float,
    val speed: Float,
    val armor: Float,
    val scrapValue: Int
)
