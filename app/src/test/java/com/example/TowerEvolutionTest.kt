package com.example

import com.example.engine.TowerUpgradeManager
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Quinn (Quality Engineer) & Dr. Alan (Architecture)
 * Unit Test Suite for Tower Evolution Pipeline (3 -> 9 -> 27 branching hierarchy, 39 total variants, L1-L4 stat scaling).
 */
class TowerEvolutionTest {

    @Test
    fun `Each base tower type provides exactly 3 Tier 1 branching options`() {
        val baseTypes = listOf(TowerType.GATLING, TowerType.CRYO, TowerType.TESLA)
        for (type in baseTypes) {
            val options = TowerUpgradeManager.getTier1Options(type)
            assertEquals("Tower $type must have exactly 3 Tier 1 options", 3, options.size)
            assertEquals("Branch keys must be A, B, C", listOf("A", "B", "C"), options.map { it.branchKey })
        }
    }

    @Test
    fun `Each base tower type provides exactly 9 Final Forms across the 3 branches`() {
        val baseTypes = listOf(TowerType.GATLING, TowerType.CRYO, TowerType.TESLA)
        for (type in baseTypes) {
            val all9 = TowerUpgradeManager.get9FinalForms(type)
            assertEquals("Tower $type must define exactly 9 Final Forms", 9, all9.size)

            val branchA = TowerUpgradeManager.getFinalFormOptions(type, "A")
            val branchB = TowerUpgradeManager.getFinalFormOptions(type, "B")
            val branchC = TowerUpgradeManager.getFinalFormOptions(type, "C")

            assertEquals("Branch A must contain 3 options", 3, branchA.size)
            assertEquals("Branch B must contain 3 options", 3, branchB.size)
            assertEquals("Branch C must contain 3 options", 3, branchC.size)

            assertTrue(branchA.all { it.code.startsWith("A.") })
            assertTrue(branchB.all { it.code.startsWith("B.") })
            assertTrue(branchC.all { it.code.startsWith("C.") })
        }
    }

    @Test
    fun `DevBalanceConfig getAll39TowerDescriptors generates exactly 39 unique tower forms`() {
        val descriptors = DevBalanceConfig.getAll39TowerDescriptors()
        assertEquals("Total descriptors must be 39 (3 L1 + 9 L2 + 27 L3)", 39, descriptors.size)

        val l1Count = descriptors.count { it.level == 1 }
        val l2Count = descriptors.count { it.level == 2 }
        val l3Count = descriptors.count { it.level == 3 }

        assertEquals(3, l1Count)
        assertEquals(9, l2Count)
        assertEquals(27, l3Count)

        val uniqueKeys = descriptors.map { it.key }.toSet()
        assertEquals("Every tower form descriptor key must be distinct", 39, uniqueKeys.size)
    }

    @Test
    fun `DevBalanceConfig getAll52TowerDescriptors generates exactly 52 unique tower forms across 4 classes`() {
        val descriptors = DevBalanceConfig.getAll52TowerDescriptors()
        assertEquals("Total descriptors must be 52 (4 L1 + 12 L2 + 36 L3)", 52, descriptors.size)

        val l1Count = descriptors.count { it.level == 1 }
        val l2Count = descriptors.count { it.level == 2 }
        val l3Count = descriptors.count { it.level == 3 }

        assertEquals(4, l1Count)
        assertEquals(12, l2Count)
        assertEquals(36, l3Count)

        val uniqueKeys = descriptors.map { it.key }.toSet()
        assertEquals("Every tower form descriptor key must be distinct across 52 units", 52, uniqueKeys.size)
    }

    @Test
    fun `TowerInstance stat scaling reflects progression from L1 to Final Form`() {
        val baseTower = TowerInstance(
            id = "t1",
            type = TowerType.GATLING,
            gridPos = GridPos(2, 2),
            tier = 0
        )
        val baseDps = baseTower.getEffectiveDps()
        assertTrue("Base DPS must be positive", baseDps > 0f)

        // Evolve to Tier 1 Branch A ("Overdrive Rotor")
        val evolvedT1 = TowerInstance(
            id = "t2",
            type = TowerType.GATLING,
            gridPos = GridPos(2, 2),
            branchTier1 = "A"
        )
        val t1Dps = evolvedT1.getEffectiveDps()
        assertTrue("Tier 1 Overdrive Rotor DPS should exceed base", t1Dps > baseDps)

        // Evolve to Final Form A.a ("Hyper-Gatling")
        val evolvedL3 = TowerInstance(
            id = "t3",
            type = TowerType.GATLING,
            gridPos = GridPos(2, 2),
            branchTier1 = "A",
            finalFormCode = "A.a"
        )
        val l3Dps = evolvedL3.getEffectiveDps()
        assertTrue("Final Form Hyper-Gatling DPS should significantly exceed Tier 1", l3Dps > t1Dps)
        assertTrue("Final Form must be marked isFinalForm", evolvedL3.isFinalForm)
    }

