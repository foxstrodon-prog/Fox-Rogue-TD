package com.example.ui.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.GameUIState
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Renders terrain background grid, obstacles, spawners, core, buff pads, spatial aura links,
 * selected range rings, and tactical Wave-1 tutorial pointer.
 */
fun DrawScope.drawTerrainGrid(
    gridWidth: Int,
    gridHeight: Int,
    boardWidthPx: Float,
    boardHeightPx: Float,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float,
    groundColor: Color,
    gridLineColor: Color
) {
    drawRect(
        color = groundColor,
        topLeft = Offset(shakeX, shakeY),
        size = Size(boardWidthPx, boardHeightPx)
    )

    val strokePx = 1.dp.toPx()
    for (x in 0..gridWidth) {
        drawLine(
            color = gridLineColor,
            start = Offset(x * cellW + shakeX, shakeY),
            end = Offset(x * cellW + shakeX, boardHeightPx + shakeY),
            strokeWidth = strokePx
        )
    }
    for (y in 0..gridHeight) {
        drawLine(
            color = gridLineColor,
            start = Offset(shakeX, y * cellH + shakeY),
            end = Offset(boardWidthPx + shakeX, y * cellH + shakeY),
            strokeWidth = strokePx
        )
    }
}

fun DrawScope.drawTerrainTiles(
    state: GameUIState,
    level: GeneratedLevel,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float,
    colorMerged: Color,
    animFlowTime: Long
) {
    val tiles = level.tiles
    for (i in 0 until tiles.size) {
        val tile = tiles[i]
        val tx = tile.pos.x * cellW + shakeX
        val ty = tile.pos.y * cellH + shakeY
        val centerX = tx + cellW * 0.5f
        val centerY = ty + cellH * 0.5f

        when (tile.type) {
            TileType.PATH_MERGED, TileType.PATH_INTERSECTION -> {
                val pulse = (sin(animFlowTime * 0.005f) * 0.15f + 0.85f).toFloat()
                drawCircle(
                    color = colorMerged.copy(alpha = 0.28f * pulse),
                    radius = cellW * 0.35f,
                    center = Offset(centerX, centerY)
                )
            }
            TileType.OBSTACLE -> {
                val obsRect = Size(cellW * 0.72f, cellH * 0.72f)
                val left = centerX - obsRect.width / 2f
                val top = centerY - obsRect.height / 2f
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(left, top),
                    size = obsRect,
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(left, top),
                    size = obsRect,
                    cornerRadius = CornerRadius(4f, 4f),
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
                    drawRoundRect(
                        color = buffColor.copy(alpha = 0.18f * pulse),
                        topLeft = Offset(tx + 2f, ty + 2f),
                        size = Size(cellW - 4f, cellH - 4f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = buffColor.copy(alpha = 0.85f),
                        topLeft = Offset(tx + 2f, ty + 2f),
                        size = Size(cellW - 4f, cellH - 4f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.8f)
                    )
                    drawCircle(color = buffColor.copy(alpha = 0.40f * pulse), radius = cellW * 0.22f, center = Offset(centerX, centerY))
                    drawCircle(color = buffColor, radius = cellW * 0.14f, center = Offset(centerX, centerY), style = Stroke(width = 2.0f))
                    drawCircle(color = Color.White, radius = cellW * 0.05f, center = Offset(centerX, centerY))

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
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2.5f)
            )
        }
    }
}

fun DrawScope.drawSelectedTowerOverlays(
    state: GameUIState,
    level: GeneratedLevel,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float,
    animFlowTime: Long
) {
    val selTower = state.selectedTower ?: return
    if (selTower.isTier3) {
        val arch = selTower.archetype ?: mapTypeToArchetype(selTower.type)
        val auraColor = Color(arch.color)
        val emitterX = (selTower.gridPos.x + 0.5f) * cellW + shakeX
        val emitterY = (selTower.gridPos.y + 0.5f) * cellH + shakeY
        val auraPulsePhase = ((animFlowTime % 1200L) / 1200f)

        for (i in 0 until state.towers.size) {
            val other = state.towers[i]
            if (other.id != selTower.id) {
                val receiverX = (other.gridPos.x + 0.5f) * cellW + shakeX
                val receiverY = (other.gridPos.y + 0.5f) * cellH + shakeY
                drawLine(color = auraColor.copy(alpha = 0.35f), start = Offset(emitterX, emitterY), end = Offset(receiverX, receiverY), strokeWidth = 2.dp.toPx())
                drawLine(color = Color.White.copy(alpha = 0.70f), start = Offset(emitterX, emitterY), end = Offset(receiverX, receiverY), strokeWidth = 0.8.dp.toPx())

                val nodeX = emitterX + (receiverX - emitterX) * auraPulsePhase
                val nodeY = emitterY + (receiverY - emitterY) * auraPulsePhase
                drawCircle(color = auraColor, radius = 4.dp.toPx(), center = Offset(nodeX, nodeY))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(nodeX, nodeY))
                drawCircle(color = auraColor.copy(alpha = 0.40f), radius = cellW * 0.45f, center = Offset(receiverX, receiverY), style = Stroke(width = 1.5f))
            }
        }
    }

    var tileBuff = TileBuff.NONE
    for (i in 0 until level.tiles.size) {
        val t = level.tiles[i]
        if (t.pos == selTower.gridPos) {
            tileBuff = t.buff
            break
        }
    }
    val effectiveRange = selTower.getEffectiveCombatRange(
        tileBuff = tileBuff,
        globalRangeBonus = state.globalRangeBonus,
        balanceRange = state.balanceConfig.getTowerBalance(selTower.type).range
    )
    val pixelRadius = effectiveRange * cellW
    val tcx = (selTower.gridPos.x + 0.5f) * cellW + shakeX
    val tcy = (selTower.gridPos.y + 0.5f) * cellH + shakeY
    val rangeColor = Color(selTower.archetype?.color ?: selTower.type.color)
    drawCircle(color = rangeColor.copy(alpha = 0.12f), radius = pixelRadius, center = Offset(tcx, tcy))
    drawCircle(color = rangeColor.copy(alpha = 0.5f), radius = pixelRadius, center = Offset(tcx, tcy), style = Stroke(width = 1.5f))
}

