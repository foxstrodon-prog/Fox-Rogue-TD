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
}
