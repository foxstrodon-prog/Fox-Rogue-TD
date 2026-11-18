package com.example.ui.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.engine.TowerUpgradeManager
import com.example.model.*
import com.example.ui.GameUIState
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
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
    animFlowTime: Long,
    scratchPath: Path = Path()
) {
    val unlockedClasses = TowerClass.entries.filter { TowerUpgradeManager.isClassUnlocked(it, state.towers) }
    val minTowerCost = if (unlockedClasses.isNotEmpty()) {
        unlockedClasses.minOf { (it.baseCost * state.balanceConfig.buildCostMultiplier).roundToInt() }
    } else {
        Int.MAX_VALUE
    }
    val canAffordNewTower = state.scrap >= minTowerCost
    val occupiedTowerPositions = state.towers.map { it.gridPos }.toSet()
    val flowTimeF = (animFlowTime % 100_000L).toFloat()

    val tiles = level.tiles
    for (i in 0 until tiles.size) {
        val tile = tiles[i]
        val tx = tile.pos.x * cellW + shakeX
        val ty = tile.pos.y * cellH + shakeY
        val centerX = tx + cellW * 0.5f
        val centerY = ty + cellH * 0.5f

        when (tile.type) {
            TileType.PATH_MERGED, TileType.PATH_INTERSECTION -> {
                val pulse = sin(flowTimeF * 0.005f) * 0.15f + 0.85f
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
                drawEntryPrismPortal(
                    centerX = centerX,
                    centerY = centerY,
                    cellW = cellW,
                    lane = level.laneAlpha,
                    primaryColor = Color(0xFF00F0FF),
                    accentColor = Color(0xFFFF2A55),
                    flowTimeF = flowTimeF,
                    scratchPath = scratchPath
                )
            }
            TileType.SPAWNER_BETA -> {
                drawEntryPrismPortal(
                    centerX = centerX,
                    centerY = centerY,
                    cellW = cellW,
                    lane = level.laneBeta,
                    primaryColor = Color(0xFFFF9100),
                    accentColor = Color(0xFFFF2A55),
                    flowTimeF = flowTimeF,
                    scratchPath = scratchPath
                )
            }
            TileType.CORE -> {
                drawCoreShieldedHeartExit(
                    centerX = centerX,
                    centerY = centerY,
                    cellW = cellW,
                    coreHp = state.coreHp,
                    maxCoreHp = state.maxCoreHp,
                    laneAlpha = level.laneAlpha,
                    laneBeta = level.laneBeta,
                    flowTimeF = flowTimeF,
                    scratchPath = scratchPath
                )
            }
            TileType.EMPTY -> {
                if (tile.buff != TileBuff.NONE) {
                    val buffColor = Color(tile.buff.badgeColor)
                    val pulse = sin(flowTimeF * 0.006f) * 0.20f + 0.80f
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
                        val harmonicPulse = sin(flowTimeF * 0.010f) * 0.25f + 0.75f
                        drawCircle(
                            color = Color(0xFFFFD700).copy(alpha = 0.70f * harmonicPulse),
                            radius = cellW * 0.34f,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.5f)
                        )
                    }
                } else if (tile.isBuildable) {
                    val isUnoccupied = tile.pos !in occupiedTowerPositions
                    if (isUnoccupied) {
                        val fadeWave = sin(flowTimeF * 0.005f) * 0.5f + 0.5f
                        if (canAffordNewTower) {
                            drawRoundRect(
                                color = Color(0xFF38BDF8).copy(alpha = 0.05f + 0.06f * fadeWave),
                                topLeft = Offset(tx + 3f, ty + 3f),
                                size = Size(cellW - 6f, cellH - 6f),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                            drawRoundRect(
                                color = Color(0xFF38BDF8).copy(alpha = 0.14f + 0.18f * fadeWave),
                                topLeft = Offset(tx + 2.5f, ty + 2.5f),
                                size = Size(cellW - 5f, cellH - 5f),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 4.0f)
                            )
                            drawRoundRect(
                                color = Color(0xFF7DD3FC).copy(alpha = 0.38f + 0.36f * fadeWave),
                                topLeft = Offset(tx + 3f, ty + 3f),
                                size = Size(cellW - 6f, cellH - 6f),
                                cornerRadius = CornerRadius(5f, 5f),
                                style = Stroke(width = 1.6f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.25f + 0.25f * fadeWave),
                                radius = cellW * 0.05f,
                                center = Offset(centerX, centerY)
                            )
                        } else {
                            drawRoundRect(
                                color = Color(0xFF38BDF8).copy(alpha = 0.03f + 0.03f * fadeWave),
                                topLeft = Offset(tx + 3f, ty + 3f),
                                size = Size(cellW - 6f, cellH - 6f),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                            drawRoundRect(
                                color = Color(0xFF38BDF8).copy(alpha = 0.10f + 0.10f * fadeWave),
                                topLeft = Offset(tx + 2.5f, ty + 2.5f),
                                size = Size(cellW - 5f, cellH - 5f),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 3.0f)
                            )
                            drawRoundRect(
                                color = Color(0xFF64748B).copy(alpha = 0.68f + 0.15f * fadeWave),
                                topLeft = Offset(tx + 3f, ty + 3f),
                                size = Size(cellW - 6f, cellH - 6f),
                                cornerRadius = CornerRadius(5f, 5f),
                                style = Stroke(width = 1.4f)
                            )
                        }
                    } else {
                        drawRoundRect(
                            color = Color(0xFF1E293B).copy(alpha = 0.55f),
                            topLeft = Offset(tx + 3f, ty + 3f),
                            size = Size(cellW - 6f, cellH - 6f),
                            cornerRadius = CornerRadius(5f, 5f),
                            style = Stroke(width = 1.2f)
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

/**
 * Resolves the tower whose Tier-3 synergy lines should be drawn: the currently selected tower, otherwise the
 * last selected tower (persisting after the squircle dock closes). Null unless that tower is Tier 3+.
 */
internal fun resolveTier3SynergyTower(state: GameUIState): TowerInstance? {
    val tower = state.selectedTower ?: state.towers.firstOrNull { it.id == state.lastSelectedTowerId }
    return tower?.takeIf { it.isTier3 }
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
    // Tier-3 synergy lines persist for the last selected tower even after the squircle dock closes.
    val synergyTower = resolveTier3SynergyTower(state)
    if (synergyTower != null) {
        val arch = synergyTower.archetype ?: mapTypeToArchetype(synergyTower.type)
        val auraColor = Color(arch.color)
        val emitterX = (synergyTower.gridPos.x + 0.5f) * cellW + shakeX
        val emitterY = (synergyTower.gridPos.y + 0.5f) * cellH + shakeY
        val auraPulsePhase = ((animFlowTime % 1200L) / 1200f)

        for (i in 0 until state.towers.size) {
            val other = state.towers[i]
            if (other.id != synergyTower.id) {
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

    val selTower = state.selectedTower ?: return
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
        balanceRange = state.balanceConfig.findCustomTowerBalance(selTower)?.range
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
        if (tile.isBuildable && tile.buff != TileBuff.NONE) {
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
            if (!tile.isBuildable) continue
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
        val flowTimeF = (animFlowTime % 100_000L).toFloat()
        val bx = (targetTile.pos.x + 0.5f) * cellW + shakeX
        val by = (targetTile.pos.y + 0.5f) * cellH + shakeY
        val bounce = sin(flowTimeF * 0.008f) * 8f
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

        val ringPulse = sin(flowTimeF * 0.007f) * 0.25f + 0.75f
        drawCircle(
            color = buffColor.copy(alpha = 0.6f * ringPulse),
            radius = cellW * 0.42f,
            center = Offset(bx, by),
            style = Stroke(width = 2.5f)
        )
    }
}

/**
 * Draws an App-Logo Inspired Prism Portal at an enemy lane entry point (`SPAWNER_ALPHA` / `SPAWNER_BETA`).
 * Features an outer hexagonal portal frame, a glowing inner directional prism oriented along the initial
 * lane vector (`lane[0] -> lane[1]`), and animated outward-pulsing directional chevrons (`>>>`).
 * Reuses [scratchPath] for zero per-frame allocations.
 */
private fun DrawScope.drawEntryPrismPortal(
    centerX: Float,
    centerY: Float,
    cellW: Float,
    lane: List<GridPos>,
    primaryColor: Color,
    accentColor: Color,
    flowTimeF: Float,
    scratchPath: Path
) {
    val pulse = sin(flowTimeF * 0.007f) * 0.15f + 0.85f
    val dirX: Float
    val dirY: Float
    if (lane.size >= 2) {
        val dx = (lane[1].x - lane[0].x).toFloat()
        val dy = (lane[1].y - lane[0].y).toFloat()
        val len = hypot(dx, dy).coerceAtLeast(0.001f)
        dirX = dx / len
        dirY = dy / len
    } else {
        dirX = 0f
        dirY = 1f
    }
    val perpX = -dirY
    val perpY = dirX

    // 1. Subtle crimson-neon portal aura
    drawCircle(
        color = accentColor.copy(alpha = 0.24f * pulse),
        radius = cellW * 0.46f,
        center = Offset(centerX, centerY)
    )

    // 2. Outer hexagonal frame (app-logo geometric silhouette)
    val hexRadius = cellW * 0.38f
    scratchPath.reset()
    for (k in 0 until 6) {
        val angle = (k * 60.0 - 30.0) * (Math.PI / 180.0)
        val vx = centerX + (cos(angle).toFloat() * hexRadius)
        val vy = centerY + (sin(angle).toFloat() * hexRadius)
        if (k == 0) scratchPath.moveTo(vx, vy) else scratchPath.lineTo(vx, vy)
    }
    scratchPath.close()
    drawPath(path = scratchPath, color = Color(0xFF0B111E).copy(alpha = 0.85f))
    drawPath(path = scratchPath, color = primaryColor.copy(alpha = 0.90f), style = Stroke(width = 2.4f))

    // 3. Directional inner prism triangle pointing toward lane[1]
    val tipDist = cellW * 0.26f
    val baseDist = -cellW * 0.16f
    val halfWing = cellW * 0.20f
    val tipX = centerX + dirX * tipDist
    val tipY = centerY + dirY * tipDist
    val baseLeftX = centerX + dirX * baseDist + perpX * halfWing
    val baseLeftY = centerY + dirY * baseDist + perpY * halfWing
    val baseRightX = centerX + dirX * baseDist - perpX * halfWing
    val baseRightY = centerY + dirY * baseDist - perpY * halfWing

    scratchPath.reset()
    scratchPath.moveTo(tipX, tipY)
    scratchPath.lineTo(baseLeftX, baseLeftY)
    scratchPath.lineTo(centerX + dirX * (baseDist * 0.45f), centerY + dirY * (baseDist * 0.45f))
    scratchPath.lineTo(baseRightX, baseRightY)
    scratchPath.close()

    drawPath(path = scratchPath, color = primaryColor.copy(alpha = 0.85f))
    drawPath(path = scratchPath, color = Color.White.copy(alpha = 0.92f), style = Stroke(width = 1.4f))

    // 4. Outward-flowing directional chevrons (>>>) stepping into the first tile
    val phase = ((flowTimeF * 0.0022f) % 1.0f + 1.0f) % 1.0f
    for (c in 0 until 2) {
        val cProg = (phase + c * 0.5f) % 1.0f
        val dist = cellW * (0.34f + cProg * 0.48f)
        val alpha = (1.0f - cProg).coerceIn(0.15f, 0.95f)
        val cx = centerX + dirX * dist
        val cy = centerY + dirY * dist
        val chevBack = cellW * 0.11f
        val chevWing = cellW * 0.14f

        val leftX = cx - dirX * chevBack + perpX * chevWing
        val leftY = cy - dirY * chevBack + perpY * chevWing
        val rightX = cx - dirX * chevBack - perpX * chevWing
        val rightY = cy - dirY * chevBack - perpY * chevWing

        drawLine(
            color = primaryColor.copy(alpha = alpha),
            start = Offset(leftX, leftY),
            end = Offset(cx, cy),
            strokeWidth = 2.2f
        )
        drawLine(
            color = primaryColor.copy(alpha = alpha),
            start = Offset(rightX, rightY),
            end = Offset(cx, cy),
            strokeWidth = 2.2f
        )
    }
}

/**
 * Draws the Defense Core Exit (`TileType.CORE`) with a Shielded Heart Emblem matching the Top HUD
 * Core HP iconography, framed by a pulsing octagonal aegis ring and inward-pointing caution chevrons.
 * Reuses [scratchPath] for zero per-frame allocations.
 */
private fun DrawScope.drawCoreShieldedHeartExit(
    centerX: Float,
    centerY: Float,
    cellW: Float,
    coreHp: Int,
    maxCoreHp: Int,
    laneAlpha: List<GridPos>,
    laneBeta: List<GridPos>,
    flowTimeF: Float,
    scratchPath: Path
) {
    val pulse = sin(flowTimeF * 0.005f) * 0.14f + 0.86f
    val hpRatio = if (maxCoreHp > 0) (coreHp.toFloat() / maxCoreHp.toFloat()).coerceIn(0f, 1f) else 1f
    val shieldColor = if (hpRatio > 0.35f) Color(0xFF00FF9D) else Color(0xFFFF2A55)
    val heartColor = Color(0xFFFF2A55)

    // 1. Ambient Core Shield Glow
    drawCircle(
        color = shieldColor.copy(alpha = 0.22f * pulse),
        radius = cellW * 0.52f,
        center = Offset(centerX, centerY)
    )

    // 2. Pulsing Octagonal Aegis Ring
    val octRadius = cellW * 0.39f * pulse
    scratchPath.reset()
    for (k in 0 until 8) {
        val angle = (k * 45.0 + 22.5) * (Math.PI / 180.0)
        val vx = centerX + (cos(angle).toFloat() * octRadius)
        val vy = centerY + (sin(angle).toFloat() * octRadius)
        if (k == 0) scratchPath.moveTo(vx, vy) else scratchPath.lineTo(vx, vy)
    }
    scratchPath.close()
    drawPath(path = scratchPath, color = Color(0xFF0B111E).copy(alpha = 0.88f))
    drawPath(path = scratchPath, color = shieldColor, style = Stroke(width = 2.6f))
    drawCircle(color = Color(0xFF00F0FF).copy(alpha = 0.65f), radius = cellW * 0.29f, center = Offset(centerX, centerY), style = Stroke(width = 1.2f))

    // 3. Shielded Heart Emblem (matches Top HUD Core HP Heart Icon)
    val hr = cellW * 0.18f
    val topDipY = centerY - hr * 0.35f
    val bottomTipY = centerY + hr * 0.95f
    scratchPath.reset()
    scratchPath.moveTo(centerX, bottomTipY)
    scratchPath.cubicTo(
        centerX - hr * 1.35f, centerY + hr * 0.25f,
        centerX - hr * 1.15f, centerY - hr * 0.95f,
        centerX, topDipY
    )
    scratchPath.cubicTo(
        centerX + hr * 1.15f, centerY - hr * 0.95f,
        centerX + hr * 1.35f, centerY + hr * 0.25f,
        centerX, bottomTipY
    )
    scratchPath.close()
    drawPath(path = scratchPath, color = heartColor.copy(alpha = 0.92f))
    drawPath(path = scratchPath, color = Color.White.copy(alpha = 0.90f), style = Stroke(width = 1.3f))

    // 4. Inward-pointing approach chevrons on final lane segment(s) leading into the Core
    fun drawApproachChevron(lane: List<GridPos>) {
        if (lane.size < 2) return
        val prev = lane[lane.size - 2]
        val last = lane.last()
        val dx = (last.x - prev.x).toFloat()
        val dy = (last.y - prev.y).toFloat()
        val len = hypot(dx, dy).coerceAtLeast(0.001f)
        val dirX = dx / len
        val dirY = dy / len
        val perpX = -dirY
        val perpY = dirX

        val phase = ((flowTimeF * 0.0022f) % 1.0f + 1.0f) % 1.0f
        val distFromCore = cellW * (0.78f - phase * 0.36f)
        val cx = centerX - dirX * distFromCore
        val cy = centerY - dirY * distFromCore
        val chevBack = cellW * 0.10f
        val chevWing = cellW * 0.13f

        val leftX = cx - dirX * chevBack + perpX * chevWing
        val leftY = cy - dirY * chevBack + perpY * chevWing
        val rightX = cx - dirX * chevBack - perpX * chevWing
        val rightY = cy - dirY * chevBack - perpY * chevWing

        drawLine(
            color = heartColor.copy(alpha = 0.80f),
            start = Offset(leftX, leftY),
            end = Offset(cx, cy),
            strokeWidth = 2.0f
        )
        drawLine(
            color = heartColor.copy(alpha = 0.80f),
            start = Offset(rightX, rightY),
            end = Offset(cx, cy),
            strokeWidth = 2.0f
        )
    }

    drawApproachChevron(laneAlpha)
    if (laneBeta.size >= 2 && (laneAlpha.size < 2 || laneBeta[laneBeta.size - 2] != laneAlpha[laneAlpha.size - 2])) {
        drawApproachChevron(laneBeta)
    }
}

