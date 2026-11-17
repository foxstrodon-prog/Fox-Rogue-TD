package com.example.ui.hud.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BankedSynergyVault
import com.example.model.GlobalAuraSynergy

@Composable
internal fun HudSynergyCapsule(
    activeGlobalSynergies: List<GlobalAuraSynergy>,
    activeBoardSynergies: List<GlobalAuraSynergy>? = null,
    maxConduitGen: Int,
    absorbedVaults: List<BankedSynergyVault>,
    absorbedSynergiesCount: Int,
    isRotated: Boolean,
    animatedRotation: Float,
    deviceAngle: Int = popupDeviceAngleFromCounterRotation(animatedRotation)
) {
    var showSynergyPopup by remember { mutableStateOf(false) }
    val singularitySynergiesCount = maxOf(
        absorbedSynergiesCount,
        absorbedVaults.sumOf { vault -> vault.synergies.sumOf { it.sourceCount } }
    )
    val boardSynergies = remember(activeBoardSynergies, activeGlobalSynergies, absorbedVaults) {
        if (activeBoardSynergies != null) {
            activeBoardSynergies
        } else if (absorbedVaults.isEmpty()) {
            activeGlobalSynergies
        } else {
            // Legacy caller provided combined activeGlobalSynergies without activeBoardSynergies:
            // subtract vaulted counts so Singularity synergies are not double-counted.
            val vaultedByName = mutableMapOf<String, Int>()
            for (vault in absorbedVaults) {
                for (syn in vault.synergies) {
                    vaultedByName[syn.auraName] = (vaultedByName[syn.auraName] ?: 0) + syn.sourceCount
                }
            }
            activeGlobalSynergies.mapNotNull { syn ->
                val sub = vaultedByName[syn.auraName] ?: 0
                if (sub <= 0) {
                    syn
                } else {
                    val remaining = syn.sourceCount - sub
                    vaultedByName[syn.auraName] = (sub - syn.sourceCount).coerceAtLeast(0)
                    if (remaining > 0) syn.copy(sourceCount = remaining) else null
                }
            }
        }
    }
    val boardSynergiesCount = boardSynergies.sumOf { it.sourceCount }
    val totalSynergiesCount = boardSynergiesCount + singularitySynergiesCount

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
                .clickable { showSynergyPopup = !showSynergyPopup }
                .testTag("hud_synergy_pill")
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

        if (showSynergyPopup) {
            HudTelemetryPopup(
                deviceAngle = deviceAngle,
                animatedRotation = animatedRotation,
                accentColor = Color(0xFFFFD700),
                popupTag = "hud_synergy_popup",
                onDismiss = { showSynergyPopup = false },
                scrollable = true
            ) {
                SynergyBreakdownContent(
                    boardSynergies = boardSynergies,
                    boardSynergiesCount = boardSynergiesCount,
                    maxConduitGen = maxConduitGen,
                    absorbedVaults = absorbedVaults,
                    singularitySynergiesCount = singularitySynergiesCount
                )
            }
        }
    }
}

@Composable
private fun SynergyBreakdownContent(
    boardSynergies: List<GlobalAuraSynergy>,
    boardSynergiesCount: Int,
    maxConduitGen: Int,
    absorbedVaults: List<BankedSynergyVault>,
    singularitySynergiesCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ACTIVE SYNERGIES ($boardSynergiesCount)",
            color = Color(0xFFFFB800),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "Board Live Synergies • ${boardSynergies.size} unique / $boardSynergiesCount total stacks",
            color = Color(0xFF94A3B8),
            fontSize = 8.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (boardSynergies.isEmpty()) {
            Text(
                text = "No active board aura synergies.\nAscend towers to Tier 3 to project global buffs across the battlefield.",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                lineHeight = 12.sp
            )
        } else {
            boardSynergies.forEach { syn ->
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
            text = "⚡ SINGULARITY TOWER (GEN $maxConduitGen • $singularitySynergiesCount Banked)",
            color = Color(0xFF00F0FF),
            fontWeight = FontWeight.Black,
            fontSize = 10.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (absorbedVaults.isEmpty()) {
            Text(
                text = "No Singularity Tower forged yet. Fill all pads with Tier 3 towers and collapse the board to bank synergies permanently.",
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
                                text = "${syn.auraName} (${syn.sourceCount}x) [Gen ${vault.generation}]",
                                color = Color(syn.color),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = "${syn.effectDescription} • [PERMANENT SINGULARITY BUFF]",
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
