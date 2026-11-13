package com.example.ui.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.GameUIState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Pre-computed cardinal angles in radians for zero-allocation CAD drafting ticks (Task D-03)
private val CARDINAL_RADS = floatArrayOf(0f, (PI / 2.0).toFloat(), PI.toFloat(), (3.0 * PI / 2.0).toFloat())

private fun buildStarPath(cx: Float, cy: Float, outerR: Float, innerR: Float, rotationRad: Float, outPath: Path) {
    outPath.reset()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = (i * PI / 5f - PI / 2f).toFloat() + rotationRad
        val px = cx + cos(angle) * r
        val py = cy + sin(angle) * r
        if (i == 0) outPath.moveTo(px, py) else outPath.lineTo(px, py)
    }
    outPath.close()
}

private fun buildTrianglePath(p1: Offset, p2: Offset, p3: Offset, outPath: Path) {
    outPath.reset()
    outPath.moveTo(p1.x, p1.y)
    outPath.lineTo(p2.x, p2.y)
    outPath.lineTo(p3.x, p3.y)
    outPath.close()
}

private fun DrawScope.drawCadCross(pt: Offset, color: Color) {
    drawLine(color, Offset(pt.x - 3f, pt.y), Offset(pt.x + 3f, pt.y), 1.2f)
    drawLine(color, Offset(pt.x, pt.y - 3f), Offset(pt.x, pt.y + 3f), 1.2f)
}

private fun DrawScope.drawFourCornerRects(cx: Float, cy: Float, halfSpan: Float, unit: Float, color: Color) {
    val sz = Size(unit, unit)
    drawRect(color, Offset(cx - halfSpan, cy - halfSpan), sz)
    drawRect(color, Offset(cx + halfSpan - unit, cy - halfSpan), sz)
    drawRect(color, Offset(cx - halfSpan, cy + halfSpan - unit), sz)
    drawRect(color, Offset(cx + halfSpan - unit, cy + halfSpan - unit), sz)
}

private fun DrawScope.drawDualRails(
    tcx: Float,
    tcy: Float,
    aimRad: Float,
    spread: Float,
    railLen: Float,
    railColor: Color,
    strokeW: Float,
    cap: StrokeCap,
    braceColor: Color? = null,
    braceWidth: Float = 2.5f
) {
    val perp = aimRad + (PI / 2f).toFloat()
    val r1 = Offset(tcx + cos(perp) * spread, tcy + sin(perp) * spread)
    val r2 = Offset(tcx - cos(perp) * spread, tcy - sin(perp) * spread)
    val dx = cos(aimRad) * railLen
    val dy = sin(aimRad) * railLen
    drawLine(railColor, r1, Offset(r1.x + dx, r1.y + dy), strokeW, cap)
    drawLine(railColor, r2, Offset(r2.x + dx, r2.y + dy), strokeW, cap)
    if (braceColor != null) {
        drawLine(braceColor, r1, r2, braceWidth)
    }
}

/**
 * Renders all towers across all 5 turret skin slots with zero per-frame collection allocations (Task D-03 & D-06).
 */
