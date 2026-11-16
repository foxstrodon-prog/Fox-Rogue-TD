package com.example.engine.coordinator

import com.example.audio.SoundFx
import com.example.audio.SoundManager
import com.example.engine.TelemetryManager
import com.example.engine.TowerUpgradeManager
import com.example.model.*
import com.example.ui.GameUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

/**
 * Domain coordinator responsible for coordinate grid snapping, path clearance verification,
 * scrap transactions, turret construction, tower upgrades, booster pad swaps, and Singularity collapse (Task D-01.ii).
 */
class TowerPlacementCoordinator(
    private val uiStateFlow: MutableStateFlow<GameUIState>,
    private val soundManager: SoundManager,
    private val telemetryManager: TelemetryManager,
    private val nextId: (String) -> String,
    private val addFloatingText: (Float, Float, String, Long, Boolean) -> Unit,
    private val onSynergyDirty: () -> Unit
) {
    private var lastBoosterSwapTimeMs: Long = 0L

    fun selectTile(tile: MapTile?) {
        if (!uiStateFlow.value.hasClickedFirstTile && tile != null) {
            telemetryManager.logTutorialComplete()
        }
        if (tile != null && tile.pos == uiStateFlow.value.selectedTile?.pos) {
            uiStateFlow.update { it.copy(selectedTile = null, selectedTower = null) }
            return
        }
        val tower = uiStateFlow.value.towers.firstOrNull { it.gridPos == tile?.pos }
        val effectiveSelectedTile = if (tile != null && (tile.isBuildable || tower != null)) tile else null
        uiStateFlow.update {
            it.copy(
                selectedTile = effectiveSelectedTile,
                selectedTower = tower,
                hasClickedFirstTile = if (tile != null) true else it.hasClickedFirstTile
            )
        }
    }

    fun buildTower(tile: MapTile, towerClass: TowerClass = TowerClass.KINETIC_BALLISTIC) {
        val state = uiStateFlow.value
        val cost = (towerClass.baseCost * state.balanceConfig.buildCostMultiplier).roundToInt()

        if (!tile.isBuildable || state.scrap < cost || state.towers.any { it.gridPos == tile.pos } ||
            !TowerUpgradeManager.isClassUnlocked(towerClass, state.towers)) {
            soundManager.play(SoundFx.INVALID)
            return
        }

        val newTower = TowerInstance(
            id = nextId("tow_"),
            type = mapClassToType(towerClass),
            gridPos = tile.pos,
            towerClass = towerClass,
            tier = 0,
            investedScrap = cost
        )
        soundManager.play(SoundFx.PLACE)
        onSynergyDirty()

        uiStateFlow.update {
            it.copy(scrap = it.scrap - cost, towers = it.towers + newTower, selectedTower = newTower)
        }
        addFloatingText(tile.pos.x.toFloat(), tile.pos.y.toFloat(), "-$cost", 0xFFFFD700, false)
    }

    fun upgradeTowerToVector(tower: TowerInstance, vector: TowerVector) {
        if (tower.tier != 0 && tower.branchTier1 == null) return
        val cost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
        if (uiStateFlow.value.scrap < cost) {
            soundManager.play(SoundFx.INVALID)
            return
        }

        val prevInvested = tower.getInvestedScrapValue()
        tower.tier = 1
        tower.vector = vector
        tower.branchTier1 = when (vector) {
            TowerVector.OFFENSIVE -> "A"
            TowerVector.DEFENSIVE -> "B"
            TowerVector.SUPPORT -> "C"
        }
        tower.investedScrap = prevInvested + cost
        tower.invalidateBalanceCache()

        soundManager.playTowerUpgrade(tower.towerClass, uiStateFlow.value.turretSkinSlot)
        onSynergyDirty()
        uiStateFlow.update { it.copy(scrap = it.scrap - cost, towers = it.towers.toList(), selectedTower = tower) }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "+${vector.displayName} ${tower.towerClass.displayName}!", 0xFF00FF9D, false)
    }

    fun upgradeTowerToArchetype(tower: TowerInstance, archetype: TowerArchetype) {
        if (tower.tier != 1 && !tower.isTier1) return
        val cost = TowerUpgradeManager.getTier2Cost(archetype)
        if (uiStateFlow.value.scrap < cost) {
            soundManager.play(SoundFx.INVALID)
            return
        }

        val prevInvested = tower.getInvestedScrapValue()
        tower.tier = 2
        tower.archetype = archetype
        tower.vector = archetype.vector
        tower.towerClass = archetype.towerClass
        tower.type = mapArchetypeToType(archetype)
        tower.finalFormCode = null
        tower.investedScrap = prevInvested + cost
        tower.invalidateBalanceCache()

        soundManager.playTowerUpgrade(tower.towerClass, uiStateFlow.value.turretSkinSlot)
        onSynergyDirty()
        uiStateFlow.update {
            it.copy(scrap = it.scrap - cost, towers = it.towers.toList(), selectedTower = tower, screenShakeIntensity = 0.4f)
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "★ ${archetype.displayName}! ★", 0xFFFFB800, false)
    }

    fun upgradeTowerToTier3Global(tower: TowerInstance, auraIndex: Int = 0) {
        if (tower.tier != 2 && !tower.isTier2) return
        val cost = TowerUpgradeManager.getTier3GlobalCost(tower, uiStateFlow.value.towers)
        if (uiStateFlow.value.scrap < cost) {
            soundManager.play(SoundFx.INVALID)
            return
        }

        val prevInvested = tower.getInvestedScrapValue()
        tower.tier = 3
        tower.isTier3GlobalActive = true
        tower.selectedAuraIndex = auraIndex
        tower.investedScrap = prevInvested + cost
        tower.invalidateBalanceCache()
        val arch = tower.archetype ?: mapTypeToArchetype(tower.type)
        val chosenAura = if (auraIndex == 1) arch.secondaryAura else arch.primaryAura

        soundManager.playTowerUpgrade(tower.towerClass, uiStateFlow.value.turretSkinSlot)
        onSynergyDirty()
        uiStateFlow.update {
            it.copy(scrap = it.scrap - cost, towers = it.towers.toList(), selectedTower = tower, screenShakeIntensity = 0.8f)
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "🌟 ${chosenAura.name} ONLINE! 🌟", 0xFFFFD700, false)
    }

    fun upgradeTowerToTier1(tower: TowerInstance, branchKey: String) {
        if (!tower.isBase) return
        val cost = TowerUpgradeManager.getTier1Cost(tower, branchKey)
        if (uiStateFlow.value.scrap < cost) {
            soundManager.play(SoundFx.INVALID)
            return
        }

        val prevInvested = tower.getInvestedScrapValue()
        tower.branchTier1 = branchKey
        tower.investedScrap = prevInvested + cost
        tower.invalidateBalanceCache()
        val option = TowerUpgradeManager.getTier1Form(tower.type, branchKey)
        val name = option?.subName ?: "Branch $branchKey"

        soundManager.playTowerUpgrade(tower.towerClass, uiStateFlow.value.turretSkinSlot)
        onSynergyDirty()
        uiStateFlow.update { it.copy(scrap = it.scrap - cost, towers = it.towers.toList(), selectedTower = tower) }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "+$name!", 0xFF00FF9D, false)
    }

    fun upgradeTowerToFinalForm(tower: TowerInstance, finalFormCode: String) {
        if (!tower.isTier1) return
        val cost = TowerUpgradeManager.getFinalFormCost(tower, finalFormCode, uiStateFlow.value.towers)
        if (uiStateFlow.value.scrap < cost) {
            soundManager.play(SoundFx.INVALID)
            return
        }

        val prevInvested = tower.getInvestedScrapValue()
        tower.finalFormCode = finalFormCode
        tower.investedScrap = prevInvested + cost
        tower.invalidateBalanceCache()
        val finalFormDef = TowerUpgradeManager.getFinalForm(tower.type, finalFormCode)
        val name = finalFormDef?.subName ?: "Apex Form"

        soundManager.playTowerUpgrade(tower.towerClass, uiStateFlow.value.turretSkinSlot)
        onSynergyDirty()
        uiStateFlow.update {
            it.copy(scrap = it.scrap - cost, towers = it.towers.toList(), selectedTower = tower, screenShakeIntensity = 0.5f)
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "★ $name! ★", 0xFFFFB800, false)
    }

    fun upgradeTowerPath(tower: TowerInstance, pathIndex: Int) {
        val currentTier = tower.pathTiers[pathIndex]
        if (currentTier >= 2) return
        val nextTier = currentTier + 1
        val cost = TowerUpgradeManager.getUpgradeCost(tower, pathIndex, nextTier, uiStateFlow.value.towers)
        if (uiStateFlow.value.scrap < cost) {
            soundManager.play(SoundFx.INVALID)
            return
        }

        val prevInvested = tower.getInvestedScrapValue()
        tower.pathTiers[pathIndex] = nextTier
        tower.investedScrap = prevInvested + cost
        tower.invalidateBalanceCache()
        val upgradeName = TowerUpgradeManager.getPathUpgrade(tower.type, pathIndex, nextTier).name
        soundManager.playTowerUpgrade(tower.towerClass, uiStateFlow.value.turretSkinSlot)
        onSynergyDirty()
        uiStateFlow.update { it.copy(scrap = it.scrap - cost, towers = it.towers.toList()) }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "+$upgradeName!", 0xFF00FF9D, false)
    }

    fun ascendTower(tower: TowerInstance) {
        if (!tower.isMaxed || tower.isAscended) return
        val cost = TowerUpgradeManager.getAscensionCost(tower)
        if (uiStateFlow.value.scrap < cost) {
            soundManager.play(SoundFx.INVALID)
            return
        }

        val prevInvested = tower.getInvestedScrapValue()
        tower.finalFormCode = "${tower.branchTier1 ?: "A"}.a"
        tower.investedScrap = prevInvested + cost
        tower.invalidateBalanceCache()
        soundManager.playTowerUpgrade(tower.towerClass, uiStateFlow.value.turretSkinSlot)
        onSynergyDirty()

        uiStateFlow.update { state ->
            val updated = state.copy(scrap = state.scrap - cost, towers = state.towers.toList(), screenShakeIntensity = 0.8f)
            when (tower.type) {
                TowerType.GATLING -> updated.copy(globalKineticSpeedBonus = updated.globalKineticSpeedBonus + 0.22f)
                TowerType.CRYO -> updated.copy(
                    ascensionFrostSlowBonus = maxOf(updated.ascensionFrostSlowBonus, 0.15f),
                    globalFrostSlowBonus = maxOf(updated.globalFrostSlowBonus, 0.15f),
                    globalDamageBonus = updated.globalDamageBonus + 0.20f
                )
                TowerType.TESLA -> updated.copy(globalTeslaArcsEnabled = true)
                TowerType.MORTAR -> updated.copy(globalMortarSplashBonus = updated.globalMortarSplashBonus + 0.25f)
                TowerType.LASER -> updated.copy(globalSolarCritsEnabled = true)
                TowerType.VOID -> updated.copy(globalVoidShredEnabled = true, scrapBountyBonus = updated.scrapBountyBonus + 0.35f)
                TowerType.STELLAR -> updated.copy(globalDamageBonus = updated.globalDamageBonus + 0.20f)
            }
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "★ APEX ASCENDED! ★", 0xFFFFB800, false)
    }

    fun swapBoosterPads() {
        val now = System.currentTimeMillis()
        if (now - lastBoosterSwapTimeMs < 750L) return
        val level = uiStateFlow.value.level ?: return
        val currentTowers = uiStateFlow.value.towers.toMutableList()
        if (currentTowers.isEmpty()) return

        val padPairs = arrayOf(TileBuff.AMPLIFIER, TileBuff.HIGH_GROUND, TileBuff.CONDUIT)
        var swappedAny = false
        val newBeams = uiStateFlow.value.laserBeams.toMutableList()
        val newExplosions = uiStateFlow.value.explosions.toMutableList()

        for (buffType in padPairs) {
            val pads = level.tiles.filter { it.buff == buffType }
            if (pads.size == 2) {
                val pad1 = pads[0].pos
                val pad2 = pads[1].pos
                val tower1 = currentTowers.firstOrNull { it.gridPos == pad1 }
                val tower2 = currentTowers.firstOrNull { it.gridPos == pad2 }

                if (tower1 != null && tower2 != null) {
                    currentTowers[currentTowers.indexOf(tower1)] = tower1.copy(gridPos = pad2)
                    currentTowers[currentTowers.indexOf(tower2)] = tower2.copy(gridPos = pad1)
                    swappedAny = true
                    addFloatingText(pad1.x.toFloat(), pad1.y.toFloat(), "⇄ TELEPORT", 0xFF00F0FF, false)
                    addFloatingText(pad2.x.toFloat(), pad2.y.toFloat(), "⇄ TELEPORT", 0xFF00F0FF, false)
                } else if (tower1 != null) {
                    currentTowers[currentTowers.indexOf(tower1)] = tower1.copy(gridPos = pad2)
                    swappedAny = true
                    addFloatingText(pad2.x.toFloat(), pad2.y.toFloat(), "⇄ TELEPORT", 0xFF00F0FF, false)
                } else if (tower2 != null) {
                    currentTowers[currentTowers.indexOf(tower2)] = tower2.copy(gridPos = pad1)
                    swappedAny = true
                    addFloatingText(pad1.x.toFloat(), pad1.y.toFloat(), "⇄ TELEPORT", 0xFF00F0FF, false)
                }

                if (tower1 != null || tower2 != null) {
                    val buffColor = when (buffType) {
                        TileBuff.AMPLIFIER -> 0xFF00FF9D // Green
                        TileBuff.HIGH_GROUND -> 0xFF00F0FF // Teal
                        TileBuff.CONDUIT -> 0xFFFFB800 // Orange
                        else -> 0xFF00F0FF
                    }
                    newBeams.add(
                        LaserBeam(
                            id = nextId("teleport_beam_"),
                            startX = pad1.x.toFloat(),
                            startY = pad1.y.toFloat(),
                            endX = pad2.x.toFloat(),
                            endY = pad2.y.toFloat(),
                            color = buffColor,
                            lifeMs = 500L
                        )
                    )
                    newExplosions.add(ExplosionEffect(id = nextId("teleport_exp1_"), x = pad1.x.toFloat(), y = pad1.y.toFloat(), maxRadius = 1.2f, color = buffColor, lifeMs = 400L, maxLifeMs = 400L))
                    newExplosions.add(ExplosionEffect(id = nextId("teleport_exp2_"), x = pad2.x.toFloat(), y = pad2.y.toFloat(), maxRadius = 1.2f, color = buffColor, lifeMs = 400L, maxLifeMs = 400L))
                }
            }
        }

        if (swappedAny) {
            lastBoosterSwapTimeMs = now
            soundManager.playPlacement()
            onSynergyDirty()
            uiStateFlow.update { it.copy(towers = currentTowers, laserBeams = newBeams, explosions = newExplosions) }
        }
    }

    fun sellSelectedTower() {
        val tower = uiStateFlow.value.selectedTower ?: return
        val sellVal = tower.getSellValue()
        soundManager.play(SoundFx.SELL)
        onSynergyDirty()
        uiStateFlow.update {
            it.copy(scrap = it.scrap + sellVal, towers = it.towers.filter { t -> t.id != tower.id }, selectedTower = null, selectedTile = null)
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "+$sellVal", 0xFFFFD700, false)
    }

    fun changeSelectedTowerPriority(priority: TargetPriority) {
        val tower = uiStateFlow.value.selectedTower ?: return
        tower.targetPriority = priority
        uiStateFlow.update { it.copy(towers = it.towers.toList()) }
    }

    fun getConduitAscensionCost(): Int {
        val state = uiStateFlow.value
        val currentMaxGen = state.towers.filter { it.isCapstone }.maxOfOrNull { it.generation } ?: 0
        return 750 * (currentMaxGen + 1)
    }

    fun isSingularityGridReady(): Boolean {
        val state = uiStateFlow.value
        val level = state.level ?: return false
        val buildableTiles = level.tiles.filter { it.isBuildable }
        if (buildableTiles.isEmpty() || state.towers.size < buildableTiles.size) return false
        return state.towers.all { it.tier >= 3 || it.finalFormCode != null || it.isCapstone }
    }

    fun canInitiateSingularity(): Boolean {
        return isSingularityGridReady() && uiStateFlow.value.scrap >= getConduitAscensionCost()
    }

    fun getSingularityAnchorTower(): TowerInstance? {
        val state = uiStateFlow.value
        val selected = state.selectedTower
        if (selected != null && (selected.tier >= 3 || selected.finalFormCode != null || selected.isCapstone)) return selected
        return state.towers.firstOrNull { it.isCapstone } ?: state.towers.minByOrNull { it.id }
    }

    fun requestSingularityCollapse() {
        val state = uiStateFlow.value
        val cost = getConduitAscensionCost()
        if (!isSingularityGridReady()) {
            val buildableTiles = state.level?.tiles?.filter { it.isBuildable } ?: emptyList()
            val readyCount = state.towers.count { it.tier >= 3 || it.finalFormCode != null || it.isCapstone }
            soundManager.play(SoundFx.INVALID)
            val anchor = state.selectedTower ?: state.towers.firstOrNull()
            if (anchor != null) {
                addFloatingText(
                    anchor.gridPos.x.toFloat(), anchor.gridPos.y.toFloat(),
                    "Grid Incomplete! Every tower must be Tier 3 ($readyCount/${buildableTiles.size})!",
                    0xFFFF3B30, false
                )
            }
            return
        }
        if (state.scrap < cost) {
            soundManager.play(SoundFx.INVALID)
            val anchor = state.selectedTower ?: state.towers.firstOrNull()
            if (anchor != null) {
                addFloatingText(
                    anchor.gridPos.x.toFloat(), anchor.gridPos.y.toFloat(),
                    "Insufficient Scrap! Conduit Ascension requires ${cost}c!",
                    0xFFFF2A55, false
                )
            }
            return
        }
        uiStateFlow.update { it.copy(isSingularityWarningOpen = true) }
    }

    fun dismissSingularityWarning() {
        uiStateFlow.update { it.copy(isSingularityWarningOpen = false) }
    }

    fun confirmSingularityCollapse(computeSynergies: (List<TowerInstance>) -> List<GlobalAuraSynergy>) {
        val state = uiStateFlow.value
        if (!canInitiateSingularity()) {
            uiStateFlow.update { it.copy(isSingularityWarningOpen = false) }
            return
        }

        val anchor = getSingularityAnchorTower() ?: return
        val sacrificed = state.towers.filter { it.id != anchor.id }

        val mergedVault = anchor.synergyVault.toMutableList()
        var resolvedGen = anchor.generation

        // Inherit vault and generation from previous Capstone if anchor is a new tower
        sacrificed.filter { it.isCapstone }.forEach { prev ->
            val existingGens = mergedVault.map { it.generation }.toSet()
            prev.synergyVault.forEach { banked ->
                if (banked.generation !in existingGens) {
                    mergedVault.add(banked)
                }
            }
            resolvedGen = maxOf(resolvedGen, prev.generation)
        }

        val ascensionCost = getConduitAscensionCost()
        val scrapBounty = maxOf(250, (sacrificed.sumOf { it.getInvestedScrapValue() } * 0.20f).toInt())
        val nextGen = resolvedGen + 1
        val sacrificedLiveOnly = sacrificed.map {
            if (it.isCapstone) it.copy(isCapstone = false, synergyVault = mutableListOf()) else it
        }
        mergedVault.add(BankedSynergyVault(generation = nextGen, synergies = computeSynergies(sacrificedLiveOnly)))
        val ascendedAnchor = anchor.copy(
            isCapstone = true,
            generation = nextGen,
            synergyVault = mergedVault
        )

        val newExplosions = state.explosions.toMutableList()
        sacrificed.forEach { st ->
            newExplosions.add(ExplosionEffect(id = nextId("exp_"), x = st.gridPos.x.toFloat(), y = st.gridPos.y.toFloat(), maxRadius = 1.6f, color = 0xFFFFD700))
        }

        soundManager.play(SoundFx.UPGRADE)
        onSynergyDirty()

        uiStateFlow.update {
            it.copy(
                towers = listOf(ascendedAnchor),
                selectedTower = ascendedAnchor,
                scrap = (it.scrap - ascensionCost).coerceAtLeast(0) + scrapBounty,
                totalScrapEarned = it.totalScrapEarned + scrapBounty,
                explosions = newExplosions,
                screenShakeIntensity = 1.0f,
                isSingularityWarningOpen = false
            )
        }
        addFloatingText(ascendedAnchor.gridPos.x.toFloat(), ascendedAnchor.gridPos.y.toFloat(), "+$scrapBounty SCRAP BOUNTY!", 0xFFFFD700, false)
        addFloatingText(ascendedAnchor.gridPos.x.toFloat(), ascendedAnchor.gridPos.y.toFloat() - 0.7f, "★ GEN ${ascendedAnchor.generation} CONDUIT ONLINE ★", 0xFF00F0FF, false)
    }
}
