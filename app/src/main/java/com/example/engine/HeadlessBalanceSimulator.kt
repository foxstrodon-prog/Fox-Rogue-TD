package com.example.engine

import com.example.model.*
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Result snapshot of a deterministic headless balance verification simulation across a full run
 * (e.g., 30 waves on Easy, 50 waves on Medium, 63 or 100 waves on Hard).
 */
data class HeadlessSimReport(
    val difficulty: GameDifficulty,
    val targetWave: Int,
    val waveReached: Int,
    val isVictory: Boolean,
    val totalCollapses: Int,
    val unspentGoldAtEnd: Int,
    val totalScrapEarned: Int,
    val uniqueAurasBanked: Int,
    val collapseWaves: List<Int>,
    val mvpTower: String,
    val coreHpRemaining: Int
)

/**
 * Deterministic Headless Balance Simulator (Track 3B / Tessa).
 * Executes a full 30/50/63/100-wave tactical bot policy in milliseconds so balance targets
 * (`Total Collapses`, `Unspent Gold at End`, `Unique Auras Banked`) can be verified without manually playing 63 rounds.
 */
object HeadlessBalanceSimulator {

    fun runAllBenchmarks(
        seed: Long = 42L,
        balanceConfig: DevBalanceConfig = DevBalanceConfig()
    ): List<HeadlessSimReport> = listOf(
        runSimulation(difficulty = GameDifficulty.EASY, targetWave = 30, seed = seed, balanceConfig = balanceConfig),
        runSimulation(difficulty = GameDifficulty.MEDIUM, targetWave = 50, seed = seed, balanceConfig = balanceConfig),
        runSimulation(difficulty = GameDifficulty.HARD, targetWave = 63, seed = seed, balanceConfig = balanceConfig)
    )

