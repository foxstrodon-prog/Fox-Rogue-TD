package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.engine.CombatSimulationEngine
import com.example.engine.MapGenerator
import com.example.engine.RemoteConfigManager
import com.example.engine.TelemetryManager
import com.example.engine.TowerUpgradeManager
import com.example.engine.WaveGenerator
import com.example.engine.coordinator.SimulationCoordinator
import com.example.engine.coordinator.TowerPlacementCoordinator
import com.example.engine.coordinator.WaveProgressionCoordinator
import com.example.model.*
import com.example.ui.CinematicController
import com.example.ui.CinematicHost
import com.example.ui.GameUIState
import com.example.ui.hud.FloatingSquircleUpgradeBar
import com.example.ui.hud.MinimalTopHud
import com.example.ui.menu.DevBalanceSettingsDialog
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h900dp")
class RedTeamRemediation026Test {

    @get:Rule
    val composeRule = createComposeRule()

    private fun buildSimpleLevel(): GeneratedLevel {
        val lane = listOf(GridPos(0, 0), GridPos(1, 0), GridPos(2, 0), GridPos(3, 0))
        val tiles = mutableListOf(
            MapTile(GridPos(0, 0), TileType.SPAWNER_ALPHA),
            MapTile(GridPos(1, 0), TileType.PATH_ALPHA),
            MapTile(GridPos(2, 0), TileType.PATH_ALPHA),
            MapTile(GridPos(3, 0), TileType.CORE),
            MapTile(GridPos(1, 1), TileType.EMPTY)
        )
        return GeneratedLevel(
            seed = 999L,
            width = 4,
            height = 2,
            biome = MapGenerator.generate(1L).biome,
            tiles = tiles,
            laneAlpha = lane,
            laneBeta = lane,
            intersectionPoints = emptySet()
        )
    }

    private fun buildTestCoordinator(
        initialState: GameUIState,
        onRecordRunCompletion: (Boolean, Int) -> Unit = { _, _ -> }
    ): Pair<MutableStateFlow<GameUIState>, SimulationCoordinator> {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val stateFlow = MutableStateFlow(initialState)
        var idCounter = 0L
        val nextId: (String) -> String = { prefix -> "$prefix${idCounter++}" }
        val remoteConfig = RemoteConfigManager(null)
        val telemetry = TelemetryManager(app)
        val soundManager = SoundManager(app)
        val waveCoordinator = WaveProgressionCoordinator(
            uiStateFlow = stateFlow,
            remoteConfigManager = remoteConfig,
            telemetryManager = telemetry,
            nextId = nextId,
            addFloatingText = { _, _, _, _, _ -> },
            onRecordRunCompletion = onRecordRunCompletion
        )
        val host = object : CinematicHost {
            override val uiStateFlow: MutableStateFlow<GameUIState> = stateFlow
            override val soundManager: SoundManager = soundManager
            override fun clearPendingSpawns() = waveCoordinator.clearPendingSpawns()
            override fun markSynergyDirty() {}
            override fun addFloatingText(x: Float, y: Float, text: String, color: Long, isCrit: Boolean) {}
            override fun swapBoosterPads() {}
            override fun deployWave() = waveCoordinator.deployWave()
        }
        val coord = SimulationCoordinator(
            uiStateFlow = stateFlow,
            combatEngine = CombatSimulationEngine(),
            waveCoordinator = waveCoordinator,
            soundManager = soundManager,
            remoteConfigManager = remoteConfig,
            cinematicController = CinematicController(host),
            nextId = nextId,
            onRecordRunCompletion = onRecordRunCompletion,
            critRandom = kotlin.random.Random(42L)
        )
        return stateFlow to coord
    }

    @Test
    fun `projectile towers GATLING MORTAR and STELLAR credit totalDamage and kills to sourceTower`() {
        val level = buildSimpleLevel()
        for (type in listOf(TowerType.GATLING, TowerType.MORTAR, TowerType.STELLAR)) {
            val tower = TowerInstance(
                id = "tower_${type.name}",
                type = type,
                gridPos = GridPos(1, 1),
                lastShotTimeMs = -10_000L
            )
            val enemy = EnemyInstance(
                id = "enemy_${type.name}",
                type = EnemyType.DRONE,
                laneIndex = 0,
                currentPathSegmentIndex = 1,
                segmentProgress = 0.0f,
                maxHp = 10f,
                currentHp = 10f,
                speed = 0.01f,
                worldX = 1.0f,
                worldY = 0.0f
            )
            val (stateFlow, coord) = buildTestCoordinator(
                GameUIState(
                    level = level,
                    isWaveActive = true,
                    towers = listOf(tower),
                    enemies = listOf(enemy),
                    scrap = 100
                )
            )
            for (step in 1..25) {
                coord.tickSimulation(0.05f, 50L, nowMs = step * 50L)
            }
            assertTrue("${type.name} should accumulate totalDamage > 0", tower.totalDamage > 0f)
            assertEquals("${type.name} should be credited with exactly 1 kill", 1, tower.kills)
            assertTrue("Wave state enemies should be cleared", stateFlow.value.enemies.isEmpty())
        }
    }

