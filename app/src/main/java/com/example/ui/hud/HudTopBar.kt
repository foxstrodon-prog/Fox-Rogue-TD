package com.example.ui.hud

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GameUIState
import com.example.ui.hud.components.*
import kotlin.math.roundToInt

/**
 * Bullet-Notification Floating Top Tactical HUD for Fox-Rogue-TD (Release v0.0.1.033).
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
    // Shortest-path counter-rotation: at 270 deg (phone tilted right), rotate +90 deg instead of -270 deg
    val normalizedAngle = ((state.deviceRotationAngle % 360f + 360f) % 360f).toInt()
    val counterAngle = when (normalizedAngle) {
        90 -> -90f
        180 -> 180f
        270 -> 90f
        else -> 0f
    }
    val isRotated = normalizedAngle == 90 || normalizedAngle == 270
    val animatedRotation by animateFloatAsState(
        targetValue = counterAngle,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "hud_counter_rotation"
    )

    val capstoneTowers = remember(state.towers) { state.towers.filter { it.isCapstone } }
    val maxConduitGen = remember(capstoneTowers) { capstoneTowers.maxOfOrNull { it.generation } ?: 0 }
    val absorbedVaults = remember(capstoneTowers) { capstoneTowers.flatMap { it.synergyVault } }
    val absorbedSynergiesCount = remember(absorbedVaults) {
        absorbedVaults.sumOf { vault -> vault.synergies.sumOf { it.sourceCount } }
    }
    val activeEnemyCount = maxOf(state.activeEnemyCount, state.enemies.size)
    val harmonicDmgPct = (state.harmonicGridDamageBoost * 100f).roundToInt()
    val isBossWave = state.currentWaveData?.isBossWave == true
    val waveAccent = if (isBossWave) Color(0xFFFF2A55) else Color(0xFF00F0FF)

    Surface(
        color = Color(0xFF070B14).copy(alpha = 0.62f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.75f)),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = maxWidth)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = if (isRotated) Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally) else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            // 1. Time Handle / Pause Trigger
            HudClockHandle(
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                onOpenPauseMenu = onOpenPauseMenu
            )

            // 2. Wave Capsule
            Surface(
                color = Color(0xFF161F30).copy(alpha = 0.78f),
                shape = HUD_CAPSULE_SHAPE,
                border = BorderStroke(1.dp, waveAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .hudCapsuleContainer(isRotated, animatedRotation)
                    .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
                    .defaultMinSize(minWidth = if (isRotated) 36.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(waveAccent)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isBossWave) "B-${state.waveNumber}" else "W-${state.waveNumber}",
                        color = if (isBossWave) waveAccent else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.5.sp,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
            }

            // 3. Core HP Capsule
            Surface(
                color = Color(0xFF161F30).copy(alpha = 0.78f),
                shape = HUD_CAPSULE_SHAPE,
                border = BorderStroke(1.dp, Color(0xFFFF2A55).copy(alpha = 0.4f)),
                modifier = Modifier
                    .hudCapsuleContainer(isRotated, animatedRotation)
                    .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
                    .defaultMinSize(minWidth = if (isRotated) 36.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Core HP",
                        tint = Color(0xFFFF2A55),
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${state.coreHp}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
            }

            // 4. Credits / Scrap Capsule
            Surface(
                color = Color(0xFF161F30).copy(alpha = 0.78f),
                shape = HUD_CAPSULE_SHAPE,
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
                modifier = Modifier
                    .hudCapsuleContainer(isRotated, animatedRotation)
                    .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
                    .defaultMinSize(minWidth = if (isRotated) 36.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Diamond,
                        contentDescription = "Credits",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${state.scrap}",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black,
                        fontSize = 8.5.sp,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
            }

            // 5. Consolidated Synergies Indicator
            HudSynergyCapsule(
                activeGlobalSynergies = state.activeGlobalSynergies,
                activeBoardSynergies = state.activeBoardSynergies,
                maxConduitGen = maxConduitGen,
                absorbedVaults = absorbedVaults,
                absorbedSynergiesCount = absorbedSynergiesCount,
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                deviceAngle = normalizedAngle
            )

            // 6. Teleportation Pads Dropdown Capsule
            HudTeleportPadsCapsule(
                padsCount = state.harmonicPadsCount,
                isHarmonicActive = state.harmonicGridActive,
                harmonicDmgPct = harmonicDmgPct,
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                deviceAngle = normalizedAngle
            )

            // 7. Auto Wave Toggle
            HudAutoWaveToggle(
                autoWaveActive = state.autoCallNextWave,
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                onToggleAutoWave = onToggleAutoWave
            )

            // 8. Launch Wave Button
            HudLaunchWaveButton(
                isWaveActive = state.isWaveActive,
                enemyCount = activeEnemyCount,
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                onDeployWave = onDeployWave
            )

            // 9. Standalone Battery Cell Widget
            HudBatteryWidget(
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                onClick = onOpenPauseMenu
            )
            }
        }
    }
}
