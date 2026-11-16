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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GameUIState
import com.example.ui.hud.components.*
import kotlin.math.roundToInt

/**
 * Bullet-Notification Floating Top Tactical HUD for Fox-Rogue-TD (Release v0.0.1.030).
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
    val counterRotateModifier = Modifier.graphicsLayer { rotationZ = animatedRotation }

    val capstoneTowers = remember(state.towers) { state.towers.filter { it.isCapstone } }
    val maxConduitGen = remember(capstoneTowers) { capstoneTowers.maxOfOrNull { it.generation } ?: 0 }
    val absorbedVaults = remember(capstoneTowers) { capstoneTowers.flatMap { it.synergyVault } }
    val absorbedSynergiesCount = remember(absorbedVaults) { absorbedVaults.sumOf { it.synergies.size } }
    val activeEnemyCount = maxOf(state.activeEnemyCount, state.enemies.size)
    val harmonicDmgPct = (state.harmonicGridDamageBoost * 100f).roundToInt()

    Surface(
        color = Color(0xFF070B14).copy(alpha = 0.50f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isRotated) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Time Handle / Pause Trigger
                HudClockHandle(
                    isRotated = isRotated,
                    counterRotateModifier = counterRotateModifier,
                    onOpenPauseMenu = onOpenPauseMenu
                )

                // 2. Wave badge
                if (isRotated) {
                    Column(
                        modifier = counterRotateModifier.defaultMinSize(minWidth = 34.dp, minHeight = 44.dp),
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

                // 3. Core HP
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.defaultMinSize(minWidth = 34.dp, minHeight = 44.dp) else Modifier)
                ) {
                    if (isRotated) {
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Core HP", tint = Color(0xFFFF2A55), modifier = Modifier.size(9.dp))
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(text = "${state.coreHp}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, maxLines = 1)
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Core HP", tint = Color(0xFFFF2A55), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "${state.coreHp}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, maxLines = 1)
                        }
                    }
                }

                // 4. Credits / Scrap
                Surface(
                    color = Color(0xFF161F30).copy(alpha = 0.70f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = counterRotateModifier
                        .then(if (isRotated) Modifier.defaultMinSize(minWidth = 34.dp, minHeight = 44.dp) else Modifier)
                ) {
                    if (isRotated) {
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Diamond, contentDescription = "Credits", tint = Color(0xFFFFD700), modifier = Modifier.size(9.dp))
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(text = "${state.scrap}", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 8.sp, maxLines = 1)
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Filled.Diamond, contentDescription = "Credits", tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "${state.scrap}", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 9.5.sp, maxLines = 1)
                        }
                    }
                }

                // 5. Consolidated Synergies Indicator
                HudSynergyCapsule(
                    activeGlobalSynergies = state.activeGlobalSynergies,
                    maxConduitGen = maxConduitGen,
                    absorbedVaults = absorbedVaults,
                    absorbedSynergiesCount = absorbedSynergiesCount,
                    isRotated = isRotated,
                    animatedRotation = animatedRotation,
                    counterRotateModifier = counterRotateModifier
                )

                // 6. Teleportation Pads Dropdown Capsule
                HudTeleportPadsCapsule(
                    padsCount = state.harmonicPadsCount,
                    isHarmonicActive = state.harmonicGridActive,
                    harmonicDmgPct = harmonicDmgPct,
                    isRotated = isRotated,
                    animatedRotation = animatedRotation,
                    counterRotateModifier = counterRotateModifier
                )

                // 7. Auto Wave Toggle
                HudAutoWaveToggle(
                    autoWaveActive = state.autoCallNextWave,
                    isRotated = isRotated,
                    counterRotateModifier = counterRotateModifier,
                    onToggleAutoWave = onToggleAutoWave
                )

                // 8. Launch Wave Button
                HudLaunchWaveButton(
                    isWaveActive = state.isWaveActive,
                    enemyCount = activeEnemyCount,
                    isRotated = isRotated,
                    counterRotateModifier = counterRotateModifier,
                    onDeployWave = onDeployWave
                )

                // 9. Battery Widget
                HudBatteryWidget(
                    isRotated = isRotated,
                    counterRotateModifier = counterRotateModifier
                )
            }
        }
    }
}
