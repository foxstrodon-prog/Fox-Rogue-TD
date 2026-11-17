package com.example.ui.menu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.BuildArchiveCatalog
import com.example.audio.BuildArchiveVisualCatalog
import com.example.audio.MusicTrack
import com.example.audio.SoundProfile
import com.example.model.TowerClass
import com.example.model.TowerDrawStyle
import com.example.model.TurretSkinDef
import com.example.model.TurretSkinSlot
import com.example.ui.GameUIState
import kotlin.math.roundToInt

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
    onOpenAssetVault: () -> Unit = {}
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
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00E5FF)),
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
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
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
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MUSIC STREAMING & PLAYLIST (BUILD #61)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("Dynamic combat ducking automatically drops BGM during turret firing.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                BuildArchiveCatalog.build61BgmTracks.forEach { trackDef ->
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
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
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
                    slotLabelFor = { BuildArchiveCatalog.getBuild61FireShortLabel(TowerClass.KINETIC_BALLISTIC, it) },
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
                    slotLabelFor = { BuildArchiveCatalog.getBuild61FireShortLabel(TowerClass.ELEMENTAL_VOID, it) },
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
                    slotLabelFor = { BuildArchiveCatalog.getBuild61FireShortLabel(TowerClass.HEAVY_SIEGE, it) },
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
                    slotLabelFor = { BuildArchiveCatalog.getBuild61FireShortLabel(TowerClass.STELLAR_COSMIC, it) },
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
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
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
                                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
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
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TURRET VISUAL SKINS (BUILD #61 LINEUP)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                BuildArchiveVisualCatalog.build61TurretSkins.forEach { skin ->
                    val isSel = (state.turretSkinId == skin.id) ||
                        (state.turretSkinSlot == skin.slotIndex && !state.turretSkinId.startsWith("b60_") && !state.turretSkinId.startsWith("pre60_"))
                    Surface(
                        color = if (isSel) Color(0xFF00FF9D).copy(alpha = 0.20f) else Color(0xFF0F172A),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF00FF9D) else Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTurretSkinSlotSelected(skin.slotIndex) }
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
                                        fontSize = 10.5.sp
                                    )
                                }
                                Text(
                                    text = skin.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 6: Map & Lane Themes (Build #61 Active Lineup)
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MAP & LANE ENVIRONMENT THEMES (BUILD #61 LINEUP)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                BuildArchiveVisualCatalog.build61MapThemes.forEach { theme ->
                    val isSel = (state.mapThemeId == theme.id) ||
                        (state.mapThemeSlot == theme.slotIndex && !state.mapThemeId.startsWith("b60_") && !state.mapThemeId.startsWith("pre60_"))
                    Surface(
                        color = if (isSel) Color(0xFFFFB800).copy(alpha = 0.20f) else Color(0xFF0F172A),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFFB800) else Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMapThemeSlotSelected(theme.slotIndex) }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.GridOn,
                                    contentDescription = null,
                                    tint = if (isSel) Color(0xFFFFB800) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Slot ${theme.slotIndex}: ${theme.displayName}",
                                    color = if (isSel) Color.White else Color(0xFFE2E8F0),
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.5.sp
                                )
                            }
                            Text(
                                text = theme.description,
                                color = Color(0xFF94A3B8),
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 7: Procedural Sound FX Profiles
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF00F0FF) else Color(0xFF334155)),
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
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFC084FC) else Color(0xFF334155)),
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.6f))
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) accentColor else Color(0xFF334155)),
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