fun DrawScope.drawTowers(
    state: GameUIState,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float,
    animFlowTime: Long,
    scratchPath1: Path,
    scratchPath2: Path
) {
    val skinSlot = TurretSkinSlot.fromSlot(state.turretSkinSlot)
    val auraPulse = (sin(animFlowTime * 0.008f) * 0.15f + 0.85f).toFloat()
    val apexAuraColor = Color(0xFFFFB800)

    for (idx in 0 until state.towers.size) {
        val tower = state.towers[idx]
        val tcx = (tower.gridPos.x + 0.5f) * cellW + shakeX
        val tcy = (tower.gridPos.y + 0.5f) * cellH + shakeY
        val center = Offset(tcx, tcy)
        val effectiveClass = tower.archetype?.towerClass ?: tower.towerClass
        val towerColor = Color(tower.archetype?.color ?: effectiveClass.color)
        val aimRad = tower.rotationAngleDeg * (PI / 180f).toFloat()
        val borderColor = if (tower.isFinalForm) apexAuraColor else towerColor.copy(alpha = 0.88f)

        val tier1Vector = tower.archetype?.vector ?: tower.vector ?: when (tower.branchTier1) {
            "A" -> TowerVector.OFFENSIVE
            "B" -> TowerVector.DEFENSIVE
            "C" -> TowerVector.SUPPORT
            else -> null
        }
        if (tier1Vector != null || tower.tier >= 1) {
            val ringColor = when (tier1Vector) {
                TowerVector.OFFENSIVE -> Color(0xFFFF2A55)
                TowerVector.DEFENSIVE -> Color(0xFF00E676)
                TowerVector.SUPPORT -> Color(0xFFFFD700)
                null -> Color(0xFFFF2A55)
            }
            drawCircle(ringColor.copy(alpha = 0.30f), cellW * 0.44f, center, style = Stroke(width = 3.5f))
            drawCircle(ringColor, cellW * 0.41f, center, style = Stroke(width = 2.0f))
        }

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

        if (upgradeLevel == 4) {
            when (effectiveClass) {
                TowerClass.KINETIC_BALLISTIC -> {
                    drawCircle(apexAuraColor.copy(alpha = 0.22f * auraPulse), cellW * 0.50f, center)
                    drawCircle(apexAuraColor, cellW * 0.45f, center, style = Stroke(width = 2.0f))
                }
                TowerClass.ELEMENTAL_VOID -> {
                    val hR = cellW * 0.54f * auraPulse
                    buildTrianglePath(Offset(tcx, tcy - hR), Offset(tcx + hR * 0.866f, tcy + hR * 0.5f), Offset(tcx - hR * 0.866f, tcy + hR * 0.5f), scratchPath1)
                    drawPath(scratchPath1, apexAuraColor.copy(alpha = 0.20f * auraPulse))
                    drawPath(scratchPath1, apexAuraColor, style = Stroke(width = 2.0f, join = StrokeJoin.Round))
                }
                TowerClass.HEAVY_SIEGE -> {
                    val hS = cellW * 0.90f * auraPulse
                    val hTl = Offset(tcx - hS / 2f, tcy - hS / 2f)
                    drawRoundRect(apexAuraColor.copy(alpha = 0.20f * auraPulse), hTl, Size(hS, hS), CornerRadius(6f, 6f))
                    drawRoundRect(apexAuraColor, hTl, Size(hS, hS), CornerRadius(6f, 6f), style = Stroke(width = 2.0f))
                }
                TowerClass.STELLAR_COSMIC -> {
                    val sR = cellW * 0.52f * auraPulse
                    buildStarPath(tcx, tcy, sR, sR * 0.48f, 0f, scratchPath1)
                    drawPath(scratchPath1, apexAuraColor.copy(alpha = 0.22f * auraPulse))
                    drawPath(scratchPath1, apexAuraColor, style = Stroke(width = 2.0f, join = StrokeJoin.Round))
                }
            }
        }

        when (skinSlot) {
            TurretSkinSlot.SLOT_1_DEFAULT -> {
                when (effectiveClass) {
                    TowerClass.KINETIC_BALLISTIC -> {
                        drawCircle(Color(0xFF0F172A), cellW * 0.38f, center)
                        drawCircle(borderColor, cellW * 0.38f, center, style = Stroke(width = 2.5f))
                        if (upgradeLevel >= 2) {
                            val perp = aimRad + (PI / 2f).toFloat()
                            val wingLen = cellW * 0.22f
                            val wOff = cellW * 0.28f
                            drawLine(vectorAccentColor, Offset(tcx + cos(perp) * wOff, tcy + sin(perp) * wOff), Offset(tcx + cos(perp) * wOff - cos(aimRad) * wingLen, tcy + sin(perp) * wOff - sin(aimRad) * wingLen), cellW * 0.05f, StrokeCap.Round)
                            drawLine(vectorAccentColor, Offset(tcx - cos(perp) * wOff, tcy - sin(perp) * wOff), Offset(tcx - cos(perp) * wOff - cos(aimRad) * wingLen, tcy - sin(perp) * wOff - sin(aimRad) * wingLen), cellW * 0.05f, StrokeCap.Round)
                        }
                        val b1X = tcx + cos(aimRad) * cellW * 0.36f
                        val b1Y = tcy + sin(aimRad) * cellW * 0.36f
                        drawLine(towerColor, center, Offset(b1X, b1Y), cellW * 0.12f, StrokeCap.Round)
                        if (upgradeLevel >= 3) {
                            drawDualRails(tcx, tcy, aimRad, cellW * 0.09f, cellW * 0.36f, towerColor, cellW * 0.06f, StrokeCap.Round)
                        }
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else Color.White, cellW * 0.14f, center)
                        drawCircle(towerColor, cellW * 0.08f, center)
                    }
                    TowerClass.ELEMENTAL_VOID -> {
                        val r = cellW * 0.44f
                        buildTrianglePath(Offset(tcx, tcy - r), Offset(tcx + r * 0.866f, tcy + r * 0.5f), Offset(tcx - r * 0.866f, tcy + r * 0.5f), scratchPath1)
                        drawPath(scratchPath1, Color(0xFF140D26))
                        drawPath(scratchPath1, borderColor, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
                        if (upgradeLevel >= 2) {
                            val inR = cellW * 0.24f
                            buildTrianglePath(Offset(tcx, tcy + inR), Offset(tcx - inR * 0.866f, tcy - inR * 0.5f), Offset(tcx + inR * 0.866f, tcy - inR * 0.5f), scratchPath2)
                            drawPath(scratchPath2, vectorAccentColor.copy(alpha = 0.35f))
                            drawPath(scratchPath2, vectorAccentColor, style = Stroke(width = 1.5f, join = StrokeJoin.Round))
                        }
                        val beamLen = cellW * (if (upgradeLevel >= 3) 0.38f else 0.32f)
                        val beamEnd = Offset(tcx + cos(aimRad) * beamLen, tcy + sin(aimRad) * beamLen)
                        drawLine(towerColor, center, beamEnd, cellW * 0.09f, StrokeCap.Round)
                        drawLine(if (upgradeLevel == 4) apexAuraColor else Color.White, center, beamEnd, cellW * 0.04f, StrokeCap.Round)
                        val moteCount = if (upgradeLevel >= 3) 4 else 3
                        val orbAngle = animFlowTime * 0.005f
                        for (i in 0 until moteCount) {
                            val a = orbAngle + i * (2f * PI.toFloat() / moteCount)
                            drawCircle(if (upgradeLevel == 4 && i % 2 == 0) apexAuraColor else towerColor, cellW * 0.045f, Offset(tcx + cos(a) * cellW * 0.24f, tcy + sin(a) * cellW * 0.24f))
                        }
                        drawCircle(Color.White, cellW * 0.10f, center)
                        drawCircle(towerColor, cellW * 0.06f, center)
                    }
                    TowerClass.HEAVY_SIEGE -> {
                        val sqR = cellW * 0.38f
                        val sqTl = Offset(tcx - sqR, tcy - sqR)
                        val sqSz = Size(sqR * 2f, sqR * 2f)
                        drawRoundRect(Color(0xFF0F1420), sqTl, sqSz, CornerRadius(6f, 6f))
                        drawRoundRect(borderColor, sqTl, sqSz, CornerRadius(6f, 6f), style = Stroke(width = 2.0f))
                        val railLen = cellW * (if (upgradeLevel >= 3) 0.42f else 0.36f)
                        val railColor = if (upgradeLevel >= 3) towerColor else borderColor
                        drawDualRails(tcx, tcy, aimRad, cellW * 0.08f, railLen, railColor, cellW * 0.06f, StrokeCap.Round, if (upgradeLevel >= 2) vectorAccentColor else null, cellW * 0.04f)
                        val coreEnd = Offset(tcx + cos(aimRad) * (railLen * 0.95f), tcy + sin(aimRad) * (railLen * 0.95f))
                        drawLine(if (upgradeLevel == 4) apexAuraColor else Color.White, center, coreEnd, cellW * 0.035f, StrokeCap.Round)
                        drawCircle(Color(0xFF0F172A), cellW * 0.13f, center)
                        drawCircle(borderColor, cellW * 0.13f, center, style = Stroke(width = 1.8f))
                        drawCircle(Color.White, cellW * 0.06f, center)
                    }
                    TowerClass.STELLAR_COSMIC -> {
                        val starR = cellW * 0.40f
                        buildStarPath(tcx, tcy, starR, starR * 0.48f, aimRad, scratchPath1)
                        drawPath(scratchPath1, Color(0xFF1B1806))
                        drawPath(scratchPath1, borderColor, style = Stroke(width = 2.2f, join = StrokeJoin.Round))
                        if (upgradeLevel >= 2) {
                            buildStarPath(tcx, tcy, starR * 0.65f, starR * 0.48f * 0.65f, aimRad, scratchPath2)
                            drawPath(scratchPath2, vectorAccentColor, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
                        }
                        if (upgradeLevel >= 3) {
                            for (i in 0 until 5) {
                                val orbAngle = animFlowTime * 0.005f + (i * 2f * PI / 5f).toFloat()
                                drawCircle(towerColor, cellW * 0.04f, Offset(tcx + cos(orbAngle) * cellW * 0.45f, tcy + sin(orbAngle) * cellW * 0.45f))
                            }
                        }
                        drawLine(if (upgradeLevel == 4) apexAuraColor else Color.White, center, Offset(tcx + cos(aimRad) * cellW * 0.42f, tcy + sin(aimRad) * cellW * 0.42f), cellW * 0.05f, StrokeCap.Round)
                        drawCircle(Color(0xFFFFD700), cellW * 0.10f, center)
                        drawCircle(Color.White, cellW * 0.05f, center)
                    }
                }
            }

            TurretSkinSlot.SLOT_2_BLUEPRINT -> {
                val cadColor = Color(0xFF60A5FA)
                val cadWhite = Color.White.copy(alpha = 0.85f)
                val ch = cellW * 0.38f
                drawLine(cadColor.copy(alpha = 0.40f), Offset(tcx - ch, tcy), Offset(tcx + ch, tcy), 1.0f)
                drawLine(cadColor.copy(alpha = 0.40f), Offset(tcx, tcy - ch), Offset(tcx, tcy + ch), 1.0f)
                drawCircle(cadColor.copy(alpha = 0.30f), cellW * 0.35f, center, style = Stroke(width = 1.0f))

                when (effectiveClass) {
                    TowerClass.KINETIC_BALLISTIC -> {
                        drawCircle(Color(0xFF0A192F), cellW * 0.36f, center)
                        drawCircle(cadColor, cellW * 0.36f, center, style = Stroke(width = 1.8f))
                        for (k in 0 until 4) {
                            val r = CARDINAL_RADS[k]
                            drawLine(cadWhite, Offset(tcx + cos(r) * cellW * 0.33f, tcy + sin(r) * cellW * 0.33f), Offset(tcx + cos(r) * cellW * 0.38f, tcy + sin(r) * cellW * 0.38f), 1.5f)
                        }
                        if (upgradeLevel >= 2) {
                            val p = aimRad + (PI / 2f).toFloat()
                            val wOff = cellW * 0.26f
                            val tip = Offset(tcx + cos(aimRad) * cellW * 0.28f, tcy + sin(aimRad) * cellW * 0.28f)
                            drawLine(vectorAccentColor, Offset(tcx + cos(p) * wOff, tcy + sin(p) * wOff), tip, 1.5f)
                            drawLine(vectorAccentColor, Offset(tcx - cos(p) * wOff, tcy - sin(p) * wOff), tip, 1.5f)
                        }
                        val bLen = cellW * 0.36f
                        drawLine(cadWhite, center, Offset(tcx + cos(aimRad) * bLen, tcy + sin(aimRad) * bLen), cellW * 0.08f, StrokeCap.Square)
                        if (upgradeLevel >= 3) {
                            drawDualRails(tcx, tcy, aimRad, cellW * 0.08f, bLen, towerColor, 2.0f, StrokeCap.Butt)
                        }
                        drawCircle(Color(0xFF0F172A), cellW * 0.12f, center)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else cadColor, cellW * 0.07f, center)
                    }
                    TowerClass.ELEMENTAL_VOID -> {
                        val r = cellW * 0.42f
                        val p1 = Offset(tcx, tcy - r)
                        val p2 = Offset(tcx + r * 0.866f, tcy + r * 0.5f)
                        val p3 = Offset(tcx - r * 0.866f, tcy + r * 0.5f)
                        buildTrianglePath(p1, p2, p3, scratchPath1)
                        drawPath(scratchPath1, Color(0xFF0A192F))
                        drawPath(scratchPath1, cadColor, style = Stroke(width = 1.8f))
                        drawCadCross(p1, cadWhite)
                        drawCadCross(p2, cadWhite)
                        drawCadCross(p3, cadWhite)
                        if (upgradeLevel >= 2) drawCircle(vectorAccentColor.copy(alpha = 0.5f), cellW * 0.22f, center, style = Stroke(width = 1.5f))
                        val bEnd = Offset(tcx + cos(aimRad) * cellW * 0.35f, tcy + sin(aimRad) * cellW * 0.35f)
                        drawLine(cadColor, center, bEnd, cellW * 0.07f)
                        if (upgradeLevel >= 3) drawLine(towerColor, center, bEnd, cellW * 0.03f)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else cadWhite, cellW * 0.08f, center)
                    }
                    TowerClass.HEAVY_SIEGE -> {
                        val s = cellW * 0.72f
                        val tl = Offset(tcx - s / 2f, tcy - s / 2f)
                        drawRect(Color(0xFF0A192F), tl, Size(s, s))
                        drawRect(cadColor, tl, Size(s, s), style = Stroke(width = 1.8f))
                        drawLine(cadColor.copy(alpha = 0.35f), tl, Offset(tl.x + s, tl.y + s), 1.0f)
                        drawLine(cadColor.copy(alpha = 0.35f), Offset(tl.x + s, tl.y), Offset(tl.x, tl.y + s), 1.0f)
                        drawDualRails(tcx, tcy, aimRad, cellW * 0.07f, cellW * 0.38f, cadWhite, 2.0f, StrokeCap.Butt, if (upgradeLevel >= 2) vectorAccentColor else null, 2.0f)
                        if (upgradeLevel >= 3) drawCircle(towerColor, cellW * 0.12f, center, style = Stroke(width = 1.5f))
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else cadColor, cellW * 0.06f, center)
                    }
                    TowerClass.STELLAR_COSMIC -> {
                        val starR = cellW * 0.38f
                        buildStarPath(tcx, tcy, starR, starR * 0.45f, aimRad, scratchPath1)
                        drawPath(scratchPath1, Color(0xFF0A192F))
                        drawPath(scratchPath1, cadColor, style = Stroke(width = 1.8f))
                        if (upgradeLevel >= 2) drawCircle(vectorAccentColor.copy(alpha = 0.5f), cellW * 0.22f, center, style = Stroke(width = 1.5f))
                        if (upgradeLevel >= 3) drawCircle(towerColor, cellW * 0.12f, center, style = Stroke(width = 1.5f))
                        drawLine(if (upgradeLevel == 4) apexAuraColor else cadWhite, center, Offset(tcx + cos(aimRad) * cellW * 0.38f, tcy + sin(aimRad) * cellW * 0.38f), 2.0f)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else cadColor, cellW * 0.06f, center)
                    }
                }
            }

            TurretSkinSlot.SLOT_3_MILITARY -> {
                val steelDark = Color(0xFF1E293B)
                val steelPlate = Color(0xFF334155)
                val rivetColor = Color(0xFF94A3B8)

                when (effectiveClass) {
                    TowerClass.KINETIC_BALLISTIC -> {
                        drawCircle(steelDark, cellW * 0.38f, center)
                        drawCircle(steelPlate, cellW * 0.38f, center, style = Stroke(width = 3.5f))
                        val rOff = cellW * 0.26f
                        drawCircle(rivetColor, 2.5f, Offset(tcx - rOff, tcy - rOff))
                        drawCircle(rivetColor, 2.5f, Offset(tcx + rOff, tcy - rOff))
                        drawCircle(rivetColor, 2.5f, Offset(tcx - rOff, tcy + rOff))
                        drawCircle(rivetColor, 2.5f, Offset(tcx + rOff, tcy + rOff))
                        if (upgradeLevel >= 2) {
                            val p = aimRad + (PI / 2f).toFloat()
                            val wOff = cellW * 0.28f
                            drawRoundRect(vectorAccentColor, Offset(tcx + cos(p) * wOff - 4f, tcy + sin(p) * wOff - 4f), Size(8f, 8f), CornerRadius(2f, 2f))
                            drawRoundRect(vectorAccentColor, Offset(tcx - cos(p) * wOff - 4f, tcy - sin(p) * wOff - 4f), Size(8f, 8f), CornerRadius(2f, 2f))
                        }
                        val bEnd = Offset(tcx + cos(aimRad) * cellW * 0.38f, tcy + sin(aimRad) * cellW * 0.38f)
                        drawLine(steelPlate, center, bEnd, cellW * 0.16f, StrokeCap.Square)
                        drawLine(if (upgradeLevel >= 3) towerColor else Color.Black, center, bEnd, cellW * 0.08f, StrokeCap.Square)
                        drawCircle(steelDark, cellW * 0.16f, center)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else rivetColor, cellW * 0.08f, center)
                    }
                    TowerClass.ELEMENTAL_VOID -> {
                        val r = cellW * 0.44f
                        val p1 = Offset(tcx, tcy - r)
                        val p2 = Offset(tcx + r * 0.866f, tcy + r * 0.5f)
                        val p3 = Offset(tcx - r * 0.866f, tcy + r * 0.5f)
                        buildTrianglePath(p1, p2, p3, scratchPath1)
                        drawPath(scratchPath1, steelDark)
                        drawPath(scratchPath1, steelPlate, style = Stroke(width = 3.5f, join = StrokeJoin.Round))
                        drawCircle(rivetColor, 3.0f, p1)
                        drawCircle(rivetColor, 3.0f, p2)
                        drawCircle(rivetColor, 3.0f, p3)
                        if (upgradeLevel >= 2) drawCircle(vectorAccentColor, cellW * 0.18f, center, style = Stroke(width = 2.0f))
                        val bEnd = Offset(tcx + cos(aimRad) * cellW * 0.34f, tcy + sin(aimRad) * cellW * 0.34f)
                        drawLine(steelPlate, center, bEnd, cellW * 0.10f, StrokeCap.Square)
                        if (upgradeLevel >= 3) drawLine(towerColor, center, bEnd, cellW * 0.05f)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else Color.White, cellW * 0.09f, center)
                    }
                    TowerClass.HEAVY_SIEGE -> {
                        val s = cellW * 0.76f
                        val tl = Offset(tcx - s / 2f, tcy - s / 2f)
                        drawRoundRect(steelDark, tl, Size(s, s), CornerRadius(4f, 4f))
                        drawRoundRect(steelPlate, tl, Size(s, s), CornerRadius(4f, 4f), style = Stroke(width = 3.5f))
                        val cSize = cellW * 0.14f
                        drawFourCornerRects(tcx, tcy, s / 2f, cSize, steelPlate)
                        val rOff = s / 2f - cSize / 2f
                        drawCircle(rivetColor, 2.0f, Offset(tcx - rOff, tcy - rOff))
                        drawCircle(rivetColor, 2.0f, Offset(tcx + rOff, tcy - rOff))
                        drawCircle(rivetColor, 2.0f, Offset(tcx - rOff, tcy + rOff))
                        drawCircle(rivetColor, 2.0f, Offset(tcx + rOff, tcy + rOff))
                        drawDualRails(tcx, tcy, aimRad, cellW * 0.09f, cellW * 0.40f, steelPlate, cellW * 0.08f, StrokeCap.Square, if (upgradeLevel >= 2) vectorAccentColor else null, 3.0f)
                        if (upgradeLevel >= 3) drawCircle(towerColor, cellW * 0.14f, center, style = Stroke(width = 2.0f))
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else rivetColor, cellW * 0.08f, center)
                    }
                    TowerClass.STELLAR_COSMIC -> {
                        val starR = cellW * 0.40f
                        buildStarPath(tcx, tcy, starR, starR * 0.46f, aimRad, scratchPath1)
                        drawPath(scratchPath1, steelDark)
                        drawPath(scratchPath1, steelPlate, style = Stroke(width = 3.0f, join = StrokeJoin.Round))
                        if (upgradeLevel >= 2) drawCircle(vectorAccentColor, cellW * 0.18f, center, style = Stroke(width = 2.0f))
                        if (upgradeLevel >= 3) drawCircle(towerColor, cellW * 0.14f, center, style = Stroke(width = 2.0f))
                        drawLine(steelPlate, center, Offset(tcx + cos(aimRad) * cellW * 0.40f, tcy + sin(aimRad) * cellW * 0.40f), cellW * 0.08f, StrokeCap.Square)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else rivetColor, cellW * 0.08f, center)
                    }
                }
            }

            TurretSkinSlot.SLOT_4_VOID -> {
                val voidDark = Color(0xFF0D061A)
                val voidRift = Color(0xFFA855F7)
                val voidGlow = Color(0xFFD946EF)
                drawCircle(voidRift.copy(alpha = 0.25f * auraPulse), cellW * 0.42f, center, style = Stroke(width = 2.0f))

                when (effectiveClass) {
                    TowerClass.KINETIC_BALLISTIC -> {
                        drawCircle(voidDark, cellW * 0.38f, center)
                        drawCircle(voidRift, cellW * 0.38f, center, style = Stroke(width = 2.0f))
                        if (upgradeLevel >= 2) {
                            val sAngle = animFlowTime * 0.007f
                            drawCircle(vectorAccentColor, 3.5f, Offset(tcx + cos(sAngle) * cellW * 0.30f, tcy + sin(sAngle) * cellW * 0.30f))
                            drawCircle(vectorAccentColor, 3.5f, Offset(tcx - cos(sAngle) * cellW * 0.30f, tcy - sin(sAngle) * cellW * 0.30f))
                        }
                        val endPt = Offset(tcx + cos(aimRad) * cellW * 0.36f, tcy + sin(aimRad) * cellW * 0.36f)
                        drawLine(voidGlow, center, endPt, cellW * 0.10f, StrokeCap.Round)
                        if (upgradeLevel >= 3) drawLine(towerColor, center, endPt, cellW * 0.05f, StrokeCap.Round)
                        drawCircle(Color.White, cellW * 0.12f, center)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else voidRift, cellW * 0.07f, center)
                    }
                    TowerClass.ELEMENTAL_VOID -> {
                        val r = cellW * 0.44f
                        val p1 = Offset(tcx, tcy - r)
                        val p2 = Offset(tcx + r * 0.866f, tcy + r * 0.5f)
                        val p3 = Offset(tcx - r * 0.866f, tcy + r * 0.5f)
                        buildTrianglePath(p1, p2, p3, scratchPath1)
                        drawPath(scratchPath1, voidDark)
                        drawPath(scratchPath1, voidRift, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
                        drawCircle(voidGlow, cellW * 0.06f, p1)
                        drawCircle(voidGlow, cellW * 0.06f, p2)
                        drawCircle(voidGlow, cellW * 0.06f, p3)
                        if (upgradeLevel >= 2) drawCircle(vectorAccentColor, cellW * 0.20f, center, style = Stroke(width = 1.8f))
                        val bEnd = Offset(tcx + cos(aimRad) * cellW * 0.36f, tcy + sin(aimRad) * cellW * 0.36f)
                        drawLine(voidGlow, center, bEnd, cellW * 0.09f, StrokeCap.Round)
                        if (upgradeLevel >= 3) drawLine(towerColor, center, bEnd, cellW * 0.04f, StrokeCap.Round)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else Color.White, cellW * 0.09f, center)
                    }
                    TowerClass.HEAVY_SIEGE -> {
                        val s = cellW * 0.74f
                        val tl = Offset(tcx - s / 2f, tcy - s / 2f)
                        drawRoundRect(voidDark, tl, Size(s, s), CornerRadius(6f, 6f))
                        drawRoundRect(voidRift, tl, Size(s, s), CornerRadius(6f, 6f), style = Stroke(width = 2.0f))
                        scratchPath2.reset()
                        scratchPath2.moveTo(tcx, tcy - s * 0.35f)
                        scratchPath2.lineTo(tcx + s * 0.35f, tcy)
                        scratchPath2.lineTo(tcx, tcy + s * 0.35f)
                        scratchPath2.lineTo(tcx - s * 0.35f, tcy)
                        scratchPath2.close()
                        drawPath(scratchPath2, voidGlow.copy(alpha = 0.25f))
                        drawDualRails(tcx, tcy, aimRad, cellW * 0.08f, cellW * 0.38f, voidRift, cellW * 0.06f, StrokeCap.Round, if (upgradeLevel >= 2) vectorAccentColor else null, 2.5f)
                        if (upgradeLevel >= 3) drawCircle(towerColor, cellW * 0.12f, center, style = Stroke(width = 1.8f))
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else Color.White, cellW * 0.07f, center)
                    }
                    TowerClass.STELLAR_COSMIC -> {
                        val starR = cellW * 0.40f
                        buildStarPath(tcx, tcy, starR, starR * 0.48f, aimRad, scratchPath1)
                        drawPath(scratchPath1, voidDark)
                        drawPath(scratchPath1, voidRift, style = Stroke(width = 2.0f, join = StrokeJoin.Round))
                        if (upgradeLevel >= 2) drawCircle(vectorAccentColor.copy(alpha = 0.5f), cellW * 0.18f, center, style = Stroke(width = 1.5f))
                        if (upgradeLevel >= 3) drawCircle(towerColor, cellW * 0.12f, center, style = Stroke(width = 1.8f))
                        drawLine(voidGlow, center, Offset(tcx + cos(aimRad) * cellW * 0.38f, tcy + sin(aimRad) * cellW * 0.38f), cellW * 0.06f, StrokeCap.Round)
                        drawCircle(if (upgradeLevel == 4) apexAuraColor else Color.White, cellW * 0.07f, center)
                    }
                }
            }

            TurretSkinSlot.SLOT_5_PIXEL -> {
                val pixelDark = Color(0xFF0F172A)
                val pixelYellow = Color(0xFFFACC15)
                val pUnit = cellW * 0.06f
                drawFourCornerRects(tcx, tcy, cellW * 0.32f, pUnit, pixelYellow)

                when (effectiveClass) {
                    TowerClass.KINETIC_BALLISTIC -> {
                        drawCircle(pixelDark, cellW * 0.36f, center)
                        drawCircle(pixelYellow, cellW * 0.36f, center, style = Stroke(width = 2.0f))
                        if (upgradeLevel >= 2) {
                            val p = aimRad + (PI / 2f).toFloat()
                            val wOff = cellW * 0.28f
                            drawRect(vectorAccentColor, Offset(tcx + cos(p) * wOff - pUnit / 2f, tcy + sin(p) * wOff - pUnit / 2f), Size(pUnit, pUnit))
                            drawRect(vectorAccentColor, Offset(tcx - cos(p) * wOff - pUnit / 2f, tcy - sin(p) * wOff - pUnit / 2f), Size(pUnit, pUnit))
                        }
                        val endPt = Offset(tcx + cos(aimRad) * cellW * 0.34f, tcy + sin(aimRad) * cellW * 0.34f)
                        drawLine(pixelYellow, center, endPt, cellW * 0.12f, StrokeCap.Square)
                        if (upgradeLevel >= 3) drawLine(towerColor, center, endPt, cellW * 0.06f, StrokeCap.Square)
                        drawRect(if (upgradeLevel == 4) apexAuraColor else Color.White, Offset(tcx - pUnit, tcy - pUnit), Size(pUnit * 2f, pUnit * 2f))
                    }
                    TowerClass.ELEMENTAL_VOID -> {
                        val r = cellW * 0.42f
                        val p1 = Offset(tcx, tcy - r)
                        val p2 = Offset(tcx + r * 0.866f, tcy + r * 0.5f)
                        val p3 = Offset(tcx - r * 0.866f, tcy + r * 0.5f)
                        buildTrianglePath(p1, p2, p3, scratchPath1)
                        drawPath(scratchPath1, pixelDark)
                        drawPath(scratchPath1, pixelYellow, style = Stroke(width = 2.0f, join = StrokeJoin.Miter))
                        drawRect(pixelYellow, Offset(p1.x - pUnit / 2f, p1.y - pUnit / 2f), Size(pUnit, pUnit))
                        drawRect(pixelYellow, Offset(p2.x - pUnit / 2f, p2.y - pUnit / 2f), Size(pUnit, pUnit))
                        drawRect(pixelYellow, Offset(p3.x - pUnit / 2f, p3.y - pUnit / 2f), Size(pUnit, pUnit))
                        if (upgradeLevel >= 2) drawRect(vectorAccentColor, Offset(tcx - pUnit * 1.5f, tcy - pUnit * 1.5f), Size(pUnit * 3f, pUnit * 3f), style = Stroke(width = 1.5f))
                        val bEnd = Offset(tcx + cos(aimRad) * cellW * 0.34f, tcy + sin(aimRad) * cellW * 0.34f)
                        drawLine(pixelYellow, center, bEnd, cellW * 0.09f, StrokeCap.Square)
                        if (upgradeLevel >= 3) drawLine(towerColor, center, bEnd, cellW * 0.045f, StrokeCap.Square)
                        drawRect(if (upgradeLevel == 4) apexAuraColor else Color.White, Offset(tcx - pUnit * 0.8f, tcy - pUnit * 0.8f), Size(pUnit * 1.6f, pUnit * 1.6f))
                    }
                    TowerClass.HEAVY_SIEGE -> {
                        val s = cellW * 0.72f
                        val tl = Offset(tcx - s / 2f, tcy - s / 2f)
                        drawRect(pixelDark, tl, Size(s, s))
                        drawRect(pixelYellow, tl, Size(s, s), style = Stroke(width = 2.0f))
                        drawFourCornerRects(tcx, tcy, s / 2f, pUnit, pixelYellow)
                        drawDualRails(tcx, tcy, aimRad, cellW * 0.08f, cellW * 0.38f, pixelYellow, cellW * 0.08f, StrokeCap.Square, if (upgradeLevel >= 2) vectorAccentColor else null, 2.5f)
                        if (upgradeLevel >= 3) drawRect(towerColor, Offset(tcx - pUnit * 1.2f, tcy - pUnit * 1.2f), Size(pUnit * 2.4f, pUnit * 2.4f), style = Stroke(width = 1.5f))
                        drawRect(if (upgradeLevel == 4) apexAuraColor else Color.White, Offset(tcx - pUnit * 0.7f, tcy - pUnit * 0.7f), Size(pUnit * 1.4f, pUnit * 1.4f))
                    }
                    TowerClass.STELLAR_COSMIC -> {
                        val starR = cellW * 0.38f
                        buildStarPath(tcx, tcy, starR, starR * 0.45f, aimRad, scratchPath1)
                        drawPath(scratchPath1, pixelDark)
                        drawPath(scratchPath1, pixelYellow, style = Stroke(width = 2.0f))
                        if (upgradeLevel >= 2) drawRect(vectorAccentColor, Offset(tcx - pUnit, tcy - pUnit), Size(pUnit * 2f, pUnit * 2f), style = Stroke(width = 1.5f))
                        if (upgradeLevel >= 3) drawRect(towerColor, Offset(tcx - pUnit * 1.2f, tcy - pUnit * 1.2f), Size(pUnit * 2.4f, pUnit * 2.4f), style = Stroke(width = 1.5f))
                        drawLine(pixelYellow, center, Offset(tcx + cos(aimRad) * cellW * 0.36f, tcy + sin(aimRad) * cellW * 0.36f), cellW * 0.08f, StrokeCap.Square)
                        drawRect(if (upgradeLevel == 4) apexAuraColor else Color.White, Offset(tcx - pUnit * 0.7f, tcy - pUnit * 0.7f), Size(pUnit * 1.4f, pUnit * 1.4f))
                    }
                }
            }
        }

        // Tier Form Badge (Dynamic Level 1-4 Indicator)
        val badgeY = tcy + cellW * 0.28f
        val badgeColor = when (upgradeLevel) {
            4 -> apexAuraColor
            3 -> towerColor
            2 -> vectorAccentColor
            else -> Color(0xFF94A3B8)
        }
        when (effectiveClass) {
            TowerClass.KINETIC_BALLISTIC -> drawCircle(badgeColor, if (upgradeLevel >= 3) 3.5f else 2.5f, Offset(tcx, badgeY))
            TowerClass.ELEMENTAL_VOID -> {
                val bR = if (upgradeLevel >= 3) 4.5f else 3.5f
                buildTrianglePath(Offset(tcx, badgeY - bR), Offset(tcx + bR * 0.866f, badgeY + bR * 0.5f), Offset(tcx - bR * 0.866f, badgeY + bR * 0.5f), scratchPath1)
                drawPath(scratchPath1, badgeColor)
            }
            TowerClass.HEAVY_SIEGE -> {
                val bS = if (upgradeLevel >= 3) 6f else 5f
                drawRect(badgeColor, Offset(tcx - bS / 2f, badgeY - bS / 2f), Size(bS, bS))
            }
            TowerClass.STELLAR_COSMIC -> {
                val bR = if (upgradeLevel >= 3) 4.5f else 3.5f
                buildStarPath(tcx, badgeY, bR, bR * 0.5f, 0f, scratchPath1)
                drawPath(scratchPath1, badgeColor)
            }
        }
    }
}

