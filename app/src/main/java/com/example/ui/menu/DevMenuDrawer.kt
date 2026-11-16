package com.example.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*

/**
 * Dev Balance Console & Live Combat Telemetry Drawer for Fox-Rogue-TD.
 * Curated by Tessa (Telemetry Specialist) & Dr. Alan (CS Architecture).
 */
@Composable
fun DevBalanceSettingsDialog(
    config: DevBalanceConfig,
    towerPerformance: List<TowerPerformanceRecord>,
    activeCreepCount: Int = 0,
    activeBulletCount: Int = 0,
    activeParticleCount: Int = 0,
    currentFps: Int = 60,
    frameJitterMs: Float = 0f,
    onConfigChanged: (DevBalanceConfig) -> Unit,
    onResetDefaults: () -> Unit,
    onResetTelemetry: () -> Unit,
    onSpawnSandboxWave: (EnemyType, Int) -> Unit = { _, _ -> },
    onLaunchCinematic: (CinematicScenario) -> Unit = {},
    onPlaySignatureWave: (CinematicScenario) -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = ECONOMY, 1 = 52 TOWERS, 2 = ENEMIES, 3 = PROBES, 4 = CINEMA
    var selectedCreepType by remember { mutableStateOf(EnemyType.DRONE) }
    var spawnCount by remember { mutableIntStateOf(5) }
    var showExportModal by remember { mutableStateOf(false) }
    var exportJsonText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !isLandscape)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.Center
        ) {
            Surface(
                color = Color(0xFF0B132B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.8f)),
                modifier = if (isLandscape) {
                    Modifier
                        .width(480.dp)
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
                                    text = "DEV BALANCE & TELEMETRY",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "v${com.example.BuildConfig.VERSION_NAME} • Live Balance Engine & Combat Probes",
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

                    // 5 Navigation Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF131F37),
                        contentColor = Color(0xFFFFB800),
                        edgePadding = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("ECONOMY", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, maxLines = 1, softWrap = false) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("52 TOWERS", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, maxLines = 1, softWrap = false) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("ENEMIES", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, maxLines = 1, softWrap = false) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("PROBES", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, maxLines = 1, softWrap = false) }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("CINEMA (6)", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, maxLines = 1, softWrap = false) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content Area for active tab
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> DevEconomyTab(
                                config = config,
                                onConfigChanged = onConfigChanged
                            )

                            1 -> {
                                val allDescriptors = remember { DevBalanceConfig.getAll52TowerDescriptors() }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "GRANULAR 52-TOWER STAT ADJUSTMENT",
                                            color = Color(0xFFFFB800),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "Fine-tune damage, range, fire rate, and custom callsigns for every evolution across all 4 tower archetypes.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 8.5.sp
                                        )
                                    }

                                    items(allDescriptors, key = { it.key }) { desc ->
                                        val balance = config.getEffectiveTowerBalance(desc)
                                        Collapsible52TowerCard(
                                            descriptor = desc,
                                            balance = balance,
                                            onBalanceChanged = { updatedBalance ->
                                                val newMap = config.customTowerConfigs.toMutableMap()
                                                newMap[desc.key] = updatedBalance
                                                onConfigChanged(config.copy(customTowerConfigs = newMap))
                                            },
                                            onResetToDefault = {
                                                val newMap = config.customTowerConfigs.toMutableMap()
                                                newMap.remove(desc.key)
                                                onConfigChanged(config.copy(customTowerConfigs = newMap))
                                            }
                                        )
                                    }
                                }
                            }

                            2 -> {
                                val allEnemies = remember { EnemyType.entries }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "ENEMY UNIT BALANCE & TRAITS",
                                            color = Color(0xFFFF2A55),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    }

                                    items(allEnemies, key = { it.name }) { enemyType ->
                                        val balance = config.getEnemyBalance(enemyType)
                                        CollapsibleEnemyBalanceCard(
                                            enemyType = enemyType,
                                            balance = balance,
                                            onBalanceChanged = { updated ->
                                                val newMap = config.customEnemyConfigs.toMutableMap()
                                                newMap[enemyType.name] = updated
                                                onConfigChanged(config.copy(customEnemyConfigs = newMap))
                                            }
                                        )
                                    }
                                }
                            }

                            3 -> DevProbesTab(
                                towerPerformance = towerPerformance,
                                activeCreepCount = activeCreepCount,
                                activeBulletCount = activeBulletCount,
                                activeParticleCount = activeParticleCount,
                                currentFps = currentFps,
                                frameJitterMs = frameJitterMs,
                                selectedCreepType = selectedCreepType,
                                spawnCount = spawnCount,
                                onSelectCreepType = { selectedCreepType = it },
                                onSpawnCountChanged = { spawnCount = it },
                                onResetTelemetry = onResetTelemetry,
                                onSpawnSandboxWave = onSpawnSandboxWave
                            )

                            4 -> DevCinemaTab(
                                context = context,
                                onLaunchCinematic = onLaunchCinematic,
                                onPlaySignatureWave = onPlaySignatureWave,
                                onDismiss = onDismiss
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Footer Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onResetDefaults,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("RESET DEFAULTS", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                exportJsonText = config.exportDiffJson()
                                showExportModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800), contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT JSON", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    if (showExportModal) {
        DevExportBalanceDialog(
            exportJsonText = exportJsonText,
            context = context,
            onDismiss = { showExportModal = false }
        )
    }
}
