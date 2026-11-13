package com.example.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                    AudioVisualCustomizationPanel(
                        state = state,
                        onMasterVolumeChanged = onMasterVolumeChanged,
                        onMusicVolumeChanged = onMusicVolumeChanged,
                        onSfxVolumeChanged = onSfxVolumeChanged,
                        onUpgradeVolumeChanged = onUpgradeVolumeChanged,
                        onToggleAudioMute = onToggleAudioMute,
                        onSoundProfileChanged = onSoundProfileChanged,
                        onMusicTrackSelected = onMusicTrackSelected,
                        onKineticSfxSlotSelected = onKineticSfxSlotSelected,
                        onElementalSfxSlotSelected = onElementalSfxSlotSelected,
                        onSiegeSfxSlotSelected = onSiegeSfxSlotSelected,
                        onPlayTurretTestSound = onPlayTurretTestSound,
                        onPlayTowerUpgradeTestSound = onPlayTowerUpgradeTestSound,
                        onTurretSkinSlotSelected = onTurretSkinSlotSelected,
                        onMapThemeSlotSelected = onMapThemeSlotSelected
                    )
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
