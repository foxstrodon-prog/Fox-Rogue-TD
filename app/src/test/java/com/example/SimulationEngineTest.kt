package com.example

import com.example.engine.SpatialHashGrid
import com.example.model.EnemyInstance
import com.example.model.EnemyType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Quinn (Lead Quality Engineer) & Leo (Physics & Performance)
 * Unit Test Suite for Simulation Engine & Spatial Partitioning Optimization.
 */
class SimulationEngineTest {

    private lateinit var spatialGrid: SpatialHashGrid

    private fun createEnemy(
        id: String,
        type: EnemyType = EnemyType.DRONE,
        x: Float = 0f,
        y: Float = 0f,
        hp: Float = 100f
    ): EnemyInstance {
        return EnemyInstance(
            id = id,
            type = type,
            laneIndex = 0,
            currentPathSegmentIndex = 0,
            segmentProgress = 0f,
            maxHp = hp,
            currentHp = hp,
            speed = 1.0f,
            worldX = x,
            worldY = y
        )
    }

    @Before
    fun setUp() {
        // 16x16 grid with 2.0f cell size
        spatialGrid = SpatialHashGrid(cellSize = 2.0f, gridWidth = 16, gridHeight = 16)
    }

    @Test
    fun `SpatialHashGrid rebuild correctly indexes non-dead enemies and clears previous entries`() {
        val e1 = createEnemy("1", x = 3f, y = 3f)
        val e2 = createEnemy("2", x = 5f, y = 5f)
        val e3Dead = createEnemy("3", x = 3f, y = 3f, hp = 0f) // currentHp = 0f -> isDead = true

        spatialGrid.rebuild(listOf(e1, e2, e3Dead))

        val results = spatialGrid.queryNearby(3f, 3f, 1.0f)
        assertEquals("Should only contain e1; e3Dead must be excluded", 1, results.size)
        assertEquals("1", results[0].id)

        // Test clear
        spatialGrid.clear()
        val emptyResults = spatialGrid.queryNearby(3f, 3f, 5.0f)
        assertTrue("Grid should be empty after clear", emptyResults.isEmpty())
    }

    @Test
    fun `SpatialHashGrid queryNearby filters strictly within circular radius`() {
        val center = createEnemy("1", x = 10f, y = 10f)
        val inRange = createEnemy("2", x = 11.5f, y = 10f) // distance = 1.5 <= 2.0
        val outOfRange = createEnemy("3", x = 12.5f, y = 10f) // distance = 2.5 > 2.0

        spatialGrid.rebuild(listOf(center, inRange, outOfRange))

        val nearby = spatialGrid.queryNearby(10f, 10f, 2.0f)
        assertEquals(2, nearby.size)
        assertTrue(nearby.any { it.id == "1" })
        assertTrue(nearby.any { it.id == "2" })
        assertFalse(nearby.any { it.id == "3" })
    }

    @Test
    fun `SpatialHashGrid findClosest accurately selects the nearest hostile`() {
        val eFar = createEnemy("1", x = 14f, y = 10f)    // dist = 4.0
        val eNear = createEnemy("2", x = 11f, y = 10f)   // dist = 1.0
        val eMedium = createEnemy("3", x = 12f, y = 10f) // dist = 2.0

        spatialGrid.rebuild(listOf(eFar, eNear, eMedium))

        val closest = spatialGrid.findClosest(10f, 10f, 5.0f)
        assertNotNull(closest)
        assertEquals("2", closest!!.id)

        // When out of maxRadius, return null
        val noneFound = spatialGrid.findClosest(10f, 10f, 0.5f)
        assertNull(noneFound)
    }

    @Test
    fun `SpatialHashGrid boundary coords do not throw ArrayIndexOutOfBounds`() {
        val negEnemy = createEnemy("1", x = -10f, y = -10f)
        val overflowEnemy = createEnemy("2", x = 100f, y = 100f)

        // Should clamp cleanly to grid bucket indices 0 and max
        spatialGrid.rebuild(listOf(negEnemy, overflowEnemy))

        val queryNeg = spatialGrid.queryNearby(-10f, -10f, 1.0f)
        assertNotNull(queryNeg)

        val queryOverflow = spatialGrid.queryNearby(100f, 100f, 1.0f)
        assertNotNull(queryOverflow)
    }

    @Test
    fun `Projectile collision simulation reduces hostile HP and sets isDead`() {
        val enemy = createEnemy("1", x = 5f, y = 5f, hp = 50f)
        val bulletDamage = 60f

        // Simulate projectile impact
        enemy.currentHp -= bulletDamage
        if (enemy.currentHp <= 0f) {
            enemy.currentHp = 0f
        }

        assertTrue("Enemy should be marked dead when damage exceeds HP", enemy.isDead)
        assertEquals(0f, enemy.currentHp, 0.001f)
    }
}