    @Test
    fun `globalSlowBonus and globalArmorIgnore from Tier-3 archetypes apply on the same tick`() {
        val level = buildSimpleLevel()

        val baseEnemy = EnemyInstance(
            id = "e_base",
            type = EnemyType.SOLDIER,
            laneIndex = 0,
            currentPathSegmentIndex = 0,
            segmentProgress = 0f,
            maxHp = 1000f,
            currentHp = 1000f,
            speed = 2.0f,
            worldX = 0f,
            worldY = 0f
        )
        val (_, baseCoord) = buildTestCoordinator(
            GameUIState(level = level, isWaveActive = true, enemies = listOf(baseEnemy))
        )
        baseCoord.tickSimulation(0.1f, 100L, nowMs = 100L)
        val baselineProgress = baseEnemy.segmentProgress

        // Tier-3 FROST_SPIRE (primaryAura = 18% global slow) + DARK_MATTER_FORGE (primaryAura = 40% armor ignore)
        val frostSpire = TowerInstance(
            id = "t_frost",
            type = TowerType.CRYO,
            gridPos = GridPos(1, 1),
            towerClass = TowerClass.ELEMENTAL_VOID,
            tier = 3,
            archetype = TowerArchetype.FROST_SPIRE,
            selectedAuraIndex = 0,
            lastShotTimeMs = 10_000L // keep far from target / on cooldown so direct shot slow doesn't confound aura test
        )
        val darkMatter = TowerInstance(
            id = "t_dm",
            type = TowerType.VOID,
            gridPos = GridPos(1, 1),
            towerClass = TowerClass.ELEMENTAL_VOID,
            tier = 3,
            archetype = TowerArchetype.DARK_MATTER_FORGE,
            selectedAuraIndex = 0,
            lastShotTimeMs = 10_000L
        )
        val slowedEnemy = EnemyInstance(
            id = "e_slowed",
            type = EnemyType.SOLDIER,
            laneIndex = 0,
            currentPathSegmentIndex = 0,
            segmentProgress = 0f,
            maxHp = 1000f,
            currentHp = 1000f,
            speed = 2.0f,
            worldX = 0f,
            worldY = 0f
        )
        val (slowFlow, slowCoord) = buildTestCoordinator(
            GameUIState(
                level = level,
                isWaveActive = true,
                towers = listOf(frostSpire, darkMatter),
                enemies = listOf(slowedEnemy)
            )
        )
        slowCoord.tickSimulation(0.1f, 100L, nowMs = 100L)

        assertTrue(
            "globalFrostSlowBonus should reflect FROST_SPIRE aura on the same tick",
            slowFlow.value.globalFrostSlowBonus >= 0.18f
        )
        assertTrue(
            "Slowed progress (${slowedEnemy.segmentProgress}) must be strictly less than baseline ($baselineProgress) on the first tick",
            slowedEnemy.segmentProgress < baselineProgress
        )

        // Verify globalArmorIgnore affects damage via a Gatling tower firing against armored BRUISER (baseArmor = 0.35f)
        val gatling = TowerInstance(
            id = "t_gat",
            type = TowerType.GATLING,
            gridPos = GridPos(1, 1),
            lastShotTimeMs = -10_000L
        )
        val armoredNoShred = EnemyInstance(
            id = "e_arm1",
            type = EnemyType.BRUISER,
            laneIndex = 0,
            currentPathSegmentIndex = 1,
            segmentProgress = 0f,
            maxHp = 5000f,
            currentHp = 5000f,
            speed = 0.01f,
            worldX = 1.0f,
            worldY = 0.0f
        )
        val armoredWithShred = armoredNoShred.copy(id = "e_arm2")

        val (_, coordNoShred) = buildTestCoordinator(
            GameUIState(
                level = level,
                isWaveActive = true,
                towers = listOf(gatling.copy(id = "g1")),
                enemies = listOf(armoredNoShred)
            )
        )
        val (_, coordWithShred) = buildTestCoordinator(
            GameUIState(
                level = level,
                isWaveActive = true,
                towers = listOf(gatling.copy(id = "g2"), darkMatter.copy(id = "t_dm2", lastShotTimeMs = 100_000L)),
                enemies = listOf(armoredWithShred)
            )
        )
        for (step in 1..20) {
            coordNoShred.tickSimulation(0.05f, 50L, nowMs = step * 50L)
            coordWithShred.tickSimulation(0.05f, 50L, nowMs = step * 50L)
        }
        val dmgNoShred = 5000f - armoredNoShred.currentHp
        val dmgWithShred = 5000f - armoredWithShred.currentHp
        assertTrue(
            "Damage with 40% armor shred ($dmgWithShred) must exceed damage without shred ($dmgNoShred)",
            dmgWithShred > dmgNoShred
        )
    }