    fun runSimulation(
        difficulty: GameDifficulty = GameDifficulty.HARD,
        targetWave: Int = when (difficulty) {
            GameDifficulty.EASY -> 30
            GameDifficulty.MEDIUM -> 50
            GameDifficulty.HARD -> 63
            GameDifficulty.INFINITE -> 63
        },
        seed: Long = 42L,
        balanceConfig: DevBalanceConfig = DevBalanceConfig()
    ): HeadlessSimReport {
        var currentSeed = seed
        var level = MapGenerator.generate(
            seed = currentSeed,
            width = balanceConfig.mapColumns,
            height = balanceConfig.mapRows,
            minLaneLength = balanceConfig.minLaneLength,
            maxLaneLength = balanceConfig.maxLaneLength,
            targetOpenSlots = balanceConfig.getTargetOpenSlotsForDifficulty(difficulty),
            minLaneOverlap = balanceConfig.minLaneOverlap,
            maxLaneOverlap = balanceConfig.maxLaneOverlap
        )

        var scrap = balanceConfig.startingScrap
        var totalScrapEarned = scrap
        var coreHp = 30
        val towers = ArrayList<TowerInstance>(20)
        val collapseWaves = ArrayList<Int>(8)
        val bankedAuras = LinkedHashSet<String>()
        val damageByArchetype = LinkedHashMap<String, Double>()
        var nextTowerSeq = 1
        var waveReached = 0
        val auraModifiers = TowerUpgradeManager.GlobalAuraModifiers()

        val maxCollapseGoal = when (difficulty) {
            GameDifficulty.EASY -> 1
            GameDifficulty.MEDIUM -> 3
            GameDifficulty.HARD, GameDifficulty.INFINITE -> 5
        }

        fun rankBuildableTiles(lvl: GeneratedLevel): List<MapTile> {
            val lanePts = (lvl.laneAlpha + lvl.laneBeta)
            return lvl.tiles.filter { it.isBuildable }.sortedWith(
                compareByDescending<MapTile> { it.buff != TileBuff.NONE }
                    .thenByDescending { tile ->
                        var cov = 0
                        for (p in lanePts) {
                            if (hypot((tile.pos.x - p.x).toFloat(), (tile.pos.y - p.y).toFloat()) <= 3.0f) cov++
                        }
                        cov
                    }
            )
        }

        var rankedTiles = rankBuildableTiles(level)

        fun executeTacticalBotPurchasing(waveNum: Int) {
            var madePurchase = true
            while (madePurchase) {
                madePurchase = false

                // 1. Check Singularity Collapse Gate first if board is full & all towers are Tier 3
                val buildableCount = rankedTiles.size
                val currentMaxGen = towers.filter { it.isCapstone }.maxOfOrNull { it.generation } ?: 0
                val isGridReady = buildableCount > 0 &&
                    towers.size >= buildableCount &&
                    towers.all { it.tier >= 3 || it.isCapstone }
                val conduitCost = 750 * (currentMaxGen + 1)

                if (isGridReady && scrap >= conduitCost && currentMaxGen < maxCollapseGoal) {
                    val anchor = towers.firstOrNull { it.isCapstone }
                        ?: towers.firstOrNull { t -> rankedTiles.firstOrNull { it.pos == t.gridPos }?.buff != TileBuff.NONE }
                        ?: towers.first()
                    val sacrificed = towers.filter { it.id != anchor.id }

                    for (s in sacrificed) {
                        val name = s.archetype?.displayName ?: s.type.displayName
                        damageByArchetype[name] = (damageByArchetype[name] ?: 0.0) + s.totalDamage.toDouble()
                    }

                    val nextGen = currentMaxGen + 1
                    val sacrificedLive = sacrificed.map {
                        if (it.isCapstone) it.copy(isCapstone = false, synergyVault = mutableListOf()) else it
                    }
                    val liveSynergies = TowerUpgradeManager.computeBoardSynergies(sacrificedLive)
                    for (syn in liveSynergies) {
                        bankedAuras.add(syn.auraName)
                    }
                    val mergedVault = anchor.synergyVault.toMutableList()
                    mergedVault.add(BankedSynergyVault(generation = nextGen, synergies = liveSynergies))

                    val scrapBounty = maxOf(250, (sacrificed.sumOf { it.getInvestedScrapValue() } * 0.20f).toInt())
                    scrap = (scrap - conduitCost).coerceAtLeast(0) + scrapBounty
                    totalScrapEarned += scrapBounty
                    collapseWaves.add(waveNum)

                    val ascendedAnchor = anchor.copy(
                        isCapstone = true,
                        generation = nextGen,
                        synergyVault = mergedVault
                    )
                    towers.clear()
                    towers.add(ascendedAnchor)

                    currentSeed += (nextGen.toLong() * 104_729L) + (sacrificed.size * 7_919L) + 1_337L
                    val anchorTile = level.tiles.firstOrNull { it.pos == ascendedAnchor.gridPos }
                    val preservedSpots = setOf(ascendedAnchor.gridPos)
                    val preservedPads = if (anchorTile != null && anchorTile.buff != TileBuff.NONE) {
                        val pads = level.tiles.filter { it.buff == anchorTile.buff }
                        if (pads.size == 2) mapOf(anchorTile.buff to (pads[0].pos to pads[1].pos)) else emptyMap()
                    } else emptyMap()

                    level = MapGenerator.generate(
                        seed = currentSeed,
                        width = balanceConfig.mapColumns,
                        height = balanceConfig.mapRows,
                        minLaneLength = balanceConfig.minLaneLength,
                        maxLaneLength = balanceConfig.maxLaneLength,
                        targetOpenSlots = balanceConfig.getTargetOpenSlotsForDifficulty(difficulty),
                        minLaneOverlap = balanceConfig.minLaneOverlap,
                        maxLaneOverlap = balanceConfig.maxLaneOverlap,
                        preservedEmptySpots = preservedSpots,
                        preservedBuffPads = preservedPads
                    )
                    rankedTiles = rankBuildableTiles(level)
                    madePurchase = true
                    continue
                }

                // 2. Build base towers until every buildable pad is occupied
                val occupied = towers.map { it.gridPos }.toSet()
                val nextPad = rankedTiles.firstOrNull { it.pos !in occupied }
                if (nextPad != null) {
                    val unlocked = TowerClass.entries.filter { TowerUpgradeManager.isClassUnlocked(it, towers) }
                    val chosenClass = unlocked[(towers.size + waveNum) % unlocked.size]
                    val buildCost = (chosenClass.baseCost * balanceConfig.buildCostMultiplier).roundToInt()
                    if (scrap >= buildCost) {
                        scrap -= buildCost
                        towers.add(
                            TowerInstance(
                                id = "sim_t_${nextTowerSeq++}",
                                type = mapClassToType(chosenClass),
                                gridPos = nextPad.pos,
                                towerClass = chosenClass,
                                tier = 0,
                                investedScrap = buildCost
                            )
                        )
                        madePurchase = true
                        continue
                    }
                }

                // 3. Upgrade Tier 0 -> Tier 1 (Vector)
                val t0 = towers.firstOrNull { it.tier == 0 }
                if (t0 != null) {
                    val cost = TowerUpgradeManager.getTier1VectorCost(t0.towerClass)
                    if (scrap >= cost) {
                        scrap -= cost
                        val padBuff = rankedTiles.firstOrNull { it.pos == t0.gridPos }?.buff ?: TileBuff.NONE
                        val vec = padBuff.vectorCategory ?: TowerVector.entries[(towers.indexOf(t0)) % TowerVector.entries.size]
                        t0.tier = 1
                        t0.vector = vec
                        t0.branchTier1 = when (vec) {
                            TowerVector.OFFENSIVE -> "A"
                            TowerVector.DEFENSIVE -> "B"
                            TowerVector.SUPPORT -> "C"
                        }
                        t0.investedScrap = t0.getInvestedScrapValue() + cost
                        t0.invalidateBalanceCache()
                        madePurchase = true
                        continue
                    }
                }

                // 4. Upgrade Tier 1 -> Tier 2 (Archetype) prioritizing unbanked auras
                val t1 = towers.firstOrNull { it.tier == 1 }
                if (t1 != null) {
                    val vec = t1.vector ?: TowerVector.OFFENSIVE
                    val candidates = TowerUpgradeManager.getTier2ArchetypesForClass(t1.towerClass, vec)
                    val chosenArch = candidates.minByOrNull { arch ->
                        val (pAct, pBank) = TowerUpgradeManager.getAuraActiveAndBankedCounts(arch.primaryAura.name, towers)
                        val (sAct, sBank) = TowerUpgradeManager.getAuraActiveAndBankedCounts(arch.secondaryAura.name, towers)
                        (pAct + pBank) + (sAct + sBank)
                    } ?: candidates.first()
                    val cost = TowerUpgradeManager.getTier2Cost(chosenArch)
                    if (scrap >= cost) {
                        scrap -= cost
                        t1.tier = 2
                        t1.archetype = chosenArch
                        t1.type = mapArchetypeToType(chosenArch)
                        t1.investedScrap = t1.getInvestedScrapValue() + cost
                        t1.invalidateBalanceCache()
                        madePurchase = true
                        continue
                    }
                }

                // 5. Upgrade Tier 2 -> Tier 3 (Global Aura) prioritizing unbanked / stackable auras
                val t2 = towers.firstOrNull { it.tier == 2 }
                if (t2 != null) {
                    val cost = TowerUpgradeManager.getTier3GlobalCost(t2, towers)
                    if (scrap >= cost) {
                        val arch = t2.archetype ?: mapTypeToArchetype(t2.type)
                        val (pAct, pBank) = TowerUpgradeManager.getAuraActiveAndBankedCounts(arch.primaryAura.name, towers)
                        val (sAct, sBank) = TowerUpgradeManager.getAuraActiveAndBankedCounts(arch.secondaryAura.name, towers)
                        val auraIdx = when {
                            pAct + pBank == 0 -> 0
                            sAct + sBank == 0 -> 1
                            TowerUpgradeManager.isAuraStackable(arch, 0) -> 0
                            TowerUpgradeManager.isAuraStackable(arch, 1) -> 1
                            else -> 0
                        }
                        scrap -= cost
                        t2.tier = 3
                        t2.isTier3GlobalActive = true
                        t2.selectedAuraIndex = auraIdx
                        t2.investedScrap = t2.getInvestedScrapValue() + cost
                        t2.invalidateBalanceCache()
                        madePurchase = true
                        continue
                    }
                }
            }
        }

        // Initial Wave 1 deployment build
        executeTacticalBotPurchasing(1)

        for (waveNum in 1..targetWave) {
            val waveData = WaveGenerator.generateWave(
                waveNumber = waveNum,
                enemyCountMultiplier = balanceConfig.enemiesPerRoundMultiplier,
                difficulty = difficulty
            )

            TowerUpgradeManager.accumulateTier3AuraModifiers(towers, auraModifiers)

            // Compute total effective board DPS and enemy wave EHP
            var boardDps = 0f
            var harmonicCount = 0
            var conduitCount = 0
            for (t in towers) {
                val buff = rankedTiles.firstOrNull { it.pos == t.gridPos }?.buff ?: TileBuff.NONE
                if (buff != TileBuff.NONE) harmonicCount++
                if (buff == TileBuff.CONDUIT) conduitCount++
            }
            val harmonicDmgBonus = if (harmonicCount >= 6) 0.20f else 0f

            for (t in towers) {
                val buff = rankedTiles.firstOrNull { it.pos == t.gridPos }?.buff ?: TileBuff.NONE
                val synBonus = if (t.vector != null && t.vector == buff.vectorCategory) 0.15f else 0f
                val dmg = t.getEffectiveDamage(balanceConfig, buff, synBonus + harmonicDmgBonus + auraModifiers.damageBonus)
                val rate = t.getEffectiveFireRate(balanceConfig, buff, synBonus + auraModifiers.fireRateBonus)
                val critFactor = 1f + (0.08f + auraModifiers.critRateBonus) * (1.5f + auraModifiers.critMultiplierBonus)
                val aoeMultiplier = when {
                    t.archetype?.isSplash == true || t.type.isSplash -> 1.85f
                    (t.archetype?.chains ?: t.type.chains) > 0 -> 1.65f
                    else -> 1.0f
                }
                val towerDps = dmg * rate * critFactor * aoeMultiplier
                boardDps += towerDps
            }

            // Capstone tower and banked vault synergies amplify effective board focus-fire and lane control
            val currentMaxGen = towers.filter { it.isCapstone }.maxOfOrNull { it.generation } ?: 0
            val genSynergyAmplifier = 1f + currentMaxGen * 0.45f
            val slowFactor = (1f + auraModifiers.slowBonus.coerceIn(0f, 0.75f))
            val avgLaneDurationSec = 20.0f * slowFactor * genSynergyAmplifier
            val totalWaveDamageCapacity = boardDps * avgLaneDurationSec

            var totalWaveEnemyHp = 0f
            for (entry in waveData.spawnList) {
                val unitBal = balanceConfig.getEnemyBalance(entry.type)
                val rawHp = unitBal.hp * entry.hpScale * balanceConfig.enemyHpMultiplier
                val effectiveArmor = (unitBal.armor * (1f - auraModifiers.armorIgnore.coerceIn(0f, 1f))).coerceAtLeast(0f)
                val armorFactor = 1f + effectiveArmor * 0.012f
                totalWaveEnemyHp += rawHp * armorFactor
            }

            // Distribute combat damage & kills across active towers
            val dealtThisWave = minOf(totalWaveEnemyHp, totalWaveDamageCapacity)
            if (towers.isNotEmpty() && boardDps > 0f) {
                val sharePerTower = dealtThisWave / towers.size.toFloat()
                val killsPerTower = (waveData.spawnList.size / towers.size).coerceAtLeast(1)
                for (t in towers) {
                    t.totalDamage += sharePerTower
                    t.kills += killsPerTower
                }
            }

            // Check if any enemies breach the Core when wave HP significantly exceeds board capacity
            if (totalWaveEnemyHp > totalWaveDamageCapacity * 2.15f) {
                val breachRatio = ((totalWaveEnemyHp - totalWaveDamageCapacity * 2.15f) / totalWaveEnemyHp.coerceAtLeast(1f))
                val coreDmg = (breachRatio * 10f).roundToInt().coerceAtLeast(1)
                coreHp = (coreHp - coreDmg).coerceAtLeast(0)
            }

            // Calculate wave kill scrap with exact SimulationCoordinator waveDensityDecay
            val auraScrapBonus = auraModifiers.scrapBonus.coerceIn(0f, 0.75f)
            val conduitBonus = minOf(0.20f, conduitCount * 0.10f)
            val padBonus = minOf(0.12f, harmonicCount * 0.02f)
            val harmonicBonus = if (harmonicCount >= 6) 0.15f else 0f
            val totalAdditiveIncomeBonus = 1.0f + minOf(0.90f, auraScrapBonus + conduitBonus + padBonus + harmonicBonus)
            val waveDensityDecay = when {
                waveNum <= 12 -> 1.0f
                waveNum <= 30 -> (1.0f / (1.0f + (waveNum - 12) * 0.022f)).coerceAtLeast(0.68f)
                else -> (0.78f / (1.0f + (waveNum - 30) * 0.015f)).coerceAtLeast(0.50f)
            }
            val diffIncomeMul = difficulty.scrapIncomeMultiplier

            var waveKillScrap = 0
            for (entry in waveData.spawnList) {
                val bounty = (
                    entry.type.scrapValue *
                        diffIncomeMul *
                        totalAdditiveIncomeBonus *
                        waveDensityDecay *
                        balanceConfig.globalIncomeMultiplier
                    ).roundToInt().coerceAtLeast(1)
                waveKillScrap += bounty
            }
            val waveCompletionScrap = (waveData.completionBonusScrap * balanceConfig.waveBonusMultiplier).roundToInt()
            val totalWaveScrap = waveKillScrap + waveCompletionScrap

            scrap += totalWaveScrap
            totalScrapEarned += totalWaveScrap
            waveReached = waveNum

            if (coreHp <= 0) {
                break
            }

            // Re-invest between waves (and mid-wave collapse opportunities)
            executeTacticalBotPurchasing(waveNum)
        }

        for (t in towers) {
            val name = t.archetype?.displayName ?: t.type.displayName
            damageByArchetype[name] = (damageByArchetype[name] ?: 0.0) + t.totalDamage.toDouble()
            if (t.isCapstone) {
                for (vault in t.synergyVault) {
                    for (syn in vault.synergies) {
                        bankedAuras.add(syn.auraName)
                    }
                }
            }
        }

        val mvpTower = damageByArchetype.maxByOrNull { it.value }?.key ?: "Vulcan Gatling"
        val totalCollapses = maxOf(
            collapseWaves.size,
            towers.filter { it.isCapstone }.maxOfOrNull { it.generation } ?: 0
        )

        return HeadlessSimReport(
            difficulty = difficulty,
            targetWave = targetWave,
            waveReached = waveReached,
            isVictory = coreHp > 0 && waveReached >= targetWave,
            totalCollapses = totalCollapses,
            unspentGoldAtEnd = scrap,
            totalScrapEarned = totalScrapEarned,
            uniqueAurasBanked = bankedAuras.size,
            collapseWaves = collapseWaves.toList(),
            mvpTower = mvpTower,
            coreHpRemaining = coreHp
        )
    }
}
