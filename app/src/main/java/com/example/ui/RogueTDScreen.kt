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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.GeneratedLevel
import com.example.engine.TowerUpgradeManager
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B13))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // === FULL SCREEN MAP CANVAS ===
        Box(modifier = Modifier.fillMaxSize()) {
            GameCanvas(
                state = state,
                onTileClicked = { viewModel.selectTile(it) }
            )

            // Floating Combat Text Overlay
            FloatingTextOverlay(texts = state.floatingTexts, level = state.level)

            // === MACRO SYNERGY HUD (Collapsible) ===
            MacroSynergyHud(
                synergies = state.activeGlobalSynergies,
                isExpanded = state.isSynergyPanelExpanded,
                onToggleExpand = { viewModel.toggleSynergyPanel() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp, start = 8.dp, end = 8.dp)
            )
        }

        // === MINIMAL FLOATING TOP TACTICAL HUD ===
        MinimalTopHud(
            state = state,
            onDeployWave = { viewModel.deployWave() },
            onToggleAutoWave = { viewModel.toggleAutoWave() },
            onOpenPauseMenu = { viewModel.openPauseMenu() },
            onEndRun = { viewModel.requestEndRun() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )

        // === MINIMAL FLOATING BOTTOM COMMAND DOCK (Only visible when tile/tower selected) ===
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
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
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
                onViewTelemetry = { viewModel.toggleTelemetryView(true) }
            )
        }

        // === TELEMETRY & LEADERBOARD MODAL ===
        if (state.isTelemetryOpen) {
            TelemetryLeaderboardDialog(
                state = state,
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
    onEndRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0A0F1D).copy(alpha = 0.95f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wave badge
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                // Core HP
                Surface(
                    color = Color(0xFF161F30),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Core HP",
                            tint = if (state.coreHp <= 8) Color(0xFFFF2A55) else Color(0xFF00FF9D),
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
                    color = Color(0xFF161F30),
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

                // Auto Wave Toggle
                Surface(
                    color = if (state.autoCallNextWave) Color(0xFF00FF9D).copy(alpha = 0.2f) else Color(0xFF161F30),
                    shape = RoundedCornerShape(5.dp),
                    border = if (state.autoCallNextWave) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF9D)) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .clickable { onToggleAutoWave() }
                        .padding(1.dp)
                ) {
                    Text(
                        text = if (state.autoCallNextWave) "AUTO ON" else "AUTO",
                        color = if (state.autoCallNextWave) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                    )
                }

                // Launch Wave Button
                Button(
                    onClick = onDeployWave,
                    enabled = !state.isWaveActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00F0FF),
                        disabledContainerColor = Color(0xFF1E293B),
                        contentColor = Color.Black,
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("deploy_wave_button")
                ) {
                    Icon(
                        imageVector = if (state.isWaveActive) Icons.Filled.FastForward else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (state.isWaveActive) "${state.enemies.size} FOES" else "LAUNCH",
                        fontWeight = FontWeight.Black,
                        fontSize = 9.5.sp
                    )
                }

                // End Run Button
                Button(
                    onClick = onEndRun,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF2A55).copy(alpha = 0.22f),
                        contentColor = Color(0xFFFF2A55)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A55)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("end_run_button")
                ) {
                    Text(
                        text = "END",
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }

                // Gear Cog Button (Opens Pause Menu)
                IconButton(
                    onClick = onOpenPauseMenu,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF161F30))
                        .testTag("pause_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Game Settings & Controls",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Harmonic Grid Status Sub-strip
            if (state.harmonicGridActive || state.harmonicPadsCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.harmonicGridActive) "⚡ HARMONIC GRID ACTIVE: 6/6 (+20% DMG / +25% SCRAP)" else "BOOSTED PADS ACTIVE: ${state.harmonicPadsCount}/6",
                        color = if (state.harmonicGridActive) Color(0xFFFFD700) else Color(0xFF00FF9D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MacroSynergyHud(
    synergies: List<GlobalAuraSynergy>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (synergies.isEmpty()) return

    Surface(
        color = Color(0xFF090E1A).copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.8f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌟", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MACRO SYNERGIES (${synergies.size} ACTIVE)",
                        color = Color(0xFFFFB800),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    synergies.forEach { syn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131F33), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = syn.iconEmoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${syn.auraName} (${syn.sourceCount} Emitter${if (syn.sourceCount > 1) "s" else ""})",
                                    color = Color(syn.color),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                )
                                Text(
                                    text = syn.effectDescription,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 8.sp,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
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

    Surface(
        color = Color(0xFF0A0F1D).copy(alpha = 0.96f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0B132B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00F0FF).copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ROGUE TD: SECTOR ZERO",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Dual-Front Tactical Leaderboard & Hall of Fame",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF131F37),
                    contentColor = Color(0xFF00F0FF),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("LEADERBOARD", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("LIFETIME METRICS", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(state.leaderboard) { index, item ->
                            val medal = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "#${index + 1}"
                            }

                            Surface(
                                color = Color(0xFF162544),
                                shape = RoundedCornerShape(10.dp),
                                border = if (index == 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = medal,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(36.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Wave ${item.wavesCleared} Cleared",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (item.isVictory) "VICTORY" else "DEFEAT",
                                                color = if (item.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Text(
                                            text = "${item.enemiesKilled} Kills | MVP: ${item.mvpTower} | ${item.sectorName}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )

                                        Text(
                                            text = dateFormat.format(Date(item.timestamp)),
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val lt = state.lifetimeTelemetry
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TelemetryCard("Total Dual-Front Runs", "${lt.totalRuns}", Icons.Filled.PlayArrow, Color(0xFF00F0FF))
                        TelemetryCard("Total Hostiles Annihilated", "${lt.totalHostilesKilled}", Icons.Filled.CrisisAlert, Color(0xFFFF2A55))
                        TelemetryCard("Total Credits Harvested", "${lt.totalScrapMined}", Icons.Filled.Diamond, Color(0xFFFFD700))
                        TelemetryCard("Longest Wave Record", "Wave ${lt.bestWaveEver}", Icons.Filled.EmojiEvents, Color(0xFF00FF9D))
                        TelemetryCard("Highest Damage MVP", lt.mostUsedTower, Icons.Filled.MilitaryTech, Color(0xFFA855F7))
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

    Box(modifier = Modifier.fillMaxSize()) {
        for (ft in texts) {
            val alpha = (ft.lifeMs.toFloat() / ft.maxLifeMs).coerceIn(0f, 1f)
            val normX = (ft.x + 0.5f) / level.width.toFloat()
            val normY = (ft.y + 0.5f) / level.height.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
                    .offset(
                        x = (normX * 340f).dp,
                        y = (normY * 560f).dp
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
fun RunSummaryDialog(
    state: GameUIState,
    onRestart: () -> Unit,
    onViewTelemetry: () -> Unit
) {
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

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryStatRow("Waves Cleared", "${state.waveNumber - 1}")
                        SummaryStatRow("Hostiles Slain", "${state.totalKills}")
                        SummaryStatRow("Damage Output", "${state.totalDamageDealt.toInt()}")
                        SummaryStatRow("Credits Harvested", "${state.totalScrapEarned}")
                        SummaryStatRow("Ascended Towers", "${state.towers.count { it.isAscended }}")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0B132B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
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
                                text = "On-the-fly unit & economy balance sliders",
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
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "ECONOMY & INCOME SLIDERS",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Starting Scrap (Early Income Fix!)
                        item {
                            BalanceSliderCard(
                                title = "Starting Credits (Early Income)",
                                subtitle = "Lower credits early so you can't place endless towers without risk",
                                value = config.startingScrap.toFloat(),
                                range = 100f..600f,
                                steps = 9,
                                valueFormatter = { "${it.toInt()} C" },
                                color = Color(0xFFFFD700),
                                onValueChange = { onConfigChanged(config.copy(startingScrap = it.roundToInt())) }
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

                        // Wave Completion Bonus
                        item {
                            BalanceSliderCard(
                                title = "End-Of-Wave Bonus Multiplier",
                                subtitle = "Adjusts bonus credits granted upon wave clear",
                                value = config.waveBonusMultiplier,
                                range = 0.2f..2.5f,
                                steps = 22,
                                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                color = Color(0xFF00FF9D),
                                onValueChange = { onConfigChanged(config.copy(waveBonusMultiplier = (it * 100).roundToInt() / 100f)) }
                            )
                        }

                        // Tower Build Cost Multiplier
                        item {
                            BalanceSliderCard(
                                title = "Tower Build Cost Multiplier",
                                subtitle = "Scales base chassis placement costs across the grid",
                                value = config.buildCostMultiplier,
                                range = 0.5f..2.5f,
                                steps = 19,
                                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                color = Color(0xFF38BDF8),
                                onValueChange = { onConfigChanged(config.copy(buildCostMultiplier = (it * 100).roundToInt() / 100f)) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PER-TOWER CLASS COMBAT TUNING",
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Kinetic Ballistics (Circle) Slider
                        item {
                            BalanceSliderCard(
                                title = "● Kinetic Ballistics Damage (Circle)",
                                subtitle = "Rapid vulcan miniguns, flak, sniper sentries",
                                value = config.kineticDamageMultiplier,
                                range = 0.3f..3.0f,
                                steps = 26,
                                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                color = Color(0xFF00F0FF),
                                onValueChange = { onConfigChanged(config.copy(kineticDamageMultiplier = (it * 100).roundToInt() / 100f)) }
                            )
                        }

                        // Elemental Void (Triangle) Slider
                        item {
                            BalanceSliderCard(
                                title = "▲ Elemental Void Damage (Triangle)",
                                subtitle = "Cryo spire, tesla coil, graviton & quantum lasers",
                                value = config.elementalDamageMultiplier,
                                range = 0.3f..3.0f,
                                steps = 26,
                                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                color = Color(0xFFA855F7),
                                onValueChange = { onConfigChanged(config.copy(elementalDamageMultiplier = (it * 100).roundToInt() / 100f)) }
                            )
                        }

                        // Heavy Siege (Square) Slider
                        item {
                            BalanceSliderCard(
                                title = "■ Heavy Siege Damage (Square)",
                                subtitle = "Hellfire mortars, railguns, EMP disruptors",
                                value = config.heavyDamageMultiplier,
                                range = 0.3f..3.0f,
                                steps = 26,
                                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                color = Color(0xFFFF5500),
                                onValueChange = { onConfigChanged(config.copy(heavyDamageMultiplier = (it * 100).roundToInt() / 100f)) }
                            )
                        }

                        // Hostile HP Difficulty Slider
                        item {
                            BalanceSliderCard(
                                title = "Enemy Health Multiplier",
                                subtitle = "Increase hostile durability to test late-game pressure",
                                value = config.enemyHpMultiplier,
                                range = 0.5f..3.0f,
                                steps = 24,
                                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                color = Color(0xFFFF2A55),
                                onValueChange = { onConfigChanged(config.copy(enemyHpMultiplier = (it * 100).roundToInt() / 100f)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reset to Defaults Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onResetDefaults,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF2A55)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A55).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESET DEFAULTS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("APPLY & CLOSE", fontSize = 11.sp, fontWeight = FontWeight.Black)
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
fun PauseMenuDialog(
    state: GameUIState,
    onSpeedChanged: (GameSpeed) -> Unit,
    onOpenTelemetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onRestart: () -> Unit,
    onEndRun: () -> Unit,
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
                        Text(
                            text = "GAME PAUSED",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
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
                        Text("RESTART CURRENT RUN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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


