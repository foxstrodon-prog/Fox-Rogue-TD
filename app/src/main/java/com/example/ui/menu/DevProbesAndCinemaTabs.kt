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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CinematicScenario
import com.example.model.EnemyType
import com.example.model.TowerPerformanceRecord
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
    onSpawnSandboxWave: (EnemyType, Int) -> Unit
) {
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
