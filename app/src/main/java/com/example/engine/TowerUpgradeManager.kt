package com.example.engine

import com.example.model.*

data class PathUpgrade(
    val name: String,
    val description: String,
    val cost: Int
)

object TowerUpgradeManager {

    private val TIER_1_BY_TYPE: Map<TowerType, List<Tier1FormDef>> = TowerType.values().associateWith { type ->
        val baseCost = type.baseCost
        when (type) {
            TowerType.GATLING -> listOf(
                Tier1FormDef("A", "Overdrive Rotor", "Drastically increases cyclic firing rate (+60% fire rate).", (baseCost * 0.8f).toInt(), 1.0f, 1.6f, 1.0f),
                Tier1FormDef("B", "High-Caliber Breach", "Heavy kinetic munitions dealing amplified impact (+60% damage).", (baseCost * 0.85f).toInt(), 1.6f, 1.0f, 1.0f),
                Tier1FormDef("C", "Sensor Array", "Extended optical targeting array and lock-on (+45% range).", (baseCost * 0.75f).toInt(), 1.15f, 1.15f, 1.45f)
            )
            TowerType.CRYO -> listOf(
                Tier1FormDef("A", "Sub-Zero Coolant", "Deep freeze laser slowing creeps by 65%.", (baseCost * 0.8f).toInt(), 1.1f, 1.0f, 1.0f),
                Tier1FormDef("B", "Brittle Cryo-Core", "Frostbite weakens creeps, increasing damage taken from all sources by 35%.", (baseCost * 0.85f).toInt(), 1.5f, 1.0f, 1.0f),
                Tier1FormDef("C", "Prism Lens Aperture", "Extended focal length laser covering wider sector (+40% range).", (baseCost * 0.75f).toInt(), 1.1f, 1.2f, 1.40f)
            )
            TowerType.TESLA -> listOf(
                Tier1FormDef("A", "Conduction Coils", "Amplified transformer chaining lightning to +2 additional targets.", (baseCost * 0.8f).toInt(), 1.1f, 1.1f, 1.0f),
                Tier1FormDef("B", "Voltage Step-Up", "High-amperage shock dealing heavy electrical damage (+55% damage).", (baseCost * 0.85f).toInt(), 1.55f, 1.0f, 1.0f),
                Tier1FormDef("C", "Flux Capacitors", "Fast-discharge capacitor banks accelerating shock frequency (+50% fire rate).", (baseCost * 0.75f).toInt(), 1.0f, 1.50f, 1.1f)
            )
            TowerType.MORTAR -> listOf(
                Tier1FormDef("A", "Cluster Payload", "Lobbed artillery shells split into 3 micro-bomblets on impact.", (baseCost * 0.8f).toInt(), 1.2f, 1.0f, 1.0f),
                Tier1FormDef("B", "Incendiary Shells", "Napalm warheads setting the target ground ablaze for 4 seconds.", (baseCost * 0.85f).toInt(), 1.6f, 0.9f, 1.0f),
                Tier1FormDef("C", "Rifled Siege Tube", "High-velocity propellant charges lobbing shells with +45% range.", (baseCost * 0.75f).toInt(), 1.2f, 1.1f, 1.45f)
            )
            TowerType.LASER -> listOf(
                Tier1FormDef("A", "Thermal Concentration", "High-heat continuous beam dealing +55% sustained DPS.", (baseCost * 0.8f).toInt(), 1.55f, 1.0f, 1.0f),
                Tier1FormDef("B", "Refraction Splitter", "Optical splitter allowing beam to simultaneously hit 2 targets.", (baseCost * 0.85f).toInt(), 1.2f, 1.0f, 1.2f),
                Tier1FormDef("C", "Solar Resonator", "Pulsed thermal frequency boosting tick rate (+50% fire rate).", (baseCost * 0.75f).toInt(), 1.0f, 1.50f, 1.1f)
            )
            TowerType.VOID -> listOf(
                Tier1FormDef("A", "Graviton Well", "Intense gravity vortex slowing hostiles by 50% and pulling them inward.", (baseCost * 0.8f).toInt(), 1.1f, 1.0f, 1.1f),
                Tier1FormDef("B", "Armor Dissolution", "Dark matter pulses permanently strip 50% of armor from all hostiles.", (baseCost * 0.85f).toInt(), 1.5f, 1.0f, 1.0f),
                Tier1FormDef("C", "Tachyon Resonator", "Rapid pulse frequency firing shockwaves 50% faster.", (baseCost * 0.75f).toInt(), 1.0f, 1.50f, 1.0f)
            )
            TowerType.STELLAR -> listOf(
                Tier1FormDef("A", "Supernova Flare", "Radiant cosmic flares dealing amplified splash (+50% splash, +25% damage).", (baseCost * 0.8f).toInt(), 1.25f, 1.0f, 1.0f),
                Tier1FormDef("B", "Gravity Well", "Dense gravitational suction slowing hostiles by 45% and drawing them inward.", (baseCost * 0.85f).toInt(), 1.2f, 1.0f, 1.15f),
                Tier1FormDef("C", "Celestial Resonator", "Harmonic astral frequency accelerating firing cadence (+40% fire rate).", (baseCost * 0.75f).toInt(), 1.0f, 1.40f, 1.30f)
            )
        }
    }

