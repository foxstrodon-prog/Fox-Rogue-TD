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
import com.example.ui.hud.MinimalTopHud
import com.example.ui.hud.MinimalBottomDock
import com.example.ui.hud.BuildTowerDrawer
import com.example.ui.hud.BranchingTowerInspector
import com.example.ui.hud.ThreePathTowerInspector
import com.example.ui.menu.DevBalanceSettingsDialog
import com.example.ui.menu.PauseMenuDialog
import com.example.ui.menu.TurretSkinPreviewThumbnail
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
            FloatingTextOverlay(
                texts = state.floatingTexts,
                level = state.level,
                rotationAngle = state.deviceRotationAngle
            )
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

        // === FLOATING SQUIRCLE UPGRADE ACTION BAR (TRACK 2) ===
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
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

        // === ROUND 1 LAUNCH POINTER CALLOUT (Points directly to Begin button once defenses are built) ===
        if (!state.isWaveActive && state.waveNumber == 1 && state.towers.isNotEmpty()) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00F0FF)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp)
                    .clickable { viewModel.deployWave() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▲ DEFENSES READY — TAP [▶ BEGIN] TO START",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.5.sp
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

        // === DUAL ONE-HANDED TELEPORTATION PAD SWAP BUTTONS ===
        val swapButtonColor = Color(0xFF00E5FF).copy(alpha = 0.28f)
        val swapBorderColor = Color(0xFF00F0FF).copy(alpha = 0.65f)

        // Left Thumb Trigger
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .size(46.dp)
                .clickable { viewModel.swapBoosterPads() },
            shape = CircleShape,
            color = swapButtonColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, swapBorderColor),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Swap Teleportation Pads Left",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right Thumb Trigger
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .size(46.dp)
                .clickable { viewModel.swapBoosterPads() },
            shape = CircleShape,
            color = swapButtonColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, swapBorderColor),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Swap Teleportation Pads Right",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // === MANUAL ROTATION SUGGESTION BUTTON (When System Rotation Lock is active and device is tilted) ===
        if (state.isSystemAutoRotateLocked && state.hardwareTiltAngle != state.deviceRotationAngle) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 28.dp)
                    .size(44.dp)
                    .clickable { viewModel.confirmManualRotation() }
                    .testTag("manual_rotate_button"),
                shape = CircleShape,
                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00F0FF)),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ScreenRotation,
                        contentDescription = "Rotate Screen to Match Device Tilt",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(22.dp)
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
                    viewModel.openTelemetryFromPauseMenu()
                },
                onOpenSettings = {
                    viewModel.openSettingsFromPauseMenu()
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
                onPlayTowerUpgradeTestSound = { cls, slot -> viewModel.playTowerUpgradeTestSound(cls, slot) },
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
                onDismiss = {
                    viewModel.toggleTelemetryView(false)
                    viewModel.openPauseMenu()
                }
            )
        }

        // === IN-APP 30S SCRIPTED CINEMATIC SHOWCASE BANNER & INPUT LOCK ===
        if (state.activeCinematic != null) {
            val cin = state.activeCinematic!!

            // Full-screen touch blocker so accidental touches cannot disrupt the cinematic
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Non-interactable: prevent player touches during cinematic */ }
                    )
            )

            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB800)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB800))
                    )
                    Text(
                        text = "SHOWCASE: ${cin.scenario.title} [${cin.currentFrame / 60}s / 30s] • ${cin.scenario.getTargetFilename()}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                    Button(
                        onClick = { viewModel.stopCinematic() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A55)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(22.dp)
                    ) {
                        Text("STOP", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }

        // === DEV BALANCE & SETTINGS MODAL (Dedicated strictly to gameplay balance values and telemetry) ===
        if (state.isSettingsOpen) {
            DevBalanceSettingsDialog(
                config = state.balanceConfig,
                towerPerformance = state.towerPerformance,
                onConfigChanged = { viewModel.updateBalanceConfig(it) },
                onResetDefaults = { viewModel.resetBalanceConfigToDefaults() },
                onResetTelemetry = { viewModel.resetTowerPerformance() },
                onSpawnSandboxWave = { type, count -> viewModel.spawnSandboxWave(type, count) },
                onLaunchCinematic = { viewModel.launchCinematic(it) },
                onPlaySignatureWave = { viewModel.jumpToSignatureWave(it) },
                activeCreepCount = state.enemies.size,
                activeBulletCount = state.projectiles.size,
                activeParticleCount = state.trackParticles.size,
                currentFps = viewModel.currentFps,
                frameJitterMs = viewModel.frameJitterMs,
                onDismiss = {
                    viewModel.toggleSettingsView(false)
                    viewModel.openPauseMenu()
                }
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
                                        val isOnline = state.leaderboardStatus == GlobalBoardStatus.ONLINE
                                        if (isOnline) {
                                            Surface(
                                                color = Color(0xFF06331E),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF9D).copy(alpha = 0.5f)),
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00FF9D),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "GLOBAL UPLINK VERIFIED • CONNECTED",
                                                        color = Color(0xFF00FF9D),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (isOnline) {
                                                "Leaderboard uplink is live!\nBe the first Commander to record a verified high score."
                                            } else {
                                                state.leaderboardErrorMessage ?: "Connection standby. Tap Refresh to connect."
                                            },
                                            color = if (isOnline) Color(0xFF94A3B8) else Color(0xFFFF2A55),
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
                                                text = if (isOnline) "Refresh Rankings" else "Retry Uplink",
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
fun FloatingTextOverlay(texts: List<FloatingText>, level: GeneratedLevel?, rotationAngle: Float = 0f) {
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
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.rotate(-rotationAngle)
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
                val isDevTuned = state.lastRunRecord?.isDevTuned == true || state.balanceConfig.isGameplayModified()
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
                            val hasPending = state.leaderboardPendingCount > 0
                            Surface(
                                color = if (hasPending) Color(0xFF64748B).copy(alpha = 0.2f) else Color(0xFFFF2A55).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (hasPending) Color(0xFF64748B) else Color(0xFFFF2A55)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .testTag("leaderboard_retry")
                            ) {
                                Text(
                                    text = if (hasPending) "QUEUED FOR UPLOAD (OFFLINE)" else "UPLOAD FAILED",
                                    color = if (hasPending) Color(0xFF94A3B8) else Color(0xFFFF2A55),
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


