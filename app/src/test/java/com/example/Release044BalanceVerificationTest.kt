package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.engine.HeadlessBalanceSimulator
import com.example.engine.MapGenerator
import com.example.engine.TelemetryManager
import com.example.engine.TowerUpgradeManager
import com.example.model.*
import com.example.ui.GameUIState
import com.example.ui.hud.FloatingSquircleUpgradeBar
import com.example.ui.hud.MinimalTopHud
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h840dp")
class Release044BalanceVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // -------------------------------------------------------------------------
    // 1A. Unified Split-Action Wave Capsule (AUTO + BEGIN)
    // -------------------------------------------------------------------------

    @Test
    fun `HudWaveControlCapsule combines AUTO and BEGIN inside a single capsule and fires both callbacks`() {
        var autoToggled = 0
        var waveDeployed = 0

        composeRule.setContent {
            MinimalTopHud(
                state = GameUIState(
                    isWaveActive = false,
                    autoCallNextWave = false,
                    waveNumber = 1
                ),
                onDeployWave = { waveDeployed++ },
                onToggleAutoWave = { autoToggled++ }
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("hud_wave_control_capsule").assertIsDisplayed()
        composeRule.onNodeWithTag("auto_wave_toggle").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("deploy_wave_button").assertIsDisplayed().performClick()

        assertEquals("Tapping AUTO segment must toggle auto-wave", 1, autoToggled)
        assertEquals("Tapping BEGIN segment must deploy wave", 1, waveDeployed)
    }

    // -------------------------------------------------------------------------
    // 1B & 1C. Left/Right Flanked Target & Sell + Centered Squircles + Aura Pips
    // -------------------------------------------------------------------------

    @Test
    fun `FloatingSquircleUpgradeBar flanks Target left and Sell right with centered upgrade squircles and AuraPipBadge`() {
        val level = MapGenerator.generate(42L)
        val pad = level.tiles.first { it.isBuildable }
        val selectedTier1Tower = TowerInstance(
            id = "t_sel",
            type = TowerType.GATLING,
            gridPos = pad.pos,
            towerClass = TowerClass.KINETIC_BALLISTIC,
            tier = 1,
            vector = TowerVector.OFFENSIVE,
            investedScrap = 152
        )
        val activeBoardTower = TowerInstance(
            id = "t_board",
            type = TowerType.GATLING,
            gridPos = GridPos(pad.pos.x + 1, pad.pos.y),
            towerClass = TowerClass.KINETIC_BALLISTIC,
            tier = 3,
            archetype = TowerArchetype.VULCAN_GATLING,
            selectedAuraIndex = 0
        )
        val capstoneTower = TowerInstance(
            id = "t_cap",
            type = TowerType.CRYO,
            gridPos = GridPos(pad.pos.x + 2, pad.pos.y),
            towerClass = TowerClass.ELEMENTAL_VOID,
            tier = 3,
            archetype = TowerArchetype.FROST_SPIRE,
            isCapstone = true,
            generation = 1,
            synergyVault = mutableListOf(
                BankedSynergyVault(
                    generation = 1,
                    synergies = listOf(
                        GlobalAuraSynergy(
                            auraName = TowerArchetype.VULCAN_GATLING.primaryAura.name,
                            effectDescription = "+15% Rate",
                            sourceCount = 2,
                            iconEmoji = "⚔️",
                            color = 0xFF00F0FF
                        )
                    )
                )
            )
        )
        val allTowers = listOf(selectedTier1Tower, activeBoardTower, capstoneTower)

        composeRule.setContent {
            FloatingSquircleUpgradeBar(
                state = GameUIState(
                    level = level,
                    scrap = 1500,
                    towers = allTowers,
                    selectedTower = selectedTier1Tower,
                    activeBoardSynergies = TowerUpgradeManager.computeBoardSynergies(allTowers)
                )
            )
        }
        composeRule.waitForIdle()

        val prioBounds = composeRule.onNodeWithTag("target_priority_button").fetchSemanticsNode().boundsInRoot
        val sellBounds = composeRule.onNodeWithTag("sell_tower_button").fetchSemanticsNode().boundsInRoot
        val vulcanBounds = composeRule.onNodeWithTag("upgrade_archetype_${TowerArchetype.VULCAN_GATLING.archetypeId}").fetchSemanticsNode().boundsInRoot

        assertTrue("Target Priority must be anchored to the left of upgrade squircles", prioBounds.right <= vulcanBounds.left + 2f)
        assertTrue("Sell button must be anchored to the right of upgrade squircles", sellBounds.left >= vulcanBounds.right - 2f)
        composeRule.onNodeWithTag("aura_pip_arch_${TowerArchetype.VULCAN_GATLING.archetypeId}", useUnmergedTree = true).assertExists()
    }

    // -------------------------------------------------------------------------
    // 2A. 10x21 Map Height & SharedPreferences Migration
    // -------------------------------------------------------------------------

    @Test
    fun `Default map height is 10x21 and legacy 20-row preference migrates cleanly without DEV TUNED flag`() {
        assertEquals(10, MapGenerator.DEFAULT_WIDTH)
        assertEquals(21, MapGenerator.DEFAULT_HEIGHT)
        assertEquals(21, DevBalanceConfig().mapRows)

        val level = MapGenerator.generate(seed = 777L)
        assertEquals(10, level.width)
        assertEquals(21, level.height)
        assertEquals(210, level.tiles.size)

        // Simulate legacy user install with cfg_map_rows = 20 and schema_version_map_rows = 0
        val prefs = context.getSharedPreferences("rogue_td_telemetry", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("cfg_map_rows", 20)
            .putInt("schema_version_map_rows", 0)
            .commit()

        val tm = TelemetryManager(context)
        val loadedConfig = tm.getBalanceConfig()
        assertEquals("Legacy 20-row preference must auto-migrate to 21", 21, loadedConfig.mapRows)
        assertFalse("Migrated default config must NOT be flagged as DEV TUNED", loadedConfig.isGameplayModified())
    }

    // -------------------------------------------------------------------------
    // 2B & 3B. Headless Balance Simulator — 1 / 2-3 / 4-5 Collapse Benchmarks
    // -------------------------------------------------------------------------

    @Test
    fun `HeadlessBalanceSimulator verifies Easy 1 collapse, Medium 2 to 3 collapses, and Hard 4 to 5 collapses`() {
        val easyReport = HeadlessBalanceSimulator.runSimulation(
            difficulty = GameDifficulty.EASY,
            targetWave = 30,
            seed = 42L
        )
        val mediumReport = HeadlessBalanceSimulator.runSimulation(
            difficulty = GameDifficulty.MEDIUM,
            targetWave = 50,
            seed = 42L
        )
        val hardReport = HeadlessBalanceSimulator.runSimulation(
            difficulty = GameDifficulty.HARD,
            targetWave = 63,
            seed = 42L
        )

        assertEquals(
            "EASY (30w) must achieve exactly 1 Singularity Collapse (report=$easyReport)",
            1,
            easyReport.totalCollapses
        )
        assertTrue(
            "MEDIUM (50w) must achieve 2 to 3 Singularity Collapses (report=$mediumReport)",
            mediumReport.totalCollapses in 2..3
        )
        assertTrue(
            "HARD (63w) must achieve 4 to 5 Singularity Collapses (report=$hardReport)",
            hardReport.totalCollapses in 4..5
        )
        assertTrue("Unique auras banked must be tracked on Hard run", hardReport.uniqueAurasBanked >= 4)
        assertTrue("Unspent gold at end must be non-negative", hardReport.unspentGoldAtEnd >= 0)
    }

    // -------------------------------------------------------------------------
    // 3A. End-of-Run Telemetry Persistence & Dialog Readout
    // -------------------------------------------------------------------------

    @Test
    fun `RunRecord persists Total Collapses, Unspent Gold at End, and Unique Auras Banked and displays in RunSummaryDialog`() {
        val tm = TelemetryManager(context, analytics = null)
        val record = RunRecord(
            id = "run_044_test",
            seed = 42L,
            sectorName = "Sector Zero",
            playerName = "Tessa",
            wavesCleared = 63,
            enemiesKilled = 1850,
            totalDamage = 950_000f,
            scrapEarned = 48_200,
            ascendedTowersCount = 4,
            mvpTower = "Vulcan Gatling",
            isVictory = true,
            timestamp = 1_700_000_000_000L,
            isDevTuned = false,
            totalCollapses = 4,
            unspentGoldAtEnd = 640,
            uniqueAurasBanked = 9,
            fatalEnemyType = "None",
            collapseWaves = listOf(14, 28, 43, 58)
        )
        tm.saveLastRun(record)
        val restored = tm.getLastRun()
        assertNotNull(restored)
        assertEquals(4, restored!!.totalCollapses)
        assertEquals(640, restored.unspentGoldAtEnd)
        assertEquals(9, restored.uniqueAurasBanked)
        assertEquals(listOf(14, 28, 43, 58), restored.collapseWaves)

        composeRule.setContent {
            com.example.ui.dialog.RunSummaryStatsCard(
                state = GameUIState(
                    isVictory = true,
                    lastRunRecord = restored,
                    scrap = 640
                )
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Total Collapses").assertIsDisplayed()
        composeRule.onNodeWithText("Unspent Gold at End").assertIsDisplayed()
        composeRule.onNodeWithText("640 c").assertIsDisplayed()
        composeRule.onNodeWithText("Unique Auras Banked").assertIsDisplayed()
        composeRule.onNodeWithText("9").assertIsDisplayed()
    }
}