    private val TIER_1_BY_KEY: Map<String, Tier1FormDef> = buildMap {
        for ((type, list) in TIER_1_BY_TYPE) {
            for (opt in list) {
                put("${type.name}_${opt.branchKey}", opt)
            }
        }
    }

    private val FINAL_FORMS_BY_TYPE: Map<TowerType, List<FinalFormDef>> = TowerType.values().associateWith { type ->
        val baseCost = type.baseCost
        val c = (baseCost * 1.5f).toInt()
        when (type) {
            TowerType.GATLING -> listOf(
                FinalFormDef("A.a", "Hyper-Gatling", "Extreme cyclic speed firing a continuous wall of lead.", c,
                    "Global Kinetic Windup", "+18% Fire Rate to ALL towers", 1.2f, 2.4f, 1.0f),
                FinalFormDef("A.b", "Rotary Twin-Cannon", "Dual synchronized rotating barrels firing parallel tracers.", c,
                    "Twin Ballistics Grid", "All kinetic bullets have a 25% chance to fire a bonus duplicate tracer", 1.8f, 1.8f, 1.0f),
                FinalFormDef("A.c", "Nano-Chamber Feeder", "Accelerated feeding mechanism never slows down.", c,
                    "Nanite Reload Logistics", "-15% Upgrade Cost on all towers", 1.3f, 2.0f, 1.1f),
                FinalFormDef("B.a", "Depleted Uranium Sabot", "Heavy penetrators ignoring 100% of enemy armor.", c,
                    "Global Armor Piercing", "All defense towers ignore 25% of hostile armor", 2.4f, 1.1f, 1.0f),
                FinalFormDef("B.b", "High-Explosive Flak", "Bullets detonate on impact into mini-shrapnel bursts.", c,
                    "Flak Screen", "All kinetic rounds deal +20% splash damage", 2.0f, 1.3f, 1.0f, "splash"),
                FinalFormDef("B.c", "Executioner Caliber", "Lethal rounds dealing 3x damage against hostiles below 40% HP.", c,
                    "Execution Protocol", "All towers deal +25% damage to damaged enemies", 2.5f, 1.0f, 1.0f),
                FinalFormDef("C.a", "Sniper Sentry", "Extreme range ballistic cannon prioritizing elite targets.", c,
                    "Long-Range Optics Net", "+25% Range to ALL defense towers", 2.2f, 1.2f, 2.0f),
                FinalFormDef("C.b", "Kinetic Ricochet Net", "Rounds ricochet across up to 3 hostiles.", c,
                    "Ricochet Resonance", "Bullets that kill enemies refund +5 Credits", 1.6f, 1.5f, 1.5f, "ricochet"),
                FinalFormDef("C.c", "Automated Phalanx", "Autonomous CIWS firing micro-missiles and bullets.", c,
                    "Phalanx Interceptor", "Nexus Core regenerates +1 HP per wave", 1.7f, 1.7f, 1.4f)
            )
            TowerType.CRYO -> listOf(
                FinalFormDef("A.a", "Absolute Zero Spire", "Beam completely freezes hostiles in place for 2 seconds.", c,
                    "Permafrost Freeze Grid", "Hostiles in both lanes are permanently slowed by 15%", 1.4f, 1.3f, 1.1f),
                FinalFormDef("A.b", "Glacial Stasis Monolith", "Emits a continuous frost field trapping swarms.", c,
                    "Stasis Field Net", "Enemies in both lanes take 20% longer to reach the Core", 1.5f, 1.2f, 1.2f),
                FinalFormDef("A.c", "Liquid Nitrogen Projector", "Flash freezes targets, reducing their armor to 0 while chilled.", c,
                    "Cryo-Vulnerability", "All frozen hostiles take +30% damage from all weapons", 1.6f, 1.3f, 1.0f),
                FinalFormDef("B.a", "Cryo Shatterburst", "Hostiles detonate into sharp ice shards on death.", c,
                    "Shatter Chain Reaction", "When any hostile dies while slowed, it bursts dealing 20% max HP AoE", 1.9f, 1.2f, 1.0f, "shatter"),
                FinalFormDef("B.b", "Frostfire Reactor", "Sub-zero beam inflicts thermal shock damage over time.", c,
                    "Thermal Shock Wave", "+20% Elemental Damage to all towers", 2.2f, 1.1f, 1.1f),
                FinalFormDef("B.c", "Hailstorm Battery", "Lobs jagged ice boulders down both lanes.", c,
                    "Hailstorm Barrage", "All towers gain +10% Critical Strike Chance", 2.0f, 1.2f, 1.3f),
                FinalFormDef("C.a", "Blizzard Pulsar", "Emits periodic screen-wide blizzard pulses.", c,
                    "Arctic Gale", "Slows all active hostiles on the screen by 25%", 1.5f, 1.4f, 1.8f),
                FinalFormDef("C.b", "Aurora Borealis Spire", "Prismatic aurora beam illuminating weak points.", c,
                    "Aurora Matrix", "+25% Critical Hit Damage across all towers", 1.8f, 1.3f, 1.5f),
                FinalFormDef("C.c", "Sub-Zero Conduit", "Freezes enemies and siphons their energy into Credits.", c,
                    "Cryo Bounty Harvest", "+30% Credits earned from all kills in both lanes", 1.6f, 1.4f, 1.4f)
            )
            TowerType.TESLA -> listOf(
                FinalFormDef("A.a", "Storm Web Generator", "Lightning arcs chain across up to 8 hostiles simultaneously.", c,
                    "Superconducting Chain Grid", "All defense towers occasionally discharge chain lightning bolts", 1.5f, 1.5f, 1.1f),
                FinalFormDef("A.b", "Arc Discharger", "Ultra-dense lightning arcs that jump between lanes.", c,
                    "Bifurcated Lightning Net", "Towers can shoot targets across intersecting lanes with +20% damage", 1.8f, 1.3f, 1.2f),
                FinalFormDef("A.c", "Faraday Capacitor", "Stores charge and discharges high-voltage shockwaves.", c,
                    "Capacitive Overcharge", "All tower active abilities recharge 25% faster", 1.6f, 1.4f, 1.1f),
                FinalFormDef("B.a", "Gigawatt Deathcoil", "Devastating single-target thunderstrike melting bruisers.", c,
                    "High-Tension Surge", "+25% Base Damage to ALL towers", 2.6f, 1.0f, 1.1f),
                FinalFormDef("B.b", "EMP Disruptor", "Shock pulses completely disable enemy armor and shields for 3s.", c,
                    "EMP Shockwave", "Disables all hostile passives and shields", 1.9f, 1.2f, 1.0f, "emp"),
                FinalFormDef("B.c", "Plasma Discharge Tube", "Fires ball lightning floating down the lane.", c,
                    "Ball Lightning Trail", "Towers deal +15% damage to targets currently suffering shock", 2.1f, 1.2f, 1.2f),
                FinalFormDef("C.a", "Static Dynamo", "Generates passive power increasing global projectile velocity.", c,
                    "Kinetic Acceleration Grid", "+35% Projectile Flight Velocity to all turrets", 1.4f, 1.8f, 1.2f),
                FinalFormDef("C.b", "Supercharged Relay", "Relays bonus energy to adjacent towers granting +25% fire rate.", c,
                    "Adjacent Overclock", "Towers immediately adjacent gain +25% Fire Rate", 1.5f, 1.7f, 1.3f),
                FinalFormDef("C.c", "Lightning Rod Siphon", "Harvests atmospheric energy granting +20 Credits per wave.", c,
                    "Atmospheric Siphon", "+20 Bonus Credits awarded upon clearing any wave", 1.6f, 1.6f, 1.2f)
            )
            TowerType.MORTAR -> listOf(
                FinalFormDef("A.a", "Hellfire Carpet Battery", "Lobs dense carpet salvos blanketing entire lanes.", c,
                    "Carpet Firestorm", "Explosions trigger 3 mini-bomblets dealing 25% AoE damage", 1.7f, 1.2f, 1.1f),
                FinalFormDef("A.b", "Shrapnel Flak Mortar", "Shells air-burst into razor shrapnel shredding swarms.", c,
                    "Flak Dispersion", "+35% Splash Radius to all explosive weapons", 1.5f, 1.4f, 1.2f, "splash"),
                FinalFormDef("A.c", "Thermite Siege Mortar", "Burns through armor plates with sustained high heat.", c,
                    "Thermite Melt", "Enemies hit by explosions permanently lose 35% armor", 2.1f, 1.1f, 1.0f),
                FinalFormDef("B.a", "Inferno Calamity Spire", "Ignites persistent ground fire pools dealing continuous burn.", c,
                    "Persistent Firestorm", "Lava pools deal 40 DPS for 5 seconds", 2.0f, 1.0f, 1.1f),
                FinalFormDef("B.b", "Volcanic Siege Cannon", "Lobs massive molten rock chunks stunning hostiles.", c,
                    "Volcanic Impact", "Shells have a 25% chance to stun enemies for 1.5s", 2.3f, 0.9f, 1.1f),
                FinalFormDef("B.c", "Magma Core Battery", "Armor-melting artillery prioritizing armored bruisers.", c,
                    "Molten Core Protocol", "+30% Damage against bruisers and boss colossi", 2.5f, 0.9f, 1.0f),
                FinalFormDef("C.a", "Titan Rail-Mortar", "Extreme-range artillery striking from across the entire map.", c,
                    "Global Bombardment", "+40% Range to all long-range weapons", 2.2f, 1.1f, 1.6f),
                FinalFormDef("C.b", "Smart-Munitions Silo", "GPS-guided artillery shells seeking dense clusters.", c,
                    "Cluster Targeting Net", "+20% Critical Strike Chance on all explosions", 1.9f, 1.3f, 1.3f),
                FinalFormDef("C.c", "Atomic Shell Battery", "Tactical micro-warheads leaving glowing fallout zones.", c,
                    "Atomic Fallout", "Defeated enemies award +40% bonus Credits", 2.4f, 1.0f, 1.2f)
            )
            TowerType.LASER -> listOf(
                FinalFormDef("A.a", "Solar Death Ray Apex", "Ultra-dense sustained photon beam melting everything in path.", c,
                    "Solar Death Core", "Continuous beam ramp-up deals up to 3x base damage over 3s", 2.4f, 1.2f, 1.1f),
                FinalFormDef("A.b", "Gamma Ray Slicer", "Pierces through all hostiles in a straight vector.", c,
                    "Linear Gamma Slicer", "Beams punch through up to 5 hostiles simultaneously", 2.0f, 1.3f, 1.2f),
                FinalFormDef("A.c", "Photon Overcharger", "Emits periodic blinding laser flashes stunning targets.", c,
                    "Photon Blindness", "Blinds creeps for 2 seconds, reducing attack speed", 1.8f, 1.5f, 1.0f),
                FinalFormDef("B.a", "Prismatic Tri-Beam", "Splits into 3 independent high-intensity laser streams.", c,
                    "Tri-Beam Network", "Laser towers can lock onto 3 distinct targets at once", 1.6f, 1.4f, 1.1f),
                FinalFormDef("B.b", "Kaleidoscope Prism", "Beams scatter into wide-angle cutting fans.", c,
                    "Prism Dispersion", "+25% Critical Hit Damage to all energy weapons", 1.8f, 1.3f, 1.2f),
                FinalFormDef("B.c", "Thermal Lance Battery", "Inflicts severe melting debuff on boss hostiles.", c,
                    "Colossus Melter", "+40% sustained DPS against boss colossi", 2.2f, 1.1f, 1.1f),
                FinalFormDef("C.a", "Pulsed Tachyon Beam", "Ultra-fast laser pulsing hitting 10 times per second.", c,
                    "Tachyon Frequency", "+25% Attack Speed to all beam and laser weapons", 1.5f, 2.0f, 1.2f),
                FinalFormDef("C.b", "Solar Flare Projector", "Emits periodic radial laser pulses around the turret.", c,
                    "Coronal Pulse", "Pulses a 360-degree thermal wave dealing 100 damage every 4s", 1.9f, 1.4f, 1.3f),
                FinalFormDef("C.c", "Solar Battery Collector", "Converts excess thermal energy into power and Credits.", c,
                    "Solar Energy Subsidy", "Generates +50 Credits at the end of each wave", 1.6f, 1.5f, 1.2f)
            )
            TowerType.VOID -> listOf(
                FinalFormDef("A.a", "Singularity Vortex Engine", "Pulls hostiles inward toward the center of the vortex.", c,
                    "Gravitational Singularity", "Pulls all nearby creeps into the center of the blast", 1.8f, 1.3f, 1.3f),
                FinalFormDef("A.b", "Event Horizon Monolith", "Hostiles crossing the event horizon lose 60% speed.", c,
                    "Time Dilation Field", "All creeps on the field move 20% slower", 1.6f, 1.4f, 1.2f),
                FinalFormDef("A.c", "Black Hole Generator", "Collapsing dark matter core triggering spatial implosions.", c,
                    "Spatial Implosion", "Implosion deals 15% enemy max HP as true damage", 2.2f, 1.1f, 1.1f),
                FinalFormDef("B.a", "Zero Armor Protocol", "Permanently strips 100% of armor from all hit targets.", c,
                    "Zero Armor Protocol", "All enemy armor on the battlefield is permanently reduced to 0", 2.1f, 1.1f, 1.1f),
                FinalFormDef("B.b", "Dark Matter Decimeter", "Eats away at enemy flesh and hulls with entropic decay.", c,
                    "Entropic Decay", "All enemies lose 2% max HP per second while on the field", 2.0f, 1.2f, 1.1f),
                FinalFormDef("B.c", "Void Devourer", "Swallows hostiles whole when below 20% health.", c,
                    "Devour Bounty", "Instantly consumed enemies award double Credits", 2.4f, 1.1f, 1.1f),
                FinalFormDef("C.a", "Dark Matter Pulse Engine", "Releases map-wide dark matter shockwaves every 3s.", c,
                    "Map-Wide Shockwave", "Pulses damage to all hostiles across the entire screen", 2.0f, 1.7f, 1.5f),
                FinalFormDef("C.b", "Spatial Distortion Field", "Warps space around the tower, doubling fire rates of adjacent towers.", c,
                    "Spatial Warp Aura", "Towers adjacent to this tower gain +40% Fire Rate", 1.8f, 1.6f, 1.2f),
                FinalFormDef("C.c", "Cosmic Singularity Bank", "Tears open cosmic rifts dropping dense scrap caches.", c,
                    "Void Scrap Yield", "+45% Scrap Credits from all defeated enemies", 1.9f, 1.5f, 1.2f)
            )
            TowerType.STELLAR -> listOf(
                FinalFormDef("A.a", "Supernova Pulsar", "Emanates periodic starburst shockwaves disintegrating swarms.", c,
                    "Supernova Flare Grid", "+25% Radiant Splash Damage to all explosive towers", 1.8f, 1.5f, 1.2f),
                FinalFormDef("A.b", "Solar Flare Cataclysm", "Channels extreme coronal mass ejections burning targets over 4s.", c,
                    "Coronal Mass Ejection", "Creeps take 20% burning damage over 4 seconds", 2.2f, 1.2f, 1.1f),
                FinalFormDef("A.c", "Celestial Comet Cannon", "Lobs dense meteors splitting into 4 star fragments on impact.", c,
                    "Comet Cluster Salvo", "All splash weapons trigger secondary mini-meteor bursts", 2.0f, 1.1f, 1.4f),
                FinalFormDef("B.a", "Event Horizon Spire", "Traps creeps in deep gravity wells, reversing their movement for 1s.", c,
                    "Event Horizon Singularity", "All hostiles traversing bottlenecks are drawn into focal points", 1.5f, 1.4f, 1.3f),
                FinalFormDef("B.b", "Singularity Bastion", "Generates micro-black holes executing creeps below 15% HP.", c,
                    "Singularity Execution", "Normal creeps below 15% health are instantly consumed", 2.1f, 1.2f, 1.2f),
                FinalFormDef("B.c", "Graviton Lance Battery", "Continuous gravity beam locking heavy bruisers in place.", c,
                    "Graviton Anchor", "Bosses and bruisers have their movement speed reduced by 30%", 1.9f, 1.6f, 1.3f),
                FinalFormDef("C.a", "Chrono Pylon Array", "Emits temporal acceleration waves hastening nearby reload cycles.", c,
                    "Temporal Haste Net", "+25% Attack Speed to all towers within 3 tiles", 1.4f, 2.2f, 1.5f),
                FinalFormDef("C.b", "Starlight Beacon Array", "Illuminates weak points granting +25% critical strike chance.", c,
                    "Prismatic Revelation", "+25% Critical Hit Chance across all defense towers", 1.7f, 1.5f, 1.8f),
                FinalFormDef("C.c", "Astral Harvester Engine", "Converts ambient celestial energy and destroyed creeps into bonus Scrap.", c,
                    "Cosmic Harvest Yield", "+40% Scrap Credits awarded from all creep kills", 1.6f, 1.4f, 1.4f)
            )
        }
    }

