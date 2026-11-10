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
import androidx.compose.ui.graphics.drawscope.withTransform
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
                    val boardAspect = gridWidth.toFloat() / gridHeight.toFloat()
                    val viewAspect = size.width.toFloat() / size.height.toFloat()
                    val (bw, bh) = if (viewAspect < boardAspect) {
                        val w = size.width.toFloat()
                        w to (w / boardAspect)
                    } else {
                        val h = size.height.toFloat()
                        (h * boardAspect) to h
                    }
                    val ox = (size.width.toFloat() - bw) / 2f
                    val oy = (size.height.toFloat() - bh) / 2f
                    val cellW = bw / gridWidth
                    val cellH = bh / gridHeight

                    val localX = offset.x - ox
                    val localY = offset.y - oy
                    if (localX in 0f..bw && localY in 0f..bh) {
                        val gx = (localX / cellW).toInt().coerceIn(0, gridWidth - 1)
                        val gy = (localY / cellH).toInt().coerceIn(0, gridHeight - 1)
                        val clickedTile = level.tiles.firstOrNull { it.pos.x == gx && it.pos.y == gy }
                        if (clickedTile != null) {
                            onTileClicked(clickedTile)
                        }
                    }
                }
            }
    ) {
        val boardAspect = gridWidth.toFloat() / gridHeight.toFloat()
        val viewAspect = size.width / size.height

        val (boardWidthPx, boardHeightPx) = if (viewAspect < boardAspect) {
            val bw = size.width
            val bh = bw / boardAspect
            bw to bh
        } else {
            val bh = size.height
            val bw = bh * boardAspect
            bw to bh
        }

        val boardOriginX = (size.width - boardWidthPx) / 2f
        val boardOriginY = (size.height - boardHeightPx) / 2f

        val cellW = boardWidthPx / gridWidth
        val cellH = boardHeightPx / gridHeight

        val shakeX = if (state.screenShakeIntensity > 0f) (sin(System.currentTimeMillis() * 0.05f) * state.screenShakeIntensity * 12f).toFloat() else 0f
        val shakeY = if (state.screenShakeIntensity > 0f) (cos(System.currentTimeMillis() * 0.05f) * state.screenShakeIntensity * 12f).toFloat() else 0f

        // Draw Base Terrain Background
        drawRect(color = Color(0xFF070B13))

        withTransform({
            translate(boardOriginX, boardOriginY)
        }) {
            val themeSlot = MapThemeSlot.fromSlot(state.mapThemeSlot)
            val groundColor = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> biome.groundColor
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFF0B192C)
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFF1C1917)
                MapThemeSlot.SLOT_4_VOID -> Color(0xFF090514)
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFF020B05)
            }
            val gridLineColor = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> biome.groundSecondary.copy(alpha = 0.5f)
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFF1E3E62).copy(alpha = 0.7f)
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFF292524).copy(alpha = 0.7f)
                MapThemeSlot.SLOT_4_VOID -> Color(0xFF2E1065).copy(alpha = 0.7f)
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFF064E3B).copy(alpha = 0.7f)
            }
            val pathBaseColor = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> biome.pathColor
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFF16253D)
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFF44403C)
                MapThemeSlot.SLOT_4_VOID -> Color(0xFF1E103A)
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFF065F46)
            }
            // Strict Contrast Audit (TASK 5): Path 1, Path 2, and Intersection must each utilize
            // distinct, contrasting colors with zero shared or mimicking hues.
            val colorAlpha = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> Color(0xFF00F0FF) // Electric Neon Cyan
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFF00E5FF) // Blueprint Technical Cyan
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFFF59E0B) // Desert Tactical Amber
                MapThemeSlot.SLOT_4_VOID -> Color(0xFFA855F7) // Astral Cosmic Purple
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFF10B981) // Terminal Matrix Emerald
            }
            val colorBeta = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> Color(0xFFFF6B00) // Solar Flame Orange (high contrast vs cyan & fuchsia)
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFFFFD600) // High-Voltage Signal Yellow (high contrast vs cyan & coral)
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFF06B6D4) // Arctic Ice Cyan (cool contrast vs warm amber & lime)
                MapThemeSlot.SLOT_4_VOID -> Color(0xFFF43F5E) // Supernova Rose Pink (high contrast vs purple & aquamarine)
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFF6366F1) // Electric Cyber Indigo (cool contrast vs emerald & gold)
            }
            val colorMerged = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> Color(0xFFD946EF) // Radiant Fuchsia Magenta
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFFFF2A6D) // Crimson Alert Coral Red
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFF84CC16) // Toxic Acid Lime Green
                MapThemeSlot.SLOT_4_VOID -> Color(0xFF06F9D2) // Quantum Electric Aquamarine
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFFFBBF24) // Blazing Solar Gold
            }

            // Draw Base Terrain Grid
            drawRect(
                color = groundColor,
                topLeft = Offset(shakeX, shakeY),
                size = Size(boardWidthPx, boardHeightPx)
            )

            // Subtle tactical grid lines
            for (x in 0..gridWidth) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(x * cellW + shakeX, 0f + shakeY),
                    end = Offset(x * cellW + shakeX, boardHeightPx + shakeY),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (y in 0..gridHeight) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f + shakeX, y * cellH + shakeY),
                    end = Offset(boardWidthPx + shakeX, y * cellH + shakeY),
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
                pathColor = pathBaseColor,
                colorAlpha = colorAlpha,
                colorBeta = colorBeta,
                colorMerged = colorMerged, // Radiant violet gradient shift when merged!
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
                    // Smooth, seamless energy junction - highlights with theme-specific intersection color (TASK 5)
                    val pulse = (sin(animFlowTime * 0.005f) * 0.15f + 0.85f).toFloat()
                    drawCircle(
                        color = colorMerged.copy(alpha = 0.28f * pulse),
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

        // Draw Selected Tower Range Circle (Exact 1:1 scale with combat range R_render = R_combat)
        state.selectedTower?.let { tower ->
            val tile = level.tiles.firstOrNull { it.pos == tower.gridPos }
            val tileBuff = tile?.buff ?: TileBuff.NONE
            val effectiveRange = tower.getEffectiveCombatRange(
                tileBuff = tileBuff,
                globalRangeBonus = state.globalRangeBonus,
                balanceRange = state.balanceConfig.getTowerBalance(tower.type).range
            )
            val pixelRadius = effectiveRange * cellW

            val tcx = (tower.gridPos.x + 0.5f) * cellW + shakeX
            val tcy = (tower.gridPos.y + 0.5f) * cellH + shakeY

            val rangeColor = Color(tower.archetype?.color ?: tower.type.color)
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
        // Static locked map coordinate system: tower orientation stays aligned to combat aim
        for (tower in state.towers) {
            val tcx = (tower.gridPos.x + 0.5f) * cellW + shakeX
            val tcy = (tower.gridPos.y + 0.5f) * cellH + shakeY
            val effectiveClass = tower.archetype?.towerClass ?: tower.towerClass
            val arch = tower.archetype
            val towerColor = Color(arch?.color ?: effectiveClass.color)
            val aimAngleDeg = tower.rotationAngleDeg
            val aimRad = aimAngleDeg * (PI / 180f).toFloat()
            val borderColor = if (tower.isFinalForm) Color(0xFFFFB800) else towerColor.copy(alpha = 0.88f)

            // === TIER-1 ARCHETYPE IDENTIFIER RING (Offensive = Red, Defense = Green, Support = Yellow) ===
            val tier1Vector = tower.archetype?.vector ?: tower.vector ?: when (tower.branchTier1) {
                "A" -> TowerVector.OFFENSIVE
                "B" -> TowerVector.DEFENSIVE
                "C" -> TowerVector.SUPPORT
                else -> null
            }
            if (tier1Vector != null || tower.tier >= 1) {
                val ringColor = when (tier1Vector) {
                    TowerVector.OFFENSIVE -> Color(0xFFFF2A55) // Red
                    TowerVector.DEFENSIVE -> Color(0xFF00E676) // Green
                    TowerVector.SUPPORT -> Color(0xFFFFD700)   // Yellow
                    null -> Color(0xFFFF2A55)
                }
                drawCircle(
                    color = ringColor.copy(alpha = 0.30f),
                    radius = cellW * 0.44f,
                    center = Offset(tcx, tcy),
                    style = Stroke(width = 3.5f)
                )
                drawCircle(
                    color = ringColor,
                    radius = cellW * 0.41f,
                    center = Offset(tcx, tcy),
                    style = Stroke(width = 2.0f)
                )
            }
            // === COMPLETE TOWER VISUAL CUSTOMIZATION & LEVEL 1-4 UPGRADE PIPELINE ===
            val skinSlot = TurretSkinSlot.fromSlot(state.turretSkinSlot)
            val upgradeLevel = when {
                tower.isFinalForm || tower.tier >= 4 -> 4
                tower.tier >= 2 || tower.archetype != null -> 3
                tower.tier >= 1 || tower.vector != null || tower.branchTier1 != null -> 2
                else -> 1
            }
            val vectorAccentColor = when (tier1Vector) {
                TowerVector.OFFENSIVE -> Color(0xFFFF2A55)
                TowerVector.DEFENSIVE -> Color(0xFF00E676)
                TowerVector.SUPPORT -> Color(0xFFFFD700)
                null -> Color(0xFF00FF9D)
            }
            val archetypeAccentColor = towerColor
            val apexAuraColor = Color(0xFFFFB800)
            val auraPulse = (sin(animFlowTime * 0.008f) * 0.15f + 0.85f).toFloat()

            // --- Level 4 Apex Radiant Halo / Corona (All Skins) ---
            if (upgradeLevel == 4) {
                when (effectiveClass) {
                    TowerClass.KINETIC_BALLISTIC -> {
                        drawCircle(color = apexAuraColor.copy(alpha = 0.22f * auraPulse), radius = cellW * 0.50f, center = Offset(tcx, tcy))
                        drawCircle(color = apexAuraColor, radius = cellW * 0.45f, center = Offset(tcx, tcy), style = Stroke(width = 2.0f))
                    }
                    TowerClass.ELEMENTAL_VOID -> {
                        val hR = cellW * 0.54f * auraPulse
                        val hTri = Path().apply {
                            moveTo(tcx, tcy - hR)
                            lineTo(tcx + hR * 0.866f, tcy + hR * 0.5f)
                            lineTo(tcx - hR * 0.866f, tcy + hR * 0.5f)
                            close()
                        }
                        drawPath(path = hTri, color = apexAuraColor.copy(alpha = 0.20f * auraPulse))
                        drawPath(path = hTri, color = apexAuraColor, style = Stroke(width = 2.0f, join = StrokeJoin.Round))
                    }
                    TowerClass.HEAVY_SIEGE -> {
                        val hS = cellW * 0.90f * auraPulse
                        val hTopLeft = Offset(tcx - hS / 2f, tcy - hS / 2f)
                        drawRoundRect(color = apexAuraColor.copy(alpha = 0.20f * auraPulse), topLeft = hTopLeft, size = Size(hS, hS), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                        drawRoundRect(color = apexAuraColor, topLeft = hTopLeft, size = Size(hS, hS), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = Stroke(width = 2.0f))
                    }
                }
            }

            // --- Skin-Driven Tower Chassis, Barrels & Level 1-4 Upgrade Cues ---
            when (skinSlot) {
                // =========================================================================================
                // SKIN 1: Vector Minimal / Neon Cyber
                // =========================================================================================
                TurretSkinSlot.SLOT_1_DEFAULT -> {
                    when (effectiveClass) {
                        TowerClass.KINETIC_BALLISTIC -> {
                            // Dark neon disc
                            drawCircle(color = Color(0xFF0F172A), radius = cellW * 0.38f, center = Offset(tcx, tcy))
                            drawCircle(color = borderColor, radius = cellW * 0.38f, center = Offset(tcx, tcy), style = Stroke(width = 2.5f))

                            // Level 2+ Vector Stabilizer Wings
                            if (upgradeLevel >= 2) {
                                val perp = aimRad + (PI / 2f).toFloat()
                                val wingLen = cellW * 0.22f
                                val wOff = cellW * 0.28f
                                drawLine(color = vectorAccentColor, start = Offset(tcx + cos(perp) * wOff, tcy + sin(perp) * wOff), end = Offset(tcx + cos(perp) * wOff - cos(aimRad) * wingLen, tcy + sin(perp) * wOff - sin(aimRad) * wingLen), strokeWidth = cellW * 0.05f, cap = StrokeCap.Round)
                                drawLine(color = vectorAccentColor, start = Offset(tcx - cos(perp) * wOff, tcy - sin(perp) * wOff), end = Offset(tcx - cos(perp) * wOff - cos(aimRad) * wingLen, tcy - sin(perp) * wOff - sin(aimRad) * wingLen), strokeWidth = cellW * 0.05f, cap = StrokeCap.Round)
                            }

                            // Level 1-4 Ballistic Barrels
                            val barrelLen = cellW * 0.36f
                            val b1X = tcx + cos(aimRad) * barrelLen
                            val b1Y = tcy + sin(aimRad) * barrelLen
                            drawLine(color = towerColor, start = Offset(tcx, tcy), end = Offset(b1X, b1Y), strokeWidth = cellW * 0.12f, cap = StrokeCap.Round)

                            // Level 3+ Heavy Twin Barrels
                            if (upgradeLevel >= 3) {
                                val perp = aimRad + (PI / 2f).toFloat()
                                val off = cellW * 0.09f
                                drawLine(color = archetypeAccentColor, start = Offset(tcx + cos(perp) * off, tcy + sin(perp) * off), end = Offset(b1X + cos(perp) * off, b1Y + sin(perp) * off), strokeWidth = cellW * 0.06f, cap = StrokeCap.Round)
                                drawLine(color = archetypeAccentColor, start = Offset(tcx - cos(perp) * off, tcy - sin(perp) * off), end = Offset(b1X - cos(perp) * off, b1Y - sin(perp) * off), strokeWidth = cellW * 0.06f, cap = StrokeCap.Round)
                            }

                            // Reactor Core
                            val coreColor = if (upgradeLevel == 4) apexAuraColor else Color.White
                            drawCircle(color = coreColor, radius = cellW * 0.14f, center = Offset(tcx, tcy))
                            drawCircle(color = towerColor, radius = cellW * 0.08f, center = Offset(tcx, tcy))
                        }

                        TowerClass.ELEMENTAL_VOID -> {
                            val triRadius = cellW * 0.44f
                            val p1 = Offset(tcx, tcy - triRadius)
                            val p2 = Offset(tcx + triRadius * 0.866f, tcy + triRadius * 0.5f)
                            val p3 = Offset(tcx - triRadius * 0.866f, tcy + triRadius * 0.5f)
                            val triPath = Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close() }

                            drawPath(path = triPath, color = Color(0xFF140D26))
                            drawPath(path = triPath, color = borderColor, style = Stroke(width = 2.5f, join = StrokeJoin.Round))

                            // Level 2+ Inner Vector Rune
                            if (upgradeLevel >= 2) {
                                val inR = cellW * 0.24f
                                val inTri = Path().apply {
                                    moveTo(tcx, tcy + inR)
                                    lineTo(tcx - inR * 0.866f, tcy - inR * 0.5f)
                                    lineTo(tcx + inR * 0.866f, tcy - inR * 0.5f)
                                    close()
                                }
                                drawPath(path = inTri, color = vectorAccentColor.copy(alpha = 0.35f))
                                drawPath(path = inTri, color = vectorAccentColor, style = Stroke(width = 1.5f, join = StrokeJoin.Round))
                            }

                            // Focusing Emitter Beam
                            val beamLen = cellW * (if (upgradeLevel >= 3) 0.38f else 0.32f)
                            val beamX = tcx + cos(aimRad) * beamLen
                            val beamY = tcy + sin(aimRad) * beamLen
                            drawLine(color = towerColor, start = Offset(tcx, tcy), end = Offset(beamX, beamY), strokeWidth = cellW * 0.09f, cap = StrokeCap.Round)
                            drawLine(color = if (upgradeLevel == 4) apexAuraColor else Color.White, start = Offset(tcx, tcy), end = Offset(beamX, beamY), strokeWidth = cellW * 0.04f, cap = StrokeCap.Round)

                            // Orbiting Motes (count scales with level)
                            val moteCount = if (upgradeLevel >= 3) 4 else 3
                            val orbAngle = animFlowTime * 0.005f
                            for (i in 0 until moteCount) {
                                val a = orbAngle + i * (2f * PI.toFloat() / moteCount)
                                val ox = tcx + cos(a) * (cellW * 0.24f)
                                val oy = tcy + sin(a) * (cellW * 0.24f)
                                val mColor = if (upgradeLevel == 4 && i % 2 == 0) apexAuraColor else towerColor
                                drawCircle(color = mColor, radius = cellW * 0.045f, center = Offset(ox, oy))
                            }
                            drawCircle(color = Color.White, radius = cellW * 0.10f, center = Offset(tcx, tcy))
                            drawCircle(color = towerColor, radius = cellW * 0.06f, center = Offset(tcx, tcy))
                        }

                        TowerClass.HEAVY_SIEGE -> {
                            val sqRadius = cellW * 0.38f
                            val sqTopLeft = Offset(tcx - sqRadius, tcy - sqRadius)
                            val sqBounds = Size(sqRadius * 2f, sqRadius * 2f)

                            drawRoundRect(color = Color(0xFF0F1420), topLeft = sqTopLeft, size = sqBounds, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                            drawRoundRect(color = borderColor, topLeft = sqTopLeft, size = sqBounds, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = Stroke(width = 2.0f))

                            // Dual accelerator rails
                            val railLen = cellW * (if (upgradeLevel >= 3) 0.42f else 0.36f)
                            val perp = aimRad + (PI / 2f).toFloat()
                            val spread = cellW * 0.08f
                            val r1Start = Offset(tcx + cos(perp) * spread, tcy + sin(perp) * spread)
                            val r1End = Offset(r1Start.x + cos(aimRad) * railLen, r1Start.y + sin(aimRad) * railLen)
                            val r2Start = Offset(tcx - cos(perp) * spread, tcy - sin(perp) * spread)
                            val r2End = Offset(r2Start.x + cos(aimRad) * railLen, r2Start.y + sin(aimRad) * railLen)

                            val railColor = if (upgradeLevel >= 3) archetypeAccentColor else borderColor
                            drawLine(color = railColor, start = r1Start, end = r1End, strokeWidth = cellW * 0.06f, cap = StrokeCap.Round)
                            drawLine(color = railColor, start = r2Start, end = r2End, strokeWidth = cellW * 0.06f, cap = StrokeCap.Round)

                            // Level 2+ Vector Reinforcement Brackets
                            if (upgradeLevel >= 2) {
                                drawLine(color = vectorAccentColor, start = Offset(r1Start.x - cos(aimRad) * 4f, r1Start.y - sin(aimRad) * 4f), end = Offset(r2Start.x - cos(aimRad) * 4f, r2Start.y - sin(aimRad) * 4f), strokeWidth = cellW * 0.04f)
                            }

                            // Central Beam Conduit
                            val coreEnd = Offset(tcx + cos(aimRad) * (railLen * 0.95f), tcy + sin(aimRad) * (railLen * 0.95f))
                            drawLine(color = if (upgradeLevel == 4) apexAuraColor else Color.White, start = Offset(tcx, tcy), end = coreEnd, strokeWidth = cellW * 0.035f, cap = StrokeCap.Round)

                            // Reactor Hub
                            drawCircle(color = Color(0xFF0F172A), radius = cellW * 0.13f, center = Offset(tcx, tcy))
                            drawCircle(color = borderColor, radius = cellW * 0.13f, center = Offset(tcx, tcy), style = Stroke(width = 1.8f))
                            drawCircle(color = Color.White, radius = cellW * 0.06f, center = Offset(tcx, tcy))
                        }
                    }
                }

                // =========================================================================================
                // SKIN 2: Blueprint Schematic / Technical CAD
                // =========================================================================================
                TurretSkinSlot.SLOT_2_BLUEPRINT -> {
                    val cadColor = Color(0xFF60A5FA)
                    val cadWhite = Color.White.copy(alpha = 0.85f)

                    // Architectural Grid Crosshairs & Guideline Circle
                    val ch = cellW * 0.38f
                    drawLine(color = cadColor.copy(alpha = 0.40f), start = Offset(tcx - ch, tcy), end = Offset(tcx + ch, tcy), strokeWidth = 1.0f)
                    drawLine(color = cadColor.copy(alpha = 0.40f), start = Offset(tcx, tcy - ch), end = Offset(tcx, tcy + ch), strokeWidth = 1.0f)
                    drawCircle(color = cadColor.copy(alpha = 0.30f), radius = cellW * 0.35f, center = Offset(tcx, tcy), style = Stroke(width = 1.0f))

                    when (effectiveClass) {
                        TowerClass.KINETIC_BALLISTIC -> {
                            drawCircle(color = Color(0xFF0A192F), radius = cellW * 0.36f, center = Offset(tcx, tcy))
                            drawCircle(color = cadColor, radius = cellW * 0.36f, center = Offset(tcx, tcy), style = Stroke(width = 1.8f))

                            // Technical drafting ticks
                            for (deg in listOf(0, 90, 180, 270)) {
                                val r = deg * (PI / 180f).toFloat()
                                drawLine(color = cadWhite, start = Offset(tcx + cos(r) * cellW * 0.33f, tcy + sin(r) * cellW * 0.33f), end = Offset(tcx + cos(r) * cellW * 0.38f, tcy + sin(r) * cellW * 0.38f), strokeWidth = 1.5f)
                            }

                            // Level 2+ Vector Telemetry Chevrons
                            if (upgradeLevel >= 2) {
                                val p = aimRad + (PI / 2f).toFloat()
                                val wOff = cellW * 0.26f
                                drawLine(color = vectorAccentColor, start = Offset(tcx + cos(p) * wOff, tcy + sin(p) * wOff), end = Offset(tcx + cos(aimRad) * cellW * 0.28f, tcy + sin(aimRad) * cellW * 0.28f), strokeWidth = 1.5f)
                                drawLine(color = vectorAccentColor, start = Offset(tcx - cos(p) * wOff, tcy - sin(p) * wOff), end = Offset(tcx + cos(aimRad) * cellW * 0.28f, tcy + sin(aimRad) * cellW * 0.28f), strokeWidth = 1.5f)
                            }

                            // Schematic Barrel Assembly
                            val bLen = cellW * 0.36f
                            val endX = tcx + cos(aimRad) * bLen
                            val endY = tcy + sin(aimRad) * bLen
                            drawLine(color = cadWhite, start = Offset(tcx, tcy), end = Offset(endX, endY), strokeWidth = cellW * 0.08f, cap = StrokeCap.Square)

                            // Level 3+ Archetype Dimension Brackets
                            if (upgradeLevel >= 3) {
                                val p = aimRad + (PI / 2f).toFloat()
                                val off = cellW * 0.08f
                                drawLine(color = archetypeAccentColor, start = Offset(tcx + cos(p) * off, tcy + sin(p) * off), end = Offset(endX + cos(p) * off, endY + sin(p) * off), strokeWidth = 2.0f)
                                drawLine(color = archetypeAccentColor, start = Offset(tcx - cos(p) * off, tcy - sin(p) * off), end = Offset(endX - cos(p) * off, endY - sin(p) * off), strokeWidth = 2.0f)
                            }

                            drawCircle(color = Color(0xFF0F172A), radius = cellW * 0.12f, center = Offset(tcx, tcy))
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else cadColor, radius = cellW * 0.07f, center = Offset(tcx, tcy))
                        }

                        TowerClass.ELEMENTAL_VOID -> {
                            val r = cellW * 0.42f
                            val p1 = Offset(tcx, tcy - r)
                            val p2 = Offset(tcx + r * 0.866f, tcy + r * 0.5f)
                            val p3 = Offset(tcx - r * 0.866f, tcy + r * 0.5f)
                            val p = Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close() }
                            drawPath(path = p, color = Color(0xFF0A192F))
                            drawPath(path = p, color = cadColor, style = Stroke(width = 1.8f))

                            // Vertex CAD Crosses
                            listOf(p1, p2, p3).forEach { pt ->
                                drawLine(color = cadWhite, start = Offset(pt.x - 3f, pt.y), end = Offset(pt.x + 3f, pt.y), strokeWidth = 1.2f)
                                drawLine(color = cadWhite, start = Offset(pt.x, pt.y - 3f), end = Offset(pt.x, pt.y + 3f), strokeWidth = 1.2f)
                            }

                            // Level 2+ Vector Dimension Arc
                            if (upgradeLevel >= 2) {
                                drawCircle(color = vectorAccentColor.copy(alpha = 0.5f), radius = cellW * 0.22f, center = Offset(tcx, tcy), style = Stroke(width = 1.5f))
                            }

                            // Level 3+ Archetype Focusing Ray
                            val bLen = cellW * 0.35f
                            val bEnd = Offset(tcx + cos(aimRad) * bLen, tcy + sin(aimRad) * bLen)
                            drawLine(color = cadColor, start = Offset(tcx, tcy), end = bEnd, strokeWidth = cellW * 0.07f)
                            if (upgradeLevel >= 3) {
                                drawLine(color = archetypeAccentColor, start = Offset(tcx, tcy), end = bEnd, strokeWidth = cellW * 0.03f)
                            }
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else cadWhite, radius = cellW * 0.08f, center = Offset(tcx, tcy))
                        }

                        TowerClass.HEAVY_SIEGE -> {
                            val s = cellW * 0.72f
                            val tl = Offset(tcx - s / 2f, tcy - s / 2f)
                            drawRect(color = Color(0xFF0A192F), topLeft = tl, size = Size(s, s))
                            drawRect(color = cadColor, topLeft = tl, size = Size(s, s), style = Stroke(width = 1.8f))

                            // CAD Drafting Diagonal Alignment Marks
                            drawLine(color = cadColor.copy(alpha = 0.35f), start = tl, end = Offset(tl.x + s, tl.y + s), strokeWidth = 1.0f)
                            drawLine(color = cadColor.copy(alpha = 0.35f), start = Offset(tl.x + s, tl.y), end = Offset(tl.x, tl.y + s), strokeWidth = 1.0f)

                            // Rail guides
                            val rLen = cellW * 0.38f
                            val perp = aimRad + (PI / 2f).toFloat()
                            val sp = cellW * 0.07f
                            val r1 = Offset(tcx + cos(perp) * sp, tcy + sin(perp) * sp)
                            val r2 = Offset(tcx - cos(perp) * sp, tcy - sin(perp) * sp)
                            drawLine(color = cadWhite, start = r1, end = Offset(r1.x + cos(aimRad) * rLen, r1.y + sin(aimRad) * rLen), strokeWidth = 2.0f)
                            drawLine(color = cadWhite, start = r2, end = Offset(r2.x + cos(aimRad) * rLen, r2.y + sin(aimRad) * rLen), strokeWidth = 2.0f)

                            if (upgradeLevel >= 2) {
                                drawLine(color = vectorAccentColor, start = r1, end = r2, strokeWidth = 2.0f)
                            }
                            if (upgradeLevel >= 3) {
                                drawCircle(color = archetypeAccentColor, radius = cellW * 0.12f, center = Offset(tcx, tcy), style = Stroke(width = 1.5f))
                            }
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else cadColor, radius = cellW * 0.06f, center = Offset(tcx, tcy))
                        }
                    }
                }

                // =========================================================================================
                // SKIN 3: Tactical Military / Heavy Plated Armor
                // =========================================================================================
                TurretSkinSlot.SLOT_3_MILITARY -> {
                    val steelDark = Color(0xFF1E293B)
                    val steelPlate = Color(0xFF334155)
                    val rivetColor = Color(0xFF94A3B8)

                    when (effectiveClass) {
                        TowerClass.KINETIC_BALLISTIC -> {
                            // Heavy armored pillbox bunker
                            drawCircle(color = steelDark, radius = cellW * 0.38f, center = Offset(tcx, tcy))
                            drawCircle(color = steelPlate, radius = cellW * 0.38f, center = Offset(tcx, tcy), style = Stroke(width = 3.5f))

                            // 4 Corner Bolted Rivets
                            val rOff = cellW * 0.26f
                            listOf(Offset(tcx - rOff, tcy - rOff), Offset(tcx + rOff, tcy - rOff), Offset(tcx - rOff, tcy + rOff), Offset(tcx + rOff, tcy + rOff)).forEach {
                                drawCircle(color = rivetColor, radius = 2.5f, center = it)
                            }

                            // Level 2+ Reactive Armor Chevrons
                            if (upgradeLevel >= 2) {
                                val p = aimRad + (PI / 2f).toFloat()
                                val wOff = cellW * 0.28f
                                drawRoundRect(color = vectorAccentColor, topLeft = Offset(tcx + cos(p) * wOff - 4f, tcy + sin(p) * wOff - 4f), size = Size(8f, 8f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
                                drawRoundRect(color = vectorAccentColor, topLeft = Offset(tcx - cos(p) * wOff - 4f, tcy - sin(p) * wOff - 4f), size = Size(8f, 8f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
                            }

                            // Heavy Autocannon Muzzle
                            val bLen = cellW * 0.38f
                            val endX = tcx + cos(aimRad) * bLen
                            val endY = tcy + sin(aimRad) * bLen
                            drawLine(color = steelPlate, start = Offset(tcx, tcy), end = Offset(endX, endY), strokeWidth = cellW * 0.16f, cap = StrokeCap.Square)
                            drawLine(color = if (upgradeLevel >= 3) archetypeAccentColor else Color.Black, start = Offset(tcx, tcy), end = Offset(endX, endY), strokeWidth = cellW * 0.08f, cap = StrokeCap.Square)

                            // Turret Cupola Mantle
                            drawCircle(color = steelDark, radius = cellW * 0.16f, center = Offset(tcx, tcy))
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else rivetColor, radius = cellW * 0.08f, center = Offset(tcx, tcy))
                        }

                        TowerClass.ELEMENTAL_VOID -> {
                            val r = cellW * 0.44f
                            val p1 = Offset(tcx, tcy - r)
                            val p2 = Offset(tcx + r * 0.866f, tcy + r * 0.5f)
                            val p3 = Offset(tcx - r * 0.866f, tcy + r * 0.5f)
                            val p = Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close() }

                            drawPath(path = p, color = steelDark)
                            drawPath(path = p, color = steelPlate, style = Stroke(width = 3.5f, join = StrokeJoin.Round))

                            // Reinforced containment brackets
                            listOf(p1, p2, p3).forEach { pt ->
                                drawCircle(color = rivetColor, radius = 3.0f, center = pt)
                            }

                            if (upgradeLevel >= 2) {
                                drawCircle(color = vectorAccentColor, radius = cellW * 0.18f, center = Offset(tcx, tcy), style = Stroke(width = 2.0f))
                            }

                            val bLen = cellW * 0.34f
                            val bEnd = Offset(tcx + cos(aimRad) * bLen, tcy + sin(aimRad) * bLen)
                            drawLine(color = steelPlate, start = Offset(tcx, tcy), end = bEnd, strokeWidth = cellW * 0.10f, cap = StrokeCap.Square)
                            if (upgradeLevel >= 3) {
                                drawLine(color = archetypeAccentColor, start = Offset(tcx, tcy), end = bEnd, strokeWidth = cellW * 0.05f)
                            }
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else Color.White, radius = cellW * 0.09f, center = Offset(tcx, tcy))
                        }

                        TowerClass.HEAVY_SIEGE -> {
                            val s = cellW * 0.76f
                            val tl = Offset(tcx - s / 2f, tcy - s / 2f)
                            drawRoundRect(color = steelDark, topLeft = tl, size = Size(s, s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                            drawRoundRect(color = steelPlate, topLeft = tl, size = Size(s, s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(width = 3.5f))

                            // Corner heavy armor plates
                            val cSize = cellW * 0.14f
                            listOf(tl, Offset(tl.x + s - cSize, tl.y), Offset(tl.x, tl.y + s - cSize), Offset(tl.x + s - cSize, tl.y + s - cSize)).forEach {
                                drawRect(color = steelPlate, topLeft = it, size = Size(cSize, cSize))
                                drawCircle(color = rivetColor, radius = 2.0f, center = Offset(it.x + cSize / 2f, it.y + cSize / 2f))
                            }

                            // Heavy Flak Dual Cannons
                            val bLen = cellW * 0.40f
                            val perp = aimRad + (PI / 2f).toFloat()
                            val sp = cellW * 0.09f
                            val c1 = Offset(tcx + cos(perp) * sp, tcy + sin(perp) * sp)
                            val c2 = Offset(tcx - cos(perp) * sp, tcy - sin(perp) * sp)
                            drawLine(color = steelPlate, start = c1, end = Offset(c1.x + cos(aimRad) * bLen, c1.y + sin(aimRad) * bLen), strokeWidth = cellW * 0.08f, cap = StrokeCap.Square)
                            drawLine(color = steelPlate, start = c2, end = Offset(c2.x + cos(aimRad) * bLen, c2.y + sin(aimRad) * bLen), strokeWidth = cellW * 0.08f, cap = StrokeCap.Square)

                            if (upgradeLevel >= 2) {
                                drawLine(color = vectorAccentColor, start = c1, end = c2, strokeWidth = 3.0f)
                            }
                            if (upgradeLevel >= 3) {
                                drawCircle(color = archetypeAccentColor, radius = cellW * 0.14f, center = Offset(tcx, tcy), style = Stroke(width = 2.0f))
                            }
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else rivetColor, radius = cellW * 0.08f, center = Offset(tcx, tcy))
                        }
                    }
                }

                // =========================================================================================
                // SKIN 4: Void Rift / Astral Occult Relic
                // =========================================================================================
                TurretSkinSlot.SLOT_4_VOID -> {
                    val voidDark = Color(0xFF0D061A)
                    val voidRift = Color(0xFFA855F7)
                    val voidGlow = Color(0xFFD946EF)

                    // Occult Swirling Astral Corona
                    drawCircle(color = voidRift.copy(alpha = 0.25f * auraPulse), radius = cellW * 0.42f, center = Offset(tcx, tcy), style = Stroke(width = 2.0f))

                    when (effectiveClass) {
                        TowerClass.KINETIC_BALLISTIC -> {
                            drawCircle(color = voidDark, radius = cellW * 0.38f, center = Offset(tcx, tcy))
                            drawCircle(color = voidRift, radius = cellW * 0.38f, center = Offset(tcx, tcy), style = Stroke(width = 2.0f))

                            // Level 2+ Orbiting Void Slivers
                            if (upgradeLevel >= 2) {
                                val sAngle = animFlowTime * 0.007f
                                for (i in 0 until 2) {
                                    val a = sAngle + i * PI.toFloat()
                                    val sx = tcx + cos(a) * cellW * 0.30f
                                    val sy = tcy + sin(a) * cellW * 0.30f
                                    drawCircle(color = vectorAccentColor, radius = 3.5f, center = Offset(sx, sy))
                                }
                            }

                            // Void Singularity Beam Emitter
                            val bLen = cellW * 0.36f
                            val endX = tcx + cos(aimRad) * bLen
                            val endY = tcy + sin(aimRad) * bLen
                            drawLine(color = voidGlow, start = Offset(tcx, tcy), end = Offset(endX, endY), strokeWidth = cellW * 0.10f, cap = StrokeCap.Round)
                            if (upgradeLevel >= 3) {
                                drawLine(color = archetypeAccentColor, start = Offset(tcx, tcy), end = Offset(endX, endY), strokeWidth = cellW * 0.05f, cap = StrokeCap.Round)
                            }
                            drawCircle(color = Color.White, radius = cellW * 0.12f, center = Offset(tcx, tcy))
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else voidRift, radius = cellW * 0.07f, center = Offset(tcx, tcy))
                        }

                        TowerClass.ELEMENTAL_VOID -> {
                            val r = cellW * 0.44f
                            val p1 = Offset(tcx, tcy - r)
                            val p2 = Offset(tcx + r * 0.866f, tcy + r * 0.5f)
                            val p3 = Offset(tcx - r * 0.866f, tcy + r * 0.5f)
                            val p = Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close() }

                            drawPath(path = p, color = voidDark)
                            drawPath(path = p, color = voidRift, style = Stroke(width = 2.5f, join = StrokeJoin.Round))

                            // Occult Shard Crystals
                            listOf(p1, p2, p3).forEach { pt ->
                                drawCircle(color = voidGlow, radius = cellW * 0.06f, center = pt)
                            }

                            if (upgradeLevel >= 2) {
                                drawCircle(color = vectorAccentColor, radius = cellW * 0.20f, center = Offset(tcx, tcy), style = Stroke(width = 1.8f))
                            }

                            val bLen = cellW * 0.36f
                            val bEnd = Offset(tcx + cos(aimRad) * bLen, tcy + sin(aimRad) * bLen)
                            drawLine(color = voidGlow, start = Offset(tcx, tcy), end = bEnd, strokeWidth = cellW * 0.09f, cap = StrokeCap.Round)
                            if (upgradeLevel >= 3) {
                                drawLine(color = archetypeAccentColor, start = Offset(tcx, tcy), end = bEnd, strokeWidth = cellW * 0.04f, cap = StrokeCap.Round)
                            }
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else Color.White, radius = cellW * 0.09f, center = Offset(tcx, tcy))
                        }

                        TowerClass.HEAVY_SIEGE -> {
                            val s = cellW * 0.74f
                            val tl = Offset(tcx - s / 2f, tcy - s / 2f)
                            drawRoundRect(color = voidDark, topLeft = tl, size = Size(s, s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                            drawRoundRect(color = voidRift, topLeft = tl, size = Size(s, s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = Stroke(width = 2.0f))

                            // Dimensional Monolith Aperture
                            val innerDiamond = Path().apply {
                                moveTo(tcx, tcy - s * 0.35f)
                                lineTo(tcx + s * 0.35f, tcy)
                                lineTo(tcx, tcy + s * 0.35f)
                                lineTo(tcx - s * 0.35f, tcy)
                                close()
                            }
                            drawPath(path = innerDiamond, color = voidGlow.copy(alpha = 0.25f))

                            val bLen = cellW * 0.38f
                            val perp = aimRad + (PI / 2f).toFloat()
                            val sp = cellW * 0.08f
                            val r1 = Offset(tcx + cos(perp) * sp, tcy + sin(perp) * sp)
                            val r2 = Offset(tcx - cos(perp) * sp, tcy - sin(perp) * sp)
                            drawLine(color = voidRift, start = r1, end = Offset(r1.x + cos(aimRad) * bLen, r1.y + sin(aimRad) * bLen), strokeWidth = cellW * 0.06f, cap = StrokeCap.Round)
                            drawLine(color = voidRift, start = r2, end = Offset(r2.x + cos(aimRad) * bLen, r2.y + sin(aimRad) * bLen), strokeWidth = cellW * 0.06f, cap = StrokeCap.Round)

                            if (upgradeLevel >= 2) {
                                drawLine(color = vectorAccentColor, start = r1, end = r2, strokeWidth = 2.5f)
                            }
                            if (upgradeLevel >= 3) {
                                drawCircle(color = archetypeAccentColor, radius = cellW * 0.12f, center = Offset(tcx, tcy), style = Stroke(width = 1.8f))
                            }
                            drawCircle(color = if (upgradeLevel == 4) apexAuraColor else Color.White, radius = cellW * 0.07f, center = Offset(tcx, tcy))
                        }
                    }
                }

                // =========================================================================================
                // SKIN 5: Retro Pixel Arcade / 8-Bit Pixel Synth
                // =========================================================================================
                TurretSkinSlot.SLOT_5_PIXEL -> {
                    val pixelDark = Color(0xFF0F172A)
                    val pixelYellow = Color(0xFFFACC15)
                    val pUnit = cellW * 0.06f

                    // 4 Stepped Pixel Corners
                    val bOff = cellW * 0.32f
                    listOf(Offset(tcx - bOff, tcy - bOff), Offset(tcx + bOff - pUnit, tcy - bOff), Offset(tcx - bOff, tcy + bOff - pUnit), Offset(tcx + bOff - pUnit, tcy + bOff - pUnit)).forEach {
                        drawRect(color = pixelYellow, topLeft = it, size = Size(pUnit, pUnit))
                    }

                    when (effectiveClass) {
                        TowerClass.KINETIC_BALLISTIC -> {
                            drawCircle(color = pixelDark, radius = cellW * 0.36f, center = Offset(tcx, tcy))
                            drawCircle(color = pixelYellow, radius = cellW * 0.36f, center = Offset(tcx, tcy), style = Stroke(width = 2.0f))

                            // Level 2+ Stepped Pixel Wing Pods
                            if (upgradeLevel >= 2) {
                                val p = aimRad + (PI / 2f).toFloat()
                                val wOff = cellW * 0.28f
                                drawRect(color = vectorAccentColor, topLeft = Offset(tcx + cos(p) * wOff - pUnit / 2f, tcy + sin(p) * wOff - pUnit / 2f), size = Size(pUnit, pUnit))
                                drawRect(color = vectorAccentColor, topLeft = Offset(tcx - cos(p) * wOff - pUnit / 2f, tcy - sin(p) * wOff - pUnit / 2f), size = Size(pUnit, pUnit))
                            }

                            // 8-Bit Square Pixel Barrel
                            val bLen = cellW * 0.34f
                            val endX = tcx + cos(aimRad) * bLen
                            val endY = tcy + sin(aimRad) * bLen
                            drawLine(color = pixelYellow, start = Offset(tcx, tcy), end = Offset(endX, endY), strokeWidth = cellW * 0.12f, cap = StrokeCap.Square)
                            if (upgradeLevel >= 3) {
                                drawLine(color = archetypeAccentColor, start = Offset(tcx, tcy), end = Offset(endX, endY), strokeWidth = cellW * 0.06f, cap = StrokeCap.Square)
                            }
                            drawRect(color = if (upgradeLevel == 4) apexAuraColor else Color.White, topLeft = Offset(tcx - pUnit, tcy - pUnit), size = Size(pUnit * 2f, pUnit * 2f))
                        }

                        TowerClass.ELEMENTAL_VOID -> {
                            val r = cellW * 0.42f
                            val p1 = Offset(tcx, tcy - r)
                            val p2 = Offset(tcx + r * 0.866f, tcy + r * 0.5f)
                            val p3 = Offset(tcx - r * 0.866f, tcy + r * 0.5f)
                            val p = Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close() }

                            drawPath(path = p, color = pixelDark)
                            drawPath(path = p, color = pixelYellow, style = Stroke(width = 2.0f, join = StrokeJoin.Miter))

                            listOf(p1, p2, p3).forEach { pt ->
                                drawRect(color = pixelYellow, topLeft = Offset(pt.x - pUnit / 2f, pt.y - pUnit / 2f), size = Size(pUnit, pUnit))
                            }

                            if (upgradeLevel >= 2) {
                                drawRect(color = vectorAccentColor, topLeft = Offset(tcx - pUnit * 1.5f, tcy - pUnit * 1.5f), size = Size(pUnit * 3f, pUnit * 3f), style = Stroke(width = 1.5f))
                            }

                            val bLen = cellW * 0.34f
                            val bEnd = Offset(tcx + cos(aimRad) * bLen, tcy + sin(aimRad) * bLen)
                            drawLine(color = pixelYellow, start = Offset(tcx, tcy), end = bEnd, strokeWidth = cellW * 0.09f, cap = StrokeCap.Square)
                            if (upgradeLevel >= 3) {
                                drawLine(color = archetypeAccentColor, start = Offset(tcx, tcy), end = bEnd, strokeWidth = cellW * 0.045f, cap = StrokeCap.Square)
                            }
                            drawRect(color = if (upgradeLevel == 4) apexAuraColor else Color.White, topLeft = Offset(tcx - pUnit * 0.8f, tcy - pUnit * 0.8f), size = Size(pUnit * 1.6f, pUnit * 1.6f))
                        }

                        TowerClass.HEAVY_SIEGE -> {
                            val s = cellW * 0.72f
                            val tl = Offset(tcx - s / 2f, tcy - s / 2f)
                            drawRect(color = pixelDark, topLeft = tl, size = Size(s, s))
                            drawRect(color = pixelYellow, topLeft = tl, size = Size(s, s), style = Stroke(width = 2.0f))

                            // Stepped Pixel Bevels
                            drawRect(color = pixelYellow, topLeft = Offset(tl.x, tl.y), size = Size(pUnit, pUnit))
                            drawRect(color = pixelYellow, topLeft = Offset(tl.x + s - pUnit, tl.y), size = Size(pUnit, pUnit))
                            drawRect(color = pixelYellow, topLeft = Offset(tl.x, tl.y + s - pUnit), size = Size(pUnit, pUnit))
                            drawRect(color = pixelYellow, topLeft = Offset(tl.x + s - pUnit, tl.y + s - pUnit), size = Size(pUnit, pUnit))

                            val bLen = cellW * 0.38f
                            val perp = aimRad + (PI / 2f).toFloat()
                            val sp = cellW * 0.08f
                            val r1 = Offset(tcx + cos(perp) * sp, tcy + sin(perp) * sp)
                            val r2 = Offset(tcx - cos(perp) * sp, tcy - sin(perp) * sp)
                            drawLine(color = pixelYellow, start = r1, end = Offset(r1.x + cos(aimRad) * bLen, r1.y + sin(aimRad) * bLen), strokeWidth = cellW * 0.08f, cap = StrokeCap.Square)
                            drawLine(color = pixelYellow, start = r2, end = Offset(r2.x + cos(aimRad) * bLen, r2.y + sin(aimRad) * bLen), strokeWidth = cellW * 0.08f, cap = StrokeCap.Square)

                            if (upgradeLevel >= 2) {
                                drawLine(color = vectorAccentColor, start = r1, end = r2, strokeWidth = 2.5f)
                            }
                            if (upgradeLevel >= 3) {
                                drawRect(color = archetypeAccentColor, topLeft = Offset(tcx - pUnit * 1.2f, tcy - pUnit * 1.2f), size = Size(pUnit * 2.4f, pUnit * 2.4f), style = Stroke(width = 1.5f))
                            }
                            drawRect(color = if (upgradeLevel == 4) apexAuraColor else Color.White, topLeft = Offset(tcx - pUnit * 0.7f, tcy - pUnit * 0.7f), size = Size(pUnit * 1.4f, pUnit * 1.4f))
                        }
                    }
                }
            }

            // === TIER FORM BADGE (Dynamic Level 1-4 Indicator) ===
            val badgeY = tcy + cellW * 0.28f
            val badgeColor = when (upgradeLevel) {
                4 -> apexAuraColor
                3 -> archetypeAccentColor
                2 -> vectorAccentColor
                else -> Color(0xFF94A3B8)
            }
            when (effectiveClass) {
                TowerClass.KINETIC_BALLISTIC -> {
                    drawCircle(color = badgeColor, radius = if (upgradeLevel >= 3) 3.5f else 2.5f, center = Offset(tcx, badgeY))
                }
                TowerClass.ELEMENTAL_VOID -> {
                    val bR = if (upgradeLevel >= 3) 4.5f else 3.5f
                    val bTri = Path().apply {
                        moveTo(tcx, badgeY - bR)
                        lineTo(tcx + bR * 0.866f, badgeY + bR * 0.5f)
                        lineTo(tcx - bR * 0.866f, badgeY + bR * 0.5f)
                        close()
                    }
                    drawPath(path = bTri, color = badgeColor)
                }
                TowerClass.HEAVY_SIEGE -> {
                    val bS = if (upgradeLevel >= 3) 6f else 5f
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

        // Draw Enemies on BOTH lanes with Distinct Geometric Shapes & Elemental Color Affinities
        for (enemy in state.enemies) {
            val ecx = (enemy.worldX + 0.5f) * cellW + shakeX
            val ecy = (enemy.worldY + 0.5f) * cellH + shakeY
            val enemyColor = Color(enemy.type.color)
            val affinityColor = Color(enemy.type.elementAffinity.colorHex)
            val enemyRadius = cellW * 0.28f * enemy.type.sizeRatio

            if (enemy.slowTimerMs > 0) {
                drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.4f), radius = enemyRadius * 1.4f, center = Offset(ecx, ecy))
            }
            if (enemy.shockTimerMs > 0) {
                drawCircle(color = Color(0xFFB026FF).copy(alpha = 0.4f), radius = enemyRadius * 1.35f, center = Offset(ecx, ecy), style = Stroke(width = 2f))
            }

            when (enemy.type.shape) {
                EnemyShape.CIRCLE -> {
                    if (enemy.isBoss) {
                        drawCircle(color = Color(0xFF581C87), radius = enemyRadius * 1.35f, center = Offset(ecx, ecy))
                        drawCircle(color = affinityColor, radius = enemyRadius * 1.15f, center = Offset(ecx, ecy), style = Stroke(width = 3.0f))
                        drawCircle(color = enemyColor, radius = enemyRadius, center = Offset(ecx, ecy))
                        drawCircle(color = Color.White, radius = enemyRadius * 0.35f, center = Offset(ecx, ecy))
                    } else {
                        drawCircle(color = enemyColor, radius = enemyRadius, center = Offset(ecx, ecy))
                        drawCircle(color = affinityColor, radius = enemyRadius, center = Offset(ecx, ecy), style = Stroke(width = 2.2f))
                        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = enemyRadius * 0.4f, center = Offset(ecx, ecy))
                        drawCircle(color = affinityColor, radius = enemyRadius * 0.2f, center = Offset(ecx, ecy))
                    }
                }
                EnemyShape.TRIANGLE -> {
                    val triRadius = enemyRadius * 1.25f
                    val heading = enemy.headingRad
                    val cosH = cos(heading)
                    val sinH = sin(heading)
                    val perpCos = cos(heading + (PI / 2f).toFloat())
                    val perpSin = sin(heading + (PI / 2f).toFloat())

                    val tipX = ecx + cosH * triRadius
                    val tipY = ecy + sinH * triRadius
                    val baseBackX = ecx - cosH * (triRadius * 0.65f)
                    val baseBackY = ecy - sinH * (triRadius * 0.65f)
                    val halfBase = triRadius * 0.85f

                    val p1 = Offset(tipX, tipY)
                    val p2 = Offset(baseBackX + perpCos * halfBase, baseBackY + perpSin * halfBase)
                    val p3 = Offset(baseBackX - perpCos * halfBase, baseBackY - perpSin * halfBase)

                    val triPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    }

                    if (enemy.isBoss) {
                        drawPath(path = triPath, color = Color(0xFF581C87))
                        drawPath(path = triPath, color = affinityColor, style = Stroke(width = 3.5f, join = StrokeJoin.Round))
                        drawCircle(color = Color.White, radius = enemyRadius * 0.35f, center = Offset(ecx, ecy))
                    } else {
                        drawPath(path = triPath, color = enemyColor)
                        drawPath(path = triPath, color = affinityColor, style = Stroke(width = 2.2f, join = StrokeJoin.Round))
                        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = enemyRadius * 0.4f, center = Offset(ecx, ecy))
                        drawCircle(color = affinityColor, radius = enemyRadius * 0.2f, center = Offset(ecx, ecy))
                    }
                }
                EnemyShape.SQUARE -> {
                    val sqRadius = enemyRadius * 0.95f
                    val sqSize = sqRadius * 2f
                    val sqTopLeft = Offset(ecx - sqRadius, ecy - sqRadius)

                    if (enemy.isBoss) {
                        drawRoundRect(color = Color(0xFF581C87), topLeft = sqTopLeft, size = Size(sqSize, sqSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                        drawRoundRect(color = affinityColor, topLeft = sqTopLeft, size = Size(sqSize, sqSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(width = 3.5f))
                        drawCircle(color = Color.White, radius = enemyRadius * 0.35f, center = Offset(ecx, ecy))
                    } else {
                        drawRoundRect(color = enemyColor, topLeft = sqTopLeft, size = Size(sqSize, sqSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                        drawRoundRect(color = affinityColor, topLeft = sqTopLeft, size = Size(sqSize, sqSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(width = 2.2f))
                        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = enemyRadius * 0.4f, center = Offset(ecx, ecy))
                        drawCircle(color = affinityColor, radius = enemyRadius * 0.2f, center = Offset(ecx, ecy))
                    }
                }
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

        // Tactical Tutorial Pointer: Animated bouncing arrow pointing to a boosted pad near the beginning/center on Wave 1
        if (!state.hasClickedFirstTile && state.waveNumber == 1 && state.towers.isEmpty()) {
            val candidates = level.tiles.filter { it.type == TileType.EMPTY }
            val targetTile = candidates.maxByOrNull { tile ->
                val hasBuffScore = if (tile.buff != TileBuff.NONE) 100f else 0f
                val spawnerAlpha = level.laneAlpha.first()
                val spawnerBeta = level.laneBeta.first()
                val dAlpha = hypot((tile.pos.x - spawnerAlpha.x).toFloat(), (tile.pos.y - spawnerAlpha.y).toFloat())
                val dBeta = hypot((tile.pos.x - spawnerBeta.x).toFloat(), (tile.pos.y - spawnerBeta.y).toFloat())
                val dCenter = hypot((tile.pos.x - gridWidth / 2f), (tile.pos.y - gridHeight / 2f))

                val earlyLaneAlpha = level.laneAlpha.take(maxOf(3, level.laneAlpha.size / 2))
                val earlyLaneBeta = level.laneBeta.take(maxOf(3, level.laneBeta.size / 2))
                val alphaCoverage = earlyLaneAlpha.count { hypot((tile.pos.x - it.x).toFloat(), (tile.pos.y - it.y).toFloat()) <= 2.8f }
                val betaCoverage = earlyLaneBeta.count { hypot((tile.pos.x - it.x).toFloat(), (tile.pos.y - it.y).toFloat()) <= 2.8f }
                val coverageScore = (alphaCoverage + betaCoverage) * 15f
                val proximityScore = 50f - minOf(dAlpha, dBeta) * 3f - dCenter * 2f

                hasBuffScore + coverageScore + proximityScore
            }
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
