package com.example

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.example.audio.BuildArchiveCatalog
import com.example.audio.SoundManager
import com.example.engine.MapGenerator
import com.example.engine.TelemetryManager
import com.example.engine.TowerUpgradeManager
import com.example.engine.WaveGenerator
import com.example.engine.coordinator.TowerPlacementCoordinator
import com.example.engine.coordinator.WaveProgressionCoordinator
import com.example.engine.RemoteConfigManager
import com.example.model.*
import com.example.ui.GameUIState
import com.example.ui.RogueTDViewModel
import com.example.ui.hud.MinimalTopHud
import com.example.ui.hud.components.SingularityWarningDialog
import com.example.ui.menu.AssetVaultDialog
import com.example.ui.menu.AudioVisualCustomizationPanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h900dp")
class Release041FeatureAndBalanceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ---------------------------------------------------------------------------------------------
    // 1. Bullet HUD — Synergy Counting with Duplicates, Singularity Separation & 4-Rotation Scroll
    // ---------------------------------------------------------------------------------------------

    private fun richSynergyHudState(angle: Float): GameUIState {
        val boardSyns = listOf(
            GlobalAuraSynergy("Rotary Overdrive", "+15% Kinetic Rate", 3, "⚔️", 0xFF00F0FF),
            GlobalAuraSynergy("Cryo Lattice", "+12% Slow Power", 2, "✨", 0xFF38BDF8),
            GlobalAuraSynergy("Siege Calibration", "+20% Mortar Splash", 2, "💥", 0xFFFFB800),
            GlobalAuraSynergy("Prismatic Sight", "+15% Range", 1, "★", 0xFFFFD700),
            GlobalAuraSynergy("Void Catalyst", "+18% Armor Shred", 1, "✨", 0xFFD946EF)
        )
        val vaultedSyns = listOf(
            GlobalAuraSynergy("Overclock Matrix", "+10% Global Fire Rate", 4, "⚡", 0xFFFFD700),
            GlobalAuraSynergy("Armor Melt", "+20% Armor Pierce", 2, "💥", 0xFFFF6B00),
            GlobalAuraSynergy("Solar Precision", "+15% Crit Chance", 2, "★", 0xFFFACC15),
            GlobalAuraSynergy("Scrap Surge", "+12% Kill Bounty", 1, "💎", 0xFF00FF9D),
            GlobalAuraSynergy("Tidal Chill", "+10% Slow Duration", 1, "✨", 0xFF22D3EE)
        )
        val capstone = TowerInstance(
            id = "cap_1",
            type = TowerType.GATLING,
            gridPos = GridPos(2, 2),
            tier = 3,
            archetype = TowerArchetype.VULCAN_GATLING,
            isCapstone = true,
            generation = 2,
            synergyVault = mutableListOf(BankedSynergyVault(generation = 2, synergies = vaultedSyns))
        )
        return GameUIState(
            isWaveActive = true,
            deviceRotationAngle = angle,
            towers = listOf(capstone),
            activeBoardSynergies = boardSyns,
            activeGlobalSynergies = TowerUpgradeManager.computeActiveSynergies(listOf(capstone))
        )
    }

    @Test
    fun `Bullet HUD pill includes duplicate stacks and popup separates board vs Singularity`() {
        composeRule.setContent {
            MinimalTopHud(
                state = richSynergyHudState(0f),
                onDeployWave = {},
                onToggleAutoWave = {}
            )
        }
        composeRule.waitForIdle()

        // 9 board stacks (3+2+2+1+1) + 10 vaulted stacks (4+2+2+1+1) = 19 total stacks
        composeRule.onNodeWithText("(19)").assertExists()
        composeRule.onNodeWithTag("hud_synergy_pill").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("ACTIVE SYNERGIES (9)", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Rotary Overdrive (3x)", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hud_synergy_popup_scroll", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 800f) }
        composeRule.onNodeWithText("SINGULARITY TOWER", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("GEN 2 • 10 Banked", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hud_synergy_popup_close").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_synergy_popup_close").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("(19)").assertExists()
    }

    private fun assertSynergyPopupScrollsAndKeepsCloseVisible(angle: Float) {
        composeRule.setContent {
            MinimalTopHud(
                state = richSynergyHudState(angle),
                onDeployWave = {},
                onToggleAutoWave = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hud_synergy_pill").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("hud_synergy_popup").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_synergy_popup_close").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_synergy_popup_scroll", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 1200f) }
        composeRule.onNodeWithText("Tidal Chill", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("hud_synergy_popup_close").assertIsDisplayed()
    }

    @Test fun `synergy popup scrolls with CLOSE pinned at 0 deg`() = assertSynergyPopupScrollsAndKeepsCloseVisible(0f)
    @Test fun `synergy popup scrolls with CLOSE pinned at 90 deg`() = assertSynergyPopupScrollsAndKeepsCloseVisible(90f)
    @Test fun `synergy popup scrolls with CLOSE pinned at 180 deg`() = assertSynergyPopupScrollsAndKeepsCloseVisible(180f)
    @Test fun `synergy popup scrolls with CLOSE pinned at 270 deg`() = assertSynergyPopupScrollsAndKeepsCloseVisible(270f)

    @Test
    fun `TowerUpgradeManager separates computeBoardSynergies from computeActiveSynergies`() {
        val boardTower1 = TowerInstance(
            id = "t1", type = TowerType.GATLING, gridPos = GridPos(1, 1),
            tier = 3, archetype = TowerArchetype.VULCAN_GATLING, selectedAuraIndex = 0
        )
        val boardTower2 = TowerInstance(
            id = "t2", type = TowerType.GATLING, gridPos = GridPos(1, 2),
            tier = 3, archetype = TowerArchetype.VULCAN_GATLING, selectedAuraIndex = 0
        )
        val capstone = TowerInstance(
            id = "cap", type = TowerType.CRYO, gridPos = GridPos(2, 2),
            tier = 3, archetype = TowerArchetype.FROST_SPIRE, selectedAuraIndex = 0,
            isCapstone = true, generation = 1,
            synergyVault = mutableListOf(
                BankedSynergyVault(
                    generation = 1,
                    synergies = listOf(GlobalAuraSynergy(TowerArchetype.VULCAN_GATLING.primaryAura.name, "desc", 3, "⚔️", 0xFF00F0FF))
                )
            )
        )
        val allTowers = listOf(boardTower1, boardTower2, capstone)
        val boardOnly = TowerUpgradeManager.computeBoardSynergies(allTowers)
        val combined = TowerUpgradeManager.computeActiveSynergies(allTowers)

        assertEquals(3, boardOnly.sumOf { it.sourceCount })
        assertEquals(2, boardOnly.first { it.auraName == TowerArchetype.VULCAN_GATLING.primaryAura.name }.sourceCount)
        assertEquals(6, combined.sumOf { it.sourceCount })
        assertEquals(5, combined.first { it.auraName == TowerArchetype.VULCAN_GATLING.primaryAura.name }.sourceCount)
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Singularity Collapse — New Map, Anchor/Booster Preservation & Mid-Round Enemy Redispatch
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `Singularity collapse generates new map preserving anchor booster pair and resets mid-round enemies`() {
        val sm = SoundManager(context)
        val tm = TelemetryManager(context)
        val level = MapGenerator.generate(seed = 12345L, width = 10, height = 20)
        val ampPads = level.tiles.filter { it.buff == TileBuff.AMPLIFIER }
        assertEquals(2, ampPads.size)
        val anchorPos = ampPads[0].pos
        val partnerPadPos = ampPads[1].pos

        val buildableTiles = level.tiles.filter { it.isBuildable }
        val towers = buildableTiles.mapIndexed { idx, tile ->
            TowerInstance(
                id = "t_$idx",
                type = TowerType.GATLING,
                gridPos = tile.pos,
                tier = 3,
                archetype = TowerArchetype.VULCAN_GATLING,
                investedScrap = 500
            )
        }
        val anchor = towers.first { it.gridPos == anchorPos }
        val midLaneEnemy = EnemyInstance(
            id = "e_mid",
            type = EnemyType.SOLDIER,
            laneIndex = 0,
            currentPathSegmentIndex = 4,
            segmentProgress = 0.6f,
            maxHp = 120f,
            currentHp = 120f,
            speed = 2.0f,
            worldX = level.laneAlpha[4].x.toFloat(),
            worldY = level.laneAlpha[4].y.toFloat()
        )

        val stateFlow = MutableStateFlow(
            GameUIState(
                seed = level.seed,
                level = level,
                towers = towers,
                selectedTower = anchor,
                scrap = 5_000,
                isWaveActive = true,
                enemies = listOf(midLaneEnemy),
                projectiles = listOf(
                    Projectile(
                        id = "p1",
                        startX = 1f,
                        startY = 1f,
                        currentX = 1f,
                        currentY = 1f,
                        targetEnemyId = "e_mid",
                        targetX = 2f,
                        targetY = 2f,
                        speed = 10f,
                        damage = 5f,
                        color = 0xFF00F0FF
                    )
                ),
                laserBeams = listOf(LaserBeam("b1", 1f, 1f, 2f, 2f, 0xFF00F0FF, 100L)),
                lightningArcs = listOf(LightningArc("l1", listOf(1f to 1f, 2f to 2f), 0xFF00F0FF, 100L)),
                isSingularityWarningOpen = true
            )
        )

        var seq = 0
        val waveCoord = WaveProgressionCoordinator(
            uiStateFlow = stateFlow,
            remoteConfigManager = RemoteConfigManager(),
            telemetryManager = tm,
            nextId = { p -> "$p${seq++}" },
            addFloatingText = { _, _, _, _, _ -> },
            onRecordRunCompletion = { _, _ -> }
        )
        val placementCoord = TowerPlacementCoordinator(
            uiStateFlow = stateFlow,
            soundManager = sm,
            telemetryManager = tm,
            nextId = { p -> "$p${seq++}" },
            addFloatingText = { _, _, _, _, _ -> },
            onSynergyDirty = {},
            onRedispatchEnemies = waveCoord::requeueActiveEnemies
        )

        placementCoord.confirmSingularityCollapse(TowerUpgradeManager::computeActiveSynergies)

        val nextState = stateFlow.value
        val nextLevel = requireNotNull(nextState.level)
        assertNotEquals("Singularity must generate a new map", level.seed, nextLevel.seed)
        assertEquals(1, nextState.towers.size)
        assertTrue(nextState.towers.first().isCapstone)
        assertEquals(anchorPos, nextState.towers.first().gridPos)

        // Preserved booster pair remains intact on the new map, and all 3 booster types have 2 pads
        val newAmpPads = nextLevel.tiles.filter { it.buff == TileBuff.AMPLIFIER }.map { it.pos }.toSet()
        assertEquals(setOf(anchorPos, partnerPadPos), newAmpPads)
        assertEquals(2, nextLevel.tiles.count { it.buff == TileBuff.HIGH_GROUND })
        assertEquals(2, nextLevel.tiles.count { it.buff == TileBuff.CONDUIT })

        // In-flight combat artifacts cleared & mid-round enemy queued for clean redispatch from new spawner
        assertTrue(nextState.projectiles.isEmpty())
        assertTrue(nextState.laserBeams.isEmpty())
        assertTrue(nextState.lightningArcs.isEmpty())
        assertTrue(nextState.enemies.isEmpty())
        assertTrue(waveCoord.hasPendingSpawns())

        val spawnedBack = mutableListOf<EnemyInstance>()
        waveCoord.tickWaveSpawning(stateFlow.value, nextLevel, 500L, spawnedBack)
        assertEquals(1, spawnedBack.size)
        val redispatched = spawnedBack.first()
        assertEquals(0, redispatched.currentPathSegmentIndex)
        assertEquals(nextLevel.laneAlpha.first().x.toFloat(), redispatched.worldX, 0.01f)
        assertEquals(nextLevel.laneAlpha.first().y.toFloat(), redispatched.worldY, 0.01f)

        sm.release()
    }

    @Test
    fun `SingularityWarningDialog informs player of new map and mid-round enemy reset`() {
        val level = MapGenerator.generate(seed = 999L, width = 10, height = 20)
        val ampPos = level.tiles.first { it.buff == TileBuff.AMPLIFIER }.pos
        val anchor = TowerInstance("t1", TowerType.GATLING, ampPos, tier = 3, archetype = TowerArchetype.VULCAN_GATLING)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SingularityWarningDialog(
                state = GameUIState(
                    level = level,
                    towers = listOf(anchor),
                    selectedTower = anchor,
                    isWaveActive = true,
                    isSingularityWarningOpen = true
                ),
                onConfirm = {},
                onDismiss = {}
            )
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("NEW MAP + 2 BOOSTER PADS", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("reset to spawners", substring = true).assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------------------------
    // 3. Skin & Theme Live Interactive Preview
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `AudioVisualCustomizationPanel renders live 4-class skin preview and map lane preview`() {
        var equippedSkinSlot = 1
        var equippedThemeSlot = 1
        composeRule.setContent {
            AudioVisualCustomizationPanel(
                state = GameUIState(turretSkinSlot = 1, turretSkinId = "default_skin_1", mapThemeSlot = 1, mapThemeId = "default_theme_1"),
                onMasterVolumeChanged = {},
                onMusicVolumeChanged = {},
                onSfxVolumeChanged = {},
                onUpgradeVolumeChanged = {},
                onToggleAudioMute = {},
                onSoundProfileChanged = {},
                onMusicTrackSelected = {},
                onKineticSfxSlotSelected = {},
                onElementalSfxSlotSelected = {},
                onSiegeSfxSlotSelected = {},
                onPlayTurretTestSound = { _, _ -> },
                onPlayTowerUpgradeTestSound = { _, _ -> },
                onTurretSkinSlotSelected = { equippedSkinSlot = it },
                onMapThemeSlotSelected = { equippedThemeSlot = it },
                animateSkinPreviewPulse = false
            )
        }

        composeRule.onNodeWithText("TURRET VISUAL SKINS (BUILD #${BuildConfig.VERSION_CODE} LINEUP)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("turret_skin_live_preview_stage").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("skin_preview_class_KINETIC_BALLISTIC").assertIsDisplayed()
        composeRule.onNodeWithTag("skin_preview_class_ELEMENTAL_VOID").assertIsDisplayed()
        composeRule.onNodeWithTag("skin_preview_class_HEAVY_SIEGE").assertIsDisplayed()
        composeRule.onNodeWithTag("skin_preview_class_STELLAR_COSMIC").assertIsDisplayed()
        composeRule.onNodeWithText("⬢ Heavy").assertIsDisplayed()

        val activeSkin2Id = BuildArchiveCatalog.buildActiveSkinId(2)
        val activeSkin2Name = BuildArchiveCatalog.resolveTurretSkin(activeSkin2Id, 2).displayName
        // Preview active build's Slot 2 skin without equipping yet, then equip via stage CTA
        composeRule.onNodeWithTag("skin_preview_btn_$activeSkin2Id").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, equippedSkinSlot)
        composeRule.onNodeWithText("PREVIEW: $activeSkin2Name").assertIsDisplayed()
        composeRule.onNodeWithTag("turret_skin_preview_equip_btn").performScrollTo().performClick()
        assertEquals(2, equippedSkinSlot)

        val activeTheme3Id = BuildArchiveCatalog.buildActiveThemeId(3)
        val activeTheme3Name = BuildArchiveCatalog.resolveMapTheme(activeTheme3Id, 3).displayName
        // Preview active build's Slot 3 map theme without equipping yet, then equip via stage CTA
        composeRule.onNodeWithTag("map_theme_live_preview_stage").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("theme_preview_btn_$activeTheme3Id").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, equippedThemeSlot)
        composeRule.onNodeWithText("PREVIEW: $activeTheme3Name").assertIsDisplayed()
        composeRule.onNodeWithTag("map_theme_preview_equip_btn").performScrollTo().performClick()
        assertEquals(3, equippedThemeSlot)
    }

    @Test
    fun `AssetVaultDialog opens live visual preview stage when previewing skins or themes`() = runBlocking {
        val sm = SoundManager(context)
        sm.awaitPendingIoForTest()
        val assets = sm.assetLibraryFlow.value

        composeRule.setContent {
            AssetVaultDialog(
                assets = assets,
                syncStatusText = "Ratings auto-saved locally",
                onPreviewAsset = {},
                onStopPreview = {},
                onLikeAsset = {},
                onDislikeAsset = {},
                onStarAsset = {},
                onEquipAsset = {},
                onCloudSync = {},
                onDismiss = {}
            )
        }

        val activeTheme4Id = BuildArchiveCatalog.buildActiveThemeId(4)
        val activeTheme4Name = BuildArchiveCatalog.resolveMapTheme(activeTheme4Id, 4).displayName
        composeRule.onNodeWithTag("vault_tab_MAP_THEMES").performScrollTo().performClick()
        composeRule.onNodeWithTag("asset_vault_list").performScrollToNode(hasTestTag("vault_preview_$activeTheme4Id"))
        composeRule.onNodeWithTag("vault_preview_$activeTheme4Id").performClick()
        composeRule.onNodeWithTag("vault_visual_preview_stage").assertIsDisplayed()
        composeRule.onNodeWithText("PREVIEW: $activeTheme4Name").assertIsDisplayed()

        val activeSkin2Id = BuildArchiveCatalog.buildActiveSkinId(2)
        val activeSkin2Name = BuildArchiveCatalog.resolveTurretSkin(activeSkin2Id, 2).displayName
        composeRule.onNodeWithTag("vault_tab_TURRET_SKINS").performScrollTo().performClick()
        composeRule.onNodeWithTag("asset_vault_list").performScrollToNode(hasTestTag("vault_preview_$activeSkin2Id"))
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("vault_preview_$activeSkin2Id").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("vault_visual_preview_stage").assertIsDisplayed()
        composeRule.onNodeWithText("PREVIEW: $activeSkin2Name").assertIsDisplayed()
        composeRule.onNodeWithText("⬢ Heavy").assertIsDisplayed()

        sm.release()
        sm.awaitPendingIoForTest()
    }

    // ---------------------------------------------------------------------------------------------
    // 4 & 5 & 7. Base Game Speed, Difficulty Rebalance & Heavy Hexagon
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `Base simulation speed multiplier boosts 1X playability`() {
        assertEquals(1.85f, RogueTDViewModel.BASE_SIM_SPEED_MULTIPLIER, 0.01f)
    }

    @Test
    fun `Difficulty scaling and wave economy prevent runaway late-game scrap inflation`() {
        val easyW30 = WaveGenerator.generateWave(30, difficulty = GameDifficulty.EASY)
        val medW50 = WaveGenerator.generateWave(50, difficulty = GameDifficulty.MEDIUM)
        val hardW63 = WaveGenerator.generateWave(63, difficulty = GameDifficulty.HARD)

        // Completion bonuses stay capped well below legacy runaway payouts
        assertTrue("Easy W30 bonus must be tightened (${easyW30.completionBonusScrap})", easyW30.completionBonusScrap <= 290)
        assertTrue("Hard W63 bonus must be tightened (${hardW63.completionBonusScrap})", hardW63.completionBonusScrap <= 350)

        // Harder modes scale materially above Easy
        val easyHp30 = easyW30.spawnList.first().hpScale
        val hardHp63 = hardW63.spawnList.first().hpScale
        assertTrue("Hard W63 HP ($hardHp63) must crush Easy W30 HP ($easyHp30)", hardHp63 > easyHp30 * 5f)
        assertTrue("Medium W50 HP must exceed Easy W30 HP", medW50.spawnList.first().hpScale > easyHp30)
    }

    @Test
    fun `Heavy Siege uses Hexagon silhouette while keeping square shape matching intact`() {
        assertEquals("⬢", TowerClass.HEAVY_SIEGE.shapeSymbol)
        assertEquals("Hexagon", TowerClass.HEAVY_SIEGE.shapeName)
        assertEquals(EnemyShape.SQUARE, TowerInstance("h1", TowerType.MORTAR, GridPos(0, 0), towerClass = TowerClass.HEAVY_SIEGE).shape)
    }
}
