package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import com.example.engine.AppUpdateState
import com.example.engine.GeneratedLevel
import com.example.engine.TowerUpgradeManager
import com.example.engine.UpdateDownloadStatus
import com.example.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RogueTDScreen(
    viewModel: RogueTDViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B13))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // === FULL SCREEN MAP CANVAS (Static locked map coordinate system) ===
        Box(modifier = Modifier.fillMaxSize()) {
            GameCanvas(
                state = state,
                onTileClicked = { viewModel.selectTile(it) }
            )

            // Floating Combat Text Overlay
            FloatingTextOverlay(texts = state.floatingTexts, level = state.level)
        }

        // === MINIMAL FLOATING TOP TACTICAL HUD (Adaptive edge docking) ===
        MinimalTopHud(
            state = state,
            onDeployWave = { viewModel.deployWave() },
            onToggleAutoWave = { viewModel.toggleAutoWave() },
            onOpenPauseMenu = { viewModel.openPauseMenu() },
            modifier = if (isLandscape) {
                Modifier
                    .align(Alignment.TopStart)
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            }
        )

        // === IN-APP GITHUB APK AUTO-UPDATER BANNER ===
        if (state.updateUIState.status in listOf(UpdateDownloadStatus.AVAILABLE, UpdateDownloadStatus.DOWNLOADING, UpdateDownloadStatus.READY_TO_INSTALL)) {
            UpdateBanner(
                updateState = state.updateUIState,
                onDownloadAndInstall = { viewModel.downloadAndInstallUpdate() },
                modifier = Modifier
                    .align(if (isLandscape) Alignment.BottomStart else Alignment.TopCenter)
                    .padding(if (isLandscape) PaddingValues(start = 12.dp, bottom = 12.dp) else PaddingValues(top = 46.dp))
            )
        }

        // === MINIMAL FLOATING BOTTOM COMMAND DOCK / SIDEBAR (Adaptive rotation transition) ===
        MinimalBottomDock(
            state = state,
            onDeployWave = { viewModel.deployWave() },
            onToggleAutoWave = { viewModel.toggleAutoWave() },
            onBuildTower = { tile, type -> viewModel.buildTower(tile, type) },
            onBuildTowerClass = { tile, cls -> viewModel.buildTower(tile, cls) },
            onUpgradeToVector = { tower, vec -> viewModel.upgradeTowerToVector(tower, vec) },
            onUpgradeToArchetype = { tower, arch -> viewModel.upgradeTowerToArchetype(tower, arch) },
            onUpgradeToTier3Global = { tower, auraIdx -> viewModel.upgradeTowerToTier3Global(tower, auraIdx) },
            onUpgradeToTier1 = { tower, branch -> viewModel.upgradeTowerToTier1(tower, branch) },
            onUpgradeToFinalForm = { tower, code -> viewModel.upgradeTowerToFinalForm(tower, code) },
            onUpgradePath = { tower, pathIdx -> viewModel.upgradeTowerPath(tower, pathIdx) },
            onAscendTower = { viewModel.ascendTower(it) },
            onSellTower = { viewModel.sellSelectedTower() },
            onChangePriority = { viewModel.changeSelectedTowerPriority(it) },
            onDeselect = { viewModel.selectTile(null) },
            modifier = if (isLandscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(340.dp)
                    .fillMaxHeight()
                    .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            }
        )

        // === ROUND 1 TACTICAL HINT (Floating non-blocking banner at top) ===
        if (!state.hasClickedFirstTile && state.waveNumber == 1 && state.towers.isEmpty()) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.88f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp)
                    .clickable { viewModel.dismissFirstTileHint() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SELECT TILE TO BUILD (Tap highlighted boosted pad)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // === EXCESS CASH REMINDER BANNER ===
        if (state.scrap >= 350 && state.hasClickedFirstTile && state.selectedTower == null && state.selectedTile == null) {
            Surface(
                color = Color(0xFF161F30).copy(alpha = 0.90f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Diamond, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Surplus Credits Available (${state.scrap} c): Build or upgrade defense towers!",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // === GEAR / PAUSE MENU MODAL ===
        if (state.isPauseMenuOpen) {
            PauseMenuDialog(
                state = state,
                onSpeedChanged = { viewModel.setGameSpeed(it) },
                onOpenTelemetry = {
                    viewModel.closePauseMenu()
                    viewModel.toggleTelemetryView(true)
                },
                onOpenSettings = {
                    viewModel.closePauseMenu()
                    viewModel.toggleSettingsView(true)
                },
                onRestart = {
                    viewModel.closePauseMenu()
                    viewModel.startNewRun()
                },
                onEndRun = {
                    viewModel.closePauseMenu()
                    viewModel.requestEndRun()
                },
                onCheckForUpdates = { viewModel.checkForAppUpdates() },
                onDownloadAndInstallUpdate = { viewModel.downloadAndInstallUpdate() },
                onResume = { viewModel.closePauseMenu() }
            )
        }

        // === END RUN CONFIRMATION MODAL ===
        if (state.showEndRunConfirmDialog) {
            EndRunConfirmDialog(
                state = state,
                onConfirmEnd = { viewModel.confirmEndRun() },
                onDismiss = { viewModel.dismissEndRunConfirm() }
            )
        }

        // === RUN COMPLETE / GAME OVER MODAL ===
        if (state.isGameOver || state.isVictory) {
            RunSummaryDialog(
                state = state,
                onRestart = { viewModel.startNewRun() },
                onViewTelemetry = { viewModel.toggleTelemetryView(true) },
                onSetPlayerName = { viewModel.setPlayerName(it) }
            )
        }

        // === TELEMETRY & LEADERBOARD MODAL ===
        if (state.isTelemetryOpen) {
            TelemetryLeaderboardDialog(
                state = state,
                onSetPlayerName = { viewModel.setPlayerName(it) },
                onRetryLeaderboard = { viewModel.fetchOnlineLeaderboard(true) },
                onDismiss = { viewModel.toggleTelemetryView(false) }
            )
        }

        // === DEV BALANCE & SETTINGS MODAL ===
        if (state.isSettingsOpen) {
            DevBalanceSettingsDialog(
                config = state.balanceConfig,
                towerPerformance = state.towerPerformance,
                onConfigChanged = { viewModel.updateBalanceConfig(it) },
                onResetDefaults = { viewModel.resetBalanceConfigToDefaults() },
                onDismiss = { viewModel.toggleSettingsView(false) }
            )
        }
    }
}

@Composable
fun MinimalTopHud(
    state: GameUIState,
    onDeployWave: () -> Unit,
    onToggleAutoWave: () -> Unit,
    onOpenPauseMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSynergyDropdown by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF070B14).copy(alpha = 0.65f), // High transparency so background map remains visible underneath
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wave badge
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (state.currentWaveData?.isBossWave == true) Color(0xFFFF2A55) else Color(0xFF00F0FF))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (state.currentWaveData?.isBossWave == true) "B-${state.waveNumber}" else "W-${state.waveNumber}",
                        color = if (state.currentWaveData?.isBossWave == true) Color(0xFFFF2A55) else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                // Core HP (Heart Icon)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Core HP",
                            tint = if (state.coreHp <= 8) Color(0xFFFF2A55) else Color(0xFFFF2A55),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${state.coreHp}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Credits
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Diamond,
                            contentDescription = "Credits",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${state.scrap}",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Consolidated Synergies Indicator with Dropdown (Symbol + count)
                val activeSynergiesCount = state.activeGlobalSynergies.size
                Box {
                    Surface(
                        color = if (activeSynergiesCount > 0) Color(0xFFFFB800).copy(alpha = 0.20f) else Color(0xFF161F30).copy(alpha = 0.60f),
                        shape = RoundedCornerShape(5.dp),
                        border = if (activeSynergiesCount > 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.6f)) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .clickable { showSynergyDropdown = !showSynergyDropdown }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🌟", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "($activeSynergiesCount)",
                                color = if (activeSynergiesCount > 0) Color(0xFFFFB800) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Lightweight Dropdown Menu showing full details
                    DropdownMenu(
                        expanded = showSynergyDropdown,
                        onDismissRequest = { showSynergyDropdown = false },
                        modifier = Modifier
                            .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                            .border(1.dp, Color(0xFFFFB800), RoundedCornerShape(6.dp))
                            .widthIn(max = 240.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "ACTIVE SYNERGIES ($activeSynergiesCount)",
                                    color = Color(0xFFFFB800),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = { },
                            enabled = false
                        )
                        if (state.activeGlobalSynergies.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "No active aura synergies.\nAscend towers to Tier 3 to project global buffs across the battlefield.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp
                                    )
                                },
                                onClick = { showSynergyDropdown = false }
                            )
                        } else {
                            state.activeGlobalSynergies.forEach { syn ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = syn.iconEmoji, fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${syn.auraName} (${syn.sourceCount}x)",
                                                    color = Color(syn.color),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Text(
                                                text = syn.effectDescription,
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 8.5.sp,
                                                lineHeight = 11.sp
                                            )
                                        }
                                    },
                                    onClick = { showSynergyDropdown = false }
                                )
                            }
                        }
                    }
                }

                // Auto Wave Toggle
                Surface(
                    color = if (state.autoCallNextWave) Color(0xFF00FF9D).copy(alpha = 0.2f) else Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(5.dp),
                    border = if (state.autoCallNextWave) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF9D)) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .clickable { onToggleAutoWave() }
                        .padding(1.dp)
                ) {
                    Text(
                        text = if (state.autoCallNextWave) "AUTO" else "AUTO",
                        color = if (state.autoCallNextWave) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Black,
                        fontSize = 8.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Launch Wave Button
                Button(
                    onClick = onDeployWave,
                    enabled = !state.isWaveActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00F0FF),
                        disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.7f),
                        contentColor = Color.Black,
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 1.dp),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .height(26.dp)
                        .testTag("deploy_wave_button")
                ) {
                    Icon(
                        imageVector = if (state.isWaveActive) Icons.Filled.FastForward else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (state.isWaveActive) "${state.enemies.size} FOES" else "LAUNCH",
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }

                // Gear Cog Button (Opens Pause Menu & Settings)
                IconButton(
                    onClick = onOpenPauseMenu,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF161F30).copy(alpha = 0.70f))
                        .testTag("pause_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Game Settings & Controls",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Harmonic Grid Status Sub-strip
            if (state.harmonicGridActive || state.harmonicPadsCount > 0) {
                Spacer(modifier = Modifier.height(1.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.harmonicGridActive) "⚡ HARMONIC GRID ACTIVE: 6/6 (+20% DMG / +25% SCRAP)" else "BOOSTED PADS ACTIVE: ${state.harmonicPadsCount}/6",
                        color = if (state.harmonicGridActive) Color(0xFFFFD700) else Color(0xFF00FF9D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalBottomDock(
    state: GameUIState,
    onDeployWave: () -> Unit,
    onToggleAutoWave: () -> Unit,
    onBuildTower: (MapTile, TowerType) -> Unit,
    onBuildTowerClass: (MapTile, TowerClass) -> Unit = { _, _ -> },
    onUpgradeToVector: (TowerInstance, TowerVector) -> Unit = { _, _ -> },
    onUpgradeToArchetype: (TowerInstance, TowerArchetype) -> Unit = { _, _ -> },
    onUpgradeToTier3Global: (TowerInstance, Int) -> Unit = { _, _ -> },
    onUpgradeToTier1: (TowerInstance, String) -> Unit = { _, _ -> },
    onUpgradeToFinalForm: (TowerInstance, String) -> Unit = { _, _ -> },
    onUpgradePath: (TowerInstance, Int) -> Unit = { _, _ -> },
    onAscendTower: (TowerInstance) -> Unit = {},
    onSellTower: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDrawerActive = state.selectedTower != null || (state.selectedTile != null && state.selectedTile.type == TileType.EMPTY)
    if (!isDrawerActive) return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    Surface(
        color = Color(0xFF0A0F1D).copy(alpha = 0.96f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .then(if (isLandscape) Modifier.fillMaxHeight().verticalScroll(scrollState) else Modifier.fillMaxWidth())
                .padding(8.dp)
        ) {
            when {
                // Placed Tower selected -> 3-Tier Hierarchy Inspector & Detailed Stat Sheet
                state.selectedTower != null -> {
                    BranchingTowerInspector(
                        tower = state.selectedTower,
                        allTowers = state.towers,
                        scrap = state.scrap,
                        activeSynergies = state.activeGlobalSynergies,
                        onUpgradeToVector = { onUpgradeToVector(state.selectedTower, it) },
                        onUpgradeToArchetype = { onUpgradeToArchetype(state.selectedTower, it) },
                        onUpgradeToTier3Global = { auraIdx -> onUpgradeToTier3Global(state.selectedTower, auraIdx) },
                        onUpgradeToTier1 = { onUpgradeToTier1(state.selectedTower, it) },
                        onUpgradeToFinalForm = { onUpgradeToFinalForm(state.selectedTower, it) },
                        onSell = onSellTower,
                        onChangePriority = onChangePriority,
                        onClose = onDeselect
                    )
                }

                // Empty Buildable Tile selected -> Deploy 3-Class Tower Drawer
                state.selectedTile != null && state.selectedTile.type == TileType.EMPTY -> {
                    BuildTowerDrawer(
                        tile = state.selectedTile,
                        allTowers = state.towers,
                        scrap = state.scrap,
                        onBuildClass = { onBuildTowerClass(state.selectedTile, it) },
                        onBuild = { onBuildTower(state.selectedTile, it) },
                        onClose = onDeselect
                    )
                }
            }
        }
    }
}

@Composable
fun BuildTowerDrawer(
    tile: MapTile,
    allTowers: List<TowerInstance> = emptyList(),
    scrap: Int,
    onBuildClass: (TowerClass) -> Unit = {},
    onBuild: (TowerType) -> Unit = {},
    onClose: () -> Unit
) {
    val classes = TowerUpgradeManager.getTowerClasses()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DEPLOY DEFENSE TOWER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
                if (tile.buff != TileBuff.NONE) {
                    Text(
                        text = "TERRAIN BONUS: ${tile.buff.label} (${tile.buff.desc})",
                        color = Color(tile.buff.badgeColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            classes.forEach { towerClass ->
                val isUnlocked = TowerUpgradeManager.isClassUnlocked(towerClass, allTowers)
                val cost = towerClass.baseCost
                val canAfford = scrap >= cost
                val isEnabled = isUnlocked && canAfford
                val themeColor = Color(towerClass.color)

                val unlockCondition = when (towerClass) {
                    TowerClass.KINETIC_BALLISTIC -> "Unlocked"
                    TowerClass.ELEMENTAL_VOID -> "Unlock: 1 Tier 3"
                    TowerClass.HEAVY_SIEGE -> "Unlock: 2 Tier 3"
                }

                Card(
                    onClick = {
                        if (isEnabled) {
                            onBuildClass(towerClass)
                            if (towerClass == TowerClass.KINETIC_BALLISTIC) {
                                onBuild(TowerType.GATLING)
                            }
                        }
                    },
                    enabled = isEnabled,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) Color(0xFF131F33) else Color(0xFF0C1322),
                        disabledContainerColor = Color(0xFF0C1322)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(if (towerClass == TowerClass.KINETIC_BALLISTIC) "build_basic_turret" else "build_class_${towerClass.name}")
                        .border(
                            1.dp,
                            if (isEnabled) themeColor.copy(alpha = 0.8f) else Color(0xFF1E293B),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = towerClass.shapeSymbol,
                                    color = Color(towerClass.color),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = towerClass.displayName,
                                    color = if (isUnlocked) (if (canAfford) Color.White else Color(0xFF94A3B8)) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!isUnlocked) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFFFF2A55),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        val statsPreview = when (towerClass) {
                            TowerClass.KINETIC_BALLISTIC -> "DMG: 26 | SPD: 4.4x | RNG: 2.6u\n+18% Global Speed Aura"
                            TowerClass.ELEMENTAL_VOID -> "DMG: 45 | SPD: 2.8x | RNG: 3.2u\nFreezing Slow & Armor Shred"
                            TowerClass.HEAVY_SIEGE -> "DMG: 145 | SPD: 0.8x | RNG: 4.8u\nAoE Splash & Boss Buster"
                        }

                        Text(
                            text = if (!isUnlocked) unlockCondition else statsPreview,
                            color = if (!isUnlocked) Color(0xFFFF2A55) else Color(0xFF00FF9D),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Diamond,
                                    contentDescription = null,
                                    tint = if (canAfford && isUnlocked) Color(0xFFFFD700) else Color(0xFF64748B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$cost c",
                                    color = if (canAfford && isUnlocked) Color(0xFFFFD700) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = if (!isUnlocked) "LOCKED" else if (canAfford) "BUILD" else "NEED C",
                                color = if (isEnabled) themeColor else Color(0xFF64748B),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BranchingTowerInspector(
    tower: TowerInstance,
    allTowers: List<TowerInstance>,
    scrap: Int,
    activeSynergies: List<GlobalAuraSynergy> = emptyList(),
    onUpgradeToVector: (TowerVector) -> Unit = {},
    onUpgradeToArchetype: (TowerArchetype) -> Unit = {},
    onUpgradeToTier3Global: (Int) -> Unit = {},
    onUpgradeToTier1: (String) -> Unit = {},
    onUpgradeToFinalForm: (String) -> Unit = {},
    onSell: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onClose: () -> Unit
) {
    val sellValue = tower.getSellValue()
    val arch = tower.archetype
    val baseDamage = arch?.baseDamage ?: tower.type.baseDamage
    val baseFireRate = arch?.baseFireRate ?: tower.type.baseFireRate
    val baseRange = arch?.baseRange ?: tower.type.baseRange

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (tower.isTier3) Color(0xFFFFB800) else Color(arch?.color ?: tower.towerClass.color))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tower.towerClass.shapeSymbol,
                            color = Color(tower.towerClass.color),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tower.getDisplayName(),
                            color = if (tower.isTier3) Color(0xFFFFB800) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = when (tower.tier) {
                            3 -> "TIER 3 [${tower.towerClass.shapeName}]: ${tower.activeAura?.name ?: "GLOBAL AURA"} ONLINE"
                            2 -> "TIER 2 [${tower.towerClass.shapeName}]: SPECIALIZED ARCHETYPE LOCKED"
                            1 -> "TIER 1 [${tower.towerClass.shapeName}]: ${tower.vector?.displayName ?: "VECTOR"} SPECIALIZED"
                            else -> "TIER 0 [${tower.towerClass.shapeName}]: ${tower.towerClass.displayName} (SELECT VECTOR)"
                        },
                        color = when (tower.tier) {
                            3 -> Color(0xFFFFB800)
                            2 -> Color(0xFF00F0FF)
                            1 -> Color(0xFF00FF9D)
                            else -> Color(0xFF94A3B8)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TargetPriority.values().forEach { prio ->
                    val isSel = tower.targetPriority == prio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) Color(0xFF00F0FF) else Color(0xFF1E293B))
                            .clickable { onChangePriority(prio) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = prio.label,
                            color = if (isSel) Color.Black else Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- DETAILED STAT SHEET (3 Sections: Base Stats, Local Upgrades, Incoming Global Multipliers) ---
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.85f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section 1: Base Stats
                Column {
                    Text(text = "BASE STATS", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "DMG: ${baseDamage.toInt()} | SPD: ${baseFireRate}x | RNG: ${baseRange}u", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
                // Section 2: Local Upgrades
                Column {
                    Text(text = "LOCAL TIER", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "Tier ${tower.tier}/3 (${tower.vector?.displayName ?: "Base"})", color = Color(0xFF00FF9D), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
                // Section 3: Incoming Global Buffs
                Column {
                    Text(text = "GLOBAL AURAS", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "${activeSynergies.size} Active Multipliers", color = Color(0xFFFFB800), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // --- TIER PROGRESSION SELECTION CARDS ---
        when (tower.tier) {
            // TIER 0: Pick 1 of 3 Vectors (Offensive, Defensive, Support) for this class
            0 -> {
                val vectors = TowerUpgradeManager.getTier1Vectors()
                val cost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
                val canAfford = scrap >= cost

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vectors.forEach { vec ->
                        val statBoost = when (vec) {
                            TowerVector.OFFENSIVE -> "+45% DMG  |  +15% CRIT  |  Heavy Penetration"
                            TowerVector.DEFENSIVE -> "+35% RNG  |  +20% Slow  |  Pushback & Stun"
                            TowerVector.SUPPORT -> "+40% SPD  |  +25% SCRAP |  Range Uplink Aura"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (canAfford) Color(vec.color).copy(alpha = 0.8f) else Color(0xFF26334D), RoundedCornerShape(8.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(7.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = vec.displayName.uppercase(),
                                        color = Color(vec.color),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${cost} c",
                                        color = if (canAfford) Color(0xFFFFD700) else Color(0xFF64748B),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = statBoost,
                                    color = Color(0xFF00FF9D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    lineHeight = 11.sp,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { if (canAfford) onUpgradeToVector(vec) },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(vec.color),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .testTag("upgrade_vector_${vec.idName}")
                                ) {
                                    Text("SELECT ${vec.displayName.uppercase()}", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // TIER 1: Pick 1 of 3 Archetypes for the selected Vector and Tower Class
            1 -> {
                val vec = tower.vector ?: TowerVector.OFFENSIVE
                val archetypes = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, vec)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    archetypes.forEach { archetype ->
                        val cost = TowerUpgradeManager.getTier2Cost(archetype)
                        val canAfford = scrap >= cost

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (canAfford) Color(archetype.color).copy(alpha = 0.8f) else Color(0xFF26334D), RoundedCornerShape(8.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(7.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = archetype.displayName,
                                        color = Color(archetype.color),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${cost} c",
                                        color = if (canAfford) Color(0xFFFFD700) else Color(0xFF64748B),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "DMG: ${archetype.baseDamage.toInt()} | SPD: ${archetype.baseFireRate}x | RNG: ${archetype.baseRange}u",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 11.sp
                                )

                                Surface(
                                    color = Color(archetype.color).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Auras: ${archetype.primaryAura.shortBadge} / ${archetype.secondaryAura.shortBadge}",
                                        color = Color(archetype.color),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.5.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Button(
                                    onClick = { if (canAfford) onUpgradeToArchetype(archetype) },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(archetype.color),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .testTag("upgrade_archetype_${archetype.archetypeId}")
                                ) {
                                    Text("LOCK IDENTITY", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // TIER 2: Upgrade to Tier 3 Global Aura Projection (Choose between 2 unique auras!)
            2 -> {
                val archetype = tower.archetype ?: mapTypeToArchetype(tower.type)
                val cost = TowerUpgradeManager.getTier3GlobalCost(tower, allTowers)
                val canAfford = scrap >= cost

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CHOOSE TIER 3 GLOBAL AURA PROJECTION (${cost} c)",
                        color = Color(0xFFFFB800),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Aura Option 0: Primary Aura
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (canAfford) Color(0xFFFFB800) else Color(0xFF26334D),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = archetype.primaryAura.name,
                                        color = Color(0xFFFFB800),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        color = Color(0xFFFFB800).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = archetype.primaryAura.shortBadge,
                                            color = Color(0xFFFFB800),
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = archetype.primaryAura.description,
                                    color = Color.White,
                                    fontSize = 7.5.sp,
                                    lineHeight = 9.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { if (canAfford) onUpgradeToTier3Global(0) },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFB800),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .testTag("upgrade_tier3_global")
                                ) {
                                    Text("ACTIVATE AURA", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        // Aura Option 1: Secondary Aura
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (canAfford) Color(0xFF00FF9D) else Color(0xFF26334D),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = archetype.secondaryAura.name,
                                        color = Color(0xFF00FF9D),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        color = Color(0xFF00FF9D).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = archetype.secondaryAura.shortBadge,
                                            color = Color(0xFF00FF9D),
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = archetype.secondaryAura.description,
                                    color = Color.White,
                                    fontSize = 7.5.sp,
                                    lineHeight = 9.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { if (canAfford) onUpgradeToTier3Global(1) },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FF9D),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .testTag("upgrade_tier3_aura_1")
                                ) {
                                    Text("ACTIVATE AURA", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // TIER 3: Apex Form Active with Selected Aura
            3 -> {
                val aura = tower.activeAura ?: (tower.archetype ?: mapTypeToArchetype(tower.type)).primaryAura
                Surface(
                    color = Color(0xFFFFB800).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌟 ${aura.name} ACTIVE",
                                color = Color(0xFFFFB800),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                            Text(
                                text = aura.description,
                                color = Color.White,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF00FF9D).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "[${aura.shortBadge}]",
                                color = Color(0xFF00FF9D),
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sell Button & Combat Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kills: ${tower.kills} | Dmg: ${tower.totalDamage.toInt()}",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp
            )

            OutlinedButton(
                onClick = onSell,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("sell_tower_button")
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("SELL (+$sellValue)", fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
            }
        }
    }
}

@Composable
fun ThreePathTowerInspector(
    tower: TowerInstance,
    allTowers: List<TowerInstance>,
    scrap: Int,
    onUpgradePath: (Int) -> Unit,
    onAscend: () -> Unit,
    onSell: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onClose: () -> Unit
) {
    BranchingTowerInspector(
        tower = tower,
        allTowers = allTowers,
        scrap = scrap,
        onUpgradeToTier1 = { branch ->
            val idx = when (branch) { "A" -> 0; "B" -> 1; else -> 2 }
            onUpgradePath(idx)
        },
        onUpgradeToFinalForm = {
            onAscend()
        },
        onSell = onSell,
        onChangePriority = onChangePriority,
        onClose = onClose
    )
}

@Composable
fun TelemetryLeaderboardDialog(
    state: GameUIState,
    onSetPlayerName: (String) -> Unit,
    onRetryLeaderboard: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = LAST RUN, 1 = MY LIFETIME, 2 = LEADERBOARD
    var editingCallsign by remember { mutableStateOf(state.activePlayerName) }
    var isEditingName by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0B132B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00F0FF).copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TACTICAL TELEMETRY & ARCHIVES",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Operative: ${state.activePlayerName} • Performance telemetry & rankings",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Three-tab selection
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF131F37),
                    contentColor = Color(0xFF00F0FF),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("LAST RUN", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("MY LIFETIME", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("GLOBAL RANKS", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    // TAB 0: LAST RUN METRICS
                    0 -> {
                        val last = state.lastRunRecord
                        if (last == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No completed combat runs recorded yet.\nDeploy and complete wave defense to log performance telemetry.",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = Color(0xFF162544),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (last.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${last.sectorName}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (last.isVictory) "VICTORY" else "DEFEAT",
                                                color = if (last.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                        Text(
                                            text = "Operative: ${last.playerName} • ${dateFormat.format(Date(last.timestamp))}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "TACTICAL SCORE: ${last.score} PTS",
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                TelemetryCard("Waves Cleared", "${last.wavesCleared}", Icons.Filled.MilitaryTech, Color(0xFF00F0FF))
                                TelemetryCard("Hostiles Annihilated", "${last.enemiesKilled}", Icons.Filled.CrisisAlert, Color(0xFFFF2A55))
                                TelemetryCard("Total Damage Inflicted", "${last.totalDamage.toInt()}", Icons.Filled.Bolt, Color(0xFFFFB800))
                                TelemetryCard("Scrap Harvested", "${last.scrapEarned} c", Icons.Filled.Diamond, Color(0xFFFFD700))
                                TelemetryCard("Most Lethal Tower (MVP)", last.mvpTower, Icons.Filled.Shield, Color(0xFFA855F7))
                            }
                        }
                    }

                    // TAB 1: MY LIFETIME METRICS
                    1 -> {
                        val lt = state.playerLifetimeTelemetry
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Operative Callsign Switcher / Manager Card
                            Surface(
                                color = Color(0xFF131F33),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ACTIVE OPERATIVE CALLSIGN",
                                            color = Color(0xFF00F0FF),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.5.sp
                                        )
                                        Text(
                                            text = "Personal Stats Isolated",
                                            color = Color(0xFF64748B),
                                            fontSize = 8.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (isEditingName) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = editingCallsign,
                                                onValueChange = { if (it.length <= 16) editingCallsign = it },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFF00F0FF),
                                                    unfocusedBorderColor = Color(0xFF334155),
                                                    focusedContainerColor = Color(0xFF0F172A),
                                                    unfocusedContainerColor = Color(0xFF0F172A)
                                                ),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(42.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    val trimmed = editingCallsign.trim()
                                                    if (trimmed.isNotEmpty()) {
                                                        onSetPlayerName(trimmed)
                                                        isEditingName = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("SET", fontWeight = FontWeight.Black, fontSize = 10.sp)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = state.activePlayerName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    editingCallsign = state.activePlayerName
                                                    isEditingName = true
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("CHANGE CALLSIGN", color = Color(0xFF00F0FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            TelemetryCard("Total Missions Flown", "${lt.totalRuns}", Icons.Filled.PlayArrow, Color(0xFF00F0FF))
                            TelemetryCard("Total Hostiles Annihilated", "${lt.totalHostilesKilled}", Icons.Filled.CrisisAlert, Color(0xFFFF2A55))
                            TelemetryCard("Total Credits Harvested", "${lt.totalScrapMined}", Icons.Filled.Diamond, Color(0xFFFFD700))
                            TelemetryCard("Longest Wave Record", "Wave ${lt.bestWaveEver}", Icons.Filled.EmojiEvents, Color(0xFF00FF9D))
                            TelemetryCard("Highest Damage MVP", lt.mostUsedTower, Icons.Filled.MilitaryTech, Color(0xFFA855F7))
                        }
                    }

                    // TAB 2: GLOBAL ONLINE LEADERBOARD (Ktor REST via Dreamlo)
                    2 -> {
                        when {
                            state.leaderboardStatus == GlobalBoardStatus.LOADING && state.onlineLeaderboard.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = Color(0xFF00F0FF),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Connecting to Global Uplink...",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            state.onlineLeaderboard.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = state.leaderboardErrorMessage ?: "No online leaderboard scores yet.\nDeploy and survive waves to earn global ranking!",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = onRetryLeaderboard,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFF00F0FF),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Retry Uplink",
                                                color = Color(0xFF00F0FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (state.leaderboardStatus == GlobalBoardStatus.OFFLINE_CACHED) {
                                        Surface(
                                            color = Color(0xFF332005),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CloudOff,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFB800),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Offline Cached Data",
                                                        color = Color(0xFFFFB800),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = "RETRY",
                                                    color = Color(0xFF00F0FF),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.clickable { onRetryLeaderboard() }
                                                )
                                            }
                                        }
                                    }

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(state.onlineLeaderboard) { index, item ->
                                            val rankDisplay = when (index) {
                                                0 -> "🥇"
                                                1 -> "🥈"
                                                2 -> "🥉"
                                                else -> "#${item.rank}"
                                            }

                                            Surface(
                                                color = Color(0xFF162544),
                                                shape = RoundedCornerShape(8.dp),
                                                border = if (index == 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)) else null,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = rankDisplay,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.width(36.dp)
                                                    )

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = item.name,
                                                                    color = Color(0xFF00F0FF),
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 12.sp
                                                                )
                                                                if (item.isDevTuned) {
                                                                    Spacer(modifier = Modifier.width(5.dp))
                                                                    Surface(
                                                                        color = Color(0xFFEAB308).copy(alpha = 0.22f),
                                                                        shape = RoundedCornerShape(3.dp),
                                                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFEAB308))
                                                                    ) {
                                                                        Text(
                                                                            text = "DEV",
                                                                            color = Color(0xFFFDE047),
                                                                            fontSize = 8.sp,
                                                                            fontWeight = FontWeight.Black,
                                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = "Wave ${item.wavesCleared}",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                            Text(
                                                                text = "${item.score} PTS",
                                                                color = Color(0xFFFFD700),
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 11.5.sp
                                                            )
                                                        }

                                                        if (item.scrapEarned > 0 || item.date.isNotBlank()) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                if (item.scrapEarned > 0) {
                                                                    Text(
                                                                        text = "${item.scrapEarned} Scrap Salvaged",
                                                                        color = Color(0xFF94A3B8),
                                                                        fontSize = 9.sp
                                                                    )
                                                                }
                                                                if (item.date.isNotBlank()) {
                                                                    Text(
                                                                        text = item.date,
                                                                        color = Color(0xFF64748B),
                                                                        fontSize = 8.5.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        color = Color(0xFF131F37),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(text = value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun FloatingTextOverlay(texts: List<FloatingText>, level: GeneratedLevel?) {
    if (level == null || texts.isEmpty()) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gridWidth = level.width
        val gridHeight = level.height
        val boardAspect = gridWidth.toFloat() / gridHeight.toFloat()
        val viewAspect = maxWidth.value / maxHeight.value

        val (boardWidthDp, boardHeightDp) = if (viewAspect < boardAspect) {
            val bw = maxWidth.value
            bw to (bw / boardAspect)
        } else {
            val bh = maxHeight.value
            (bh * boardAspect) to bh
        }

        val boardOriginXDp = (maxWidth.value - boardWidthDp) / 2f
        val boardOriginYDp = (maxHeight.value - boardHeightDp) / 2f
        val cellWDp = boardWidthDp / gridWidth
        val cellHDp = boardHeightDp / gridHeight

        for (ft in texts) {
            val alpha = (ft.lifeMs.toFloat() / ft.maxLifeMs).coerceIn(0f, 1f)
            val posX = boardOriginXDp + (ft.x + 0.5f) * cellWDp
            val posY = boardOriginYDp + (ft.y + 0.5f) * cellHDp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
                    .offset(
                        x = (posX - 18f).dp,
                        y = (posY - 12f).dp
                    )
            ) {
                Text(
                    text = ft.text,
                    color = Color(ft.color).copy(alpha = alpha),
                    fontSize = if (ft.isCrit) 14.sp else 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun UpdateBanner(
    updateState: AppUpdateState,
    onDownloadAndInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = updateState.updateInfo ?: return
    val isDownloading = updateState.status == UpdateDownloadStatus.DOWNLOADING
    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00F0FF)),
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clickable(enabled = !isDownloading) { onDownloadAndInstall() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (updateState.status) {
                UpdateDownloadStatus.DOWNLOADING -> {
                    CircularProgressIndicator(
                        progress = { updateState.progressPercent / 100f },
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF00F0FF),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Downloading v${info.latestVersion}: ${updateState.progressPercent}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                UpdateDownloadStatus.READY_TO_INSTALL -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00FF9D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Update v${info.latestVersion} Ready • Tap to Install",
                        color = Color(0xFF00FF9D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New update available: v${info.latestVersion} [Download & Install]",
                        color = Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RunSummaryDialog(
    state: GameUIState,
    onRestart: () -> Unit,
    onViewTelemetry: () -> Unit,
    onSetPlayerName: (String) -> Unit
) {
    var callsignInput by remember { mutableStateOf(state.activePlayerName) }
    var nameSaved by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { }) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (state.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state.isVictory) "DUAL SECTOR SECURED!" else "CORE BREACHED",
                    color = if (state.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )

                Text(
                    text = if (state.isVictory) "Both lanes held! Sector Zero defenses triumph." else "Dual-front hostiles overran the core nexus.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pilot Callsign Input Section
                Surface(
                    color = Color(0xFF131F33),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (nameSaved) Color(0xFF00FF9D) else Color(0xFF00F0FF).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OPERATIVE CALLSIGN",
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                            if (nameSaved) {
                                Text(
                                    text = "SAVED TO PROFILE",
                                    color = Color(0xFF00FF9D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = callsignInput,
                                onValueChange = {
                                    if (it.length <= 16) {
                                        callsignInput = it
                                        nameSaved = false
                                    }
                                },
                                placeholder = { Text("Callsign (e.g. Maverick)", fontSize = 11.sp, color = Color(0xFF64748B)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("operative_name_input")
                            )

                            Button(
                                onClick = {
                                    val trimmed = callsignInput.trim()
                                    if (trimmed.isNotEmpty()) {
                                        onSetPlayerName(trimmed)
                                        nameSaved = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (nameSaved) Color(0xFF00FF9D) else Color(0xFF00F0FF),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("save_operative_name_button")
                            ) {
                                Text(if (nameSaved) "SAVED" else "SAVE", fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val score = (state.waveNumber.coerceAtLeast(1) - 1) * 1000 + (state.totalKills * 15) + (state.totalDamageDealt * 0.1f).toInt()
                        SummaryStatRow("Final Tactical Score", "$score PTS")
                        SummaryStatRow("Waves Cleared", "${state.waveNumber - 1}")
                        SummaryStatRow("Hostiles Slain", "${state.totalKills}")
                        SummaryStatRow("Damage Output", "${state.totalDamageDealt.toInt()}")
                        SummaryStatRow("Credits Harvested", "${state.totalScrapEarned}")
                        SummaryStatRow("Ascended Towers", "${state.towers.count { it.isAscended }}")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewTelemetry,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LEADERBOARD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F0FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("restart_run_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NEW RUN", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun DevBalanceSettingsDialog(
    config: DevBalanceConfig,
    towerPerformance: List<TowerPerformanceRecord>,
    onConfigChanged: (DevBalanceConfig) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Economy & Combat Sliders, 1 = Tower Efficiency Telemetry

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = !isLandscape)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.Center
        ) {
            Surface(
                color = Color(0xFF0B132B),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.8f)),
                modifier = if (isLandscape) {
                    Modifier
                        .width(420.dp)
                        .fillMaxHeight()
                        .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .padding(4.dp)
                }
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFB800).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Tune, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "DEV BALANCE CONSOLE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "v${com.example.BuildConfig.VERSION_NAME} • On-the-fly unit & economy balance sliders",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Sliders vs Telemetry
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF131F37),
                    contentColor = Color(0xFFFFB800),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("SLIDERS", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("TOWER TELEMETRY", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // Sliders View
                    var showExportModal by remember { mutableStateOf(false) }
                    var exportJsonText by remember { mutableStateOf("") }
                    val context = androidx.compose.ui.platform.LocalContext.current

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "GLOBAL ECONOMY & ROUND SLIDERS",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Starting Scrap
                        item {
                            BalanceSliderCard(
                                title = "Starting Credits (Early Economy)",
                                subtitle = "Lower credits early to avoid effortless initial defense",
                                value = config.startingScrap.toFloat(),
                                range = 100f..600f,
                                steps = 9,
                                valueFormatter = { "${it.toInt()} C" },
                                color = Color(0xFFFFD700),
                                onValueChange = { onConfigChanged(config.copy(startingScrap = it.roundToInt())) }
                            )
                        }

                        // Enemies Per Round Slider
                        item {
                            BalanceSliderCard(
                                title = "Enemies Per Round Multiplier",
                                subtitle = "Scale hostile assault volume per wave across both lanes",
                                value = config.enemiesPerRoundMultiplier,
                                range = 0.5f..3.0f,
                                steps = 24,
                                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                color = Color(0xFFFF2A55),
                                onValueChange = { onConfigChanged(config.copy(enemiesPerRoundMultiplier = (it * 100).roundToInt() / 100f)) }
                            )
                        }

                        // Minimum Lane Length Slider
                        item {
                            BalanceSliderCard(
                                title = "Min Lane Path Length",
                                subtitle = "Minimum tile travel distance for procedural lane generation",
                                value = config.minLaneLength.toFloat(),
                                range = 8f..25f,
                                steps = 16,
                                valueFormatter = { "${it.toInt()} tiles" },
                                color = Color(0xFF00F0FF),
                                onValueChange = {
                                    val newMin = it.roundToInt()
                                    val newMax = maxOf(newMin + 2, config.maxLaneLength)
                                    onConfigChanged(config.copy(minLaneLength = newMin, maxLaneLength = newMax))
                                }
                            )
                        }

                        // Maximum Lane Length Slider
                        item {
                            BalanceSliderCard(
                                title = "Max Lane Path Length",
                                subtitle = "Maximum tile curve winding distance across grid terrain",
                                value = config.maxLaneLength.toFloat(),
                                range = 15f..40f,
                                steps = 24,
                                valueFormatter = { "${it.toInt()} tiles" },
                                color = Color(0xFF38BDF8),
                                onValueChange = {
                                    val newMax = it.roundToInt()
                                    val newMin = minOf(newMax - 2, config.minLaneLength)
                                    onConfigChanged(config.copy(maxLaneLength = newMax, minLaneLength = newMin))
                                }
                            )
                        }

                        // Overall Income Multiplier
                        item {
                            BalanceSliderCard(
                                title = "Overall Income Multiplier",
                                subtitle = "Scales enemy bounties and resource drops on-the-fly",
                                value = config.globalIncomeMultiplier,
                                range = 0.2f..3.0f,
                                steps = 27,
                                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                color = Color(0xFFFFB800),
                                onValueChange = { onConfigChanged(config.copy(globalIncomeMultiplier = (it * 100).roundToInt() / 100f)) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "INDIVIDUAL TOWER UNITS (TAP LEFT TO EXPAND)",
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Collapsible Tower Unit Cards for every tower
                        items(TowerType.values()) { type ->
                            val currentTowerBal = config.getTowerBalance(type)
                            CollapsibleTowerBalanceCard(
                                towerType = type,
                                balance = currentTowerBal,
                                allTowers = TowerType.values().map { config.getTowerBalance(it) },
                                onBalanceChanged = { updated ->
                                    val updatedMap = config.customTowerConfigs.toMutableMap()
                                    updatedMap[type.name] = updated
                                    onConfigChanged(config.copy(customTowerConfigs = updatedMap))
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "INDIVIDUAL ENEMY HOSTILES (TAP LEFT TO EXPAND)",
                                color = Color(0xFFFF2A55),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Collapsible Enemy Unit Cards for every hostile
                        items(EnemyType.values()) { type ->
                            val currentEnemyBal = config.getEnemyBalance(type)
                            CollapsibleEnemyBalanceCard(
                                enemyType = type,
                                balance = currentEnemyBal,
                                onBalanceChanged = { updated ->
                                    val updatedMap = config.customEnemyConfigs.toMutableMap()
                                    updatedMap[type.name] = updated
                                    onConfigChanged(config.copy(customEnemyConfigs = updatedMap))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons Row: Reset Defaults, Export New Defaults (Diff Only), Apply & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onResetDefaults,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF2A55)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A55).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("RESET", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // EXPORT NEW DEFAULTS (DIFF-ONLY JSON)
                        Button(
                            onClick = {
                                exportJsonText = config.exportDiffJson()
                                showExportModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(38.dp)
                                .testTag("export_new_defaults_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT DEFAULTS", fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp)
                        ) {
                            Text("APPLY", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Diff Export Modal Dialog
                    if (showExportModal) {
                        Dialog(onDismissRequest = { showExportModal = false }) {
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00FF9D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.75f)
                                    .padding(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "DIFF-ONLY BALANCE EXPORT",
                                                color = Color(0xFF00FF9D),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Only delta settings differing from stock defaults are included",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 8.5.sp
                                            )
                                        }
                                        IconButton(onClick = { showExportModal = false }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = Color.White)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        color = Color(0xFF070B13),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(vertical = 4.dp)
                                    ) {
                                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                                            item {
                                                Text(
                                                    text = if (exportJsonText == "{\n}") "// No changes from defaults. Adjust any slider or name to export a delta diff!" else exportJsonText,
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 9.sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("RogueTD_BalanceDiff", exportJsonText)
                                                clipboard.setPrimaryClip(clip)
                                                android.widget.Toast.makeText(context, "Diff JSON copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("COPY JSON", fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = { showExportModal = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Tower Efficiency Telemetry View
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TOWER EFFECTIVENESS TELEMETRY",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Historical telemetry tracked across runs to guide unit balancing sliders",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(towerPerformance) { record ->
                                Surface(
                                    color = Color(0xFF131F37),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(record.towerClass.color).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = record.towerClass.shapeSymbol,
                                            color = Color(record.towerClass.color),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.width(20.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = record.towerName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "${record.totalBuilt} built",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 9.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "DMG: ${record.totalDamage.toInt()} | Kills: ${record.totalKills}",
                                                    color = Color(0xFF00FF9D),
                                                    fontSize = 9.sp
                                                )
                                                Text(
                                                    text = "DPS/Cost: ${String.format(Locale.US, "%.1f", record.dpsCostEfficiency)}",
                                                    color = Color(0xFFFFD700),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun BalanceSliderCard(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Surface(
        color = Color(0xFF131F37),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = valueFormatter(value),
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
    }
}

@Composable
fun CollapsibleTowerBalanceCard(
    towerType: TowerType,
    balance: TowerUnitBalance,
    allTowers: List<TowerUnitBalance>,
    onBalanceChanged: (TowerUnitBalance) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraft by remember(balance.customName) { mutableStateOf(balance.customName ?: towerType.displayName) }

    val currentName = balance.customName ?: towerType.displayName
    val towerColor = Color(towerType.color)

    // Identity analysis & cross-referencing
    val dps = balance.damage * balance.fireRate
    val overlaps = remember(balance, allTowers) {
        allTowers.filter { other ->
            other != balance &&
            Math.abs((other.damage * other.fireRate) - dps) < (dps * 0.08f) &&
            Math.abs(other.range - balance.range) < 0.4f
        }
    }

    val identityRole = remember(balance, towerType) {
        when {
            towerType.isBeam -> "Continuous High-Heat Melting Beam"
            towerType.isSplash -> "Kinetic Area-Denial & Splash Shells"
            towerType.chains > 0 -> "Multi-Arc Lightning Voltage Surge"
            towerType.slowPercent > 0f -> "Cryogenic Stasis & Slow Chilling"
            balance.range >= 4.0f -> "Long-Range Precision Interceptor"
            balance.fireRate >= 3.0f -> "High-Cadence Rapid Anti-Swarm"
            else -> "Tactical Point-Defense Kinetic Battery"
        }
    }

    Surface(
        color = Color(0xFF131F37),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) towerColor else Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header Row: Left-side click toggles expanded state!
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left click zone to expand / collapse
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(towerColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = towerColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${towerType.name})",
                                color = Color(0xFF64748B),
                                fontSize = 8.5.sp
                            )
                        }
                        Text(
                            text = "DMG: ${balance.damage.toInt()} | RNG: ${String.format(Locale.US, "%.1f", balance.range)} | SPD: ${String.format(Locale.US, "%.1f", balance.fireRate)}/s | ${balance.cost}C",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp
                        )
                    }
                }

                // Edit name button on right of header
                IconButton(
                    onClick = {
                        if (isEditingName) {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == towerType.displayName) null else trimmed))
                            isEditingName = false
                        } else {
                            nameDraft = currentName
                            isEditingName = true
                            isExpanded = true
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isEditingName) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = "Rename Unit",
                        tint = if (isEditingName) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Inline Name Editor TextField if active
            if (isEditingName) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        singleLine = true,
                        placeholder = { Text(towerType.displayName, fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = towerColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == towerType.displayName) null else trimmed))
                            isEditingName = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = towerColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("SAVE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Identity Role Badge & Cross-Reference Warnings
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IDENTITY: $identityRole",
                    color = towerColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (overlaps.isNotEmpty()) {
                    Text(
                        text = "⚠️ SIMILAR STATS DETECTED",
                        color = Color(0xFFFFB800),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        text = "✓ DISTINCT",
                        color = Color(0xFF00FF9D),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (overlaps.isNotEmpty()) {
                Text(
                    text = "Suggestion: Differentiate DPS (${dps.toInt()}) or Range from other units for unique tactical niche.",
                    color = Color(0xFFFFB800).copy(alpha = 0.85f),
                    fontSize = 7.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Collapsible Granular Sliders Area
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Base Damage Slider
                    BalanceSliderCard(
                        title = "Base Damage Per Shot",
                        subtitle = "Direct kinetic/energy burst power",
                        value = balance.damage,
                        range = 5f..350f,
                        steps = 68,
                        valueFormatter = { "${it.toInt()} DMG" },
                        color = Color(0xFFFF2A55),
                        onValueChange = { onBalanceChanged(balance.copy(damage = it.roundToInt().toFloat())) }
                    )

                    // Base Range Slider
                    BalanceSliderCard(
                        title = "Targeting Range Radius",
                        subtitle = "Grid radius (triggers accurately along lane sprite)",
                        value = balance.range,
                        range = 1.5f..7.0f,
                        steps = 21,
                        valueFormatter = { String.format(Locale.US, "%.1f cells", it) },
                        color = Color(0xFF00F0FF),
                        onValueChange = { onBalanceChanged(balance.copy(range = (it * 10).roundToInt() / 10f)) }
                    )

                    // Fire Rate Slider
                    BalanceSliderCard(
                        title = "Attack Rate (Shots / sec)",
                        subtitle = "Frequency of defensive weapon discharge",
                        value = balance.fireRate,
                        range = 0.2f..6.0f,
                        steps = 28,
                        valueFormatter = { String.format(Locale.US, "%.1f/s", it) },
                        color = Color(0xFFFFB800),
                        onValueChange = { onBalanceChanged(balance.copy(fireRate = (it * 10).roundToInt() / 10f)) }
                    )

                    // Cost Slider
                    BalanceSliderCard(
                        title = "Deployment Credit Cost",
                        subtitle = "Initial scrap investment required",
                        value = balance.cost.toFloat(),
                        range = 40f..400f,
                        steps = 35,
                        valueFormatter = { "${it.toInt()} C" },
                        color = Color(0xFFFFD700),
                        onValueChange = { onBalanceChanged(balance.copy(cost = (it / 10).roundToInt() * 10)) }
                    )

                    // Special Effect Slider (Splash Radius, Slow %, or Chains)
                    when {
                        towerType.isSplash -> {
                            BalanceSliderCard(
                                title = "Splash Explosion Radius",
                                subtitle = "Detonation blast footprint in grid cells",
                                value = balance.specialEffectValue.coerceAtLeast(0.5f),
                                range = 0.5f..4.0f,
                                steps = 13,
                                valueFormatter = { String.format(Locale.US, "%.1f cells", it) },
                                color = Color(0xFFFF5722),
                                onValueChange = { onBalanceChanged(balance.copy(specialEffectValue = (it * 10).roundToInt() / 10f)) }
                            )
                        }
                        towerType.slowPercent > 0f -> {
                            BalanceSliderCard(
                                title = "Cryo Slowdown Factor",
                                subtitle = "Speed impairment applied to affected hostiles",
                                value = balance.specialEffectValue.coerceAtLeast(0.1f),
                                range = 0.10f..0.80f,
                                steps = 13,
                                valueFormatter = { "${(it * 100).toInt()}% Slow" },
                                color = Color(0xFF00F0FF),
                                onValueChange = { onBalanceChanged(balance.copy(specialEffectValue = (it * 100).roundToInt() / 100f)) }
                            )
                        }
                        towerType.chains > 0 -> {
                            BalanceSliderCard(
                                title = "Tesla Chain Targets",
                                subtitle = "Maximum arc jumps across adjacent enemies",
                                value = balance.specialEffectValue.coerceAtLeast(1f),
                                range = 1f..8f,
                                steps = 6,
                                valueFormatter = { "${it.toInt()} Arcs" },
                                color = Color(0xFFB388FF),
                                onValueChange = { onBalanceChanged(balance.copy(specialEffectValue = it.roundToInt().toFloat())) }
                            )
                        }
                    }

                    // Reset this tower button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onBalanceChanged(
                                    TowerUnitBalance(
                                        customName = null,
                                        damage = towerType.baseDamage,
                                        range = towerType.baseRange,
                                        fireRate = towerType.baseFireRate,
                                        cost = towerType.baseCost,
                                        specialEffectValue = when {
                                            towerType.isSplash -> towerType.splashRadius
                                            towerType.slowPercent > 0f -> towerType.slowPercent
                                            towerType.chains > 0 -> towerType.chains.toFloat()
                                            else -> 0f
                                        }
                                    )
                                )
                                isEditingName = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset ${towerType.displayName} Defaults", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleEnemyBalanceCard(
    enemyType: EnemyType,
    balance: EnemyUnitBalance,
    onBalanceChanged: (EnemyUnitBalance) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraft by remember(balance.customName) { mutableStateOf(balance.customName ?: enemyType.displayName) }

    val currentName = balance.customName ?: enemyType.displayName
    val enemyColor = Color(enemyType.color)

    Surface(
        color = Color(0xFF131F37),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) enemyColor else Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header Row: Left-side click toggles expanded state!
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left click zone to expand / collapse
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(enemyColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = enemyColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${enemyType.name})",
                                color = Color(0xFF64748B),
                                fontSize = 8.5.sp
                            )
                        }
                        Text(
                            text = "HP: ${balance.hp.toInt()} | SPD: ${String.format(Locale.US, "%.2f", balance.speed)} | ARMOR: ${balance.armor.toInt()} | +${balance.scrapValue}C",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp
                        )
                    }
                }

                // Edit name button on right of header
                IconButton(
                    onClick = {
                        if (isEditingName) {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == enemyType.displayName) null else trimmed))
                            isEditingName = false
                        } else {
                            nameDraft = currentName
                            isEditingName = true
                            isExpanded = true
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isEditingName) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = "Rename Enemy",
                        tint = if (isEditingName) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Inline Name Editor TextField if active
            if (isEditingName) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        singleLine = true,
                        placeholder = { Text(enemyType.displayName, fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = enemyColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == enemyType.displayName) null else trimmed))
                            isEditingName = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = enemyColor, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("SAVE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Collapsible Granular Sliders Area
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Base HP Slider
                    BalanceSliderCard(
                        title = "Base Health Points",
                        subtitle = "Starting hit points before wave scaling",
                        value = balance.hp,
                        range = 25f..2000f,
                        steps = 78,
                        valueFormatter = { "${it.toInt()} HP" },
                        color = Color(0xFFFF2A55),
                        onValueChange = { onBalanceChanged(balance.copy(hp = it.roundToInt().toFloat())) }
                    )

                    // Movement Speed Slider
                    BalanceSliderCard(
                        title = "Traversal Speed",
                        subtitle = "Lane navigation speed in grid cells/sec",
                        value = balance.speed,
                        range = 0.3f..3.0f,
                        steps = 26,
                        valueFormatter = { String.format(Locale.US, "%.2f cells/s", it) },
                        color = Color(0xFF00FF9D),
                        onValueChange = { onBalanceChanged(balance.copy(speed = (it * 100).roundToInt() / 100f)) }
                    )

                    // Armor Slider
                    BalanceSliderCard(
                        title = "Damage Reduction Armor",
                        subtitle = "Flat or percentage reduction applied against hits",
                        value = balance.armor,
                        range = 0f..50f,
                        steps = 50,
                        valueFormatter = { "${it.toInt()} Armor" },
                        color = Color(0xFF38BDF8),
                        onValueChange = { onBalanceChanged(balance.copy(armor = it.roundToInt().toFloat())) }
                    )

                    // Scrap Value Bounty Slider
                    BalanceSliderCard(
                        title = "Scrap Bounty Yield",
                        subtitle = "Credits granted to player bank upon kill",
                        value = balance.scrapValue.toFloat(),
                        range = 2f..60f,
                        steps = 29,
                        valueFormatter = { "+${it.toInt()} Credits" },
                        color = Color(0xFFFFD700),
                        onValueChange = { onBalanceChanged(balance.copy(scrapValue = it.roundToInt())) }
                    )

                    // Reset this enemy button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onBalanceChanged(
                                    EnemyUnitBalance(
                                        customName = null,
                                        hp = enemyType.baseHp,
                                        speed = enemyType.baseSpeed,
                                        armor = enemyType.baseArmor,
                                        scrapValue = enemyType.scrapValue
                                    )
                                )
                                isEditingName = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset ${enemyType.displayName} Defaults", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PauseMenuDialog(
    state: GameUIState,
    onSpeedChanged: (GameSpeed) -> Unit,
    onOpenTelemetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onRestart: () -> Unit,
    onEndRun: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onDownloadAndInstallUpdate: () -> Unit = {},
    onResume: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB800)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.PauseCircle, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GAME PAUSED",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF00F0FF).copy(alpha = 0.20f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.7f))
                            ) {
                                Text(
                                    text = "v${com.example.BuildConfig.VERSION_NAME}",
                                    color = Color(0xFF00F0FF),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speeds Selection (Including new 10X!)
                Text(
                    text = "COMBAT SIMULATION SPEED",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(GameSpeed.NORMAL, GameSpeed.FAST, GameSpeed.ULTRA, GameSpeed.MAX, GameSpeed.HYPER).forEach { speed ->
                        val activeSpeed = if (state.isPauseMenuOpen) state.prePauseSpeed else (if (state.gameSpeed != GameSpeed.PAUSED) state.gameSpeed else state.prePauseSpeed)
                        val isSel = (activeSpeed == speed)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF00F0FF) else Color.Transparent)
                                .clickable { onSpeedChanged(speed) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = speed.label,
                                color = if (isSel) Color.Black else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Menu Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Resume
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESUME BATTLE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }

                    // Leaderboard & Telemetry
                    OutlinedButton(
                        onClick = onOpenTelemetry,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GLOBAL RANKS & METRICS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // Dev Balance Console
                    OutlinedButton(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB800)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DEV BALANCE CONSOLE (SLIDERS)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // End Run & Save Stats
                    Button(
                        onClick = onEndRun,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A55), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("pause_menu_end_run_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("END RUN & SAVE STATS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // Restart Run
                    OutlinedButton(
                        onClick = onRestart,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESTART WITH NEW MAP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // In-App APK Updater Status & Action
                    val updateState = state.updateUIState
                    if (updateState.status == UpdateDownloadStatus.AVAILABLE && updateState.updateInfo != null) {
                        Button(
                            onClick = onDownloadAndInstallUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPDATE TO v${updateState.updateInfo.latestVersion} [DOWNLOAD & INSTALL]", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                        }
                    } else if (updateState.status == UpdateDownloadStatus.DOWNLOADING) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00F0FF), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Downloading Update: ${updateState.progressPercent}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (updateState.status == UpdateDownloadStatus.READY_TO_INSTALL) {
                        Button(
                            onClick = onDownloadAndInstallUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("INSTALL DOWNLOADED UPDATE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onCheckForUpdates,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (updateState.status == UpdateDownloadStatus.CHECKING) "CHECKING GITHUB RELEASES..." else "CHECK FOR UPDATES (v${com.example.BuildConfig.VERSION_NAME})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Build Version Tag
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ROGUE TD • VERSION ${com.example.BuildConfig.VERSION_NAME} (BUILD #${com.example.BuildConfig.VERSION_CODE})",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("pause_menu_version_label")
                    )
                }
            }
        }
    }
}

@Composable
fun EndRunConfirmDialog(
    state: GameUIState,
    onConfirmEnd: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF2A55)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF2A55),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "END DEFENSE RUN?",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Wave ${state.waveNumber} • ${state.totalKills} Hostiles Terminated • Score: ${state.totalDamageDealt.toInt()}\n\nEnding now will log all performance stats to your records and leaderboard.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onConfirmEnd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF2A55),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("confirm_end_run_button")
                ) {
                    Text("CONFIRM & LOG STATS", fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text("RESUME DEFENSE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}


