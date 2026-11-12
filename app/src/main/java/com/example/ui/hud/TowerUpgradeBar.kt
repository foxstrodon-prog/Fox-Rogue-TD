package com.example.ui.hud

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TowerUpgradeManager
import com.example.model.*
import com.example.ui.GameUIState
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Procedural Vector Tower Class Silhouette.
 * Kinetic Ballistic: Dual-barrel rotating Gatling silhouette / Circle emblem.
 * Elemental Void: Prismatic Crystal / Triangle beam emitter emblem.
 * Heavy Siege: Armored Cupola Mortar / Square blast box emblem.
 * Stellar Cosmic: 8-point Radiant Star / Celestial Corona emblem.
 */
@Composable
fun TowerClassSilhouette(
    towerClass: TowerClass,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(15.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> {
                // Dual-barrel rotating Gatling silhouette / Circle chassis
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
                // Prismatic Crystal / Triangle beam emitter emblem
                val path = Path().apply {
                    moveTo(cx, cy - h * 0.45f)
                    lineTo(cx + w * 0.42f, cy + h * 0.38f)
                    lineTo(cx - w * 0.42f, cy + h * 0.38f)
                    close()
                }
                drawPath(
                    path = path,
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
                // Armored Cupola Mortar / Square blast box emblem
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
                // 8-point Radiant Star / Celestial Corona emblem
                val starPath = Path()
                val outerR = w * 0.46f
                val innerR = w * 0.18f
                val numPoints = 8
                for (i in 0 until numPoints * 2) {
                    val angle = (i * Math.PI / numPoints - Math.PI / 2).toFloat()
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = cx + r * kotlin.math.cos(angle)
                    val y = cy + r * kotlin.math.sin(angle)
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()
                drawPath(path = starPath, color = color)
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
 * Scaled squircle (CornerSize 14dp) with glowing neon border matching unique color,
 * procedural tower silhouette / glyph, 2-line stacked title & subtitle,
 * scrap currency coin badge ("🪙 180c"), and alert RED cost highlighting when unaffordable.
 */
@Composable
fun SquircleActionButton(
    title: String = "",
    subtitle: String? = null,
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

    // Resolve 2-line stacked typography if title/subtitle or legacy name was provided
    val (resolvedTitle, resolvedSubtitle) = when {
        title.isNotEmpty() -> title to subtitle
        name.isNotEmpty() -> {
            val parts = name.split(" ", limit = 2)
            parts[0] to (subtitle ?: parts.getOrNull(1))
        }
        else -> "" to subtitle
    }

    // Resolve price text
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
                    else -> Color(0xFF080C14).copy(alpha = 0.92f) // Darker container when unaffordable
                }
            )
            .border(
                1.5.dp,
                when {
                    !isUnlocked -> Color(0xFF334155).copy(alpha = 0.6f)
                    canAfford -> themeColor // Glowing neon border strictly matching unique theme color
                    else -> themeColor.copy(alpha = 0.35f) // Dimmed theme border when unaffordable
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
            // Header Row: Tower Class Silhouette / Glyph on Left, 🪙 Cost / LOCK on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconComposable != null) {
                    Box(modifier = Modifier.size(15.dp), contentAlignment = Alignment.Center) {
                        iconComposable()
                    }
                } else {
                    Text(
                        text = iconText,
                        color = if (!isUnlocked) Color(0xFF64748B) else if (canAfford) themeColor else themeColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Price / Lock Badge
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
                        Text(
                            text = "🪙",
                            fontSize = 7.5.sp
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = displayPrice,
                            color = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF2A55), // VIVID ALERT RED when unaffordable!
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }

            // 2-Line Stacked Name Labels with Unique Color Accent
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = resolvedTitle,
                    color = if (!isUnlocked) Color(0xFF64748B) else Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (resolvedSubtitle != null) {
                    Text(
                        text = resolvedSubtitle,
                        color = if (!isUnlocked) Color(0xFF475569) else if (canAfford) themeColor else themeColor.copy(alpha = 0.6f),
                        fontSize = 7.2.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2 Short, Sweet Bullet Points
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
 * Sleek row of floating squircle action buttons positioned 32dp above gesture bar.
 */
@Composable
fun FloatingSquircleUpgradeBar(
    state: GameUIState,
    onBuildTowerClass: (MapTile, TowerClass) -> Unit = { _, _ -> },
    onBuildTower: (MapTile, TowerType) -> Unit = { _, _ -> },
    onUpgradeToVector: (TowerInstance, TowerVector) -> Unit = { _, _ -> },
    onUpgradeToArchetype: (TowerInstance, TowerArchetype) -> Unit = { _, _ -> },
    onUpgradeToTier3Global: (TowerInstance, Int) -> Unit = { _, _ -> },
    onSellTower: () -> Unit = {},
    onChangePriority: (TargetPriority) -> Unit = {},
    onDeselect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isActionActive = state.selectedTower != null || (state.selectedTile != null && state.selectedTile.type == TileType.EMPTY)
    if (!isActionActive) return

    // Shortest-path counter-rotation: at 270 deg (phone tilted right), rotate +90 deg instead of -270 deg
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

    // Floating free without an obstructive background menu card sheet
    Row(
        modifier = modifier
            .padding(bottom = 32.dp)
            .wrapContentSize(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            // 1. PLACED TOWER SELECTED (Evaluated FIRST so upgrade buttons always appear on placed towers)
            state.selectedTower != null -> {
                val tower = state.selectedTower
                val sellValue = tower.getSellValue()

                when (tower.tier) {
                    0 -> {
                        // 3 Vectors: Offense, Defense, Support (Title Case, no truncation, 2-line stacked)
                        val vectors = TowerUpgradeManager.getTier1Vectors()
                        val cost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
                        val canAfford = state.scrap >= cost
                        val itemWidth = 84.dp
                        val itemHeight = 76.dp

                        vectors.forEach { vec ->
                            val icon = when (vec) {
                                TowerVector.OFFENSIVE -> "⚔"
                                TowerVector.DEFENSIVE -> "🛡"
                                TowerVector.SUPPORT -> "⚡"
                            }
                            val (b1, b2) = when (vec) {
                                TowerVector.OFFENSIVE -> "+35% Damage" to "High Fire Rate"
                                TowerVector.DEFENSIVE -> "+40% Range" to "Slow & Fortify"
                                TowerVector.SUPPORT -> "Aura Synergies" to "+Scrap Bounty"
                            }
                            SquircleActionButton(
                                title = vec.displayName,
                                subtitle = "Vector",
                                iconText = icon,
                                cost = cost,
                                themeColor = Color(vec.color),
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
                        // 3 Archetypes for this class & vector (2-line stacked: Primary Name + Specialization)
                        val vec = tower.vector ?: TowerVector.OFFENSIVE
                        val archetypes = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, vec)
                        val itemWidth = 84.dp
                        val itemHeight = 76.dp

                        archetypes.forEach { arch ->
                            val cost = TowerUpgradeManager.getTier2Cost(arch)
                            val canAfford = state.scrap >= cost
                            val b1 = when {
                                arch.isBeam -> "Continuous Beam"
                                arch.isSplash -> "AoE Splash Burst"
                                arch.chains > 0 -> "Chain Arc Shock"
                                arch.slowPercent > 0 -> "Slows Movement"
                                arch.armorShredPercent > 0 -> "Armor Shredding"
                                else -> arch.role.take(14)
                            }
                            val b2 = arch.primaryAura.shortBadge

                            val nameParts = arch.displayName.split(" ", limit = 2)
                            val archTitle = nameParts.firstOrNull() ?: arch.displayName
                            val archSub = nameParts.getOrNull(1) ?: "Turret"

                            SquircleActionButton(
                                title = archTitle,
                                subtitle = archSub,
                                iconText = arch.displayName.take(2).uppercase(),
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
                        // 3 Tier 3 Global Auras / Ascensions (2-line stacked: Apex + Sub-class)
                        val cost = 280
                        val canAfford = state.scrap >= cost
                        val itemWidth = 84.dp
                        val itemHeight = 76.dp

                        listOf("Alpha" to "Apex Alpha", "Beta" to "Apex Beta", "Gamma" to "Apex Gamma").forEachIndexed { idx, (subTitle, _) ->
                            val (b1, b2) = when (idx) {
                                0 -> "Global Aura Buff" to "+50% Apex DMG"
                                1 -> "Corona Shockwave" to "+50% Apex DMG"
                                else -> "Overcharge Surge" to "+50% Apex DMG"
                            }
                            SquircleActionButton(
                                title = "Apex",
                                subtitle = subTitle,
                                iconText = "★",
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
                    }

                    else -> {
                        // Tier 3 Maxed Squircle
                        Box(
                            modifier = Modifier
                                .size(width = 72.dp, height = 74.dp)
                                .graphicsLayer { rotationZ = animatedRotation }
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.92f))
                                .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("★", color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text("Apex Max", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                Text("• Fully Ascended", color = Color(0xFF94A3B8), fontSize = 6.5.sp)
                            }
                        }
                    }
                }

                // Priority toggle button
                val prio = tower.targetPriority
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 74.dp)
                        .graphicsLayer { rotationZ = animatedRotation }
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.92f))
                        .border(1.5.dp, Color(0xFF00F0FF).copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable {
                            val nextPrio = when (prio) {
                                TargetPriority.FIRST -> TargetPriority.CLOSEST
                                TargetPriority.CLOSEST -> TargetPriority.STRONGEST
                                TargetPriority.STRONGEST -> TargetPriority.WEAKEST
                                TargetPriority.WEAKEST -> TargetPriority.FIRST
                            }
                            onChangePriority(nextPrio)
                        }
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("🎯", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(prio.label.take(4).uppercase(), color = Color(0xFF00F0FF), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Sell button
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 74.dp)
                        .graphicsLayer { rotationZ = animatedRotation }
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.92f))
                        .border(1.5.dp, Color(0xFFFF2A55).copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable { onSellTower() }
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("🗑", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("+$sellValue", color = Color(0xFFFFD700), fontSize = 7.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // 2. EMPTY TILE SELECTED (Shows 4 Class Build Action Squircles with Vector Silhouette Icons)
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

/**
 * Minimal Floating Bottom Command Dock / Tower Upgrade Bar for Fox-Rogue-TD.
 * Delegates directly to the sleek FloatingSquircleUpgradeBar.
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
        onSellTower = onSellTower,
        onChangePriority = onChangePriority,
        onDeselect = onDeselect,
        modifier = modifier
    )
}

@Composable
fun BuildTowerDrawer(
    tile: MapTile,
    allTowers: List<TowerInstance> = emptyList(),
    scrap: Int,
    isLandscape: Boolean = false,
    onBuildClass: (TowerClass) -> Unit = {},
    onBuild: (TowerType) -> Unit = {},
    onClose: () -> Unit
) {
    val classes = TowerUpgradeManager.getTowerClasses()

    val renderCard: @Composable (TowerClass, Modifier) -> Unit = { towerClass, cardModifier ->
        val isUnlocked = TowerUpgradeManager.isClassUnlocked(towerClass, allTowers)
        val cost = towerClass.baseCost
        val canAfford = scrap >= cost
        val isEnabled = isUnlocked && canAfford
        val themeColor = Color(towerClass.color)

        val unlockCondition = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> "Unlocked"
            TowerClass.ELEMENTAL_VOID -> "Unlock: 1 Tier 3"
            TowerClass.HEAVY_SIEGE -> "Unlock: 2 Tier 3"
            TowerClass.STELLAR_COSMIC -> "Unlock: 3 Tier 3"
        }

        Card(
            onClick = {
                if (isEnabled) {
                    onBuildClass(towerClass)
                    if (towerClass == TowerClass.KINETIC_BALLISTIC) {
                        onBuild(TowerType.GATLING)
                    } else if (towerClass == TowerClass.STELLAR_COSMIC) {
                        onBuild(TowerType.STELLAR)
                    }
                }
            },
            enabled = isEnabled,
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) Color(0xFF131F33) else Color(0xFF0C1322),
                disabledContainerColor = Color(0xFF0C1322)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = cardModifier
                .testTag(if (towerClass == TowerClass.KINETIC_BALLISTIC) "build_basic_turret" else "build_class_${towerClass.name}")
                .border(
                    1.dp,
                    if (isEnabled) themeColor.copy(alpha = 0.8f) else Color(0xFF1E293B),
                    RoundedCornerShape(8.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TowerClassSilhouette(
                            towerClass = towerClass,
                            color = if (!isUnlocked) Color(0xFF64748B) else Color(towerClass.color),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = towerClass.displayName,
                            color = if (isUnlocked) (if (canAfford) Color.White else Color(0xFF94A3B8)) else Color(0xFF64748B),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!isUnlocked) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFFF2A55),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val statsPreview = when (towerClass) {
                    TowerClass.KINETIC_BALLISTIC -> "DMG: 26 | SPD: 4.4x | RNG: 2.6u\n+18% Global Speed Aura"
                    TowerClass.ELEMENTAL_VOID -> "DMG: 45 | SPD: 2.8x | RNG: 3.2u\nFreezing Slow & Armor Shred"
                    TowerClass.HEAVY_SIEGE -> "DMG: 145 | SPD: 0.8x | RNG: 4.8u\nAoE Splash & Boss Buster"
                    TowerClass.STELLAR_COSMIC -> "DMG: 180 | SPD: 2.0x | RNG: 5.5u\nCosmic Radiance & Apex Pulsar"
                }

                Text(
                    text = if (!isUnlocked) unlockCondition else statsPreview,
                    color = if (!isUnlocked) Color(0xFFFF2A55) else Color(0xFF00FF9D),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🪙",
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$cost c",
                            color = if (!isUnlocked) Color(0xFF64748B) else if (canAfford) Color(0xFFFFD700) else Color(0xFFFF2A55),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = if (!isUnlocked) "LOCKED" else if (canAfford) "BUILD" else "NEED SCRAP",
                        color = if (!isUnlocked) Color(0xFF64748B) else if (canAfford) themeColor else Color(0xFFFF2A55),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DEPLOY DEFENSE TOWER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
                if (tile.buff != TileBuff.NONE) {
                    Text(
                        text = "TERRAIN BONUS: ${tile.buff.label} (${tile.buff.desc})",
                        color = Color(tile.buff.badgeColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isLandscape) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                classes.forEach { towerClass ->
                    renderCard(towerClass, Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                classes.chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowPair.forEach { towerClass ->
                            renderCard(towerClass, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BranchingTowerInspector(
    tower: TowerInstance,
    allTowers: List<TowerInstance>,
    scrap: Int,
    activeSynergies: List<GlobalAuraSynergy> = emptyList(),
    isLandscape: Boolean = false,
    onUpgradeToVector: (TowerVector) -> Unit = {},
    onUpgradeToArchetype: (TowerArchetype) -> Unit = {},
    onUpgradeToTier3Global: (Int) -> Unit = {},
    onUpgradeToTier1: (String) -> Unit = {},
    onUpgradeToFinalForm: (String) -> Unit = {},
    onSell: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onClose: () -> Unit
) {
    val sellValue = tower.getSellValue()
    val arch = tower.archetype

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (tower.isTier3) Color(0xFFFFB800) else Color(arch?.color ?: tower.towerClass.color))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tower.towerClass.shapeSymbol,
                            color = Color(tower.towerClass.color),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tower.getDisplayName(),
                            color = if (tower.isTier3) Color(0xFFFFB800) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = when (tower.tier) {
                            3 -> "TIER 3 [${tower.towerClass.shapeName}]: ${tower.activeAura?.name ?: "GLOBAL AURA"} ONLINE"
                            2 -> "TIER 2 [${tower.towerClass.shapeName}]: SPECIALIZED ARCHETYPE LOCKED"
                            1 -> "TIER 1 [${tower.towerClass.shapeName}]: ${tower.vector?.displayName ?: "VECTOR"} SPECIALIZED"
                            else -> "TIER 0 [${tower.towerClass.shapeName}]: ${tower.towerClass.displayName} (SELECT VECTOR)"
                        },
                        color = when (tower.tier) {
                            3 -> Color(0xFFFFB800)
                            2 -> Color(0xFF00F0FF)
                            1 -> Color(0xFF00FF9D)
                            else -> Color(0xFF94A3B8)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TargetPriority.values().forEach { prio ->
                    val isSel = tower.targetPriority == prio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) Color(0xFF00F0FF) else Color(0xFF1E293B))
                            .clickable { onChangePriority(prio) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = prio.label,
                            color = if (isSel) Color.Black else Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- DETAILED STAT SHEET (3 Sections: Base Stats, Local Upgrades, Incoming Global Multipliers) ---
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.85f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section 1: Effective Combat Stats with accurate DPS
                val effDmg = tower.getEffectiveDamage()
                val effSpd = tower.getEffectiveFireRate()
                val effDps = tower.getEffectiveDps()
                val effRng = tower.getEffectiveCombatRange()
                Column {
                    Text(text = "COMBAT STATS (LIVE DPS)", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "DMG: ${effDmg.toInt()} | SPD: ${String.format(Locale.US, "%.1f", effSpd)}/s | DPS: ${effDps.toInt()} | RNG: ${effRng.toInt()}u", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                // Section 2: Local Upgrades
                Column {
                    Text(text = "LOCAL TIER", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "Tier ${tower.tier}/3 (${tower.vector?.displayName ?: "Base"})", color = Color(0xFF00FF9D), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
                // Section 3: Incoming Global Buffs
                Column {
                    Text(text = "GLOBAL AURAS", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "${activeSynergies.size} Active Multipliers", color = Color(0xFFFFB800), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // --- TIER PROGRESSION SELECTION CARDS ---
        when (tower.tier) {
            // TIER 0: Pick 1 of 3 Vectors (Offensive, Defensive, Support) for this class
            0 -> {
                val vectors = TowerUpgradeManager.getTier1Vectors()
                val cost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
                val canAfford = scrap >= cost

                val renderVectorCard: @Composable (TowerVector, Modifier) -> Unit = { vec, cardModifier ->
                    val statBoost = when (vec) {
                        TowerVector.OFFENSIVE -> "+45% DMG  |  +15% CRIT  |  Heavy Penetration"
                        TowerVector.DEFENSIVE -> "+35% RNG  |  +20% Slow  |  Pushback & Stun"
                        TowerVector.SUPPORT -> "+40% SPD  |  +25% SCRAP |  Range Uplink Aura"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
                            .border(1.dp, if (canAfford) Color(vec.color).copy(alpha = 0.8f) else Color(0xFF26334D), RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = vec.displayName,
                                    color = Color(vec.color),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🪙", fontSize = 9.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${cost} c",
                                        color = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF2A55),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = statBoost,
                                color = Color(0xFF00FF9D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                lineHeight = 11.sp,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { if (canAfford) onUpgradeToVector(vec) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(vec.color),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .testTag("upgrade_vector_${vec.idName}")
                            ) {
                                Text("Select ${vec.displayName}", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                if (isLandscape) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        vectors.forEach { vec ->
                            renderVectorCard(vec, Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        vectors.forEach { vec ->
                            renderVectorCard(vec, Modifier.weight(1f))
                        }
                    }
                }
            }

            // TIER 1: Pick 1 of 3 Archetypes for the selected Vector and Tower Class
            1 -> {
                val vec = tower.vector ?: TowerVector.OFFENSIVE
                val archetypes = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, vec)

                val renderArchetypeCard: @Composable (TowerArchetype, Modifier) -> Unit = { archetype, cardModifier ->
                    val cost = TowerUpgradeManager.getTier2Cost(archetype)
                    val canAfford = scrap >= cost

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
                            .border(1.dp, if (canAfford) Color(archetype.color).copy(alpha = 0.8f) else Color(0xFF26334D), RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = archetype.displayName,
                                    color = Color(archetype.color),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🪙", fontSize = 9.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${cost} c",
                                        color = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF2A55),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "DMG: ${archetype.baseDamage.toInt()} | SPD: ${archetype.baseFireRate}x | RNG: ${archetype.baseRange}u",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp
                            )

                            Surface(
                                color = Color(archetype.color).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Auras: ${archetype.primaryAura.shortBadge} / ${archetype.secondaryAura.shortBadge}",
                                    color = Color(archetype.color),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Button(
                                onClick = { if (canAfford) onUpgradeToArchetype(archetype) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(archetype.color),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .testTag("upgrade_archetype_${archetype.archetypeId}")
                            ) {
                                Text("Lock Identity", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                if (isLandscape) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        archetypes.forEach { archetype ->
                            renderArchetypeCard(archetype, Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        archetypes.forEach { archetype ->
                            renderArchetypeCard(archetype, Modifier.weight(1f))
                        }
                    }
                }
            }

            // TIER 2: Upgrade to Tier 3 Global Aura Projection (Choose between 2 unique auras!)
            2 -> {
                val archetype = tower.archetype ?: mapTypeToArchetype(tower.type)
                val cost = TowerUpgradeManager.getTier3GlobalCost(tower, allTowers)
                val canAfford = scrap >= cost

                val renderAuraCard: @Composable (AuraOption, Int, Modifier) -> Unit = { aura, auraIndex, cardModifier ->
                    val auraColor = if (auraIndex == 0) Color(0xFFFFB800) else Color(0xFF00FF9D)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
                            .border(
                                1.dp,
                                if (canAfford) auraColor else Color(0xFF26334D),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = aura.name,
                                    color = auraColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = auraColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = aura.shortBadge,
                                        color = auraColor,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = aura.description,
                                color = Color.White,
                                fontSize = 7.5.sp,
                                lineHeight = 9.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { if (canAfford) onUpgradeToTier3Global(auraIndex) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = auraColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .testTag(if (auraIndex == 0) "upgrade_tier3_global" else "upgrade_tier3_aura_1")
                            ) {
                                Text("Activate Aura", fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose Tier 3 Global Aura Projection",
                            color = Color(0xFFFFB800),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 8.5.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${cost} c",
                                color = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF2A55),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    if (isLandscape) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            renderAuraCard(archetype.primaryAura, 0, Modifier.fillMaxWidth())
                            renderAuraCard(archetype.secondaryAura, 1, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            renderAuraCard(archetype.primaryAura, 0, Modifier.weight(1f))
                            renderAuraCard(archetype.secondaryAura, 1, Modifier.weight(1f))
                        }
                    }
                }
            }

            // TIER 3: Apex Form Active with Selected Aura
            3 -> {
                val aura = tower.activeAura ?: (tower.archetype ?: mapTypeToArchetype(tower.type)).primaryAura
                Surface(
                    color = Color(0xFFFFB800).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌟 ${aura.name} ACTIVE",
                                color = Color(0xFFFFB800),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                            Text(
                                text = aura.description,
                                color = Color.White,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF00FF9D).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "[${aura.shortBadge}]",
                                color = Color(0xFF00FF9D),
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sell Button & Combat Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kills: ${tower.kills} | Dmg: ${tower.totalDamage.toInt()}",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp
            )

            OutlinedButton(
                onClick = onSell,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("sell_tower_button")
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("SELL (+$sellValue)", fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
            }
        }
    }
}

@Composable
fun ThreePathTowerInspector(
    tower: TowerInstance,
    allTowers: List<TowerInstance>,
    scrap: Int,
    onUpgradePath: (Int) -> Unit,
    onAscend: () -> Unit,
    onSell: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onClose: () -> Unit
) {
    BranchingTowerInspector(
        tower = tower,
        allTowers = allTowers,
        scrap = scrap,
        onUpgradeToTier1 = { branch ->
            val idx = when (branch) { "A" -> 0; "B" -> 1; else -> 2 }
            onUpgradePath(idx)
        },
        onUpgradeToFinalForm = {
            onAscend()
        },
        onSell = onSell,
        onChangePriority = onChangePriority,
        onClose = onClose
    )
}