    @Test
    fun `TowerUpgradeManager accumulates all 36 archetype auras and banked synergyVault entries`() {
        val out = TowerUpgradeManager.GlobalAuraModifiers()
        for (arch in TowerArchetype.entries) {
            for (auraIdx in 0..1) {
                val t = TowerInstance(
                    id = "${arch.name}_$auraIdx",
                    type = mapArchetypeToType(arch),
                    gridPos = GridPos(0, 0),
                    tier = 3,
                    archetype = arch,
                    selectedAuraIndex = auraIdx
                )
                TowerUpgradeManager.accumulateTier3AuraModifiers(listOf(t), out)
                val sum = out.fireRateBonus + out.damageBonus + out.rangeBonus +
                    out.slowBonus + out.critRateBonus + out.critMultiplierBonus +
                    out.armorIgnore + out.scrapBonus
                assertTrue("Archetype ${arch.name} aura #$auraIdx must map to a non-zero global modifier", sum > 0f)
            }
        }

        val bankedSynergy = GlobalAuraSynergy(
            auraName = TowerArchetype.FROST_SPIRE.primaryAura.name,
            effectDescription = TowerArchetype.FROST_SPIRE.primaryAura.description,
            sourceCount = 1,
            iconEmoji = "✨",
            color = TowerArchetype.FROST_SPIRE.color
        )
        val vault = BankedSynergyVault(
            generation = 1,
            synergies = listOf(bankedSynergy)
        )
        val capstone = TowerInstance(
            id = "cap_1",
            type = TowerType.MORTAR,
            gridPos = GridPos(0, 0),
            tier = 3,
            archetype = TowerArchetype.TITAN_RAILGUN,
            selectedAuraIndex = 0,
            isCapstone = true,
            synergyVault = mutableListOf(vault)
        )
        TowerUpgradeManager.accumulateTier3AuraModifiers(listOf(capstone), out)
        assertTrue("Banked FROST_SPIRE must contribute slowBonus", out.slowBonus >= 0.18f)
        assertTrue("Active TITAN_RAILGUN must contribute damage or armorIgnore", out.damageBonus > 0f || out.armorIgnore > 0f)
    }

    @Test
    fun `DevBalanceConfig 52-tower overrides affect TowerInstance isGameplayModified and exportDiffJson`() {
        val customDamage = 123f
        val config = DevBalanceConfig(
            customTowerConfigs = mapOf(
                TowerArchetype.HELLFIRE_MORTAR.name to TowerUnitBalance(
                    customName = "Doom Mortar",
                    damage = customDamage,
                    range = 4.2f,
                    fireRate = 1.5f,
                    cost = 200
                ),
                "STELLAR" to TowerUnitBalance(
                    damage = 42f,
                    range = 3.8f,
                    fireRate = 2.2f,
                    cost = 180
                )
            )
        )
        assertTrue("Custom 52-tower overrides must flag isGameplayModified()", config.isGameplayModified())

        val mortarTower = TowerInstance(
            id = "m1",
            type = TowerType.MORTAR,
            gridPos = GridPos(0, 0),
            towerClass = TowerClass.HEAVY_SIEGE,
            tier = 2,
            archetype = TowerArchetype.HELLFIRE_MORTAR
        )
        // Tier 2 Heavy Siege gets classDmgMul = heavyDamageMultiplier (1.0f) and t3Mul = 1.0f
        assertEquals(customDamage, mortarTower.getEffectiveDamage(config), 0.01f)
        assertEquals(1.5f, mortarTower.getEffectiveFireRate(config), 0.01f)

        val stellarTower = TowerInstance(
            id = "s1",
            type = TowerType.STELLAR,
            gridPos = GridPos(0, 0),
            towerClass = TowerClass.STELLAR_COSMIC,
            tier = 0
        )
        // STELLAR_COSMIC applies 1.15f class multiplier
        assertEquals(42f * 1.15f, stellarTower.getEffectiveDamage(config), 0.01f)

        val json = config.exportDiffJson()
        assertTrue("exportDiffJson must include HELLFIRE_MORTAR diff", json.contains(TowerArchetype.HELLFIRE_MORTAR.name))
        assertTrue("exportDiffJson must include STELLAR diff", json.contains("\"STELLAR\""))
    }

