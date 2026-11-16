package com.example.ui.hud.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Edge-bleeding "Bullet Tip" tab for one-handed teleportation swaps and manual rotation hints.
 * Keeps a full 48x48dp touch target inside screen bounds while visually bleeding the pill off-edge.
 * Styled to match the top bullet HUD capsules (`0xFF161F30` @ 86% fill, 1.2dp accent border).
 *
 * When [isPulsing] is true the icon gently pulses (alpha 0.65–1.0, scale 0.96–1.06). It defaults to
 * false so isolated unit tests stay deterministic; tests that enable it must pause the main clock.
 */
@Composable
internal fun EdgeBulletTipTab(
    isLeftEdge: Boolean,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00F0FF),
    counterRotation: Float = 0f,
    tag: String? = null,
    isPulsing: Boolean = false,
    onClick: () -> Unit
) {
    val tipShape = if (isLeftEdge) {
        RoundedCornerShape(topEnd = 19.dp, bottomEnd = 19.dp, topStart = 0.dp, bottomStart = 0.dp)
    } else {
        RoundedCornerShape(topStart = 19.dp, bottomStart = 19.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    }

    val pulseAlpha: Float
    val pulseScale: Float
    if (isPulsing) {
        val transition = rememberInfiniteTransition(label = "edge_tab_pulse")
        val alphaState = transition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "edge_tab_pulse_alpha"
        )
        val scaleState = transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "edge_tab_pulse_scale"
        )
        pulseAlpha = alphaState.value
        pulseScale = scaleState.value
    } else {
        pulseAlpha = 1f
        pulseScale = 1f
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clipToBounds()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = if (isLeftEdge) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .size(width = 28.dp, height = 38.dp)
                .offset(x = if (isLeftEdge) (-6).dp else 6.dp),
            shape = tipShape,
            color = Color(0xFF161F30).copy(alpha = 0.86f),
            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.85f)),
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isLeftEdge) 6.dp else 0.dp,
                        end = if (isLeftEdge) 0.dp else 6.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = accentColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            rotationZ = counterRotation
                            alpha = pulseAlpha
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                )
            }
        }
    }
}
