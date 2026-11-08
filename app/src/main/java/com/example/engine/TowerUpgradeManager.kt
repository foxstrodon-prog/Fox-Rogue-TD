package com.example.engine

import com.example.model.*

object TowerUpgradeManager {

    fun getTier1Options(type: TowerType): List<Tier1FormDef> {
        val baseCost = type.baseCost
        return when (type) {
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
        }
    }

    fun getFinalFormOptions(type: TowerType, branchTier1: String): List<FinalFormDef> {
        val allFinalForms = get9FinalForms(type)
        return allFinalForms.filter { it.code.startsWith(branchTier1) }
    }

    fun getFinalForm(type: TowerType, code: String): FinalFormDef? {
        return get9FinalForms(type).firstOrNull { it.code == code }
    }

    fun get9FinalForms(type: TowerType): List<FinalFormDef> {
        val baseCost = type.baseCost
        val c = (baseCost * 1.5f).toInt()

        return when (type) {
            TowerType.GATLING -> listOf(
                // Branch A (Overdrive Rotor) -> A.a, A.b, A.c
                FinalFormDef("A.a", "Hyper-Gatling", "Extreme cyclic speed firing a continuous wall of lead.", c,
                    "Global Kinetic Windup", "+18% Fire Rate to ALL towers", 1.2f, 2.4f, 1.0f),
                FinalFormDef("A.b", "Rotary Twin-Cannon", "Dual synchronized rotating barrels firing parallel tracers.", c,
                    "Twin Ballistics Grid", "All kinetic bullets have a 25% chance to fire a bonus duplicate tracer", 1.8f, 1.8f, 1.0f),
                FinalFormDef("A.c", "Nano-Chamber Feeder", "Accelerated feeding mechanism never slows down.", c,
                    "Nanite Reload Logistics", "-15% Upgrade Cost on all towers", 1.3f, 2.0f, 1.1f),

                // Branch B (Heavy Munitions) -> B.a, B.b, B.c
                FinalFormDef("B.a", "Depleted Uranium Sabot", "Heavy penetrators ignoring 100% of enemy armor.", c,
                    "Global Armor Piercing", "All defense towers ignore 25% of hostile armor", 2.4f, 1.1f, 1.0f),
                FinalFormDef("B.b", "High-Explosive Flak", "Bullets detonate on impact into mini-shrapnel bursts.", c,
                    "Flak Screen", "All kinetic rounds deal +20% splash damage", 2.0f, 1.3f, 1.0f, "splash"),
                FinalFormDef("B.c", "Executioner Caliber", "Lethal rounds dealing 3x damage against hostiles below 40% HP.", c,
                    "Execution Protocol", "All towers deal +25% damage to damaged enemies", 2.5f, 1.0f, 1.0f),

                // Branch C (Sensor Array) -> C.a, C.b, C.c
                FinalFormDef("C.a", "Sniper Sentry", "Extreme range ballistic cannon prioritizing elite targets.", c,
                    "Long-Range Optics Net", "+25% Range to ALL defense towers", 2.2f, 1.2f, 2.0f),
                FinalFormDef("C.b", "Kinetic Ricochet Net", "Rounds ricochet across up to 3 hostiles.", c,
                    "Ricochet Resonance", "Bullets that kill enemies refund +5 Credits", 1.6f, 1.5f, 1.5f, "ricochet"),
                FinalFormDef("C.c", "Automated Phalanx", "Autonomous CIWS firing micro-missiles and bullets.", c,
                    "Phalanx Interceptor", "Nexus Core regenerates +1 HP per wave", 1.7f, 1.7f, 1.4f)
            )

            TowerType.CRYO -> listOf(
                // Branch A (Sub-Zero Coolant) -> A.a, A.b, A.c
                FinalFormDef("A.a", "Absolute Zero Spire", "Beam completely freezes hostiles in place for 2 seconds.", c,
                    "Permafrost Freeze Grid", "Hostiles in both lanes are permanently slowed by 15%", 1.4f, 1.3f, 1.1f),
                FinalFormDef("A.b", "Glacial Stasis Monolith", "Emits a continuous frost field trapping swarms.", c,
                    "Stasis Field Net", "Enemies in both lanes take 20% longer to reach the Core", 1.5f, 1.2f, 1.2f),
                FinalFormDef("A.c", "Liquid Nitrogen Projector", "Flash freezes targets, reducing their armor to 0 while chilled.", c,
                    "Cryo-Vulnerability", "All frozen hostiles take +30% damage from all weapons", 1.6f, 1.3f, 1.0f),

                // Branch B (Brittle Cryo-Core) -> B.a, B.b, B.c
                FinalFormDef("B.a", "Cryo Shatterburst", "Hostiles detonate into sharp ice shards on death.", c,
                    "Shatter Chain Reaction", "When any hostile dies while slowed, it bursts dealing 20% max HP AoE", 1.9f, 1.2f, 1.0f, "shatter"),
                FinalFormDef("B.b", "Frostfire Reactor", "Sub-zero beam inflicts thermal shock damage over time.", c,
                    "Thermal Shock Wave", "+20% Elemental Damage to all towers", 2.2f, 1.1f, 1.1f),
                FinalFormDef("B.c", "Hailstorm Battery", "Lobs jagged ice boulders down both lanes.", c,
                    "Hailstorm Barrage", "All towers gain +10% Critical Strike Chance", 2.0f, 1.2f, 1.3f),

                // Branch C (Prism Lens) -> C.a, C.b, C.c
                FinalFormDef("C.a", "Blizzard Pulsar", "Emits periodic screen-wide blizzard pulses.", c,
                    "Arctic Gale", "Slows all active hostiles on the screen by 25%", 1.5f, 1.4f, 1.8f),
                FinalFormDef("C.b", "Aurora Borealis Spire", "Prismatic aurora beam illuminating weak points.", c,
                    "Aurora Matrix", "+25% Critical Hit Damage across all towers", 1.8f, 1.3f, 1.5f),
                FinalFormDef("C.c", "Sub-Zero Conduit", "Freezes enemies and siphons their energy into Credits.", c,
                    "Cryo Bounty Harvest", "+30% Credits earned from all kills in both lanes", 1.6f, 1.4f, 1.4f)
            )

            TowerType.TESLA -> listOf(
                // Branch A (Conduction Coils) -> A.a, A.b, A.c
                FinalFormDef("A.a", "Storm Web Generator", "Lightning arcs chain across up to 8 hostiles simultaneously.", c,
                    "Superconducting Chain Grid", "All defense towers occasionally discharge chain lightning bolts", 1.5f, 1.5f, 1.1f),
                FinalFormDef("A.b", "Arc Discharger", "Ultra-dense lightning arcs that jump between lanes.", c,
                    "Bifurcated Lightning Net", "Towers can shoot targets across intersecting lanes with +20% damage", 1.8f, 1.3f, 1.2f),
                FinalFormDef("A.c", "Faraday Capacitor", "Stores charge and discharges high-voltage shockwaves.", c,
                    "Capacitive Overcharge", "All tower active abilities recharge 25% faster", 1.6f, 1.4f, 1.1f),

                // Branch B (Voltage Step-Up) -> B.a, B.b, B.c
                FinalFormDef("B.a", "Gigawatt Deathcoil", "Devastating single-target thunderstrike melting bruisers.", c,
                    "High-Tension Surge", "+25% Base Damage to ALL towers", 2.6f, 1.0f, 1.1f),
                FinalFormDef("B.b", "Static Overload Core", "Shocked hostiles explode with electricity upon taking damage.", c,
                    "Static Resonance", "Damaged enemies discharge 20% of damage to nearby hostiles", 2.0f, 1.2f, 1.1f),
                FinalFormDef("B.c", "Ionization Cannon", "Fires ionized lightning beams that strip armor.", c,
                    "Ionized Plating", "Strips 40% of armor from any enemy entering the lanes", 2.2f, 1.1f, 1.2f),

                // Branch C (Flux Capacitors) -> C.a, C.b, C.c
                FinalFormDef("C.a", "Ball Lightning Emitter", "Releases rolling plasma orbs traveling down the lanes.", c,
                    "Rolling Plasma Waves", "Spawns automated plasma spheres down Lane Alpha & Beta every 15s", 1.5f, 1.8f, 1.2f),
                FinalFormDef("C.b", "Empirical Coil", "High-frequency electromagnetic pulses stunning hostiles.", c,
                    "Micro-EMP Pulse", "All towers have a 10% chance to stun hostiles for 1.0s on hit", 1.7f, 1.6f, 1.2f),
                FinalFormDef("C.c", "Lightning Rod Array", "Draws energy from defeated hostiles into free scrap.", c,
                    "Dynamo Harvesting", "+35% Scrap harvested from electrified enemies", 1.6f, 1.7f, 1.1f)
            )

            TowerType.MORTAR -> listOf(
                // Branch A (Cluster Payload) -> A.a, A.b, A.c
                FinalFormDef("A.a", "Cluster Storm Fortress", "Shells split into 5 high-explosive bomblets.", c,
                    "Orbital Cluster Shrapnel", "All explosive attacks gain +30% blast radius", 1.6f, 1.1f, 1.2f),
                FinalFormDef("A.b", "Bouncing Betty Matrix", "Lobbed munitions skip along the path before detonating.", c,
                    "Path Denial Traps", "Leaves explosive minelets along lane intersections", 1.8f, 1.1f, 1.3f),
                FinalFormDef("A.c", "Carpet Bomber Battery", "Rapid salvo of 3 mortar shells per firing cycle.", c,
                    "Saturation Bombing", "+20% Attack Speed to all artillery and heavy weapons", 1.7f, 1.4f, 1.1f),

                // Branch B (Incendiary Shells) -> B.a, B.b, B.c
                FinalFormDef("B.a", "Thermobaric Cataclysm", "Devastating vacuum blast dealing massive splash damage.", c,
                    "Cataclysmic Shockwaves", "+30% Damage against Colossus and Boss hostiles", 2.6f, 0.9f, 1.1f),
                FinalFormDef("B.b", "Hellfire Scorcher", "Covers lanes in permanent inferno magma burning for 8s.", c,
                    "Inferno Ground Heat", "All hostiles in burning zones move 20% slower", 2.1f, 1.1f, 1.1f),
                FinalFormDef("B.c", "Phosphorus Shells", "White phosphorus shells melting shields and heavy armor.", c,
                    "Thermal Dissolution", "Hostiles hit by artillery have armor reduced to 0", 2.3f, 1.0f, 1.2f),

                // Branch C (Rifled Barrel) -> C.a, C.b, C.c
                FinalFormDef("C.a", "Orbital Kinetic Sunderer", "Global-range artillery able to strike any point on the map.", c,
                    "Global Ballistic Coverage", "+35% Range to all long-range towers", 2.4f, 1.0f, 2.5f),
                FinalFormDef("C.b", "Concussion Howitzer", "Impact creates a shockwave stunning all hostiles for 2.0s.", c,
                    "Concussive Tremors", "Bosses take 50% extra impact damage", 2.1f, 1.1f, 1.5f),
                FinalFormDef("C.c", "Atomic Shell Battery", "Tactical micro-nuclear detonation annihilating swarms.", c,
                    "Scrap Melter Yield", "+40% Scrap Credits awarded at the end of each wave", 2.5f, 0.9f, 1.4f)
            )

            TowerType.LASER -> listOf(
                // Branch A (Thermal Intensity) -> A.a, A.b, A.c
                FinalFormDef("A.a", "Solar Meltdown Core", "Continuous death ray ramping damage up to 400% on bosses.", c,
                    "Thermal Meltdown", "Boss and elite enemies take 2x damage from all towers", 2.5f, 1.2f, 1.1f),
                FinalFormDef("A.b", "Gamma Ray Incinerator", "High-frequency radioactive beam melting armor immediately.", c,
                    "Gamma Radiation Field", "Hostiles damaged by lasers take +20% damage from all other towers", 2.4f, 1.1f, 1.1f),
                FinalFormDef("A.c", "Hyper-Focused Particle Lance", "Piercing lance cutting in a straight line through all creeps.", c,
                    "Line Penetration Optics", "Lasers pierce through all hostiles without damage drop-off", 2.2f, 1.3f, 1.3f),

                // Branch B (Refraction Splitter) -> B.a, B.b, B.c
                FinalFormDef("B.a", "Prism Matrix Multi-Beam", "Fires 3 continuous independent death rays.", c,
                    "Prismatic Resonance", "All laser and beam weapons gain +30% damage", 1.8f, 1.4f, 1.2f),
                FinalFormDef("B.b", "Kaleidoscope Projector", "Refracts laser into a fan of burning beams.", c,
                    "Diffuse Optics Net", "+20% Attack Range for all towers", 1.7f, 1.5f, 1.3f),
                FinalFormDef("B.c", "Orbital Solar Mirror", "Focuses orbital solar beam from the sky onto the intersection.", c,
                    "Solar Death Sweep", "Periodically burns the lane intersection for massive damage", 2.0f, 1.3f, 1.5f),

                // Branch C (Solar Resonator) -> C.a, C.b, C.c
                FinalFormDef("C.a", "Solar Flare Nova", "Pulsing thermal flare blinding and burning all hostiles.", c,
                    "Blinding Solar Flares", "All towers gain +20% Critical Chance and 2.5x Crit Damage", 1.9f, 1.8f, 1.2f),
                FinalFormDef("C.b", "Tachyon Flash Emitter", "Fires rapid-fire laser pulses like a light machine gun.", c,
                    "Tachyon Frequency", "+25% Fire Rate to all energy towers", 1.8f, 2.0f, 1.1f),
                FinalFormDef("C.c", "Solar Battery Collector", "Harnesses solar energy to mint Credits from every burn tick.", c,
                    "Solar Power Subsidy", "Generates +50 bonus Credits per wave", 1.7f, 1.6f, 1.2f)
            )

            TowerType.VOID -> listOf(
                // Branch A (Graviton Well) -> A.a, A.b, A.c
                FinalFormDef("A.a", "Black Hole Singularity", "Pulls hostiles backwards along the lanes towards the vortex.", c,
                    "Event Horizon Reversal", "All hostiles in both lanes take 25% longer to advance", 2.2f, 1.2f, 1.3f),
                FinalFormDef("A.b", "Dark Matter Siphon", "Gravitational drain that siphons enemy HP into Core shields.", c,
                    "Core Nanite Reconstruction", "Nexus Core gains a regenerating energy shield absorbing 5 hits", 2.0f, 1.2f, 1.2f),
                FinalFormDef("A.c", "Vortex Annihilator", "Crushes enemies in the vortex with intense gravity pressure.", c,
                    "Gravitational Squeeze", "Hostiles in the center take 2.5x damage", 2.5f, 1.1f, 1.1f),

                // Branch B (Armor Dissolution) -> B.a, B.b, B.c
                FinalFormDef("B.a", "Sub-Space Degenerator", "Permanently dissolves 100% of enemy armor and shields.", c,
                    "Zero Armor Protocol", "All hostiles across BOTH lanes permanently have 0 Armor", 2.2f, 1.1f, 1.2f),
                FinalFormDef("B.b", "Entropy Catalyst", "Hostiles caught in the vortex take decaying damage over time.", c,
                    "Entropic Decay", "All enemies lose 2% max HP per second while on the field", 2.0f, 1.2f, 1.1f),
                FinalFormDef("B.c", "Void Devourer", "Swallows hostiles whole when below 20% health.", c,
                    "Devour Bounty", "Instantly consumed enemies award double Credits", 2.4f, 1.1f, 1.1f),

                // Branch C (Tachyon Resonator) -> C.a, C.b, C.c
                FinalFormDef("C.a", "Dark Matter Pulse Engine", "Releases map-wide dark matter shockwaves every 3s.", c,
                    "Map-Wide Shockwave", "Pulses damage to all hostiles across the entire screen", 2.0f, 1.7f, 1.5f),
                FinalFormDef("C.b", "Spatial Distortion Field", "Warps space around the tower, doubling fire rates of adjacent towers.", c,
                    "Spatial Warp Aura", "Towers adjacent to this tower gain +40% Fire Rate", 1.8f, 1.6f, 1.2f),
                FinalFormDef("C.c", "Cosmic Singularity Bank", "Tears open cosmic rifts dropping dense scrap caches.", c,
                    "Void Scrap Yield", "+45% Scrap Credits from all defeated enemies", 1.9f, 1.5f, 1.2f)
            )
        }
    }

