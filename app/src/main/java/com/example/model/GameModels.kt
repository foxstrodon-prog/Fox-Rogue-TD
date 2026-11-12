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

enum class TowerArchetype(
    val archetypeId: String,
    val towerClass: TowerClass,
    val vector: TowerVector,
    val displayName: String,
    val role: String,
    val baseCost: Int,
    val baseRange: Float,
    val baseDamage: Float,
    val baseFireRate: Float,
    val color: Long,
    val primaryAura: AuraOption,
    val secondaryAura: AuraOption,
    val isBeam: Boolean = false,
    val isSplash: Boolean = false,
    val splashRadius: Float = 0f,
    val chains: Int = 0,
    val slowPercent: Float = 0f,
    val armorShredPercent: Float = 0f
) {
    // ==========================================
    // CLASS 1: KINETIC BALLISTICS (9 Archetypes)
    // ==========================================
    // Offensive (3)
    VULCAN_GATLING(
        archetypeId = "vulcan_gatling",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.OFFENSIVE,
        displayName = "Vulcan Gatling",
        role = "Rapid Kinetic Rotary Cannons",
        baseCost = 130,
        baseRange = 2.6f,
        baseDamage = 26f,
        baseFireRate = 4.4f,
        color = 0xFF06B6D4,
        primaryAura = AuraOption("Global Speed Aura", "+18% Attack Speed to all towers", "+18% SPD"),
        secondaryAura = AuraOption("Kinetic Windup Aura", "Towers gain ramping +25% attack speed during continuous firing", "RAMP SPD")
    ),
    FLAK_CANNON(
        archetypeId = "flak_cannon",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.OFFENSIVE,
        displayName = "Twin Flak Cannon",
        role = "Shrapnel Burst Anti-Air & Ground",
        baseCost = 150,
        baseRange = 3.0f,
        baseDamage = 54f,
        baseFireRate = 2.2f,
        color = 0xFFFF5500,
        primaryAura = AuraOption("Shrapnel Aura", "All projectile weapons deal +20% micro-splash damage", "+20% SPLASH"),
        secondaryAura = AuraOption("Flak Barrage Aura", "15% chance for bullets to trigger secondary concussive bursts", "BURST")
    ),
    SNIPER_SENTRY(
        archetypeId = "sniper_sentry",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.OFFENSIVE,
        displayName = "Sniper Sentry",
        role = "Long-Range High-Caliber Penetrator",
        baseCost = 170,
        baseRange = 4.6f,
        baseDamage = 130f,
        baseFireRate = 0.9f,
        color = 0xFF818CF8,
        primaryAura = AuraOption("Execution Aura", "+25% Damage against enemies below 40% health", "+25% EXEC"),
        secondaryAura = AuraOption("Headhunter Aura", "Critical hits award +4 bonus Credits", "+4 CRIT CR")
    ),

    // Defensive (3)
    BARRICADE_TURRET(
        archetypeId = "barricade_turret",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.DEFENSIVE,
        displayName = "Barricade Turret",
        role = "Suppression Fire & Kinetic Concussion",
        baseCost = 120,
        baseRange = 2.4f,
        baseDamage = 32f,
        baseFireRate = 2.8f,
        color = 0xFF0284C7,
        primaryAura = AuraOption("Suppression Aura", "Hostiles entering weapon range are pushed back 0.5 tiles", "KNOCKBACK"),
        secondaryAura = AuraOption("Fortified Plating", "Nexus Core gains +5 Max HP and absorbs 2 free breaches", "+5 CORE HP")
    ),
    MINE_DISPENSER(
        archetypeId = "mine_dispenser",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.DEFENSIVE,
        displayName = "Mine Dispenser",
        role = "Deploys Proximity Mines On Lanes",
        baseCost = 140,
        baseRange = 2.8f,
        baseDamage = 75f,
        baseFireRate = 1.2f,
        color = 0xFFF59E0B,
        primaryAura = AuraOption("Minefield Net", "Automatically scatters 2 explosive traps along lanes each wave", "MINEFIELD"),
        secondaryAura = AuraOption("Caltrop Grid", "Slows all creeps traversing merged rail sections by 20%", "CALTROPS")
    ),
    RIOT_DISRUPTOR(
        archetypeId = "riot_disruptor",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.DEFENSIVE,
        displayName = "Riot Disruptor",
        role = "Acoustic Concussion Shockwaves",
        baseCost = 160,
        baseRange = 2.3f,
        baseDamage = 45f,
        baseFireRate = 2.0f,
        color = 0xFF059669,
        primaryAura = AuraOption("Concussive Aura", "All towers have a 12% chance to stun targets for 1.2s", "STUN 12%"),
        secondaryAura = AuraOption("Sonic Buffer", "Hostiles in both lanes lose 15% movement speed permanently", "-15% MOVE")
    ),

    // Support (3)
    RADAR_SENTRY(
        archetypeId = "radar_sentry",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.SUPPORT,
        displayName = "Radar Sentry",
        role = "Tactical Sensor Array & Range Uplink",
        baseCost = 120,
        baseRange = 4.2f,
        baseDamage = 35f,
        baseFireRate = 2.2f,
        color = 0xFF10B981,
        primaryAura = AuraOption("Global Targeting Aura", "+25% Range to all defense towers", "+25% RNG"),
        secondaryAura = AuraOption("Spotter Uplink", "Reveals enemy weak points, granting +20% Critical Strike Chance", "+20% CRIT")
    ),
    AMMO_SUPPLY_DEPOT(
        archetypeId = "ammo_supply_depot",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.SUPPORT,
        displayName = "Ammo Depot",
        role = "Logistical Feeder & Recoil Stabilizer",
        baseCost = 140,
        baseRange = 2.5f,
        baseDamage = 28f,
        baseFireRate = 2.5f,
        color = 0xFF94A3B8,
        primaryAura = AuraOption("Munitions Logistics", "Reduces all tower upgrade and placement costs by 15%", "-15% COST"),
        secondaryAura = AuraOption("High-Velocity Powder", "+20% Projectile flight speed and +15% kinetic base damage", "+15% DMG")
    ),
    SALVAGE_CRUSHER(
        archetypeId = "salvage_crusher",
        towerClass = TowerClass.KINETIC_BALLISTIC,
        vector = TowerVector.SUPPORT,
        displayName = "Salvage Crusher",
        role = "Scrap Recycler & Bounty Extractor",
        baseCost = 160,
        baseRange = 2.6f,
        baseDamage = 40f,
        baseFireRate = 2.0f,
        color = 0xFFE879F9,
        primaryAura = AuraOption("Scrap Bounty Aura", "+35% Scrap Credits awarded from all creep kills", "+35% SCRAP"),
        secondaryAura = AuraOption("Material Recycling", "Defeated boss/elite creeps drop +100 bonus Credits", "+100 BOSS CR")
    ),

    // ==========================================
    // CLASS 2: ELEMENTAL VOID (9 Archetypes)
    // ==========================================
    // Offensive (3)
    SOLAR_DEATH_RAY(
        archetypeId = "solar_death_ray",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.OFFENSIVE,
        displayName = "Solar Death Ray",
        role = "Thermal Piercing High-Intensity Beam",
        baseCost = 190,
        baseRange = 3.2f,
        baseDamage = 85f,
        baseFireRate = 4.6f,
        color = 0xFFFB7185,
        primaryAura = AuraOption("Solar Focus Aura", "+25% Critical Strike Chance & 2.5x Crit Damage", "2.5x CRIT"),
        secondaryAura = AuraOption("Thermal Dissolution", "Laser weapons melt and bypass 100% of creep armor", "MELT ARMOR"),
        isBeam = true
    ),
    INFERNO_CANNON(
        archetypeId = "inferno_cannon",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.OFFENSIVE,
        displayName = "Inferno Emitter",
        role = "Magma Stream & Thermal Burn DoT",
        baseCost = 175,
        baseRange = 2.8f,
        baseDamage = 60f,
        baseFireRate = 3.6f,
        color = 0xFFFF4500,
        primaryAura = AuraOption("Ignition Aura", "Damaged creeps suffer 15% damage/sec burning for 4 seconds", "BURN DoT"),
        secondaryAura = AuraOption("Thermal Conflagration", "Burning enemies explode on death for 50 AoE fire damage", "FIRE EXPLODE")
    ),
    PLASMA_PULSAR(
        archetypeId = "plasma_pulsar",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.OFFENSIVE,
        displayName = "Plasma Pulsar",
        role = "Ionized Plasma Spheres Down Lanes",
        baseCost = 185,
        baseRange = 3.0f,
        baseDamage = 95f,
        baseFireRate = 1.6f,
        color = 0xFFC026D3,
        primaryAura = AuraOption("Plasma Cascade", "Periodically fires automated plasma waves down both lanes", "PLASMA WAVE"),
        secondaryAura = AuraOption("Ion Overload", "+30% Damage to all beam and plasma towers", "+30% PLASMA")
    ),

    // Defensive (3)
    FROST_SPIRE(
        archetypeId = "frost_spire",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.DEFENSIVE,
        displayName = "Frost Spire",
        role = "Sub-Zero Freezing Laser Spire",
        baseCost = 160,
        baseRange = 2.6f,
        baseDamage = 26f,
        baseFireRate = 5.0f,
        color = 0xFF7DD3FC,
        primaryAura = AuraOption("Permafrost Aura", "All hostiles in both lanes are permanently slowed by 18%", "-18% SLOW"),
        secondaryAura = AuraOption("Cryo Vulnerability", "Frozen hostiles take +30% amplified damage from all weapons", "+30% CRYO DMG"),
        isBeam = true,
        slowPercent = 0.55f
    ),
    TESLA_STORMCOIL(
        archetypeId = "tesla_stormcoil",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.DEFENSIVE,
        displayName = "Tesla Stormcoil",
        role = "Chaining High-Voltage Lightning Web",
        baseCost = 170,
        baseRange = 2.5f,
        baseDamage = 62f,
        baseFireRate = 1.6f,
        color = 0xFFB026FF,
        primaryAura = AuraOption("Lightning Grid Aura", "All defense towers periodically discharge chain lightning arcs", "CHAIN GRID"),
        secondaryAura = AuraOption("Supercharge Pulse", "+25% Attack speed to all adjacent energy towers", "+25% ADJ SPD"),
        chains = 4
    ),
    GRAVITON_VOID(
        archetypeId = "graviton_void",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.DEFENSIVE,
        displayName = "Graviton Void",
        role = "Gravitational Vortex & Spatial Stasis",
        baseCost = 210,
        baseRange = 2.7f,
        baseDamage = 110f,
        baseFireRate = 1.2f,
        color = 0xFF6366F1,
        primaryAura = AuraOption("Event Horizon Aura", "Vortex pulls creeps backward along lanes towards center", "GRAV PULL"),
        secondaryAura = AuraOption("Stasis Dilation", "Enemies in both lanes take 25% longer to advance", "STASIS 25%"),
        isSplash = true,
        splashRadius = 2.6f
    ),

    // Support (3)
    DYNAMO_CONDUIT(
        archetypeId = "dynamo_conduit",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.SUPPORT,
        displayName = "Dynamo Conduit",
        role = "Flux Power Siphon & Battery Bank",
        baseCost = 180,
        baseRange = 2.6f,
        baseDamage = 35f,
        baseFireRate = 3.0f,
        color = 0xFFA78BFA,
        primaryAura = AuraOption("Dynamo Harvest Aura", "+40% Scrap harvested from electrified or burned creeps", "+40% DYNAMO"),
        secondaryAura = AuraOption("Grid Battery Subsidy", "Generates +60 bonus Credits at the end of each wave", "+60 CR/WAVE")
    ),
    QUANTUM_BEACON(
        archetypeId = "quantum_beacon",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.SUPPORT,
        displayName = "Quantum Beacon",
        role = "Spatial Phase Transmitter & Warp Gate",
        baseCost = 195,
        baseRange = 3.5f,
        baseDamage = 30f,
        baseFireRate = 2.4f,
        color = 0xFF4F46E5,
        primaryAura = AuraOption("Phase Warp Aura", "Towers can fire across walls and obstacles without range penalty", "PHASE SHOOT"),
        secondaryAura = AuraOption("Tachyon Frequency", "+20% Fire rate to all elemental and void towers", "+20% ELEM SPD")
    ),
    DARK_MATTER_FORGE(
        archetypeId = "dark_matter_forge",
        towerClass = TowerClass.ELEMENTAL_VOID,
        vector = TowerVector.SUPPORT,
        displayName = "Dark Matter Forge",
        role = "Armor Liquefier & Shield Dissolution",
        baseCost = 205,
        baseRange = 2.8f,
        baseDamage = 50f,
        baseFireRate = 2.2f,
        color = 0xFF581C87,
        primaryAura = AuraOption("Zero Armor Protocol", "Permanently dissolves 40% armor from all enemy units", "-40% ARMOR"),
        secondaryAura = AuraOption("Entropic Shield", "Nexus Core projects a dark matter shield absorbing 4 hits", "4 HIT SHIELD"),
        armorShredPercent = 0.40f
    ),

    // ==========================================
    // CLASS 3: HEAVY SIEGE (9 Archetypes)
    // ==========================================
    // Offensive (3)
    HELLFIRE_MORTAR(
        archetypeId = "hellfire_mortar",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.OFFENSIVE,
        displayName = "Hellfire Mortar",
        role = "Heavy Incendiary Ballistic Artillery",
        baseCost = 200,
        baseRange = 4.4f,
        baseDamage = 145f,
        baseFireRate = 0.75f,
        color = 0xFFDC2626,
        primaryAura = AuraOption("Global Blast Aura", "+30% Splash Radius & +25% Explosive Damage to all AoE", "+30% BLAST"),
        secondaryAura = AuraOption("Carpet Salvo Aura", "Mortars fire a secondary cluster of 3 bomblets on impact", "CLUSTER 3x"),
        isSplash = true,
        splashRadius = 2.2f
    ),
    TITAN_RAILGUN(
        archetypeId = "titan_railgun",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.OFFENSIVE,
        displayName = "Titan Railgun",
        role = "Hypersonic Depleted Uranium Slug",
        baseCost = 230,
        baseRange = 5.2f,
        baseDamage = 280f,
        baseFireRate = 0.45f,
        color = 0xFF64748B,
        primaryAura = AuraOption("Colossus Buster Aura", "Towers deal +50% bonus damage to Boss and Colossus enemies", "+50% BOSS DMG"),
        secondaryAura = AuraOption("Kinetic Piercer", "Slugs punch through up to 4 enemies in a straight line", "LINE PIERCE")
    ),
    ORBITAL_STRIKE_RELAY(
        archetypeId = "orbital_strike_relay",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.OFFENSIVE,
        displayName = "Orbital Relay",
        role = "Targeting Uplink for Satellite Beams",
        baseCost = 250,
        baseRange = 5.0f,
        baseDamage = 210f,
        baseFireRate = 0.6f,
        color = 0xFF4338CA,
        primaryAura = AuraOption("Orbital Sweep Aura", "Periodically drops kinetic orbital strikes on lane bottlenecks", "ORBITAL BOMB"),
        secondaryAura = AuraOption("Global Range Net", "+35% Attack range to all long-range weapons", "+35% RNG")
    ),

    // Defensive (3)
    EARTHQUAKE_STAMPER(
        archetypeId = "earthquake_stamper",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.DEFENSIVE,
        displayName = "Seismic Slammer",
        role = "Pneumatic Ground Impact Shockwaves",
        baseCost = 190,
        baseRange = 2.2f,
        baseDamage = 120f,
        baseFireRate = 1.0f,
        color = 0xFF78350F,
        primaryAura = AuraOption("Tremor Aura", "All enemies traversing ground shake take 20% impact stumble", "STUMBLE"),
        secondaryAura = AuraOption("Structural Integrity", "Repairs Nexus Core for +2 HP whenever a wave is cleared", "+2 HP/WAVE"),
        isSplash = true,
        splashRadius = 2.4f
    ),
    CRYO_FORTRESS(
        archetypeId = "cryo_fortress",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.DEFENSIVE,
        displayName = "Cryo Fortress",
        role = "Heavy Glacier Mortar & Ice Calamity",
        baseCost = 210,
        baseRange = 3.8f,
        baseDamage = 95f,
        baseFireRate = 1.1f,
        color = 0xFF0369A1,
        primaryAura = AuraOption("Glacial Wall Aura", "Lobbed ice boulders create frozen hazard patches lasting 6s", "FROST WALL"),
        secondaryAura = AuraOption("Blizzard Gale", "Slows all creeps across the entire map by 20%", "20% MAP SLOW"),
        isSplash = true,
        splashRadius = 2.0f
    ),
    SHOCKWAVE_BASTION(
        archetypeId = "shockwave_bastion",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.DEFENSIVE,
        displayName = "Shockwave Bastion",
        role = "Heavy EMP Disruptor & Armor Melter",
        baseCost = 220,
        baseRange = 2.5f,
        baseDamage = 135f,
        baseFireRate = 1.1f,
        color = 0xFF334155,
        primaryAura = AuraOption("EMP Bastion Aura", "Stuns bosses and armored creeps for 2.0s every 12 seconds", "EMP 2s"),
        secondaryAura = AuraOption("Heavy Armor Sunder", "Permanently strips 50% armor from all hit targets", "-50% ARMOR"),
        isSplash = true,
        splashRadius = 2.0f
    ),

    // Support (3)
    NANITE_FORGE(
        archetypeId = "nanite_forge",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.SUPPORT,
        displayName = "Nanite Siege Forge",
        role = "Corrosive Armor Shredder & Core Reconstruction",
        baseCost = 210,
        baseRange = 2.8f,
        baseDamage = 60f,
        baseFireRate = 2.4f,
        color = 0xFF14B8A6,
        primaryAura = AuraOption("Nanite Armor-Shred", "Strips 45% armor from all creeps and regenerates Core HP +1", "SHRED & REPAIR"),
        secondaryAura = AuraOption("Automated Phalanx", "Nexus Core gains autonomous CIWS firing micro-missiles", "CIWS DEFENSE"),
        armorShredPercent = 0.45f
    ),
    SIEGE_COMMAND_POST(
        archetypeId = "siege_command_post",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.SUPPORT,
        displayName = "Command Post",
        role = "Battlefield Uplink & Tactical Overcharge",
        baseCost = 230,
        baseRange = 3.8f,
        baseDamage = 45f,
        baseFireRate = 1.8f,
        color = 0xFFBE185D,
        primaryAura = AuraOption("Overcharge Command", "All defense towers deal +25% Base Damage across the map", "+25% GLOBAL DMG"),
        secondaryAura = AuraOption("Siege Coordination", "+20% Attack speed and +15% range to all heavy and mortar towers", "+20% SIEGE SPD")
    ),
    WAR_CHEST_VAULT(
        archetypeId = "war_chest_vault",
        towerClass = TowerClass.HEAVY_SIEGE,
        vector = TowerVector.SUPPORT,
        displayName = "War Chest Vault",
        role = "Industrial Smelter & Strategic Reserves",
        baseCost = 240,
        baseRange = 2.6f,
        baseDamage = 40f,
        baseFireRate = 2.0f,
        color = 0xFFB45309,
        primaryAura = AuraOption("War Chest Dividend", "+50% Scrap Credits awarded at the completion of each wave", "+50% WAVE CR"),
        secondaryAura = AuraOption("Compound Interest", "Earn +5% bonus Credits based on current bank every wave", "+5% INTEREST")
    ),

    // ==========================================
    // CLASS 4: STELLAR STAR (9 Archetypes)
    // ==========================================
    // Offensive (3)
    SUPERNOVA_PULSAR(
        archetypeId = "supernova_pulsar",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.OFFENSIVE,
        displayName = "Supernova Pulsar",
        role = "Cosmic Flare & Radial Shockwaves",
        baseCost = 210,
        baseRange = 3.6f,
        baseDamage = 115f,
        baseFireRate = 1.6f,
        color = 0xFFFB923C,
        primaryAura = AuraOption("Supernova Flare", "+25% Radiant Splash Damage to all explosive weapons", "+25% RADIANT"),
        secondaryAura = AuraOption("Cosmic Radiation", "15% chance to blind hostiles, reducing speed by 30%", "BLIND 30%"),
        isSplash = true,
        splashRadius = 2.2f
    ),
    GRAVITON_LANCE(
        archetypeId = "graviton_lance",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.OFFENSIVE,
        displayName = "Graviton Lance",
        role = "Hyper-Dense Gravity Piercing Beam",
        baseCost = 230,
        baseRange = 3.8f,
        baseDamage = 85f,
        baseFireRate = 4.2f,
        color = 0xFFD97706,
        primaryAura = AuraOption("Gravity Well Aura", "Draws surrounding creeps inward towards the beam focal point", "GRAV WELL"),
        secondaryAura = AuraOption("Singularity Pull", "Reverses enemy movement for 0.8s on impact", "REVERSE 0.8s"),
        isBeam = true
    ),
    CELESTIAL_COMET(
        archetypeId = "celestial_comet",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.OFFENSIVE,
        displayName = "Celestial Comet",
        role = "Lobbed Meteor Detonating Into Star Fragments",
        baseCost = 260,
        baseRange = 4.8f,
        baseDamage = 220f,
        baseFireRate = 0.65f,
        color = 0xFFF43F5E,
        primaryAura = AuraOption("Meteor Storm", "+30% Meteor blast radius and leaves a 4s stardust pool", "+30% BLAST"),
        secondaryAura = AuraOption("Asteroid Impact", "Bosses take +40% impact damage", "+40% BOSS"),
        isSplash = true,
        splashRadius = 2.5f
    ),

    // Defensive (3)
    EVENT_HORIZON(
        archetypeId = "event_horizon",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.DEFENSIVE,
        displayName = "Event Horizon",
        role = "Space-Time Distortion Field Trapping Creeps",
        baseCost = 200,
        baseRange = 2.8f,
        baseDamage = 55f,
        baseFireRate = 2.4f,
        color = 0xFFCA8A04,
        primaryAura = AuraOption("Time Dilation", "All enemies in range have attack speed and movement halved", "50% DILATE"),
        secondaryAura = AuraOption("Temporal Lock", "Freezes target in time for 1.5s when health drops below 50%", "STASIS 1.5s"),
        slowPercent = 0.50f
    ),
    NEBULA_AEGIS(
        archetypeId = "nebula_aegis",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.DEFENSIVE,
        displayName = "Nebula Aegis",
        role = "Protective Cosmic Barrier Pulsing Defense Waves",
        baseCost = 220,
        baseRange = 3.2f,
        baseDamage = 70f,
        baseFireRate = 1.8f,
        color = 0xFFFCD34D,
        primaryAura = AuraOption("Stellar Shield", "Grants nearby turrets 20% faster weapon charge and heals Core +1 HP every 3 waves", "SHIELD +1HP"),
        secondaryAura = AuraOption("Solar Flare Repulsor", "Knocks back creeps within 2 tiles when shield pulses", "KNOCKBACK")
    ),
    BLACK_HOLE_SENTRY(
        archetypeId = "black_hole_sentry",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.DEFENSIVE,
        displayName = "Singularity Bastion",
        role = "Micro-Black Hole Generator Clustering Enemies",
        baseCost = 250,
        baseRange = 3.4f,
        baseDamage = 95f,
        baseFireRate = 1.2f,
        color = 0xFF92400E,
        primaryAura = AuraOption("Vortex Trap", "Pulls up to 5 enemies together, enabling massive AoE combos", "VORTEX 5x"),
        secondaryAura = AuraOption("Gravitational Collapse", "Executes normal creeps below 15% health", "EXEC 15%"),
        isSplash = true,
        splashRadius = 2.8f
    ),

    // Support (3)
    CHRONO_PYLON(
        archetypeId = "chrono_pylon",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.SUPPORT,
        displayName = "Chrono Pylon",
        role = "Accelerates Reload Cycles of Neighboring Towers",
        baseCost = 190,
        baseRange = 4.0f,
        baseDamage = 35f,
        baseFireRate = 2.0f,
        color = 0xFFFDE047,
        primaryAura = AuraOption("Temporal Haste", "+25% Attack speed to all towers within 3 tiles", "+25% HASTE"),
        secondaryAura = AuraOption("Time Warp", "Increases global game economy scrap ticks by 15%", "+15% SCRAP TICK")
    ),
    STARLIGHT_BEACON(
        archetypeId = "starlight_beacon",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.SUPPORT,
        displayName = "Starlight Beacon",
        role = "Celestial Illumination & Critical Amplification",
        baseCost = 210,
        baseRange = 4.5f,
        baseDamage = 45f,
        baseFireRate = 2.2f,
        color = 0xFFFEF08A,
        primaryAura = AuraOption("Prismatic Insight", "All allied turrets gain +25% Critical Hit Chance against illuminated targets", "+25% CRIT"),
        secondaryAura = AuraOption("Cosmic Radiance", "Reveals and strips cloaked/stealthed creeps instantly", "TRUE SIGHT")
    ),
    ASTRAL_HARVESTER(
        archetypeId = "astral_harvester",
        towerClass = TowerClass.STELLAR_COSMIC,
        vector = TowerVector.SUPPORT,
        displayName = "Astral Harvester",
        role = "Siphons Ambient Cosmic Energy into Scrap",
        baseCost = 230,
        baseRange = 3.2f,
        baseDamage = 40f,
        baseFireRate = 1.6f,
        color = 0xFFE11D48,
        primaryAura = AuraOption("Cosmic Harvest", "Creeps killed within aura range drop +40% scrap", "+40% SCRAP"),
        secondaryAura = AuraOption("Quantum Interest", "Generates +15 Scrap at the end of each wave per occupied booster pad", "+15c/PAD")
    );

    val globalBuffName: String get() = primaryAura.name
    val globalBuffDescription: String get() = primaryAura.description
}

