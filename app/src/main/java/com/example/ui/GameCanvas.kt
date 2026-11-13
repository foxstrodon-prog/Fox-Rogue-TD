package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.MapThemeSlot
import com.example.model.MapTile
import com.example.ui.canvas.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Top-level Tactical Game Canvas orchestrating specialized sub-renderers:
 * - [drawTerrainGrid], [drawTerrainTiles], [drawSelectedTowerOverlays], [drawTacticalTutorialPointer] in `TerrainRenderer.kt`
 * - [drawSmoothMergedPathways], [drawTrackParticles] in `TrackRenderer.kt`
 * - [drawTowers], [drawCombatEntities] in `EntityRenderer.kt`
 */
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

    val scratchPath1 = remember { Path() }
    val scratchPath2 = remember { Path() }
    val lanePathA = remember { Path() }
    val lanePathB = remember { Path() }
    val mergedPath = remember { Path() }

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
            bw to (bw / boardAspect)
        } else {
            val bh = size.height
            (bh * boardAspect) to bh
        }

        val boardOriginX = (size.width - boardWidthPx) / 2f
        val boardOriginY = (size.height - boardHeightPx) / 2f

        val cellW = boardWidthPx / gridWidth
        val cellH = boardHeightPx / gridHeight

        val shakeX = if (state.screenShakeIntensity > 0f) (sin(System.currentTimeMillis() * 0.05f) * state.screenShakeIntensity * 12f).toFloat() else 0f
        val shakeY = if (state.screenShakeIntensity > 0f) (cos(System.currentTimeMillis() * 0.05f) * state.screenShakeIntensity * 12f).toFloat() else 0f

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
            val colorAlpha = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> Color(0xFF00F0FF)
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFF00E5FF)
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFFF59E0B)
                MapThemeSlot.SLOT_4_VOID -> Color(0xFFA855F7)
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFF10B981)
            }
            val colorBeta = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> Color(0xFFFF6B00)
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFFFFD600)
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFF06B6D4)
                MapThemeSlot.SLOT_4_VOID -> Color(0xFFF43F5E)
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFF6366F1)
            }
            val colorMerged = when (themeSlot) {
                MapThemeSlot.SLOT_1_DEFAULT -> Color(0xFFD946EF)
                MapThemeSlot.SLOT_2_BLUEPRINT -> Color(0xFFFF2A6D)
                MapThemeSlot.SLOT_3_OUTPOST -> Color(0xFF84CC16)
                MapThemeSlot.SLOT_4_VOID -> Color(0xFF06F9D2)
                MapThemeSlot.SLOT_5_MATRIX -> Color(0xFFFBBF24)
            }

            drawTerrainGrid(
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                boardWidthPx = boardWidthPx,
                boardHeightPx = boardHeightPx,
                cellW = cellW,
                cellH = cellH,
                shakeX = shakeX,
                shakeY = shakeY,
                groundColor = groundColor,
                gridLineColor = gridLineColor
            )

            val animFlowTime = if (state.animTimeMs > 0L) state.animTimeMs else System.currentTimeMillis()

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
                colorMerged = colorMerged,
                lanePathA = lanePathA,
                lanePathB = lanePathB,
                mergedPath = mergedPath
            )

            drawTerrainTiles(
                state = state,
                level = level,
                cellW = cellW,
                cellH = cellH,
                shakeX = shakeX,
                shakeY = shakeY,
                colorMerged = colorMerged,
                animFlowTime = animFlowTime
            )

            drawTrackParticles(
                trackParticles = state.trackParticles,
                level = level,
                cellW = cellW,
                cellH = cellH,
                shakeX = shakeX,
                shakeY = shakeY
            )

            drawSelectedTowerOverlays(
                state = state,
                level = level,
                cellW = cellW,
                cellH = cellH,
                shakeX = shakeX,
                shakeY = shakeY,
                animFlowTime = animFlowTime
            )

            drawTowers(
                state = state,
                cellW = cellW,
                cellH = cellH,
                shakeX = shakeX,
                shakeY = shakeY,
                animFlowTime = animFlowTime,
                scratchPath1 = scratchPath1,
                scratchPath2 = scratchPath2
            )

            drawCombatEntities(
                state = state,
                cellW = cellW,
                cellH = cellH,
                shakeX = shakeX,
                shakeY = shakeY,
                scratchPath1 = scratchPath1
            )

            drawTacticalTutorialPointer(
                state = state,
                level = level,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                cellW = cellW,
                cellH = cellH,
                shakeX = shakeX,
                shakeY = shakeY,
                animFlowTime = animFlowTime,
                scratchPath = scratchPath1
            )
        }
    }
}