    fun getTier1Cost(tower: TowerInstance, branchKey: String): Int {
        val formDef = getTier1Options(tower.type).firstOrNull { it.branchKey == branchKey }
        return formDef?.cost ?: (tower.type.baseCost * 0.8f).toInt()
    }

    fun getFinalFormCost(tower: TowerInstance, code: String, allTowers: List<TowerInstance>): Int {
        val formDef = getFinalForm(tower.type, code)
        val baseCost = formDef?.cost ?: (tower.type.baseCost * 1.5f).toInt()

        // Dynamic inflation rule: When final forms exist, 2nd upgrade cost for other towers
        // scales based on currently existing towers, so selling towers stabilizes the economy!
        val finalFormsCount = allTowers.count { it.isFinalForm }
        if (finalFormsCount > 0) {
            val scale = 1.0f + (0.15f * finalFormsCount * (allTowers.size.toFloat() / 5f).coerceIn(0.6f, 1.8f))
            return (baseCost * scale).toInt()
        }
        return baseCost
    }

    fun getSellValue(tower: TowerInstance): Int {
        var invested = tower.type.baseCost
        if (tower.branchTier1 != null) {
            val t1 = getTier1Options(tower.type).firstOrNull { it.branchKey == tower.branchTier1 }
            invested += (t1?.cost ?: (tower.type.baseCost * 0.8f).toInt())
        }
        if (tower.finalFormCode != null) {
            val ff = getFinalForm(tower.type, tower.finalFormCode!!)
            invested += (ff?.cost ?: (tower.type.baseCost * 1.5f).toInt())
        }
        if (tower.archetype != null) {
            invested += tower.archetype!!.baseCost
        }
        if (tower.isTier3GlobalActive) {
            val arch = tower.archetype ?: mapTypeToArchetype(tower.type)
            invested += (arch.baseCost * 1.5f).toInt()
        }
        return (invested * 0.75f).toInt()
    }

