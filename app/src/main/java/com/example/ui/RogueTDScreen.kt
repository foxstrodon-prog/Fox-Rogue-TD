package com.example.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
import com.example.ui.hud.components.EdgeBulletTipTab
import com.example.ui.hud.components.SingularityWarningDialog
import com.example.ui.menu.DevBalanceSettingsDialog
import com.example.ui.menu.PauseMenuDialog

@Composable
fun RogueTDScreen(
    viewModel: RogueTDViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hudState by remember { derivedStateOf { state.toHudState() } }
    val renderState by remember { derivedStateOf { state.toRenderState() } }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isSquircleOpen = hudState.selectedTower != null || (hudState.selectedTile != null && hudState.selectedTile?.isBuildable == true)
    BackHandler(
        enabled = hudState.activeCinematic == null && !hudState.isGameOver && !hudState.isVictory && !hudState.isPauseMenuOpen
    ) {
        if (isSquircleOpen) {
            viewModel.selectTile(null)
        } else {
            viewModel.openPauseMenu()
        }
    }

    val density = LocalDensity.current
    var hudOffsetY by remember { mutableFloatStateOf(0f) }
    val maxUpOffsetPx = remember(density) { with(density) { (-48).dp.toPx() } }
    val maxDownOffsetPx = remember(density) { with(density) { 76.dp.toPx() } }

    val normalizedRot = ((hudState.deviceRotationAngle % 360f + 360f) % 360f).toInt()
    val isHudRotated = normalizedRot == 90 || normalizedRot == 270
    val targetEdgeCounterRotation = when (normalizedRot) {
        90 -> -90f
        180 -> 180f
        270 -> 90f
        else -> 0f
    }
    val edgeCounterRotation by animateFloatAsState(
        targetValue = targetEdgeCounterRotation,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "edge_tab_rotation"
    )
    val topBannerOffsetDp = (if (isHudRotated) 52.dp else 40.dp)
    val dynamicBannerShiftPx = hudOffsetY.roundToInt().coerceAtLeast(0)
    val hasUpdateBanner = hudState.updateUIState.status in listOf(
        UpdateDownloadStatus.AVAILABLE,
        UpdateDownloadStatus.DOWNLOADING,
        UpdateDownloadStatus.READY_TO_INSTALL
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B13))
    ) {
        // === FULL SCREEN MAP CANVAS (Static locked map coordinate system) ===
        Box(modifier = Modifier.fillMaxSize()) {
            GameCanvas(
                state = state,
                onTileClicked = { viewModel.selectTile(it) }
            )

            FloatingTextOverlay(
                texts = renderState.floatingTexts,
                level = hudState.level,
                rotationAngle = hudState.deviceRotationAngle
            )
        }

        // === MINIMAL FLOATING TOP TACTICAL HUD ===
        MinimalTopHud(
            state = hudState,
            onDeployWave = { viewModel.deployWave() },
            onToggleAutoWave = { viewModel.toggleAutoWave() },
            onOpenPauseMenu = { viewModel.openPauseMenu() },
            modifier = Modifier
                .offset { IntOffset(0, hudOffsetY.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        hudOffsetY = (hudOffsetY + delta).coerceIn(maxUpOffsetPx, maxDownOffsetPx)
                    }
                )
                .then(
                    if (isLandscape) {
                        Modifier
                            .align(Alignment.TopStart)
                            .widthIn(max = 560.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    } else {
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    }
                )
        )

        // === IN-APP GITHUB APK AUTO-UPDATER BANNER ===
        if (hasUpdateBanner) {
            val hasRound1TopBanner = (!state.hasClickedFirstTile && state.waveNumber == 1 && state.towers.isEmpty()) ||
                (!state.isWaveActive && state.waveNumber == 1 && state.towers.isNotEmpty())
            UpdateBanner(
                updateState = hudState.updateUIState,
                onDownloadAndInstall = {
                    if (hudState.updateUIState.status == UpdateDownloadStatus.AVAILABLE) {
                        viewModel.openUpdateModal()
                    } else {
                        viewModel.downloadAndInstallUpdate()
                    }
                },
                modifier = Modifier
                    .align(if (isLandscape) Alignment.BottomStart else Alignment.TopCenter)
                    .padding(
                        if (isLandscape) PaddingValues(start = 12.dp, bottom = 12.dp)
                        else PaddingValues(top = if (hasRound1TopBanner) topBannerOffsetDp + 30.dp else topBannerOffsetDp)
                    )
            )
        }

        // === FLOATING SQUIRCLE UPGRADE ACTION BAR (TRACK 2) ===
        MinimalBottomDock(
            state = hudState,
            onBuildTowerClass = { tile, cls -> viewModel.buildTower(tile, cls) },
            onUpgradeToVector = { tower, vec -> viewModel.upgradeTowerToVector(tower, vec) },
            onUpgradeToArchetype = { tower, arch -> viewModel.upgradeTowerToArchetype(tower, arch) },
            onUpgradeToTier3Global = { tower, auraIdx -> viewModel.upgradeTowerToTier3Global(tower, auraIdx) },
            onRequestSingularity = { viewModel.requestSingularityCollapse() },
            onSellTower = { viewModel.sellSelectedTower() },
            onChangePriority = { viewModel.changeSelectedTowerPriority(it) },
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
                color = Color(0xFF0A101D).copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF00E676)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topBannerOffsetDp)
                    .offset { IntOffset(0, dynamicBannerShiftPx) }
                    .clickable { viewModel.dismissFirstTileHint() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "SELECT TILE TO BUILD (Tap boosted pad)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
            }
        }

        // === ROUND 1 LAUNCH POINTER CALLOUT ===
        if (!state.isWaveActive && state.waveNumber == 1 && state.towers.isNotEmpty()) {
            Surface(
                color = Color(0xFF0A101D).copy(alpha = 0.90f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.85f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topBannerOffsetDp)
                    .offset { IntOffset(0, dynamicBannerShiftPx) }
                    .clickable { viewModel.deployWave() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DEFENSES READY — TAP [▶ BEGIN] TO START",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
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

        // === DUAL ONE-HANDED TELEPORTATION PAD SWAP "BULLET TIPS" ===
        EdgeBulletTipTab(
            isLeftEdge = true,
            icon = Icons.Filled.SwapHoriz,
            contentDescription = "Swap Teleportation Pads Left",
            counterRotation = edgeCounterRotation,
            onClick = { viewModel.swapBoosterPads() },
            modifier = Modifier.align(Alignment.CenterStart)
        )
        EdgeBulletTipTab(
            isLeftEdge = false,
            icon = Icons.Filled.SwapHoriz,
            contentDescription = "Swap Teleportation Pads Right",
            counterRotation = edgeCounterRotation,
            onClick = { viewModel.swapBoosterPads() },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // === MANUAL ROTATION SUGGESTION "BULLET TIPS" (SLIDE + FADE) ===
        val showManualRotate = state.isSystemAutoRotateLocked && state.hardwareTiltAngle != state.deviceRotationAngle
        AnimatedVisibility(
            visible = showManualRotate,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 180)
            ) + fadeOut(animationSpec = tween(durationMillis = 150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 48.dp)
        ) {
            EdgeBulletTipTab(
                isLeftEdge = false,
                icon = Icons.Filled.ScreenRotation,
                contentDescription = "Rotate Screen to Match Device Tilt",
                counterRotation = edgeCounterRotation,
                tag = "manual_rotate_button_right",
                onClick = { viewModel.confirmManualRotation() }
            )
        }

        AnimatedVisibility(
            visible = showManualRotate,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(durationMillis = 180)
            ) + fadeOut(animationSpec = tween(durationMillis = 150)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = if (isLandscape && hasUpdateBanner) 64.dp else 48.dp)
        ) {
            EdgeBulletTipTab(
                isLeftEdge = true,
                icon = Icons.Filled.ScreenRotation,
                contentDescription = "Rotate Screen to Match Device Tilt",
                counterRotation = edgeCounterRotation,
                tag = "manual_rotate_button_left",
                onClick = { viewModel.confirmManualRotation() }
            )
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
                onSetPlayerName = { viewModel.setPlayerName(it) },
                onRetryLeaderboard = { viewModel.fetchOnlineLeaderboard(true) }
            )
        }

        // === TELEMETRY & LEADERBOARD MODAL ===
        if (state.isTelemetryOpen) {
            TelemetryLeaderboardDialog(
                state = state,
                onSetPlayerName = { viewModel.setPlayerName(it) },
                onRetryLeaderboard = { viewModel.fetchOnlineLeaderboard(true) },
                onClearPendingQueue = { viewModel.clearPendingLeaderboardQueue() },
                onDismiss = {
                    viewModel.toggleTelemetryView(false)
                    if (!state.isGameOver && !state.isVictory) {
                        viewModel.openPauseMenu()
                    }
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
