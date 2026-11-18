package com.example.ui.hud.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal val HUD_CAPSULE_SHAPE = RoundedCornerShape(8.dp)
internal val HUD_CAPSULE_HEIGHT = 28.dp
internal val HUD_ROTATED_CAPSULE_HEIGHT = 36.dp

internal fun isQuarterTurnRotation(angle: Float): Boolean {
    val normalized = ((angle % 360f) + 360f) % 360f
    return normalized in 45f..135f || normalized in 225f..315f
}

/**
 * Wraps a counter-rotated HUD capsule so its measured Compose layout bounds match its rotated
 * visual footprint, preventing 90/270 degree overlap or vertical clipping in the top HUD row.
 */
internal fun Modifier.hudCapsuleContainer(
    isRotated: Boolean,
    animatedRotation: Float
): Modifier = this
    .layout { measurable, constraints ->
        val childConstraints = if (isRotated) {
            constraints.copy(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth
            )
        } else {
            constraints
        }
        val placeable = measurable.measure(childConstraints)
        if (isRotated) {
            val outW = placeable.height
            val outH = placeable.width
            layout(outW, outH) {
                placeable.placeWithLayer(
                    x = (outW - placeable.width) / 2,
                    y = (outH - placeable.height) / 2
                ) {
                    rotationZ = animatedRotation
                }
            }
        } else {
            layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    rotationZ = animatedRotation
                }
            }
        }
    }

