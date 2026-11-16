package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.example.audio.SoundManager
import com.example.engine.AppUpdateState
import com.example.engine.MapGenerator
import com.example.engine.TelemetryManager
import com.example.engine.TowerUpgradeManager
import com.example.engine.UpdateDownloadStatus
import com.example.engine.UpdateInfo
import com.example.engine.coordinator.TowerPlacementCoordinator
import com.example.model.GlobalAuraSynergy
import com.example.model.GridPos
import com.example.model.TargetPriority
import com.example.model.TowerArchetype
import com.example.model.TowerClass
import com.example.model.TowerInstance
import com.example.model.TowerType
import com.example.model.TowerVector
import com.example.ui.GameUIState
import com.example.ui.ROUND1_TILE_HINT_TEXT
import com.example.ui.Round1LaunchBanner
import com.example.ui.Round1TileHintBanner
import com.example.ui.RogueTDScreen
import com.example.ui.RogueTDViewModel
import com.example.ui.UpdateBanner
import com.example.ui.canvas.resolveTier3SynergyTower
import com.example.ui.clampHudOffsetY
import com.example.ui.floatingTopHudPlacement
import com.example.ui.hud.FloatingSquircleUpgradeBar
import com.example.ui.hud.MinimalTopHud
import com.example.ui.hud.SquircleActionButton
import com.example.ui.hud.getArchetypeCardLabels
import com.example.ui.hud.components.EdgeBulletTipTab
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Release v0.0.1.033 hotfix coverage: top-HUD clamp / safe insets, Round-1 banners, battery -> pause,
 * unified orientation-aware telemetry popups, pulsing edge tabs, untruncated squircle labels,
 * squircle auto-close + persistent Tier-3 synergy tower.
 *
 * Any test with infinite animations (pulsing tabs, full screen) pauses the main clock to avoid AppNotIdleException.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h900dp")
class BulletHudModernization033Test {

    @get:Rule
    val composeRule = createComposeRule()

    // ---------------------------------------------------------------------------------------------
    // Task 1: top HUD drag floor + safe insets
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `top HUD drag offset is clamped at zero and never negative`() {
        assertEquals(0f, clampHudOffsetY(0f, -48f, 200f), 0f)
        assertEquals(0f, clampHudOffsetY(10f, -500f, 200f), 0f)
        assertEquals(12f, clampHudOffsetY(0f, 12f, 200f), 0f)
        assertEquals(200f, clampHudOffsetY(190f, 50f, 200f), 0f)
    }

    @Test
    fun `top HUD rests below display cutout inset at zero drag offset`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                MinimalTopHud(
                    state = GameUIState(isWaveActive = true, deviceRotationAngle = 0f),
                    onDeployWave = {},
                    onToggleAutoWave = {},
                    modifier = Modifier.floatingTopHudPlacement(WindowInsets(top = 40.dp)) { 0f }
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        val top = composeRule.onNodeWithTag("hud_pause_button").getUnclippedBoundsInRoot().top
        assertTrue("HUD top ($top) must sit at/below the 40dp cutout inset", top.value >= 40f)
    }