    // --- 3-Tier Hierarchy API ---
    fun getBaseTurretCost(): Int = 80

    fun getTowerClasses(): List<TowerClass> = TowerClass.values().toList()

    fun isClassUnlocked(towerClass: TowerClass, allTowers: List<TowerInstance>): Boolean {
        val tier3Count = allTowers.count { it.isTier3 }
        return when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> true
            TowerClass.ELEMENTAL_VOID -> tier3Count >= 1 // Unlocked after one basic tower turns to level 3!
            TowerClass.HEAVY_SIEGE -> tier3Count >= 2 // Unlocked after 2 level 3 towers
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
        if (tier3Count > 0) {
            val scale = 1.0f + (0.15f * tier3Count * (allTowers.size.toFloat() / 5f).coerceIn(0.6f, 1.8f))
            return (baseCost * scale).toInt()
        }
        return baseCost
    }

    // Compatibility helpers for legacy/test paths
    data class PathUpgrade(val name: String, val description: String, val cost: Int)

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
}

val TowerType.ascensionGlobalBuff: String
    get() = when (this) {
        TowerType.GATLING -> "+18% Fire Rate to ALL Towers"
        TowerType.CRYO -> "15% Global Creep Slowdown"
        TowerType.TESLA -> "Chain Lightning Grid Online"
        TowerType.MORTAR -> "+30% Explosive Blast Radius"
        TowerType.LASER -> "+20% Global Critical Strike Rate"
        TowerType.VOID -> "Zero Armor Protocol & +35% Scrap"
    }