@Composable
internal fun HudClockHandle(
    isRotated: Boolean,
    animatedRotation: Float,
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
        color = Color(0xFF161F30).copy(alpha = 0.78f),
        shape = HUD_CAPSULE_SHAPE,
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .hudCapsuleContainer(isRotated, animatedRotation)
            .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
            .defaultMinSize(minWidth = if (isRotated) 36.dp else 1.dp)
            .clip(HUD_CAPSULE_SHAPE)
            .clickable(onClick = onOpenPauseMenu)
            .testTag("hud_pause_button")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp),
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
                    fontSize = if (isRotated) 8.sp else 8.5.sp,
                    lineHeight = 9.sp,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = ":",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isRotated) 8.sp else 8.5.sp,
                    lineHeight = 9.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.graphicsLayer { alpha = colonAlpha }
                )
                Text(
                    text = currentMinutes,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isRotated) 8.sp else 8.5.sp,
                    lineHeight = 9.sp,
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
internal fun HudWaveControlCapsule(
    autoWaveActive: Boolean,
    isWaveActive: Boolean,
    enemyCount: Int,
    isRotated: Boolean,
    animatedRotation: Float,
    onToggleAutoWave: () -> Unit,
    onDeployWave: () -> Unit
) {
    val activeGreen = Color(0xFF00FF9D)
    val inactiveSlate = Color(0xFF334155)
    val isWaveReady = !isWaveActive
    val pulseAlphaState = if (isWaveReady) {
        val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
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
    val pulseAlpha = pulseAlphaState?.value ?: 1f

    val borderColor = when {
        isWaveReady -> Color.White.copy(alpha = 0.85f * pulseAlpha)
        autoWaveActive -> activeGreen.copy(alpha = 0.85f)
        else -> inactiveSlate
    }

    Surface(
        color = Color(0xFF161F30).copy(alpha = 0.82f),
        shape = HUD_CAPSULE_SHAPE,
        border = BorderStroke(width = if (autoWaveActive || isWaveReady) 1.2.dp else 1.dp, color = borderColor),
        modifier = Modifier
            .hudCapsuleContainer(isRotated, animatedRotation)
            .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
            .clip(HUD_CAPSULE_SHAPE)
            .testTag("hud_wave_control_capsule")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 1. Integrated AUTO Toggle Segment (Left)
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .defaultMinSize(minWidth = if (isRotated) 32.dp else 1.dp)
                    .background(
                        if (autoWaveActive) Color(0xFF003822).copy(alpha = 0.90f)
                        else Color.Transparent
                    )
                    .clickable { onToggleAutoWave() }
                    .padding(horizontal = if (isRotated) 4.dp else 5.dp)
                    .testTag("auto_wave_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.5.dp)
                        .clip(CircleShape)
                        .then(
                            if (autoWaveActive) {
                                Modifier.background(activeGreen)
                            } else {
                                Modifier.border(1.1.dp, Color(0xFF64748B), CircleShape)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(2.5.dp))
                Text(
                    text = "AUTO",
                    color = if (autoWaveActive) activeGreen else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Black,
                    fontSize = 7.8.sp,
                    maxLines = 1
                )
            }

            // 1dp vertical divider between AUTO and BEGIN
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(if (autoWaveActive) activeGreen.copy(alpha = 0.55f) else Color(0xFF334155))
            )

            // 2. Main BEGIN / Active Enemy Count Segment (Right)
            val contentColor = if (isWaveReady) Color(0xFF040812) else Color(0xFF94A3B8)
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .defaultMinSize(minWidth = if (isRotated) 34.dp else 1.dp)
                    .background(
                        if (isWaveReady) Color(0xFF00F0FF).copy(alpha = 0.88f * pulseAlpha)
                        else Color(0xFF1E293B).copy(alpha = 0.80f)
                    )
                    .clickable(enabled = isWaveReady, onClick = onDeployWave)
                    .padding(horizontal = if (isRotated) 5.dp else 6.dp)
                    .testTag("deploy_wave_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isWaveActive) Icons.Filled.FastForward else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(10.5.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (isWaveActive) "$enemyCount" else "BEGIN",
                    color = contentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.2.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun HudAutoWaveToggle(
    autoWaveActive: Boolean,
    isRotated: Boolean,
    animatedRotation: Float,
    onToggleAutoWave: () -> Unit
) {
    val activeGreen = Color(0xFF00FF9D)
    val inactiveSlate = Color(0xFF334155)

    Surface(
        color = if (autoWaveActive) Color(0xFF003822).copy(alpha = 0.88f) else Color(0xFF161F30).copy(alpha = 0.78f),
        shape = HUD_CAPSULE_SHAPE,
        border = BorderStroke(
            width = if (autoWaveActive) 1.2.dp else 1.dp,
            color = if (autoWaveActive) activeGreen else inactiveSlate
        ),
        modifier = Modifier
            .hudCapsuleContainer(isRotated, animatedRotation)
            .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
            .defaultMinSize(minWidth = if (isRotated) 36.dp else 1.dp)
            .clip(HUD_CAPSULE_SHAPE)
            .clickable { onToggleAutoWave() }
            .testTag("auto_wave_toggle")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isRotated) 5.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .then(
                        if (autoWaveActive) {
                            Modifier.background(activeGreen)
                        } else {
                            Modifier.border(1.2.dp, Color(0xFF64748B), CircleShape)
                        }
                    )
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "AUTO",
                color = if (autoWaveActive) activeGreen else Color(0xFF94A3B8),
                fontWeight = FontWeight.Black,
                fontSize = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun HudLaunchWaveButton(
    isWaveActive: Boolean,
    enemyCount: Int,
    isRotated: Boolean,
    animatedRotation: Float,
    onDeployWave: () -> Unit
) {
    val isWaveReady = !isWaveActive
    val pulseAlphaState = if (isWaveReady) {
        val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
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

    val pulseAlpha = pulseAlphaState?.value ?: 1f

    Surface(
        color = if (isWaveReady) Color(0xFF00F0FF).copy(alpha = 0.85f * pulseAlpha) else Color(0xFF1E293B).copy(alpha = 0.78f),
        shape = HUD_CAPSULE_SHAPE,
        border = BorderStroke(
            width = 1.dp,
            color = if (isWaveReady) Color.White.copy(alpha = 0.85f * pulseAlpha) else Color(0xFF334155)
        ),
        modifier = Modifier
            .hudCapsuleContainer(isRotated, animatedRotation)
            .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
            .defaultMinSize(minWidth = if (isRotated) 36.dp else 1.dp)
            .clip(HUD_CAPSULE_SHAPE)
            .clickable(enabled = isWaveReady, onClick = onDeployWave)
            .testTag("deploy_wave_button")
    ) {
        val contentColor = if (isWaveReady) Color(0xFF040812) else Color(0xFF94A3B8)
        Row(
            modifier = Modifier.padding(horizontal = if (isRotated) 5.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isWaveActive) Icons.Filled.FastForward else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = if (isWaveActive) "$enemyCount" else "BEGIN",
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = 8.5.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun HudBatteryWidget(
    isRotated: Boolean,
    animatedRotation: Float,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        fun updateFromIntent(intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel = ((level * 100) / scale).coerceIn(0, 100)
                }
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) = updateFromIntent(intent)
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialSticky = context.registerReceiver(receiver, filter)
        updateFromIntent(initialSticky)
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    val chargeSweep = if (isCharging) {
        val infiniteTransition = rememberInfiniteTransition(label = "battery_charge")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "charge_sweep"
        )
    } else {
        null
    }

    val chargePulse = if (isCharging) {
        val infiniteTransition = rememberInfiniteTransition(label = "battery_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "charge_pulse"
        )
    } else {
        null
    }

    val batteryAccent = when {
        isCharging -> Color(0xFF00FF9D)
        batteryLevel <= 20 -> Color(0xFFFF2A55)
        else -> Color(0xFF00E676)
    }
    val pctFraction = (batteryLevel / 100f).coerceIn(0f, 1f)

    // Standalone battery cell graphic with percentage centered inside the body
    Box(
        modifier = Modifier
            .hudCapsuleContainer(isRotated, animatedRotation)
            .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
            .width(38.dp)
            .clip(HUD_CAPSULE_SHAPE)
            .clickable(onClick = onClick)
            .testTag("hud_battery_widget"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nubWidth = 2.5.dp.toPx()
            val strokeW = 1.2.dp.toPx()
            val bodyW = size.width - nubWidth - strokeW
            val bodyH = (size.height - 8.dp.toPx()).coerceAtLeast(16.dp.toPx())
            val bodyTop = (size.height - bodyH) / 2f
            val bodyLeft = strokeW / 2f
            val cornerR = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            // Dark translucent cell interior
            drawRoundRect(
                color = Color(0xFF0D1524).copy(alpha = 0.86f),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = cornerR
            )

            // Translucent charge level fill inside cell body (kept subtle for text contrast)
            val innerPad = 1.8.dp.toPx()
            val fillMaxW = (bodyW - innerPad * 2f).coerceAtLeast(0f)
            val fillW = fillMaxW * pctFraction
            val fillH = (bodyH - innerPad * 2f).coerceAtLeast(0f)
            if (fillW > 0f) {
                drawRoundRect(
                    color = batteryAccent.copy(alpha = if (isCharging) 0.24f else 0.20f),
                    topLeft = Offset(bodyLeft + innerPad, bodyTop + innerPad),
                    size = Size(fillW, fillH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }

            // Looping green sweep when charging
            if (isCharging && chargeSweep != null) {
                val sweepX = bodyLeft + innerPad + (fillMaxW * chargeSweep.value)
                val bandW = (fillMaxW * 0.35f).coerceAtLeast(4f)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00FF9D).copy(alpha = 0.42f),
                            Color.Transparent
                        ),
                        startX = sweepX - bandW,
                        endX = sweepX + bandW
                    ),
                    topLeft = Offset(bodyLeft + innerPad, bodyTop + innerPad),
                    size = Size(fillMaxW, fillH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }

            // Battery outline + terminal nub
            val borderAlpha = if (isCharging && chargePulse != null) chargePulse.value else 0.72f
            drawRoundRect(
                color = batteryAccent.copy(alpha = borderAlpha),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = cornerR,
                style = Stroke(width = strokeW)
            )
            val nubH = bodyH * 0.42f
            val nubTop = (size.height - nubH) / 2f
            drawRoundRect(
                color = batteryAccent.copy(alpha = borderAlpha),
                topLeft = Offset(bodyLeft + bodyW + 0.5.dp.toPx(), nubTop),
                size = Size(nubWidth, nubH),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }

        Text(
            text = "$batteryLevel%",
            color = if (batteryLevel <= 20 && !isCharging) Color(0xFFFF8A9B) else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 8.sp,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
            modifier = Modifier.padding(end = 2.5.dp)
        )
    }
}
