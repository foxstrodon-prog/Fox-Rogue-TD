package com.example.ui.hud

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TowerUpgradeManager
import com.example.model.*
import com.example.ui.GameUIState
import com.example.ui.hud.components.TargetPriorityMenu
import kotlinx.coroutines.launch

/**
 * Procedural Vector Tower Class Silhouette.
 * Uses cached Skia Path (remember { Path() }) for zero per-frame allocations (Task D-03).
 */
@Composable
fun TowerClassSilhouette(
    towerClass: TowerClass,
    color: Color,
    modifier: Modifier = Modifier
) {
    val cachedPath = remember { Path() }

    Canvas(modifier = modifier.size(15.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> {
                drawCircle(
                    color = color,
                    radius = w * 0.44f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.6f)
                )
                drawCircle(
                    color = color,
                    radius = w * 0.20f,
                    center = Offset(cx, cy)
                )
                val barrelLength = h * 0.38f
                drawLine(
                    color = color,
                    start = Offset(cx - 2.8f, cy),
                    end = Offset(cx - 2.8f, cy - barrelLength),
                    strokeWidth = 2.0f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx + 2.8f, cy),
                    end = Offset(cx + 2.8f, cy - barrelLength),
                    strokeWidth = 2.0f,
                    cap = StrokeCap.Round
                )
            }
            TowerClass.ELEMENTAL_VOID -> {
                cachedPath.reset()
                cachedPath.moveTo(cx, cy - h * 0.45f)
                cachedPath.lineTo(cx + w * 0.42f, cy + h * 0.38f)
                cachedPath.lineTo(cx - w * 0.42f, cy + h * 0.38f)
                cachedPath.close()
                drawPath(
                    path = cachedPath,
                    color = color,
                    style = Stroke(width = 1.6f)
                )
                drawCircle(
                    color = color,
                    radius = w * 0.16f,
                    center = Offset(cx, cy + h * 0.08f)
                )
                drawLine(
                    color = color,
                    start = Offset(cx, cy + h * 0.08f),
                    end = Offset(cx, cy - h * 0.40f),
                    strokeWidth = 1.6f
                )
            }
            TowerClass.HEAVY_SIEGE -> {
                val pad = w * 0.12f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pad, pad),
                    size = Size(w - 2 * pad, h - 2 * pad),
                    cornerRadius = CornerRadius(2.5f, 2.5f),
                    style = Stroke(width = 1.6f)
                )
                drawCircle(
                    color = color,
                    radius = w * 0.24f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = w * 0.11f,
                    center = Offset(cx, cy)
                )
            }
            TowerClass.STELLAR_COSMIC -> {
                cachedPath.reset()
                val outerR = w * 0.46f
                val innerR = w * 0.18f
                val numPoints = 8
                for (i in 0 until numPoints * 2) {
                    val angle = (i * Math.PI / numPoints - Math.PI / 2).toFloat()
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = cx + r * kotlin.math.cos(angle)
                    val y = cy + r * kotlin.math.sin(angle)
                    if (i == 0) cachedPath.moveTo(x, y) else cachedPath.lineTo(x, y)
                }
                cachedPath.close()
                drawPath(path = cachedPath, color = color)
                drawCircle(
                    color = Color.White,
                    radius = w * 0.10f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

/**
 * Floating Squircle Action Button (Track 2 / v0.0.1.005 Tactical Ergonomics).
 */
@Composable
fun SquircleActionButton(
    title: String = "",
    subtitle: String? = null,
    titleColor: Color? = null,
    name: String = "",
    iconText: String = "",
    iconComposable: (@Composable () -> Unit)? = null,
    cost: Int? = null,
    badgeText: String = "",
    themeColor: Color,
    canAfford: Boolean,
    isUnlocked: Boolean,
    bullet1: String? = null,
    bullet2: String? = null,
    itemWidth: Dp = 72.dp,
    itemHeight: Dp = 76.dp,
    rotationAngle: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    val (resolvedTitle, resolvedSubtitle) = when {
        title.isNotEmpty() -> title to subtitle
        name.isNotEmpty() -> {
            val parts = name.split(" ", limit = 2)
            parts[0] to (subtitle ?: parts.getOrNull(1))
        }
        else -> "" to subtitle
    }

    val displayPrice = when {
        cost != null -> "${cost}c"
        badgeText.isNotEmpty() -> badgeText
        else -> ""
    }

    Box(
        modifier = modifier
            .size(width = itemWidth, height = itemHeight)
            .graphicsLayer {
                rotationZ = rotationAngle
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    !isUnlocked -> Color(0xFF090D16).copy(alpha = 0.88f)
                    canAfford -> Color(0xFF0F172A).copy(alpha = 0.95f)
                    else -> Color(0xFF080C14).copy(alpha = 0.92f)
                }
            )
            .border(
                1.5.dp,
                when {
                    !isUnlocked -> Color(0xFF334155).copy(alpha = 0.6f)
                    canAfford -> themeColor
                    else -> themeColor.copy(alpha = 0.35f)
                },
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = canAfford && isUnlocked) {
                coroutineScope.launch {
                    scale.animateTo(0.88f, animationSpec = tween(60))
                    scale.animateTo(1f, animationSpec = tween(100))
                }
                onClick()
            }
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (iconComposable != null) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconComposable != null) {
                    Box(modifier = Modifier.size(15.dp), contentAlignment = Alignment.Center) {
                        iconComposable()
                    }
                }

                if (!isUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔒", fontSize = 7.sp)
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = "LOCK",
                            color = Color(0xFFFF2A55),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🪙", fontSize = 7.5.sp)
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = displayPrice,
                            color = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF2A55),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Text(
                    text = resolvedTitle,
                    color = if (!isUnlocked) Color(0xFF64748B) else (titleColor ?: if (canAfford) themeColor else themeColor.copy(alpha = 0.65f)),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    lineHeight = 10.sp,
                    overflow = TextOverflow.Ellipsis
                )
                if (!resolvedSubtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = resolvedSubtitle,
                        color = if (!isUnlocked) Color(0xFF475569) else if (canAfford) themeColor else themeColor.copy(alpha = 0.65f),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        lineHeight = 9.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.5.dp)
            ) {
                if (bullet1 != null) {
                    Text(
                        text = "• $bullet1",
                        color = Color(0xFF94A3B8),
                        fontSize = 6.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 7.5.sp
                    )
                }
                if (bullet2 != null) {
                    Text(
                        text = "• $bullet2",
                        color = Color(0xFF94A3B8),
                        fontSize = 6.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 7.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Floating Squircle Upgrade Action Bar (Track 2 / v0.0.1.005).
 */
@Composable
fun FloatingSquircleUpgradeBar(
    state: GameUIState,
    onBuildTowerClass: (MapTile, TowerClass) -> Unit = { _, _ -> },
    onBuildTower: (MapTile, TowerType) -> Unit = { _, _ -> },
    onUpgradeToVector: (TowerInstance, TowerVector) -> Unit = { _, _ -> },
    onUpgradeToArchetype: (TowerInstance, TowerArchetype) -> Unit = { _, _ -> },
    onUpgradeToTier3Global: (TowerInstance, Int) -> Unit = { _, _ -> },
    onRequestSingularity: () -> Unit = {},
    onSellTower: () -> Unit = {},
    onChangePriority: (TargetPriority) -> Unit = {},
    onDeselect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isActionActive = state.selectedTower != null || (state.selectedTile != null && state.selectedTile.type == TileType.EMPTY)
    if (!isActionActive) return

    val counterAngle = when (((state.deviceRotationAngle % 360f + 360f) % 360f).toInt()) {
        90 -> -90f
        180 -> 180f
        270 -> 90f
        else -> 0f
    }
    val animatedRotation by animateFloatAsState(
        targetValue = counterAngle,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "squircle_counter_rotation"
    )

    Row(
        modifier = modifier
            .padding(bottom = 32.dp)
            .wrapContentSize(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            state.selectedTower != null -> {
                val tower = state.selectedTower
                val sellValue = tower.getSellValue()
                val prio = tower.targetPriority
                val isRotated = counterAngle != 0f

                val level = state.level
                val buildableTiles = level?.tiles?.filter { it.isBuildable } ?: emptyList()
                val isBoardFull = buildableTiles.isNotEmpty() && state.towers.size >= buildableTiles.size
                val areAllTowersMaxed = state.towers.isNotEmpty() && state.towers.all { it.tier >= 2 || it.finalFormCode != null }
                val isSingularityGateReady = isBoardFull && areAllTowersMaxed

                when (tower.tier) {
                    0 -> {
                        val vectors = TowerUpgradeManager.getTier1Vectors()
                        val cost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
                        val canAfford = state.scrap >= cost
                        val itemWidth = 84.dp
                        val itemHeight = 76.dp

                        val classPrefix = when (tower.towerClass) {
                            TowerClass.KINETIC_BALLISTIC -> "Kinetic"
                            TowerClass.ELEMENTAL_VOID -> "Void"
                            TowerClass.HEAVY_SIEGE -> "Siege"
                            TowerClass.STELLAR_COSMIC -> "Stellar"
                        }

                        vectors.forEach { vec ->
                            val vecSuffix = when (vec) {
                                TowerVector.OFFENSIVE -> "Offense"
                                TowerVector.DEFENSIVE -> "Defense"
                                TowerVector.SUPPORT -> "Support"
                            }
                            val vecColor = Color(getTier1VectorColor(tower.towerClass, vec))
                            val (b1, b2) = when (vec) {
                                TowerVector.OFFENSIVE -> "+45% DMG" to "+15% CRIT"
                                TowerVector.DEFENSIVE -> "+35% RNG" to "+20% Slow"
                                TowerVector.SUPPORT -> "+40% SPD" to "+25% Scrap"
                            }
                            SquircleActionButton(
                                title = classPrefix,
                                subtitle = vecSuffix,
                                cost = cost,
                                themeColor = vecColor,
                                canAfford = canAfford,
                                isUnlocked = true,
                                bullet1 = b1,
                                bullet2 = b2,
                                itemWidth = itemWidth,
                                itemHeight = itemHeight,
                                rotationAngle = animatedRotation,
                                onClick = { onUpgradeToVector(tower, vec) },
                                modifier = Modifier.testTag("upgrade_vector_${vec.idName}")
                            )
                        }
                    }

                    1 -> {
                        val vec = tower.vector ?: TowerVector.OFFENSIVE
                        val archetypes = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, vec)
                        val itemWidth = 84.dp
                        val itemHeight = 76.dp

                        archetypes.forEach { arch ->
                            val cost = TowerUpgradeManager.getTier2Cost(arch)
                            val canAfford = state.scrap >= cost
                            val (archTitle, archSub, b1) = getArchetypeCardLabels(arch)
                            val b2 = arch.primaryAura.shortBadge

                            SquircleActionButton(
                                title = archTitle,
                                subtitle = archSub,
                                cost = cost,
                                themeColor = Color(arch.color),
                                canAfford = canAfford,
                                isUnlocked = true,
                                bullet1 = b1,
                                bullet2 = b2,
                                itemWidth = itemWidth,
                                itemHeight = itemHeight,
                                rotationAngle = animatedRotation,
                                onClick = { onUpgradeToArchetype(tower, arch) },
                                modifier = Modifier.testTag("upgrade_archetype_${arch.archetypeId}")
                            )
                        }
                    }

                    2 -> {
                        val cost = TowerUpgradeManager.getTier3GlobalCost(tower, state.towers)
                        val canAfford = state.scrap >= cost
                        val itemWidth = 84.dp
                        val itemHeight = 76.dp

                        for (idx in 0..2) {
                            val subTitle = when (idx) {
                                0 -> "Alpha"
                                1 -> "Beta"
                                else -> "Gamma"
                            }
                            val (b1, b2) = when (idx) {
                                0 -> "Global Aura Buff" to "+50% Apex DMG"
                                1 -> "Corona Shockwave" to "+50% Apex DMG"
                                else -> "Overcharge Surge" to "+50% Apex DMG"
                            }
                            SquircleActionButton(
                                title = "Apex",
                                subtitle = subTitle,
                                cost = cost,
                                themeColor = Color(0xFFFFD700),
                                canAfford = canAfford,
                                isUnlocked = true,
                                bullet1 = b1,
                                bullet2 = b2,
                                itemWidth = itemWidth,
                                itemHeight = itemHeight,
                                rotationAngle = animatedRotation,
                                onClick = { onUpgradeToTier3Global(tower, idx) },
                                modifier = Modifier.testTag("upgrade_tier3_$idx")
                            )
                        }

                        val readyCount = state.towers.count { it.tier >= 2 || it.finalFormCode != null }
                        SingularityGateSquircle(
                            tower = tower,
                            isSingularityGateReady = isSingularityGateReady,
                            readyCount = readyCount,
                            totalBuildable = buildableTiles.size,
                            itemWidth = itemWidth,
                            itemHeight = itemHeight,
                            animatedRotation = animatedRotation,
                            onRequestSingularity = onRequestSingularity
                        )
                    }

                    else -> {
                        val readyCount = state.towers.count { it.tier >= 2 || it.finalFormCode != null }
                        val itemWidth = 84.dp
                        val itemHeight = 76.dp

                        if (tower.isCapstone) {
                            Box(
                                modifier = Modifier
                                    .size(width = itemWidth, height = itemHeight)
                                    .graphicsLayer { rotationZ = animatedRotation }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                                    .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("★ CONDUIT ★", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    Text("GEN ${tower.generation}", color = Color(0xFF00F0FF), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Text("${tower.synergyVault.sumOf { it.synergies.size }} Banked", color = Color(0xFF94A3B8), fontSize = 7.sp)
                                    Text("${state.towers.size}/${buildableTiles.size} Full", color = Color(0xFFFF9100), fontSize = 6.5.sp)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(width = itemWidth, height = itemHeight)
                                    .graphicsLayer { rotationZ = animatedRotation }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F172A).copy(alpha = 0.92f))
                                    .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("★", color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    Text("Apex Max", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Text(tower.finalFormCode ?: "Ascended", color = Color(0xFF38BDF8), fontSize = 7.sp)
                                    Text("• Fully Ascended", color = Color(0xFF94A3B8), fontSize = 6.5.sp)
                                }
                            }
                        }

                        SingularityGateSquircle(
                            tower = tower,
                            isSingularityGateReady = isSingularityGateReady,
                            readyCount = readyCount,
                            totalBuildable = buildableTiles.size,
                            itemWidth = itemWidth,
                            itemHeight = itemHeight,
                            animatedRotation = animatedRotation,
                            onRequestSingularity = onRequestSingularity
                        )
                    }
                }

                TargetPriorityMenu(
                    priority = prio,
                    sellValue = sellValue,
                    isRotated = isRotated,
                    animatedRotation = animatedRotation,
                    onChangePriority = onChangePriority,
                    onSellTower = onSellTower
                )
            }

            state.selectedTile != null && state.selectedTile.type == TileType.EMPTY -> {
                val tile = state.selectedTile
                val classes = TowerUpgradeManager.getTowerClasses()
                val itemWidth = 72.dp
                val itemHeight = 76.dp

                classes.forEach { towerClass ->
                    val isUnlocked = TowerUpgradeManager.isClassUnlocked(towerClass, state.towers)
                    val cost = (towerClass.baseCost * state.balanceConfig.buildCostMultiplier).toInt()
                    val canAfford = state.scrap >= cost
                    val (b1, b2) = when (towerClass) {
                        TowerClass.KINETIC_BALLISTIC -> "Ballistic Fire" to "Single-Target"
                        TowerClass.ELEMENTAL_VOID -> "Cryo / Plasma" to "Area AoE Slow"
                        TowerClass.HEAVY_SIEGE -> "Siege Blasts" to "Armor Shred"
                        TowerClass.STELLAR_COSMIC -> "Cosmic Rays" to "Barrier Bypass"
                    }

                    val (cTitle, cSub) = when (towerClass) {
                        TowerClass.KINETIC_BALLISTIC -> "Kinetic" to "Ballistic"
                        TowerClass.ELEMENTAL_VOID -> "Elemental" to "Void"
                        TowerClass.HEAVY_SIEGE -> "Heavy" to "Siege"
                        TowerClass.STELLAR_COSMIC -> "Stellar" to "Cosmic"
                    }

                    SquircleActionButton(
                        title = cTitle,
                        subtitle = cSub,
                        iconText = towerClass.shapeSymbol,
                        iconComposable = {
                            TowerClassSilhouette(
                                towerClass = towerClass,
                                color = if (!isUnlocked) Color(0xFF64748B) else if (canAfford) Color(towerClass.color) else Color(towerClass.color).copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        cost = cost,
                        themeColor = Color(towerClass.color),
                        canAfford = canAfford,
                        isUnlocked = isUnlocked,
                        bullet1 = b1,
                        bullet2 = b2,
                        itemWidth = itemWidth,
                        itemHeight = itemHeight,
                        rotationAngle = animatedRotation,
                        onClick = {
                            onBuildTowerClass(tile, towerClass)
                            if (towerClass == TowerClass.KINETIC_BALLISTIC) {
                                onBuildTower(tile, TowerType.GATLING)
                            } else if (towerClass == TowerClass.STELLAR_COSMIC) {
                                onBuildTower(tile, TowerType.STELLAR)
                            }
                        },
                        modifier = Modifier.testTag(if (towerClass == TowerClass.KINETIC_BALLISTIC) "build_basic_turret" else "build_class_${towerClass.name}")
                    )
                }
            }
        }
    }
}

@Composable
private fun SingularityGateSquircle(
    tower: TowerInstance,
    isSingularityGateReady: Boolean,
    readyCount: Int,
    totalBuildable: Int,
    itemWidth: Dp,
    itemHeight: Dp,
    animatedRotation: Float,
    onRequestSingularity: () -> Unit
) {
    if (isSingularityGateReady) {
        SquircleActionButton(
            title = "★ FORGE ★",
            subtitle = if (tower.isCapstone) "GEN ${tower.generation + 1}" else "CONDUIT",
            cost = 0,
            themeColor = Color(0xFFFFD700),
            canAfford = true,
            isUnlocked = true,
            bullet1 = "Bank Synergies",
            bullet2 = "+20% Scrap",
            itemWidth = itemWidth,
            itemHeight = itemHeight,
            rotationAngle = animatedRotation,
            onClick = onRequestSingularity,
            modifier = Modifier.testTag("singularity_capstone_button")
        )
    } else {
        SquircleActionButton(
            title = "CONDUIT",
            subtitle = "PROGRESS",
            cost = 0,
            themeColor = Color(0xFF38BDF8),
            canAfford = true,
            isUnlocked = true,
            bullet1 = "$readyCount/$totalBuildable Ready",
            bullet2 = "Tap for Info",
            itemWidth = itemWidth,
            itemHeight = itemHeight,
            rotationAngle = animatedRotation,
            onClick = onRequestSingularity,
            modifier = Modifier.testTag("singularity_conduit_teaser_button")
        )
    }
}

private fun getArchetypeCardLabels(arch: TowerArchetype): Triple<String, String, String> = when (arch) {
    TowerArchetype.VULCAN_GATLING -> Triple("Vulcan", "Gatling", "Rotary DPS")
    TowerArchetype.FLAK_CANNON -> Triple("Twin Flak", "Cannon", "Shrapnel AoE")
    TowerArchetype.SNIPER_SENTRY -> Triple("Sniper", "Sentry", "Penetrator")
    TowerArchetype.BARRICADE_TURRET -> Triple("Barricade", "Turret", "Suppression")
    TowerArchetype.MINE_DISPENSER -> Triple("Mine", "Dispenser", "Lane Mines")
    TowerArchetype.RIOT_DISRUPTOR -> Triple("Riot", "Disruptor", "Acoustic Wave")
    TowerArchetype.RADAR_SENTRY -> Triple("Radar", "Sentry", "Target Link")
    TowerArchetype.AMMO_SUPPLY_DEPOT -> Triple("Ammo", "Depot", "Munitions")
    TowerArchetype.SALVAGE_CRUSHER -> Triple("Salvage", "Crusher", "Scrap Siphon")
    TowerArchetype.SOLAR_DEATH_RAY -> Triple("Solar", "Death Ray", "Thermal Beam")
    TowerArchetype.INFERNO_CANNON -> Triple("Inferno", "Emitter", "Burn DoT")
    TowerArchetype.PLASMA_PULSAR -> Triple("Plasma", "Pulsar", "Ion Cascade")
    TowerArchetype.FROST_SPIRE -> Triple("Frost", "Spire", "Cryo Stasis")
    TowerArchetype.TESLA_STORMCOIL -> Triple("Tesla", "Stormcoil", "Lightning Web")
    TowerArchetype.GRAVITON_VOID -> Triple("Graviton", "Void", "Vortex Pull")
    TowerArchetype.DYNAMO_CONDUIT -> Triple("Dynamo", "Conduit", "Flux Battery")
    TowerArchetype.QUANTUM_BEACON -> Triple("Quantum", "Beacon", "Phase Warp")
    TowerArchetype.DARK_MATTER_FORGE -> Triple("Dark", "Matter", "Armor Melter")
    TowerArchetype.HELLFIRE_MORTAR -> Triple("Hellfire", "Mortar", "Artillery AoE")
    TowerArchetype.TITAN_RAILGUN -> Triple("Titan", "Railgun", "Uranium Slug")
    TowerArchetype.ORBITAL_STRIKE_RELAY -> Triple("Orbital", "Relay", "Kinetic Bomb")
    TowerArchetype.EARTHQUAKE_STAMPER -> Triple("Seismic", "Slammer", "Tremor Stun")
    TowerArchetype.CRYO_FORTRESS -> Triple("Cryo", "Fortress", "Glacier Wall")
    TowerArchetype.SHOCKWAVE_BASTION -> Triple("Shockwave", "Bastion", "EMP Bastion")
    TowerArchetype.NANITE_FORGE -> Triple("Nanite", "Forge", "Nanite Shred")
    TowerArchetype.SIEGE_COMMAND_POST -> Triple("Command", "Post", "Overcharge")
    TowerArchetype.WAR_CHEST_VAULT -> Triple("War Chest", "Vault", "Yield Smelter")
    TowerArchetype.SUPERNOVA_PULSAR -> Triple("Supernova", "Pulsar", "Cosmic Flare")
    TowerArchetype.GRAVITON_LANCE -> Triple("Graviton", "Lance", "Gravity Well")
    TowerArchetype.CELESTIAL_COMET -> Triple("Celestial", "Comet", "Meteor Storm")
    TowerArchetype.EVENT_HORIZON -> Triple("Event", "Horizon", "Time Warp")
    TowerArchetype.NEBULA_AEGIS -> Triple("Nebula", "Aegis", "Cosmic Shield")
    TowerArchetype.BLACK_HOLE_SENTRY -> Triple("Singular.", "Bastion", "Micro Void")
    TowerArchetype.CHRONO_PYLON -> Triple("Chrono", "Pylon", "Time Haste")
    TowerArchetype.STARLIGHT_BEACON -> Triple("Starlight", "Beacon", "True Sight")
    TowerArchetype.ASTRAL_HARVESTER -> Triple("Astral", "Harvest", "Cosmic Siphon")
}

/**
 * Minimal Floating Bottom Command Dock / Tower Upgrade Bar for Fox-Rogue-TD.
 */
@Composable
fun MinimalBottomDock(
    state: GameUIState,
    onDeployWave: () -> Unit,
    onToggleAutoWave: () -> Unit,
    onBuildTower: (MapTile, TowerType) -> Unit,
    onBuildTowerClass: (MapTile, TowerClass) -> Unit = { _, _ -> },
    onUpgradeToVector: (TowerInstance, TowerVector) -> Unit = { _, _ -> },
    onUpgradeToArchetype: (TowerInstance, TowerArchetype) -> Unit = { _, _ -> },
    onUpgradeToTier3Global: (TowerInstance, Int) -> Unit = { _, _ -> },
    onUpgradeToTier1: (TowerInstance, String) -> Unit = { _, _ -> },
    onUpgradeToFinalForm: (TowerInstance, String) -> Unit = { _, _ -> },
    onUpgradePath: (TowerInstance, Int) -> Unit = { _, _ -> },
    onAscendTower: (TowerInstance) -> Unit = {},
    onRequestSingularity: () -> Unit = {},
    onSellTower: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingSquircleUpgradeBar(
        state = state,
        onBuildTowerClass = onBuildTowerClass,
        onBuildTower = onBuildTower,
        onUpgradeToVector = onUpgradeToVector,
        onUpgradeToArchetype = onUpgradeToArchetype,
        onUpgradeToTier3Global = onUpgradeToTier3Global,
        onRequestSingularity = onRequestSingularity,
        onSellTower = onSellTower,
        onChangePriority = onChangePriority,
        onDeselect = onDeselect,
        modifier = modifier
    )
}
