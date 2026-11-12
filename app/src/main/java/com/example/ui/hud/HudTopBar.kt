package com.example.ui.hud

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.GameUIState

/**
 * Bullet-Notification Floating Top Tactical HUD for Fox-Rogue-TD.
 * Features an orientation-resilient capsule container, 25% higher transparency margins,
 * 6-digit scrap credit headroom, and an integrated teleportation pad tracker (PADS: 0/6).
 * Counter-rotates internal icons and text when the device turns to landscape (Track 1).
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
    var showPadsDropdown by remember { mutableStateOf(false) }

    // Energetic tactical pulse for wave launch readiness
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "launch_pulse"
    )

    // Shortest-path counter-rotation: at 270 deg (phone tilted right), rotate +90 deg instead of -270 deg
    val counterAngle = when (((state.deviceRotationAngle % 360f + 360f) % 360f).toInt()) {
        90 -> -90f
        180 -> 180f
        270 -> 90f
        else -> 0f
    }
    val isRotated = counterAngle != 0f
    val animatedRotation by animateFloatAsState(
        targetValue = counterAngle,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "hud_counter_rotation"
    )
    val counterRotateModifier = Modifier.graphicsLayer { rotationZ = animatedRotation }

    Surface(
        color = Color(0xFF070B14).copy(alpha = 0.50f), // +25% transparency margin over previous 0.65f
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wave badge (Dynamic: Inline Row in portrait, Stacked Column in landscape/rotation)
                if (isRotated) {
                    Column(
                        modifier = counterRotateModifier
                            .size(width = 28.dp, height = 38.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (state.currentWaveData?.isBossWave == true) Color(0xFFFF2A55) else Color(0xFF00F0FF))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (state.currentWaveData?.isBossWave == true) "B-${state.waveNumber}" else "W-${state.waveNumber}",
                            color = if (state.currentWaveData?.isBossWave == true) Color(0xFFFF2A55) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp,
                            maxLines = 1
                        )
                    }
                } else {
                    Row(
                        modifier = counterRotateModifier,
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
                }

                // Core HP (Dynamic: Inline Row in portrait, Stacked Column in landscape/rotation)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.size(width = 28.dp, height = 38.dp) else Modifier)
                ) {
                    if (isRotated) {
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Core HP",
                                tint = Color(0xFFFF2A55),
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "${state.coreHp}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                maxLines = 1
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Core HP",
                                tint = Color(0xFFFF2A55),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${state.coreHp}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Credits / Scrap (Dynamic: Inline Row in portrait, Stacked Column in landscape/rotation)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.size(width = 28.dp, height = 38.dp) else Modifier)
                ) {
                    if (isRotated) {
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Diamond,
                                contentDescription = "Credits",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "${state.scrap}",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Diamond,
                                contentDescription = "Credits",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${state.scrap}",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Consolidated Synergies Indicator with Dropdown (Dynamic: Row in portrait, Column in landscape)
                val activeSynergiesCount = state.activeGlobalSynergies.size
                Box(modifier = counterRotateModifier) {
                    Surface(
                        color = if (activeSynergiesCount > 0) Color(0xFFFFB800).copy(alpha = 0.20f) else Color(0xFF161F30).copy(alpha = 0.60f),
                        shape = RoundedCornerShape(5.dp),
                        border = if (activeSynergiesCount > 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.6f)) else null,
                        modifier = Modifier
                            .then(if (isRotated) Modifier.size(width = 28.dp, height = 38.dp) else Modifier)
                            .clip(RoundedCornerShape(5.dp))
                            .clickable { showSynergyDropdown = !showSynergyDropdown }
                    ) {
                        if (isRotated) {
                            Column(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🌟", fontSize = 8.5.sp)
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "($activeSynergiesCount)",
                                    color = if (activeSynergiesCount > 0) Color(0xFFFFB800) else Color(0xFF94A3B8),
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
                                Text(text = "🌟", fontSize = 9.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "($activeSynergiesCount)",
                                    color = if (activeSynergiesCount > 0) Color(0xFFFFB800) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (!isRotated) {
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
                }

                if (isRotated && showSynergyDropdown) {
                    Dialog(
                        onDismissRequest = { showSynergyDropdown = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { showSynergyDropdown = false },
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Surface(
                                color = Color(0xFF0F172A).copy(alpha = 0.98f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB800)),
                                modifier = Modifier
                                    .padding(top = 52.dp)
                                    .graphicsLayer { rotationZ = animatedRotation }
                                    .widthIn(min = 320.dp, max = 360.dp)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                        enabled = true
                                    ) {}
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "ACTIVE SYNERGIES ($activeSynergiesCount)",
                                        color = Color(0xFFFFB800),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    if (state.activeGlobalSynergies.isEmpty()) {
                                        Text(
                                            text = "No active aura synergies.\nAscend towers to Tier 3 to project global buffs across the battlefield.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.5.sp,
                                            lineHeight = 13.sp
                                        )
                                    } else {
                                        state.activeGlobalSynergies.forEach { syn ->
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = syn.iconEmoji, fontSize = 12.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${syn.auraName} (${syn.sourceCount}x)",
                                                        color = Color(syn.color),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                                Text(
                                                    text = syn.effectDescription,
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 9.sp,
                                                    lineHeight = 12.sp
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showSynergyDropdown = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("CLOSE", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Teleportation Pads Dropdown Capsule (Dynamic: Row in portrait, Column in landscape)
                val padsCount = state.harmonicPadsCount
                val isHarmonicActive = state.harmonicGridActive
                Box(modifier = counterRotateModifier) {
                    Surface(
                        color = if (isHarmonicActive) Color(0xFFFFD700).copy(alpha = 0.25f)
                        else if (padsCount > 0) Color(0xFF00E5FF).copy(alpha = 0.20f)
                        else Color(0xFF161F30).copy(alpha = 0.60f),
                        shape = RoundedCornerShape(5.dp),
                        border = if (isHarmonicActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.8f))
                        else if (padsCount > 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f))
                        else null,
                        modifier = Modifier
                            .then(if (isRotated) Modifier.size(width = 28.dp, height = 38.dp) else Modifier)
                            .clip(RoundedCornerShape(5.dp))
                            .clickable { showPadsDropdown = !showPadsDropdown }
                    ) {
                        if (isRotated) {
                            Column(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "⚡", fontSize = 8.5.sp)
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "($padsCount/6)",
                                    color = if (isHarmonicActive) Color(0xFFFFD700) else if (padsCount > 0) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 7.5.sp,
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
                                    Text(
                                        text = if (isHarmonicActive) "⚡ HARMONIC GRID ACTIVE (6/6)" else "⚡ TELEPORTATION PADS ($padsCount/6)",
                                        color = if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                },
                                onClick = { },
                                enabled = false
                            )
                            DropdownMenuItem(
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "• Scrap Boost: +${padsCount * 3}% (+3% per active teleportation pad)",
                                            color = Color(0xFF00E5FF),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        )
                                        Text(
                                            text = "• 6/6 Harmonic Grid: +20% Tower Damage & +25% Bonus Scrap across all lanes",
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
                                },
                                onClick = { showPadsDropdown = false }
                            )
                        }
                    }
                }

                if (isRotated && showPadsDropdown) {
                    Dialog(
                        onDismissRequest = { showPadsDropdown = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { showPadsDropdown = false },
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Surface(
                                color = Color(0xFF0F172A).copy(alpha = 0.98f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF00E5FF)),
                                modifier = Modifier
                                    .padding(top = 52.dp)
                                    .graphicsLayer { rotationZ = animatedRotation }
                                    .widthIn(min = 320.dp, max = 360.dp)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                        enabled = true
                                    ) {}
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isHarmonicActive) "⚡ HARMONIC GRID ACTIVE (6/6)" else "⚡ TELEPORTATION PADS ($padsCount/6)",
                                        color = if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = "• Scrap Boost: +${padsCount * 3}% (+3% per active pad)",
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp
                                    )
                                    Text(
                                        text = "• 6/6 Harmonic Grid: +20% Tower Damage & +25% Bonus Scrap across all lanes",
                                        color = if (isHarmonicActive) Color(0xFFFFD700) else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tactical Mechanics:\nPads recharge when towers fire. Use Dual-Thumb buttons to teleport pads across Front Alpha & Omega lanes.",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showPadsDropdown = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("CLOSE", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Auto Wave Toggle (Tactile switch styling, stacks vertically when rotated)
                val autoWaveActive = state.autoCallNextWave
                Surface(
                    color = if (autoWaveActive) Color(0xFF003822).copy(alpha = 0.90f) else Color(0xFF161F30).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF334155)
                    ),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.size(width = 28.dp, height = 38.dp) else Modifier)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleAutoWave() }
                ) {
                    if (isRotated) {
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "AUTO",
                                color = if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF64748B))
                                )
                                Spacer(modifier = Modifier.width(1.5.dp))
                                Text(
                                    text = if (autoWaveActive) "ON" else "OFF",
                                    color = if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 7.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF64748B))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (autoWaveActive) "AUTO: ON" else "AUTO: OFF",
                                color = if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                // Launch Wave Button with energetic pulse & single arrow accent (Dynamic: Row in portrait, Column in landscape)
                val isWaveReady = !state.isWaveActive
                val launchContainerColor = if (isWaveReady) {
                    Color(0xFF00F0FF).copy(alpha = pulseGlow)
                } else {
                    Color(0xFF1E293B).copy(alpha = 0.7f)
                }

                Button(
                    onClick = onDeployWave,
                    enabled = isWaveReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = launchContainerColor,
                        disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.7f),
                        contentColor = Color.Black,
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    border = if (isWaveReady) androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = pulseGlow)) else null,
                    contentPadding = PaddingValues(horizontal = if (isRotated) 2.dp else 6.dp, vertical = 1.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = counterRotateModifier
                        .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                        .then(if (isRotated) Modifier.width(28.dp).height(38.dp) else Modifier.height(26.dp))
                        .testTag("deploy_wave_button")
                ) {
                    if (isRotated) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (state.isWaveActive) Icons.Filled.FastForward else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = if (state.isWaveActive) "${state.enemies.size} FOES" else "BEGIN",
                                fontWeight = FontWeight.Black,
                                fontSize = 7.5.sp,
                                maxLines = 1
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isWaveActive) Icons.Filled.FastForward else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (state.isWaveActive) "${state.enemies.size} FOES" else "BEGIN",
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Gear Cog Button (Clean borderless floating icon, stationary circular control)
                IconButton(
                    onClick = onOpenPauseMenu,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("pause_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Game Settings & Controls",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
