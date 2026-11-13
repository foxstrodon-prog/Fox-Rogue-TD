package com.example.ui.hud.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.GameUIState

/**
 * Tactical Warning Modal for Synergy Singularity Collapse (v0.0.1.015).
 * Warns player before sacrificing all active towers to forge the Capstone Monument.
 */
@Composable
fun SingularityWarningDialog(
    state: GameUIState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isSingularityWarningOpen) return
    val anchor = state.selectedTower?.takeIf { it.tier >= 2 || it.finalFormCode != null }
        ?: state.towers.firstOrNull { it.isCapstone }
        ?: state.towers.minByOrNull { it.id }
    val sacrificedCount = state.towers.count { it.id != anchor?.id }
    val totalInvested = state.towers.filter { it.id != anchor?.id }.sumOf { it.getInvestedScrapValue() }
    val scrapBounty = maxOf(250, (totalInvested * 0.20f).toInt())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color(0xFFFF3B30)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "SYNERGY SINGULARITY",
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "COLLAPSE SACRIFICE SUMMARY",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            "• Sacrificing $sacrificedCount maxed defensive turrets",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(
                            "• Permanent Bank: ALL active auras stored into Capstone",
                            color = Color(0xFF00FF9D),
                            fontSize = 11.sp
                        )
                        Text(
                            "• Singularity Scrap Bounty: +$scrapBounty Scrap (20% Refund)",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.isWaveActive) {
                            Text(
                                "⚠️ HIGH-STAKES WARNING: Active wave creeps will NOT be despawned! Your defenses will be down to 1 Capstone tower until you rebuild!",
                                color = Color(0xFFFF5722),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "• Tactical Intermission: Clears board so you can draft a brand-new generation of turrets before the next wave.",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF64748B))
                    ) {
                        Text("ABORT", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.3f).height(42.dp).testTag("confirm_singularity_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A55), contentColor = Color.White)
                    ) {
                        Text("COLLAPSE BOARD", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
