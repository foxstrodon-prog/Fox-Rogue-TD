package com.example.ui.hud.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun HudTeleportPadsCapsule(
    padsCount: Int,
    isHarmonicActive: Boolean,
    harmonicDmgPct: Int,
    isRotated: Boolean,
    animatedRotation: Float,
    counterRotateModifier: Modifier
) {
    var showPadsDropdown by remember { mutableStateOf(false) }

    Box(modifier = counterRotateModifier) {
        Surface(
            color = if (isHarmonicActive) Color(0xFFFFD700).copy(alpha = 0.25f)
            else if (padsCount > 0) Color(0xFF00E5FF).copy(alpha = 0.20f)
            else Color(0xFF161F30).copy(alpha = 0.60f),
            shape = RoundedCornerShape(5.dp),
            border = if (isHarmonicActive) BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.8f))
            else if (padsCount > 0) BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f))
            else null,
            modifier = Modifier
                .then(if (isRotated) Modifier.defaultMinSize(minWidth = 34.dp, minHeight = 44.dp) else Modifier)
                .clip(RoundedCornerShape(5.dp))
                .clickable { showPadsDropdown = !showPadsDropdown }
        ) {
            if (isRotated) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "⚡", fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "($padsCount/6)",
                        color = if (isHarmonicActive) Color(0xFFFFD700) else if (padsCount > 0) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            } else {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⚡", fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "($padsCount/6)",
                        color = if (isHarmonicActive) Color(0xFFFFD700) else if (padsCount > 0) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }

        if (!isRotated) {
            DropdownMenu(
                expanded = showPadsDropdown,
                onDismissRequest = { showPadsDropdown = false },
                modifier = Modifier
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(1.dp, if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                    .widthIn(max = 250.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        TeleportPadsBreakdownContent(
                            padsCount = padsCount,
                            isHarmonicActive = isHarmonicActive,
                            harmonicDmgPct = harmonicDmgPct
                        )
                    },
                    onClick = { showPadsDropdown = false }
                )
            }
        }
    }

    if (isRotated && showPadsDropdown) {
        Dialog(
            onDismissRequest = { showPadsDropdown = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showPadsDropdown = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.98f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF00E5FF)),
                    modifier = Modifier
                        .graphicsLayer { rotationZ = animatedRotation }
                        .widthIn(min = 300.dp, max = 340.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = true
                        ) {}
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TeleportPadsBreakdownContent(
                            padsCount = padsCount,
                            isHarmonicActive = isHarmonicActive,
                            harmonicDmgPct = harmonicDmgPct
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showPadsDropdown = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("CLOSE", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeleportPadsBreakdownContent(
    padsCount: Int,
    isHarmonicActive: Boolean,
    harmonicDmgPct: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (isHarmonicActive) "⚡ HARMONIC GRID ACTIVE (6/6)" else "⚡ TELEPORTATION PADS ($padsCount/6)",
            color = if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF00E5FF),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "• Scrap Boost: +${padsCount * 3}% (+3% per active teleportation pad)",
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp
        )
        Text(
            text = "• 6/6 Harmonic Grid: +${harmonicDmgPct}% Tower Damage & +25% Bonus Scrap across all lanes",
            color = if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Tactical Mechanics:\nPads recharge when towers fire. Use Dual-Thumb buttons to teleport pads across Front Alpha & Omega lanes.",
            color = Color(0xFFCBD5E1),
            fontSize = 8.5.sp,
            lineHeight = 11.sp
        )
    }
}
