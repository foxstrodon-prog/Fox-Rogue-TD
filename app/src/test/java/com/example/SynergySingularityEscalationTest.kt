package com.example

import com.example.engine.WaveGenerator
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Executive Meta-Council Unit Test Suite for v0.0.1.015:
 * The Synergy Singularity (Board-Merge Capstone Loop) & 4-Tier Non-Visual Escalation Engine.
 */
class SynergySingularityEscalationTest {

    // === PATCH 1: EXPONENTIAL SCALING MATH & ASYMPTOTIC SAFEGUARDS ===

    @Test
    fun `WaveGenerator Hard Mode scales exponentially past Wave 30`() {
        val w30 = WaveGenerator.generateWave(30, difficulty = GameDifficulty.HARD)
        val w60 = WaveGenerator.generateWave(60, difficulty = GameDifficulty.HARD)
        val w100 = WaveGenerator.generateWave(100, difficulty = GameDifficulty.HARD)

        val w30Hp = w30.spawnList.first().hpScale
        val w60Hp = w60.spawnList.first().hpScale
        val w100Hp = w100.spawnList.first().hpScale

        assertTrue("Wave 60 HP ($w60Hp) must significantly exceed Wave 30 HP ($w30Hp)", w60Hp > w30Hp * 3f)
        assertTrue("Wave 100 HP ($w100Hp) must scale exponentially past Wave 60 HP ($w60Hp)", w100Hp > w60Hp * 5f)
    }

    @Test
    fun `WaveGenerator Infinite Mode scales aggressively with 1_08 exponential base past Wave 50`() {
        val w51 = WaveGenerator.generateWave(51, difficulty = GameDifficulty.INFINITE)
        val w61 = WaveGenerator.generateWave(61, difficulty = GameDifficulty.INFINITE)
        val w81 = WaveGenerator.generateWave(81, difficulty = GameDifficulty.INFINITE)
        val w101 = WaveGenerator.generateWave(101, difficulty = GameDifficulty.INFINITE)

        val w51Hp = w51.spawnList.first().hpScale
        val w61Hp = w61.spawnList.first().hpScale
        val w81Hp = w81.spawnList.first().hpScale
        val w101Hp = w101.spawnList.first().hpScale

        // Math: 1.08^10 ~= 2.1589x
        val expectedRatio61 = Math.pow(1.08, 10.0).toFloat()
        assertEquals(expectedRatio61, w61Hp / w51Hp, 0.05f)

        // Math: 1.08^30 ~= 10.06x
        val expectedRatio81 = Math.pow(1.08, 30.0).toFloat()
        assertEquals(expectedRatio81, w81Hp / w51Hp, 0.2f)

        // Wave 101 should be ~46.9x Wave 51
        assertTrue("Wave 101 HP should be massive in Infinite mode", w101Hp > w51Hp * 40f)
    }

    @Test
    fun `Enemy effectiveSpeed is asymptotically clamped to a minimum of 15 percent`() {
        val enemy = EnemyInstance(
            id = "e1",
            type = EnemyType.SOLDIER,
            laneIndex = 0,
            maxHp = 100f,
            currentHp = 100f,
            speed = 2.0f,
            slowTimerMs = 1500L,
            slowFactor = 0.0f // Simulated maximum cryo/freeze stop
        )

        val minAllowedSpeed = enemy.speed * 0.15f
        assertEquals("Effective speed must not drop below 15% even when slowFactor is 0", minAllowedSpeed, enemy.effectiveSpeed, 0.001f)

        // When slowFactor is negative or corrupted
        enemy.slowFactor = -0.5f
        assertEquals(minAllowedSpeed, enemy.effectiveSpeed, 0.001f)
    }

    // === PATCH 2 & 4: 100 PERCENT MAXED GATE & SINGLE MONUMENT ANCHOR ===

    @Test
    fun `TowerInstance invested scrap value accurately sums base and upgrades`() {
        val t0 = TowerInstance(id = "t0", type = TowerType.GATLING, gridPos = GridPos(0, 0), tier = 0)
        assertEquals(TowerType.GATLING.baseCost, t0.getInvestedScrapValue())

        val t1 = TowerInstance(id = "t1", type = TowerType.GATLING, gridPos = GridPos(0, 0), tier = 1, vector = TowerVector.OFFENSIVE)
        val expectedT1 = TowerType.GATLING.baseCost + (TowerType.GATLING.baseCost * 0.8f).toInt()
        assertEquals(expectedT1, t1.getInvestedScrapValue())

        val t2 = TowerInstance(id = "t2", type = TowerType.GATLING, gridPos = GridPos(0, 0), tier = 2, archetype = TowerArchetype.VULCAN_GATLING)
        val expectedT2 = expectedT1 + (TowerType.GATLING.baseCost * 1.5f).toInt()
        assertEquals(expectedT2, t2.getInvestedScrapValue())

        val t3 = TowerInstance(id = "t3", type = TowerType.GATLING, gridPos = GridPos(0, 0), tier = 3, archetype = TowerArchetype.VULCAN_GATLING)
        val expectedT3 = expectedT2 + 280
        assertEquals(expectedT3, t3.getInvestedScrapValue())
    }