/**
 * Renders combat projectiles, laser beams, lightning arcs, enemies, explosions, and floating combat text.
 */
fun DrawScope.drawCombatEntities(
    state: GameUIState,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float,
    scratchPath1: Path
) {
    for (i in 0 until state.laserBeams.size) {
        val beam = state.laserBeams[i]
        val bStart = Offset((beam.startX + 0.5f) * cellW + shakeX, (beam.startY + 0.5f) * cellH + shakeY)
        val bEnd = Offset((beam.endX + 0.5f) * cellW + shakeX, (beam.endY + 0.5f) * cellH + shakeY)
        drawLine(Color(beam.color).copy(alpha = 0.5f), bStart, bEnd, 6.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White, bStart, bEnd, 2.dp.toPx(), StrokeCap.Round)
    }

    for (idx in 0 until state.lightningArcs.size) {
        val arc = state.lightningArcs[idx]
        val arcColor = Color(arc.color).copy(alpha = 0.6f)
        for (i in 0 until arc.points.size - 1) {
            val p0 = arc.points[i]
            val p1 = arc.points[i + 1]
            val sx = (p0.first + 0.5f) * cellW + shakeX
            val sy = (p0.second + 0.5f) * cellH + shakeY
            val ex = (p1.first + 0.5f) * cellW + shakeX
            val ey = (p1.second + 0.5f) * cellH + shakeY
            val mid = Offset((sx + ex) / 2f + sin(i * 3.7f) * 10f, (sy + ey) / 2f + cos(i * 3.7f) * 10f)
            drawLine(arcColor, Offset(sx, sy), mid, 3.5.dp.toPx())
            drawLine(Color.White, Offset(sx, sy), mid, 1.5.dp.toPx())
            drawLine(arcColor, mid, Offset(ex, ey), 3.5.dp.toPx())
            drawLine(Color.White, mid, Offset(ex, ey), 1.5.dp.toPx())
        }
    }

    for (i in 0 until state.projectiles.size) {
        val proj = state.projectiles[i]
        val px = (proj.currentX + 0.5f) * cellW + shakeX
        val py = (proj.currentY + 0.5f) * cellH + shakeY
        val projColor = Color(proj.color)
        if (proj.isMortarArc) {
            val progress = (proj.traveledDistance / proj.totalDistance).coerceIn(0f, 1f)
            val arcY = py - sin(progress * PI.toFloat()) * (cellH * 1.3f)
            drawCircle(Color.Black.copy(alpha = 0.35f), cellW * 0.12f, Offset(px, py))
            drawCircle(projColor, cellW * 0.16f, Offset(px, arcY))
            drawCircle(Color.White, cellW * 0.08f, Offset(px, arcY))
        } else {
            drawCircle(projColor.copy(alpha = 0.6f), cellW * 0.11f, Offset(px, py))
            drawCircle(Color.White, cellW * 0.06f, Offset(px, py))
        }
    }

    for (idx in 0 until state.enemies.size) {
        val enemy = state.enemies[idx]
        val ecx = (enemy.worldX + 0.5f) * cellW + shakeX
        val ecy = (enemy.worldY + 0.5f) * cellH + shakeY
        val center = Offset(ecx, ecy)
        val enemyColor = Color(enemy.type.color)
        val affinityColor = Color(enemy.type.elementAffinity.colorHex)
        val enemyRadius = cellW * 0.28f * enemy.type.sizeRatio
        val fillColor = if (enemy.isBoss) Color(0xFF581C87) else enemyColor
        val strokeW = if (enemy.isBoss) 3.5f else 2.2f

        if (enemy.slowTimerMs > 0) drawCircle(Color(0xFF38BDF8).copy(alpha = 0.4f), enemyRadius * 1.4f, center)
        if (enemy.shockTimerMs > 0) drawCircle(Color(0xFFB026FF).copy(alpha = 0.4f), enemyRadius * 1.35f, center, style = Stroke(width = 2f))

        when (enemy.type.shape) {
            EnemyShape.CIRCLE -> {
                if (enemy.isBoss) {
                    drawCircle(fillColor, enemyRadius * 1.35f, center)
                    drawCircle(affinityColor, enemyRadius * 1.15f, center, style = Stroke(width = 3.0f))
                    drawCircle(enemyColor, enemyRadius, center)
                } else {
                    drawCircle(fillColor, enemyRadius, center)
                    drawCircle(affinityColor, enemyRadius, center, style = Stroke(width = strokeW))
                }
            }
            EnemyShape.TRIANGLE -> {
                val triR = enemyRadius * 1.25f
                val h = enemy.headingRad
                val cosH = cos(h)
                val sinH = sin(h)
                val pCos = cos(h + (PI / 2f).toFloat())
                val pSin = sin(h + (PI / 2f).toFloat())
                val bx = ecx - cosH * (triR * 0.65f)
                val by = ecy - sinH * (triR * 0.65f)
                val hb = triR * 0.85f
                buildTrianglePath(Offset(ecx + cosH * triR, ecy + sinH * triR), Offset(bx + pCos * hb, by + pSin * hb), Offset(bx - pCos * hb, by - pSin * hb), scratchPath1)
                drawPath(scratchPath1, fillColor)
                drawPath(scratchPath1, affinityColor, style = Stroke(width = strokeW, join = StrokeJoin.Round))
            }
            EnemyShape.SQUARE -> {
                val sqR = enemyRadius * 0.95f
                val sqTl = Offset(ecx - sqR, ecy - sqR)
                val sqSz = Size(sqR * 2f, sqR * 2f)
                drawRoundRect(fillColor, sqTl, sqSz, CornerRadius(4f, 4f))
                drawRoundRect(affinityColor, sqTl, sqSz, CornerRadius(4f, 4f), style = Stroke(width = strokeW))
            }
            EnemyShape.STAR -> {
                val starR = enemyRadius * 1.30f
                buildStarPath(ecx, ecy, starR, starR * 0.48f, enemy.headingRad, scratchPath1)
                drawPath(scratchPath1, fillColor)
                drawPath(scratchPath1, affinityColor, style = Stroke(width = strokeW, join = StrokeJoin.Round))
            }
        }
        if (enemy.isBoss) {
            drawCircle(Color.White, enemyRadius * 0.35f, center)
        } else {
            drawCircle(Color.Black.copy(alpha = 0.45f), enemyRadius * 0.4f, center)
            drawCircle(affinityColor, enemyRadius * 0.2f, center)
        }

        val hpBarW = cellW * 0.70f * enemy.type.sizeRatio
        val hpBarH = 4.dp.toPx()
        val hpBarY = ecy - enemyRadius - 7.dp.toPx()
        val hpRatio = (enemy.currentHp / enemy.maxHp).coerceIn(0f, 1f)
        drawRoundRect(Color.Black.copy(alpha = 0.75f), Offset(ecx - hpBarW / 2, hpBarY), Size(hpBarW, hpBarH), CornerRadius(2f, 2f))
        val hpColor = when {
            hpRatio > 0.5f -> Color(0xFF00FF9D)
            hpRatio > 0.25f -> Color(0xFFFFB800)
            else -> Color(0xFFFF2A55)
        }
        drawRoundRect(hpColor, Offset(ecx - hpBarW / 2, hpBarY), Size(hpBarW * hpRatio, hpBarH), CornerRadius(2f, 2f))
    }

    for (i in 0 until state.explosions.size) {
        val exp = state.explosions[i]
        val center = Offset((exp.x + 0.5f) * cellW + shakeX, (exp.y + 0.5f) * cellH + shakeY)
        val radiusPx = exp.currentRadius * cellW
        val alpha = (exp.lifeMs.toFloat() / exp.maxLifeMs).coerceIn(0f, 1f)
        drawCircle(Color(exp.color).copy(alpha = 0.28f * alpha), radiusPx, center)
        drawCircle(Color(exp.color).copy(alpha = 0.8f * alpha), radiusPx, center, style = Stroke(width = 3f))
    }

    for (i in 0 until state.floatingTexts.size) {
        val ft = state.floatingTexts[i]
        val alpha = (ft.lifeMs.toFloat() / ft.maxLifeMs).coerceIn(0f, 1f)
        drawCircle(
            Color(ft.color).copy(alpha = 0.25f * alpha),
            if (ft.isCrit) 16.dp.toPx() else 10.dp.toPx(),
            Offset((ft.x + 0.5f) * cellW + shakeX, (ft.y + 0.5f) * cellH + shakeY)
        )
    }
}