fun DrawScope.drawTacticalTutorialPointer(
    state: GameUIState,
    level: GeneratedLevel,
    gridWidth: Int,
    gridHeight: Int,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float,
    animFlowTime: Long,
    scratchPath: Path
) {
    if (state.hasClickedFirstTile || state.waveNumber != 1 || state.towers.isNotEmpty()) return

    var targetTile: MapTile? = null
    for (i in 0 until level.tiles.size) {
        val tile = level.tiles[i]
        if (tile.type == TileType.EMPTY && tile.buff != TileBuff.NONE) {
            targetTile = tile
            break
        }
    }
    if (targetTile == null) {
        var bestScore = Float.NEGATIVE_INFINITY
        val spawnerAlpha = level.laneAlpha.first()
        val spawnerBeta = level.laneBeta.first()
        val earlyAlphaLimit = maxOf(3, level.laneAlpha.size / 2)
        val earlyBetaLimit = maxOf(3, level.laneBeta.size / 2)
        for (i in 0 until level.tiles.size) {
            val tile = level.tiles[i]
            if (tile.type != TileType.EMPTY) continue
            val dAlpha = hypot((tile.pos.x - spawnerAlpha.x).toFloat(), (tile.pos.y - spawnerAlpha.y).toFloat())
            val dBeta = hypot((tile.pos.x - spawnerBeta.x).toFloat(), (tile.pos.y - spawnerBeta.y).toFloat())
            val dCenter = hypot((tile.pos.x - gridWidth / 2f), (tile.pos.y - gridHeight / 2f))
            var coverage = 0
            for (j in 0 until minOf(earlyAlphaLimit, level.laneAlpha.size)) {
                val p = level.laneAlpha[j]
                if (hypot((tile.pos.x - p.x).toFloat(), (tile.pos.y - p.y).toFloat()) <= 2.8f) coverage++
            }
            for (j in 0 until minOf(earlyBetaLimit, level.laneBeta.size)) {
                val p = level.laneBeta[j]
                if (hypot((tile.pos.x - p.x).toFloat(), (tile.pos.y - p.y).toFloat()) <= 2.8f) coverage++
            }
            val score = coverage * 15f + (50f - minOf(dAlpha, dBeta) * 3f - dCenter * 2f)
            if (score > bestScore) {
                bestScore = score
                targetTile = tile
            }
        }
    }

    if (targetTile != null) {
        val bx = (targetTile.pos.x + 0.5f) * cellW + shakeX
        val by = (targetTile.pos.y + 0.5f) * cellH + shakeY
        val bounce = sin(animFlowTime * 0.008f) * 8f
        val arrowTipY = by - cellH * 0.40f + bounce
        val buffColor = Color(targetTile.buff.badgeColor)

        scratchPath.reset()
        scratchPath.moveTo(bx, arrowTipY)
        scratchPath.lineTo(bx - cellW * 0.22f, arrowTipY - cellH * 0.28f)
        scratchPath.lineTo(bx - cellW * 0.09f, arrowTipY - cellH * 0.28f)
        scratchPath.lineTo(bx - cellW * 0.09f, arrowTipY - cellH * 0.50f)
        scratchPath.lineTo(bx + cellW * 0.09f, arrowTipY - cellH * 0.50f)
        scratchPath.lineTo(bx + cellW * 0.09f, arrowTipY - cellH * 0.28f)
        scratchPath.lineTo(bx + cellW * 0.22f, arrowTipY - cellH * 0.28f)
        scratchPath.close()

        drawPath(scratchPath, color = buffColor)
        drawPath(scratchPath, color = Color.White, style = Stroke(width = 2.0f))

        val ringPulse = (sin(animFlowTime * 0.007f) * 0.25f + 0.75f).toFloat()
        drawCircle(
            color = buffColor.copy(alpha = 0.6f * ringPulse),
            radius = cellW * 0.42f,
            center = Offset(bx, by),
            style = Stroke(width = 2.5f)
        )
    }
}