    private val FINAL_FORM_BY_KEY: Map<String, FinalFormDef> = buildMap {
        for ((type, list) in FINAL_FORMS_BY_TYPE) {
            for (ff in list) {
                put("${type.name}_${ff.code}", ff)
            }
        }
    }

    fun getTier1Options(type: TowerType): List<Tier1FormDef> = TIER_1_BY_TYPE[type] ?: emptyList()

    fun getTier1Form(type: TowerType, branchKey: String): Tier1FormDef? =
        TIER_1_BY_KEY["${type.name}_$branchKey"]

    fun getFinalFormOptions(type: TowerType, branchTier1: String): List<FinalFormDef> {
        val allFinalForms = get9FinalForms(type)
        return allFinalForms.filter { it.code.startsWith(branchTier1) }
    }

    fun getFinalForm(type: TowerType, code: String): FinalFormDef? =
        FINAL_FORM_BY_KEY["${type.name}_$code"]

    fun get9FinalForms(type: TowerType): List<FinalFormDef> = FINAL_FORMS_BY_TYPE[type] ?: emptyList()

    fun getTier1Cost(tower: TowerInstance, branchKey: String): Int {
        val formDef = getTier1Form(tower.type, branchKey)
        return formDef?.cost ?: (tower.type.baseCost * 0.8f).toInt()
    }

