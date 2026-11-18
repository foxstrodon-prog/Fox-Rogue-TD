package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.GridPos
import com.example.model.TowerArchetype
import com.example.model.TowerInstance
import com.example.model.TowerType
import com.example.ui.GameUIState
import com.example.ui.hud.FloatingSquircleUpgradeBar
import com.example.ui.hud.MinimalTopHud
import com.example.ui.hud.components.EdgeBulletTipTab
import com.example.ui.hud.components.SingularityWarningDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h900dp")
class BulletHudModernization032Test {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `MinimalTopHud renders compact AUTO toggle, BEGIN button, and battery cell in portrait and 90-deg rotated modes`() {
        var autoToggles = 0
        var deployClicks = 0

        val statePortrait = GameUIState(
            waveNumber = 2,
            coreHp = 30,
            scrap = 500,
            autoCallNextWave = true,
            isWaveActive = false,
            deviceRotationAngle = 0f
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MinimalTopHud(
                state = statePortrait,
                onDeployWave = { deployClicks++ },
                onToggleAutoWave = { autoToggles++ }
            )
        }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithText("AUTO").assertIsDisplayed()
        composeRule.onNodeWithText("AUTO: ON").assertDoesNotExist()
        composeRule.onNodeWithTag("auto_wave_toggle").performClick()
        composeRule.onNodeWithTag("deploy_wave_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("hud_battery_widget").assertIsDisplayed()
        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(1, autoToggles)
        assertEquals(1, deployClicks)
    }

    @Test
    fun `MinimalTopHud counter-rotates cleanly at 90 and 270 degrees without clipping active widgets`() {
        var pauseClicks = 0
        val rotatedState = GameUIState(
            waveNumber = 7,
            coreHp = 24,
            scrap = 1250,
            autoCallNextWave = false,
            isWaveActive = true,
            activeEnemyCount = 14,
            deviceRotationAngle = 270f
        )

        composeRule.setContent {
            MinimalTopHud(
                state = rotatedState,
                onDeployWave = {},
                onToggleAutoWave = {},
                onOpenPauseMenu = { pauseClicks++ }
            )
        }

        composeRule.onNodeWithTag("hud_pause_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("AUTO").assertIsDisplayed()
        composeRule.onNodeWithTag("deploy_wave_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_battery_widget").assertIsDisplayed()
        assertEquals(1, pauseClicks)
    }

    @Test
    fun `SingularityWarningDialog renders dark-glass telemetry across rotation and triggers confirm`() {
        var confirmed = 0
        val anchor = TowerInstance(
            id = "t1",
            type = TowerType.GATLING,
            gridPos = GridPos(1, 1),
            tier = 3,
            archetype = TowerArchetype.VULCAN_GATLING
        )
        val sacrificed = TowerInstance(
            id = "t2",
            type = TowerType.MORTAR,
            gridPos = GridPos(2, 2),
            tier = 3,
            archetype = TowerArchetype.HELLFIRE_MORTAR
        )
        val state = GameUIState(
            isSingularityWarningOpen = true,
            towers = listOf(anchor, sacrificed),
            selectedTower = anchor,
            deviceRotationAngle = 90f
        )

        composeRule.setContent {
            SingularityWarningDialog(
                state = state,
                onConfirm = { confirmed++ },
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("SYNERGY SINGULARITY").assertIsDisplayed()
        composeRule.onNodeWithText("1 TURRETS").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_singularity_button").assertIsDisplayed().performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun `EdgeBulletTipTab preserves 48dp touch target and click callback`() {
        var clicks = 0
        composeRule.setContent {
            EdgeBulletTipTab(
                isLeftEdge = true,
                icon = Icons.Filled.ScreenRotation,
                contentDescription = "Rotate Screen",
                tag = "manual_rotate_button_left",
                onClick = { clicks++ }
            )
        }

        composeRule.onNodeWithTag("manual_rotate_button_left").assertIsDisplayed().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `FloatingSquircleUpgradeBar wires target priority and sell controls`() {
        var prioChanges = 0
        var sellClicks = 0
        val tower = TowerInstance(
            id = "t1",
            type = TowerType.GATLING,
            gridPos = GridPos(1, 1),
            tier = 0
        )

        composeRule.setContent {
            FloatingSquircleUpgradeBar(
                state = GameUIState(scrap = 500, selectedTower = tower),
                onChangePriority = { prioChanges++ },
                onSellTower = { sellClicks++ }
            )
        }

        composeRule.onNodeWithTag("target_priority_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("sell_tower_button").assertIsDisplayed().performClick()
        assertEquals(1, prioChanges)
        assertEquals(1, sellClicks)
    }
}
