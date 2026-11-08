package com.example.model

import androidx.compose.ui.graphics.Color

enum class GameSpeed(val multiplier: Float, val label: String) {
    PAUSED(0f, "PAUSE"),
    NORMAL(1f, "1X"),
    FAST(2f, "2X"),
    ULTRA(3.5f, "3.5X"),
    MAX(5.0f, "5X"),
    HYPER(10.0f, "10X")
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
    val buff: TileBuff = TileBuff.NONE
)

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
    );

    val color: Long get() = themeColor
    val shapeSymbol: String get() = when (this) {
        KINETIC_BALLISTIC -> "●"
        ELEMENTAL_VOID -> "▲"
        HEAVY_SIEGE -> "■"
    }
    val shapeName: String get() = when (this) {
        KINETIC_BALLISTIC -> "Circle"
        ELEMENTAL_VOID -> "Triangle"
        HEAVY_SIEGE -> "Square"
    }
}

enum class TowerVector(
    val idName: String,
    val displayName: String,
    val role: String,
    val color: Long,
    val description: String
) {
    OFFENSIVE("offensive", "Offensive", "Heavy firepower & burst damage", 0xFFFF2A55, "Focuses on massive damage, rapid cannons, and heavy ordnance"),
    DEFENSIVE("defensive", "Defensive", "Crowd control, stasis & area denial", 0xFF00F0FF, "Focuses on slowing, trapping, and electrical arcs"),
    SUPPORT("support", "Support", "Targeting uplinks, armor shred & economy", 0xFFFFB800, "Focuses on range uplinks, stripping enemy armor, and credit yields")
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
        color = 0xFF00F0FF,
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
        color = 0xFFFF0055,
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
        color = 0xFF38BDF8,
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
        color = 0xFF0284C7,
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
        color = 0xFF00FF9D,
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
        color = 0xFF10B981,
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
        color = 0xFFFFD700,
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
        color = 0xFFFF0055,
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
        color = 0xFFFF6600,
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
        color = 0xFFD946EF,
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
        color = 0xFF38BDF8,
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
        color = 0xFF8B5CF6,
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
        color = 0xFFFFD700,
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
        color = 0xFF6366F1,
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
        color = 0xFF4C1D95,
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
        color = 0xFFFF7A00,
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
        color = 0xFFEF4444,
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
        color = 0xFFFF2A55,
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
        color = 0xFF0284C7,
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
        color = 0xFF475569,
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
        color = 0xFFE11D48,
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
        color = 0xFFFBBF24,
        primaryAura = AuraOption("War Chest Dividend", "+50% Scrap Credits awarded at the completion of each wave", "+50% WAVE CR"),
        secondaryAura = AuraOption("Compound Interest", "Earn +5% bonus Credits based on current bank every wave", "+5% INTEREST")
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
        baseFireRate = 3.8f,
        color = 0xFF00F0FF
    ),
    CRYO(
        displayName = "Cryo Glacier",
        role = "Sub-Zero Freezing Laser Spire",
        baseCost = 130,
        baseRange = 2.4f,
        baseDamage = 18f,
        baseFireRate = 5.2f,
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
    var rotationAngleDeg: Float = 0f
) {
    val isBase: Boolean get() = tier == 0 && branchTier1 == null
    val isTier1: Boolean get() = tier == 1 || (branchTier1 != null && finalFormCode == null && tier == 0)
    val isTier2: Boolean get() = tier == 2
    val isTier3: Boolean get() = tier == 3 || isTier3GlobalActive
    val isFinalForm: Boolean get() = finalFormCode != null || tier == 3 || isTier3GlobalActive
    val isMaxed: Boolean get() = isTier3 || finalFormCode != null
    val isAscended: Boolean get() = isTier3 || finalFormCode != null

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

data class GlobalAuraSynergy(
    val auraName: String,
    val effectDescription: String,
    val sourceCount: Int,
    val iconEmoji: String,
    val color: Long
)

enum class EnemyType(
    val displayName: String,
    val baseHp: Float,
    val baseSpeed: Float,
    val baseArmor: Float,
    val scrapValue: Int,
    val color: Long,
    val sizeRatio: Float
) {
    DRONE(
        displayName = "Scuttle Drone",
        baseHp = 45f,
        baseSpeed = 1.60f,
        baseArmor = 0f,
        scrapValue = 12,
        color = 0xFFEF4444,
        sizeRatio = 0.55f
    ),
    SOLDIER(
        displayName = "Automaton",
        baseHp = 115f,
        baseSpeed = 1.15f,
        baseArmor = 0.10f,
        scrapValue = 20,
        color = 0xFFF97316,
        sizeRatio = 0.75f
    ),
    SPEEDSTER(
        displayName = "Phase Scout",
        baseHp = 75f,
        baseSpeed = 2.05f,
        baseArmor = 0.05f,
        scrapValue = 24,
        color = 0xFFEAB308,
        sizeRatio = 0.60f
    ),
    BRUISER(
        displayName = "Siege Golem",
        baseHp = 420f,
        baseSpeed = 0.72f,
        baseArmor = 0.35f,
        scrapValue = 48,
        color = 0xFFEC4899,
        sizeRatio = 0.95f
    ),
    DISRUPTOR(
        displayName = "Shield Disruptor",
        baseHp = 240f,
        baseSpeed = 0.98f,
        baseArmor = 0.20f,
        scrapValue = 38,
        color = 0xFF06B6D4,
        sizeRatio = 0.85f
    ),
    BOSS(
        displayName = "Vanguard Titan",
        baseHp = 2000f,
        baseSpeed = 0.58f,
        baseArmor = 0.30f,
        scrapValue = 220,
        color = 0xFFA855F7,
        sizeRatio = 1.40f
    )
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
    val isBoss: Boolean = false
) {
    val isDead: Boolean get() = currentHp <= 0f
    val effectiveSpeed: Float
        get() = if (slowTimerMs > 0L) speed * slowFactor else speed
}

data class Projectile(
    val id: String,
    val startX: Float,
    val startY: Float,
    var currentX: Float,
    var currentY: Float,
    val targetEnemyId: String?,
    val targetX: Float,
    val targetY: Float,
    val speed: Float,
    val damage: Float,
    val color: Long,
    val isSplash: Boolean = false,
    val splashRadius: Float = 0f,
    val isMortarArc: Boolean = false,
    val totalDistance: Float = 1f,
    var traveledDistance: Float = 0f,
    val isCrit: Boolean = false,
    val hasClusterSplit: Boolean = false
)

data class LaserBeam(
    val id: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: Long,
    var lifeMs: Long = 120L
)

data class LightningArc(
    val id: String,
    val points: List<Pair<Float, Float>>,
    val color: Long = 0xFFB026FF,
    var lifeMs: Long = 180L
)

data class ExplosionEffect(
    val id: String,
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    var currentRadius: Float = 0f,
    val color: Long,
    var lifeMs: Long = 250L,
    val maxLifeMs: Long = 250L
)

data class FloatingText(
    val id: String,
    val x: Float,
    var y: Float,
    val text: String,
    val color: Long,
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
    val timestamp: Long
) {
    val score: Int get() = (wavesCleared * 1000) + (enemiesKilled * 15) + (totalDamage * 0.1f).toInt()
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

data class DevBalanceConfig(
    val startingScrap: Int = 220,             // Default reduced early income
    val globalIncomeMultiplier: Float = 1.0f,  // 0.2x to 3.0x slider
    val kineticDamageMultiplier: Float = 1.0f, // 0.2x to 3.0x per class/tower sliders
    val elementalDamageMultiplier: Float = 1.0f,
    val heavyDamageMultiplier: Float = 1.0f,
    val waveBonusMultiplier: Float = 1.0f,     // 0.2x to 2.5x end of wave bonus
    val enemyHpMultiplier: Float = 1.0f,       // 0.5x to 3.0x enemy difficulty
    val buildCostMultiplier: Float = 1.0f,     // 0.5x to 2.0x tower build costs
    val customTowerConfigs: Map<String, TowerUnitBalance> = emptyMap(),
    val customEnemyConfigs: Map<String, EnemyUnitBalance> = emptyMap()
) {
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
     * Generates a strict delta / diff-only JSON string containing ONLY values
     * that differ from the stock baseline defaults.
     */
    fun exportDiffJson(): String {
        val diffMap = mutableMapOf<String, Any>()
        val default = DevBalanceConfig()

        if (startingScrap != default.startingScrap) diffMap["startingScrap"] = startingScrap
        if (globalIncomeMultiplier != default.globalIncomeMultiplier) diffMap["globalIncomeMultiplier"] = globalIncomeMultiplier
        if (kineticDamageMultiplier != default.kineticDamageMultiplier) diffMap["kineticDamageMultiplier"] = kineticDamageMultiplier
        if (elementalDamageMultiplier != default.elementalDamageMultiplier) diffMap["elementalDamageMultiplier"] = elementalDamageMultiplier
        if (heavyDamageMultiplier != default.heavyDamageMultiplier) diffMap["heavyDamageMultiplier"] = heavyDamageMultiplier
        if (waveBonusMultiplier != default.waveBonusMultiplier) diffMap["waveBonusMultiplier"] = waveBonusMultiplier
        if (enemyHpMultiplier != default.enemyHpMultiplier) diffMap["enemyHpMultiplier"] = enemyHpMultiplier
        if (buildCostMultiplier != default.buildCostMultiplier) diffMap["buildCostMultiplier"] = buildCostMultiplier

        // Tower diffs
        val towerDiffs = mutableMapOf<String, Map<String, Any>>()
        for (type in TowerType.values()) {
            val curr = getTowerBalance(type)
            val defTower = default.getTowerBalance(type)
            val tDiff = mutableMapOf<String, Any>()

            if (!curr.customName.isNullOrBlank() && curr.customName != type.displayName) {
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
                towerDiffs[type.name] = tDiff
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

        // Serialize to clean formatted JSON string
        val sb = StringBuilder("{\n")
        val entries = diffMap.entries.toList()
        entries.forEachIndexed { i, (k, v) ->
            sb.append("  \"").append(k).append("\": ")
            when (v) {
                is Number -> sb.append(v)
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

