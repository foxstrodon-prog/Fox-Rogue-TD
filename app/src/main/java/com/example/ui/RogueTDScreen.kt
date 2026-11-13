package com.example.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.AppUpdateState
import com.example.engine.GeneratedLevel
import com.example.engine.UpdateDownloadStatus
import com.example.model.FloatingText
import com.example.ui.dialog.EndRunConfirmDialog
import com.example.ui.dialog.RunSummaryDialog
import com.example.ui.dialog.TelemetryLeaderboardDialog
import com.example.ui.dialog.UpdateModalDialog
import com.example.ui.hud.MinimalBottomDock
import com.example.ui.hud.MinimalTopHud
import com.example.ui.hud.components.SingularityWarningDialog
import com.example.ui.menu.DevBalanceSettingsDialog
import com.example.ui.menu.PauseMenuDialog

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

            FloatingTextOverlay(
                texts = state.floatingTexts,
                level = state.level,
                rotationAngle = state.deviceRotationAngle
            )
        }

        // === MINIMAL FLOATING TOP TACTICAL HUD ===
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
            onRequestSingularity = { viewModel.requestSingularityCollapse() },
            onSellTower = { viewModel.sellSelectedTower() },
            onChangePriority = { viewModel.changeSelectedTowerPriority(it) },
            onDeselect = { viewModel.selectTile(null) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        // === TACTICAL SINGULARITY WARNING MODAL ===
        if (state.isSingularityWarningOpen) {
            SingularityWarningDialog(
                state = state,
                onConfirm = { viewModel.confirmSingularityCollapse() },
                onDismiss = { viewModel.dismissSingularityWarning() }
            )
        }

        // === ROUND 1 TACTICAL HINT ===
        if (!state.hasClickedFirstTile && state.waveNumber == 1 && state.towers.isEmpty()) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.88f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF00E676)),
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

        // === ROUND 1 LAUNCH POINTER CALLOUT ===
        if (!state.isWaveActive && state.waveNumber == 1 && state.towers.isNotEmpty()) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, Color(0xFF00F0FF)),
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
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f)),
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

        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .size(46.dp)
                .clickable { viewModel.swapBoosterPads() },
            shape = CircleShape,
            color = swapButtonColor,
            border = BorderStroke(1.5.dp, swapBorderColor),
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

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .size(46.dp)
                .clickable { viewModel.swapBoosterPads() },
            shape = CircleShape,
            color = swapButtonColor,
            border = BorderStroke(1.5.dp, swapBorderColor),
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

        // === MANUAL ROTATION SUGGESTION BUTTON ===
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
                border = BorderStroke(1.5.dp, Color(0xFF00F0FF)),
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
                onOpenTelemetry = { viewModel.openTelemetryFromPauseMenu() },
                onOpenSettings = { viewModel.openSettingsFromPauseMenu() },
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
                onDifficultyChanged = { viewModel.setGameDifficulty(it) },
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
                onViewTelemetry = { viewModel.toggleTelemetryView(true, initialTab = 2) },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    )
            )

            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFFB800)),
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

        // === DEV BALANCE & SETTINGS MODAL ===
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
        border = BorderStroke(1.2.dp, Color(0xFF00F0FF)),
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