    @Test
    fun `core breach records run completion after committing final state and never double-fires`() {
        val level = buildSimpleLevel()
        val breachingEnemy = EnemyInstance(
            id = "e_breach",
            type = EnemyType.DRONE,
            laneIndex = 0,
            currentPathSegmentIndex = 2,
            segmentProgress = 0.99f,
            maxHp = 50f,
            currentHp = 50f,
            speed = 5.0f,
            worldX = 2.99f,
            worldY = 0f
        )

        var recordedCalls = 0
        var observedIsGameOverDuringCallback = false
        var observedWaveRecorded = -1
        lateinit var stateFlowRef: MutableStateFlow<GameUIState>

        val (stateFlow, coord) = buildTestCoordinator(
            GameUIState(
                level = level,
                isWaveActive = true,
                waveNumber = 4,
                coreHp = 1,
                enemies = listOf(breachingEnemy)
            ),
            onRecordRunCompletion = { victory, wavesCleared ->
                recordedCalls++
                observedIsGameOverDuringCallback = stateFlowRef.value.isGameOver
                observedWaveRecorded = wavesCleared
                assertFalse(victory)
            }
        )
        stateFlowRef = stateFlow

        coord.tickSimulation(0.1f, 100L, nowMs = 100L)
        assertEquals(1, recordedCalls)
        assertTrue("uiStateFlow.isGameOver must be true BEFORE onRecordRunCompletion runs", observedIsGameOverDuringCallback)
        assertEquals(3, observedWaveRecorded)

        coord.tickSimulation(0.1f, 100L, nowMs = 200L)
        assertEquals("onRecordRunCompletion must not fire again when already gameOver", 1, recordedCalls)
    }

    @Test
    fun `MinimalTopHud wires pause button and does not double-count absorbed synergies`() {
        var pauseClicks = 0
        val synergy = GlobalAuraSynergy(
            auraName = "Apex Synergy",
            effectDescription = "+10% All Damage",
            sourceCount = 1,
            iconEmoji = "★",
            color = 0xFFFFB800
        )
        val capstone = TowerInstance(
            id = "cap",
            type = TowerType.GATLING,
            gridPos = GridPos(0, 0),
            tier = 3,
            archetype = TowerArchetype.VULCAN_GATLING,
            isCapstone = true,
            generation = 1,
            synergyVault = mutableListOf(BankedSynergyVault(1, listOf(synergy)))
        )
        val uiState = GameUIState(
            waveNumber = 5,
            coreHp = 20,
            scrap = 300,
            towers = listOf(capstone),
            activeGlobalSynergies = listOf(synergy)
        )

        composeRule.setContent {
            MinimalTopHud(
                state = uiState,
                onDeployWave = {},
                onToggleAutoWave = {},
                onOpenPauseMenu = { pauseClicks++ }
            )
        }

        composeRule.onNodeWithTag("hud_pause_button").performClick()
        assertEquals(1, pauseClicks)
        composeRule.onNodeWithText("(1)").assertIsDisplayed()
    }

    @Test
    fun `FloatingSquircleUpgradeBar invokes single build callback without double-firing legacy callback`() {
        val emptyTile = MapTile(GridPos(1, 1), TileType.EMPTY)
        var classBuildCount = 0
        var legacyBuildCount = 0

        val buildState = GameUIState(
            scrap = 500,
            selectedTile = emptyTile
        )
        composeRule.setContent {
            FloatingSquircleUpgradeBar(
                state = buildState,
                onBuildTowerClass = { _, _ -> classBuildCount++ },
                onBuildTower = { _, _ -> legacyBuildCount++ }
            )
        }
        composeRule.onNodeWithTag("build_basic_turret").performClick()
        assertEquals(1, classBuildCount)
        assertEquals("Legacy onBuildTower must not fire when onBuildTowerClass is provided", 0, legacyBuildCount)
    }

