package com.example.ui.hud.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.GameUIState

/**
 * Tactical Warning Modal for Synergy Singularity Collapse (Minimal Bullet Dark-Glass UI).
 * Warns player before sacrificing active towers to forge/ascend the Capstone Monument.
 */
@Composable
fun SingularityWarningDialog(
    state: GameUIState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isSingularityWarningOpen) return
    val anchor = state.selectedTower?.takeIf { it.tier >= 3 || it.finalFormCode != null || it.isCapstone }
        ?: state.towers.firstOrNull { it.isCapstone }
        ?: state.towers.minByOrNull { it.id }
    val sacrificedCount = state.towers.count { it.id != anchor?.id }
    val totalInvested = state.towers.filter { it.id != anchor?.id }.sumOf { it.getInvestedScrapValue() }
    val scrapBounty = maxOf(250, (totalInvested * 0.20f).toInt())
    val currentMaxGen = state.towers.filter { it.isCapstone }.maxOfOrNull { it.generation } ?: 0
    val conduitCost = 750 * (currentMaxGen + 1)

    val normalizedAngle = ((state.deviceRotationAngle % 360f + 360f) % 360f).toInt()
    val targetAngle = when (normalizedAngle) {
        90 -> -90f
        180 -> 180f
        270 -> 90f
        else -> 0f
    }
    val animatedRotation by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "singularity_dialog_rotation"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF0A101D).copy(alpha = 0.94f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFF3B30).copy(alpha = 0.85f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .graphicsLayer { rotationZ = animatedRotation }
                    .widthIn(min = 290.dp, max = 340.dp)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = true
                    ) {}
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Singularity Warning",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SYNERGY SINGULARITY",
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "CONDUIT GEN ${currentMaxGen + 1} ASCENSION",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    // Tabular telemetry readout
                    val anchorTile = state.level?.tiles?.firstOrNull { it.pos == anchor?.gridPos }
                    val isBoosterSpot = anchorTile != null && anchorTile.buff != com.example.model.TileBuff.NONE
                    Surface(
                        color = Color(0xFF131C2E).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SingularityTelemetryRow(
                                label = "TOWERS SACRIFICED",
                                value = "$sacrificedCount TURRETS",
                                valueColor = Color.White
                            )
                            SingularityTelemetryRow(
                                label = "ASCENSION COST",
                                value = "-$conduitCost c",
                                valueColor = Color(0xFFFF2A55)
                            )
                            SingularityTelemetryRow(
                                label = "REFUND BOUNTY (20%)",
                                value = "+$scrapBounty c",
                                valueColor = Color(0xFFFFD700)
                            )
                            SingularityTelemetryRow(
                                label = "BATTLEFIELD RESET",
                                value = if (isBoosterSpot) "NEW MAP + 2 BOOSTER PADS" else "NEW MAP + KEEP SPOT",
                                valueColor = Color(0xFF00F0FF)
                            )
                            SingularityTelemetryRow(
                                label = "GLOBAL AURA VAULT",
                                value = "PERMANENT",
                                valueColor = Color(0xFF00FF9D)
                            )
                        }
                    }

                    // Wave state advisory badge
                    Surface(
                        color = if (state.isWaveActive) Color(0xFFFF3B30).copy(alpha = 0.14f) else Color(0xFF00F0FF).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (state.isWaveActive) Color(0xFFFF3B30).copy(alpha = 0.6f) else Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isWaveActive) Icons.Filled.Warning else Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = if (state.isWaveActive) Color(0xFFFF5722) else Color(0xFF00F0FF),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isWaveActive) {
                                    "ACTIVE WAVE: New map generates & active enemies reset to spawners! Defense drops to 1 Singularity until you rebuild."
                                } else {
                                    "INTERMISSION SAFE: Generates a new map while preserving your Singularity spot${if (isBoosterSpot) " & linked booster pair" else ""}."
                                },
                                color = if (state.isWaveActive) Color(0xFFFFAB91) else Color(0xFFCBD5E1),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    // Squircle action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF161F30).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onDismiss)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "ABORT",
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFFF2A55),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF8A9B).copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onConfirm)
                                .testTag("confirm_singularity_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "COLLAPSE BOARD",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.4.sp
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
private fun SingularityTelemetryRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}
