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
            .pointerInput(level.seed, gridWidth, gridHeight) {
                detectTapGestures { offset ->
                    val cellSize = minOf(size.width.toFloat() / gridWidth.toFloat(), size.height.toFloat() / gridHeight.toFloat())
                    val boardWidthPx = cellSize * gridWidth
                    val boardHeightPx = cellSize * gridHeight
                    val cellW = cellSize
                    val cellH = cellSize

                    val boardOriginX = (size.width.toFloat() - boardWidthPx) / 2f
                    val boardOriginY = (size.height.toFloat() - boardHeightPx) / 2f

                    val localX = offset.x - boardOriginX
                    val localY = offset.y - boardOriginY
                    if (localX in 0f..boardWidthPx && localY in 0f..boardHeightPx) {
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
        val cellSize = minOf(size.width / gridWidth.toFloat(), size.height / gridHeight.toFloat())
        val boardWidthPx = cellSize * gridWidth
        val boardHeightPx = cellSize * gridHeight
        val cellW = cellSize
        val cellH = cellSize

        val boardOriginX = (size.width - boardWidthPx) / 2f
        val boardOriginY = (size.height - boardHeightPx) / 2f

        val shakePhase = (System.currentTimeMillis() % 100_000L).toFloat() * 0.05f
        val shakeX = if (state.screenShakeIntensity > 0f) sin(shakePhase) * state.screenShakeIntensity * 12f else 0f
        val shakeY = if (state.screenShakeIntensity > 0f) cos(shakePhase) * state.screenShakeIntensity * 12f else 0f

        drawRect(color = Color(0xFF070B13))

        withTransform({
            translate(boardOriginX, boardOriginY)
        }) {
            val themeDef = com.example.audio.BuildArchiveCatalog.resolveMapTheme(state.mapThemeId, state.mapThemeSlot)
            val groundColor = if (themeDef.useBiomePalette) biome.groundColor else Color(themeDef.groundColor)
            val gridLineColor = if (themeDef.useBiomePalette) biome.groundSecondary.copy(alpha = 0.5f) else Color(themeDef.gridLineColor).copy(alpha = themeDef.gridLineAlpha)
            val pathBaseColor = if (themeDef.useBiomePalette) biome.pathColor else Color(themeDef.pathBaseColor)
            val colorAlpha = Color(themeDef.laneAlphaColor)
            val colorBeta = Color(themeDef.laneBetaColor)
            val colorMerged = Color(themeDef.laneMergedColor)

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
                animFlowTime = animFlowTime,
                scratchPath = scratchPath1
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
