package com.example.ui.hud.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun HudClockHandle(
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
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = counterRotateModifier
            .then(if (isRotated) Modifier.defaultMinSize(minWidth = 34.dp, minHeight = 44.dp) else Modifier)
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
internal fun HudAutoWaveToggle(
    autoWaveActive: Boolean,
    isRotated: Boolean,
    counterRotateModifier: Modifier,
    onToggleAutoWave: () -> Unit
) {
    Surface(
        color = if (autoWaveActive) Color(0xFF003822).copy(alpha = 0.90f) else Color(0xFF161F30).copy(alpha = 0.75f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.2.dp,
            if (autoWaveActive) Color(0xFF00FF9D) else Color(0xFF334155)
        ),
        modifier = counterRotateModifier
            .then(if (isRotated) Modifier.defaultMinSize(minWidth = 34.dp, minHeight = 44.dp) else Modifier)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
internal fun HudLaunchWaveButton(
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
            .then(if (isRotated) Modifier.defaultMinSize(minWidth = 34.dp, minHeight = 44.dp) else Modifier.height(26.dp))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
internal fun HudBatteryWidget(
    isRotated: Boolean,
    counterRotateModifier: Modifier
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
                    batteryLevel = (level * 100) / scale
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

    val batteryColor = when {
        isCharging -> Color(0xFF00F0FF)
        batteryLevel <= 20 -> Color(0xFFFF2A55)
        else -> Color(0xFF00FF9D)
    }

    Surface(
        color = Color(0xFF161F30).copy(alpha = 0.70f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, batteryColor.copy(alpha = 0.5f)),
        modifier = counterRotateModifier
            .then(if (isRotated) Modifier.defaultMinSize(minWidth = 34.dp, minHeight = 44.dp) else Modifier)
    ) {
        if (isRotated) {
            Column(
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = if (isCharging) "⚡" else "🔋", fontSize = 8.5.sp)
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
                Text(text = if (isCharging) "⚡" else "🔋", fontSize = 9.sp)
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
