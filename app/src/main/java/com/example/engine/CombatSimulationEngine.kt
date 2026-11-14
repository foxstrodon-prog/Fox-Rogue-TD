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
    // Pre-sized double-buffered scratch lists for zero-allocation combat loops without concurrent aliasing
    private val scratchSurvivingEnemiesA = ArrayList<EnemyInstance>(128)
    private val scratchSurvivingEnemiesB = ArrayList<EnemyInstance>(128)
    private var useEnemyBufferA = true

    private val scratchSurvivingProjectilesA = ArrayList<Projectile>(128)
    private val scratchSurvivingProjectilesB = ArrayList<Projectile>(128)
    private var useProjBufferA = true

    private val scratchAoEList = ArrayList<EnemyInstance>(64)

    /**
     * Advances track energy particles along lane vectors with zero heap allocations.
     */
    fun advanceTrackParticles(
        particles: List<TrackParticle>,
        level: GeneratedLevel,
        deltaSec: Float
    ): List<TrackParticle> {
        if (particles.isEmpty() || !deltaSec.isFinite() || deltaSec <= 0f) return particles
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
        useEnemyBufferA = !useEnemyBufferA
        val scratchSurvivingEnemies = if (useEnemyBufferA) scratchSurvivingEnemiesA else scratchSurvivingEnemiesB
        scratchSurvivingEnemies.clear()
        var coreDamageTaken = 0

        val safeDeltaSec = if (deltaSec.isFinite() && deltaSec >= 0f) deltaSec else 0f
        val globalSlow = if (globalFrostSlowBonus > 0f) maxOf(0.15f, 1f - globalFrostSlowBonus) else 1.0f
        val pathA = level.laneAlpha
        val pathB = level.laneBeta

        for (i in currentEnemies.indices) {
            val enemy = currentEnemies[i]
            if (enemy.slowTimerMs > 0) enemy.slowTimerMs = maxOf(0L, enemy.slowTimerMs - deltaMs)
            if (enemy.shockTimerMs > 0) enemy.shockTimerMs = maxOf(0L, enemy.shockTimerMs - deltaMs)

            val path = if (enemy.laneIndex == 0) pathA else pathB
            val moveDistance = enemy.effectiveSpeed * enemySpeedMultiplier * globalSlow * safeDeltaSec
            if (moveDistance.isFinite()) {
                enemy.segmentProgress += moveDistance
            }

            while (enemy.segmentProgress >= 1f && enemy.currentPathSegmentIndex < path.size - 1) {
                enemy.segmentProgress -= 1f
                enemy.currentPathSegmentIndex++
            }

            if (enemy.currentPathSegmentIndex >= path.size - 1) {
                coreDamageTaken += if (enemy.isBoss) 6 else 1
            } else {
                val p0 = path[enemy.currentPathSegmentIndex]
                val p1 = path[enemy.currentPathSegmentIndex + 1]
                val t = if (enemy.segmentProgress.isFinite()) enemy.segmentProgress else 0f
                val wx = p0.x + (p1.x - p0.x) * t
                val wy = p0.y + (p1.y - p0.y) * t
                enemy.worldX = if (wx.isFinite()) wx else p0.x.toFloat()
                enemy.worldY = if (wy.isFinite()) wy else p0.y.toFloat()
                val dy = (p1.y - p0.y).toFloat()
                val dx = (p1.x - p0.x).toFloat()
                enemy.headingRad = if (dx != 0f || dy != 0f) atan2(dy, dx) else 0f
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
        if (projectiles.isEmpty()) return emptyList()
        if (!deltaSec.isFinite() || deltaSec <= 0f) return projectiles

        useProjBufferA = !useProjBufferA
        val scratchSurvivingProjectiles = if (useProjBufferA) scratchSurvivingProjectilesA else scratchSurvivingProjectilesB
        scratchSurvivingProjectiles.clear()

        for (i in projectiles.indices) {
            val proj = projectiles[i]
            if (!proj.currentX.isFinite() || !proj.currentY.isFinite() || !proj.speed.isFinite() || proj.speed <= 0f) {
                continue
            }

            val targetEnemy = if (proj.targetEnemyId != null) findEnemyById(survivingEnemies, proj.targetEnemyId) else null
            val destX = targetEnemy?.worldX ?: proj.targetX
            val destY = targetEnemy?.worldY ?: proj.targetY

            if (!destX.isFinite() || !destY.isFinite()) {
                continue
            }

            val dx = destX - proj.currentX
            val dy = destY - proj.currentY
            val dist = hypot(dx, dy)
            val step = proj.speed * deltaSec

            if (!dist.isFinite() || !step.isFinite()) {
                continue
            }

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
                // Task D-04 & Red-Team Flaw 5: Strict zero-distance and NaN/Infinity guards
                if (dist > 0.0001f && dist.isFinite()) {
                    val invDist = 1.0f / dist
                    val nextX = proj.currentX + (dx * invDist) * step
                    val nextY = proj.currentY + (dy * invDist) * step
                    if (nextX.isFinite() && nextY.isFinite()) {
                        proj.currentX = nextX
                        proj.currentY = nextY
                        proj.traveledDistance += step
                        scratchSurvivingProjectiles.add(proj)
                    }
                }
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
        if (!enemy.worldX.isFinite() || !enemy.worldY.isFinite()) return
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
            val safeMaxLife = it.maxLifeMs
            val ratio = if (safeMaxLife > 0L) (it.lifeMs.toFloat() / safeMaxLife.toFloat()).coerceIn(0f, 1f) else 0f
            it.currentRadius = it.maxRadius * (1f - ratio)
            it.lifeMs <= 0
        }
    }
}
