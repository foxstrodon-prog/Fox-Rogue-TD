package com.example.ui.menu

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

                    // 4 Navigation Tabs
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

                                    // Enemies Per Round Multiplier
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
                                }
                            }

                            1 -> {
                                // Tab 1: 52 Tower Live-Tuning Cards
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
                                // Tab 2: Enemy Unit Balance Cards
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
                                // Tab 3: Tessa's Live Telemetry Probes & Spawn Wave Sandbox
                                var selectedCreepType by remember { mutableStateOf(EnemyType.DRONE) }
                                var spawnCount by remember { mutableStateOf(5) }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Real-Time Performance & Jitter Probes
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

                                    // 2. Live DPS Meter per Active Tower Archetype
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

                                    // 3. Spawn Wave Sandbox Controls
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

                                                // Creep Type Selector
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

                                                // Quantity Slider
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
    var showCustomInput by remember { mutableStateOf(false) }
    var textInput by remember(value) { mutableStateOf(String.format(java.util.Locale.US, "%.2f", value)) }

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
                    modifier = Modifier
                        .clickable { showCustomInput = !showCustomInput }
                        .padding(start = 6.dp)
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

            if (showCustomInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp),
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                            .border(1.dp, color, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    Button(
                        onClick = {
                            val parsed = textInput.toFloatOrNull()
                            if (parsed != null) {
                                onValueChange(parsed.coerceIn(range.start, range.endInclusive))
                            }
                            showCustomInput = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("SET", color = Color.Black, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showCustomInput = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("X", color = Color.White, fontSize = 8.5.sp)
                    }
                }
            }
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
                                    fontWeight = FontWeight.Black,
                                    fontSize = 7.5.sp,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = currentName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "DMG: ${balance.damage.toInt()} | RNG: ${String.format(Locale.US, "%.1f", balance.range)}u | SPD: ${String.format(Locale.US, "%.1f", balance.fireRate)}/s",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp
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
                        contentDescription = "Rename Tower",
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
                        title = "Base Bullet Damage",
                        subtitle = "Per-hit impact damage value",
                        value = balance.damage,
                        range = 5f..800f,
                        steps = 79,
                        valueFormatter = { "${it.toInt()} DMG" },
                        color = Color(0xFFFF2A55),
                        onValueChange = { onBalanceChanged(balance.copy(damage = it.roundToInt().toFloat())) }
                    )

                    BalanceSliderCard(
                        title = "Targeting Engagement Range",
                        subtitle = "Combat radius in grid units",
                        value = balance.range,
                        range = 1.0f..8.0f,
                        steps = 70,
                        valueFormatter = { String.format(Locale.US, "%.1f u", it) },
                        color = Color(0xFF00F0FF),
                        onValueChange = { onBalanceChanged(balance.copy(range = (it * 10).roundToInt() / 10f)) }
                    )

                    BalanceSliderCard(
                        title = "Firing Cadence / Speed",
                        subtitle = "Shots fired per second",
                        value = balance.fireRate,
                        range = 0.2f..15.0f,
                        steps = 74,
                        valueFormatter = { String.format(Locale.US, "%.1f /s", it) },
                        color = Color(0xFF00FF9D),
                        onValueChange = { onBalanceChanged(balance.copy(fireRate = (it * 10).roundToInt() / 10f)) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onResetToDefault,
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

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
