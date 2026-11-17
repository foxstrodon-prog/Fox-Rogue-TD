package com.example.ui.menu

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.BuildArchiveCatalog
import com.example.audio.BuildArchiveVisualCatalog
import com.example.audio.CURRENT_BUILD_CODE
import com.example.audio.MusicTrack
import com.example.audio.SoundProfile
import com.example.model.MapThemeDef
import com.example.model.TowerClass
import com.example.model.TowerDrawStyle
import com.example.model.TurretSkinDef
import com.example.model.TurretSkinSlot
import com.example.ui.GameUIState
import com.example.ui.canvas.buildHexagonPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val PREVIEW_STROKE_1_2 = Stroke(width = 1.2f)
private val PREVIEW_STROKE_2_2 = Stroke(width = 2.2f, join = StrokeJoin.Round)
private val PREVIEW_STROKE_2_8 = Stroke(width = 2.8f, join = StrokeJoin.Round)

/**
 * Data-driven visual thumbnail preview for custom turret vector skins (active & archived).
 */
@Composable
fun TurretSkinPreviewThumbnail(
    skinDef: TurretSkinDef,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(36.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.width * 0.42f
        val hullCol = Color(skinDef.hullColor)
        val frameCol = Color(skinDef.frameColor)
        val detailCol = Color(skinDef.detailColor)
        val accentCol = Color(if (skinDef.accentTint != 0L) skinDef.accentTint else skinDef.frameColor)

        drawCircle(color = hullCol, radius = radius, center = Offset(cx, cy))
        drawCircle(
            color = if (isSelected) Color(0xFF00FF9D) else frameCol,
            radius = radius * 0.85f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = if (isSelected) Color(0xFF00F0FF) else accentCol,
            radius = radius * 0.35f,
            center = Offset(cx, cy)
        )

        if (skinDef.drawStyle == TowerDrawStyle.NEON_GLOW) {
            drawCircle(
                color = accentCol.copy(alpha = 0.7f),
                radius = radius * 0.58f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        if (skinDef.hasCrosshair || skinDef.drawStyle == TowerDrawStyle.BLUEPRINT_LINE) {
            val ch = radius * 0.75f
            drawLine(detailCol.copy(alpha = 0.85f), Offset(cx - ch, cy), Offset(cx + ch, cy), 1.dp.toPx())
            drawLine(detailCol.copy(alpha = 0.85f), Offset(cx, cy - ch), Offset(cx, cy + ch), 1.dp.toPx())
            drawCircle(frameCol.copy(alpha = 0.65f), radius * 0.95f, Offset(cx, cy), style = Stroke(1.dp.toPx()))
        }
        if (skinDef.hasCornerStuds || skinDef.drawStyle == TowerDrawStyle.SOLID_MILITARY) {
            val rOff = radius * 0.55f
            val rRad = 2.dp.toPx()
            val innerRad = rRad * 0.5f
            val corners = arrayOf(
                Offset(cx - rOff, cy - rOff),
                Offset(cx + rOff, cy - rOff),
                Offset(cx - rOff, cy + rOff),
                Offset(cx + rOff, cy + rOff)
            )
            for (pt in corners) {
                drawCircle(color = frameCol, radius = rRad, center = pt)
                drawCircle(color = detailCol, radius = innerRad, center = pt)
            }
        }
        if (skinDef.hasVoidHalo || skinDef.drawStyle == TowerDrawStyle.VOID_RING) {
            drawCircle(frameCol.copy(alpha = 0.85f), radius * 0.95f, Offset(cx, cy), style = Stroke(1.8.dp.toPx()))
            drawCircle(detailCol.copy(alpha = 0.5f), radius * 0.65f, Offset(cx, cy), style = Stroke(1.dp.toPx()))
        }
        if (skinDef.hasPixelCorners || skinDef.drawStyle == TowerDrawStyle.PIXEL_BLOCK) {
            val pSize = 3.5.dp.toPx()
            val bOff = radius * 0.68f
            val sz = Size(pSize, pSize)
            drawRect(color = detailCol, topLeft = Offset(cx - bOff, cy - bOff), size = sz)
            drawRect(color = detailCol, topLeft = Offset(cx + bOff - pSize, cy - bOff), size = sz)
            drawRect(color = detailCol, topLeft = Offset(cx - bOff, cy + bOff - pSize), size = sz)
            drawRect(color = detailCol, topLeft = Offset(cx + bOff - pSize, cy + bOff - pSize), size = sz)
        }
    }
}

@Composable
fun TurretSkinPreviewThumbnail(
    skinSlot: TurretSkinSlot,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    TurretSkinPreviewThumbnail(
        skinDef = BuildArchiveCatalog.resolveTurretSkin(null, skinSlot.slot),
        isSelected = isSelected,
        modifier = modifier
    )
}

/**
 * Interactive multi-class live preview stage rendering all 4 tower classes
 * (Kinetic Circle, Elemental Triangle, Heavy Hexagon, Stellar Star) using [skinDef].
 * Uses cached Skia Paths for zero per-frame DrawScope allocations.
 */
@Composable
internal fun TurretSkinMultiClassPreviewStage(
    skinDef: TurretSkinDef,
    isEquipped: Boolean,
    onEquipClick: (() -> Unit)? = null,
    animateAuraPulse: Boolean = true,
    modifier: Modifier = Modifier
) {
    val auraPulse = if (animateAuraPulse) {
        val transition = rememberInfiniteTransition(label = "skin_preview_pulse")
        transition.animateFloat(
            initialValue = 0.86f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aura_pulse"
        ).value
    } else {
        1.0f
    }
    val hullCol = Color(skinDef.hullColor)
    val frameCol = Color(skinDef.frameColor)
    val detailCol = Color(skinDef.detailColor)
    val accentCol = Color(if (skinDef.accentTint != 0L) skinDef.accentTint else skinDef.frameColor)

    Surface(
        color = Color(0xFF090F1B),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.2.dp, if (isEquipped) Color(0xFF00FF9D) else accentCol.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("turret_skin_live_preview_stage")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "PREVIEW: ${skinDef.displayName}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${skinDef.drawStyle.name.replace('_', ' ')} • ${skinDef.buildLabel}",
                        color = accentCol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isEquipped) {
                    Surface(
                        color = Color(0xFF00FF9D).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF00FF9D))
                    ) {
                        Text(
                            text = "EQUIPPED",
                            color = Color(0xFF00FF9D),
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else if (onEquipClick != null) {
                    Button(
                        onClick = onEquipClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(24.dp)
                            .testTag("turret_skin_preview_equip_btn")
                    ) {
                        Text("EQUIP SKIN", fontWeight = FontWeight.Black, fontSize = 8.5.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TowerClass.entries.forEach { towerClass ->
                    val classTint = Color(skinDef.resolveClassTint(towerClass, towerClass.color))
                    val path1 = remember { Path() }
                    val path2 = remember { Path() }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, classTint.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                            .testTag("skin_preview_class_${towerClass.name}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Canvas(modifier = Modifier.size(44.dp)) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val r = size.width * 0.36f
                            val center = Offset(cx, cy)
                            val aimRad = -0.55f
                            val borderColor = if (skinDef.overrideClassTint) classTint else frameCol

                            // Tier-4 aura pulse preview
                            val auraR = r * 1.24f * auraPulse
                            when (towerClass) {
                                TowerClass.KINETIC_BALLISTIC -> {
                                    drawCircle(classTint.copy(alpha = 0.22f * auraPulse), auraR, center)
                                    drawCircle(classTint.copy(alpha = 0.72f), auraR * 0.94f, center, style = PREVIEW_STROKE_1_2)
                                }
                                TowerClass.ELEMENTAL_VOID -> {
                                    path1.reset()
                                    path1.moveTo(cx, cy - auraR)
                                    path1.lineTo(cx + auraR * 0.866f, cy + auraR * 0.5f)
                                    path1.lineTo(cx - auraR * 0.866f, cy + auraR * 0.5f)
                                    path1.close()
                                    drawPath(path1, classTint.copy(alpha = 0.20f * auraPulse))
                                    drawPath(path1, classTint.copy(alpha = 0.72f), style = PREVIEW_STROKE_1_2)
                                }
                                TowerClass.HEAVY_SIEGE -> {
                                    buildHexagonPath(cx, cy, auraR, 0f, path1)
                                    drawPath(path1, classTint.copy(alpha = 0.20f * auraPulse))
                                    drawPath(path1, classTint.copy(alpha = 0.72f), style = PREVIEW_STROKE_1_2)
                                }
                                TowerClass.STELLAR_COSMIC -> {
                                    path1.reset()
                                    for (i in 0 until 10) {
                                        val sr = if (i % 2 == 0) auraR else auraR * 0.48f
                                        val a = (i * PI / 5f - PI / 2f).toFloat()
                                        val px = cx + cos(a) * sr
                                        val py = cy + sin(a) * sr
                                        if (i == 0) path1.moveTo(px, py) else path1.lineTo(px, py)
                                    }
                                    path1.close()
                                    drawPath(path1, classTint.copy(alpha = 0.22f * auraPulse))
                                    drawPath(path1, classTint.copy(alpha = 0.72f), style = PREVIEW_STROKE_1_2)
                                }
                            }

                            // Style overlays (crosshair / void halo / pixel corners)
                            if (skinDef.hasVoidHalo || skinDef.drawStyle == TowerDrawStyle.VOID_RING) {
                                drawCircle(frameCol.copy(alpha = 0.40f), r * 1.12f, center, style = PREVIEW_STROKE_1_2)
                            }
                            if (skinDef.hasCrosshair || skinDef.drawStyle == TowerDrawStyle.BLUEPRINT_LINE) {
                                val ch = r * 1.05f
                                drawLine(frameCol.copy(alpha = 0.45f), Offset(cx - ch, cy), Offset(cx + ch, cy), 1f)
                                drawLine(frameCol.copy(alpha = 0.45f), Offset(cx, cy - ch), Offset(cx, cy + ch), 1f)
                            }

                            val strokeStyle = if (skinDef.drawStyle == TowerDrawStyle.SOLID_MILITARY) PREVIEW_STROKE_2_8 else PREVIEW_STROKE_2_2
                            when (towerClass) {
                                TowerClass.KINETIC_BALLISTIC -> {
                                    drawCircle(hullCol, r, center)
                                    drawCircle(borderColor, r, center, style = strokeStyle)
                                    drawCircle(detailCol.copy(alpha = 0.45f), r * 0.65f, center, style = PREVIEW_STROKE_1_2)
                                    val end = Offset(cx + cos(aimRad) * r * 0.95f, cy + sin(aimRad) * r * 0.95f)
                                    drawLine(classTint, center, end, r * 0.28f, StrokeCap.Round)
                                    drawCircle(detailCol, r * 0.32f, center)
                                    drawCircle(classTint, r * 0.16f, center)
                                }
                                TowerClass.ELEMENTAL_VOID -> {
                                    val tr = r * 1.08f
                                    path1.reset()
                                    path1.moveTo(cx, cy - tr)
                                    path1.lineTo(cx + tr * 0.866f, cy + tr * 0.5f)
                                    path1.lineTo(cx - tr * 0.866f, cy + tr * 0.5f)
                                    path1.close()
                                    drawPath(path1, hullCol)
                                    drawPath(path1, borderColor, style = strokeStyle)
                                    val inR = tr * 0.52f
                                    path2.reset()
                                    path2.moveTo(cx, cy + inR)
                                    path2.lineTo(cx - inR * 0.866f, cy - inR * 0.5f)
                                    path2.lineTo(cx + inR * 0.866f, cy - inR * 0.5f)
                                    path2.close()
                                    drawPath(path2, detailCol.copy(alpha = 0.35f), style = PREVIEW_STROKE_1_2)
                                    val end = Offset(cx + cos(aimRad) * r * 0.88f, cy + sin(aimRad) * r * 0.88f)
                                    drawLine(classTint, center, end, r * 0.22f, StrokeCap.Round)
                                    drawCircle(detailCol, r * 0.24f, center)
                                }
                                TowerClass.HEAVY_SIEGE -> {
                                    val hr = r * 1.04f
                                    buildHexagonPath(cx, cy, hr, 0f, path1)
                                    drawPath(path1, hullCol)
                                    drawPath(path1, borderColor, style = strokeStyle)
                                    buildHexagonPath(cx, cy, hr * 0.62f, 0f, path2)
                                    drawPath(path2, detailCol.copy(alpha = 0.40f), style = PREVIEW_STROKE_1_2)
                                    val perp = aimRad + (PI / 2f).toFloat()
                                    val spread = r * 0.22f
                                    val dx = cos(aimRad) * r * 0.95f
                                    val dy = sin(aimRad) * r * 0.95f
                                    val r1 = Offset(cx + cos(perp) * spread, cy + sin(perp) * spread)
                                    val r2 = Offset(cx - cos(perp) * spread, cy - sin(perp) * spread)
                                    drawLine(classTint, r1, Offset(r1.x + dx, r1.y + dy), r * 0.16f, StrokeCap.Round)
                                    drawLine(classTint, r2, Offset(r2.x + dx, r2.y + dy), r * 0.16f, StrokeCap.Round)
                                    drawCircle(detailCol, r * 0.22f, center)
                                }
                                TowerClass.STELLAR_COSMIC -> {
                                    val sr = r * 1.05f
                                    path1.reset()
                                    for (i in 0 until 10) {
                                        val rad = if (i % 2 == 0) sr else sr * 0.48f
                                        val a = (i * PI / 5f - PI / 2f).toFloat() + aimRad
                                        val px = cx + cos(a) * rad
                                        val py = cy + sin(a) * rad
                                        if (i == 0) path1.moveTo(px, py) else path1.lineTo(px, py)
                                    }
                                    path1.close()
                                    drawPath(path1, hullCol)
                                    drawPath(path1, borderColor, style = strokeStyle)
                                    val end = Offset(cx + cos(aimRad) * r * 0.96f, cy + sin(aimRad) * r * 0.96f)
                                    drawLine(detailCol, center, end, r * 0.14f, StrokeCap.Round)
                                    drawCircle(Color(0xFFFFD700), r * 0.25f, center)
                                }
                            }

                            if (skinDef.hasCornerStuds || skinDef.drawStyle == TowerDrawStyle.SOLID_MILITARY) {
                                val off = r * 0.62f
                                drawCircle(detailCol, 1.8f, Offset(cx - off, cy - off))
                                drawCircle(detailCol, 1.8f, Offset(cx + off, cy - off))
                                drawCircle(detailCol, 1.8f, Offset(cx - off, cy + off))
                                drawCircle(detailCol, 1.8f, Offset(cx + off, cy + off))
                            }
                            if (skinDef.hasPixelCorners || skinDef.drawStyle == TowerDrawStyle.PIXEL_BLOCK) {
                                val u = 3.2f
                                val off = r * 0.78f
                                val sz = Size(u, u)
                                drawRect(detailCol, Offset(cx - off, cy - off), sz)
                                drawRect(detailCol, Offset(cx + off - u, cy - off), sz)
                                drawRect(detailCol, Offset(cx - off, cy + off - u), sz)
                                drawRect(detailCol, Offset(cx + off - u, cy + off - u), sz)
                            }
                        }
                        Text(
                            text = "${towerClass.shapeSymbol} ${towerClass.displayName.substringBefore(' ')}",
                            color = classTint,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact dual-lane swatch thumbnail for Map & Lane Environment themes.
 */
@Composable
fun MapThemePreviewThumbnail(
    themeDef: MapThemeDef,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val lanePath = remember { Path() }
    val groundCol = Color(themeDef.groundColor)
    val gridCol = Color(themeDef.gridLineColor).copy(alpha = themeDef.gridLineAlpha.coerceIn(0.35f, 1f))
    val alphaCol = Color(themeDef.laneAlphaColor)
    val betaCol = Color(themeDef.laneBetaColor)
    val mergedCol = Color(themeDef.laneMergedColor)
    val borderCol = if (isSelected) Color(0xFFFFB800) else Color(0xFF475569)

    Canvas(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(groundCol)
            .border(1.5.dp, borderCol, RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height
        for (i in 1..2) {
            drawLine(gridCol, Offset(w * i / 3f, 0f), Offset(w * i / 3f, h), 1f)
            drawLine(gridCol, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), 1f)
        }
        lanePath.reset()
        lanePath.moveTo(w * 0.14f, h * 0.24f)
        lanePath.lineTo(w * 0.50f, h * 0.50f)
        drawPath(lanePath, alphaCol, style = PREVIEW_STROKE_2_2)
        lanePath.reset()
        lanePath.moveTo(w * 0.14f, h * 0.76f)
        lanePath.lineTo(w * 0.50f, h * 0.50f)
        drawPath(lanePath, betaCol, style = PREVIEW_STROKE_2_2)
        drawLine(mergedCol, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.88f, h * 0.50f), 2.8f, StrokeCap.Round)
        drawCircle(mergedCol, w * 0.08f, Offset(w * 0.86f, h * 0.50f))
    }
}

/**
 * Interactive mini dual-lane board preview showing ground, grid lines, blank glowing pad,
 * Lane Alpha, Lane Beta, Merged Lane, Spawners, and Core for [themeDef].
 */
@Composable
internal fun MapThemeLivePreviewStage(
    themeDef: MapThemeDef,
    isEquipped: Boolean,
    onEquipClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val groundCol = Color(themeDef.groundColor)
    val gridCol = Color(themeDef.gridLineColor).copy(alpha = themeDef.gridLineAlpha.coerceIn(0.35f, 1f))
    val pathBaseCol = Color(themeDef.pathBaseColor)
    val alphaCol = Color(themeDef.laneAlphaColor)
    val betaCol = Color(themeDef.laneBetaColor)
    val mergedCol = Color(themeDef.laneMergedColor)
    val pathAlpha = remember { Path() }
    val pathBeta = remember { Path() }

    Surface(
        color = Color(0xFF090F1B),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.2.dp, if (isEquipped) Color(0xFFFFB800) else mergedCol.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_theme_live_preview_stage")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "PREVIEW: ${themeDef.displayName}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${themeDef.mapStyle.name.replace('_', ' ')} • ${themeDef.buildLabel}",
                        color = mergedCol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isEquipped) {
                    Surface(
                        color = Color(0xFFFFB800).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB800))
                    ) {
                        Text(
                            text = "EQUIPPED",
                            color = Color(0xFFFFB800),
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else if (onEquipClick != null) {
                    Button(
                        onClick = onEquipClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800), contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(24.dp)
                            .testTag("map_theme_preview_equip_btn")
                    ) {
                        Text("EQUIP THEME", fontWeight = FontWeight.Black, fontSize = 8.5.sp)
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(groundCol)
                    .border(1.dp, gridCol, RoundedCornerShape(8.dp))
            ) {
                val w = size.width
                val h = size.height
                val cols = 10
                val rows = 4
                for (c in 1 until cols) {
                    val x = w * c / cols
                    drawLine(gridCol, Offset(x, 0f), Offset(x, h), 1f)
                }
                for (r in 1 until rows) {
                    val y = h * r / rows
                    drawLine(gridCol, Offset(0f, y), Offset(w, y), 1f)
                }

                // Glowing blank buildable spot preview
                val cellW = w / cols
                val cellH = h / rows
                val padX = cellW * 4f + 2f
                val padY = 2f
                drawRoundRect(
                    color = Color(0xFF7DD3FC).copy(alpha = 0.65f),
                    topLeft = Offset(padX, padY),
                    size = Size(cellW - 4f, cellH - 4f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = PREVIEW_STROKE_1_2
                )
                drawCircle(Color.White.copy(alpha = 0.45f), 2f, Offset(padX + (cellW - 4f) / 2f, padY + (cellH - 4f) / 2f))

                // Dual lanes merging into Core
                val mergePt = Offset(w * 0.55f, h * 0.50f)
                val corePt = Offset(w * 0.90f, h * 0.50f)
                val spawnA = Offset(w * 0.10f, h * 0.22f)
                val spawnB = Offset(w * 0.10f, h * 0.78f)

                pathAlpha.reset()
                pathAlpha.moveTo(spawnA.x, spawnA.y)
                pathAlpha.lineTo(w * 0.34f, spawnA.y)
                pathAlpha.lineTo(mergePt.x, mergePt.y)

                pathBeta.reset()
                pathBeta.moveTo(spawnB.x, spawnB.y)
                pathBeta.lineTo(w * 0.34f, spawnB.y)
                pathBeta.lineTo(mergePt.x, mergePt.y)

                drawPath(pathAlpha, pathBaseCol, style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(pathBeta, pathBaseCol, style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(pathBaseCol, mergePt, corePt, 12f, StrokeCap.Round)

                drawPath(pathAlpha, alphaCol, style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(pathBeta, betaCol, style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(mergedCol, mergePt, corePt, 4.5f, StrokeCap.Round)

                drawCircle(alphaCol, 5.5f, spawnA)
                drawCircle(betaCol, 5.5f, spawnB)
                drawCircle(mergedCol.copy(alpha = 0.35f), 8f, mergePt)
                drawCircle(Color(0xFF00FF9D), 6.5f, corePt)
                drawCircle(Color.White, 2.5f, corePt)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeLegendChip("Alpha", alphaCol)
                ThemeLegendChip("Beta", betaCol)
                ThemeLegendChip("Merged", mergedCol)
                ThemeLegendChip("Pad", Color(0xFF7DD3FC))
            }
        }
    }
}

@Composable
private fun ThemeLegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, color = Color(0xFFCBD5E1), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun AudioVisualCustomizationPanel(
    state: GameUIState,
    onMasterVolumeChanged: (Float) -> Unit,
    onMusicVolumeChanged: (Float) -> Unit,
    onSfxVolumeChanged: (Float) -> Unit,
    onUpgradeVolumeChanged: (Float) -> Unit,
    onToggleAudioMute: () -> Unit,
    onSoundProfileChanged: (SoundProfile) -> Unit,
    onMusicTrackSelected: (MusicTrack) -> Unit,
    onBgmAssetSelected: (String) -> Unit = {},
    onKineticSfxSlotSelected: (Int) -> Unit,
    onElementalSfxSlotSelected: (Int) -> Unit,
    onSiegeSfxSlotSelected: (Int) -> Unit,
    onStellarSfxSlotSelected: (Int) -> Unit = {},
    onPlayTurretTestSound: (TowerClass, Int) -> Unit,
    onPlayTowerUpgradeTestSound: (TowerClass, Int) -> Unit,
    onTurretSkinSlotSelected: (Int) -> Unit,
    onMapThemeSlotSelected: (Int) -> Unit,
    onOpenAssetVault: () -> Unit = {},
    animateSkinPreviewPulse: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(
            onClick = onOpenAssetVault,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
            border = BorderStroke(1.2.dp, Color(0xFF00E5FF)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Icon(imageVector = Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("OPEN SOUND & SKIN VAULT (ARCHIVES & VOTES)", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
        }

        // Section 1: Audio Channels & Attenuation
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AUDIO MIXER & BUS VOLUMES", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Button(
                        onClick = onToggleAudioMute,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isAudioMuted) Color(0xFFFF2A55) else Color(0xFF334155),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isAudioMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (state.isAudioMuted) "MUTED" else "UNMUTED", fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                    }
                }

                VolumeSliderRow("Master Output", state.masterVolume, Color(0xFF00F0FF), Color(0xFF94A3B8), onMasterVolumeChanged)
                VolumeSliderRow("Music Volume (Live -50% Pause Level)", state.musicVolume, Color(0xFFC084FC), Color(0xFFC084FC), onMusicVolumeChanged)
                VolumeSliderRow("Combat Sound Effects", state.sfxVolume, Color(0xFFFFB800), Color(0xFFFFB800), onSfxVolumeChanged)
                VolumeSliderRow("Upgrade Sound Effects", state.upgradeVolume, Color(0xFF00FF9D), Color(0xFF00FF9D), onUpgradeVolumeChanged)
            }
        }

        // Section 2: Music Playlist & Track Selector (Build #61 + Rolling + Default)
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MUSIC STREAMING & PLAYLIST (BUILD #$CURRENT_BUILD_CODE)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("Dynamic combat ducking automatically drops BGM during turret firing.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                BuildArchiveCatalog.activeBgmTracks.forEach { trackDef ->
                    val isSel = (state.musicTrack != MusicTrack.NONE && state.bgmTrackId == trackDef.id)
                    val label = if (trackDef.isRollingPlaylist) "🔁 ${trackDef.displayName}" else trackDef.displayName
                    MusicOptionRow(
                        label = label,
                        isSelected = isSel,
                        showPreviewingBadge = isSel,
                        onClick = { onBgmAssetSelected(trackDef.id) }
                    )
                }
                val isMutedTrack = (state.musicTrack == MusicTrack.NONE)
                MusicOptionRow(
                    label = "Mute Music Track",
                    isSelected = isMutedTrack,
                    showPreviewingBadge = false,
                    onClick = { onMusicTrackSelected(MusicTrack.NONE) }
                )
            }
        }

        // Section 3: Turret Gunfire Audio Variants (4 Classes x 5 Slots)
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TURRET GUNFIRE AUDIO TESTING (4 CLASSES × 5 SLOTS)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("A/B test gunfire audio profiles across all 4 turret archetypes. Tap any slot to test immediately.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                TurretGunfireSlotSection(
                    title = "Kinetic Ballistics (Rotary)",
                    accentColor = Color(0xFF00F0FF),
                    darkTextOnSelected = true,
                    selectedSlot = state.kineticSfxSlot,
                    slotLabelFor = { BuildArchiveCatalog.getActiveFireShortLabel(TowerClass.KINETIC_BALLISTIC, it) },
                    onSelectAndTest = { slot ->
                        onKineticSfxSlotSelected(slot)
                        onPlayTurretTestSound(TowerClass.KINETIC_BALLISTIC, slot)
                    },
                    onTestActive = { onPlayTurretTestSound(TowerClass.KINETIC_BALLISTIC, state.kineticSfxSlot) }
                )
                TurretGunfireSlotSection(
                    title = "Elemental Void (Energy)",
                    accentColor = Color(0xFFD946EF),
                    darkTextOnSelected = false,
                    selectedSlot = state.elementalSfxSlot,
                    slotLabelFor = { BuildArchiveCatalog.getActiveFireShortLabel(TowerClass.ELEMENTAL_VOID, it) },
                    onSelectAndTest = { slot ->
                        onElementalSfxSlotSelected(slot)
                        onPlayTurretTestSound(TowerClass.ELEMENTAL_VOID, slot)
                    },
                    onTestActive = { onPlayTurretTestSound(TowerClass.ELEMENTAL_VOID, state.elementalSfxSlot) }
                )
                TurretGunfireSlotSection(
                    title = "Heavy Siege (Artillery)",
                    accentColor = Color(0xFFFFB800),
                    darkTextOnSelected = true,
                    selectedSlot = state.siegeSfxSlot,
                    slotLabelFor = { BuildArchiveCatalog.getActiveFireShortLabel(TowerClass.HEAVY_SIEGE, it) },
                    onSelectAndTest = { slot ->
                        onSiegeSfxSlotSelected(slot)
                        onPlayTurretTestSound(TowerClass.HEAVY_SIEGE, slot)
                    },
                    onTestActive = { onPlayTurretTestSound(TowerClass.HEAVY_SIEGE, state.siegeSfxSlot) }
                )
                TurretGunfireSlotSection(
                    title = "Stellar Cosmic (Singularity)",
                    accentColor = Color(0xFFFFD700),
                    darkTextOnSelected = true,
                    selectedSlot = state.stellarSfxSlot,
                    slotLabelFor = { BuildArchiveCatalog.getActiveFireShortLabel(TowerClass.STELLAR_COSMIC, it) },
                    onSelectAndTest = { slot ->
                        onStellarSfxSlotSelected(slot)
                        onPlayTurretTestSound(TowerClass.STELLAR_COSMIC, slot)
                    },
                    onTestActive = { onPlayTurretTestSound(TowerClass.STELLAR_COSMIC, state.stellarSfxSlot) }
                )
            }
        }

        // Section 4: Tower Upgrade Audio Testing (4 Classes x 5 Skins)
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TOWER UPGRADE AUDIO TESTING (4 CLASSES × 5 SKINS)", color = Color(0xFF00FF9D), fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("Test class-specific and skin-tailored tower upgrade sound profiles directly from the menu.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                val upgradeClasses = listOf(
                    Triple(TowerClass.KINETIC_BALLISTIC, "Kinetic Upgrade", Color(0xFF00F0FF)),
                    Triple(TowerClass.ELEMENTAL_VOID, "Elemental Upgrade", Color(0xFFD946EF)),
                    Triple(TowerClass.HEAVY_SIEGE, "Siege Upgrade", Color(0xFFFFB800)),
                    Triple(TowerClass.STELLAR_COSMIC, "Stellar Upgrade", Color(0xFFFFD700))
                )

                upgradeClasses.forEach { (cls, name, color) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Button(
                                onClick = { onPlayTowerUpgradeTestSound(cls, state.turretSkinSlot) },
                                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(22.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("TEST (SKIN ${state.turretSkinSlot})", fontWeight = FontWeight.Black, fontSize = 8.5.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { slot ->
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onPlayTowerUpgradeTestSound(cls, slot) }
                                ) {
                                    Text(
                                        text = "Skin $slot",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Turret Visual Skins (Build #61 Active Lineup)
        val equippedSkinDef = BuildArchiveCatalog.resolveTurretSkin(state.turretSkinId, state.turretSkinSlot)
        var previewSkinId by remember(state.turretSkinId, state.turretSkinSlot) {
            mutableStateOf(equippedSkinDef.id)
        }
        val previewSkinDef = remember(previewSkinId, state.turretSkinId, state.turretSkinSlot) {
            BuildArchiveCatalog.resolveTurretSkin(previewSkinId, state.turretSkinSlot)
        }

        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TURRET VISUAL SKINS (BUILD #$CURRENT_BUILD_CODE LINEUP)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("Tap PREVIEW to inspect all 4 classes or tap a row to equip immediately.", color = Color(0xFF94A3B8), fontSize = 8.5.sp)

                TurretSkinMultiClassPreviewStage(
                    skinDef = previewSkinDef,
                    isEquipped = (previewSkinDef.id == equippedSkinDef.id),
                    onEquipClick = { onTurretSkinSlotSelected(previewSkinDef.slotIndex) },
                    animateAuraPulse = animateSkinPreviewPulse
                )

                BuildArchiveVisualCatalog.activeTurretSkins.forEach { skin ->
                    val isSel = (state.turretSkinId == skin.id) ||
                        (state.turretSkinSlot == skin.slotIndex && !BuildArchiveCatalog.isArchivedAssetId(state.turretSkinId))
                    val isPreviewing = (previewSkinDef.id == skin.id)
                    Surface(
                        color = when {
                            isSel -> Color(0xFF00FF9D).copy(alpha = 0.20f)
                            isPreviewing -> Color(0xFF00F0FF).copy(alpha = 0.14f)
                            else -> Color(0xFF0F172A)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSel -> Color(0xFF00FF9D)
                                isPreviewing -> Color(0xFF00F0FF)
                                else -> Color(0xFF334155)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("skin_row_${skin.id}")
                            .clickable {
                                previewSkinId = skin.id
                                onTurretSkinSlotSelected(skin.slotIndex)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TurretSkinPreviewThumbnail(skinDef = skin, isSelected = isSel)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.Palette,
                                        contentDescription = null,
                                        tint = if (isSel) Color(0xFF00FF9D) else Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Slot ${skin.slotIndex}: ${skin.displayName}",
                                        color = if (isSel) Color.White else Color(0xFFE2E8F0),
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = skin.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isPreviewing) Color(0xFF00F0FF).copy(alpha = 0.20f) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(1.dp, if (isPreviewing) Color(0xFF00F0FF) else Color(0xFF475569)),
                                modifier = Modifier
                                    .testTag("skin_preview_btn_${skin.id}")
                                    .clickable { previewSkinId = skin.id }
                            ) {
                                Text(
                                    text = if (isPreviewing) "VIEWING" else "PREVIEW",
                                    color = if (isPreviewing) Color(0xFF00F0FF) else Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 6: Map & Lane Themes (Build #61 Active Lineup)
        val equippedThemeDef = BuildArchiveCatalog.resolveMapTheme(state.mapThemeId, state.mapThemeSlot)
        var previewThemeId by remember(state.mapThemeId, state.mapThemeSlot) {
            mutableStateOf(equippedThemeDef.id)
        }
        val previewThemeDef = remember(previewThemeId, state.mapThemeId, state.mapThemeSlot) {
            BuildArchiveCatalog.resolveMapTheme(previewThemeId, state.mapThemeSlot)
        }

        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MAP & LANE ENVIRONMENT THEMES (BUILD #$CURRENT_BUILD_CODE LINEUP)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("Preview lane conduits, grid contrast, and terrain palette live.", color = Color(0xFF94A3B8), fontSize = 8.5.sp)

                MapThemeLivePreviewStage(
                    themeDef = previewThemeDef,
                    isEquipped = (previewThemeDef.id == equippedThemeDef.id),
                    onEquipClick = { onMapThemeSlotSelected(previewThemeDef.slotIndex) }
                )

                BuildArchiveVisualCatalog.activeMapThemes.forEach { theme ->
                    val isSel = (state.mapThemeId == theme.id) ||
                        (state.mapThemeSlot == theme.slotIndex && !BuildArchiveCatalog.isArchivedAssetId(state.mapThemeId))
                    val isPreviewing = (previewThemeDef.id == theme.id)
                    Surface(
                        color = when {
                            isSel -> Color(0xFFFFB800).copy(alpha = 0.20f)
                            isPreviewing -> Color(0xFF00F0FF).copy(alpha = 0.14f)
                            else -> Color(0xFF0F172A)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSel -> Color(0xFFFFB800)
                                isPreviewing -> Color(0xFF00F0FF)
                                else -> Color(0xFF334155)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_row_${theme.id}")
                            .clickable {
                                previewThemeId = theme.id
                                onMapThemeSlotSelected(theme.slotIndex)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MapThemePreviewThumbnail(themeDef = theme, isSelected = isSel)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.GridOn,
                                        contentDescription = null,
                                        tint = if (isSel) Color(0xFFFFB800) else Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Slot ${theme.slotIndex}: ${theme.displayName}",
                                        color = if (isSel) Color.White else Color(0xFFE2E8F0),
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = theme.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isPreviewing) Color(0xFF00F0FF).copy(alpha = 0.20f) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(1.dp, if (isPreviewing) Color(0xFF00F0FF) else Color(0xFF475569)),
                                modifier = Modifier
                                    .testTag("theme_preview_btn_${theme.id}")
                                    .clickable { previewThemeId = theme.id }
                            ) {
                                Text(
                                    text = if (isPreviewing) "VIEWING" else "PREVIEW",
                                    color = if (isPreviewing) Color(0xFF00F0FF) else Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 7: Procedural Sound FX Profiles
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PROCEDURAL SOUND FX PROFILES", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("Select the synthesized procedural sound effect wave generator profile.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                SoundProfile.entries.forEach { profile ->
                    val isSel = (state.soundProfile == profile)
                    Surface(
                        color = if (isSel) Color(0xFF00F0FF) else Color(0xFF0F172A),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF00F0FF) else Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSoundProfileChanged(profile) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = if (isSel) Color(0xFF00F0FF) else Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = profile.label,
                                color = if (isSel) Color.White else Color(0xFFCBD5E1),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeSliderRow(
    label: String,
    value: Float,
    accentColor: Color,
    pctColor: Color,
    onValueChanged: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            Text("${(value * 100).roundToInt()}%", color = pctColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChanged,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
        )
    }
}

@Composable
private fun MusicOptionRow(
    label: String,
    isSelected: Boolean,
    showPreviewingBadge: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFFC084FC).copy(alpha = 0.25f) else Color(0xFF0F172A),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFC084FC) else Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFC084FC) else Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.5.sp
                )
            }
            if (showPreviewingBadge) {
                Surface(
                    color = Color(0xFFC084FC).copy(alpha = 0.20f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "▶ PREVIEWING",
                        color = Color(0xFFC084FC),
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TurretGunfireSlotSection(
    title: String,
    accentColor: Color,
    darkTextOnSelected: Boolean,
    selectedSlot: Int,
    slotLabelFor: (Int) -> String,
    onSelectAndTest: (Int) -> Unit,
    onTestActive: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Button(
                onClick = onTestActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (darkTextOnSelected) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(22.dp)
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("TEST", fontWeight = FontWeight.Black, fontSize = 9.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { slot ->
                val isSel = selectedSlot == slot
                Surface(
                    color = if (isSel) accentColor else Color(0xFF0F172A),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isSel) accentColor else Color(0xFF334155)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectAndTest(slot) }
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Slot $slot",
                            color = if (isSel && darkTextOnSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                        Text(
                            slotLabelFor(slot),
                            color = if (isSel) (if (darkTextOnSelected) Color(0xFF0F172A) else Color(0xFFFDF4FF)) else Color(0xFF94A3B8),
                            fontSize = 7.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