    @Test
    fun `FloatingSquircleUpgradeBar legacy fallback works when onBuildTowerClass is null`() {
        val emptyTile = MapTile(GridPos(1, 1), TileType.EMPTY)
        var legacyBuildCount = 0

        composeRule.setContent {
            FloatingSquircleUpgradeBar(
                state = GameUIState(scrap = 500, selectedTile = emptyTile),
                onBuildTower = { _, _ -> legacyBuildCount++ }
            )
        }
        composeRule.onNodeWithTag("build_basic_turret").performClick()
        assertEquals("Legacy onBuildTower fallback must fire when onBuildTowerClass is null", 1, legacyBuildCount)
    }

    @Test
    fun `FloatingSquircleUpgradeBar Tier-2 tower shows 2 real AuraOption cards even with fallback archetype`() {
        val tier2NullArch = TowerInstance(
            id = "t2_legacy",
            type = TowerType.GATLING,
            gridPos = GridPos(1, 1),
            tier = 2,
            archetype = null
        )
        val clickedAuraIndices = mutableListOf<Int>()

        composeRule.setContent {
            FloatingSquircleUpgradeBar(
                state = GameUIState(scrap = 1000, selectedTower = tier2NullArch),
                onUpgradeToTier3Global = { _, idx -> clickedAuraIndices.add(idx) }
            )
        }
        composeRule.onNodeWithTag("upgrade_tier3_0").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("upgrade_tier3_1").assertIsDisplayed().performClick()
        assertEquals(listOf(0, 1), clickedAuraIndices)
    }

