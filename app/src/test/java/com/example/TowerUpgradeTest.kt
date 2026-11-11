package com.example

import com.example.engine.TowerUpgradeManager
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Quinn (Lead QA & Test Architect)
 * Domain Test Suite for Floating Squircle Upgrade Action Bar (Track 2).
 * Verifies squircle upgrade costs, unlock criteria across 4 classes, and tier vectors.
 */
class TowerUpgradeTest {

    @Test
    fun `Tower classes provide 4 archetypes with distinct base costs`() {
        val classes = TowerUpgradeManager.getTowerClasses()
        assertEquals(4, classes.size)

        val kinetic = classes.first { it == TowerClass.KINETIC_BALLISTIC }
        val elemental = classes.first { it == TowerClass.ELEMENTAL_VOID }
        val siege = classes.first { it == TowerClass.HEAVY_SIEGE }
        val stellar = classes.first { it == TowerClass.STELLAR_COSMIC }

        assertEquals(80, kinetic.baseCost)
        assertEquals(140, elemental.baseCost)
        assertEquals(210, siege.baseCost)
        assertEquals(180, stellar.baseCost)
    }

    @Test
    fun `Class unlock requirements gate higher tiers based on Tier 3 ascensions`() {
        val emptyTowers = emptyList<TowerInstance>()
        assertTrue("Kinetic Ballistic must always be unlocked", TowerUpgradeManager.isClassUnlocked(TowerClass.KINETIC_BALLISTIC, emptyTowers))
        assertFalse("Elemental Void requires 1 Tier 3 tower", TowerUpgradeManager.isClassUnlocked(TowerClass.ELEMENTAL_VOID, emptyTowers))
        assertFalse("Heavy Siege requires 2 Tier 3 towers", TowerUpgradeManager.isClassUnlocked(TowerClass.HEAVY_SIEGE, emptyTowers))
        assertFalse("Stellar Cosmic requires 3 Tier 3 towers", TowerUpgradeManager.isClassUnlocked(TowerClass.STELLAR_COSMIC, emptyTowers))

        val oneTier3 = listOf(
            TowerInstance(id = "1", gridPos = GridPos(0, 0), tier = 3)
        )
        assertTrue(TowerUpgradeManager.isClassUnlocked(TowerClass.ELEMENTAL_VOID, oneTier3))
        assertFalse(TowerUpgradeManager.isClassUnlocked(TowerClass.HEAVY_SIEGE, oneTier3))

        val twoTier3 = listOf(
            TowerInstance(id = "1", gridPos = GridPos(0, 0), tier = 3),
            TowerInstance(id = "2", gridPos = GridPos(0, 1), tier = 3)
        )
        assertTrue(TowerUpgradeManager.isClassUnlocked(TowerClass.HEAVY_SIEGE, twoTier3))
        assertFalse(TowerUpgradeManager.isClassUnlocked(TowerClass.STELLAR_COSMIC, twoTier3))

        val threeTier3 = listOf(
            TowerInstance(id = "1", gridPos = GridPos(0, 0), tier = 3),
            TowerInstance(id = "2", gridPos = GridPos(0, 1), tier = 3),
            TowerInstance(id = "3", gridPos = GridPos(0, 2), tier = 3)
        )
        assertTrue("Stellar Cosmic unlocked with 3 Tier 3 towers", TowerUpgradeManager.isClassUnlocked(TowerClass.STELLAR_COSMIC, threeTier3))
    }

    @Test
    fun `Tier 1 Vectors provide exactly 3 options per class with correct costs`() {
        val vectors = TowerUpgradeManager.getTier1Vectors()
        assertEquals(3, vectors.size)
        assertTrue(vectors.contains(TowerVector.OFFENSIVE))
        assertTrue(vectors.contains(TowerVector.DEFENSIVE))
        assertTrue(vectors.contains(TowerVector.SUPPORT))

        val kineticCost = TowerUpgradeManager.getTier1VectorCost(TowerClass.KINETIC_BALLISTIC)
        val stellarCost = TowerUpgradeManager.getTier1VectorCost(TowerClass.STELLAR_COSMIC)
        assertTrue("Stellar vector cost must exceed kinetic vector cost", stellarCost > kineticCost)
    }
}