    fun getFinalFormCost(tower: TowerInstance, code: String, allTowers: List<TowerInstance>): Int {
        val formDef = getFinalForm(tower.type, code)
        val baseCost = formDef?.cost ?: (tower.type.baseCost * 1.5f).toInt()

        val finalFormsCount = allTowers.count { it.isFinalForm }
        if (finalFormsCount > 0) {
            val scale = 1.0f + (0.15f * finalFormsCount * (allTowers.size.toFloat() / 5f).coerceIn(0.6f, 1.8f))
            return (baseCost * scale).toInt()
        }
        return baseCost
    }

    fun getSellValue(tower: TowerInstance): Int {
        return (tower.getInvestedScrapValue() * 0.75f).toInt()
    }

    // --- 3-Tier Hierarchy API ---
    fun getBaseTurretCost(): Int = 80

    fun getTowerClasses(): List<TowerClass> = TowerClass.values().toList()

    fun isClassUnlocked(towerClass: TowerClass, allTowers: List<TowerInstance>): Boolean {
        val tier3Count = allTowers.count { it.isTier3 }
        return when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> true
            TowerClass.ELEMENTAL_VOID -> tier3Count >= 1
            TowerClass.HEAVY_SIEGE -> tier3Count >= 2
            TowerClass.STELLAR_COSMIC -> tier3Count >= 3
        }
    }

    fun getTier1Vectors(): List<TowerVector> = TowerVector.values().toList()

    fun getTier1VectorCost(towerClass: TowerClass = TowerClass.KINETIC_BALLISTIC): Int =
        (towerClass.baseCost * 0.9f).toInt()

    fun getTier2Archetypes(vector: TowerVector): List<TowerArchetype> =
        TowerArchetype.values().filter { it.vector == vector && it.towerClass == TowerClass.KINETIC_BALLISTIC }

    fun getTier2ArchetypesForClass(towerClass: TowerClass, vector: TowerVector): List<TowerArchetype> =
        TowerArchetype.values().filter { it.towerClass == towerClass && it.vector == vector }

    fun getTier2Cost(archetype: TowerArchetype): Int = archetype.baseCost

    fun getTier3GlobalCost(tower: TowerInstance, allTowers: List<TowerInstance>): Int {
        val arch = tower.archetype ?: mapTypeToArchetype(tower.type)
        val baseCost = (arch.baseCost * 1.5f).toInt()
        val tier3Count = allTowers.count { it.isTier3 }
        return if (tier3Count > 0) {
            (baseCost * (1.0f + 0.25f * tier3Count)).toInt()
        } else {
            baseCost
        }
    }

    fun getPathUpgrade(type: TowerType, pathIndex: Int, tier: Int): PathUpgrade {
        val branchKey = when (pathIndex) {
            0 -> "A"
            1 -> "B"
            else -> "C"
        }
        if (tier == 1) {
            val t1 = getTier1Options(type).firstOrNull { it.branchKey == branchKey }
            return PathUpgrade(t1?.subName ?: "Branch $branchKey", t1?.description ?: "", t1?.cost ?: (type.baseCost * 0.8f).toInt())
        } else {
            val ff = get9FinalForms(type).firstOrNull { it.code.startsWith(branchKey) }
            return PathUpgrade(ff?.subName ?: "Final Form", ff?.description ?: "", ff?.cost ?: (type.baseCost * 1.5f).toInt())
        }
    }

    fun getUpgradeCost(tower: TowerInstance, pathIndex: Int, tier: Int, allTowers: List<TowerInstance>): Int {
        val branchKey = when (pathIndex) {
            0 -> "A"
            1 -> "B"
            else -> "C"
        }
        return if (tier == 1) {
            getTier1Cost(tower, branchKey)
        } else {
            getFinalFormCost(tower, "$branchKey.a", allTowers)
        }
    }

    fun getAscensionCost(tower: TowerInstance): Int {
        return (tower.type.baseCost * 2.0f).toInt()
    }

    fun computeActiveSynergies(towers: List<TowerInstance>): List<GlobalAuraSynergy> {
        val counts = LinkedHashMap<Pair<TowerArchetype, Int>, Int>()
        for (i in towers.indices) {
            val t = towers[i]
            if (t.isTier3) {
                val arch = t.archetype ?: mapTypeToArchetype(t.type)
                val key = arch to t.selectedAuraIndex
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
        val activeList = counts.map { (key, count) ->
            val (arch, auraIdx) = key
            val aura = if (auraIdx == 1) arch.secondaryAura else arch.primaryAura
            val emoji = when (arch.towerClass) {
                TowerClass.KINETIC_BALLISTIC -> "⚔️"
                TowerClass.ELEMENTAL_VOID -> "✨"
                TowerClass.HEAVY_SIEGE -> "💥"
                TowerClass.STELLAR_COSMIC -> "★"
            }
            GlobalAuraSynergy(aura.name, aura.description, count, emoji, arch.color)
        }.toMutableList()

        val capstone = towers.firstOrNull { it.isCapstone }
        if (capstone != null && capstone.synergyVault.isNotEmpty()) {
            for (vault in capstone.synergyVault) {
                for (bs in vault.synergies) {
                    val idx = activeList.indexOfFirst { it.auraName == bs.auraName }
                    if (idx >= 0) activeList[idx] = activeList[idx].copy(sourceCount = activeList[idx].sourceCount + bs.sourceCount)
                    else activeList.add(bs)
                }
            }
        }
        return activeList
    }

    data class GlobalAuraModifiers(
        var fireRateBonus: Float = 0f,
        var rangeBonus: Float = 0f,
        var damageBonus: Float = 0f,
        var slowBonus: Float = 0f,
        var critRateBonus: Float = 0f,
        var critMultiplierBonus: Float = 0f,
        var armorIgnore: Float = 0f,
        var scrapBonus: Float = 0f
    ) {
        fun reset() {
            fireRateBonus = 0f
            rangeBonus = 0f
            damageBonus = 0f
            slowBonus = 0f
            critRateBonus = 0f
            critMultiplierBonus = 0f
            armorIgnore = 0f
            scrapBonus = 0f
        }
    }

    private val AURA_LOOKUP_BY_NAME: Map<String, Pair<TowerArchetype, Int>> = buildMap {
        for (arch in TowerArchetype.values()) {
            put(arch.primaryAura.name, arch to 0)
            put(arch.secondaryAura.name, arch to 1)
        }
    }

    fun accumulateTier3AuraModifiers(towers: List<TowerInstance>, out: GlobalAuraModifiers) {
        out.reset()
        for (i in towers.indices) {
            val t = towers[i]
            if (t.isTier3) {
                val arch = t.archetype ?: mapTypeToArchetype(t.type)
                applyArchetypeAura(arch, t.selectedAuraIndex, 1, out)
            }
            if (t.isCapstone && t.synergyVault.isNotEmpty()) {
                for (vIdx in t.synergyVault.indices) {
                    val vault = t.synergyVault[vIdx]
                    for (sIdx in vault.synergies.indices) {
                        val syn = vault.synergies[sIdx]
                        val mapped = AURA_LOOKUP_BY_NAME[syn.auraName]
                        if (mapped != null) {
                            applyArchetypeAura(mapped.first, mapped.second, syn.sourceCount, out)
                        }
                    }
                }
            }
        }
        out.slowBonus = out.slowBonus.coerceIn(0f, 0.85f)
        out.armorIgnore = out.armorIgnore.coerceIn(0f, 1.0f)
    }

    fun applyArchetypeAura(
        archetype: TowerArchetype,
        auraIndex: Int,
        sourceCount: Int,
        out: GlobalAuraModifiers
    ) {
        val count = sourceCount.coerceAtLeast(1)
        val isPrimary = (auraIndex != 1)
        when (archetype) {
            TowerArchetype.VULCAN_GATLING -> if (isPrimary) out.fireRateBonus += 0.18f * count else out.fireRateBonus += 0.15f * count
            TowerArchetype.FLAK_CANNON -> if (isPrimary) out.damageBonus += 0.12f * count else out.damageBonus += 0.15f * count
            TowerArchetype.SNIPER_SENTRY -> if (isPrimary) out.damageBonus += 0.18f * count else out.critRateBonus += 0.10f * count
            TowerArchetype.BARRICADE_TURRET -> if (isPrimary) out.slowBonus = maxOf(out.slowBonus, 0.12f) else out.damageBonus += 0.10f * count
            TowerArchetype.MINE_DISPENSER -> if (isPrimary) out.damageBonus += 0.15f * count else out.slowBonus = maxOf(out.slowBonus, 0.20f)
            TowerArchetype.RIOT_DISRUPTOR -> out.slowBonus = maxOf(out.slowBonus, 0.15f)
            TowerArchetype.RADAR_SENTRY -> if (isPrimary) out.rangeBonus += 0.25f * count else out.critRateBonus += 0.20f * count
            TowerArchetype.AMMO_SUPPLY_DEPOT -> if (isPrimary) out.scrapBonus += 0.15f * count else out.damageBonus += 0.15f * count
            TowerArchetype.SALVAGE_CRUSHER -> if (isPrimary) out.scrapBonus += 0.35f * count else out.scrapBonus += 0.20f * count

            TowerArchetype.SOLAR_DEATH_RAY -> if (isPrimary) {
                out.critRateBonus += 0.25f * count
                out.critMultiplierBonus = maxOf(out.critMultiplierBonus, 0.50f)
            } else {
                out.armorIgnore = 1.0f
            }
            TowerArchetype.INFERNO_CANNON -> if (isPrimary) out.damageBonus += 0.15f * count else out.damageBonus += 0.18f * count
            TowerArchetype.PLASMA_PULSAR -> if (isPrimary) out.damageBonus += 0.20f * count else out.damageBonus += 0.25f * count
            TowerArchetype.FROST_SPIRE -> if (isPrimary) out.slowBonus = maxOf(out.slowBonus, 0.18f) else out.damageBonus += 0.22f * count
            TowerArchetype.TESLA_STORMCOIL -> if (isPrimary) out.damageBonus += 0.15f * count else out.fireRateBonus += 0.25f * count
            TowerArchetype.GRAVITON_VOID -> out.slowBonus = maxOf(out.slowBonus, if (isPrimary) 0.20f else 0.25f)
            TowerArchetype.DYNAMO_CONDUIT -> if (isPrimary) out.scrapBonus += 0.40f * count else out.scrapBonus += 0.25f * count
            TowerArchetype.QUANTUM_BEACON -> if (isPrimary) out.rangeBonus += 0.25f * count else out.fireRateBonus += 0.20f * count
            TowerArchetype.DARK_MATTER_FORGE -> if (isPrimary) {
                out.armorIgnore = maxOf(out.armorIgnore, 0.40f)
            } else {
                out.armorIgnore = maxOf(out.armorIgnore, 0.25f)
                out.damageBonus += 0.10f * count
            }

            TowerArchetype.HELLFIRE_MORTAR -> if (isPrimary) out.damageBonus += 0.25f * count else out.damageBonus += 0.22f * count
            TowerArchetype.TITAN_RAILGUN -> if (isPrimary) out.damageBonus += 0.25f * count else out.armorIgnore = maxOf(out.armorIgnore, 0.50f)
            TowerArchetype.ORBITAL_STRIKE_RELAY -> if (isPrimary) out.damageBonus += 0.22f * count else out.rangeBonus += 0.35f * count
            TowerArchetype.EARTHQUAKE_STAMPER -> if (isPrimary) out.slowBonus = maxOf(out.slowBonus, 0.20f) else out.damageBonus += 0.12f * count
            TowerArchetype.CRYO_FORTRESS -> out.slowBonus = maxOf(out.slowBonus, if (isPrimary) 0.22f else 0.20f)
            TowerArchetype.SHOCKWAVE_BASTION -> if (isPrimary) out.slowBonus = maxOf(out.slowBonus, 0.18f) else out.armorIgnore = maxOf(out.armorIgnore, 0.50f)
            TowerArchetype.NANITE_FORGE -> if (isPrimary) out.armorIgnore = maxOf(out.armorIgnore, 0.45f) else out.fireRateBonus += 0.15f * count
            TowerArchetype.SIEGE_COMMAND_POST -> if (isPrimary) {
                out.damageBonus += 0.25f * count
            } else {
                out.fireRateBonus += 0.20f * count
                out.rangeBonus += 0.15f * count
            }
            TowerArchetype.WAR_CHEST_VAULT -> if (isPrimary) out.scrapBonus += 0.35f * count else out.scrapBonus += 0.30f * count

            TowerArchetype.SUPERNOVA_PULSAR -> if (isPrimary) out.damageBonus += 0.25f * count else out.slowBonus = maxOf(out.slowBonus, 0.25f)
            TowerArchetype.GRAVITON_LANCE -> out.slowBonus = maxOf(out.slowBonus, if (isPrimary) 0.20f else 0.25f)
            TowerArchetype.CELESTIAL_COMET -> if (isPrimary) out.damageBonus += 0.22f * count else out.damageBonus += 0.25f * count
            TowerArchetype.EVENT_HORIZON -> out.slowBonus = maxOf(out.slowBonus, if (isPrimary) 0.30f else 0.28f)
            TowerArchetype.NEBULA_AEGIS -> if (isPrimary) out.fireRateBonus += 0.20f * count else out.slowBonus = maxOf(out.slowBonus, 0.15f)
            TowerArchetype.BLACK_HOLE_SENTRY -> if (isPrimary) out.slowBonus = maxOf(out.slowBonus, 0.22f) else out.damageBonus += 0.25f * count
            TowerArchetype.CHRONO_PYLON -> if (isPrimary) out.fireRateBonus += 0.25f * count else out.scrapBonus += 0.15f * count
            TowerArchetype.STARLIGHT_BEACON -> if (isPrimary) out.critRateBonus += 0.25f * count else out.armorIgnore = maxOf(out.armorIgnore, 0.50f)
            TowerArchetype.ASTRAL_HARVESTER -> if (isPrimary) out.scrapBonus += 0.40f * count else out.scrapBonus += 0.35f * count
        }
    }
}

val TowerType.ascensionGlobalBuff: String
    get() = when (this) {
        TowerType.GATLING -> "+18% Fire Rate to ALL Towers"
        TowerType.CRYO -> "15% Global Creep Slowdown"
        TowerType.TESLA -> "Chain Lightning Grid Online"
        TowerType.MORTAR -> "+30% Explosive Blast Radius"
        TowerType.LASER -> "+20% Global Critical Strike Rate"
        TowerType.VOID -> "Zero Armor Protocol & +35% Scrap"
        TowerType.STELLAR -> "+25% Cosmic Gravity Well & +20% Scrap"
    }
