package com.example.engine

import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

data class EnemyAdvanceResult(
    val survivingEnemies: List<EnemyInstance>,
    val coreDamageTaken: Int
)

/**
 * Pure combat physics, ballistics math, enemy path movement, and AoE spatial query acceleration engine.
 * Decoupled from ViewModel state orchestration for modularity and testability (Task C-01, C-03, D-02 & D-04).
 */
class CombatSimulationEngine(
    val spatialGrid: SpatialHashGrid = SpatialHashGrid(cellSize = 2.0f, gridWidth = 16, gridHeight = 20)
) {
    // Pre-sized scratch lists for zero-allocation combat loops (Task C-06 & D-02)
    private val scratchSurvivingEnemies = ArrayList<EnemyInstance>(128)
    private val scratchSurvivingProjectiles = ArrayList<Projectile>(128)
    private val scratchAoEList = ArrayList<EnemyInstance>(64)

    /**
     * Advances track energy particles along lane vectors with zero heap allocations.
     */
    fun advanceTrackParticles(
        particles: List<TrackParticle>,
        level: GeneratedLevel,
        deltaSec: Float
    ): List<TrackParticle> {
        if (particles.isEmpty()) return emptyList()
        val pathA = level.laneAlpha
        val pathB = level.laneBeta
        for (i in particles.indices) {
            val p = particles[i]
            val path = if (p.laneIndex == 0) pathA else pathB
            var seg = p.segmentIndex
            var prog = p.progress + p.speed * deltaSec
            val maxSeg = path.size - 2
            while (prog >= 1.0f && seg < maxSeg) {
                prog -= 1.0f
                seg++
            }
            if (seg >= maxSeg && prog >= 1.0f) {
                seg = 0
                prog = 0f
            }
            p.segmentIndex = seg
            p.progress = prog
        }
        return particles
    }

    /**
     * Advances enemy movement, ticks status timers, and calculates path progress and core damage.
     */
    fun advanceEnemies(
        currentEnemies: List<EnemyInstance>,
        level: GeneratedLevel,
        deltaSec: Float,
        deltaMs: Long,
        globalFrostSlowBonus: Float,
        enemySpeedMultiplier: Float
    ): EnemyAdvanceResult {
        scratchSurvivingEnemies.clear()
        var coreDamageTaken = 0

        val globalSlow = if (globalFrostSlowBonus > 0f) maxOf(0.15f, 1f - globalFrostSlowBonus) else 1.0f
        val pathA = level.laneAlpha
        val pathB = level.laneBeta

        for (i in currentEnemies.indices) {
            val enemy = currentEnemies[i]
            if (enemy.slowTimerMs > 0) enemy.slowTimerMs = maxOf(0L, enemy.slowTimerMs - deltaMs)
            if (enemy.shockTimerMs > 0) enemy.shockTimerMs = maxOf(0L, enemy.shockTimerMs - deltaMs)

            val path = if (enemy.laneIndex == 0) pathA else pathB
            val moveDistance = enemy.effectiveSpeed * enemySpeedMultiplier * globalSlow * deltaSec
            enemy.segmentProgress += moveDistance

            while (enemy.segmentProgress >= 1f && enemy.currentPathSegmentIndex < path.size - 1) {
                enemy.segmentProgress -= 1f
                enemy.currentPathSegmentIndex++
            }

            if (enemy.currentPathSegmentIndex >= path.size - 1) {
                coreDamageTaken += if (enemy.isBoss) 6 else 1
            } else {
                val p0 = path[enemy.currentPathSegmentIndex]
                val p1 = path[enemy.currentPathSegmentIndex + 1]
                val t = enemy.segmentProgress
                enemy.worldX = p0.x + (p1.x - p0.x) * t
                enemy.worldY = p0.y + (p1.y - p0.y) * t
                enemy.headingRad = atan2((p1.y - p0.y).toFloat(), (p1.x - p0.x).toFloat())
                scratchSurvivingEnemies.add(enemy)
            }
        }

        spatialGrid.rebuild(scratchSurvivingEnemies)
        return EnemyAdvanceResult(
            survivingEnemies = scratchSurvivingEnemies,
            coreDamageTaken = coreDamageTaken
        )
    }

    private fun findEnemyById(enemies: List<EnemyInstance>, id: String): EnemyInstance? {
        for (i in enemies.indices) {
            val e = enemies[i]
            if (e.id == id) return e
        }
        return null
    }

    /**
     * Updates projectile physics, applies target tracking, resolves collisions, and accelerates
     * AoE munitions (Mortar, Hellfire, Cluster, Ricochet) through [SpatialHashGrid.getPotentialCollisions].
     */
    fun updateProjectiles(
        projectiles: List<Projectile>,
        survivingEnemies: List<EnemyInstance>,
        deltaSec: Float,
        applyDamage: (target: EnemyInstance, rawDamage: Float, sourceTower: TowerInstance?, isCrit: Boolean, armorPierce: Boolean, sourceShape: EnemyShape?, sourceAffinity: ElementAffinity?) -> Unit,
        addExplosion: (x: Float, y: Float, radius: Float, color: Long, lifeMs: Long) -> Unit
    ): List<Projectile> {
        scratchSurvivingProjectiles.clear()
        if (projectiles.isEmpty()) return emptyList()

        for (i in projectiles.indices) {
            val proj = projectiles[i]
            val targetEnemy = if (proj.targetEnemyId != null) findEnemyById(survivingEnemies, proj.targetEnemyId) else null
            val destX = targetEnemy?.worldX ?: proj.targetX
            val destY = targetEnemy?.worldY ?: proj.targetY

            val dx = destX - proj.currentX
            val dy = destY - proj.currentY
            val dist = hypot(dx, dy)
            val step = proj.speed * deltaSec

            if (dist <= step || dist < 0.25f) {
                if (proj.isSplash) {
                    addExplosion(destX, destY, proj.splashRadius, proj.color, 250L)
                    // AoE accelerated via SpatialHashGrid
                    spatialGrid.getPotentialCollisions(destX, destY, proj.splashRadius, scratchAoEList)
                    for (j in scratchAoEList.indices) {
                        val st = scratchAoEList[j]
                        applyDamage(st, proj.damage, null, proj.isCrit, false, proj.sourceShape, proj.sourceAffinity)
                    }

                    // Mortar Cluster Munitions
                    if (proj.hasClusterSplit) {
                        for (c in 0 until 3) {
                            val angle = (c * 2.094f) + Random.nextFloat()
                            val offsetX = cos(angle) * 0.85f
                            val offsetY = sin(angle) * 0.85f
                            val clusterX = destX + offsetX
                            val clusterY = destY + offsetY
                            addExplosion(clusterX, clusterY, 0.9f, 0xFFFFB800, 200L)
                            spatialGrid.getPotentialCollisions(clusterX, clusterY, 0.9f, scratchAoEList)
                            for (j in scratchAoEList.indices) {
                                val ct = scratchAoEList[j]
                                applyDamage(ct, proj.damage * 0.45f, null, false, false, proj.sourceShape, proj.sourceAffinity)
                            }
                        }
                    }
                } else {
                    val hitEnemy = if (proj.targetEnemyId != null) findEnemyById(survivingEnemies, proj.targetEnemyId) else null
                    if (hitEnemy != null) {
                        applyDamage(hitEnemy, proj.damage, null, proj.isCrit, false, proj.sourceShape, proj.sourceAffinity)
                        // Gatling Ricochet
                        if (proj.hasClusterSplit) {
                            spatialGrid.getPotentialCollisions(hitEnemy.worldX, hitEnemy.worldY, 2.5f, scratchAoEList)
                            var bounceTarget: EnemyInstance? = null
                            for (j in scratchAoEList.indices) {
                                val candidate = scratchAoEList[j]
                                if (candidate.id != hitEnemy.id) {
                                    bounceTarget = candidate
                                    break
                                }
                            }
                            if (bounceTarget != null) {
                                applyDamage(bounceTarget, proj.damage * 0.50f, null, false, false, proj.sourceShape, proj.sourceAffinity)
                            }
                        }
                    }
                }
            } else {
                // Task D-04: Strict zero-distance and NaN guards
                @Suppress("KotlinConstantConditions")
                if (!dist.isNaN() && dist > 0.0001f) {
                    val invDist = 1.0f / dist
                    proj.currentX += (dx * invDist) * step
                    proj.currentY += (dy * invDist) * step
                }
                proj.traveledDistance += step
                scratchSurvivingProjectiles.add(proj)
            }
        }

        return scratchSurvivingProjectiles
    }

    /**
     * Resolves Cryo Shatterburst AoE using [SpatialHashGrid.getPotentialCollisions].
     */
    fun processCryoShatterburst(
        enemy: EnemyInstance,
        addExplosion: (x: Float, y: Float, radius: Float, color: Long, lifeMs: Long) -> Unit,
        applyDamage: (target: EnemyInstance, rawDamage: Float, sourceTower: TowerInstance?, isCrit: Boolean, armorPierce: Boolean, sourceShape: EnemyShape?, sourceAffinity: ElementAffinity?) -> Unit
    ) {
        addExplosion(enemy.worldX, enemy.worldY, 1.4f, 0xFF38BDF8, 250L)
        spatialGrid.getPotentialCollisions(enemy.worldX, enemy.worldY, 1.4f, scratchAoEList)
        for (i in scratchAoEList.indices) {
            val st = scratchAoEList[i]
            if (st.id != enemy.id) {
                applyDamage(st, enemy.maxHp * 0.25f, null, false, true, null, null)
            }
        }
    }

    /**
     * Updates visual effect timers and decays for laser beams, lightning arcs, and explosions.
     */
    fun updateVisualEffects(
        beams: MutableList<LaserBeam>,
        arcs: MutableList<LightningArc>,
        explosions: MutableList<ExplosionEffect>,
        deltaMs: Long
    ) {
        beams.removeAll {
            it.lifeMs -= deltaMs
            it.lifeMs <= 0
        }
        arcs.removeAll {
            it.lifeMs -= deltaMs
            it.lifeMs <= 0
        }
        explosions.removeAll {
            it.lifeMs -= deltaMs
            it.currentRadius = it.maxRadius * (1f - (it.lifeMs.toFloat() / it.maxLifeMs))
            it.lifeMs <= 0
        }
    }
}