    @Test
    fun `DevBalanceSettingsDialog renders 52 TOWERS tab and preserves PROBES state across tab switch`() {
        composeRule.setContent {
            DevBalanceSettingsDialog(
                config = DevBalanceConfig(),
                towerPerformance = emptyList(),
                onConfigChanged = {},
                onResetDefaults = {},
                onResetTelemetry = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("PROBES").performClick()
        composeRule.onNodeWithText("Spawn Count: 5 units").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Scuttle Drone").performScrollTo().performClick()
        composeRule.onNodeWithText("52 TOWERS").performClick()
        composeRule.onNodeWithText("GRANULAR 52-TOWER STAT ADJUSTMENT").assertIsDisplayed()
        composeRule.onNodeWithText("PROBES").performClick()
        composeRule.onNodeWithText("Spawn Count: 5 units").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `globalFrostSlowBonus clears after FROST_SPIRE is removed while ascensionFrostSlowBonus persists`() {
        val level = buildSimpleLevel()
        val frostSpire = TowerInstance(
            id = "t_frost",
            type = TowerType.CRYO,
            gridPos = GridPos(1, 1),
            towerClass = TowerClass.ELEMENTAL_VOID,
            tier = 3,
            archetype = TowerArchetype.FROST_SPIRE,
            selectedAuraIndex = 0
        )
        val (flow, coord) = buildTestCoordinator(
            GameUIState(
                level = level,
                isWaveActive = true,
                towers = listOf(frostSpire),
                enemies = emptyList()
            )
        )
        coord.tickSimulation(0.05f, 50L, nowMs = 50L)
        assertTrue(flow.value.globalFrostSlowBonus >= 0.18f)

        // Remove FROST_SPIRE: slow must drop back to 0f
        flow.value = flow.value.copy(towers = emptyList())
        coord.tickSimulation(0.05f, 50L, nowMs = 100L)
        assertEquals(0f, flow.value.globalFrostSlowBonus, 0.0001f)

        // With permanent Cryo ascension slow, floor persists even without towers
        flow.value = flow.value.copy(ascensionFrostSlowBonus = 0.15f)
        coord.tickSimulation(0.05f, 50L, nowMs = 150L)
        assertEquals(0.15f, flow.value.globalFrostSlowBonus, 0.0001f)
    }

    @Test
    fun `confirmSingularityCollapse does not double-count anchor or compound vaults across generations`() {
        val level = buildSimpleLevel()
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val emptyTiles = level.tiles.filter { it.isBuildable }
        val anchor = TowerInstance(
            id = "a1",
            type = TowerType.GATLING,
            gridPos = emptyTiles[0].pos,
            towerClass = TowerClass.KINETIC_BALLISTIC,
            tier = 3,
            archetype = TowerArchetype.VULCAN_GATLING,
            selectedAuraIndex = 0
        )
        // Add an extra Tier-3 tower on a synthetic second buildable tile so collapse is legal
        val twoTileLevel = level.copy(
            tiles = level.tiles + MapTile(GridPos(2, 1), TileType.EMPTY)
        )
        val sacrificed = TowerInstance(
            id = "s1",
            type = TowerType.CRYO,
            gridPos = GridPos(2, 1),
            towerClass = TowerClass.ELEMENTAL_VOID,
            tier = 3,
            archetype = TowerArchetype.FROST_SPIRE,
            selectedAuraIndex = 0
        )
        val stateFlow = MutableStateFlow(
            GameUIState(
                level = twoTileLevel,
                scrap = 5000,
                towers = listOf(anchor, sacrificed),
                selectedTower = anchor
            )
        )
        var nextIdVal = 0
        val placement = TowerPlacementCoordinator(
            uiStateFlow = stateFlow,
            soundManager = SoundManager(app),
            telemetryManager = TelemetryManager(app),
            nextId = { "$it${nextIdVal++}" },
            addFloatingText = { _, _, _, _, _ -> },
            onSynergyDirty = {}
        )

        // Gen 1 Collapse
        placement.confirmSingularityCollapse(TowerUpgradeManager::computeActiveSynergies)
        assertTrue("Original anchor instance must not be mutated in place", anchor.synergyVault.isEmpty())
        val gen1Anchor = stateFlow.value.towers.single()
        assertEquals(1, gen1Anchor.generation)
        val gen1Synergies = TowerUpgradeManager.computeActiveSynergies(stateFlow.value.towers)
        val vulcanGen1 = gen1Synergies.first { it.auraName == TowerArchetype.VULCAN_GATLING.primaryAura.name }
        val frostGen1 = gen1Synergies.first { it.auraName == TowerArchetype.FROST_SPIRE.primaryAura.name }
        assertEquals("Anchor must be counted once", 1, vulcanGen1.sourceCount)
        assertEquals("Sacrificed tower must be counted once", 1, frostGen1.sourceCount)

        // Gen 2 Collapse with one new sacrificed tower
        val gen2Sacrifice = TowerInstance(
            id = "s2",
            type = TowerType.CRYO,
            gridPos = GridPos(2, 1),
            towerClass = TowerClass.ELEMENTAL_VOID,
            tier = 3,
            archetype = TowerArchetype.FROST_SPIRE,
            selectedAuraIndex = 0
        )
        stateFlow.value = stateFlow.value.copy(towers = listOf(gen1Anchor, gen2Sacrifice), selectedTower = gen1Anchor)
        placement.confirmSingularityCollapse(TowerUpgradeManager::computeActiveSynergies)
        val gen2Synergies = TowerUpgradeManager.computeActiveSynergies(stateFlow.value.towers)
        val vulcanGen2 = gen2Synergies.first { it.auraName == TowerArchetype.VULCAN_GATLING.primaryAura.name }
        val frostGen2 = gen2Synergies.first { it.auraName == TowerArchetype.FROST_SPIRE.primaryAura.name }
        assertEquals("Vulcan anchor must remain count 1 after Gen 2", 1, vulcanGen2.sourceCount)
        assertEquals("Frost Spire total must be 2 (1 from Gen 1 + 1 from Gen 2)", 2, frostGen2.sourceCount)
    }

    @Test
    fun `findCustomTowerBalance isolates tiers and DEFAULT_TOWER_BALANCES aligns with ALL_52_DESCRIPTORS`() {
        val defaultCfg = DevBalanceConfig()
        assertFalse(defaultCfg.isGameplayModified())
        assertTrue(defaultCfg.exportDiffJson().contains("\"deltas\": {}"))
        for (desc in DevBalanceConfig.getAll52TowerDescriptors()) {
            val eff = defaultCfg.getEffectiveTowerBalance(desc)
            assertEquals("Cost mismatch for ${desc.key}", desc.defaultCost, eff.cost)
            assertEquals("Damage mismatch for ${desc.key}", desc.defaultDamage, eff.damage, 0.01f)
            assertEquals("Range mismatch for ${desc.key}", desc.defaultRange, eff.range, 0.01f)
            assertEquals("FireRate mismatch for ${desc.key}", desc.defaultFireRate, eff.fireRate, 0.01f)
            if (desc.level == 1 || desc.level == 3) {
                assertNull("customName must be null by default for ${desc.key}", eff.customName)
            }
        }

        val customL1Config = DevBalanceConfig(
            customTowerConfigs = mapOf(
                "GATLING" to TowerUnitBalance(
                    customName = "Quote\"Back\\Slash\nLine",
                    damage = 64f,
                    range = 3.0f,
                    fireRate = 2.0f,
                    cost = 80
                )
            )
        )
        val sniperL3 = TowerInstance(
            id = "sniper",
            type = TowerType.GATLING,
            gridPos = GridPos(0, 0),
            towerClass = TowerClass.KINETIC_BALLISTIC,
            tier = 3,
            branchTier1 = "A",
            archetype = TowerArchetype.SNIPER_SENTRY
        )
        assertNull("L1 override must not apply to unedited L3 archetype", customL1Config.findCustomTowerBalance(sniperL3))
        // L3 sniper has tier=3 -> t3Mul = 1.25f and isFinalForm = true -> apexBonus +0.30f (1.30x)
        assertEquals(TowerArchetype.SNIPER_SENTRY.baseDamage * 1.25f * 1.30f, sniperL3.getEffectiveDamage(customL1Config), 0.01f)

        val escapedJson = customL1Config.exportDiffJson()
        assertTrue(escapedJson.contains("Quote\\\"Back\\\\Slash\\nLine"))
    }

    @Test
    fun `getEffectiveCombatRange does not double-apply towerRngMul when balanceRange is provided`() {
        val supportL2 = TowerInstance(
            id = "sup_l2",
            type = TowerType.GATLING,
            gridPos = GridPos(0, 0),
            towerClass = TowerClass.KINETIC_BALLISTIC,
            tier = 1,
            vector = TowerVector.SUPPORT,
            branchTier1 = "C"
        )
        assertEquals(3.625f, supportL2.getEffectiveCombatRange(balanceRange = 3.625f), 0.001f)
    }

    @Test
    fun `wave completion preserves floatingTexts and breach tick skips post-mortem tower fire`() {
        val level = buildSimpleLevel()
        val tower = TowerInstance(
            id = "t_post",
            type = TowerType.GATLING,
            gridPos = GridPos(1, 1),
            lastShotTimeMs = -10_000L
        )
        val breachingEnemy = EnemyInstance(
            id = "e_breach_post",
            type = EnemyType.DRONE,
            laneIndex = 0,
            currentPathSegmentIndex = 2,
            segmentProgress = 0.99f,
            maxHp = 50f,
            currentHp = 50f,
            speed = 5.0f,
            worldX = 2.99f,
            worldY = 0f
        )
        val (breachFlow, breachCoord) = buildTestCoordinator(
            GameUIState(
                level = level,
                isWaveActive = true,
                waveNumber = 1,
                coreHp = 1,
                towers = listOf(tower),
                enemies = listOf(breachingEnemy)
            )
        )
        breachCoord.tickSimulation(0.1f, 100L, nowMs = 100L)
        assertTrue(breachFlow.value.isGameOver)
        assertEquals(0, breachFlow.value.totalKills)
        assertEquals(0f, breachFlow.value.totalDamageDealt, 0.001f)
        assertTrue(breachFlow.value.projectiles.isEmpty())

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val waveFlow = MutableStateFlow(
            GameUIState(
                level = level,
                isWaveActive = false,
                waveNumber = 1,
                currentWaveData = WaveGenerator.generateWave(1),
                towers = emptyList(),
                enemies = emptyList()
            )
        )
        val waveCoordinator = WaveProgressionCoordinator(
            uiStateFlow = waveFlow,
            remoteConfigManager = RemoteConfigManager(null),
            telemetryManager = TelemetryManager(app),
            nextId = { "id_$it" },
            addFloatingText = { _, _, _, _, _ -> },
            onRecordRunCompletion = { _, _ -> }
        )
        val soundMgr = SoundManager(app)
        val simCoord = SimulationCoordinator(
            uiStateFlow = waveFlow,
            combatEngine = CombatSimulationEngine(),
            waveCoordinator = waveCoordinator,
            soundManager = soundMgr,
            remoteConfigManager = RemoteConfigManager(null),
            cinematicController = CinematicController(object : CinematicHost {
                override val uiStateFlow = waveFlow
                override val soundManager = soundMgr
                override fun clearPendingSpawns() = waveCoordinator.clearPendingSpawns()
                override fun markSynergyDirty() {}
                override fun addFloatingText(x: Float, y: Float, text: String, color: Long, isCrit: Boolean) {}
                override fun swapBoosterPads() {}
                override fun deployWave() = waveCoordinator.deployWave()
            }),
            nextId = { "ft_test_$it" },
            onRecordRunCompletion = { _, _ -> }
        )
        waveCoordinator.deployWave()
        var timeMs = 0L
        while (waveCoordinator.hasPendingSpawns() && timeMs < 60_000L) {
            timeMs += 1000L
            simCoord.tickSimulation(0.05f, 1000L, nowMs = timeMs)
            waveFlow.value = waveFlow.value.copy(enemies = emptyList())
        }
        assertFalse(waveCoordinator.hasPendingSpawns())
        timeMs += 100L
        simCoord.tickSimulation(0.1f, 100L, nowMs = timeMs) // completes wave
        assertTrue(
            "Wave cleared banner must survive floatingTexts commit",
            waveFlow.value.floatingTexts.any { it.text.contains("Wave 1 Cleared!") }
        )
    }

    @Test
    fun `Tier-3 tower with legacy finalFormCode and null archetype does not double-stack global fire rate`() {
        val level = buildSimpleLevel()
        val legacyTier3 = TowerInstance(
            id = "t_leg_t3",
            type = TowerType.GATLING,
            gridPos = GridPos(1, 1),
            towerClass = TowerClass.KINETIC_BALLISTIC,
            tier = 3,
            finalFormCode = "A.a",
            archetype = null,
            lastShotTimeMs = -10_000L
        )
        val targetEnemy = EnemyInstance(
            id = "e_t3",
            type = EnemyType.BRUISER,
            laneIndex = 0,
            currentPathSegmentIndex = 1,
            segmentProgress = 0f,
            maxHp = 10_000f,
            currentHp = 10_000f,
            speed = 0.01f,
            worldX = 1f,
            worldY = 0f
        )
        val (flow, coord) = buildTestCoordinator(
            GameUIState(
                level = level,
                isWaveActive = true,
                towers = listOf(legacyTier3),
                enemies = listOf(targetEnemy)
            )
        )
        val singleCooldown = maxOf(50L, (1000f / legacyTier3.getEffectiveFireRate(globalFireRateBonus = 0.18f)).toLong())
        val doubleCooldown = maxOf(50L, (1000f / legacyTier3.getEffectiveFireRate(globalFireRateBonus = 0.36f)).toLong())
        assertTrue("Single-aura cooldown ($singleCooldown) must exceed double-stacked ($doubleCooldown)", singleCooldown > doubleCooldown + 5L)

        coord.tickSimulation(0.05f, 50L, nowMs = 50L) // fires at virtualSimTimeMs = 50L
        assertEquals(50L, legacyTier3.lastShotTimeMs)

        val midElapsed = (doubleCooldown + singleCooldown) / 2L
        coord.tickSimulation(midElapsed / 1000f, midElapsed, nowMs = 50L + midElapsed)
        assertEquals("Must not fire at double-stacked cooldown window ($midElapsed ms)", 50L, legacyTier3.lastShotTimeMs)

        val remainingToSingle = (singleCooldown - midElapsed) + 5L
        coord.tickSimulation(remainingToSingle / 1000f, remainingToSingle, nowMs = 50L + midElapsed + remainingToSingle)
        assertEquals(50L + midElapsed + remainingToSingle, legacyTier3.lastShotTimeMs)
        assertTrue(flow.value.totalDamageDealt > 0f)
    }

    @Test
    fun `TowerUpgradeBar uses roundToInt matching TowerPlacementCoordinator on fractional multiplier`() {
        val emptyTile = MapTile(GridPos(1, 1), TileType.EMPTY)
        val fractionalConfig = DevBalanceConfig(buildCostMultiplier = 1.12f) // 80 * 1.12 = 89.6 -> 90c
        composeRule.setContent {
            FloatingSquircleUpgradeBar(
                state = GameUIState(
                    scrap = 100,
                    selectedTile = emptyTile,
                    balanceConfig = fractionalConfig
                )
            )
        }
        composeRule.onNodeWithText("90c", substring = true).assertIsDisplayed()
    }
}