    // ---------------------------------------------------------------------------------------------
    // Task 2: Round-1 banners + UpdateBanner counter-rotation
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `round 1 tile hint says SELECT ANY TILE TO BUILD and is clickable`() {
        var clicks = 0
        composeRule.setContent {
            Round1TileHintBanner(counterRotation = 0f, onClick = { clicks++ })
        }
        assertEquals("SELECT ANY TILE TO BUILD (Tap boosted pad)", ROUND1_TILE_HINT_TEXT)
        composeRule.onNodeWithText("SELECT ANY TILE TO BUILD", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("SELECT TILE TO BUILD", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("round1_tile_hint_banner").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `round 1 banners and update banner render at 180 degree counter rotation`() {
        val info = UpdateInfo(isUpdateAvailable = true, latestVersion = "0.0.1.034", currentVersion = "0.0.1.033")
        composeRule.setContent {
            androidx.compose.foundation.layout.Column {
                Round1TileHintBanner(counterRotation = 180f, onClick = {})
                Round1LaunchBanner(counterRotation = 180f, onClick = {})
                UpdateBanner(
                    updateState = AppUpdateState(status = UpdateDownloadStatus.AVAILABLE, updateInfo = info),
                    onDownloadAndInstall = {},
                    counterRotation = 180f
                )
            }
        }
        composeRule.onNodeWithTag("round1_tile_hint_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("round1_launch_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("update_banner").assertIsDisplayed()
    }

    @Test
    fun `round 1 tile hint swaps layout extents when counter rotated 90 degrees`() {
        var rotation by mutableFloatStateOf(0f)
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                Round1TileHintBanner(counterRotation = rotation, onClick = {})
            }
        }
        val upright = composeRule.onNodeWithTag("round1_tile_hint_banner").getBoundsInRoot()
        val uprightHeight = (upright.bottom - upright.top).value
        composeRule.runOnIdle { rotation = 90f }
        val rotated = composeRule.onNodeWithTag("round1_tile_hint_banner").getBoundsInRoot()
        val rotatedWidth = (rotated.right - rotated.left).value
        assertTrue(
            "Rotated banner width $rotatedWidth should be ~upright height $uprightHeight",
            abs(rotatedWidth - uprightHeight) < 2f
        )
    }

    private inline fun withClearedViewModel(block: (RogueTDViewModel) -> Unit) {
        val store = androidx.lifecycle.ViewModelStore()
        val vm = androidx.lifecycle.ViewModelProvider(
            store,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(RuntimeEnvironment.getApplication())
        )[RogueTDViewModel::class.java]
        try {
            block(vm)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `full RogueTDScreen shows lowered round 1 tile hint`() {
        withClearedViewModel { vm ->
            composeRule.mainClock.autoAdvance = false
            composeRule.setContent { RogueTDScreen(viewModel = vm) }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithText("SELECT ANY TILE TO BUILD", substring = true).assertIsDisplayed()
            // Lowered banner sits below vertical center (~3/4 height), clear of the top HUD
            val bounds = composeRule.onNodeWithTag("round1_tile_hint_banner").getBoundsInRoot()
            assertTrue("Hint top ${bounds.top} should be in the lower half of a 900dp screen", bounds.top.value > 450f)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Task 3: battery widget opens pause menu
    // ---------------------------------------------------------------------------------------------

    private fun assertBatteryOpensPause(angle: Float) {
        var pauseClicks = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MinimalTopHud(
                state = GameUIState(isWaveActive = true, deviceRotationAngle = angle),
                onDeployWave = {},
                onToggleAutoWave = {},
                onOpenPauseMenu = { pauseClicks++ }
            )
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("hud_battery_widget").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("hud_pause_button").performClick()
        assertEquals("angle=$angle", 2, pauseClicks)
    }

    @Test
    fun `tapping battery widget triggers onOpenPauseMenu at 0 degrees`() = assertBatteryOpensPause(0f)

    @Test
    fun `tapping battery widget triggers onOpenPauseMenu at 90 degrees`() = assertBatteryOpensPause(90f)

    @Test
    fun `tapping battery widget triggers onOpenPauseMenu at 180 degrees`() = assertBatteryOpensPause(180f)

    @Test
    fun `tapping battery widget triggers onOpenPauseMenu at 270 degrees`() = assertBatteryOpensPause(270f)

    // ---------------------------------------------------------------------------------------------
    // Task 4: unified telemetry popups
    // ---------------------------------------------------------------------------------------------

    private fun hudStateAt(angle: Float) = GameUIState(
        isWaveActive = true,
        deviceRotationAngle = angle,
        harmonicPadsCount = 2,
        activeGlobalSynergies = listOf(
            GlobalAuraSynergy(
                auraName = "Overclock",
                effectDescription = "+10% fire rate",
                sourceCount = 1,
                iconEmoji = "⚡",
                color = 0xFFFFD700
            )
        )
    )

    private fun assertPopupAnchored(angle: Int, pillTag: String, popupTag: String, headerText: String) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MinimalTopHud(
                state = hudStateAt(angle.toFloat()),
                onDeployWave = {},
                onToggleAutoWave = {}
            )
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag(pillTag).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(600)

        composeRule.onNodeWithText(headerText, substring = true).assertIsDisplayed()
        val popup = composeRule.onNodeWithTag(popupTag).assertIsDisplayed().getBoundsInRoot()
        val scrim = composeRule.onNodeWithTag("${popupTag}_scrim").getBoundsInRoot()

        when (angle) {
            0 -> assertTrue("0deg popup anchors to the top", (popup.top.value - scrim.top.value) < 80f)
            90 -> {
                assertTrue("90deg popup anchors to the left edge", (popup.left.value - scrim.left.value) < 24f)
                assertTrue(
                    "90deg popup is counter-rotated (visual height > width)",
                    (popup.bottom - popup.top) > (popup.right - popup.left)
                )
            }
            180 -> assertTrue("180deg popup anchors to the bottom (visual top)", (scrim.bottom.value - popup.bottom.value) < 24f)
            270 -> {
                assertTrue("270deg popup anchors to the right edge", (scrim.right.value - popup.right.value) < 24f)
                assertTrue(
                    "270deg popup is counter-rotated (visual height > width)",
                    (popup.bottom - popup.top) > (popup.right - popup.left)
                )
            }
        }

        composeRule.onNodeWithTag("${popupTag}_close").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag(popupTag).assertDoesNotExist()
    }

    @Test
    fun `teleport pads popup is unified and anchored at 0 90 180 and 270 degrees`() {
        assertPopupAnchored(0, "hud_teleport_pads_pill", "hud_teleport_pads_popup", "TELEPORTATION PADS")
    }

    @Test
    fun `teleport pads popup anchored at 90 degrees`() {
        assertPopupAnchored(90, "hud_teleport_pads_pill", "hud_teleport_pads_popup", "TELEPORTATION PADS")
    }

    @Test
    fun `teleport pads popup anchored at 180 degrees`() {
        assertPopupAnchored(180, "hud_teleport_pads_pill", "hud_teleport_pads_popup", "TELEPORTATION PADS")
    }

    @Test
    fun `teleport pads popup anchored at 270 degrees`() {
        assertPopupAnchored(270, "hud_teleport_pads_pill", "hud_teleport_pads_popup", "TELEPORTATION PADS")
    }

    @Test
    fun `synergy popup anchored at 0 degrees`() {
        assertPopupAnchored(0, "hud_synergy_pill", "hud_synergy_popup", "ACTIVE SYNERGIES")
    }

    @Test
    fun `synergy popup anchored at 90 degrees`() {
        assertPopupAnchored(90, "hud_synergy_pill", "hud_synergy_popup", "ACTIVE SYNERGIES")
    }

    @Test
    fun `synergy popup anchored at 180 degrees`() {
        assertPopupAnchored(180, "hud_synergy_pill", "hud_synergy_popup", "ACTIVE SYNERGIES")
    }

    @Test
    fun `synergy popup anchored at 270 degrees`() {
        assertPopupAnchored(270, "hud_synergy_pill", "hud_synergy_popup", "ACTIVE SYNERGIES")
    }

    @Test
    fun `popup dismisses when scrim is tapped`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MinimalTopHud(state = hudStateAt(0f), onDeployWave = {}, onToggleAutoWave = {})
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("hud_synergy_pill").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.onNodeWithTag("hud_synergy_popup").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_synergy_popup_scrim").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("hud_synergy_popup").assertDoesNotExist()
    }

    // ---------------------------------------------------------------------------------------------
    // Task 5: edge bullet tip tab pulse
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `EdgeBulletTipTab pulsing keeps touch target and click with paused clock`() {
        var clicks = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            EdgeBulletTipTab(
                isLeftEdge = false,
                icon = Icons.Filled.SwapHoriz,
                contentDescription = "Swap",
                tag = "pulsing_tab",
                isPulsing = true,
                onClick = { clicks++ }
            )
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(550)
        val bounds = composeRule.onNodeWithTag("pulsing_tab").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals(48f, (bounds.right - bounds.left).value, 0.5f)
        assertEquals(48f, (bounds.bottom - bounds.top).value, 0.5f)
        composeRule.onNodeWithTag("pulsing_tab").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, clicks)
    }

    // ---------------------------------------------------------------------------------------------
    // Task 6: untruncated squircle labels
    // ---------------------------------------------------------------------------------------------

    private fun layoutResultFor(text: String): TextLayoutResult {
        val node = composeRule.onNodeWithText(text).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first()
    }

    private fun assertTextFitsSingleLine(text: String) {
        val lr = layoutResultFor(text)
        assertEquals("'$text' should fit on 1 line", 1, lr.lineCount)
        assertFalse("'$text' must not be ellipsized", lr.isLineEllipsized(0))
        assertFalse("'$text' must not overflow height", lr.didOverflowHeight)
        assertTrue(
            "'$text' width (${lr.getLineRight(0)}) must fit constraints.maxWidth (${lr.layoutInput.constraints.maxWidth})",
            lr.getLineRight(0) <= lr.layoutInput.constraints.maxWidth.toFloat()
        )
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
    fun `TargetPriorityMenu renders full FIRST label inside a 360dp dock`() {
        val tower = TowerInstance(id = "t1", type = TowerType.GATLING, gridPos = GridPos(1, 1), tier = 0, targetPriority = TargetPriority.FIRST)
        composeRule.setContent {
            FloatingSquircleUpgradeBar(state = GameUIState(scrap = 500, selectedTower = tower))
        }
        composeRule.onNodeWithText("FIRST").assertIsDisplayed()
        composeRule.onNodeWithText("FIRS").assertDoesNotExist()
        assertTextFitsSingleLine("FIRST")
        val right = composeRule.onNodeWithTag("target_priority_button").getUnclippedBoundsInRoot().right
        assertTrue("Priority pill right edge $right must fit inside 360dp", right.value <= 360f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
    fun `TargetPriorityMenu renders full STRONGEST label without clipping`() {
        val tower = TowerInstance(id = "t1", type = TowerType.GATLING, gridPos = GridPos(1, 1), tier = 0, targetPriority = TargetPriority.STRONGEST)
        composeRule.setContent {
            FloatingSquircleUpgradeBar(state = GameUIState(scrap = 500, selectedTower = tower))
        }
        composeRule.onNodeWithText("STRONGEST").assertIsDisplayed()
        assertTextFitsSingleLine("STRONGEST")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [35], qualifiers = "w480dp-h900dp-xxhdpi")
    fun `archetype squircle labels use full Singularity and fit the card`() {
        val (title, subtitle, _) = getArchetypeCardLabels(TowerArchetype.BLACK_HOLE_SENTRY)
        assertEquals("Singularity", title)
        assertEquals("Bastion", subtitle)

        composeRule.setContent {
            SquircleActionButton(
                title = title,
                subtitle = subtitle,
                cost = 100,
                themeColor = Color(0xFF00F0FF),
                canAfford = true,
                isUnlocked = true,
                itemWidth = 84.dp,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("Singularity").assertIsDisplayed()
        assertTextFitsSingleLine("Singularity")
    }

    @Test
    fun `no archetype title is abbreviated with a trailing dot`() {
        TowerArchetype.entries.forEach { arch ->
            val (title, _, _) = getArchetypeCardLabels(arch)
            assertFalse("Title '$title' for $arch must not be abbreviated", title.endsWith("."))
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Task 7: auto-close + persistent selected tower id
    // ---------------------------------------------------------------------------------------------

    private fun newCoordinator(flow: MutableStateFlow<GameUIState>): TowerPlacementCoordinator {
        val app = RuntimeEnvironment.getApplication()
        var counter = 0
        return TowerPlacementCoordinator(
            uiStateFlow = flow,
            soundManager = SoundManager(app),
            telemetryManager = TelemetryManager(app),
            nextId = { "$it${counter++}" },
            addFloatingText = { _, _, _, _, _ -> },
            onSynergyDirty = {}
        )
    }

    private fun buildCost(state: GameUIState): Int =
        (TowerClass.KINETIC_BALLISTIC.baseCost * state.balanceConfig.buildCostMultiplier).roundToInt()

    @Test
    fun `buildTower auto-closes squircles on low scrap and keeps lastSelectedTowerId`() {
        val level = MapGenerator.generate(seed = 42L)
        val tile = level.tiles.first { it.isBuildable }
        val base = GameUIState(level = level)
        val vecCost = TowerUpgradeManager.getTier1VectorCost(TowerClass.KINETIC_BALLISTIC)
        val flow = MutableStateFlow(base.copy(scrap = buildCost(base) + vecCost - 1, selectedTile = tile))
        val coordinator = newCoordinator(flow)

        coordinator.buildTower(tile)

        val s = flow.value
        assertEquals(1, s.towers.size)
        assertNull(s.selectedTower)
        assertNull(s.selectedTile)
        assertEquals(s.towers.first().id, s.lastSelectedTowerId)
    }

    @Test
    fun `buildTower keeps squircles open when next upgrade is affordable`() {
        val level = MapGenerator.generate(seed = 42L)
        val tile = level.tiles.first { it.isBuildable }
        val base = GameUIState(level = level)
        val vecCost = TowerUpgradeManager.getTier1VectorCost(TowerClass.KINETIC_BALLISTIC)
        val flow = MutableStateFlow(base.copy(scrap = buildCost(base) + vecCost, selectedTile = tile))
        val coordinator = newCoordinator(flow)

        coordinator.buildTower(tile)

        val s = flow.value
        assertNotNull(s.selectedTower)
        assertEquals(s.selectedTower?.id, s.lastSelectedTowerId)
    }

    @Test
    fun `upgradeTowerToVector auto-closes when cheapest Tier 2 is unaffordable`() {
        val level = MapGenerator.generate(seed = 42L)
        val tile = level.tiles.first { it.isBuildable }
        val tower = TowerInstance(id = "t1", gridPos = tile.pos, tier = 0, investedScrap = 100)
        val vecCost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
        val cheapestT2 = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, TowerVector.OFFENSIVE)
            .minOf { TowerUpgradeManager.getTier2Cost(it) }
        val flow = MutableStateFlow(
            GameUIState(level = level, towers = listOf(tower), selectedTower = tower, selectedTile = tile, scrap = vecCost + cheapestT2 - 1)
        )
        val coordinator = newCoordinator(flow)

        coordinator.upgradeTowerToVector(tower, TowerVector.OFFENSIVE)

        val s = flow.value
        assertEquals(1, tower.tier)
        assertNull(s.selectedTower)
        assertNull(s.selectedTile)
        assertEquals("t1", s.lastSelectedTowerId)
    }

    @Test
    fun `upgradeTowerToVector stays open when Tier 2 is affordable`() {
        val level = MapGenerator.generate(seed = 42L)
        val tile = level.tiles.first { it.isBuildable }
        val tower = TowerInstance(id = "t1", gridPos = tile.pos, tier = 0, investedScrap = 100)
        val vecCost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
        val cheapestT2 = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, TowerVector.OFFENSIVE)
            .minOf { TowerUpgradeManager.getTier2Cost(it) }
        val flow = MutableStateFlow(
            GameUIState(level = level, towers = listOf(tower), selectedTower = tower, selectedTile = tile, scrap = vecCost + cheapestT2)
        )
        newCoordinator(flow).upgradeTowerToVector(tower, TowerVector.OFFENSIVE)
        assertNotNull(flow.value.selectedTower)
    }

    @Test
    fun `upgradeTowerToArchetype does NOT auto-close at Tier 2 even with zero scrap left`() {
        val level = MapGenerator.generate(seed = 42L)
        val tile = level.tiles.first { it.isBuildable }
        val tower = TowerInstance(id = "t1", gridPos = tile.pos, tier = 1, vector = TowerVector.OFFENSIVE, investedScrap = 200)
        val arch = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, TowerVector.OFFENSIVE).first()
        val cost = TowerUpgradeManager.getTier2Cost(arch)
        val flow = MutableStateFlow(
            GameUIState(level = level, towers = listOf(tower), selectedTower = tower, selectedTile = tile, scrap = cost)
        )

        newCoordinator(flow).upgradeTowerToArchetype(tower, arch)

        val s = flow.value
        assertEquals(2, tower.tier)
        assertEquals(0, s.scrap)
        assertEquals("t1", s.selectedTower?.id)
        assertEquals("t1", s.lastSelectedTowerId)
    }

    @Test
    fun `upgradeTowerToTier3Global does NOT auto-close at Tier 3`() {
        val level = MapGenerator.generate(seed = 42L)
        val tile = level.tiles.first { it.isBuildable }
        val arch = TowerUpgradeManager.getTier2ArchetypesForClass(TowerClass.KINETIC_BALLISTIC, TowerVector.OFFENSIVE).first()
        val tower = TowerInstance(
            id = "t1", gridPos = tile.pos, tier = 2, vector = TowerVector.OFFENSIVE, archetype = arch, investedScrap = 400
        )
        val cost = TowerUpgradeManager.getTier3GlobalCost(tower, listOf(tower))
        val flow = MutableStateFlow(
            GameUIState(level = level, towers = listOf(tower), selectedTower = tower, selectedTile = tile, scrap = cost)
        )

        newCoordinator(flow).upgradeTowerToTier3Global(tower, 0)

        val s = flow.value
        assertTrue(tower.isTier3)
        assertEquals("t1", s.selectedTower?.id)
        assertEquals("t1", s.lastSelectedTowerId)
    }

    @Test
    fun `manual selectTile opens squircles on an existing tower even with zero scrap`() {
        val level = MapGenerator.generate(seed = 42L)
        val tile = level.tiles.first { it.isBuildable }
        val tower = TowerInstance(id = "t1", gridPos = tile.pos, tier = 0)
        val flow = MutableStateFlow(GameUIState(level = level, towers = listOf(tower), scrap = 0, hasClickedFirstTile = true))
        val coordinator = newCoordinator(flow)

        coordinator.selectTile(tile)

        assertEquals("t1", flow.value.selectedTower?.id)
        assertEquals("t1", flow.value.lastSelectedTowerId)
    }

    @Test
    fun `lastSelectedTowerId persists across close and keeps Tier 3 synergy tower until another is selected or sold`() {
        val level = MapGenerator.generate(seed = 42L)
        val buildable = level.tiles.filter { it.isBuildable }
        val tileA = buildable[0]
        val tileB = buildable[1]
        val towerA = TowerInstance(id = "A", gridPos = tileA.pos, tier = 3, isTier3GlobalActive = true, investedScrap = 500)
        val towerB = TowerInstance(id = "B", gridPos = tileB.pos, tier = 0, investedScrap = 50)
        val flow = MutableStateFlow(
            GameUIState(level = level, towers = listOf(towerA, towerB), scrap = 0, hasClickedFirstTile = true)
        )
        val coordinator = newCoordinator(flow)

        coordinator.selectTile(tileA)
        assertEquals("A", flow.value.lastSelectedTowerId)
        assertEquals("A", resolveTier3SynergyTower(flow.value)?.id)

        // Back arrow / lane tap / empty-tile tap close the squircle but keep the Tier-3 synergy source
        coordinator.selectTile(null)
        assertNull(flow.value.selectedTower)
        assertEquals("A", flow.value.lastSelectedTowerId)
        assertEquals("A", resolveTier3SynergyTower(flow.value)?.id)

        // Double-tap closes but preserves
        coordinator.selectTile(tileA)
        coordinator.selectTile(tileA)
        assertNull(flow.value.selectedTower)
        assertEquals("A", flow.value.lastSelectedTowerId)
        assertEquals("A", resolveTier3SynergyTower(flow.value)?.id)

        // Selecting a non-max tower hides the Tier-3 lines
        coordinator.selectTile(tileB)
        assertEquals("B", flow.value.lastSelectedTowerId)
        assertNull(resolveTier3SynergyTower(flow.value))
        coordinator.selectTile(null)
        assertEquals("B", flow.value.lastSelectedTowerId)
        assertNull(resolveTier3SynergyTower(flow.value))

        // Re-select A then sell it -> id cleared
        coordinator.selectTile(tileA)
        assertEquals("A", flow.value.lastSelectedTowerId)
        coordinator.sellSelectedTower()
        assertNull(flow.value.lastSelectedTowerId)
        assertNull(resolveTier3SynergyTower(flow.value))
    }

    @Test
    fun `stale lastSelectedTowerId resolves gracefully to null`() {
        val state = GameUIState(towers = emptyList(), lastSelectedTowerId = "teleported_or_sacrificed")
        assertNull(resolveTier3SynergyTower(state))
    }

    @Test
    fun `startNewRun clears lastSelectedTowerId`() {
        withClearedViewModel { vm ->
            val tile = vm.uiState.value.level!!.tiles.first { it.isBuildable }
            vm.buildTower(tile, TowerClass.KINETIC_BALLISTIC)
            assertTrue("tower must have been built", vm.uiState.value.towers.isNotEmpty())
            assertNotNull(vm.uiState.value.lastSelectedTowerId)

            vm.startNewRun()

            assertNull(vm.uiState.value.lastSelectedTowerId)
            assertTrue(vm.uiState.value.towers.isEmpty())
        }
    }
}
