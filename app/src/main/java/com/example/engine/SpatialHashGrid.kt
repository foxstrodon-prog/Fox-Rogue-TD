package com.example.engine

import com.example.model.EnemyInstance
import kotlin.math.floor

/**
 * High-performance, zero-allocation Uniform Grid Spatial Partitioning for Fox-Rogue-TD.
 *
 * Replaces naive O(N*M) projectile/tower-to-enemy distance checks with an O(N) localized bucket query.
 * Reuses internal entity reference arrays to eliminate garbage collection pressure during 60/120 FPS hot loops.
 */
class SpatialHashGrid(
    private val cellSize: Float = 2.0f,
    private val gridWidth: Int = 16,
    private val gridHeight: Int = 20
) {
    // Array of buckets, each holding an index list of enemies
    private val buckets = Array(gridWidth * gridHeight) { mutableListOf<EnemyInstance>() }
    private val queryResults = mutableListOf<EnemyInstance>()

    /**
     * Clears all buckets without reallocating underlying collection structures.
     */
    fun clear() {
        for (i in buckets.indices) {
            buckets[i].clear()
        }
    }

    /**
     * Inserts an enemy into the spatial grid based on its world coordinate.
     */
    fun insert(enemy: EnemyInstance) {
        val cellX = floor(enemy.worldX / cellSize).toInt().coerceIn(0, gridWidth - 1)
        val cellY = floor(enemy.worldY / cellSize).toInt().coerceIn(0, gridHeight - 1)
        val bucketIndex = cellY * gridWidth + cellX
        buckets[bucketIndex].add(enemy)
    }

    /**
     * Populates the grid with the current active enemies in a single pass.
     */
    fun rebuild(enemies: List<EnemyInstance>) {
        clear()
        for (i in enemies.indices) {
            val enemy = enemies[i]
            if (!enemy.isDead) {
                insert(enemy)
            }
        }
    }

    /**
     * Queries all enemies located within [radius] of ([centerX], [centerY]).
     * Returns a pooled list to avoid intermediate heap allocations.
     */
    fun queryNearby(centerX: Float, centerY: Float, radius: Float): List<EnemyInstance> {
        queryResults.clear()
        val minCellX = floor((centerX - radius) / cellSize).toInt().coerceIn(0, gridWidth - 1)
        val maxCellX = floor((centerX + radius) / cellSize).toInt().coerceIn(0, gridWidth - 1)
        val minCellY = floor((centerY - radius) / cellSize).toInt().coerceIn(0, gridHeight - 1)
        val maxCellY = floor((centerY + radius) / cellSize).toInt().coerceIn(0, gridHeight - 1)

        val radiusSq = radius * radius

        for (cy in minCellY..maxCellY) {
            val rowOffset = cy * gridWidth
            for (cx in minCellX..maxCellX) {
                val bucket = buckets[rowOffset + cx]
                for (i in bucket.indices) {
                    val enemy = bucket[i]
                    val dx = enemy.worldX - centerX
                    val dy = enemy.worldY - centerY
                    if ((dx * dx + dy * dy) <= radiusSq) {
                        queryResults.add(enemy)
                    }
                }
            }
        }
        return queryResults
    }

    /**
     * Spatial hash AoE acceleration sweep for splash munitions (Mortar, Hellfire, Cluster, Void).
     * Retrieves all living hostiles within [radius] of ([centerX], [centerY]) using O(1) cell buckets.
     */
    fun getPotentialCollisions(centerX: Float, centerY: Float, radius: Float): List<EnemyInstance> {
        return queryNearby(centerX, centerY, radius)
    }

    /**
     * Non-allocating variant of [getPotentialCollisions] that fills a reusable caller-supplied [out] list.
     */
    fun getPotentialCollisions(centerX: Float, centerY: Float, radius: Float, out: MutableList<EnemyInstance>): MutableList<EnemyInstance> {
        out.clear()
        val minCellX = floor((centerX - radius) / cellSize).toInt().coerceIn(0, gridWidth - 1)
        val maxCellX = floor((centerX + radius) / cellSize).toInt().coerceIn(0, gridWidth - 1)
        val minCellY = floor((centerY - radius) / cellSize).toInt().coerceIn(0, gridHeight - 1)
        val maxCellY = floor((centerY + radius) / cellSize).toInt().coerceIn(0, gridHeight - 1)

        val radiusSq = radius * radius

        for (cy in minCellY..maxCellY) {
            val rowOffset = cy * gridWidth
            for (cx in minCellX..maxCellX) {
                val bucket = buckets[rowOffset + cx]
                for (i in bucket.indices) {
                    val enemy = bucket[i]
                    val dx = enemy.worldX - centerX
                    val dy = enemy.worldY - centerY
                    if ((dx * dx + dy * dy) <= radiusSq) {
                        out.add(enemy)
                    }
                }
            }
        }
        return out
    }

    /**
     * Finds the closest enemy to ([centerX], [centerY]) within [maxRadius].
     */
    fun findClosest(centerX: Float, centerY: Float, maxRadius: Float): EnemyInstance? {
        val candidates = queryNearby(centerX, centerY, maxRadius)
        if (candidates.isEmpty()) return null

        var bestEnemy: EnemyInstance? = null
        var bestDistSq = Float.MAX_VALUE
        for (i in candidates.indices) {
            val enemy = candidates[i]
            val dx = enemy.worldX - centerX
            val dy = enemy.worldY - centerY
            val distSq = dx * dx + dy * dy
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestEnemy = enemy
            }
        }
        return bestEnemy
    }
}
