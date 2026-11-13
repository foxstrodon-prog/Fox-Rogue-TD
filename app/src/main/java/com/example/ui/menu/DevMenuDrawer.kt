package com.example.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import java.util.Locale
import kotlin.math.roundToInt

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
    var selectedTab by remember { mutableStateOf(0) } // 0 = ECONOMY, 1 = 39 TOWERS, 2 = ENEMIES, 3 = TELEMETRY & SANDBOX
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
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.8f)),
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
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF131F37),
                        contentColor = Color(0xFFFFB800),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("ECONOMY", fontWeight = FontWeight.Bold, fontSize = 8.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("52 TOWERS", fontWeight = FontWeight.Bold, fontSize = 8.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("ENEMIES", fontWeight = FontWeight.Bold, fontSize = 8.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("PROBES", fontWeight = FontWeight.Bold, fontSize = 8.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("CINEMA (6)", fontWeight = FontWeight.Bold, fontSize = 8.5.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content Area for active tab
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> {
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

                                    item {
                                        BalanceSliderCard(
                                            title = "Overall Enemy Speed Multiplier",
                                            subtitle = "Scales traversal rate along paths",
                                            value = config.enemySpeedMultiplier,
                                            range = 0.5f..2.5f,
                                            steps = 19,
                                            valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                                            color = Color(0xFF38BDF8),
                                            onValueChange = { onConfigChanged(config.copy(enemySpeedMultiplier = (it * 100).roundToInt() / 100f)) }
                                        )
                                    }

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
                                }
                            }

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
                                        val balance = config.getEffectiveTowerBalance(desc.family, desc.branchTier1, desc.finalFormCode)
                                        Collapsible39TowerCard(
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

                            3 -> {
                                var selectedCreepType by remember { mutableStateOf(EnemyType.DRONE) }
                                var spawnCount by remember { mutableStateOf(5) }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        Surface(
                                            color = Color(0xFF131F37),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("FRAME ENGINE TELEMETRY", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                                    Text("$currentFps FPS", color = if (currentFps >= 55) Color(0xFF00FF9D) else Color(0xFFFF2A55), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Frame-Time Jitter: ${String.format(Locale.US, "%.1f ms", frameJitterMs)}", color = Color(0xFF94A3B8), fontSize = 9.sp)
                                                    Text("Active Creeps: $activeCreepCount", color = Color(0xFFFF2A55), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Active Projectiles: $activeBulletCount", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Text("Particles: $activeParticleCount", color = Color(0xFF00FF9D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Surface(
                                            color = Color(0xFF131F37),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("COMBAT DPS METER", color = Color(0xFFFFB800), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                                    TextButton(onClick = onResetTelemetry, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                                                        Text("RESET", color = Color(0xFF94A3B8), fontSize = 8.5.sp)
                                                    }
                                                }

                                                if (towerPerformance.isEmpty()) {
                                                    Text("No combat data logged yet for this run.", color = Color(0xFF64748B), fontSize = 8.5.sp)
                                                } else {
                                                    towerPerformance.take(6).forEach { rec ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(rec.towerName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            Text("${rec.totalDamage.toInt()} dmg (${rec.totalKills} kills)", color = Color(0xFF00FF9D), fontSize = 8.5.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Surface(
                                            color = Color(0xFF131F37),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("SPAWN WAVE SANDBOX", color = Color(0xFFA855F7), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                                Text("Spawn custom test formations instantly into the active map lanes.", color = Color(0xFF94A3B8), fontSize = 8.5.sp)

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    listOf(EnemyType.DRONE, EnemyType.SOLDIER, EnemyType.SPEEDSTER, EnemyType.BRUISER, EnemyType.BOSS).forEach { type ->
                                                        val isSel = (selectedCreepType == type)
                                                        Surface(
                                                            color = if (isSel) Color(type.color) else Color(0xFF0F172A),
                                                            shape = RoundedCornerShape(4.dp),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(type.color) else Color(0xFF334155)),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable { selectedCreepType = type }
                                                        ) {
                                                            Text(
                                                                text = type.displayName,
                                                                color = if (isSel) Color.Black else Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 7.5.sp,
                                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Spawn Count: $spawnCount units", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Slider(
                                                    value = spawnCount.toFloat(),
                                                    onValueChange = { spawnCount = it.roundToInt() },
                                                    valueRange = 1f..20f,
                                                    steps = 19,
                                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFA855F7), activeTrackColor = Color(0xFFA855F7))
                                                )

                                                Button(
                                                    onClick = { onSpawnSandboxWave(selectedCreepType, spawnCount) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("SPAWN TEST FORMATION", fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "IN-GAME FEATURE CINEMATICS (SHOWCASE MODE)",
                                            color = Color(0xFFFFB800),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "Deterministic, 1,800-frame (30.00s @ 60 FPS) scripted showcase scenarios highlighting core mechanics. Screen controls are locked during playback for pristine presentation.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 8.5.sp
                                        )
                                    }

                                    items(CinematicScenario.entries) { scenario ->
                                        Surface(
                                            color = Color(0xFF131F37),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = scenario.title,
                                                        color = Color(0xFFFFD700),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = "PEAK: ${scenario.thumbnailTimestampSec}s",
                                                        color = Color(0xFF00FF9D),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 8.5.sp
                                                    )
                                                }
                                                Text(
                                                    text = scenario.subtitle,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                                Text(
                                                    text = scenario.description,
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 8.sp,
                                                    lineHeight = 10.5.sp
                                                )
                                                Text(
                                                    text = "Target Clip: ${scenario.getTargetFilename()}",
                                                    color = Color(0xFF38BDF8),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 7.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                                            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Cinematic Filename", scenario.getTargetFilename()))
                                                            android.widget.Toast.makeText(context, "${scenario.getTargetFilename()} copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                                            onLaunchCinematic(scenario)
                                                            onDismiss()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFFFB800),
                                                            contentColor = Color.Black
                                                        ),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.weight(1f).height(28.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("SHOWCASE (30s)", fontWeight = FontWeight.Black, fontSize = 8.5.sp)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            onPlaySignatureWave(scenario)
                                                            onDismiss()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF00FF9D),
                                                            contentColor = Color.Black
                                                        ),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.weight(1f).height(28.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("PLAY WAVE ${scenario.targetWave}", fontWeight = FontWeight.Black, fontSize = 8.5.sp)
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

                    // Footer Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onResetDefaults,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
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
        Dialog(onDismissRequest = { showExportModal = false }) {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800)),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("EXPORT BALANCE PROFILE JSON", color = Color(0xFFFFB800), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("Copy this profile JSON to apply custom balance in dev builds or share presets.", color = Color(0xFF94A3B8), fontSize = 9.sp)
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) {
                        Text(
                            text = exportJsonText,
                            color = Color(0xFF00FF9D),
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("RogueTD_BalanceDiff", exportJsonText)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Balance JSON copied!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("COPY", fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                        }
                        Button(
                            onClick = { showExportModal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                        }
                    }
                }
            }
        }
    }
}
