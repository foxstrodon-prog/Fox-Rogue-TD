package com.example.ui.menu

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.HeadlessBalanceSimulator
import com.example.engine.HeadlessSimReport
import com.example.model.CinematicScenario
import com.example.model.DevBalanceConfig
import com.example.model.EnemyType
import com.example.model.TowerPerformanceRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DevProbesTab(
    towerPerformance: List<TowerPerformanceRecord>,
    activeCreepCount: Int,
    activeBulletCount: Int,
    activeParticleCount: Int,
    currentFps: Int,
    frameJitterMs: Float,
    selectedCreepType: EnemyType,
    spawnCount: Int,
    onSelectCreepType: (EnemyType) -> Unit,
    onSpawnCountChanged: (Int) -> Unit,
    onResetTelemetry: () -> Unit,
    onSpawnSandboxWave: (EnemyType, Int) -> Unit,
    balanceConfig: DevBalanceConfig = DevBalanceConfig()
) {
    var simReports by remember { mutableStateOf<List<HeadlessSimReport>>(emptyList()) }
    var isSimRunning by remember { mutableStateOf(false) }
    val simScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = Color(0xFF131F37),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f)),
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

        // === HEADLESS 63-WAVE BALANCE VERIFICATION PROBE (TRACK 3B) ===
        item {
            Surface(
                color = Color(0xFF131F37),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF00FF9D).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HEADLESS BALANCE SIMULATOR", color = Color(0xFF00FF9D), fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Text("1 / 2-3 / 4-5 Collapse Targets", color = Color(0xFF94A3B8), fontSize = 8.sp)
                    }

                    Button(
                        onClick = {
                            if (!isSimRunning) {
                                isSimRunning = true
                                simScope.launch {
                                    try {
                                        simReports = withContext(Dispatchers.Default) {
                                            HeadlessBalanceSimulator.runAllBenchmarks(
                                                seed = 42L,
                                                balanceConfig = balanceConfig
                                            )
                                        }
                                    } finally {
                                        isSimRunning = false
                                    }
                                }
                            }
                        },
                        enabled = !isSimRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .testTag("run_headless_sim_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isSimRunning) "SIMULATING 63 WAVES..." else "RUN 63W HEADLESS SIM",
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                    }

                    if (simReports.isNotEmpty()) {
                        simReports.forEach { report ->
                            Surface(
                                color = Color(0xFF0B132B),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${report.difficulty.label} (W${report.waveReached}/${report.targetWave})",
                                            color = Color(0xFF00F0FF),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.5.sp
                                        )
                                        Text(
                                            text = "Collapses: ${report.totalCollapses}",
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.5.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Unspent Gold: ${report.unspentGoldAtEnd} c",
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 8.sp
                                        )
                                        Text(
                                            text = "Unique Auras Banked: ${report.uniqueAurasBanked}",
                                            color = Color(0xFF00FF9D),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // === DATA-DRIVEN COMBAT DPS & EFFICIENCY ANALYZER (TRACK 3C) ===
        item {
            Surface(
                color = Color(0xFF131F37),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("COMBAT DPS & BALANCE ANALYZER", color = Color(0xFFFFB800), fontWeight = FontWeight.Black, fontSize = 10.sp)
                        TextButton(onClick = onResetTelemetry, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text("RESET", color = Color(0xFF94A3B8), fontSize = 8.5.sp)
                        }
                    }

                    if (towerPerformance.isEmpty()) {
                        Text("No combat data logged yet for this run.", color = Color(0xFF64748B), fontSize = 8.5.sp)
                    } else {
                        val totalBuiltSum = towerPerformance.sumOf { it.totalBuilt }.coerceAtLeast(1)
                        val avgEfficiency = towerPerformance.map { it.dpsCostEfficiency }.average().coerceAtLeast(1.0)

                        towerPerformance.take(8).forEach { rec ->
                            val eff = rec.dpsCostEfficiency
                            val pickSharePct = ((rec.totalBuilt.toFloat() / totalBuiltSum.toFloat()) * 100f).roundToInt()
                            val (statusLabel, statusColor) = when {
                                eff > avgEfficiency * 1.30 -> "OVERPERFORMING" to Color(0xFFFF2A55)
                                eff < avgEfficiency * 0.70 -> "UNDERPERFORMING" to Color(0xFF38BDF8)
                                else -> "BALANCED" to Color(0xFF00FF9D)
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rec.towerName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = statusLabel,
                                        color = statusColor,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${rec.totalDamage.toInt()} dmg (${rec.totalKills} kills)",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 8.sp
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", eff)} DMG/c • $pickSharePct% pick",
                                        color = Color(0xFFFFD700),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
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
                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
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
                                border = BorderStroke(1.dp, if (isSel) Color(type.color) else Color(0xFF334155)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectCreepType(type) }
                            ) {
                                Text(
                                    text = type.displayName,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 7.5.sp,
                                    textAlign = TextAlign.Center,
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
                        onValueChange = { onSpawnCountChanged(it.roundToInt()) },
                        valueRange = 1f..20f,
                        steps = 18,
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

@Composable
fun DevCinemaTab(
    context: Context,
    onLaunchCinematic: (CinematicScenario) -> Unit,
    onPlaySignatureWave: (CinematicScenario) -> Unit,
    onDismiss: () -> Unit
) {
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
                border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f)),
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
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
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