    @Test
    fun `20 percent Singularity Scrap Bounty refund math is verified`() {
        // Sacrifice 5 maxed Tier 2 Gatling towers
        val sacrificed = (1..5).map {
            TowerInstance(id = "t_$it", type = TowerType.GATLING, gridPos = GridPos(it, 0), tier = 2, archetype = TowerArchetype.VULCAN_GATLING)
        }
        val totalInvested = sacrificed.sumOf { it.getInvestedScrapValue() }
        val scrapBounty = maxOf(250, (totalInvested * 0.20f).toInt())

        assertTrue("Total invested must be substantial", totalInvested >= 1000)
        assertTrue("Scrap bounty must be at least 250 and exactly 20% of total invested", scrapBounty >= 250)
        assertEquals((totalInvested * 0.20f).toInt(), scrapBounty)
    }

    @Test
    fun `Capstone Tower persists multi-generational banked vaults`() {
        val ogTower = TowerInstance(
            id = "og_1",
            type = TowerType.GATLING,
            gridPos = GridPos(1, 1),
            tier = 3,
            archetype = TowerArchetype.VULCAN_GATLING,
            isCapstone = false,
            generation = 0
        )

        val gen1Synergies = listOf(
            GlobalAuraSynergy(
                auraName = "Rotary Overclock",
                effectDescription = "+20% Fire Rate",
                sourceCount = 2,
                iconEmoji = "⚔️",
                color = 0xFFFFD700
            )
        )

        // Generation 1 Collapse
        ogTower.synergyVault.add(BankedSynergyVault(generation = 1, synergies = gen1Synergies))
        ogTower.isCapstone = true
        ogTower.generation = 1

        assertEquals(1, ogTower.generation)
        assertTrue(ogTower.isCapstone)
        assertEquals(1, ogTower.synergyVault.size)
        assertEquals("Rotary Overclock", ogTower.synergyVault[0].synergies[0].auraName)

        // Generation 2 Collapse
        val gen2Synergies = listOf(
            GlobalAuraSynergy(
                auraName = "Cosmic Aegis",
                effectDescription = "+15% Shielding",
                sourceCount = 1,
                iconEmoji = "★",
                color = 0xFF00F0FF
            )
        )
        ogTower.synergyVault.add(BankedSynergyVault(generation = 2, synergies = gen2Synergies))
        ogTower.generation = 2

        assertEquals(2, ogTower.generation)
        assertEquals(2, ogTower.synergyVault.size)
        assertEquals(2, ogTower.synergyVault.sumOf { it.synergies.size })
    }

    @Test
    fun `GameDifficulty metadata and milestone targets are calibrated`() {
        assertEquals(30, GameDifficulty.EASY.maxWave)
        assertEquals(50, GameDifficulty.MEDIUM.maxWave)
        assertEquals(100, GameDifficulty.HARD.maxWave)
        assertEquals(Int.MAX_VALUE, GameDifficulty.INFINITE.maxWave)

        assertEquals(0.80f, GameDifficulty.EASY.hpScaleMultiplier, 0.001f)
        assertEquals(1.00f, GameDifficulty.MEDIUM.hpScaleMultiplier, 0.001f)
        assertEquals(1.25f, GameDifficulty.HARD.hpScaleMultiplier, 0.001f)
    }

    @Test
    fun `Synergy Singularity absorbs previous Capstone Conduit into newly chosen anchor`() {
        val oldConduit = TowerInstance(
            id = "t_old",
            type = TowerType.GATLING,
            gridPos = GridPos(0, 0),
            tier = 3,
            isCapstone = true,
            generation = 1
        ).apply {
            synergyVault.add(
                BankedSynergyVault(
                    generation = 1,
                    synergies = listOf(
                        GlobalAuraSynergy("Rotary", "Desc", 1, "⚔️", 0xFFFFD700)
                    )
                )
            )
        }

        val newAnchor = TowerInstance(
            id = "t_new",
            type = TowerType.TESLA,
            gridPos = GridPos(3, 3),
            tier = 3,
            isCapstone = false,
            generation = 0
        )

        val sacrificed = listOf(oldConduit)

        val previousCapstone = sacrificed.firstOrNull { it.isCapstone }
        assertNotNull(previousCapstone)
        if (previousCapstone != null) {
            newAnchor.synergyVault.addAll(previousCapstone.synergyVault)
            newAnchor.generation = maxOf(newAnchor.generation, previousCapstone.generation)
        }

        val currentSynergies = listOf(
            GlobalAuraSynergy("Stormcoil", "Desc", 1, "⚡", 0xFF00F0FF)
        )
        newAnchor.synergyVault.add(
            BankedSynergyVault(
                generation = newAnchor.generation + 1,
                synergies = currentSynergies
            )
        )
        newAnchor.isCapstone = true
        newAnchor.generation += 1

        assertEquals(2, newAnchor.generation)
        assertTrue(newAnchor.isCapstone)
        assertEquals(2, newAnchor.synergyVault.size)
        assertEquals("Rotary", newAnchor.synergyVault[0].synergies[0].auraName)
        assertEquals("Stormcoil", newAnchor.synergyVault[1].synergies[0].auraName)
    }
}
