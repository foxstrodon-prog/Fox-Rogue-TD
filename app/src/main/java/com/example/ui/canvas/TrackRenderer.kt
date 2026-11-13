package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.GeneratedLevel
import com.example.model.GridPos
import com.example.model.TrackParticle
import kotlin.math.abs

/**
 * Renders dual-front lane pathways, merged radiant convergence segments, and flowing track particles
 * with zero per-frame heap allocations.
 */
fun DrawScope.drawSmoothMergedPathways(
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
    lanePathA: Path,
    lanePathB: Path,
    mergedPath: Path
) {
    if (laneAlpha.size < 2 || laneBeta.size < 2) return

    fun buildPath(points: List<GridPos>, startIdx: Int, endExclusive: Int, outPath: Path): Path {
        outPath.reset()
        val first = points[startIdx]
        outPath.moveTo((first.x + 0.5f) * cellW + shakeX, (first.y + 0.5f) * cellH + shakeY)
        for (i in (startIdx + 1) until endExclusive) {
            val pt = points[i]
            outPath.lineTo((pt.x + 0.5f) * cellW + shakeX, (pt.y + 0.5f) * cellH + shakeY)
        }
        return outPath
    }

    val pathA = buildPath(laneAlpha, 0, laneAlpha.size, lanePathA)
    val pathB = buildPath(laneBeta, 0, laneBeta.size, lanePathB)

    // 1. Draw Asphalt/Terrain Underbed (smooth rounded curves)
    drawPath(path = pathA, color = pathColor, style = Stroke(width = cellW * 0.72f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = pathB, color = pathColor, style = Stroke(width = cellW * 0.72f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 2. Draw Clean Glowing Conduit Lines
    drawPath(path = pathA, color = colorAlpha.copy(alpha = 0.45f), style = Stroke(width = cellW * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = pathB, color = colorBeta.copy(alpha = 0.45f), style = Stroke(width = cellW * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 3. Highlight the Merged Shared Segment with Radiant Gradient Shift (zero-allocation index scan)
    val lastPt = laneAlpha.last()
    var segStart = -1
    var segLen = 0

    fun flushSegment(startIdx: Int, length: Int) {
        if (length >= 2) {
            val segPath = buildPath(laneAlpha, startIdx, startIdx + length, mergedPath)
            drawPath(
                path = segPath,
                color = colorMerged.copy(alpha = 0.75f),
                style = Stroke(width = cellW * 0.30f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = segPath,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = cellW * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    for (i in 0 until laneAlpha.size) {
        val pt = laneAlpha[i]
        if (pt in intersections && pt != lastPt) {
            if (segLen == 0) {
                segStart = i
                segLen = 1
            } else {
                val prev = laneAlpha[i - 1]
                val isAdjacent = (abs(pt.x - prev.x) + abs(pt.y - prev.y)) == 1
                if (isAdjacent) {
                    segLen++
                } else {
                    flushSegment(segStart, segLen)
                    segStart = i
                    segLen = 1
                }
            }
        } else {
            flushSegment(segStart, segLen)
            segStart = -1
            segLen = 0
        }
    }
    flushSegment(segStart, segLen)
}

/**
 * Renders dynamic track sparks flowing along both enemy approach lanes.
 */
fun DrawScope.drawTrackParticles(
    trackParticles: List<TrackParticle>,
    level: GeneratedLevel,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float
) {
    for (i in 0 until trackParticles.size) {
        val tp = trackParticles[i]
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
}