    @Test
    fun `Tower archetype aura selection toggles correctly between primary and secondary`() {
        val archetype = TowerArchetype.VULCAN_GATLING
        val tower = TowerInstance(
            id = "t4",
            type = TowerType.GATLING,
            gridPos = GridPos(1, 1),
            archetype = archetype,
            tier = 3,
            selectedAuraIndex = 0
        )

        assertEquals(archetype.primaryAura, tower.activeAura)

        tower.selectedAuraIndex = 1
        assertEquals(archetype.secondaryAura, tower.activeAura)
    }

    @Test
    fun `getEffectiveTowerBalance and TowerUpgradeManager return cached static instances with zero heap allocation`() {
        val config = DevBalanceConfig()
        val b1 = config.getEffectiveTowerBalance(TowerType.GATLING, "A", "A.a")
        val b2 = config.getEffectiveTowerBalance(TowerType.GATLING, "A", "A.a")
        assertSame("Repeated getEffectiveTowerBalance calls must return identical pre-allocated reference", b1, b2)

        val ff1 = TowerUpgradeManager.getFinalForm(TowerType.TESLA, "B.c")
        val ff2 = TowerUpgradeManager.getFinalForm(TowerType.TESLA, "B.c")
        assertNotNull(ff1)
        assertSame("Repeated getFinalForm calls must return identical pre-allocated reference", ff1, ff2)
    }

    @Test
    fun `getEffectiveCombatRange progresses monotonically across Tiers 0 1 2 and 3 without shadowing`() {
        val t0 = TowerInstance(id = "r0", type = TowerType.GATLING, gridPos = GridPos(0, 0), tier = 0)
        val t1 = TowerInstance(id = "r1", type = TowerType.GATLING, gridPos = GridPos(0, 0), tier = 1, branchTier1 = "A", vector = TowerVector.OFFENSIVE)
        val t2 = TowerInstance(
            id = "r2",
            type = TowerType.GATLING,
            gridPos = GridPos(0, 0),
            tier = 2,
            branchTier1 = "A",
            vector = TowerVector.OFFENSIVE,
            archetype = TowerArchetype.VULCAN_GATLING,
            finalFormCode = "A.a" // rangeMultiplier = 1.0f, should still get Tier 2 1.20x bonus!
        )
        val t3 = TowerInstance(
            id = "r3",
            type = TowerType.GATLING,
            gridPos = GridPos(0, 0),
            tier = 3,
            branchTier1 = "A",
            vector = TowerVector.OFFENSIVE,
            archetype = TowerArchetype.VULCAN_GATLING,
            finalFormCode = "A.a"
        )

        val range0 = t0.getEffectiveCombatRange()
        val range1 = t1.getEffectiveCombatRange()
        val range2 = t2.getEffectiveCombatRange()
        val range3 = t3.getEffectiveCombatRange()

        assertEquals(range0, range1, 0.001f)
        assertTrue("Tier 2 range ($range2) must exceed Tier 1 range ($range1)", range2 > range1 * 1.15f)
        assertTrue("Tier 3 range ($range3) must exceed Tier 2 range ($range2)", range3 > range2 * 1.10f)
    }

    @Test
    fun `mapClassToType and mapArchetypeToType resolve weapon types and archetype fire rates without double dipping`() {
        assertEquals(TowerType.GATLING, mapClassToType(TowerClass.KINETIC_BALLISTIC))
        assertEquals(TowerType.CRYO, mapClassToType(TowerClass.ELEMENTAL_VOID))
        assertEquals(TowerType.MORTAR, mapClassToType(TowerClass.HEAVY_SIEGE))
        assertEquals(TowerType.STELLAR, mapClassToType(TowerClass.STELLAR_COSMIC))

        assertEquals(TowerType.LASER, mapArchetypeToType(TowerArchetype.SOLAR_DEATH_RAY))
        assertEquals(TowerType.CRYO, mapArchetypeToType(TowerArchetype.FROST_SPIRE))
        assertEquals(TowerType.TESLA, mapArchetypeToType(TowerArchetype.TESLA_STORMCOIL))
        assertEquals(TowerType.VOID, mapArchetypeToType(TowerArchetype.GRAVITON_VOID))
        assertEquals(TowerType.MORTAR, mapArchetypeToType(TowerArchetype.HELLFIRE_MORTAR))
        assertEquals(TowerType.STELLAR, mapArchetypeToType(TowerArchetype.SUPERNOVA_PULSAR))
        assertEquals(TowerType.GATLING, mapArchetypeToType(TowerArchetype.VULCAN_GATLING))

        val archTower = TowerInstance(
            id = "arch_t2",
            type = mapArchetypeToType(TowerArchetype.HELLFIRE_MORTAR),
            gridPos = GridPos(1, 1),
            towerClass = TowerClass.HEAVY_SIEGE,
            tier = 2,
            vector = TowerVector.OFFENSIVE,
            archetype = TowerArchetype.HELLFIRE_MORTAR,
            finalFormCode = null
        )
        assertEquals("Tier 2 archetype fire rate must match archetype.baseFireRate without 1.25x double-dip",
            TowerArchetype.HELLFIRE_MORTAR.baseFireRate,
            archTower.getEffectiveFireRate(),
            0.001f
        )
    }
}
