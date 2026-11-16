package com.example.ui.hud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.GameUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Bullet-Notification Floating Top Tactical HUD for Fox-Rogue-TD (Release v0.0.1.026).
 * Sequential layout order:
 * [1. Time Handle] | [2. Current Round] | [3. Core HP] | [4. Scrap Credits] |
 * [5. Synergies] | [6. Energy/Pads] | [7. Auto Wave] | [8. Begin Wave] | [9. Battery Widget]
 */
@Composable
fun MinimalTopHud(
    state: GameUIState,
    onDeployWave: () -> Unit,
    onToggleAutoWave: () -> Unit,
    onOpenPauseMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSynergyDropdown by remember { mutableStateOf(false) }
    var showPadsDropdown by remember { mutableStateOf(false) }

    // Live battery level and charging monitoring
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100) / scale
                    }
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialSticky = context.registerReceiver(receiver, filter)
        initialSticky?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100) / scale
            }
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

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
        color = Color(0xFF070B14).copy(alpha = 0.50f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Time Handle / Pause Trigger (isolated 1Hz clock leaf)
                HudClockHandle(
                    isRotated = isRotated,
                    counterRotateModifier = counterRotateModifier,
                    onOpenPauseMenu = onOpenPauseMenu
                )

                // 2. Wave badge (Dynamic: Inline Row in portrait, Stacked Column in landscape/rotation)
                if (isRotated) {
                    Column(
                        modifier = counterRotateModifier
                            .defaultMinSize(minWidth = 36.dp, minHeight = 44.dp),
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

                // 3. Core HP (Dynamic: Inline Row in portrait, Stacked Column in landscape/rotation)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 44.dp) else Modifier)
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
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
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

                // 4. Credits / Scrap (Dynamic: Inline Row in portrait, Stacked Column in landscape/rotation)
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 44.dp) else Modifier)
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
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
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

                // 5. Consolidated Synergies Indicator with Dropdown (Dynamic: Row in portrait, Column in landscape)
                val activeSynergiesCount = state.activeGlobalSynergies.size
                val capstoneTowers = remember(state.towers) { state.towers.filter { it.isCapstone } }
                val maxConduitGen = remember(capstoneTowers) { capstoneTowers.maxOfOrNull { it.generation } ?: 0 }
                val absorbedVaults = remember(capstoneTowers) { capstoneTowers.flatMap { it.synergyVault } }
                val absorbedSynergiesCount = remember(absorbedVaults) { absorbedVaults.sumOf { it.synergies.size } }
                // activeGlobalSynergies already includes banked capstone synergies via computeActiveSynergies()
                val totalSynergiesCount = activeSynergiesCount
                Box(modifier = counterRotateModifier) {
                    Surface(
                        color = if (totalSynergiesCount > 0) Color(0xFFFFB800).copy(alpha = 0.20f) else Color(0xFF161F30).copy(alpha = 0.60f),
                        shape = RoundedCornerShape(5.dp),
                        border = if (totalSynergiesCount > 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.6f)) else null,
                        modifier = Modifier
                            .then(if (isRotated) Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 44.dp) else Modifier)
                            .clip(RoundedCornerShape(5.dp))
                            .clickable { showSynergyDropdown = !showSynergyDropdown }
                    ) {
                        if (isRotated) {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🌟", fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "($totalSynergiesCount)",
                                    color = if (totalSynergiesCount > 0) Color(0xFFFFB800) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5.sp,
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
                                    text = "($totalSynergiesCount)",
                                    color = if (totalSynergiesCount > 0) Color(0xFFFFB800) else Color(0xFF94A3B8),
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
                                .widthIn(max = 260.dp)
                                .heightIn(max = 320.dp)
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
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                thickness = 1.dp,
                                color = Color(0xFF1E293B)
                            )
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(
                                    text = "⚡ ABSORBED CONDUIT SYNERGIES (GEN $maxConduitGen • $absorbedSynergiesCount Banked)",
                                    color = Color(0xFF00F0FF),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.5.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                if (absorbedVaults.isEmpty()) {
                                    Text(
                                        text = "No absorbed conduit synergies yet. Reach Tier 3 on all towers and ascend into a Conduit to permanently bank synergies.",
                                        color = Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp
                                    )
                                } else {
                                    absorbedVaults.forEach { vault ->
                                        vault.synergies.forEach { syn ->
                                            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = syn.iconEmoji, fontSize = 11.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${syn.auraName} [Gen ${vault.generation}]",
                                                        color = Color(syn.color),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Text(
                                                    text = "${syn.effectDescription} • [PERMANENT CORE BUFF]",
                                                    color = Color(0xFF00FF9D),
                                                    fontSize = 8.5.sp,
                                                    lineHeight = 11.sp
                                                )
                                            }
                                        }
                                    }
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
                                Column(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .heightIn(max = 340.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .verticalScroll(rememberScrollState())
                                    ) {
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
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            thickness = 1.dp,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "⚡ ABSORBED CONDUIT SYNERGIES (GEN $maxConduitGen • $absorbedSynergiesCount Banked)",
                                            color = Color(0xFF00F0FF),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        if (absorbedVaults.isEmpty()) {
                                            Text(
                                                text = "No absorbed conduit synergies yet. Reach Tier 3 on all towers and ascend into a Conduit to permanently bank synergies.",
                                                color = Color(0xFF64748B),
                                                fontSize = 9.sp,
                                                lineHeight = 12.sp
                                            )
                                        } else {
                                            absorbedVaults.forEach { vault ->
                                                vault.synergies.forEach { syn ->
                                                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(text = syn.iconEmoji, fontSize = 11.sp)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "${syn.auraName} [Gen ${vault.generation}]",
                                                                color = Color(syn.color),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                        Text(
                                                            text = "${syn.effectDescription} • [PERMANENT CORE BUFF]",
                                                            color = Color(0xFF00FF9D),
                                                            fontSize = 8.5.sp,
                                                            lineHeight = 11.sp
                                                        )
                                                    }
                                                }
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

                // 6. Teleportation Pads Dropdown Capsule (Energy)
                val padsCount = state.harmonicPadsCount
                val isHarmonicActive = state.harmonicGridActive
                val harmonicDmgPct = (state.harmonicGridDamageBoost * 100f).roundToInt()
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
                            .then(if (isRotated) Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 44.dp) else Modifier)
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
                                        text = "• 6/6 Harmonic Grid: +${harmonicDmgPct}% Tower Damage & +25% Bonus Scrap across all lanes",
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

                // 7. Auto Wave Toggle (Dynamic: Row in portrait, Column in landscape)
                val autoWaveActive = state.autoCallNextWave
                Surface(
                    color = if (autoWaveActive) Color(0xFF003822).copy(alpha = 0.90f) else Color(0xFF161F30).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF334155)
                    ),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 44.dp) else Modifier)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleAutoWave() }
                ) {
                    if (isRotated) {
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "AUTO",
                                color = if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF64748B))
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (autoWaveActive) "ON" else "OFF",
                                    color = if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp
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

                // 8. Launch Wave Button (isolated 60Hz pulse leaf)
                HudLaunchWaveButton(
                    isWaveActive = state.isWaveActive,
                    enemyCount = state.enemies.size,
                    isRotated = isRotated,
                    counterRotateModifier = counterRotateModifier,
                    onDeployWave = onDeployWave
                )

                // 9. Battery Widget (Dynamic: Inline Row in portrait, Stacked Column in landscape/rotation)
                val batteryColor = when {
                    isCharging -> Color(0xFF00F0FF) // Charging Cyan
                    batteryLevel <= 20 -> Color(0xFFFF2A55) // Critical Red
                    else -> Color(0xFF00FF9D) // Healthy Green
                }

                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, batteryColor.copy(alpha = 0.5f)),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 44.dp) else Modifier)
                ) {
                    if (isRotated) {
                        Column(
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isCharging) "⚡" else "🔋",
                                fontSize = 8.5.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "$batteryLevel%",
                                color = batteryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                maxLines = 1
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCharging) "⚡" else "🔋",
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$batteryLevel%",
                                color = batteryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudClockHandle(
    isRotated: Boolean,
    counterRotateModifier: Modifier,
    onOpenPauseMenu: () -> Unit
) {
    val context = LocalContext.current
    var currentHours by remember { mutableStateOf("") }
    var currentMinutes by remember { mutableStateOf("") }
    var colonAlpha by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(context) {
        var cachedIs24h: Boolean? = null
        var cachedFormatter: SimpleDateFormat? = null
        while (isActive) {
            val is24h = DateFormat.is24HourFormat(context)
            if (cachedFormatter == null || cachedIs24h != is24h) {
                cachedIs24h = is24h
                cachedFormatter = SimpleDateFormat(if (is24h) "HH mm" else "h mm", Locale.getDefault())
            }
            val formatted = cachedFormatter.format(Calendar.getInstance().time)
            val spaceIdx = formatted.indexOf(' ')
            if (spaceIdx > 0) {
                currentHours = formatted.substring(0, spaceIdx)
                currentMinutes = formatted.substring(spaceIdx + 1)
            }
            colonAlpha = if (colonAlpha > 0.5f) 0.15f else 1.0f
            delay(1000L)
        }
    }

    Surface(
        color = Color(0xFF161F30).copy(alpha = 0.75f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = counterRotateModifier
            .then(if (isRotated) Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 44.dp) else Modifier)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onOpenPauseMenu)
            .testTag("hud_pause_button")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentHours,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isRotated) 8.sp else 9.sp,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = ":",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isRotated) 8.sp else 9.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.graphicsLayer { alpha = colonAlpha }
                )
                Text(
                    text = currentMinutes,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isRotated) 8.sp else 9.sp,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
            Text(
                text = "≡",
                color = Color(0xFF94A3B8),
                fontSize = 6.5.sp,
                lineHeight = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HudLaunchWaveButton(
    isWaveActive: Boolean,
    enemyCount: Int,
    isRotated: Boolean,
    counterRotateModifier: Modifier,
    onDeployWave: () -> Unit
) {
    val isWaveReady = !isWaveActive
    val pulseAlphaState = if (isWaveReady) {
        val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "launch_pulse"
        )
    } else {
        null
    }

    Button(
        onClick = onDeployWave,
        enabled = isWaveReady,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.7f),
            contentColor = Color.Black,
            disabledContentColor = Color(0xFF64748B)
        ),
        border = null,
        contentPadding = PaddingValues(horizontal = if (isRotated) 3.dp else 6.dp, vertical = 1.dp),
        shape = RoundedCornerShape(6.dp),
        modifier = counterRotateModifier
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            .then(if (isRotated) Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 44.dp) else Modifier.height(26.dp))
            .then(
                if (pulseAlphaState != null) {
                    Modifier.drawBehind {
                        val radiusPx = 6.dp.toPx()
                        val cornerRadius = CornerRadius(radiusPx, radiusPx)
                        val alpha = pulseAlphaState.value
                        drawRoundRect(
                            color = Color(0xFF00F0FF).copy(alpha = alpha),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = alpha),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                } else {
                    Modifier
                }
            )
            .testTag("deploy_wave_button")
    ) {
        if (isRotated) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isWaveActive) Icons.Filled.FastForward else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = if (isWaveActive) "$enemyCount FOES" else "BEGIN",
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
                    imageVector = if (isWaveActive) Icons.Filled.FastForward else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (isWaveActive) "$enemyCount FOES" else "BEGIN",
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}
