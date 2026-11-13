package com.example.engine.coordinator

import com.example.audio.SoundManager
import com.example.engine.CombatSimulationEngine
import com.example.engine.DamageCalculator
import com.example.engine.RemoteConfigManager
import com.example.engine.TowerUpgradeManager
import com.example.model.*
import com.example.ui.CinematicController
import com.example.ui.GameUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.*
import kotlin.random.Random

/**
 * Domain coordinator for virtual time, simulation ticking, track particle stepping,
 * status decay, turret targeting/firing, and sound tick dispatch with zero-allocation hot loops (Task D-01.i & D-02).
 */
class SimulationCoordinator(
    private val uiStateFlow: MutableStateFlow<GameUIState>,
    val combatEngine: CombatSimulationEngine,
    private val waveCoordinator: WaveProgressionCoordinator,
    private val soundManager: SoundManager,
    private val remoteConfigManager: RemoteConfigManager,
    private val cinematicController: CinematicController,
    private val nextId: (String) -> String,
    private val addFloatingText: (Float, Float, String, Long, Boolean) -> Unit,
    private val onRecordRunCompletion: (Boolean, Int) -> Unit
) {
    var virtualSimTimeMs: Long = 0L
        private set

    private val scratchEnemiesInput = ArrayList<EnemyInstance>(128)
    private val scratchRemainingAliveA = ArrayList<EnemyInstance>(128)
    private val scratchRemainingAliveB = ArrayList<EnemyInstance>(128)
    private var useBufferA = true

    private val scratchProjectilesInput = ArrayList<Projectile>(128)
    private val scratchBeamsA = ArrayList<LaserBeam>(64)
    private val scratchBeamsB = ArrayList<LaserBeam>(64)
    private val scratchArcsA = ArrayList<LightningArc>(32)
    private val scratchArcsB = ArrayList<LightningArc>(32)
    private val scratchExplosionsA = ArrayList<ExplosionEffect>(64)
    private val scratchExplosionsB = ArrayList<ExplosionEffect>(64)
    private val scratchFloatingTextsA = ArrayList<FloatingText>(64)
    private val scratchFloatingTextsB = ArrayList<FloatingText>(64)

    private val scratchInRangeEnemies = ArrayList<EnemyInstance>(64)
    private val scratchChainTargets = ArrayList<EnemyInstance>(16)
    private val scratchChainRemaining = ArrayList<EnemyInstance>(64)
    private val scratchDamageResult = DamageCalculator.DamageResult()

    var isSynergyCacheDirty: Boolean = true
    private var cachedSynergies: List<GlobalAuraSynergy> = emptyList()

    fun resetVirtualTime() {
        virtualSimTimeMs = 0L; isSynergyCacheDirty = true; cachedSynergies = emptyList()
    }

    fun tickSimulation(deltaSec: Float, deltaMs: Long, nowMs: Long = System.currentTimeMillis()) {
        virtualSimTimeMs += deltaMs
        soundManager.updateTick(nowMs, deltaMs)
        val state = uiStateFlow.value
        val level = state.level ?: return
        if (state.activeCinematic != null && !cinematicController.processFrame(state.activeCinematic, state)) return

        val newOvercharge = maxOf(0L, state.overchargeRemainingMs - deltaMs)
        scratchEnemiesInput.clear()
        val prevEnemies = state.enemies
        for (i in prevEnemies.indices) scratchEnemiesInput.add(prevEnemies[i])

        val updatedParticles = combatEngine.advanceTrackParticles(state.trackParticles, level, deltaSec)
        waveCoordinator.tickWaveSpawning(state, level, deltaMs, scratchEnemiesInput)

        val enemyAdvance = combatEngine.advanceEnemies(
            currentEnemies = scratchEnemiesInput, level = level, deltaSec = deltaSec, deltaMs = deltaMs,
            globalFrostSlowBonus = state.globalFrostSlowBonus, enemySpeedMultiplier = state.balanceConfig.enemySpeedMultiplier
        )
        val survivingEnemies = enemyAdvance.survivingEnemies
        val coreDamageTaken = enemyAdvance.coreDamageTaken

        var newCoreHp = state.coreHp
        var gameOver = state.isGameOver
        var shake = maxOf(0f, state.screenShakeIntensity - deltaSec * 3f)

        if (coreDamageTaken > 0) {
            newCoreHp = maxOf(0, state.coreHp - coreDamageTaken); shake = 1.0f
            val corePos = findCorePos(level)
            addFloatingText(corePos.x.toFloat(), corePos.y.toFloat(), "-$coreDamageTaken BREACH!", 0xFFFF2A55, false)
            if (newCoreHp <= 0) { gameOver = true; onRecordRunCompletion(false, state.waveNumber - 1) }
        }

        val towers = state.towers
        var globalFireRateBonus = state.globalKineticSpeedBonus
        var globalRangeBonus = 0f
        var globalDamageBonus = (state.globalDamageBonus - 1.0f)
        var globalSlowBonus = state.globalFrostSlowBonus
        var globalCritRate = if (state.globalSolarCritsEnabled) 0.28f else 0.08f
        var globalCritMul = 2.5f
        var globalArmorIgnore = if (state.globalVoidShredEnabled) 1.0f else 0f
        var globalScrapBonus = state.scrapBountyBonus

        for (i in towers.indices) {
            val t = towers[i]
            val ffCode = t.finalFormCode
            if (t.isFinalForm && ffCode != null) {
                val ff = TowerUpgradeManager.getFinalForm(t.type, ffCode) ?: continue
                val n = ff.subName
                when (ff.code) {
                    "A.a" -> {
                        if (n.contains("Hyper-Gatling")) globalFireRateBonus += 0.18f
                        if (n.contains("Absolute Zero")) globalSlowBonus = maxOf(globalSlowBonus, 0.15f)
                        if (n.contains("Black Hole")) globalSlowBonus = maxOf(globalSlowBonus, 0.25f)
                    }
                    "A.c" -> if (n.contains("Carpet Bomber")) globalFireRateBonus += 0.20f
                    "B.a" -> {
                        if (n.contains("Sabot")) globalArmorIgnore = maxOf(globalArmorIgnore, 0.25f)
                        if (n.contains("Gigawatt")) globalDamageBonus += 0.25f
                        if (n.contains("Sub-Space")) globalArmorIgnore = 1.0f
                    }
                    "B.b" -> {
                        if (n.contains("Aurora")) globalCritMul += 0.50f
                        if (n.contains("Kaleidoscope")) globalRangeBonus += 0.20f
                    }
                    "B.c" -> if (n.contains("Hailstorm")) globalCritRate += 0.10f
                    "C.a" -> {
                        if (n.contains("Sniper")) globalRangeBonus += 0.25f
                        if (n.contains("Blizzard")) globalSlowBonus = maxOf(globalSlowBonus, 0.25f)
                        if (n.contains("Solar Flare")) { globalCritRate += 0.20f; globalCritMul = maxOf(globalCritMul, 2.5f) }
                        if (n.contains("Orbital Kinetic")) globalRangeBonus += 0.35f
                    }
                    "C.b" -> if (n.contains("Tachyon Flash")) globalFireRateBonus += 0.25f
                    "C.c" -> {
                        if (n.contains("Cryo Bounty")) globalScrapBonus += 0.30f
                        if (n.contains("Dynamo")) globalScrapBonus += 0.35f
                        if (n.contains("Void Scrap")) globalScrapBonus += 0.45f
                    }
                }
            }
        }

        useBufferA = !useBufferA
        val newBeams = if (useBufferA) scratchBeamsA else scratchBeamsB
        val newArcs = if (useBufferA) scratchArcsA else scratchArcsB
        val newExplosions = if (useBufferA) scratchExplosionsA else scratchExplosionsB
        val remainingAlive = if (useBufferA) scratchRemainingAliveA else scratchRemainingAliveB
        val updatedFloatingTexts = if (useBufferA) scratchFloatingTextsA else scratchFloatingTextsB

        scratchProjectilesInput.clear(); for (i in state.projectiles.indices) scratchProjectilesInput.add(state.projectiles[i])
        newBeams.clear(); for (i in state.laserBeams.indices) newBeams.add(state.laserBeams[i])
        newArcs.clear(); for (i in state.lightningArcs.indices) newArcs.add(state.lightningArcs[i])
        newExplosions.clear(); for (i in state.explosions.indices) newExplosions.add(state.explosions[i])

        var boostedTilesExist = false
        var harmonicCount = 0
        var conduitTilesCount = 0
        val tiles = level.tiles
        for (i in tiles.indices) if (tiles[i].buff != TileBuff.NONE) { boostedTilesExist = true; break }
        for (i in towers.indices) {
            val buff = findTileAt(tiles, towers[i].gridPos)?.buff ?: TileBuff.NONE
            if (buff != TileBuff.NONE) harmonicCount++
            if (buff == TileBuff.CONDUIT) conduitTilesCount++
        }

        val isHarmonicActive = harmonicCount >= 6 && boostedTilesExist
        val harmonicDmgBonus = if (isHarmonicActive) remoteConfigManager.harmonicGridDamageBoost else 0.0f
        val harmonicIncomeMul = if (isHarmonicActive) 1.25f else 1.0f
        var frameDamageDealt = 0f

        for (tIdx in towers.indices) {
            val tower = towers[tIdx]
            val tileBuff = findTileAt(tiles, tower.gridPos)?.buff ?: TileBuff.NONE
            val p0 = if (tower.branchTier1 == "A" || tower.vector == TowerVector.OFFENSIVE) (if (tower.isFinalForm) 2 else 1) else 0
            val p1 = if (tower.branchTier1 == "B" || tower.vector == TowerVector.DEFENSIVE) (if (tower.isFinalForm) 2 else 1) else 0
            val p2 = if (tower.branchTier1 == "C" || tower.vector == TowerVector.SUPPORT) (if (tower.isFinalForm) 2 else 1) else 0

            val t1Def = if (tower.branchTier1 != null) TowerUpgradeManager.getTier1Options(tower.type).firstOrNull { it.branchKey == tower.branchTier1 } else null
            val ffDef = if (tower.finalFormCode != null) TowerUpgradeManager.getFinalForm(tower.type, tower.finalFormCode!!) else null

            val tierSpdMultiplier = maxOf(ffDef?.fireRateMultiplier ?: t1Def?.fireRateMultiplier ?: 1.0f, when (tower.tier) { 3 -> 1.70f; 2 -> 1.40f; 1 -> 1.20f; else -> 1.0f })
            val tierDmgMultiplier = maxOf(ffDef?.damageMultiplier ?: t1Def?.damageMultiplier ?: 1.0f, when (tower.tier) { 3 -> 1.60f; 2 -> 1.35f; 1 -> 1.15f; else -> 1.0f })

            val synergyBonus = if (tower.vector != null && tower.vector == tileBuff.vectorCategory) 0.15f else 0.0f
            val customTConfig = state.balanceConfig.getEffectiveTowerBalance(tower.type, tower.branchTier1, tower.finalFormCode)
            val effectiveRange = tower.getEffectiveCombatRange(tileBuff, globalRangeBonus, customTConfig.range)

            val tileSpdBonus = if (tileBuff == TileBuff.CONDUIT) 0.35f else 0.0f
            val overchargeSpd = if (newOvercharge > 0) 1.0f else 0.0f
            val fireRate = customTConfig.fireRate * state.balanceConfig.globalTowerFireRateMultiplier * tierSpdMultiplier * (1.0f + tileSpdBonus + synergyBonus + globalFireRateBonus + overchargeSpd)
            val cooldownMs = maxOf(50L, if (fireRate > 0f) (1000f / fireRate).toLong() else 1000L)

            val towerX = tower.gridPos.x.toFloat()
            val towerY = tower.gridPos.y.toFloat()

            combatEngine.spatialGrid.queryNearby(towerX, towerY, effectiveRange + 0.6f, scratchInRangeEnemies) { enemy ->
                hypot(enemy.worldX - towerX, enemy.worldY - towerY) <= (effectiveRange + enemy.type.hitRadius)
            }
            if (scratchInRangeEnemies.isEmpty()) { tower.targetEnemyId = null; continue }

            val target = selectPriorityTarget(scratchInRangeEnemies, tower.targetPriority, towerX, towerY)
            tower.targetEnemyId = target.id
            tower.rotationAngleDeg = (atan2(target.worldY - towerY, target.worldX - towerX) * (180f / PI.toFloat()))

            if (virtualSimTimeMs - tower.lastShotTimeMs >= cooldownMs) {
                tower.lastShotTimeMs = virtualSimTimeMs
                val sfxSlot = when (tower.towerClass) {
                    TowerClass.KINETIC_BALLISTIC -> state.kineticSfxSlot; TowerClass.ELEMENTAL_VOID -> state.elementalSfxSlot
                    TowerClass.HEAVY_SIEGE -> state.siegeSfxSlot; TowerClass.STELLAR_COSMIC -> 1
                }
                soundManager.playTurretFire(tower.towerClass, sfxSlot)

                val tileDmgBonus = if (tileBuff == TileBuff.AMPLIFIER) 0.30f else 0.0f
                val apexBonus = if (tower.isFinalForm) 0.30f else 0.0f
                val classDmgMul = when (tower.towerClass) {
                    TowerClass.KINETIC_BALLISTIC -> state.balanceConfig.kineticDamageMultiplier
                    TowerClass.ELEMENTAL_VOID -> state.balanceConfig.elementalDamageMultiplier
                    TowerClass.HEAVY_SIEGE -> state.balanceConfig.heavyDamageMultiplier
                    TowerClass.STELLAR_COSMIC -> 1.15f
                }
                val baseDmg = customTConfig.damage * tierDmgMultiplier * classDmgMul * (1.0f + tileDmgBonus + synergyBonus + harmonicDmgBonus + globalDamageBonus + apexBonus)
                val isCrit = Random.nextFloat() < globalCritRate
                val finalDmg = if (isCrit) baseDmg * globalCritMul else baseDmg

                frameDamageDealt += fireTowerWeapon(
                    tower, target, towerX, towerY, finalDmg, isCrit, p0, p1, p2,
                    survivingEnemies, scratchProjectilesInput, newBeams, newArcs, newExplosions
                )
            }
        }

        val survivingProjectiles = combatEngine.updateProjectiles(
            projectiles = scratchProjectilesInput, survivingEnemies = survivingEnemies, deltaSec = deltaSec,
            applyDamage = { target, dmg, tower, crit, pierce, shape, affinity ->
                frameDamageDealt += applyDamageToEnemy(target, dmg, tower, crit, pierce, shape, affinity)
            },
            addExplosion = { x, y, radius, color, lifeMs ->
                newExplosions.add(ExplosionEffect(nextId("exp_"), x, y, radius, color = color, lifeMs = lifeMs))
            }
        )

        var killsGained = 0
        var scrapGained = 0
        remainingAlive.clear()
        val conduitIncomeBonus = 1.0f + (conduitTilesCount * 0.15f)
        val padEconomyBonus = 1.0f + (harmonicCount * 0.03f)

        for (i in survivingEnemies.indices) {
            val enemy = survivingEnemies[i]
            if (enemy.isDead) {
                killsGained++
                val bounty = (enemy.type.scrapValue * globalScrapBonus * state.balanceConfig.globalIncomeMultiplier * remoteConfigManager.scrapBountyMultiplier * conduitIncomeBonus * harmonicIncomeMul * padEconomyBonus).roundToInt().coerceAtLeast(1)
                scrapGained += bounty
                addFloatingText(enemy.worldX, enemy.worldY, "+$bounty", 0xFFFFD700, false)

                if (enemy.slowTimerMs > 0 && state.globalFrostSlowBonus > 0f) {
                    combatEngine.processCryoShatterburst(
                        enemy = enemy,
                        addExplosion = { x, y, radius, color, lifeMs ->
                            newExplosions.add(ExplosionEffect(nextId("exp_"), x, y, radius, color = color, lifeMs = lifeMs))
                        },
                        applyDamage = { target, dmg, tower, crit, pierce, shape, affinity ->
                            frameDamageDealt += applyDamageToEnemy(target, dmg, tower, crit, pierce, shape, affinity)
                        }
                    )
                }
            } else remainingAlive.add(enemy)
        }

        combatEngine.updateVisualEffects(newBeams, newArcs, newExplosions, deltaMs)

        updatedFloatingTexts.clear()
        val currentFts = uiStateFlow.value.floatingTexts
        for (i in currentFts.indices) {
            val ft = currentFts[i]
            val remLife = ft.lifeMs - deltaMs
            if (remLife > 0) { ft.lifeMs = remLife; ft.y -= (deltaSec * 0.7f); updatedFloatingTexts.add(ft) }
        }

        val waveEval = waveCoordinator.evaluateWaveCompletion(state, level, towers, remainingAlive, newCoreHp)
        scrapGained += waveEval.bonusScrap
        newCoreHp = waveEval.updatedCoreHp

        val synergies = if (isSynergyCacheDirty) {
            computeActiveSynergies(towers).also { cachedSynergies = it; isSynergyCacheDirty = false }
        } else cachedSynergies

        uiStateFlow.update { prev ->
            prev.copy(
                enemies = remainingAlive, projectiles = survivingProjectiles,
                laserBeams = newBeams, lightningArcs = newArcs, explosions = newExplosions,
                floatingTexts = updatedFloatingTexts, trackParticles = updatedParticles,
                activeGlobalSynergies = synergies, harmonicGridActive = isHarmonicActive,
                harmonicPadsCount = harmonicCount, globalRangeBonus = globalRangeBonus,
                animTimeMs = nowMs, coreHp = newCoreHp,
                scrap = prev.scrap + scrapGained, totalScrapEarned = prev.totalScrapEarned + scrapGained,
                totalKills = prev.totalKills + killsGained, totalDamageDealt = prev.totalDamageDealt + frameDamageDealt,
                isGameOver = gameOver, isVictory = waveEval.isVictory,
                isWaveActive = waveEval.isWaveActive, waveNumber = waveEval.waveNumber,
                screenShakeIntensity = shake, overchargeRemainingMs = newOvercharge,
                bestWaveRecord = maxOf(prev.bestWaveRecord, waveEval.waveNumber)
            )
        }

        if (!waveEval.isWaveActive && state.autoCallNextWave && !gameOver && !waveEval.isVictory) {
            waveCoordinator.deployWave()
        }
    }

    private fun fireTowerWeapon(
        tower: TowerInstance, target: EnemyInstance, towerX: Float, towerY: Float,
        finalDmg: Float, isCrit: Boolean, p0: Int, p1: Int, p2: Int,
        survivingEnemies: List<EnemyInstance>, newProjectiles: MutableList<Projectile>,
        newBeams: MutableList<LaserBeam>, newArcs: MutableList<LightningArc>, newExplosions: MutableList<ExplosionEffect>
    ): Float {
        var dealt = 0f
        when (tower.type) {
            TowerType.GATLING -> {
                newProjectiles.add(Projectile(id = nextId("p_"), startX = towerX, startY = towerY, currentX = towerX, currentY = towerY, targetEnemyId = target.id, targetX = target.worldX, targetY = target.worldY, speed = 8.5f, damage = finalDmg, color = tower.type.color, isCrit = isCrit, hasClusterSplit = (p2 >= 2), sourceShape = tower.shape, sourceAffinity = tower.affinity))
                if (p0 >= 2) newProjectiles.add(Projectile(id = nextId("p_"), startX = towerX + 0.1f, startY = towerY + 0.1f, currentX = towerX + 0.1f, currentY = towerY + 0.1f, targetEnemyId = target.id, targetX = target.worldX, targetY = target.worldY, speed = 8.5f, damage = finalDmg * 0.75f, color = 0xFFFFFFFF, isCrit = isCrit, sourceShape = tower.shape, sourceAffinity = tower.affinity))
            }
            TowerType.CRYO -> {
                newBeams.add(LaserBeam(nextId("beam_"), towerX, towerY, target.worldX, target.worldY, tower.type.color))
                val rawSlow = if (p0 >= 2) 0.85f else if (p0 >= 1) 0.65f else 0.50f
                target.slowTimerMs = if (p0 >= 2) 2200L else 1400L; target.slowFactor = maxOf(0.15f, 1f - rawSlow)
                dealt += applyDamageToEnemy(target, finalDmg, tower, isCrit, false)
            }
            TowerType.TESLA -> {
                val maxChains = 3 + (if (p0 == 1) 2 else if (p0 >= 2) 5 else 0)
                scratchChainTargets.clear(); scratchChainTargets.add(target); scratchChainRemaining.clear()
                for (i in survivingEnemies.indices) if (survivingEnemies[i].id != target.id) scratchChainRemaining.add(survivingEnemies[i])
                while (scratchChainTargets.size < maxChains && scratchChainRemaining.isNotEmpty()) {
                    val last = scratchChainTargets[scratchChainTargets.size - 1]
                    var bestNext: EnemyInstance? = null; var bestDist = Float.MAX_VALUE
                    for (i in scratchChainRemaining.indices) {
                        val cand = scratchChainRemaining[i]
                        val d = hypot(cand.worldX - last.worldX, cand.worldY - last.worldY)
                        if (d < bestDist) { bestDist = d; bestNext = cand }
                    }
                    if (bestNext != null && bestDist < 3.4f) { scratchChainTargets.add(bestNext); scratchChainRemaining.remove(bestNext) } else break
                }
                val arcPoints = ArrayList<Pair<Float, Float>>(scratchChainTargets.size + 1)
                arcPoints.add(towerX to towerY)
                for (i in scratchChainTargets.indices) {
                    val ct = scratchChainTargets[i]
                    arcPoints.add(ct.worldX to ct.worldY); ct.shockTimerMs = 2500L
                    dealt += applyDamageToEnemy(ct, finalDmg, tower, isCrit, false)
                }
                newArcs.add(LightningArc(nextId("arc_"), arcPoints, tower.type.color))
            }
            TowerType.MORTAR -> {
                val dist = hypot(target.worldX - towerX, target.worldY - towerY)
                val splashRad = tower.type.splashRadius * (if (p1 >= 2) 1.6f else 1.0f)
                newProjectiles.add(Projectile(id = nextId("p_"), startX = towerX, startY = towerY, currentX = towerX, currentY = towerY, targetEnemyId = null, targetX = target.worldX, targetY = target.worldY, speed = 4.2f, damage = finalDmg, color = tower.type.color, isSplash = true, splashRadius = splashRad, isMortarArc = true, totalDistance = maxOf(0.1f, dist), isCrit = isCrit, hasClusterSplit = (p0 >= 1), sourceShape = tower.shape, sourceAffinity = tower.affinity))
            }
            TowerType.LASER -> {
                newBeams.add(LaserBeam(nextId("beam_"), towerX, towerY, target.worldX, target.worldY, tower.type.color))
                if (p1 >= 2 && scratchInRangeEnemies.size >= 2) {
                    for (i in scratchInRangeEnemies.indices) {
                        val cand = scratchInRangeEnemies[i]
                        if (cand.id != target.id) {
                            newBeams.add(LaserBeam(nextId("beam_"), towerX, towerY, cand.worldX, cand.worldY, 0xFFFF7A00))
                            dealt += applyDamageToEnemy(cand, finalDmg * 0.75f, tower, isCrit, true); break
                        }
                    }
                }
                dealt += applyDamageToEnemy(target, finalDmg, tower, isCrit, true)
            }
            TowerType.VOID -> {
                newExplosions.add(ExplosionEffect(nextId("exp_"), towerX, towerY, tower.type.splashRadius, color = tower.type.color))
                for (i in scratchInRangeEnemies.indices) {
                    val enemy = scratchInRangeEnemies[i]
                    dealt += applyDamageToEnemy(enemy, finalDmg, tower, isCrit, true)
                    if (p0 >= 1) enemy.segmentProgress = maxOf(0f, enemy.segmentProgress - (if (p0 >= 2) 0.16f else 0.08f))
                }
            }
            TowerType.STELLAR -> {
                newProjectiles.add(Projectile(id = nextId("p_"), startX = towerX, startY = towerY, currentX = towerX, currentY = towerY, targetEnemyId = target.id, targetX = target.worldX, targetY = target.worldY, speed = 6.0f, damage = finalDmg, isCrit = isCrit, color = 0xFFFFD700, sourceShape = EnemyShape.STAR, sourceAffinity = ElementAffinity.YELLOW))
            }
        }
        return dealt
    }

    private fun selectPriorityTarget(enemies: List<EnemyInstance>, priority: TargetPriority, towerX: Float, towerY: Float): EnemyInstance {
        var best = enemies[0]
        for (i in 1 until enemies.size) {
            val cand = enemies[i]
            val isBetter = when (priority) {
                TargetPriority.FIRST -> (cand.currentPathSegmentIndex + cand.segmentProgress) > (best.currentPathSegmentIndex + best.segmentProgress)
                TargetPriority.CLOSEST -> hypot(cand.worldX - towerX, cand.worldY - towerY) < hypot(best.worldX - towerX, best.worldY - towerY)
                TargetPriority.STRONGEST -> cand.currentHp > best.currentHp
                TargetPriority.WEAKEST -> cand.currentHp < best.currentHp
            }
            if (isBetter) best = cand
        }
        return best
    }

    private fun findTileAt(tiles: List<MapTile>, pos: GridPos): MapTile? {
        for (i in tiles.indices) if (tiles[i].pos == pos) return tiles[i]
        return null
    }

    private fun findCorePos(level: GeneratedLevel): GridPos {
        val tiles = level.tiles
        for (i in tiles.indices) if (tiles[i].type == TileType.CORE) return tiles[i].pos
        return GridPos(level.width / 2, level.height - 1)
    }

    private fun applyDamageToEnemy(
        enemy: EnemyInstance, rawDamage: Float, sourceTower: TowerInstance?,
        isCrit: Boolean, armorPierce: Boolean, sourceShape: EnemyShape? = null, sourceAffinity: ElementAffinity? = null
    ): Float {
        val state = uiStateFlow.value
        val armor = if (armorPierce || state.globalVoidShredEnabled) 0f else enemy.type.baseArmor
        val combatResult = DamageCalculator.calculateDamage(
            baseDamage = rawDamage,
            towerShape = sourceTower?.shape ?: sourceShape ?: EnemyShape.CIRCLE,
            towerAffinity = sourceTower?.affinity ?: sourceAffinity ?: ElementAffinity.RED,
            enemyShape = enemy.type.shape, enemyAffinity = enemy.type.elementAffinity,
            enemyArmor = armor, out = scratchDamageResult
        )
        val finalDamage = combatResult.finalDamage
        enemy.currentHp -= finalDamage
        sourceTower?.let { it.totalDamage += finalDamage; if (enemy.isDead) it.kills++ }

        if (isCrit || (combatResult.isShapeMatch && combatResult.isColorAdvantage) || combatResult.shapeMultiplier < 1.0f || finalDamage > 60f) {
            val tag = when {
                isCrit -> "${finalDamage.toInt()} CRIT!"
                combatResult.isShapeMatch && combatResult.isColorAdvantage -> "${finalDamage.toInt()} WEAK!"
                combatResult.shapeMultiplier < 1.0f -> "${finalDamage.toInt()} RESIST"
                else -> "${finalDamage.toInt()}"
            }
            val textColor = when {
                isCrit -> 0xFFFFB800
                combatResult.isShapeMatch && combatResult.isColorAdvantage -> 0xFF00FF9D
                combatResult.shapeMultiplier < 1.0f -> 0xFF94A3B8
                else -> 0xFFFFFFFF
            }
            addFloatingText(enemy.worldX, enemy.worldY - 0.2f, tag, textColor, isCrit)
        }
        return finalDamage
    }

    fun computeActiveSynergies(towers: List<TowerInstance>): List<GlobalAuraSynergy> =
        TowerUpgradeManager.computeActiveSynergies(towers)
}
