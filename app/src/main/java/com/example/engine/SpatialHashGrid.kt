package com.example.engine

import com.example.model.EnemyInstance
import kotlin.math.floor

/**
 * High-performance, zero-allocation Uniform Grid Spatial Partitioning for Fox-Rogue-TD.
 *
 * Replaces naive O(N*M) projectile/tower-to-enemy distance checks with an O(N) localized bucket query.
 * Eliminates shared mutable return references and supports destination-passing & in-place predicates (Tasks D-02 & D-04).
 */
class SpatialHashGrid(
    @PublishedApi internal val cellSize: Float = 2.0f,
    @PublishedApi internal val gridWidth: Int = 16,
    @PublishedApi internal val gridHeight: Int = 20
) {
    // Array of buckets, each holding an index list of enemies
    @PublishedApi internal val buckets = Array(gridWidth * gridHeight) { ArrayList<EnemyInstance>(16) }

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
     * Note: For hot loops, prefer passing a caller-supplied output buffer via the 4-arg overload.
     */
    fun queryNearby(centerX: Float, centerY: Float, radius: Float): List<EnemyInstance> {
        return queryNearby(centerX, centerY, radius, ArrayList())
    }

    /**
     * Non-allocating destination-passing variant of [queryNearby] that writes into a caller-supplied [outList].
     */
    fun queryNearby(
        centerX: Float,
        centerY: Float,
        radius: Float,
        outList: MutableList<EnemyInstance>
    ): MutableList<EnemyInstance> {
        return getPotentialCollisions(centerX, centerY, radius, outList)
    }

    /**
     * Non-allocating variant of [queryNearby] with an in-place filter [predicate] and caller-supplied [outList].
     */
    inline fun queryNearby(
        centerX: Float,
        centerY: Float,
        radius: Float,
        outList: MutableList<EnemyInstance>,
        predicate: (EnemyInstance) -> Boolean
    ): MutableList<EnemyInstance> {
        outList.clear()
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
                    if ((dx * dx + dy * dy) <= radiusSq && predicate(enemy)) {
                        outList.add(enemy)
                    }
                }
            }
        }
        return outList
    }

    /**
     * Inline zero-allocation traversal of nearby hostiles.
     */
    inline fun forEachNearby(
        centerX: Float,
        centerY: Float,
        radius: Float,
        action: (EnemyInstance) -> Unit
    ) {
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
                        action(enemy)
                    }
                }
            }
        }
    }

    /**
     * Spatial hash AoE acceleration sweep for splash munitions (Mortar, Hellfire, Cluster, Void).
     * Retrieves all living hostiles within [radius] of ([centerX], [centerY]) using O(1) cell buckets.
     */
    fun getPotentialCollisions(centerX: Float, centerY: Float, radius: Float): List<EnemyInstance> {
        return queryNearby(centerX, centerY, radius, ArrayList())
    }

    /**
     * Non-allocating variant of [getPotentialCollisions] that fills a reusable caller-supplied [out] list.
     */
    fun getPotentialCollisions(
        centerX: Float,
        centerY: Float,
        radius: Float,
        out: MutableList<EnemyInstance>
    ): MutableList<EnemyInstance> {
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
     * Finds the closest enemy to ([centerX], [centerY]) within [maxRadius] with zero heap allocations.
     */
    fun findClosest(centerX: Float, centerY: Float, maxRadius: Float): EnemyInstance? {
        var bestEnemy: EnemyInstance? = null
        var bestDistSq = Float.MAX_VALUE
        forEachNearby(centerX, centerY, maxRadius) { enemy ->
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
