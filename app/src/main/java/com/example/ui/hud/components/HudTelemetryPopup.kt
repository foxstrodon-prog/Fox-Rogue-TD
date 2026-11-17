package com.example.ui.hud.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs

/**
 * Maps the HUD counter-rotation (0 / -90 / 180 / 90) back to the device rotation bucket
 * (0 / 90 / 180 / 270 degrees). Used as a fallback when the explicit device angle is not supplied.
 */
internal fun popupDeviceAngleFromCounterRotation(counterRotation: Float): Int = when {
    abs(counterRotation) >= 135f -> 180
    counterRotation <= -45f -> 90
    counterRotation >= 45f -> 270
    else -> 0
}

/**
 * Unified, orientation-aware dark-glass telemetry popup shared by the Teleport Pads and
 * Active Synergies capsules. It is a single full-width [Dialog] whose container is anchored to the
 * player's visual top edge for each rotation bucket and counter-rotated so it is always upright.
 *
 * - 0°:   TopCenter, slide down + fade + scale
 * - 90°:  CenterStart, slide from left + fade
 * - 180°: BottomCenter (visual top when upside down), slide up + fade
 * - 270°: CenterEnd (visual top when held at 270°), slide from right + fade
 */
@Composable
internal fun HudTelemetryPopup(
    deviceAngle: Int,
    animatedRotation: Float,
    accentColor: Color,
    popupTag: String,
    onDismiss: () -> Unit,
    scrollable: Boolean = false,
    safeInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable () -> Unit
) {
    val isRotated = deviceAngle == 90 || deviceAngle == 270
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    val alignment: Alignment
    val enter: EnterTransition
    val edgePadding: PaddingValues
    when (deviceAngle) {
        90 -> {
            alignment = Alignment.CenterStart
            enter = slideInHorizontally { -it / 3 } + fadeIn()
            edgePadding = PaddingValues(start = 12.dp)
        }
        180 -> {
            alignment = Alignment.BottomCenter
            enter = slideInVertically { it / 3 } + fadeIn()
            edgePadding = PaddingValues(bottom = 12.dp)
        }
        270 -> {
            alignment = Alignment.CenterEnd
            enter = slideInHorizontally { it / 3 } + fadeIn()
            edgePadding = PaddingValues(end = 12.dp)
        }
        else -> {
            alignment = Alignment.TopCenter
            enter = slideInVertically { -it / 3 } + fadeIn() + scaleIn(initialScale = 0.94f)
            edgePadding = PaddingValues(top = 48.dp)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .testTag("${popupTag}_scrim")
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(safeInsets)
            ) {
                val maxLocalWidth = if (isRotated) {
                    (maxHeight - 32.dp).coerceIn(260.dp, 340.dp)
                } else {
                    (maxWidth - 24.dp).coerceIn(260.dp, 340.dp)
                }
                val maxLocalHeight = if (isRotated) {
                    minOf((maxWidth - 36.dp).coerceAtLeast(180.dp), (maxLocalWidth - 20.dp).coerceAtLeast(180.dp))
                } else {
                    (maxHeight - 88.dp).coerceIn(200.dp, 460.dp)
                }

                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = enter,
                    exit = ExitTransition.None + fadeOut(),
                    modifier = Modifier
                        .align(alignment)
                        .padding(edgePadding)
                ) {
                    Surface(
                        color = Color(0xFF0A101D).copy(alpha = 0.96f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, accentColor),
                        modifier = Modifier
                            .testTag(popupTag)
                            .hudCapsuleContainer(isRotated, animatedRotation)
                            .widthIn(min = 260.dp, max = maxLocalWidth)
                            .heightIn(max = maxLocalHeight)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .then(
                                        if (scrollable) {
                                            Modifier
                                                .testTag("${popupTag}_scroll")
                                                .verticalScroll(rememberScrollState())
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                content()
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .testTag("${popupTag}_close"),
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
}
