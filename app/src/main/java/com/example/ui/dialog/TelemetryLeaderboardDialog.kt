package com.example.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GlobalBoardStatus
import com.example.ui.GameUIState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelemetryLeaderboardDialog(
    state: GameUIState,
    onSetPlayerName: (String) -> Unit,
    onRetryLeaderboard: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember(state.telemetryInitialTab) { mutableStateOf(state.telemetryInitialTab) }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) {
            onRetryLeaderboard()
        }
    }
    var editingCallsign by remember { mutableStateOf(state.activePlayerName) }
    var isEditingName by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0B132B),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00F0FF).copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TACTICAL TELEMETRY & ARCHIVES",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Operative: ${state.activePlayerName} • Performance telemetry & rankings",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF131F37),
                    contentColor = Color(0xFF00F0FF),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("LAST RUN", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("MY LIFETIME", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("GLOBAL RANKS", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> {
                        val last = state.lastRunRecord
                        if (last == null) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No completed combat runs recorded yet.\nDeploy and complete wave defense to log performance telemetry.",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = Color(0xFF162544),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (last.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = last.sectorName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            Text(
                                                text = if (last.isVictory) "VICTORY" else "DEFEAT",
                                                color = if (last.isVictory) Color(0xFF00FF9D) else Color(0xFFFF2A55),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                        Text(
                                            text = "Operative: ${last.playerName} • ${dateFormat.format(Date(last.timestamp))}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "TACTICAL SCORE: ${last.score} PTS",
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                TelemetryCard("Waves Cleared", "${last.wavesCleared}", Icons.Filled.MilitaryTech, Color(0xFF00F0FF))
                                TelemetryCard("Hostiles Annihilated", "${last.enemiesKilled}", Icons.Filled.CrisisAlert, Color(0xFFFF2A55))
                                TelemetryCard("Total Damage Inflicted", "${last.totalDamage.toInt()}", Icons.Filled.Bolt, Color(0xFFFFB800))
                                TelemetryCard("Scrap Harvested", "${last.scrapEarned} c", Icons.Filled.Diamond, Color(0xFFFFD700))
                                TelemetryCard("Most Lethal Tower (MVP)", last.mvpTower, Icons.Filled.Shield, Color(0xFFA855F7))
                            }
                        }
                    }

                    1 -> {
                        val lt = state.playerLifetimeTelemetry
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = Color(0xFF131F33),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "ACTIVE OPERATIVE CALLSIGN", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 8.5.sp)
                                        Text(text = "Personal Stats Isolated", color = Color(0xFF64748B), fontSize = 8.sp)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (isEditingName) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = editingCallsign,
                                                onValueChange = { if (it.length <= 16) editingCallsign = it },
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
                                                modifier = Modifier.weight(1f).height(42.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    val trimmed = editingCallsign.trim()
                                                    if (trimmed.isNotEmpty()) {
                                                        onSetPlayerName(trimmed)
                                                        isEditingName = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("SET", fontWeight = FontWeight.Black, fontSize = 10.sp)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = state.activePlayerName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            }
                                            TextButton(
                                                onClick = {
                                                    editingCallsign = state.activePlayerName
                                                    isEditingName = true
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("CHANGE CALLSIGN", color = Color(0xFF00F0FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            TelemetryCard("Total Missions Flown", "${lt.totalRuns}", Icons.Filled.PlayArrow, Color(0xFF00F0FF))
                            TelemetryCard("Total Hostiles Annihilated", "${lt.totalHostilesKilled}", Icons.Filled.CrisisAlert, Color(0xFFFF2A55))
                            TelemetryCard("Total Credits Harvested", "${lt.totalScrapMined}", Icons.Filled.Diamond, Color(0xFFFFD700))
                            TelemetryCard("Longest Wave Record", "Wave ${lt.bestWaveEver}", Icons.Filled.EmojiEvents, Color(0xFF00FF9D))
                            TelemetryCard("Highest Damage MVP", lt.mostUsedTower, Icons.Filled.MilitaryTech, Color(0xFFA855F7))
                        }
                    }

                    2 -> {
                        when {
                            state.leaderboardStatus == GlobalBoardStatus.LOADING && state.onlineLeaderboard.isEmpty() -> {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color(0xFF00F0FF), strokeWidth = 2.5.dp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(text = "Connecting to Global Uplink...", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                }
                            }
                            state.onlineLeaderboard.isEmpty() -> {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                        val isOnline = state.leaderboardStatus == GlobalBoardStatus.ONLINE
                                        if (isOnline) {
                                            Surface(
                                                color = Color(0xFF06331E),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFF00FF9D).copy(alpha = 0.5f)),
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            ) {
                                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF00FF9D), modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(text = "GLOBAL UPLINK VERIFIED • CONNECTED", color = Color(0xFF00FF9D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (isOnline) "Leaderboard uplink is live!\nBe the first Commander to record a verified high score."
                                            else (state.leaderboardErrorMessage ?: "Connection standby. Tap Refresh to connect."),
                                            color = if (isOnline) Color(0xFF94A3B8) else Color(0xFFFF2A55),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = onRetryLeaderboard,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = if (isOnline) "Refresh Rankings" else "Retry Uplink", color = Color(0xFF00F0FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Column(modifier = Modifier.weight(1f)) {
                                    when (state.leaderboardStatus) {
                                        GlobalBoardStatus.ONLINE -> {
                                            LeaderboardStatusBanner(
                                                bgColor = Color(0xFF06331E),
                                                borderColor = Color(0xFF00FF9D),
                                                icon = Icons.Filled.CheckCircle,
                                                text = "Global Rankings (Verified)",
                                                actionText = "REFRESH",
                                                onAction = onRetryLeaderboard
                                            )
                                        }
                                        GlobalBoardStatus.OFFLINE_CACHED -> {
                                            LeaderboardStatusBanner(
                                                bgColor = Color(0xFF332005),
                                                borderColor = Color(0xFFFFB800),
                                                icon = Icons.Filled.CloudOff,
                                                text = "Cached Rankings (Offline Mode)",
                                                actionText = "RETRY",
                                                onAction = onRetryLeaderboard
                                            )
                                        }
                                        GlobalBoardStatus.NOT_CONFIGURED -> {
                                            LeaderboardStatusBanner(
                                                bgColor = Color(0xFF1E293B),
                                                borderColor = Color(0xFF64748B),
                                                icon = Icons.Filled.Info,
                                                text = "Local Only (Global Board Unconfigured)",
                                                actionText = null,
                                                onAction = {}
                                            )
                                        }
                                        GlobalBoardStatus.ERROR -> {
                                            LeaderboardStatusBanner(
                                                bgColor = Color(0xFF3B0B13),
                                                borderColor = Color(0xFFFF2A55),
                                                icon = Icons.Filled.Warning,
                                                text = state.leaderboardErrorMessage ?: "Network Error - Tap Refresh",
                                                actionText = "RETRY",
                                                onAction = onRetryLeaderboard
                                            )
                                        }
                                        else -> {}
                                    }

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(state.onlineLeaderboard) { index, item ->
                                            val rankDisplay = when (index) {
                                                0 -> "🥇"
                                                1 -> "🥈"
                                                2 -> "🥉"
                                                else -> "#${item.rank}"
                                            }
                                            val isLocalPlayer = item.isLocalPlayer || item.name.equals(state.activePlayerName, ignoreCase = true)

                                            Surface(
                                                color = if (isLocalPlayer) Color(0xFF132A44) else Color(0xFF162544),
                                                shape = RoundedCornerShape(8.dp),
                                                border = when {
                                                    isLocalPlayer -> BorderStroke(1.5.dp, Color(0xFF00F0FF))
                                                    index == 0 -> BorderStroke(1.dp, Color(0xFFFFD700))
                                                    else -> null
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = rankDisplay, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(text = item.name, color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                                if (item.isDevTuned) {
                                                                    Spacer(modifier = Modifier.width(5.dp))
                                                                    Surface(
                                                                        color = Color(0xFFEAB308).copy(alpha = 0.22f),
                                                                        shape = RoundedCornerShape(3.dp),
                                                                        border = BorderStroke(0.8.dp, Color(0xFFEAB308))
                                                                    ) {
                                                                        Text(
                                                                            text = "DEV",
                                                                            color = Color(0xFFFDE047),
                                                                            fontSize = 8.sp,
                                                                            fontWeight = FontWeight.Black,
                                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(text = "Wave ${item.wavesCleared}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                            }
                                                            Text(text = "${item.score} PTS", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                                                        }

                                                        if (item.scrapEarned > 0 || item.date.isNotBlank()) {
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                                if (item.scrapEarned > 0) {
                                                                    Text(text = "${item.scrapEarned} Scrap Salvaged", color = Color(0xFF94A3B8), fontSize = 9.sp)
                                                                }
                                                                if (item.date.isNotBlank()) {
                                                                    Text(text = item.date, color = Color(0xFF64748B), fontSize = 8.5.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardStatusBanner(
    bgColor: Color,
    borderColor: Color,
    icon: ImageVector,
    text: String,
    actionText: String?,
    onAction: () -> Unit
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = borderColor, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = text, color = borderColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (actionText != null) {
                Text(
                    text = actionText,
                    color = Color(0xFF00F0FF),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { onAction() }
                )
            }
        }
    }
}

@Composable
fun TelemetryCard(label: String, value: String, icon: ImageVector, color: Color) {
    Surface(
        color = Color(0xFF131F37),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(text = value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}
