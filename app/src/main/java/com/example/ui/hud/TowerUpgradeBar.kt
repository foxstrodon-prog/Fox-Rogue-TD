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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TowerUpgradeManager
import com.example.model.*
import com.example.ui.GameUIState
import com.example.ui.hud.components.SellTowerPillButton
import com.example.ui.hud.components.TargetPriorityPillButton
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
                cachedPath.reset()
                val r = w * 0.45f
                for (i in 0 until 6) {
                    val angle = (i * Math.PI / 3.0 - Math.PI / 2.0).toFloat()
                    val x = cx + r * kotlin.math.cos(angle)
                    val y = cy + r * kotlin.math.sin(angle)
                    if (i == 0) cachedPath.moveTo(x, y) else cachedPath.lineTo(x, y)
                }
                cachedPath.close()
                drawPath(
                    path = cachedPath,
                    color = color,
                    style = Stroke(width = 1.6f)
                )
                drawCircle(
                    color = color,
                    radius = w * 0.22f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = w * 0.10f,
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
 * Compact Aura Pip Badge rendered in the top-left of Tier-1 Archetype and Tier-2 Aura upgrade squircles.
 * Shows active board count + banked Singularity vault count, and visually distinguishes
 * Stackable (`+`) vs Non-Stacking Capped (`CAP`) auras.
 */
@Composable
fun AuraPipBadge(
    activeCount: Int,
    bankedCount: Int,
    isStackable: Boolean,
    tag: String? = null
) {
    val total = activeCount + bankedCount
    val (label, bgColor, textColor) = when {
        total == 0 -> Triple("★ NEW", Color(0xFF0F172A).copy(alpha = 0.90f), Color(0xFF38BDF8))
        !isStackable -> Triple("CAP ${total}x", Color(0xFF3B1A08).copy(alpha = 0.92f), Color(0xFFFFB800))
        bankedCount > 0 && activeCount > 0 -> Triple("★$activeCount+$bankedCount", Color(0xFF06331E).copy(alpha = 0.92f), Color(0xFF00FF9D))
        bankedCount > 0 -> Triple("⚡${bankedCount}B", Color(0xFF0E294B).copy(alpha = 0.92f), Color(0xFF00F0FF))
        else -> Triple("● ${activeCount}x", Color(0xFF06331E).copy(alpha = 0.92f), Color(0xFF00FF9D))
    }

    Box(
        modifier = Modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(0.8.dp, textColor.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 6.2.sp,
            lineHeight = 7.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1
        )
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
    iconComposable: (@Composable () -> Unit)? = null,
    auraPipBadge: (@Composable () -> Unit)? = null,
    cost: Int? = null,
    badgeText: String = "",
    themeColor: Color,
    canAfford: Boolean,
    isUnlocked: Boolean,
    allowClickWhenLocked: Boolean = false,
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
            .clip(RoundedCornerShape(13.dp))
            .background(
                when {
                    !isUnlocked -> Color(0xFF080C16).copy(alpha = 0.88f)
                    canAfford -> Color(0xFF0A101D).copy(alpha = 0.94f)
                    else -> Color(0xFF070B13).copy(alpha = 0.90f)
                }
            )
            .border(
                1.2.dp,
                when {
                    !isUnlocked -> Color(0xFF334155).copy(alpha = 0.65f)
                    canAfford -> themeColor.copy(alpha = 0.92f)
                    else -> themeColor.copy(alpha = 0.35f)
                },
                RoundedCornerShape(13.dp)
            )
            .clickable(enabled = (canAfford && isUnlocked) || allowClickWhenLocked) {
                coroutineScope.launch {
                    scale.animateTo(0.88f, animationSpec = tween(60))
                    scale.animateTo(1f, animationSpec = tween(100))
                }
                onClick()
            }
            .padding(horizontal = 5.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (iconComposable != null || auraPipBadge != null) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (auraPipBadge != null) {
                    auraPipBadge()
                } else if (iconComposable != null) {
                    Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                        iconComposable()
                    }
                }

                if (!isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = if (cost != null && cost > 0) Color(0xFF94A3B8) else Color(0xFFFF2A55),
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (cost != null && cost > 0) "${cost}c" else "LOCK",
                            color = if (cost != null && cost > 0) Color(0xFF94A3B8) else Color(0xFFFF2A55),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF161F30).copy(alpha = 0.85f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Diamond,
                            contentDescription = null,
                            tint = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF2A55),
                            modifier = Modifier.size(7.5.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = displayPrice,
                            color = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF2A55),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            style = TextStyle(fontFeatureSettings = "tnum"),
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
 * Flanks Target Priority on the left screen edge and Sell on the right screen edge so
 * upgrade squircles remain 100% centered and rapid upgrade taps can never accidentally trigger Sell.
 */
@Composable
fun FloatingSquircleUpgradeBar(
    state: GameUIState,
    onBuildTowerClass: ((MapTile, TowerClass) -> Unit)? = null,
    onBuildTower: ((MapTile, TowerType) -> Unit)? = null,
    onUpgradeToVector: (TowerInstance, TowerVector) -> Unit = { _, _ -> },
    onUpgradeToArchetype: (TowerInstance, TowerArchetype) -> Unit = { _, _ -> },
    onUpgradeToTier3Global: (TowerInstance, Int) -> Unit = { _, _ -> },
    onRequestSingularity: () -> Unit = {},
    onSellTower: () -> Unit = {},
    onChangePriority: (TargetPriority) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isActionActive = state.selectedTower != null || (state.selectedTile != null && state.selectedTile.isBuildable)
    if (!isActionActive) return

    val counterAngle = when (((state.deviceRotationAngle % 360f + 360f) % 360f).toInt()) {
        90 -> -90f
        180 -> 180f
        270 -> 90f
        else -> 0f
    }
    val isRotated = counterAngle == -90f || counterAngle == 90f
    val animatedRotation by animateFloatAsState(
        targetValue = counterAngle,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "squircle_counter_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        when {
            state.selectedTower != null -> {
                val tower = state.selectedTower
                val sellValue = tower.getSellValue()
                val prio = tower.targetPriority

                // Left Edge: Target Priority Pill
                TargetPriorityPillButton(
                    priority = prio,
                    isRotated = isRotated,
                    animatedRotation = animatedRotation,
                    onChangePriority = onChangePriority,
                    modifier = Modifier.align(if (isRotated) Alignment.CenterStart else Alignment.TopStart)
                )

                // Center: Upgrade Squircles (strictly centered across all tiers)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (tower.tier) {
                        0 -> {
                            val vectors = TowerUpgradeManager.getTier1Vectors()
                            val cost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
                            val canAfford = state.scrap >= cost
                            val itemWidth = 72.dp
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
                            val itemWidth = 74.dp
                            val itemHeight = 76.dp

                            archetypes.forEach { arch ->
                                val cost = TowerUpgradeManager.getTier2Cost(arch)
                                val canAfford = state.scrap >= cost
                                val (archTitle, archSub, b1) = getArchetypeCardLabels(arch)
                                val b2 = arch.primaryAura.shortBadge
                                val (pActive, pBanked) = TowerUpgradeManager.getAuraActiveAndBankedCounts(
                                    auraName = arch.primaryAura.name,
                                    towers = state.towers,
                                    activeBoardSynergies = state.activeBoardSynergies
                                )
                                val (sActive, sBanked) = TowerUpgradeManager.getAuraActiveAndBankedCounts(
                                    auraName = arch.secondaryAura.name,
                                    towers = state.towers,
                                    activeBoardSynergies = state.activeBoardSynergies
                                )
                                val totalActive = pActive + sActive
                                val totalBanked = pBanked + sBanked
                                val isStackable = TowerUpgradeManager.isAuraStackable(arch, 0) ||
                                    TowerUpgradeManager.isAuraStackable(arch, 1)

                                SquircleActionButton(
                                    title = archTitle,
                                    subtitle = archSub,
                                    auraPipBadge = {
                                        AuraPipBadge(
                                            activeCount = totalActive,
                                            bankedCount = totalBanked,
                                            isStackable = isStackable,
                                            tag = "aura_pip_arch_${arch.archetypeId}"
                                        )
                                    },
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
                            val arch = tower.archetype ?: mapTypeToArchetype(tower.type)
                            val auraOptions = listOf(arch.primaryAura, arch.secondaryAura)

                            auraOptions.forEachIndexed { idx, aura ->
                                val auraColor = if (idx == 0) Color(0xFFFFD700) else Color(0xFF00FF9D)
                                val (activeCnt, bankedCnt) = TowerUpgradeManager.getAuraActiveAndBankedCounts(
                                    auraName = aura.name,
                                    towers = state.towers,
                                    activeBoardSynergies = state.activeBoardSynergies
                                )
                                val stackable = TowerUpgradeManager.isAuraStackable(arch, idx)
                                SquircleActionButton(
                                    title = aura.name,
                                    subtitle = if (idx == 0) "Primary Aura" else "Secondary Aura",
                                    auraPipBadge = {
                                        AuraPipBadge(
                                            activeCount = activeCnt,
                                            bankedCount = bankedCnt,
                                            isStackable = stackable,
                                            tag = "aura_pip_tier3_$idx"
                                        )
                                    },
                                    cost = cost,
                                    themeColor = auraColor,
                                    canAfford = canAfford,
                                    isUnlocked = true,
                                    bullet1 = aura.shortBadge,
                                    bullet2 = if (stackable) "Stackable Aura" else "Global Aura (Cap)",
                                    itemWidth = itemWidth,
                                    itemHeight = itemHeight,
                                    rotationAngle = animatedRotation,
                                    onClick = { onUpgradeToTier3Global(tower, idx) },
                                    modifier = Modifier.testTag("upgrade_tier3_$idx")
                                )
                            }
                        }

                        else -> {
                            val itemWidth = 84.dp
                            val itemHeight = 76.dp
                            val level = state.level
                            val buildableTiles = level?.tiles?.filter { it.isBuildable } ?: emptyList()
                            val readyTier3Count = state.towers.count { it.tier >= 3 || it.finalFormCode != null || it.isCapstone }
                            val isBoardFull = buildableTiles.isNotEmpty() && state.towers.size >= buildableTiles.size
                            val areAllTowersTier3 = state.towers.isNotEmpty() && state.towers.all { it.tier >= 3 || it.finalFormCode != null || it.isCapstone }
                            val isSingularityGateReady = isBoardFull && areAllTowersTier3
                            val currentMaxGen = state.towers.filter { it.isCapstone }.maxOfOrNull { it.generation } ?: 0
                            val conduitCost = 750 * (currentMaxGen + 1)

                            SingularityGateSquircle(
                                tower = tower,
                                isSingularityGateReady = isSingularityGateReady,
                                readyCount = readyTier3Count,
                                totalBuildable = buildableTiles.size,
                                currentMaxGen = currentMaxGen,
                                conduitCost = conduitCost,
                                currentScrap = state.scrap,
                                itemWidth = itemWidth,
                                itemHeight = itemHeight,
                                animatedRotation = animatedRotation,
                                onRequestSingularity = onRequestSingularity
                            )
                        }
                    }
                }

                // Right Edge: Sell Tower Pill
                SellTowerPillButton(
                    sellValue = sellValue,
                    isRotated = isRotated,
                    animatedRotation = animatedRotation,
                    onSellTower = onSellTower,
                    modifier = Modifier.align(if (isRotated) Alignment.CenterEnd else Alignment.BottomEnd)
                )
            }

            state.selectedTile != null && state.selectedTile.isBuildable -> {
                val tile = state.selectedTile
                val classes = TowerUpgradeManager.getTowerClasses()
                val itemWidth = 72.dp
                val itemHeight = 76.dp

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    classes.forEach { towerClass ->
                        val isUnlocked = TowerUpgradeManager.isClassUnlocked(towerClass, state.towers)
                        val cost = (towerClass.baseCost * state.balanceConfig.buildCostMultiplier).roundToInt()
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
                                if (onBuildTowerClass != null) {
                                    onBuildTowerClass(tile, towerClass)
                                } else {
                                    onBuildTower?.invoke(tile, mapClassToType(towerClass))
                                }
                            },
                            modifier = Modifier.testTag(if (towerClass == TowerClass.KINETIC_BALLISTIC) "build_basic_turret" else "build_class_${towerClass.name}")
                        )
                    }
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
    currentMaxGen: Int,
    conduitCost: Int,
    currentScrap: Int,
    itemWidth: Dp,
    itemHeight: Dp,
    animatedRotation: Float,
    onRequestSingularity: () -> Unit
) {
    val canAfford = currentScrap >= conduitCost
    val nextGen = maxOf(currentMaxGen, tower.generation) + 1

    if (isSingularityGateReady && canAfford) {
        SquircleActionButton(
            title = "CONDUIT",
            subtitle = "GEN $nextGen",
            iconComposable = {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Singularity Ready",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(13.dp)
                )
            },
            cost = conduitCost,
            themeColor = Color(0xFFFFD700),
            canAfford = true,
            isUnlocked = true,
            bullet1 = "Bank Synergies",
            bullet2 = "Ascend Grid",
            itemWidth = itemWidth,
            itemHeight = itemHeight,
            rotationAngle = animatedRotation,
            onClick = onRequestSingularity,
            modifier = Modifier.testTag("singularity_capstone_button")
        )
    } else {
        val anticipationColor = if (!isSingularityGateReady) Color(0xFF64748B) else Color(0xFFFF2A55)
        SquircleActionButton(
            title = "CONDUIT",
            subtitle = if (currentMaxGen > 0 || tower.isCapstone) "GEN $nextGen" else "ASCENSION",
            iconComposable = {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = anticipationColor,
                    modifier = Modifier.size(12.dp)
                )
            },
            cost = conduitCost,
            themeColor = anticipationColor,
            canAfford = canAfford,
            isUnlocked = isSingularityGateReady,
            allowClickWhenLocked = true,
            bullet1 = "$readyCount/$totalBuildable Tier 3",
            bullet2 = if (!isSingularityGateReady) "Max All Towers" else "Needs ${conduitCost}c",
            itemWidth = itemWidth,
            itemHeight = itemHeight,
            rotationAngle = animatedRotation,
            onClick = onRequestSingularity,
            modifier = Modifier.testTag("singularity_conduit_teaser_button")
        )
    }
}

internal fun getArchetypeCardLabels(arch: TowerArchetype): Triple<String, String, String> = when (arch) {
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
    TowerArchetype.BLACK_HOLE_SENTRY -> Triple("Singularity", "Bastion", "Micro Void")
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
    onBuildTower: ((MapTile, TowerType) -> Unit)? = null,
    onBuildTowerClass: ((MapTile, TowerClass) -> Unit)? = null,
    onUpgradeToVector: (TowerInstance, TowerVector) -> Unit = { _, _ -> },
    onUpgradeToArchetype: (TowerInstance, TowerArchetype) -> Unit = { _, _ -> },
    onUpgradeToTier3Global: (TowerInstance, Int) -> Unit = { _, _ -> },
    onRequestSingularity: () -> Unit = {},
    onSellTower: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
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
        modifier = modifier
    )
}
