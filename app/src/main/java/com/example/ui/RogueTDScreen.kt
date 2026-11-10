package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.draw.rotate
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
import com.example.audio.*
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

        // === MINIMAL FLOATING TOP TACTICAL HUD (Adaptive edge docking & rotation safe margins) ===
        MinimalTopHud(
            state = state,
            onDeployWave = { viewModel.deployWave() },
            onToggleAutoWave = { viewModel.toggleAutoWave() },
            onOpenPauseMenu = { viewModel.openPauseMenu() },
            modifier = if (isLandscape) {
                Modifier
                    .align(Alignment.TopStart)
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            }
        )

        // === IN-APP GITHUB APK AUTO-UPDATER BANNER ===
        if (state.updateUIState.status in listOf(UpdateDownloadStatus.AVAILABLE, UpdateDownloadStatus.DOWNLOADING, UpdateDownloadStatus.READY_TO_INSTALL)) {
            UpdateBanner(
                updateState = state.updateUIState,
                onDownloadAndInstall = {
                    if (state.updateUIState.status == UpdateDownloadStatus.AVAILABLE) {
                        viewModel.openUpdateModal()
                    } else {
                        viewModel.downloadAndInstallUpdate()
                    }
                },
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
                onMasterVolumeChanged = { viewModel.setMasterVolume(it) },
                onMusicVolumeChanged = { viewModel.setMusicVolume(it) },
                onSfxVolumeChanged = { viewModel.setSfxVolume(it) },
                onUpgradeVolumeChanged = { viewModel.setUpgradeVolume(it) },
                onToggleAudioMute = { viewModel.toggleAudioMute() },
                onSoundProfileChanged = { viewModel.setSoundProfile(it) },
                onMusicTrackSelected = { viewModel.setMusicTrack(it) },
                onKineticSfxSlotSelected = { viewModel.setKineticSfxSlot(it) },
                onElementalSfxSlotSelected = { viewModel.setElementalSfxSlot(it) },
                onSiegeSfxSlotSelected = { viewModel.setSiegeSfxSlot(it) },
                onPlayTurretTestSound = { cls, slot -> viewModel.playTurretTestSound(cls, slot) },
                onTurretSkinSlotSelected = { viewModel.setTurretSkinSlot(it) },
                onMapThemeSlotSelected = { viewModel.setMapThemeSlot(it) },
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

        // === DEV BALANCE & SETTINGS MODAL (Dedicated strictly to gameplay balance values and telemetry) ===
        if (state.isSettingsOpen) {
            DevBalanceSettingsDialog(
                config = state.balanceConfig,
                towerPerformance = state.towerPerformance,
                onConfigChanged = { viewModel.updateBalanceConfig(it) },
                onResetDefaults = { viewModel.resetBalanceConfigToDefaults() },
                onResetTelemetry = { viewModel.resetTowerPerformance() },
                onDismiss = { viewModel.toggleSettingsView(false) }
            )
        }

        // === STARTUP / BOOT AUTO-UPDATE MODAL ===
        if (state.showUpdateModal && state.updateUIState.updateInfo?.isUpdateAvailable == true) {
            UpdateModalDialog(
                updateState = state.updateUIState,
                onDownloadAndInstall = { viewModel.downloadAndInstallUpdate() },
                onDismiss = { viewModel.dismissUpdateModal() }
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

    val hudRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = state.deviceRotationAngle,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "hudRotation"
    )

    Surface(
        color = Color(0xFF070B14).copy(alpha = 0.65f), // High transparency so background map remains visible underneath
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wave badge
                Row(
                    modifier = Modifier.rotate(hudRotation),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
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

                // Core HP (Stacked Heart Icon + Count for compact width & rotation resilience)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.rotate(hudRotation)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Core HP",
                            tint = Color(0xFFFF2A55),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "${state.coreHp}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            maxLines = 1
                        )
                    }
                }

                // Credits (Stacked Currency Icon above numeric count - TASK 2)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.rotate(hudRotation)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Diamond,
                            contentDescription = "Credits",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "${state.scrap}",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.5.sp,
                            maxLines = 1
                        )
                    }
                }

                // Consolidated Synergies Indicator with Dropdown (Symbol + count)
                val activeSynergiesCount = state.activeGlobalSynergies.size
                Box(modifier = Modifier.rotate(hudRotation)) {
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
                        .rotate(hudRotation)
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
                        .rotate(hudRotation)
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
                        .rotate(hudRotation)
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
                        isLandscape = isLandscape,
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
                        isLandscape = isLandscape,
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
    isLandscape: Boolean = false,
    onBuildClass: (TowerClass) -> Unit = {},
    onBuild: (TowerType) -> Unit = {},
    onClose: () -> Unit
) {
    val classes = TowerUpgradeManager.getTowerClasses()

    val renderCard: @Composable (TowerClass, Modifier) -> Unit = { towerClass, cardModifier ->
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
            modifier = cardModifier
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

        if (isLandscape) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                classes.forEach { towerClass ->
                    renderCard(towerClass, Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                classes.forEach { towerClass ->
                    renderCard(towerClass, Modifier.weight(1f))
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
    isLandscape: Boolean = false,
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
                // Section 1: Effective Combat Stats with accurate DPS
                val effDmg = tower.getEffectiveDamage()
                val effSpd = tower.getEffectiveFireRate()
                val effDps = tower.getEffectiveDps()
                val effRng = tower.getEffectiveCombatRange()
                Column {
                    Text(text = "COMBAT STATS (LIVE DPS)", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "DMG: ${effDmg.toInt()} | SPD: ${String.format(Locale.US, "%.1f", effSpd)}/s | DPS: ${effDps.toInt()} | RNG: ${effRng.toInt()}u", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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

                val renderVectorCard: @Composable (TowerVector, Modifier) -> Unit = { vec, cardModifier ->
                    val statBoost = when (vec) {
                        TowerVector.OFFENSIVE -> "+45% DMG  |  +15% CRIT  |  Heavy Penetration"
                        TowerVector.DEFENSIVE -> "+35% RNG  |  +20% Slow  |  Pushback & Stun"
                        TowerVector.SUPPORT -> "+40% SPD  |  +25% SCRAP |  Range Uplink Aura"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
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

                if (isLandscape) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        vectors.forEach { vec ->
                            renderVectorCard(vec, Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        vectors.forEach { vec ->
                            renderVectorCard(vec, Modifier.weight(1f))
                        }
                    }
                }
            }

            // TIER 1: Pick 1 of 3 Archetypes for the selected Vector and Tower Class
            1 -> {
                val vec = tower.vector ?: TowerVector.OFFENSIVE
                val archetypes = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, vec)

                val renderArchetypeCard: @Composable (TowerArchetype, Modifier) -> Unit = { archetype, cardModifier ->
                    val cost = TowerUpgradeManager.getTier2Cost(archetype)
                    val canAfford = scrap >= cost

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
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

                if (isLandscape) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        archetypes.forEach { archetype ->
                            renderArchetypeCard(archetype, Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        archetypes.forEach { archetype ->
                            renderArchetypeCard(archetype, Modifier.weight(1f))
                        }
                    }
                }
            }

            // TIER 2: Upgrade to Tier 3 Global Aura Projection (Choose between 2 unique auras!)
            2 -> {
                val archetype = tower.archetype ?: mapTypeToArchetype(tower.type)
                val cost = TowerUpgradeManager.getTier3GlobalCost(tower, allTowers)
                val canAfford = scrap >= cost

                val renderAuraCard: @Composable (AuraOption, Int, Modifier) -> Unit = { aura, auraIndex, cardModifier ->
                    val auraColor = if (auraIndex == 0) Color(0xFFFFB800) else Color(0xFF00FF9D)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
                            .border(
                                1.dp,
                                if (canAfford) auraColor else Color(0xFF26334D),
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
                                    text = aura.name,
                                    color = auraColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = auraColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = aura.shortBadge,
                                        color = auraColor,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = aura.description,
                                color = Color.White,
                                fontSize = 7.5.sp,
                                lineHeight = 9.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { if (canAfford) onUpgradeToTier3Global(auraIndex) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = auraColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .testTag(if (auraIndex == 0) "upgrade_tier3_global" else "upgrade_tier3_aura_1")
                            ) {
                                Text("ACTIVATE AURA", fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CHOOSE TIER 3 GLOBAL AURA PROJECTION (${cost} c)",
                        color = Color(0xFFFFB800),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    if (isLandscape) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            renderAuraCard(archetype.primaryAura, 0, Modifier.fillMaxWidth())
                            renderAuraCard(archetype.secondaryAura, 1, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            renderAuraCard(archetype.primaryAura, 0, Modifier.weight(1f))
                            renderAuraCard(archetype.secondaryAura, 1, Modifier.weight(1f))
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
                                    when (state.leaderboardStatus) {
                                        GlobalBoardStatus.ONLINE -> {
                                            Surface(
                                                color = Color(0xFF06331E),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF9D).copy(alpha = 0.5f)),
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
                                                            imageVector = Icons.Filled.CheckCircle,
                                                            contentDescription = null,
                                                            tint = Color(0xFF00FF9D),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Global Rankings (Verified)",
                                                            color = Color(0xFF00FF9D),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = "REFRESH",
                                                        color = Color(0xFF00F0FF),
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.clickable { onRetryLeaderboard() }
                                                    )
                                                }
                                            }
                                        }
                                        GlobalBoardStatus.OFFLINE_CACHED -> {
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
                                                            text = "Cached Rankings (Offline Mode)",
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
                                        GlobalBoardStatus.NOT_CONFIGURED -> {
                                            Surface(
                                                color = Color(0xFF1E293B),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Info,
                                                        contentDescription = null,
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Local Only (Global Board Unconfigured)",
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        GlobalBoardStatus.ERROR -> {
                                            Surface(
                                                color = Color(0xFF3B0B13),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A55).copy(alpha = 0.5f)),
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
                                                            imageVector = Icons.Filled.Warning,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFF2A55),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = state.leaderboardErrorMessage ?: "Network Error - Tap Refresh",
                                                            color = Color(0xFFFF2A55),
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
                                        else -> {}
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
                                            val isLocalPlayer = item.isLocalPlayer || item.name.equals(state.activePlayerName, ignoreCase = true)

                                            Surface(
                                                color = if (isLocalPlayer) Color(0xFF132A44) else Color(0xFF162544),
                                                shape = RoundedCornerShape(8.dp),
                                                border = when {
                                                    isLocalPlayer -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00F0FF))
                                                    index == 0 -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                                                    else -> null
                                                },
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
                        text = "New update available: v${info.latestVersion} [View & Update]",
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
fun UpdateModalDialog(
    updateState: AppUpdateState,
    onDownloadAndInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val info = updateState.updateInfo ?: return
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.8.dp, Color(0xFF00F0FF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon & Title
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00F0FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SYSTEM UPDATE AVAILABLE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "v${info.currentVersion} ➔ v${info.latestVersion}",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (info.releaseTitle.isNotBlank() && info.releaseTitle != "v${info.latestVersion}") {
                    Text(
                        text = info.releaseTitle,
                        color = Color(0xFFFFB800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Release Notes
                if (info.releaseNotes.isNotBlank()) {
                    Text(
                        text = "RELEASE NOTES",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFF161F30),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = info.releaseNotes,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Progress or status
                when (updateState.status) {
                    UpdateDownloadStatus.DOWNLOADING -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { updateState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF00F0FF),
                                trackColor = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Downloading APK: ${updateState.progressPercent}%",
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    UpdateDownloadStatus.READY_TO_INSTALL -> {
                        Surface(
                            color = Color(0xFF00FF9D).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF9D)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF9D),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "APK downloaded. Ready to install.",
                                    color = Color(0xFF00FF9D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    UpdateDownloadStatus.FAILED -> {
                        Text(
                            text = updateState.errorMessage ?: "Update download failed.",
                            color = Color(0xFFFF2A55),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    else -> {}
                }

                // Action Buttons (Update Now & Later/Skip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text("LATER", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }

                    Button(
                        onClick = onDownloadAndInstall,
                        enabled = updateState.status != UpdateDownloadStatus.DOWNLOADING,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (updateState.status == UpdateDownloadStatus.READY_TO_INSTALL) Color(0xFF00FF9D) else Color(0xFF00F0FF),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = when (updateState.status) {
                                UpdateDownloadStatus.READY_TO_INSTALL -> "INSTALL NOW"
                                UpdateDownloadStatus.DOWNLOADING -> "DOWNLOADING..."
                                UpdateDownloadStatus.FAILED -> "RETRY"
                                else -> "UPDATE NOW"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp
                        )
                    }
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

                Spacer(modifier = Modifier.height(8.dp))

                // Online Leaderboard Submission Status Badge
                val isDevTuned = state.lastRunRecord?.isDevTuned == true || state.balanceConfig != DevBalanceConfig()
                if (isDevTuned) {
                    Surface(
                        color = Color(0xFFEAB308).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "DEV TUNED • EXCLUDED FROM ONLINE LEADERBOARD",
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    when (state.leaderboardStatus) {
                        GlobalBoardStatus.LOADING -> {
                            Surface(
                                color = Color(0xFF00F0FF).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.5.dp,
                                        color = Color(0xFF00F0FF)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "UPLOADING RUN...",
                                        color = Color(0xFF00F0FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        }
                        GlobalBoardStatus.ONLINE -> {
                            Surface(
                                color = Color(0xFF00FF9D).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF9D)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "SCORE SUBMITTED TO GLOBAL LEADERBOARD",
                                    color = Color(0xFF00FF9D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        GlobalBoardStatus.ERROR -> {
                            Surface(
                                color = Color(0xFFFF2A55).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A55)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .testTag("leaderboard_retry")
                            ) {
                                Text(
                                    text = "UPLOAD FAILED",
                                    color = Color(0xFFFF2A55),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        GlobalBoardStatus.OFFLINE_CACHED, GlobalBoardStatus.IDLE -> {
                            Surface(
                                color = Color(0xFF64748B).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "QUEUED FOR UPLOAD (OFFLINE)",
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        GlobalBoardStatus.NOT_CONFIGURED -> {
                            Surface(
                                color = Color(0xFF64748B).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "LOCAL ONLY (GLOBAL BOARD UNCONFIGURED)",
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

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
    onResetTelemetry: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = ECONOMY, 1 = 39 TOWERS, 2 = TELEMETRY
    var showExportModal by remember { mutableStateOf(false) }
    var exportJsonText by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                        .width(460.dp)
                        .fillMaxHeight()
                        .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                        .padding(4.dp)
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
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
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "v${com.example.BuildConfig.VERSION_NAME} • Dual Front TD Dev Architect Tools",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 8.5.sp
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3 Navigation Tabs (Dev Console strictly dedicated to gameplay balance values and telemetry - TASK 3)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF131F37),
                        contentColor = Color(0xFFFFB800),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("ECONOMY", fontWeight = FontWeight.Bold, fontSize = 9.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("39 TOWERS", fontWeight = FontWeight.Bold, fontSize = 9.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("TELEMETRY", fontWeight = FontWeight.Bold, fontSize = 9.5.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content Area for active tab
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> {
                                // Tab 0: Economy & Global Combat Sliders
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "GLOBAL COMBAT & ECONOMY MULTIPLIERS",
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
                                            subtitle = "Default 220 Credits for tactical round placement",
                                            value = config.startingScrap.toFloat(),
                                            range = 100f..600f,
                                            steps = 9,
                                            valueFormatter = { "${it.toInt()} C" },
                                            color = Color(0xFFFFD700),
                                            onValueChange = { onConfigChanged(config.copy(startingScrap = it.roundToInt())) }
                                        )
                                    }

                                    // Global Tower Fire Rate Multiplier
                                    item {
                                        BalanceSliderCard(
                                            title = "Overall Tower Fire Rate Multiplier",
                                            subtitle = "Accelerates/slows all 39 towers firing speed",
                                            value = config.globalTowerFireRateMultiplier,
                                            range = 0.2f..3.0f,
                                            steps = 27,
                                            valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                            color = Color(0xFF00FF9D),
                                            onValueChange = { onConfigChanged(config.copy(globalTowerFireRateMultiplier = (it * 100).roundToInt() / 100f)) }
                                        )
                                    }

                                    // Overall Enemy Health Multiplier
                                    item {
                                        BalanceSliderCard(
                                            title = "Overall Unit Health Multiplier",
                                            subtitle = "Scales enemy base HP pool across all waves",
                                            value = config.enemyHpMultiplier,
                                            range = 0.5f..3.0f,
                                            steps = 24,
                                            valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                            color = Color(0xFFFF2A55),
                                            onValueChange = { onConfigChanged(config.copy(enemyHpMultiplier = (it * 100).roundToInt() / 100f)) }
                                        )
                                    }

                                    // Overall Unit Speed Multiplier
                                    item {
                                        BalanceSliderCard(
                                            title = "Overall Unit Speed Multiplier",
                                            subtitle = "Scales hostile lane traversal movement velocity",
                                            value = config.enemySpeedMultiplier,
                                            range = 0.2f..3.0f,
                                            steps = 27,
                                            valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                            color = Color(0xFF38BDF8),
                                            onValueChange = { onConfigChanged(config.copy(enemySpeedMultiplier = (it * 100).roundToInt() / 100f)) }
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

                                    // Enemies Per Round Slider
                                    item {
                                        BalanceSliderCard(
                                            title = "Enemies Per Round Multiplier",
                                            subtitle = "Scale hostile assault volume per wave across both lanes",
                                            value = config.enemiesPerRoundMultiplier,
                                            range = 0.5f..3.0f,
                                            steps = 24,
                                            valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                            color = Color(0xFFFF6600),
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
                                            color = Color(0xFFB026FF),
                                            onValueChange = {
                                                val newMax = it.roundToInt()
                                                val newMin = minOf(newMax - 2, config.minLaneLength)
                                                onConfigChanged(config.copy(maxLaneLength = newMax, minLaneLength = newMin))
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
                            }
                            1 -> {
                                // Tab 1: All 39 Towers (L1 Base, L2 Spec, L3 Final Forms)
                                val all39Descriptors = remember { DevBalanceConfig.getAll39TowerDescriptors() }
                                var familyFilter by remember { mutableStateOf("ALL") }
                                var levelFilter by remember { mutableStateOf(0) }

                                val filteredDescriptors = remember(familyFilter, levelFilter) {
                                    all39Descriptors.filter { desc ->
                                        (familyFilter == "ALL" || desc.family.name == familyFilter) &&
                                        (levelFilter == 0 || desc.level == levelFilter)
                                    }
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Filter Row 1: Tower Family
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("ALL" to "All 39", "GATLING" to "Gatling (13)", "CRYO" to "Cryo (13)", "TESLA" to "Tesla (13)").forEach { (key, label) ->
                                            val isSel = familyFilter == key
                                            Surface(
                                                color = if (isSel) Color(0xFFFFB800) else Color(0xFF131F37),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { familyFilter = key }
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSel) Color.Black else Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Filter Row 2: Level / Tier
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(0 to "All Tiers", 1 to "L1 Base (3)", 2 to "L2 Spec (9)", 3 to "L3 Final (27)").forEach { (lvl, label) ->
                                            val isSel = levelFilter == lvl
                                            Surface(
                                                color = if (isSel) Color(0xFF00F0FF) else Color(0xFF131F37),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { levelFilter = lvl }
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSel) Color.Black else Color(0xFF94A3B8),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filteredDescriptors, key = { it.key }) { desc ->
                                            val currentBal = config.getEffectiveTowerBalance(desc.family, desc.branchTier1, desc.finalFormCode)
                                            Collapsible39TowerCard(
                                                descriptor = desc,
                                                balance = currentBal,
                                                onBalanceChanged = { updated ->
                                                    val updatedMap = config.customTowerConfigs.toMutableMap()
                                                    updatedMap[desc.key] = updated
                                                    onConfigChanged(config.copy(customTowerConfigs = updatedMap))
                                                },
                                                onResetToDefault = {
                                                    val updatedMap = config.customTowerConfigs.toMutableMap()
                                                    updatedMap.remove(desc.key)
                                                    onConfigChanged(config.copy(customTowerConfigs = updatedMap))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Tab 2: Tower Telemetry & Live Combat Profiling
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "TOWER EFFECTIVENESS TELEMETRY",
                                                color = Color(0xFF00F0FF),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Reset telemetry to capture focused diagnostic windows",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 8.5.sp
                                            )
                                        }

                                        Button(
                                            onClick = onResetTelemetry,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A55), contentColor = Color.White),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("RESET WINDOW", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (towerPerformance.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF131F37))
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No telemetry captured in this window.\nDeploy waves or place towers to view live combat statistics.",
                                                color = Color(0xFF64748B),
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Action Bar
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
                            Text("RESET ALL", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

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
fun Collapsible39TowerCard(
    descriptor: TowerBalanceDescriptor,
    balance: TowerUnitBalance,
    onBalanceChanged: (TowerUnitBalance) -> Unit,
    onResetToDefault: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraft by remember(balance.customName) { mutableStateOf(balance.customName ?: descriptor.displayName) }

    val currentName = balance.customName ?: descriptor.displayName
    val towerColor = Color(descriptor.family.color)
    val levelColor = when (descriptor.level) {
        1 -> Color(0xFF00FF9D)
        2 -> Color(0xFF00F0FF)
        else -> Color(0xFFFFB800)
    }
    val levelLabel = when (descriptor.level) {
        1 -> "L1 BASE"
        2 -> "L2 SPEC"
        else -> "L3 FINAL"
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
            // Header Row: click to expand / collapse
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            Surface(
                                color = levelColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = levelLabel,
                                    color = levelColor,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = currentName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            text = "DMG: ${balance.damage.toInt()} | RNG: ${String.format(Locale.US, "%.1f", balance.range)} | SPD: ${String.format(Locale.US, "%.1f", balance.fireRate)}/s | ${balance.cost}C",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (isEditingName) {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == descriptor.displayName) null else trimmed))
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
                        placeholder = { Text(descriptor.displayName, fontSize = 10.sp) },
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
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == descriptor.displayName) null else trimmed))
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

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BalanceSliderCard(
                        title = "Base Damage Per Shot",
                        subtitle = "Direct kinetic/energy burst power",
                        value = balance.damage,
                        range = 5f..450f,
                        steps = 88,
                        valueFormatter = { "${it.toInt()} DMG" },
                        color = Color(0xFFFF2A55),
                        onValueChange = { onBalanceChanged(balance.copy(damage = it.roundToInt().toFloat())) }
                    )

                    BalanceSliderCard(
                        title = "Targeting Range Radius",
                        subtitle = "Grid cell targeting distance",
                        value = balance.range,
                        range = 1.5f..7.5f,
                        steps = 24,
                        valueFormatter = { String.format(Locale.US, "%.1f cells", it) },
                        color = Color(0xFF00F0FF),
                        onValueChange = { onBalanceChanged(balance.copy(range = (it * 10).roundToInt() / 10f)) }
                    )

                    BalanceSliderCard(
                        title = "Attack Rate (Shots / sec)",
                        subtitle = "Frequency of defensive weapon discharge",
                        value = balance.fireRate,
                        range = 0.2f..8.0f,
                        steps = 39,
                        valueFormatter = { String.format(Locale.US, "%.1f/s", it) },
                        color = Color(0xFFFFB800),
                        onValueChange = { onBalanceChanged(balance.copy(fireRate = (it * 10).roundToInt() / 10f)) }
                    )

                    BalanceSliderCard(
                        title = "Deployment Credit Cost",
                        subtitle = "Credits required for construction",
                        value = balance.cost.toFloat(),
                        range = 40f..600f,
                        steps = 55,
                        valueFormatter = { "${it.toInt()} C" },
                        color = Color(0xFFFFD700),
                        onValueChange = { onBalanceChanged(balance.copy(cost = (it / 10).roundToInt() * 10)) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onResetToDefault()
                                isEditingName = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset ${descriptor.displayName} Defaults", color = Color(0xFF94A3B8), fontSize = 9.sp)
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

/**
 * Visual thumbnail preview for turret skin slots (TASK 4).
 * Displays an inline sprite thumbnail rendering chassis, core, and skin overlay.
 */
@Composable
fun TurretSkinPreviewThumbnail(
    skinSlot: TurretSkinSlot,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(36.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.width * 0.42f

        // Base circular chassis foundation
        drawCircle(
            color = Color(0xFF1E293B),
            radius = radius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = if (isSelected) Color(0xFF00FF9D) else Color(0xFF475569),
            radius = radius * 0.85f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF94A3B8),
            radius = radius * 0.35f,
            center = Offset(cx, cy)
        )

        // Visual skin features (TASK 4)
        when (skinSlot) {
            TurretSkinSlot.SLOT_1_DEFAULT -> {
                // Sleek Neon Vector: concentric energy aura
                drawCircle(
                    color = Color(0xFF00F0FF).copy(alpha = 0.7f),
                    radius = radius * 0.58f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            TurretSkinSlot.SLOT_2_BLUEPRINT -> {
                // Blueprint Schematic: technical crosshairs & drafting ring
                val ch = radius * 0.75f
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx - ch, cy),
                    end = Offset(cx + ch, cy),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx, cy - ch),
                    end = Offset(cx, cy + ch),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = 0.65f),
                    radius = radius * 0.95f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            TurretSkinSlot.SLOT_3_MILITARY -> {
                // Tactical Military: heavy armor with 4 corner rivet bolts
                val rOff = radius * 0.55f
                val rRad = 2.dp.toPx()
                listOf(
                    Offset(cx - rOff, cy - rOff),
                    Offset(cx + rOff, cy - rOff),
                    Offset(cx - rOff, cy + rOff),
                    Offset(cx + rOff, cy + rOff)
                ).forEach { rPos ->
                    drawCircle(color = Color(0xFF475569), radius = rRad, center = rPos)
                    drawCircle(color = Color(0xFFCBD5E1), radius = rRad * 0.5f, center = rPos)
                }
            }
            TurretSkinSlot.SLOT_4_VOID -> {
                // Deep Void Rift: astral purple corona ring
                drawCircle(
                    color = Color(0xFFA855F7).copy(alpha = 0.85f),
                    radius = radius * 0.95f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFEC4899).copy(alpha = 0.5f),
                    radius = radius * 0.65f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            TurretSkinSlot.SLOT_5_PIXEL -> {
                // Retro Pixel Arcade: 8-bit stepped golden corners
                val pSize = 3.5.dp.toPx()
                val bOff = radius * 0.68f
                listOf(
                    Offset(cx - bOff, cy - bOff),
                    Offset(cx + bOff - pSize, cy - bOff),
                    Offset(cx - bOff, cy + bOff - pSize),
                    Offset(cx + bOff - pSize, cy + bOff - pSize)
                ).forEach { corner ->
                    drawRect(color = Color(0xFFFACC15), topLeft = corner, size = Size(pSize, pSize))
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
    onMasterVolumeChanged: (Float) -> Unit = {},
    onMusicVolumeChanged: (Float) -> Unit = {},
    onSfxVolumeChanged: (Float) -> Unit = {},
    onUpgradeVolumeChanged: (Float) -> Unit = {},
    onToggleAudioMute: () -> Unit = {},
    onSoundProfileChanged: (SoundProfile) -> Unit = {},
    onMusicTrackSelected: (MusicTrack) -> Unit = {},
    onKineticSfxSlotSelected: (Int) -> Unit = {},
    onElementalSfxSlotSelected: (Int) -> Unit = {},
    onSiegeSfxSlotSelected: (Int) -> Unit = {},
    onPlayTurretTestSound: (TowerClass, Int) -> Unit = { _, _ -> },
    onTurretSkinSlotSelected: (Int) -> Unit = {},
    onMapThemeSlotSelected: (Int) -> Unit = {},
    onResume: () -> Unit
) {
    var isCustomizationOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onResume) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isCustomizationOpen) Color(0xFFC084FC) else Color(0xFFFFB800)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCustomizationOpen) {
                            IconButton(
                                onClick = { isCustomizationOpen = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFC084FC), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AUDIO & SKINS",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        } else {
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
                    }

                    IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isCustomizationOpen) {
                    // === AUDIO & VISUAL CUSTOMIZATION SUBSECTION (TASK 09) ===
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section 1: Audio Channels & Attenuation
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("AUDIO MIXER & BUS VOLUMES", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    Button(
                                        onClick = onToggleAudioMute,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (state.isAudioMuted) Color(0xFFFF2A55) else Color(0xFF334155),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.isAudioMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (state.isAudioMuted) "MUTED" else "UNMUTED", fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                                    }
                                }

                                // Master Volume
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Master Output", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(state.masterVolume * 100).roundToInt()}%", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = state.masterVolume,
                                        onValueChange = onMasterVolumeChanged,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF))
                                    )
                                }

                                // Music Volume (Live attenuated feedback while paused)
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Music Volume (Live -50% Pause Level)", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(state.musicVolume * 100).roundToInt()}%", color = Color(0xFFC084FC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = state.musicVolume,
                                        onValueChange = onMusicVolumeChanged,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFC084FC), activeTrackColor = Color(0xFFC084FC))
                                    )
                                }

                                // SFX Volume
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Combat Sound Effects", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(state.sfxVolume * 100).roundToInt()}%", color = Color(0xFFFFB800), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = state.sfxVolume,
                                        onValueChange = onSfxVolumeChanged,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFB800), activeTrackColor = Color(0xFFFFB800))
                                    )
                                }

                                // Upgrade Sound Effects Volume (TASK 3)
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Upgrade Sound Effects", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(state.upgradeVolume * 100).roundToInt()}%", color = Color(0xFF00FF9D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = state.upgradeVolume,
                                        onValueChange = onUpgradeVolumeChanged,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9D), activeTrackColor = Color(0xFF00FF9D))
                                    )
                                }
                            }
                        }

                        // Section 2: Music Playlist & Track Selector (TASK 01 & TASK 04)
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("MUSIC STREAMING & PLAYLIST", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("Dynamic combat ducking automatically drops BGM during turret firing.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                                val tracks = listOf(
                                    MusicTrack.ROLLING_PLAYLIST to "🔁 Rolling Playlist (Cycles 1-5)",
                                    MusicTrack.CYBER_AMBIENT to "Track 1: Sector Zero Synth",
                                    MusicTrack.PULSE_ARPEGGIO to "Track 2: Dual Front Pulse",
                                    MusicTrack.DEEP_SPACE to "Track 3: Sub-Void Drone",
                                    MusicTrack.NEON_OVERDRIVE to "Track 4: Neon Overdrive",
                                    MusicTrack.CELESTIAL_BASTION to "Track 5: Celestial Bastion",
                                    MusicTrack.NONE to "Mute Music Track"
                                )

                                tracks.forEach { (track, label) ->
                                    val isSel = (state.musicTrack == track)
                                    Surface(
                                        color = if (isSel) Color(0xFFC084FC).copy(alpha = 0.25f) else Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFC084FC) else Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onMusicTrackSelected(track) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.MusicNote,
                                                contentDescription = null,
                                                tint = if (isSel) Color(0xFFC084FC) else Color(0xFF64748B),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = label,
                                                color = if (isSel) Color.White else Color(0xFFCBD5E1),
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section 3: Turret Gunfire Audio Variants (TASK 06)
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("TURRET GUNFIRE AUDIO TESTING (5 SLOTS)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("A/B test gunfire audio profiles across the 3 turret archetypes.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                                // Kinetic Ballistics
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Kinetic Ballistics (Rotary)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Button(
                                            onClick = { onPlayTurretTestSound(TowerClass.KINETIC_BALLISTIC, state.kineticSfxSlot) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(22.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("TEST", fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        (1..5).forEach { slot ->
                                            val isSel = state.kineticSfxSlot == slot
                                            val slotName = TurretSfxSlot.fromSlot(slot).kineticName
                                            Surface(
                                                color = if (isSel) Color(0xFF00F0FF) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF00F0FF) else Color(0xFF334155)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onKineticSfxSlotSelected(slot) }
                                            ) {
                                                Column(modifier = Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Slot $slot", color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    Text(slotName.split(" ").first(), color = if (isSel) Color(0xFF0F172A) else Color(0xFF94A3B8), fontSize = 7.5.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Elemental Void
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Elemental Void (Energy)", color = Color(0xFFD946EF), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Button(
                                            onClick = { onPlayTurretTestSound(TowerClass.ELEMENTAL_VOID, state.elementalSfxSlot) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD946EF), contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(22.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("TEST", fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        (1..5).forEach { slot ->
                                            val isSel = state.elementalSfxSlot == slot
                                            val slotName = TurretSfxSlot.fromSlot(slot).elementalName
                                            Surface(
                                                color = if (isSel) Color(0xFFD946EF) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFD946EF) else Color(0xFF334155)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onElementalSfxSlotSelected(slot) }
                                            ) {
                                                Column(modifier = Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Slot $slot", color = if (isSel) Color.White else Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    Text(slotName.split(" ").first(), color = if (isSel) Color(0xFFFDF4FF) else Color(0xFF94A3B8), fontSize = 7.5.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Heavy Siege
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Heavy Siege (Artillery)", color = Color(0xFFFFB800), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Button(
                                            onClick = { onPlayTurretTestSound(TowerClass.HEAVY_SIEGE, state.siegeSfxSlot) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800), contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(22.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("TEST", fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        (1..5).forEach { slot ->
                                            val isSel = state.siegeSfxSlot == slot
                                            val slotName = TurretSfxSlot.fromSlot(slot).siegeName
                                            Surface(
                                                color = if (isSel) Color(0xFFFFB800) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFFB800) else Color(0xFF334155)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onSiegeSfxSlotSelected(slot) }
                                            ) {
                                                Column(modifier = Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Slot $slot", color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    Text(slotName.split(" ").first(), color = if (isSel) Color(0xFF0F172A) else Color(0xFF94A3B8), fontSize = 7.5.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 4: Turret Visual Skins (TASK 07)
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("TURRET VISUAL SKINS (5 SLOTS)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                TurretSkinSlot.values().forEach { skin ->
                                    val isSel = (state.turretSkinSlot == skin.slot)
                                    Surface(
                                        color = if (isSel) Color(0xFF00FF9D).copy(alpha = 0.20f) else Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF00FF9D) else Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onTurretSkinSlotSelected(skin.slot) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Inline Visual Thumbnail Preview (TASK 4)
                                            TurretSkinPreviewThumbnail(
                                                skinSlot = skin,
                                                isSelected = isSel
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.Palette,
                                                        contentDescription = null,
                                                        tint = if (isSel) Color(0xFF00FF9D) else Color(0xFF64748B),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = "Slot ${skin.slot}: ${skin.displayName}",
                                                        color = if (isSel) Color.White else Color(0xFFE2E8F0),
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                                Text(
                                                    text = skin.description,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 8.5.sp,
                                                    modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 5: Map & Lane Themes (TASK 08)
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("MAP & LANE ENVIRONMENT THEMES (5 SLOTS)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                MapThemeSlot.values().forEach { theme ->
                                    val isSel = (state.mapThemeSlot == theme.slot)
                                    Surface(
                                        color = if (isSel) Color(0xFFFFB800).copy(alpha = 0.20f) else Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFFB800) else Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onMapThemeSlotSelected(theme.slot) }
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.GridOn,
                                                    contentDescription = null,
                                                    tint = if (isSel) Color(0xFFFFB800) else Color(0xFF64748B),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Slot ${theme.slot}: ${theme.displayName}",
                                                    color = if (isSel) Color.White else Color(0xFFE2E8F0),
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                            Text(
                                                text = theme.description,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 8.5.sp,
                                                modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section 6: Procedural Sound FX Profiles (TASK 3)
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("PROCEDURAL SOUND FX PROFILES", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("Select the synthesized procedural sound effect wave generator profile.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                                SoundProfile.values().forEach { profile ->
                                    val isSel = (state.soundProfile == profile)
                                    Surface(
                                        color = if (isSel) Color(0xFF00F0FF).copy(alpha = 0.20f) else Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF00F0FF) else Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSoundProfileChanged(profile) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.GraphicEq,
                                                contentDescription = null,
                                                tint = if (isSel) Color(0xFF00F0FF) else Color(0xFF64748B),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = profile.label,
                                                color = if (isSel) Color.White else Color(0xFFCBD5E1),
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // === MAIN PAUSE ACTIONS VIEW ===
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

                        // Audio & Visual Customization (TASK 09)
                        OutlinedButton(
                            onClick = { isCustomizationOpen = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC084FC)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC084FC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AUDIO & VISUAL CUSTOMIZATION", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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


