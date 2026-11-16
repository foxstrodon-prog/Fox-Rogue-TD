package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val ROUND1_TILE_HINT_TEXT = "SELECT ANY TILE TO BUILD (Tap boosted pad)"
internal const val ROUND1_LAUNCH_HINT_TEXT = "DEFENSES READY — TAP [▶ BEGIN] TO START"

/**
 * Round-1 "select any tile" callout. Anchored by the caller (~3/4 screen height) and counter-rotated
 * so the text is never upside down / sideways when the device is held at 90°, 180° or 270°.
 */
@Composable
internal fun Round1TileHintBanner(
    counterRotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0A101D).copy(alpha = 0.88f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF00E676)),
        modifier = modifier
            .graphicsLayer { rotationZ = counterRotation }
            .testTag("round1_tile_hint_banner")
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.TouchApp,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = ROUND1_TILE_HINT_TEXT,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp
            )
        }
    }
}

/** Round-1 "press BEGIN" launch pointer callout shown under the top HUD, counter-rotated for 180°/90°/270°. */
@Composable
internal fun Round1LaunchBanner(
    counterRotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0A101D).copy(alpha = 0.90f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.85f)),
        modifier = modifier
            .graphicsLayer { rotationZ = counterRotation }
            .testTag("round1_launch_banner")
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF00F0FF),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = ROUND1_LAUNCH_HINT_TEXT,
                color = Color(0xFF00F0FF),
                fontWeight = FontWeight.Black,
                fontSize = 9.sp
            )
        }
    }
}
