package com.example.ui.hud.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BankedSynergyVault
import com.example.model.GlobalAuraSynergy

@Composable
internal fun HudSynergyCapsule(
    activeGlobalSynergies: List<GlobalAuraSynergy>,
    maxConduitGen: Int,
    absorbedVaults: List<BankedSynergyVault>,
    absorbedSynergiesCount: Int,
    isRotated: Boolean,
    animatedRotation: Float
) {
    var showSynergyDropdown by remember { mutableStateOf(false) }
    val activeSynergiesCount = activeGlobalSynergies.size
    val totalSynergiesCount = activeSynergiesCount

    Box(modifier = Modifier.hudCapsuleContainer(isRotated, animatedRotation)) {
        Surface(
            color = if (totalSynergiesCount > 0) Color(0xFFFFB800).copy(alpha = 0.22f) else Color(0xFF161F30).copy(alpha = 0.78f),
            shape = HUD_CAPSULE_SHAPE,
            border = BorderStroke(
                width = 1.dp,
                color = if (totalSynergiesCount > 0) Color(0xFFFFB800).copy(alpha = 0.7f) else Color(0xFF334155)
            ),
            modifier = Modifier
                .height(if (isRotated) HUD_ROTATED_CAPSULE_HEIGHT else HUD_CAPSULE_HEIGHT)
                .defaultMinSize(minWidth = if (isRotated) 36.dp else 1.dp)
                .clip(HUD_CAPSULE_SHAPE)
                .clickable { showSynergyDropdown = !showSynergyDropdown }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Active Synergies",
                    tint = if (totalSynergiesCount > 0) Color(0xFFFFB800) else Color(0xFF94A3B8),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "($totalSynergiesCount)",
                    color = if (totalSynergiesCount > 0) Color(0xFFFFB800) else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1
                )
            }
        }

        if (!isRotated) {
            DropdownMenu(
                expanded = showSynergyDropdown,
                onDismissRequest = { showSynergyDropdown = false },
                modifier = Modifier
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFFFFB800), RoundedCornerShape(6.dp))
                    .widthIn(max = 260.dp)
                    .heightIn(max = 320.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        SynergyBreakdownContent(
                            activeGlobalSynergies = activeGlobalSynergies,
                            maxConduitGen = maxConduitGen,
                            absorbedVaults = absorbedVaults,
                            absorbedSynergiesCount = absorbedSynergiesCount
                        )
                    },
                    onClick = { showSynergyDropdown = false }
                )
            }
        }
    }

    if (isRotated && showSynergyDropdown) {
        Dialog(
            onDismissRequest = { showSynergyDropdown = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSynergyDropdown = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.98f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFFB800)),
                    modifier = Modifier
                        .graphicsLayer { rotationZ = animatedRotation }
                        .widthIn(min = 300.dp, max = 340.dp)
                        .heightIn(max = 320.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = true
                        ) {}
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            SynergyBreakdownContent(
                                activeGlobalSynergies = activeGlobalSynergies,
                                maxConduitGen = maxConduitGen,
                                absorbedVaults = absorbedVaults,
                                absorbedSynergiesCount = absorbedSynergiesCount
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showSynergyDropdown = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("CLOSE", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SynergyBreakdownContent(
    activeGlobalSynergies: List<GlobalAuraSynergy>,
    maxConduitGen: Int,
    absorbedVaults: List<BankedSynergyVault>,
    absorbedSynergiesCount: Int
) {
    Column {
        Text(
            text = "ACTIVE SYNERGIES (${activeGlobalSynergies.size})",
            color = Color(0xFFFFB800),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (activeGlobalSynergies.isEmpty()) {
            Text(
                text = "No active aura synergies.\nAscend towers to Tier 3 to project global buffs across the battlefield.",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                lineHeight = 12.sp
            )
        } else {
            activeGlobalSynergies.forEach { syn ->
                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = syn.iconEmoji, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${syn.auraName} (${syn.sourceCount}x)",
                            color = Color(syn.color),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = syn.effectDescription,
                        color = Color(0xFFCBD5E1),
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            thickness = 1.dp,
            color = Color(0xFF1E293B)
        )
        Text(
            text = "⚡ ABSORBED CONDUIT SYNERGIES (GEN $maxConduitGen • $absorbedSynergiesCount Banked)",
            color = Color(0xFF00F0FF),
            fontWeight = FontWeight.Black,
            fontSize = 10.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (absorbedVaults.isEmpty()) {
            Text(
                text = "No absorbed conduit synergies yet. Reach Tier 3 on all towers and ascend into a Conduit to permanently bank synergies.",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                lineHeight = 12.sp
            )
        } else {
            absorbedVaults.forEach { vault ->
                vault.synergies.forEach { syn ->
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = syn.iconEmoji, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${syn.auraName} [Gen ${vault.generation}]",
                                color = Color(syn.color),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = "${syn.effectDescription} • [PERMANENT CORE BUFF]",
                            color = Color(0xFF00FF9D),
                            fontSize = 8.5.sp,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}
