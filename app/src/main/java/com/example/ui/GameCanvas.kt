package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.*
import kotlin.math.*

@Composable
fun GameCanvas(
    state: GameUIState,
    onTileClicked: (MapTile) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = state.level ?: return
    val gridWidth = level.width
    val gridHeight = level.height
    val biome = level.biome

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(level.seed) {
                detectTapGestures { offset ->
                    val cellW = size.width / gridWidth
                    val cellH = size.height / gridHeight
                    val gx = (offset.x / cellW).toInt().coerceIn(0, gridWidth - 1)
                    val gy = (offset.y / cellH).toInt().coerceIn(0, gridHeight - 1)
                    val clickedTile = level.tiles.firstOrNull { it.pos.x == gx && it.pos.y == gy }
                    if (clickedTile != null) {
                        onTileClicked(clickedTile)
                    }
                }
            }
    ) {
        val cellW = size.width / gridWidth
        val cellH = size.height / gridHeight

        val shakeX = if (state.screenShakeIntensity > 0f) (sin(System.currentTimeMillis() * 0.05f) * state.screenShakeIntensity * 12f).toFloat() else 0f
        val shakeY = if (state.screenShakeIntensity > 0f) (cos(System.currentTimeMillis() * 0.05f) * state.screenShakeIntensity * 12f).toFloat() else 0f

        // Draw Base Terrain Grid
        drawRect(color = biome.groundColor)

        // Subtle tactical grid lines
        val gridLineColor = biome.groundSecondary.copy(alpha = 0.5f)
        for (x in 0..gridWidth) {
            drawLine(
                color = gridLineColor,
                start = Offset(x * cellW + shakeX, 0f + shakeY),
                end = Offset(x * cellW + shakeX, size.height + shakeY),
                strokeWidth = 1.dp.toPx()
            )
        }
        for (y in 0..gridHeight) {
            drawLine(
                color = gridLineColor,
                start = Offset(0f + shakeX, y * cellH + shakeY),
                end = Offset(size.width + shakeX, y * cellH + shakeY),
                strokeWidth = 1.dp.toPx()
            )
        }

        val animFlowTime = if (state.animTimeMs > 0L) state.animTimeMs else System.currentTimeMillis()

        // 1. Draw Clean Organic Gradient Pathways (Merge into radiant violet, then split naturally)
        drawSmoothMergedPathways(
            laneAlpha = level.laneAlpha,
            laneBeta = level.laneBeta,
            intersections = level.intersectionPoints,
            cellW = cellW,
            cellH = cellH,
            shakeX = shakeX,
            shakeY = shakeY,
            pathColor = biome.pathColor,
            colorAlpha = Color(0xFF00F0FF),
            colorBeta = Color(0xFFFFB800),
            colorMerged = Color(0xFFC084FC), // Radiant violet gradient shift when merged!
            animTime = animFlowTime
        )

        // Draw Tiles (Obstacles, Buffs, Spawners, Core)
        for (tile in level.tiles) {
            val tx = tile.pos.x * cellW + shakeX
            val ty = tile.pos.y * cellH + shakeY
            val centerX = tx + cellW * 0.5f
            val centerY = ty + cellH * 0.5f

            when (tile.type) {
                TileType.PATH_MERGED, TileType.PATH_INTERSECTION -> {
                    // Smooth, seamless energy junction - no bulky train ties or odd overlay tiles
                    val pulse = (sin(animFlowTime * 0.005f) * 0.15f + 0.85f).toFloat()
                    drawCircle(
                        color = Color(0xFFC084FC).copy(alpha = 0.20f * pulse),
                        radius = cellW * 0.35f,
                        center = Offset(centerX, centerY)
                    )
                }
                TileType.OBSTACLE -> {
                    // Gray box with X through it - clear dead tile with no confusion
                    val obsRect = Size(cellW * 0.72f, cellH * 0.72f)
                    val left = centerX - obsRect.width / 2
                    val top = centerY - obsRect.height / 2
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(left, top),
                        size = obsRect,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = Color(0xFF334155),
                        topLeft = Offset(left, top),
                        size = obsRect,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                        style = Stroke(width = 1.0f)
                    )
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(left + 6f, top + 6f),
                        end = Offset(left + obsRect.width - 6f, top + obsRect.height - 6f),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(left + obsRect.width - 6f, top + 6f),
                        end = Offset(left + 6f, top + obsRect.height - 6f),
                        strokeWidth = 1.5f
                    )
                }
                TileType.SPAWNER_ALPHA -> {
                    drawCircle(color = Color(0xFFFF2A55).copy(alpha = 0.3f), radius = cellW * 0.44f, center = Offset(centerX, centerY))
                    drawCircle(color = Color(0xFF00F0FF), radius = cellW * 0.28f, center = Offset(centerX, centerY), style = Stroke(width = 2.5f))
                    drawCircle(color = Color.White, radius = cellW * 0.12f, center = Offset(centerX, centerY))
                }
                TileType.SPAWNER_BETA -> {
                    drawCircle(color = Color(0xFFFF2A55).copy(alpha = 0.3f), radius = cellW * 0.44f, center = Offset(centerX, centerY))
                    drawCircle(color = Color(0xFFFF9100), radius = cellW * 0.28f, center = Offset(centerX, centerY), style = Stroke(width = 2.5f))
                    drawCircle(color = Color.White, radius = cellW * 0.12f, center = Offset(centerX, centerY))
                }
                TileType.CORE -> {
                    val pulse = (sin(animFlowTime * 0.005f) * 0.15f + 0.85f).toFloat()
                    drawCircle(color = Color(0xFF00FF9D).copy(alpha = 0.25f * pulse), radius = cellW * 0.52f, center = Offset(centerX, centerY))
                    drawCircle(color = Color(0xFF00FF9D), radius = cellW * 0.35f * pulse, center = Offset(centerX, centerY), style = Stroke(width = 3f))
                    drawCircle(color = Color(0xFF00F0FF), radius = cellW * 0.22f, center = Offset(centerX, centerY), style = Stroke(width = 1.5f))
                    drawCircle(color = Color.White, radius = cellW * 0.12f, center = Offset(centerX, centerY))
                }
                TileType.EMPTY -> {
                    if (tile.buff != TileBuff.NONE) {
                        val buffColor = Color(tile.buff.badgeColor)
                        val pulse = (sin(animFlowTime * 0.006f) * 0.20f + 0.80f).toFloat()
                        // Glowing translucent floor plate
                        drawRoundRect(
                            color = buffColor.copy(alpha = 0.18f * pulse),
                            topLeft = Offset(tx + 2f, ty + 2f),
                            size = Size(cellW - 4f, cellH - 4f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        // Radiant cyber border
                        drawRoundRect(
                            color = buffColor.copy(alpha = 0.85f),
                            topLeft = Offset(tx + 2f, ty + 2f),
                            size = Size(cellW - 4f, cellH - 4f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                            style = Stroke(width = 1.8f)
                        )

                        // Uniform cybernetic energy matrix (clean concentric nodes for all 3 colors)
                        drawCircle(
                            color = buffColor.copy(alpha = 0.40f * pulse),
                            radius = cellW * 0.22f,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = buffColor,
                            radius = cellW * 0.14f,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 2.0f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = cellW * 0.05f,
                            center = Offset(centerX, centerY)
                        )

                        // If Harmonic Grid is fully active, add outer golden harmony ring
                        if (state.harmonicGridActive) {
                            val harmonicPulse = (sin(animFlowTime * 0.010f) * 0.25f + 0.75f).toFloat()
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = 0.70f * harmonicPulse),
                                radius = cellW * 0.34f,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }
                }
                else -> Unit
            }

            if (state.selectedTile?.pos == tile.pos) {
                drawRoundRect(
                    color = Color(0xFF00F0FF),
                    topLeft = Offset(tx + 2f, ty + 2f),
                    size = Size(cellW - 4f, cellH - 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                    style = Stroke(width = 2.5f)
                )
            }
        }

        // Draw Dynamic Track Sparks flowing along lanes
        for (tp in state.trackParticles) {
            val path = if (tp.laneIndex == 0) level.laneAlpha else level.laneBeta
            if (tp.segmentIndex < path.size - 1) {
                val p0 = path[tp.segmentIndex]
                val p1 = path[tp.segmentIndex + 1]
                val sx = (p0.x + 0.5f) * cellW + shakeX
                val sy = (p0.y + 0.5f) * cellH + shakeY
                val ex = (p1.x + 0.5f) * cellW + shakeX
                val ey = (p1.y + 0.5f) * cellH + shakeY
                val px = sx + (ex - sx) * tp.progress
                val py = sy + (ey - sy) * tp.progress

                drawCircle(
                    color = Color(tp.color).copy(alpha = 0.85f),
                    radius = tp.size * 1.2f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White,
                    radius = tp.size * 0.5f,
                    center = Offset(px, py)
                )
            }
        }

        // Spatial Aura Connections: When a Tier 3 (Global Aura) tower is selected,
        // render glowing energy lines and animated pulse nodes to all other towers on the grid!
        state.selectedTower?.let { selTower ->
            if (selTower.isTier3) {
                val arch = selTower.archetype ?: mapTypeToArchetype(selTower.type)
                val auraColor = Color(arch.color)
                val emitterX = (selTower.gridPos.x + 0.5f) * cellW + shakeX
                val emitterY = (selTower.gridPos.y + 0.5f) * cellH + shakeY
                val auraPulsePhase = ((animFlowTime % 1200L) / 1200f)

                for (other in state.towers) {
                    if (other.id != selTower.id) {
                        val receiverX = (other.gridPos.x + 0.5f) * cellW + shakeX
                        val receiverY = (other.gridPos.y + 0.5f) * cellH + shakeY

                        // Glowing connective energy line
                        drawLine(
                            color = auraColor.copy(alpha = 0.35f),
                            start = Offset(emitterX, emitterY),
                            end = Offset(receiverX, receiverY),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.70f),
                            start = Offset(emitterX, emitterY),
                            end = Offset(receiverX, receiverY),
                            strokeWidth = 0.8.dp.toPx()
                        )

                        // Animated traveling energy node from Tier 3 emitter to empowered tower
                        val nodeX = emitterX + (receiverX - emitterX) * auraPulsePhase
                        val nodeY = emitterY + (receiverY - emitterY) * auraPulsePhase
                        drawCircle(
                            color = auraColor,
                            radius = 4.dp.toPx(),
                            center = Offset(nodeX, nodeY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(nodeX, nodeY)
                        )

                        // Empowered receiver glow ring
                        drawCircle(
                            color = auraColor.copy(alpha = 0.40f),
                            radius = cellW * 0.45f,
                            center = Offset(receiverX, receiverY),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }
        }

        // Draw Selected Tower Range Circle
        state.selectedTower?.let { tower ->
            val tile = level.tiles.firstOrNull { it.pos == tower.gridPos }
            val tileBuff = tile?.buff ?: TileBuff.NONE
            val arch = tower.archetype
            val baseRng = arch?.baseRange ?: tower.type.baseRange
            val rangeBonus = (if (tileBuff == TileBuff.HIGH_GROUND) 0.30f else 0.0f) +
                    (if (tower.branchTier1 == "C") 0.35f else 0.0f) +
                    (if (tower.isTier3) 0.35f else if (tower.isTier2) 0.20f else 0.0f)
            val effectiveRange = baseRng * (1.0f + rangeBonus)
            val pixelRadius = effectiveRange * cellW

            val tcx = (tower.gridPos.x + 0.5f) * cellW + shakeX
            val tcy = (tower.gridPos.y + 0.5f) * cellH + shakeY

            val rangeColor = Color(arch?.color ?: tower.type.color)
            drawCircle(
                color = rangeColor.copy(alpha = 0.12f),
                radius = pixelRadius,
                center = Offset(tcx, tcy)
            )
            drawCircle(
                color = rangeColor.copy(alpha = 0.5f),
                radius = pixelRadius,
                center = Offset(tcx, tcy),
                style = Stroke(width = 1.5f)
            )
        }

        // Draw Towers with Distinctive Silhouette (Circle for Kinetic, Triangle for Elemental, Square for Heavy)
        for (tower in state.towers) {
            val tcx = (tower.gridPos.x + 0.5f) * cellW + shakeX
            val tcy = (tower.gridPos.y + 0.5f) * cellH + shakeY
            val effectiveClass = tower.archetype?.towerClass ?: tower.towerClass
            val arch = tower.archetype
            val towerColor = Color(arch?.color ?: effectiveClass.color)
            val rad = tower.rotationAngleDeg * (PI / 180f).toFloat()
            val borderColor = if (tower.isFinalForm) Color(0xFFFFB800) else towerColor.copy(alpha = 0.88f)

            // === 1. FINAL FORM RADIANT HALO (Class-Specific Geometry) ===
            if (tower.isFinalForm) {
                val auraPulse = (sin(animFlowTime * 0.008f) * 0.15f + 0.85f).toFloat()
                val haloColor = Color(0xFFFFB800)
                when (effectiveClass) {
                    TowerClass.KINETIC_BALLISTIC -> {
                        // Radiant Circle Halo
                        drawCircle(
                            color = haloColor.copy(alpha = 0.25f * auraPulse),
                            radius = cellW * 0.48f,
                            center = Offset(tcx, tcy)
                        )
                        drawCircle(
                            color = haloColor,
                            radius = cellW * 0.44f,
                            center = Offset(tcx, tcy),
                            style = Stroke(width = 2f)
                        )
                    }
                    TowerClass.ELEMENTAL_VOID -> {
                        // Radiant Triangle Halo
                        val hRadius = cellW * 0.52f * auraPulse
                        val haloTri = Path().apply {
                            moveTo(tcx, tcy - hRadius)
                            lineTo(tcx + hRadius * 0.866f, tcy + hRadius * 0.5f)
                            lineTo(tcx - hRadius * 0.866f, tcy + hRadius * 0.5f)
                            close()
                        }
                        drawPath(path = haloTri, color = haloColor.copy(alpha = 0.20f * auraPulse))
                        drawPath(path = haloTri, color = haloColor, style = Stroke(width = 2f, join = StrokeJoin.Round))
                    }
                    TowerClass.HEAVY_SIEGE -> {
                        // Radiant Fortified Square Halo
                        val hSize = cellW * 0.88f * auraPulse
                        val hTopLeft = Offset(tcx - hSize / 2f, tcy - hSize / 2f)
                        drawRoundRect(
                            color = haloColor.copy(alpha = 0.20f * auraPulse),
                            topLeft = hTopLeft,
                            size = Size(hSize, hSize),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = haloColor,
                            topLeft = hTopLeft,
                            size = Size(hSize, hSize),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            // === 2. TOWER BASE CHASSIS (Circle, Triangle, Square) ===
            when (effectiveClass) {
                TowerClass.KINETIC_BALLISTIC -> {
                    // === KINETIC BALLISTICS: CIRCLE BASE ===
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = cellW * 0.38f,
                        center = Offset(tcx, tcy)
                    )
                    drawCircle(
                        color = borderColor,
                        radius = cellW * 0.38f,
                        center = Offset(tcx, tcy),
                        style = Stroke(width = 2.5f)
                    )

                    // Rotary barrel or ballistic cannons protruding along rad
                    val barrelLen = cellW * 0.35f
                    val b1X = tcx + cos(rad) * barrelLen
                    val b1Y = tcy + sin(rad) * barrelLen
                    drawLine(color = towerColor, start = Offset(tcx, tcy), end = Offset(b1X, b1Y), strokeWidth = cellW * 0.12f, cap = StrokeCap.Round)

                    if (tower.branchTier1 == "A" || tower.isFinalForm) {
                        val perpAngle = rad + (PI / 2f).toFloat()
                        val offX = cos(perpAngle) * (cellW * 0.08f)
                        val offY = sin(perpAngle) * (cellW * 0.08f)
                        drawLine(color = Color.White, start = Offset(tcx + offX, tcy + offY), end = Offset(b1X + offX, b1Y + offY), strokeWidth = cellW * 0.07f, cap = StrokeCap.Round)
                    }
                    drawCircle(color = Color.White, radius = cellW * 0.14f, center = Offset(tcx, tcy))
                    drawCircle(color = towerColor, radius = cellW * 0.08f, center = Offset(tcx, tcy))
                }

                TowerClass.ELEMENTAL_VOID -> {
                    // === ELEMENTAL VOID: TRIANGLE BASE ===
                    val triRadius = cellW * 0.44f
                    val p1 = Offset(tcx, tcy - triRadius)
                    val p2 = Offset(tcx + triRadius * 0.866f, tcy + triRadius * 0.5f)
                    val p3 = Offset(tcx - triRadius * 0.866f, tcy + triRadius * 0.5f)

                    val triPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    }

                    drawPath(path = triPath, color = Color(0xFF140D26))
                    drawPath(path = triPath, color = borderColor, style = Stroke(width = 2.5f, join = StrokeJoin.Round))

                    // Inverted inner rune triangle
                    val innerRadius = cellW * 0.25f
                    val innerTri = Path().apply {
                        moveTo(tcx, tcy + innerRadius)
                        lineTo(tcx - innerRadius * 0.866f, tcy - innerRadius * 0.5f)
                        lineTo(tcx + innerRadius * 0.866f, tcy - innerRadius * 0.5f)
                        close()
                    }
                    drawPath(path = innerTri, color = towerColor.copy(alpha = 0.30f))
                    drawPath(path = innerTri, color = borderColor.copy(alpha = 0.60f), style = Stroke(width = 1.2f, join = StrokeJoin.Round))

                    // Elemental power crystals at 3 triangle vertices
                    drawCircle(color = borderColor, radius = cellW * 0.05f, center = p1)
                    drawCircle(color = borderColor, radius = cellW * 0.05f, center = p2)
                    drawCircle(color = borderColor, radius = cellW * 0.05f, center = p3)

                    // Central focusing crystal / emitter pointing along rad
                    val beamX = tcx + cos(rad) * (cellW * 0.34f)
                    val beamY = tcy + sin(rad) * (cellW * 0.34f)
                    drawLine(color = towerColor, start = Offset(tcx, tcy), end = Offset(beamX, beamY), strokeWidth = cellW * 0.09f, cap = StrokeCap.Round)
                    drawLine(color = Color.White, start = Offset(tcx, tcy), end = Offset(beamX, beamY), strokeWidth = cellW * 0.04f, cap = StrokeCap.Round)

                    // Orbiting elemental motes
                    val orbAngle = animFlowTime * 0.005f
                    for (i in 0 until 3) {
                        val a = orbAngle + i * (2f * PI.toFloat() / 3f)
                        val ox = tcx + cos(a) * (cellW * 0.22f)
                        val oy = tcy + sin(a) * (cellW * 0.22f)
                        drawCircle(color = towerColor, radius = cellW * 0.045f, center = Offset(ox, oy))
                    }
                    drawCircle(color = Color.White, radius = cellW * 0.10f, center = Offset(tcx, tcy))
                    drawCircle(color = towerColor, radius = cellW * 0.06f, center = Offset(tcx, tcy))
                }

                TowerClass.HEAVY_SIEGE -> {
                    // === HEAVY SIEGE: SQUARE BASE (Fortified Bunker) ===
                    val sqSize = cellW * 0.72f
                    val sqTopLeft = Offset(tcx - sqSize / 2f, tcy - sqSize / 2f)
                    drawRoundRect(
                        color = Color(0xFF181310),
                        topLeft = sqTopLeft,
                        size = Size(sqSize, sqSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = borderColor,
                        topLeft = sqTopLeft,
                        size = Size(sqSize, sqSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                        style = Stroke(width = 2.5f)
                    )

                    // 4 Fortified corner rivets
                    val rivetOffset = sqSize * 0.36f
                    val rivets = listOf(
                        Offset(tcx - rivetOffset, tcy - rivetOffset),
                        Offset(tcx + rivetOffset, tcy - rivetOffset),
                        Offset(tcx - rivetOffset, tcy + rivetOffset),
                        Offset(tcx + rivetOffset, tcy + rivetOffset)
                    )
                    for (rivet in rivets) {
                        drawCircle(color = borderColor, radius = 2.5.dp.toPx(), center = rivet)
                        drawCircle(color = Color.Black, radius = 1.0.dp.toPx(), center = rivet)
                    }

                    // Heavy dual siege barrels extending along rad
                    val barrelLen = cellW * 0.36f
                    val bX = tcx + cos(rad) * barrelLen
                    val bY = tcy + sin(rad) * barrelLen
                    drawLine(color = Color(0xFF3E2723), start = Offset(tcx, tcy), end = Offset(bX, bY), strokeWidth = cellW * 0.22f, cap = StrokeCap.Square)
                    drawLine(color = towerColor, start = Offset(tcx, tcy), end = Offset(bX, bY), strokeWidth = cellW * 0.14f, cap = StrokeCap.Square)

                    // Heavy square turret hatch
                    val hatchSize = cellW * 0.28f
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(tcx - hatchSize / 2f, tcy - hatchSize / 2f),
                        size = Size(hatchSize, hatchSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = towerColor,
                        topLeft = Offset(tcx - hatchSize / 2f, tcy - hatchSize / 2f),
                        size = Size(hatchSize, hatchSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }

            // === 3. TIER FORM BADGE (Circle, Triangle, Square matching the class) ===
            val badgeY = tcy + cellW * 0.28f
            val badgeColor = if (tower.isFinalForm) Color(0xFFFFB800) else Color(0xFF00FF9D)
            when (effectiveClass) {
                TowerClass.KINETIC_BALLISTIC -> {
                    drawCircle(color = badgeColor, radius = if (tower.isFinalForm) 3.5f else 2.5f, center = Offset(tcx, badgeY))
                }
                TowerClass.ELEMENTAL_VOID -> {
                    val bR = if (tower.isFinalForm) 4.5f else 3.5f
                    val bTri = Path().apply {
                        moveTo(tcx, badgeY - bR)
                        lineTo(tcx + bR * 0.866f, badgeY + bR * 0.5f)
                        lineTo(tcx - bR * 0.866f, badgeY + bR * 0.5f)
                        close()
                    }
                    drawPath(path = bTri, color = badgeColor)
                }
                TowerClass.HEAVY_SIEGE -> {
                    val bS = if (tower.isFinalForm) 6f else 5f
                    drawRect(color = badgeColor, topLeft = Offset(tcx - bS / 2f, badgeY - bS / 2f), size = Size(bS, bS))
                }
            }
        }

        // Draw Laser Beams
        for (beam in state.laserBeams) {
            val bStartX = (beam.startX + 0.5f) * cellW + shakeX
            val bStartY = (beam.startY + 0.5f) * cellH + shakeY
            val bEndX = (beam.endX + 0.5f) * cellW + shakeX
            val bEndY = (beam.endY + 0.5f) * cellH + shakeY
            val beamColor = Color(beam.color)

            drawLine(color = beamColor.copy(alpha = 0.5f), start = Offset(bStartX, bStartY), end = Offset(bEndX, bEndY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color = Color.White, start = Offset(bStartX, bStartY), end = Offset(bEndX, bEndY), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }

        // Draw Lightning Arcs
        for (arc in state.lightningArcs) {
            val arcColor = Color(arc.color)
            for (i in 0 until arc.points.size - 1) {
                val p0 = arc.points[i]
                val p1 = arc.points[i + 1]
                val sx = (p0.first + 0.5f) * cellW + shakeX
                val sy = (p0.second + 0.5f) * cellH + shakeY
                val ex = (p1.first + 0.5f) * cellW + shakeX
                val ey = (p1.second + 0.5f) * cellH + shakeY

                val midX = (sx + ex) / 2f + (sin(i * 3.7f) * 10f)
                val midY = (sy + ey) / 2f + (cos(i * 3.7f) * 10f)

                drawLine(color = arcColor.copy(alpha = 0.6f), start = Offset(sx, sy), end = Offset(midX, midY), strokeWidth = 3.5.dp.toPx())
                drawLine(color = Color.White, start = Offset(sx, sy), end = Offset(midX, midY), strokeWidth = 1.5.dp.toPx())
                drawLine(color = arcColor.copy(alpha = 0.6f), start = Offset(midX, midY), end = Offset(ex, ey), strokeWidth = 3.5.dp.toPx())
                drawLine(color = Color.White, start = Offset(midX, midY), end = Offset(ex, ey), strokeWidth = 1.5.dp.toPx())
            }
        }

        // Draw Projectiles
        for (proj in state.projectiles) {
            val px = (proj.currentX + 0.5f) * cellW + shakeX
            val py = (proj.currentY + 0.5f) * cellH + shakeY
            val projColor = Color(proj.color)

            if (proj.isMortarArc) {
                val progress = (proj.traveledDistance / proj.totalDistance).coerceIn(0f, 1f)
                val arcHeight = sin(progress * PI.toFloat()) * (cellH * 1.3f)
                drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = cellW * 0.12f, center = Offset(px, py))
                drawCircle(color = projColor, radius = cellW * 0.16f, center = Offset(px, py - arcHeight))
                drawCircle(color = Color.White, radius = cellW * 0.08f, center = Offset(px, py - arcHeight))
            } else {
                drawCircle(color = projColor.copy(alpha = 0.6f), radius = cellW * 0.11f, center = Offset(px, py))
                drawCircle(color = Color.White, radius = cellW * 0.06f, center = Offset(px, py))
            }
        }

        // Draw Enemies on BOTH lanes
        for (enemy in state.enemies) {
            val ecx = (enemy.worldX + 0.5f) * cellW + shakeX
            val ecy = (enemy.worldY + 0.5f) * cellH + shakeY
            val enemyColor = Color(enemy.type.color)
            val enemyRadius = cellW * 0.28f * enemy.type.sizeRatio

            if (enemy.slowTimerMs > 0) {
                drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.4f), radius = enemyRadius * 1.4f, center = Offset(ecx, ecy))
            }
            if (enemy.shockTimerMs > 0) {
                drawCircle(color = Color(0xFFB026FF).copy(alpha = 0.4f), radius = enemyRadius * 1.35f, center = Offset(ecx, ecy), style = Stroke(width = 2f))
            }

            if (enemy.isBoss) {
                drawCircle(color = Color(0xFF581C87), radius = enemyRadius * 1.25f, center = Offset(ecx, ecy))
                drawCircle(color = enemyColor, radius = enemyRadius, center = Offset(ecx, ecy))
                drawCircle(color = Color.White, radius = enemyRadius * 0.35f, center = Offset(ecx, ecy))
            } else {
                drawCircle(color = enemyColor, radius = enemyRadius, center = Offset(ecx, ecy))
                drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = enemyRadius * 0.4f, center = Offset(ecx, ecy))
            }

            val hpBarW = cellW * 0.70f * enemy.type.sizeRatio
            val hpBarH = 4.dp.toPx()
            val hpBarY = ecy - enemyRadius - 7.dp.toPx()
            val hpRatio = (enemy.currentHp / enemy.maxHp).coerceIn(0f, 1f)

            drawRoundRect(color = Color.Black.copy(alpha = 0.75f), topLeft = Offset(ecx - hpBarW / 2, hpBarY), size = Size(hpBarW, hpBarH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
            val hpColor = when {
                hpRatio > 0.5f -> Color(0xFF00FF9D)
                hpRatio > 0.25f -> Color(0xFFFFB800)
                else -> Color(0xFFFF2A55)
            }
            drawRoundRect(color = hpColor, topLeft = Offset(ecx - hpBarW / 2, hpBarY), size = Size(hpBarW * hpRatio, hpBarH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
        }

        // Draw Explosions
        for (exp in state.explosions) {
            val ex = (exp.x + 0.5f) * cellW + shakeX
            val ey = (exp.y + 0.5f) * cellH + shakeY
            val radiusPx = exp.currentRadius * cellW
            val alpha = (exp.lifeMs.toFloat() / exp.maxLifeMs).coerceIn(0f, 1f)

            drawCircle(color = Color(exp.color).copy(alpha = 0.28f * alpha), radius = radiusPx, center = Offset(ex, ey))
            drawCircle(color = Color(exp.color).copy(alpha = 0.8f * alpha), radius = radiusPx, center = Offset(ex, ey), style = Stroke(width = 3f))
        }

        // Draw Floating Combat Text
        for (ft in state.floatingTexts) {
            val fx = (ft.x + 0.5f) * cellW + shakeX
            val fy = (ft.y + 0.5f) * cellH + shakeY
            val alpha = (ft.lifeMs.toFloat() / ft.maxLifeMs).coerceIn(0f, 1f)

            drawCircle(
                color = Color(ft.color).copy(alpha = 0.25f * alpha),
                radius = if (ft.isCrit) 16.dp.toPx() else 10.dp.toPx(),
                center = Offset(fx, fy)
            )
        }

        // Tactical Tutorial Pointer: Animated bouncing arrow pointing to a boosted pad on Wave 1
        if (!state.hasClickedFirstTile && state.waveNumber == 1 && state.towers.isEmpty()) {
            val targetTile = level.tiles.firstOrNull { it.type == TileType.EMPTY && it.buff != TileBuff.NONE }
            if (targetTile != null) {
                val bx = (targetTile.pos.x + 0.5f) * cellW + shakeX
                val by = (targetTile.pos.y + 0.5f) * cellH + shakeY
                val bounce = (sin(animFlowTime * 0.008f) * 8f)
                val arrowTipY = by - cellH * 0.40f + bounce
                val buffColor = Color(targetTile.buff.badgeColor)

                // Downward pointing arrow chevron
                val arrowPath = Path().apply {
                    moveTo(bx, arrowTipY)
                    lineTo(bx - cellW * 0.22f, arrowTipY - cellH * 0.28f)
                    lineTo(bx - cellW * 0.09f, arrowTipY - cellH * 0.28f)
                    lineTo(bx - cellW * 0.09f, arrowTipY - cellH * 0.50f)
                    lineTo(bx + cellW * 0.09f, arrowTipY - cellH * 0.50f)
                    lineTo(bx + cellW * 0.09f, arrowTipY - cellH * 0.28f)
                    lineTo(bx + cellW * 0.22f, arrowTipY - cellH * 0.28f)
                    close()
                }
                drawPath(arrowPath, color = buffColor)
                drawPath(arrowPath, color = Color.White, style = Stroke(width = 2.0f))

                // Pulsing highlight circle on the boosted tile pad
                val ringPulse = (sin(animFlowTime * 0.007f) * 0.25f + 0.75f).toFloat()
                drawCircle(
                    color = buffColor.copy(alpha = 0.6f * ringPulse),
                    radius = cellW * 0.42f,
                    center = Offset(bx, by),
                    style = Stroke(width = 2.5f)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothMergedPathways(
    laneAlpha: List<GridPos>,
    laneBeta: List<GridPos>,
    intersections: Set<GridPos>,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float,
    pathColor: Color,
    colorAlpha: Color,
    colorBeta: Color,
    colorMerged: Color,
    animTime: Long
) {
    if (laneAlpha.size < 2 || laneBeta.size < 2) return

    fun buildPath(points: List<GridPos>): Path {
        val p = Path()
        val start = Offset((points.first().x + 0.5f) * cellW + shakeX, (points.first().y + 0.5f) * cellH + shakeY)
        p.moveTo(start.x, start.y)
        for (i in 1 until points.size) {
            val pt = points[i]
            p.lineTo((pt.x + 0.5f) * cellW + shakeX, (pt.y + 0.5f) * cellH + shakeY)
        }
        return p
    }

    val pathA = buildPath(laneAlpha)
    val pathB = buildPath(laneBeta)

    // 1. Draw Asphalt/Terrain Underbed (smooth rounded curves)
    drawPath(path = pathA, color = pathColor, style = Stroke(width = cellW * 0.72f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = pathB, color = pathColor, style = Stroke(width = cellW * 0.72f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 2. Draw Clean Glowing Conduit Lines (without arrows!)
    drawPath(path = pathA, color = colorAlpha.copy(alpha = 0.45f), style = Stroke(width = cellW * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = pathB, color = colorBeta.copy(alpha = 0.45f), style = Stroke(width = cellW * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 3. Highlight the Merged Shared Segment with Radiant Violet/Indigo Gradient Shift
    // Extract strictly contiguous consecutive segments to prevent any disconnected jump lines
    val contiguousSegments = mutableListOf<List<GridPos>>()
    var curSegment = mutableListOf<GridPos>()

    for (pt in laneAlpha) {
        // Exclude the final core tile to prevent any stray line connecting to the core
        if (pt in intersections && pt != laneAlpha.last()) {
            if (curSegment.isEmpty()) {
                curSegment.add(pt)
            } else {
                val last = curSegment.last()
                val isAdjacent = (abs(pt.x - last.x) + abs(pt.y - last.y)) == 1
                if (isAdjacent) {
                    curSegment.add(pt)
                } else {
                    if (curSegment.size >= 2) {
                        contiguousSegments.add(curSegment)
                    }
                    curSegment = mutableListOf(pt)
                }
            }
        } else {
            if (curSegment.size >= 2) {
                contiguousSegments.add(curSegment)
            }
            curSegment = mutableListOf()
        }
    }
    if (curSegment.size >= 2) {
        contiguousSegments.add(curSegment)
    }

    for (segment in contiguousSegments) {
        val mergedPath = buildPath(segment)
        // Vibrant purple-violet convergence glow
        drawPath(
            path = mergedPath,
            color = colorMerged.copy(alpha = 0.75f),
            style = Stroke(width = cellW * 0.30f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = mergedPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = cellW * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
