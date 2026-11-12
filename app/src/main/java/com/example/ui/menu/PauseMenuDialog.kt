package com.example.ui.menu

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.MusicTrack
import com.example.audio.SoundProfile
import com.example.engine.UpdateDownloadStatus
import com.example.model.*
import com.example.ui.GameUIState
import kotlin.math.roundToInt

/**
 * Visual thumbnail preview for custom turret vector skins.
 */
@Composable
fun TurretSkinPreviewThumbnail(
    skinSlot: TurretSkinSlot,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(36.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.width * 0.42f

        // Base circular chassis foundation
        drawCircle(
            color = Color(0xFF1E293B),
            radius = radius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = if (isSelected) Color(0xFF00FF9D) else Color(0xFF475569),
            radius = radius * 0.85f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF94A3B8),
            radius = radius * 0.35f,
            center = Offset(cx, cy)
        )

        // Visual skin features
        when (skinSlot) {
            TurretSkinSlot.SLOT_1_DEFAULT -> {
                // Sleek Neon Vector: concentric energy aura
                drawCircle(
                    color = Color(0xFF00F0FF).copy(alpha = 0.7f),
                    radius = radius * 0.58f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            TurretSkinSlot.SLOT_2_BLUEPRINT -> {
                // Blueprint Schematic: technical crosshairs & drafting ring
                val ch = radius * 0.75f
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx - ch, cy),
                    end = Offset(cx + ch, cy),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx, cy - ch),
                    end = Offset(cx, cy + ch),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = 0.65f),
                    radius = radius * 0.95f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            TurretSkinSlot.SLOT_3_MILITARY -> {
                // Tactical Military: heavy armor with 4 corner rivet bolts
                val rOff = radius * 0.55f
                val rRad = 2.dp.toPx()
                listOf(
                    Offset(cx - rOff, cy - rOff),
                    Offset(cx + rOff, cy - rOff),
                    Offset(cx - rOff, cy + rOff),
                    Offset(cx + rOff, cy + rOff)
                ).forEach { rPos ->
                    drawCircle(color = Color(0xFF475569), radius = rRad, center = rPos)
                    drawCircle(color = Color(0xFFCBD5E1), radius = rRad * 0.5f, center = rPos)
                }
            }
            TurretSkinSlot.SLOT_4_VOID -> {
                // Deep Void Rift: astral purple corona ring
                drawCircle(
                    color = Color(0xFFA855F7).copy(alpha = 0.85f),
                    radius = radius * 0.95f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFEC4899).copy(alpha = 0.5f),
                    radius = radius * 0.65f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            TurretSkinSlot.SLOT_5_PIXEL -> {
                // Retro Pixel Arcade: 8-bit stepped golden corners
                val pSize = 3.5.dp.toPx()
                val bOff = radius * 0.68f
                listOf(
                    Offset(cx - bOff, cy - bOff),
                    Offset(cx + bOff - pSize, cy - bOff),
                    Offset(cx - bOff, cy + bOff - pSize),
                    Offset(cx + bOff - pSize, cy + bOff - pSize)
                ).forEach { corner ->
                    drawRect(color = Color(0xFFFACC15), topLeft = corner, size = Size(pSize, pSize))
                }
            }
        }
    }
}

/**
 * Main Pause Menu & Settings Hub Dialog for Fox-Rogue-TD.
 * Houses combat simulation speed selection, audio mixing & test controls, visual skins, and auto-updater.
 */
