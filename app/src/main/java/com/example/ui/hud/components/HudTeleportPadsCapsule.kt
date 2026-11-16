package com.example.ui.hud.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HudTeleportPadsCapsule(
    padsCount: Int,
    isHarmonicActive: Boolean,
    harmonicDmgPct: Int,
    isRotated: Boolean,
    animatedRotation: Float,
    deviceAngle: Int = popupDeviceAngleFromCounterRotation(animatedRotation)
) {
    var showPadsPopup by remember { mutableStateOf(false) }
    val activeTint = if (isHarmonicActive) Color(0xFFFFD700) else if (padsCount > 0) Color(0xFF00E5FF) else Color(0xFF94A3B8)

    Box(modifier = Modifier.hudCapsuleContainer(isRotated, animatedRotation)) {
        Surface(
            color = if (isHarmonicActive) Color(0xFFFFD700).copy(alpha = 0.25f)
            else if (padsCount > 0) Color(0xFF00E5FF).copy(alpha = 0.20f)
            else Color(0xFF161F30).copy(alpha = 0.78f),
            shape = HUD_CAPSULE_SHAPE,
            border = BorderStroke(
                width = 1.dp,
                color = if (isHarmonicActive) Color(0xFFFFD700).copy(alpha = 0.8f)
                else if (padsCount > 0) Color(0xFF00E5FF).copy(alpha = 0.6f)
                else Color(0xFF334155)
            ),
            modifier = Modifier
                .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
                .defaultMinSize(minWidth = if (isRotated) 36.dp else 1.dp)
                .clip(HUD_CAPSULE_SHAPE)
                .clickable { showPadsPopup = !showPadsPopup }
                .testTag("hud_teleport_pads_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = "Teleportation Pads",
                    tint = activeTint,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(1.dp))
                Text(
                    text = "($padsCount/6)",
                    color = activeTint,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1
                )
            }
        }

        if (showPadsPopup) {
            HudTelemetryPopup(
                deviceAngle = deviceAngle,
                animatedRotation = animatedRotation,
                accentColor = if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF00F0FF),
                popupTag = "hud_teleport_pads_popup",
                onDismiss = { showPadsPopup = false }
            ) {
                TeleportPadsBreakdownContent(
                    padsCount = padsCount,
                    isHarmonicActive = isHarmonicActive,
                    harmonicDmgPct = harmonicDmgPct
                )
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
