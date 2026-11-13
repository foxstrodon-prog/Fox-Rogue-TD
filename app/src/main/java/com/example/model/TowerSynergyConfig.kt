package com.example.model

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
    VULCAN_GATLING(
        archetypeId = "vulcan_gatling", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.OFFENSIVE,
        displayName = "Vulcan Gatling", role = "Rapid Kinetic Rotary Cannons", baseCost = 130, baseRange = 2.6f, baseDamage = 26f, baseFireRate = 4.4f, color = 0xFF06B6D4,
        primaryAura = AuraOption("Global Speed Aura", "+18% Attack Speed to all towers", "+18% SPD"),
        secondaryAura = AuraOption("Kinetic Windup Aura", "Towers gain ramping +25% attack speed during continuous firing", "RAMP SPD")
    ),
    FLAK_CANNON(
        archetypeId = "flak_cannon", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.OFFENSIVE,
        displayName = "Twin Flak Cannon", role = "Shrapnel Burst Anti-Air & Ground", baseCost = 150, baseRange = 3.0f, baseDamage = 54f, baseFireRate = 2.2f, color = 0xFFFF5500,
        primaryAura = AuraOption("Shrapnel Aura", "All projectile weapons deal +20% micro-splash damage", "+20% SPLASH"),
        secondaryAura = AuraOption("Flak Barrage Aura", "15% chance for bullets to trigger secondary concussive bursts", "BURST")
    ),
    SNIPER_SENTRY(
        archetypeId = "sniper_sentry", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.OFFENSIVE,
        displayName = "Sniper Sentry", role = "Long-Range High-Caliber Penetrator", baseCost = 170, baseRange = 4.6f, baseDamage = 130f, baseFireRate = 0.9f, color = 0xFF818CF8,
        primaryAura = AuraOption("Execution Aura", "+25% Damage against enemies below 40% health", "+25% EXEC"),
        secondaryAura = AuraOption("Headhunter Aura", "Critical hits award +4 bonus Credits", "+4 CRIT CR")
    ),
    BARRICADE_TURRET(
        archetypeId = "barricade_turret", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.DEFENSIVE,
        displayName = "Barricade Turret", role = "Suppression Fire & Kinetic Concussion", baseCost = 120, baseRange = 2.4f, baseDamage = 32f, baseFireRate = 2.8f, color = 0xFF0284C7,
        primaryAura = AuraOption("Suppression Aura", "Hostiles entering weapon range are pushed back 0.5 tiles", "KNOCKBACK"),
        secondaryAura = AuraOption("Fortified Plating", "Nexus Core gains +5 Max HP and absorbs 2 free breaches", "+5 CORE HP")
    ),
    MINE_DISPENSER(
        archetypeId = "mine_dispenser", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.DEFENSIVE,
        displayName = "Mine Dispenser", role = "Deploys Proximity Mines On Lanes", baseCost = 140, baseRange = 2.8f, baseDamage = 75f, baseFireRate = 1.2f, color = 0xFFF59E0B,
        primaryAura = AuraOption("Minefield Net", "Automatically scatters 2 explosive traps along lanes each wave", "MINEFIELD"),
        secondaryAura = AuraOption("Caltrop Grid", "Slows all creeps traversing merged rail sections by 20%", "CALTROPS")
    ),
    RIOT_DISRUPTOR(
        archetypeId = "riot_disruptor", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.DEFENSIVE,
        displayName = "Riot Disruptor", role = "Acoustic Concussion Shockwaves", baseCost = 160, baseRange = 2.3f, baseDamage = 45f, baseFireRate = 2.0f, color = 0xFF059669,
        primaryAura = AuraOption("Concussive Aura", "All towers have a 12% chance to stun targets for 1.2s", "STUN 12%"),
        secondaryAura = AuraOption("Sonic Buffer", "Hostiles in both lanes lose 15% movement speed permanently", "-15% MOVE")
    ),
    RADAR_SENTRY(
        archetypeId = "radar_sentry", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.SUPPORT,
        displayName = "Radar Sentry", role = "Tactical Sensor Array & Range Uplink", baseCost = 120, baseRange = 4.2f, baseDamage = 35f, baseFireRate = 2.2f, color = 0xFF10B981,
        primaryAura = AuraOption("Global Targeting Aura", "+25% Range to all defense towers", "+25% RNG"),
        secondaryAura = AuraOption("Spotter Uplink", "Reveals enemy weak points, granting +20% Critical Strike Chance", "+20% CRIT")
    ),
    AMMO_SUPPLY_DEPOT(
        archetypeId = "ammo_supply_depot", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.SUPPORT,
        displayName = "Ammo Depot", role = "Logistical Feeder & Recoil Stabilizer", baseCost = 140, baseRange = 2.5f, baseDamage = 28f, baseFireRate = 2.5f, color = 0xFF94A3B8,
        primaryAura = AuraOption("Munitions Logistics", "Reduces all tower upgrade and placement costs by 15%", "-15% COST"),
        secondaryAura = AuraOption("High-Velocity Powder", "+20% Projectile flight speed and +15% kinetic base damage", "+15% DMG")
    ),
    SALVAGE_CRUSHER(
        archetypeId = "salvage_crusher", towerClass = TowerClass.KINETIC_BALLISTIC, vector = TowerVector.SUPPORT,
        displayName = "Salvage Crusher", role = "Scrap Recycler & Bounty Extractor", baseCost = 160, baseRange = 2.6f, baseDamage = 40f, baseFireRate = 2.0f, color = 0xFFE879F9,
        primaryAura = AuraOption("Scrap Bounty Aura", "+35% Scrap Credits awarded from all creep kills", "+35% SCRAP"),
        secondaryAura = AuraOption("Material Recycling", "Defeated boss/elite creeps drop +100 bonus Credits", "+100 BOSS CR")
    ),

    // ==========================================
    // CLASS 2: ELEMENTAL VOID (9 Archetypes)
    // ==========================================
    SOLAR_DEATH_RAY(
        archetypeId = "solar_death_ray", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.OFFENSIVE,
        displayName = "Solar Death Ray", role = "Thermal Piercing High-Intensity Beam", baseCost = 190, baseRange = 3.2f, baseDamage = 85f, baseFireRate = 4.6f, color = 0xFFFB7185,
        primaryAura = AuraOption("Solar Focus Aura", "+25% Critical Strike Chance & 2.5x Crit Damage", "2.5x CRIT"),
        secondaryAura = AuraOption("Thermal Dissolution", "Laser weapons melt and bypass 100% of creep armor", "MELT ARMOR"),
        isBeam = true
    ),
    INFERNO_CANNON(
        archetypeId = "inferno_cannon", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.OFFENSIVE,
        displayName = "Inferno Emitter", role = "Magma Stream & Thermal Burn DoT", baseCost = 175, baseRange = 2.8f, baseDamage = 60f, baseFireRate = 3.6f, color = 0xFFFF4500,
        primaryAura = AuraOption("Ignition Aura", "Damaged creeps suffer 15% damage/sec burning for 4 seconds", "BURN DoT"),
        secondaryAura = AuraOption("Thermal Conflagration", "Burning enemies explode on death for 50 AoE fire damage", "FIRE EXPLODE")
    ),
    PLASMA_PULSAR(
        archetypeId = "plasma_pulsar", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.OFFENSIVE,
        displayName = "Plasma Pulsar", role = "Ionized Plasma Spheres Down Lanes", baseCost = 185, baseRange = 3.0f, baseDamage = 95f, baseFireRate = 1.6f, color = 0xFFC026D3,
        primaryAura = AuraOption("Plasma Cascade", "Periodically fires automated plasma waves down both lanes", "PLASMA WAVE"),
        secondaryAura = AuraOption("Ion Overload", "+30% Damage to all beam and plasma towers", "+30% PLASMA")
    ),
    FROST_SPIRE(
        archetypeId = "frost_spire", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.DEFENSIVE,
        displayName = "Frost Spire", role = "Sub-Zero Freezing Laser Spire", baseCost = 160, baseRange = 2.6f, baseDamage = 26f, baseFireRate = 5.0f, color = 0xFF7DD3FC,
        primaryAura = AuraOption("Permafrost Aura", "All hostiles in both lanes are permanently slowed by 18%", "-18% SLOW"),
        secondaryAura = AuraOption("Cryo Vulnerability", "Frozen hostiles take +30% amplified damage from all weapons", "+30% CRYO DMG"),
        isBeam = true, slowPercent = 0.55f
    ),
    TESLA_STORMCOIL(
        archetypeId = "tesla_stormcoil", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.DEFENSIVE,
        displayName = "Tesla Stormcoil", role = "Chaining High-Voltage Lightning Web", baseCost = 170, baseRange = 2.5f, baseDamage = 62f, baseFireRate = 1.6f, color = 0xFFB026FF,
        primaryAura = AuraOption("Lightning Grid Aura", "All defense towers periodically discharge chain lightning arcs", "CHAIN GRID"),
        secondaryAura = AuraOption("Supercharge Pulse", "+25% Attack speed to all adjacent energy towers", "+25% ADJ SPD"),
        chains = 4
    ),
    GRAVITON_VOID(
        archetypeId = "graviton_void", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.DEFENSIVE,
        displayName = "Graviton Void", role = "Gravitational Vortex & Spatial Stasis", baseCost = 210, baseRange = 2.7f, baseDamage = 110f, baseFireRate = 1.2f, color = 0xFF6366F1,
        primaryAura = AuraOption("Event Horizon Aura", "Vortex pulls creeps backward along lanes towards center", "GRAV PULL"),
        secondaryAura = AuraOption("Stasis Dilation", "Enemies in both lanes take 25% longer to advance", "STASIS 25%"),
        isSplash = true, splashRadius = 2.6f
    ),
    DYNAMO_CONDUIT(
        archetypeId = "dynamo_conduit", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.SUPPORT,
        displayName = "Dynamo Conduit", role = "Flux Power Siphon & Battery Bank", baseCost = 180, baseRange = 2.6f, baseDamage = 35f, baseFireRate = 3.0f, color = 0xFFA78BFA,
        primaryAura = AuraOption("Dynamo Harvest Aura", "+40% Scrap harvested from electrified or burned creeps", "+40% DYNAMO"),
        secondaryAura = AuraOption("Grid Battery Subsidy", "Generates +60 bonus Credits at the end of each wave", "+60 CR/WAVE")
    ),
    QUANTUM_BEACON(
        archetypeId = "quantum_beacon", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.SUPPORT,
        displayName = "Quantum Beacon", role = "Spatial Phase Transmitter & Warp Gate", baseCost = 195, baseRange = 3.5f, baseDamage = 30f, baseFireRate = 2.4f, color = 0xFF4F46E5,
        primaryAura = AuraOption("Phase Warp Aura", "Towers can fire across walls and obstacles without range penalty", "PHASE SHOOT"),
        secondaryAura = AuraOption("Tachyon Frequency", "+20% Fire rate to all elemental and void towers", "+20% ELEM SPD")
    ),
    DARK_MATTER_FORGE(
        archetypeId = "dark_matter_forge", towerClass = TowerClass.ELEMENTAL_VOID, vector = TowerVector.SUPPORT,
        displayName = "Dark Matter Forge", role = "Armor Liquefier & Shield Dissolution", baseCost = 205, baseRange = 2.8f, baseDamage = 50f, baseFireRate = 2.2f, color = 0xFF581C87,
        primaryAura = AuraOption("Zero Armor Protocol", "Permanently dissolves 40% armor from all enemy units", "-40% ARMOR"),
        secondaryAura = AuraOption("Entropic Shield", "Nexus Core projects a dark matter shield absorbing 4 hits", "4 HIT SHIELD"),
        armorShredPercent = 0.40f
    ),

    // ==========================================
    // CLASS 3: HEAVY SIEGE (9 Archetypes)
    // ==========================================
    HELLFIRE_MORTAR(
        archetypeId = "hellfire_mortar", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.OFFENSIVE,
        displayName = "Hellfire Mortar", role = "Heavy Incendiary Ballistic Artillery", baseCost = 200, baseRange = 4.4f, baseDamage = 145f, baseFireRate = 0.75f, color = 0xFFDC2626,
        primaryAura = AuraOption("Global Blast Aura", "+30% Splash Radius & +25% Explosive Damage to all AoE", "+30% BLAST"),
        secondaryAura = AuraOption("Carpet Salvo Aura", "Mortars fire a secondary cluster of 3 bomblets on impact", "CLUSTER 3x"),
        isSplash = true, splashRadius = 2.2f
    ),
    TITAN_RAILGUN(
        archetypeId = "titan_railgun", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.OFFENSIVE,
        displayName = "Titan Railgun", role = "Hypersonic Depleted Uranium Slug", baseCost = 230, baseRange = 5.2f, baseDamage = 280f, baseFireRate = 0.45f, color = 0xFF64748B,
        primaryAura = AuraOption("Colossus Buster Aura", "Towers deal +50% bonus damage to Boss and Colossus enemies", "+50% BOSS DMG"),
        secondaryAura = AuraOption("Kinetic Piercer", "Slugs punch through up to 4 enemies in a straight line", "LINE PIERCE")
    ),
    ORBITAL_STRIKE_RELAY(
        archetypeId = "orbital_strike_relay", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.OFFENSIVE,
        displayName = "Orbital Relay", role = "Targeting Uplink for Satellite Beams", baseCost = 250, baseRange = 5.0f, baseDamage = 210f, baseFireRate = 0.6f, color = 0xFF4338CA,
        primaryAura = AuraOption("Orbital Sweep Aura", "Periodically drops kinetic orbital strikes on lane bottlenecks", "ORBITAL BOMB"),
        secondaryAura = AuraOption("Global Range Net", "+35% Attack range to all long-range weapons", "+35% RNG")
    ),
    EARTHQUAKE_STAMPER(
        archetypeId = "earthquake_stamper", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.DEFENSIVE,
        displayName = "Seismic Slammer", role = "Pneumatic Ground Impact Shockwaves", baseCost = 190, baseRange = 2.2f, baseDamage = 120f, baseFireRate = 1.0f, color = 0xFF78350F,
        primaryAura = AuraOption("Tremor Aura", "All enemies traversing ground shake take 20% impact stumble", "STUMBLE"),
        secondaryAura = AuraOption("Structural Integrity", "Repairs Nexus Core for +2 HP whenever a wave is cleared", "+2 HP/WAVE"),
        isSplash = true, splashRadius = 2.4f
    ),
    CRYO_FORTRESS(
        archetypeId = "cryo_fortress", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.DEFENSIVE,
        displayName = "Cryo Fortress", role = "Heavy Glacier Mortar & Ice Calamity", baseCost = 210, baseRange = 3.8f, baseDamage = 95f, baseFireRate = 1.1f, color = 0xFF0369A1,
        primaryAura = AuraOption("Glacial Wall Aura", "Lobbed ice boulders create frozen hazard patches lasting 6s", "FROST WALL"),
        secondaryAura = AuraOption("Blizzard Gale", "Slows all creeps across the entire map by 20%", "20% MAP SLOW"),
        isSplash = true, splashRadius = 2.0f
    ),
    SHOCKWAVE_BASTION(
        archetypeId = "shockwave_bastion", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.DEFENSIVE,
        displayName = "Shockwave Bastion", role = "Heavy EMP Disruptor & Armor Melter", baseCost = 220, baseRange = 2.5f, baseDamage = 135f, baseFireRate = 1.1f, color = 0xFF334155,
        primaryAura = AuraOption("EMP Bastion Aura", "Stuns bosses and armored creeps for 2.0s every 12 seconds", "EMP 2s"),
        secondaryAura = AuraOption("Heavy Armor Sunder", "Permanently strips 50% armor from all hit targets", "-50% ARMOR"),
        isSplash = true, splashRadius = 2.0f
    ),
    NANITE_FORGE(
        archetypeId = "nanite_forge", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.SUPPORT,
        displayName = "Nanite Siege Forge", role = "Corrosive Armor Shredder & Core Reconstruction", baseCost = 210, baseRange = 2.8f, baseDamage = 60f, baseFireRate = 2.4f, color = 0xFF14B8A6,
        primaryAura = AuraOption("Nanite Armor-Shred", "Strips 45% armor from all creeps and regenerates Core +1", "SHRED & REPAIR"),
        secondaryAura = AuraOption("Automated Phalanx", "Nexus Core gains autonomous CIWS firing micro-missiles", "CIWS DEFENSE"),
        armorShredPercent = 0.45f
    ),
    SIEGE_COMMAND_POST(
        archetypeId = "siege_command_post", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.SUPPORT,
        displayName = "Command Post", role = "Battlefield Uplink & Tactical Overcharge", baseCost = 230, baseRange = 3.8f, baseDamage = 45f, baseFireRate = 1.8f, color = 0xFFBE185D,
        primaryAura = AuraOption("Overcharge Command", "All defense towers deal +25% Base Damage across the map", "+25% GLOBAL DMG"),
        secondaryAura = AuraOption("Siege Coordination", "+20% Attack speed and +15% range to all heavy and mortar towers", "+20% SIEGE SPD")
    ),
    WAR_CHEST_VAULT(
        archetypeId = "war_chest_vault", towerClass = TowerClass.HEAVY_SIEGE, vector = TowerVector.SUPPORT,
        displayName = "War Chest Vault", role = "Industrial Smelter & Strategic Reserves", baseCost = 240, baseRange = 2.6f, baseDamage = 40f, baseFireRate = 2.0f, color = 0xFFB45309,
        primaryAura = AuraOption("War Chest Dividend", "+50% Scrap Credits awarded at the completion of each wave", "+50% WAVE CR"),
        secondaryAura = AuraOption("Compound Interest", "Earn +5% bonus Credits based on current bank every wave", "+5% INTEREST")
    ),

    // ==========================================
    // CLASS 4: STELLAR STAR (9 Archetypes)
    // ==========================================
    SUPERNOVA_PULSAR(
        archetypeId = "supernova_pulsar", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.OFFENSIVE,
        displayName = "Supernova Pulsar", role = "Cosmic Flare & Radial Shockwaves", baseCost = 210, baseRange = 3.6f, baseDamage = 115f, baseFireRate = 1.6f, color = 0xFFFB923C,
        primaryAura = AuraOption("Supernova Flare", "+25% Radiant Splash Damage to all explosive weapons", "+25% RADIANT"),
        secondaryAura = AuraOption("Cosmic Radiation", "15% chance to blind hostiles, reducing speed by 30%", "BLIND 30%"),
        isSplash = true, splashRadius = 2.2f
    ),
    GRAVITON_LANCE(
        archetypeId = "graviton_lance", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.OFFENSIVE,
        displayName = "Graviton Lance", role = "Hyper-Dense Gravity Piercing Beam", baseCost = 230, baseRange = 3.8f, baseDamage = 85f, baseFireRate = 4.2f, color = 0xFFD97706,
        primaryAura = AuraOption("Gravity Well Aura", "Draws surrounding creeps inward towards the beam focal point", "GRAV WELL"),
        secondaryAura = AuraOption("Singularity Pull", "Reverses enemy movement for 0.8s on impact", "REVERSE 0.8s"),
        isBeam = true
    ),
    CELESTIAL_COMET(
        archetypeId = "celestial_comet", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.OFFENSIVE,
        displayName = "Celestial Comet", role = "Lobbed Meteor Detonating Into Star Fragments", baseCost = 260, baseRange = 4.8f, baseDamage = 220f, baseFireRate = 0.65f, color = 0xFFF43F5E,
        primaryAura = AuraOption("Meteor Storm", "+30% Meteor blast radius and leaves a 4s stardust pool", "+30% BLAST"),
        secondaryAura = AuraOption("Asteroid Impact", "Bosses take +40% impact damage", "+40% BOSS"),
        isSplash = true, splashRadius = 2.5f
    ),
    EVENT_HORIZON(
        archetypeId = "event_horizon", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.DEFENSIVE,
        displayName = "Event Horizon", role = "Space-Time Distortion Field Trapping Creeps", baseCost = 200, baseRange = 2.8f, baseDamage = 55f, baseFireRate = 2.4f, color = 0xFFCA8A04,
        primaryAura = AuraOption("Time Dilation", "All enemies in range have attack speed and movement halved", "50% DILATE"),
        secondaryAura = AuraOption("Temporal Lock", "Freezes target in time for 1.5s when health drops below 50%", "STASIS 1.5s"),
        slowPercent = 0.50f
    ),
    NEBULA_AEGIS(
        archetypeId = "nebula_aegis", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.DEFENSIVE,
        displayName = "Nebula Aegis", role = "Protective Cosmic Barrier Pulsing Defense Waves", baseCost = 220, baseRange = 3.2f, baseDamage = 70f, baseFireRate = 1.8f, color = 0xFFFCD34D,
        primaryAura = AuraOption("Stellar Shield", "Grants nearby turrets 20% faster weapon charge and heals Core +1 HP every 3 waves", "SHIELD +1HP"),
        secondaryAura = AuraOption("Solar Flare Repulsor", "Knocks back creeps within 2 tiles when shield pulses", "KNOCKBACK")
    ),
    BLACK_HOLE_SENTRY(
        archetypeId = "black_hole_sentry", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.DEFENSIVE,
        displayName = "Singularity Bastion", role = "Micro-Black Hole Generator Clustering Enemies", baseCost = 250, baseRange = 3.4f, baseDamage = 95f, baseFireRate = 1.2f, color = 0xFF92400E,
        primaryAura = AuraOption("Vortex Trap", "Pulls up to 5 enemies together, enabling massive AoE combos", "VORTEX 5x"),
        secondaryAura = AuraOption("Gravitational Collapse", "Executes normal creeps below 15% health", "EXEC 15%"),
        isSplash = true, splashRadius = 2.8f
    ),
    CHRONO_PYLON(
        archetypeId = "chrono_pylon", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.SUPPORT,
        displayName = "Chrono Pylon", role = "Accelerates Reload Cycles of Neighboring Towers", baseCost = 190, baseRange = 4.0f, baseDamage = 35f, baseFireRate = 2.0f, color = 0xFFFDE047,
        primaryAura = AuraOption("Temporal Haste", "+25% Attack speed to all towers within 3 tiles", "+25% HASTE"),
        secondaryAura = AuraOption("Time Warp", "Increases global game economy scrap ticks by 15%", "+15% SCRAP TICK")
    ),
    STARLIGHT_BEACON(
        archetypeId = "starlight_beacon", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.SUPPORT,
        displayName = "Starlight Beacon", role = "Celestial Illumination & Critical Amplification", baseCost = 210, baseRange = 4.5f, baseDamage = 45f, baseFireRate = 2.2f, color = 0xFFFEF08A,
        primaryAura = AuraOption("Prismatic Insight", "All allied turrets gain +25% Critical Hit Chance against illuminated targets", "+25% CRIT"),
        secondaryAura = AuraOption("Cosmic Radiance", "Reveals and strips cloaked/stealthed creeps instantly", "TRUE SIGHT")
    ),
    ASTRAL_HARVESTER(
        archetypeId = "astral_harvester", towerClass = TowerClass.STELLAR_COSMIC, vector = TowerVector.SUPPORT,
        displayName = "Astral Harvester", role = "Siphons Ambient Cosmic Energy into Scrap", baseCost = 230, baseRange = 3.2f, baseDamage = 40f, baseFireRate = 1.6f, color = 0xFFE11D48,
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
