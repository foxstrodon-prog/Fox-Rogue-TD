package com.example.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.AppUpdateState
import com.example.engine.UpdateDownloadStatus
import com.example.model.GlobalBoardStatus
import com.example.ui.GameUIState

@Composable
fun UpdateModalDialog(
    updateState: AppUpdateState,
    onDownloadAndInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val info = updateState.updateInfo ?: return
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.8.dp, Color(0xFF00F0FF)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00F0FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SYSTEM UPDATE AVAILABLE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "v${info.currentVersion} ➔ v${info.latestVersion}",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (info.releaseTitle.isNotBlank() && info.releaseTitle != "v${info.latestVersion}") {
                    Text(
                        text = info.releaseTitle,
                        color = Color(0xFFFFB800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (info.releaseNotes.isNotBlank()) {
                    Text(
                        text = "RELEASE NOTES",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFF161F30),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = info.releaseNotes,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                when (updateState.status) {
                    UpdateDownloadStatus.DOWNLOADING -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { updateState.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF00F0FF),
                                trackColor = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Downloading APK: ${updateState.progressPercent}%",
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    UpdateDownloadStatus.READY_TO_INSTALL -> {
                        Surface(
                            color = Color(0xFF00FF9D).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF00FF9D)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF00FF9D), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "APK downloaded. Ready to install.", color = Color(0xFF00FF9D), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    UpdateDownloadStatus.FAILED -> {
                        Text(
                            text = updateState.errorMessage ?: "Update download failed.",
                            color = Color(0xFFFF2A55),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    else -> {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("LATER", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }

                    Button(
                        onClick = onDownloadAndInstall,
                        enabled = updateState.status != UpdateDownloadStatus.DOWNLOADING,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (updateState.status == UpdateDownloadStatus.READY_TO_INSTALL) Color(0xFF00FF9D) else Color(0xFF00F0FF),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1.4f).height(40.dp)
                    ) {
                        Text(
                            text = when (updateState.status) {
                                UpdateDownloadStatus.READY_TO_INSTALL -> "INSTALL NOW"
                                UpdateDownloadStatus.DOWNLOADING -> "DOWNLOADING..."
                                UpdateDownloadStatus.FAILED -> "RETRY"
                                else -> "UPDATE NOW"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RunSummaryDialog(
    state: GameUIState,
    onRestart: () -> Unit,
    onViewTelemetry: () -> Unit,
    onSetPlayerName: (String) -> Unit
) {
    var callsignInput by remember { mutableStateOf(state.activePlayerName) }
    var nameSaved by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { }) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, if (state.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state.isVictory) "DUAL SECTOR SECURED!" else "CORE BREACHED",
                    color = if (state.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )

                Text(
                    text = if (state.isVictory) "Both lanes held! Sector Zero defenses triumph." else "Dual-front hostiles overran the core nexus.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val isDevTuned = state.lastRunRecord?.isDevTuned == true || state.balanceConfig.isGameplayModified()
                if (isDevTuned) {
                    Surface(
                        color = Color(0xFFEAB308).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFEAB308)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "DEV TUNED • EXCLUDED FROM ONLINE LEADERBOARD",
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (state.leaderboardPendingCount > 0) {
                    Surface(
                        color = Color(0xFF00F0FF).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                        modifier = Modifier.padding(bottom = 8.dp).testTag("leaderboard_retry")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = Color(0xFF00F0FF))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "UPLOADING RUN TO GLOBAL RANKS...", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                        }
                    }
                } else {
                    when (state.leaderboardStatus) {
                        GlobalBoardStatus.LOADING -> {
                            Surface(
                                color = Color(0xFF00F0FF).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = Color(0xFF00F0FF))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "SYNCING LEADERBOARD...", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                                }
                            }
                        }
                        GlobalBoardStatus.ONLINE, GlobalBoardStatus.OFFLINE_CACHED, GlobalBoardStatus.IDLE -> {
                            Surface(
                                color = Color(0xFF00FF9D).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF00FF9D)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "SCORE SUBMITTED TO GLOBAL LEADERBOARD",
                                    color = Color(0xFF00FF9D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        GlobalBoardStatus.ERROR -> {
                            Surface(
                                color = Color(0xFFFF2A55).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF2A55)),
                                modifier = Modifier.padding(bottom = 8.dp).testTag("leaderboard_retry")
                            ) {
                                Text(
                                    text = "UPLOAD FAILED",
                                    color = Color(0xFFFF2A55),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        GlobalBoardStatus.NOT_CONFIGURED -> {
                            Surface(
                                color = Color(0xFF64748B).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF64748B)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "LOCAL ONLY (GLOBAL BOARD UNCONFIGURED)",
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = Color(0xFF131F33),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (nameSaved) Color(0xFF00FF9D) else Color(0xFF00F0FF).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "OPERATIVE CALLSIGN", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 9.sp)
                            if (nameSaved) {
                                Text(text = "SAVED TO PROFILE", color = Color(0xFF00FF9D), fontWeight = FontWeight.Bold, fontSize = 8.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = callsignInput,
                                onValueChange = {
                                    if (it.length <= 16) {
                                        callsignInput = it
                                        nameSaved = false
                                    }
                                },
                                placeholder = { Text("Callsign (e.g. Maverick)", fontSize = 11.sp, color = Color(0xFF64748B)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                ),
                                textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("operative_name_input")
                            )

                            Button(
                                onClick = {
                                    val trimmed = callsignInput.trim()
                                    if (trimmed.isNotEmpty()) {
                                        onSetPlayerName(trimmed)
                                        nameSaved = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (nameSaved) Color(0xFF00FF9D) else Color(0xFF00F0FF),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(44.dp).testTag("save_operative_name_button")
                            ) {
                                Text(if (nameSaved) "SAVED" else "SAVE", fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val score = (state.waveNumber.coerceAtLeast(1) - 1) * 1000 + (state.totalKills * 15) + (state.totalDamageDealt * 0.1f).toInt()
                        SummaryStatRow("Final Tactical Score", "$score PTS")
                        SummaryStatRow("Waves Cleared", "${state.waveNumber - 1}")
                        SummaryStatRow("Hostiles Slain", "${state.totalKills}")
                        SummaryStatRow("Damage Output", "${state.totalDamageDealt.toInt()}")
                        SummaryStatRow("Credits Harvested", "${state.totalScrapEarned}")
                        SummaryStatRow("Ascended Towers", "${state.towers.count { it.isAscended }}")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewTelemetry,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                        border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LEADERBOARD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f).height(44.dp).testTag("restart_run_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NEW RUN", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun EndRunConfirmDialog(
    state: GameUIState,
    onConfirmEnd: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Color(0xFFFF2A55)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF2A55),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "END DEFENSE RUN?",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Wave ${state.waveNumber} • ${state.totalKills} Hostiles Terminated • Score: ${state.totalDamageDealt.toInt()}\n\nEnding now will log all performance stats to your records and leaderboard.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onConfirmEnd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A55), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("confirm_end_run_button")
                ) {
                    Text("CONFIRM & LOG STATS", fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                    border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("RESUME DEFENSE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