@Composable
fun PauseMenuDialog(
    state: GameUIState,
    onSpeedChanged: (GameSpeed) -> Unit,
    onOpenTelemetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onRestart: () -> Unit,
    onEndRun: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onDownloadAndInstallUpdate: () -> Unit = {},
    onMasterVolumeChanged: (Float) -> Unit = {},
    onMusicVolumeChanged: (Float) -> Unit = {},
    onSfxVolumeChanged: (Float) -> Unit = {},
    onUpgradeVolumeChanged: (Float) -> Unit = {},
    onToggleAudioMute: () -> Unit = {},
    onSoundProfileChanged: (SoundProfile) -> Unit = {},
    onMusicTrackSelected: (MusicTrack) -> Unit = {},
    onKineticSfxSlotSelected: (Int) -> Unit = {},
    onElementalSfxSlotSelected: (Int) -> Unit = {},
    onSiegeSfxSlotSelected: (Int) -> Unit = {},
    onPlayTurretTestSound: (TowerClass, Int) -> Unit = { _, _ -> },
    onPlayTowerUpgradeTestSound: (TowerClass, Int) -> Unit = { _, _ -> },
    onTurretSkinSlotSelected: (Int) -> Unit = {},
    onMapThemeSlotSelected: (Int) -> Unit = {},
    onDifficultyChanged: (GameDifficulty) -> Unit = {},
    onResume: () -> Unit
) {
    var isCustomizationOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onResume) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isCustomizationOpen) Color(0xFFC084FC) else Color(0xFFFFB800)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCustomizationOpen) {
                            IconButton(
                                onClick = { isCustomizationOpen = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFC084FC), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AUDIO & SKINS",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(imageVector = Icons.Filled.PauseCircle, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GAME PAUSED",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF00F0FF).copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.7f))
                                ) {
                                    Text(
                                        text = "v${com.example.BuildConfig.VERSION_NAME}",
                                        color = Color(0xFF00F0FF),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isCustomizationOpen) {
                    // === AUDIO & VISUAL CUSTOMIZATION SUBSECTION ===
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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

                                // Master Volume
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Master Output", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(state.masterVolume * 100).roundToInt()}%", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = state.masterVolume,
                                        onValueChange = onMasterVolumeChanged,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF))
                                    )
                                }

                                // Music Volume (Live attenuated feedback while paused)
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Music Volume (Live -50% Pause Level)", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(state.musicVolume * 100).roundToInt()}%", color = Color(0xFFC084FC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = state.musicVolume,
                                        onValueChange = onMusicVolumeChanged,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFC084FC), activeTrackColor = Color(0xFFC084FC))
                                    )
                                }

                                // SFX Volume
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Combat Sound Effects", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(state.sfxVolume * 100).roundToInt()}%", color = Color(0xFFFFB800), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = state.sfxVolume,
                                        onValueChange = onSfxVolumeChanged,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFB800), activeTrackColor = Color(0xFFFFB800))
                                    )
                                }

                                // Upgrade Sound Effects Volume
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Upgrade Sound Effects", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(state.upgradeVolume * 100).roundToInt()}%", color = Color(0xFF00FF9D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = state.upgradeVolume,
                                        onValueChange = onUpgradeVolumeChanged,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9D), activeTrackColor = Color(0xFF00FF9D))
                                    )
                                }
                            }
                        }

                        // Section 2: Music Playlist & Track Selector
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("MUSIC STREAMING & PLAYLIST", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("Dynamic combat ducking automatically drops BGM during turret firing.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                                val tracks = listOf(
                                    MusicTrack.ROLLING_PLAYLIST to "🔁 Rolling Playlist (Cycles 1-5)",
                                    MusicTrack.CYBER_AMBIENT to "Track 1: Sector Zero Synth",
                                    MusicTrack.PULSE_ARPEGGIO to "Track 2: Dual Front Pulse",
                                    MusicTrack.DEEP_SPACE to "Track 3: Sub-Void Drone",
                                    MusicTrack.NEON_OVERDRIVE to "Track 4: Neon Overdrive",
                                    MusicTrack.CELESTIAL_BASTION to "Track 5: Celestial Bastion",
                                    MusicTrack.NONE to "Mute Music Track"
                                )

                                tracks.forEach { (track, label) ->
                                    val isSel = (state.musicTrack == track)
                                    Surface(
                                        color = if (isSel) Color(0xFFC084FC).copy(alpha = 0.25f) else Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFC084FC) else Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onMusicTrackSelected(track) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (isSel) Color(0xFFC084FC) else Color(0xFF64748B),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = label,
                                                    color = if (isSel) Color.White else Color(0xFFCBD5E1),
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                            if (isSel && track != MusicTrack.NONE) {
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
                            }
                        }

                        // Section 3: Turret Gunfire Audio Variants
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("TURRET GUNFIRE AUDIO TESTING (5 SLOTS)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("A/B test gunfire audio profiles across the 3 turret archetypes. Tap any slot to test immediately.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                                // Kinetic Ballistics
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Kinetic Ballistics (Rotary)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Button(
                                            onClick = { onPlayTurretTestSound(TowerClass.KINETIC_BALLISTIC, state.kineticSfxSlot) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
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
                                            val isSel = state.kineticSfxSlot == slot
                                            val slotName = TurretSfxSlot.fromSlot(slot).kineticName
                                            Surface(
                                                color = if (isSel) Color(0xFF00F0FF) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF00F0FF) else Color(0xFF334155)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        onKineticSfxSlotSelected(slot)
                                                        onPlayTurretTestSound(TowerClass.KINETIC_BALLISTIC, slot)
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Slot $slot", color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    Text(slotName.split(" ").first(), color = if (isSel) Color(0xFF0F172A) else Color(0xFF94A3B8), fontSize = 7.5.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Elemental Void
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Elemental Void (Energy)", color = Color(0xFFD946EF), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Button(
                                            onClick = { onPlayTurretTestSound(TowerClass.ELEMENTAL_VOID, state.elementalSfxSlot) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD946EF), contentColor = Color.White),
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
                                            val isSel = state.elementalSfxSlot == slot
                                            val slotName = TurretSfxSlot.fromSlot(slot).elementalName
                                            Surface(
                                                color = if (isSel) Color(0xFFD946EF) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFD946EF) else Color(0xFF334155)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        onElementalSfxSlotSelected(slot)
                                                        onPlayTurretTestSound(TowerClass.ELEMENTAL_VOID, slot)
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Slot $slot", color = if (isSel) Color.White else Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    Text(slotName.split(" ").first(), color = if (isSel) Color(0xFFFDF4FF) else Color(0xFF94A3B8), fontSize = 7.5.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Heavy Siege
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Heavy Siege (Artillery)", color = Color(0xFFFFB800), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Button(
                                            onClick = { onPlayTurretTestSound(TowerClass.HEAVY_SIEGE, state.siegeSfxSlot) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800), contentColor = Color.Black),
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
                                            val isSel = state.siegeSfxSlot == slot
                                            val slotName = TurretSfxSlot.fromSlot(slot).siegeName
                                            Surface(
                                                color = if (isSel) Color(0xFFFFB800) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFFB800) else Color(0xFF334155)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        onSiegeSfxSlotSelected(slot)
                                                        onPlayTurretTestSound(TowerClass.HEAVY_SIEGE, slot)
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Slot $slot", color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    Text(slotName.split(" ").first(), color = if (isSel) Color(0xFF0F172A) else Color(0xFF94A3B8), fontSize = 7.5.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 4: Tower Upgrade Audio Testing (3 Classes x 5 Skins)
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("TOWER UPGRADE AUDIO TESTING (3 CLASSES × 5 SKINS)", color = Color(0xFF00FF9D), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("Test class-specific and skin-tailored tower upgrade sound profiles directly from the menu.", color = Color(0xFF94A3B8), fontSize = 9.sp)

                                val upgradeClasses = listOf(
                                    Triple(TowerClass.KINETIC_BALLISTIC, "Kinetic Upgrade", Color(0xFF00F0FF)),
                                    Triple(TowerClass.ELEMENTAL_VOID, "Elemental Upgrade", Color(0xFFD946EF)),
                                    Triple(TowerClass.HEAVY_SIEGE, "Siege Upgrade", Color(0xFFFFB800))
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

                        // Section 5: Turret Visual Skins
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("TURRET VISUAL SKINS (5 SLOTS)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                TurretSkinSlot.entries.forEach { skin ->
                                    val isSel = (state.turretSkinSlot == skin.slot)
                                    Surface(
                                        color = if (isSel) Color(0xFF00FF9D).copy(alpha = 0.20f) else Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF00FF9D) else Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onTurretSkinSlotSelected(skin.slot) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TurretSkinPreviewThumbnail(
                                                skinSlot = skin,
                                                isSelected = isSel
                                            )
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
                                                        text = "Slot ${skin.slot}: ${skin.displayName}",
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

                        // Section 6: Map & Lane Themes
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("MAP & LANE ENVIRONMENT THEMES (5 SLOTS)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                MapThemeSlot.entries.forEach { theme ->
                                    val isSel = (state.mapThemeSlot == theme.slot)
                                    Surface(
                                        color = if (isSel) Color(0xFFFFB800).copy(alpha = 0.20f) else Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFFB800) else Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onMapThemeSlotSelected(theme.slot) }
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
                                                    text = "Slot ${theme.slot}: ${theme.displayName}",
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
                } else {
                    // === MAIN PAUSE ACTIONS VIEW ===
                    Text(
                        text = "COMBAT SIMULATION SPEED",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(GameSpeed.NORMAL, GameSpeed.FAST, GameSpeed.ULTRA, GameSpeed.MAX, GameSpeed.HYPER).forEach { speed ->
                            val activeSpeed = if (state.isPauseMenuOpen) state.prePauseSpeed else (if (state.gameSpeed != GameSpeed.PAUSED) state.gameSpeed else state.prePauseSpeed)
                            val isSel = (activeSpeed == speed)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFF00F0FF) else Color.Transparent)
                                    .clickable { onSpeedChanged(speed) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = speed.label,
                                    color = if (isSel) Color.Black else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "TACTICAL ESCALATION / DIFFICULTY",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GameDifficulty.entries.forEach { diff ->
                            val isSel = state.difficulty == diff
                            val diffColor = when (diff) {
                                GameDifficulty.EASY -> Color(0xFF10B981)
                                GameDifficulty.MEDIUM -> Color(0xFF38BDF8)
                                GameDifficulty.HARD -> Color(0xFFFF5722)
                                GameDifficulty.INFINITE -> Color(0xFFA855F7)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) diffColor else Color.Transparent)
                                    .clickable { onDifficultyChanged(diff) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff.label,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.difficulty.description,
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Menu Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Resume
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESUME BATTLE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        // Audio & Visual Customization
                        OutlinedButton(
                            onClick = { isCustomizationOpen = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC084FC)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC084FC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AUDIO & VISUAL CUSTOMIZATION", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Leaderboard & Telemetry
                        OutlinedButton(
                            onClick = onOpenTelemetry,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GLOBAL RANKS & METRICS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Dev Balance Console
                        OutlinedButton(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB800)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DEV BALANCE CONSOLE (SLIDERS)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // End Run & Save Stats
                        Button(
                            onClick = onEndRun,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A55), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("pause_menu_end_run_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("END RUN & SAVE STATS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Restart Run
                        OutlinedButton(
                            onClick = onRestart,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESTART WITH NEW MAP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // In-App APK Updater Status & Action
                        val updateState = state.updateUIState
                        if (updateState.status == UpdateDownloadStatus.AVAILABLE && updateState.updateInfo != null) {
                            Button(
                                onClick = onDownloadAndInstallUpdate,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("UPDATE TO v${updateState.updateInfo.latestVersion} [DOWNLOAD & INSTALL]", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                            }
                        } else if (updateState.status == UpdateDownloadStatus.DOWNLOADING) {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00F0FF), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Downloading Update: ${updateState.progressPercent}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (updateState.status == UpdateDownloadStatus.READY_TO_INSTALL) {
                            Button(
                                onClick = onDownloadAndInstallUpdate,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9D), contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("INSTALL DOWNLOADED UPDATE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onCheckForUpdates,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (updateState.status == UpdateDownloadStatus.CHECKING) "CHECKING GITHUB RELEASES..." else "CHECK FOR UPDATES (v${com.example.BuildConfig.VERSION_NAME})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Build Version Tag
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ROGUE TD • VERSION ${com.example.BuildConfig.VERSION_NAME} (BUILD #${com.example.BuildConfig.VERSION_CODE})",
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("pause_menu_version_label")
                        )
                    }
                }
            }
        }
    }
}