fun mapTypeToArchetype(type: TowerType): TowerArchetype {
    return when (type) {
        TowerType.GATLING -> TowerArchetype.VULCAN_GATLING
        TowerType.CRYO -> TowerArchetype.FROST_SPIRE
        TowerType.TESLA -> TowerArchetype.TESLA_STORMCOIL
        TowerType.MORTAR -> TowerArchetype.HELLFIRE_MORTAR
        TowerType.LASER -> TowerArchetype.SOLAR_DEATH_RAY
        TowerType.VOID -> TowerArchetype.GRAVITON_VOID
        TowerType.STELLAR -> TowerArchetype.SUPERNOVA_PULSAR
    }
}

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
        baseDamage = 24f,
        baseFireRate = 2.4f,
        color = 0xFF00F0FF
    ),
    CRYO(
        displayName = "Cryo Glacier",
        role = "Sub-Zero Freezing Laser Spire",
        baseCost = 130,
        baseRange = 2.4f,
        baseDamage = 18f,
        baseFireRate = 3.2f,
        color = 0xFF38BDF8,
        isBeam = true,
        slowPercent = 0.50f
    ),
    TESLA(
        displayName = "Tesla Stormcoil",
        role = "Chaining High-Voltage Lightning Web",
        baseCost = 160,
        baseRange = 2.2f,
        baseDamage = 50f,
        baseFireRate = 1.4f,
        color = 0xFFB026FF,
        chains = 3
    ),
    MORTAR(
        displayName = "Hellfire Mortar",
        role = "Heavy Incendiary Ballistic Artillery",
        baseCost = 200,
        baseRange = 4.2f,
        baseDamage = 120f,
        baseFireRate = 0.70f,
        color = 0xFFFF7A00,
        isSplash = true,
        splashRadius = 1.8f
    ),
    LASER(
        displayName = "Solar Death Ray",
        role = "High-Intensity Thermal Piercing Beam",
        baseCost = 240,
        baseRange = 3.0f,
        baseDamage = 75f,
        baseFireRate = 4.2f,
        color = 0xFFFF0055,
        isBeam = true
    ),
    VOID(
        displayName = "Void Singularity",
        role = "Graviton Vortex & Armor Shredder",
        baseCost = 280,
        baseRange = 2.5f,
        baseDamage = 95f,
        baseFireRate = 1.1f,
        color = 0xFF8B5CF6,
        isSplash = true,
        splashRadius = 2.5f
    ),
    STELLAR(
        displayName = "Stellar Star",
        role = "Cosmic Flare & Gravitational Warp",
        baseCost = 180,
        baseRange = 3.6f,
        baseDamage = 65f,
        baseFireRate = 1.8f,
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

data class GlobalAuraSynergy(
    val auraName: String,
    val effectDescription: String,
    val sourceCount: Int,
    val iconEmoji: String,
    val color: Long
)

data class BankedSynergyVault(
    val generation: Int,
    val synergies: List<GlobalAuraSynergy>
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

data class TowerUnitBalance(
    val customName: String? = null,
    val damage: Float,
    val range: Float,
    val fireRate: Float,
    val cost: Int,
    val specialEffectValue: Float = 0f // splash radius, slow %, or chains depending on type
)

data class EnemyUnitBalance(
    val customName: String? = null,
    val hp: Float,
    val speed: Float,
    val armor: Float,
    val scrapValue: Int
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
    val minLaneLength: Int = 12,              // 8 to 25 slider
    val maxLaneLength: Int = 26,              // 15 to 40 slider
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

