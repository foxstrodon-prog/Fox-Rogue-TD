package com.example.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GameUIState

/**
 * Bullet-Notification Floating Top Tactical HUD for Fox-Rogue-TD.
 * Features an orientation-resilient capsule container, 25% higher transparency margins,
 * 6-digit scrap credit headroom, and an integrated booster pad tracker (PADS: 0/6).
 */
@Composable
fun MinimalTopHud(
    state: GameUIState,
    onDeployWave: () -> Unit,
    onToggleAutoWave: () -> Unit,
    onOpenPauseMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSynergyDropdown by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF070B14).copy(alpha = 0.50f), // +25% transparency margin over previous 0.65f
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wave badge
                Row(
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

                // Core HP (Stacked Heart Icon + Count)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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

                // Credits (Formatted for 5-6 digits with high visibility)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
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

                // Consolidated Synergies Indicator with Dropdown
                val activeSynergiesCount = state.activeGlobalSynergies.size
                Box {
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
                        .clip(RoundedCornerShape(5.dp))
                        .clickable { onToggleAutoWave() }
                        .padding(1.dp)
                ) {
                    Text(
                        text = "AUTO",
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

            // Bullet Capsule Sub-strip: Booster Pad Status & Harmonic Grid
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.harmonicGridActive) "⚡ HARMONIC GRID ACTIVE: 6/6 (+20% DMG / +25% SCRAP)" else "PADS: ${state.harmonicPadsCount}/6 (${state.harmonicPadsCount * 3}% SCRAP BOOST)",
                    color = if (state.harmonicGridActive) Color(0xFFFFD700) else Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                )
            }
        }
    }
}
