package com.example.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.CustomAssetRecord
import com.example.model.MapThemeSlot
import com.example.model.TurretSkinSlot

private enum class VaultTab(val label: String) {
    ALL("All"),
    FIRE("Turret Fire (20)"),
    UPGRADE("Upgrades (20)"),
    BGM_SFX("BGM & SFX"),
    ARCHIVED("Archived WAVs"),
    TURRET_SKINS("Turret Skins (5)"),
    MAP_THEMES("Map Themes (5)"),
    LIKED("Liked / Starred"),
    DISLIKED("Disliked")
}

/**
 * In-Game Sound & Skin Vault Dialog (Sam's responsive squircle layout).
 * Supports 2-row cards for 360dp portrait phones and wide landscape tablets,
 * preview playback, equipping into GameUIState, and tri-state Like/Dislike/Star voting.
 */
@Composable
fun AssetVaultDialog(
    assets: List<CustomAssetRecord>,
    syncStatusText: String,
    onPreviewAsset: (String) -> Unit,
    onStopPreview: () -> Unit,
    onLikeAsset: (String) -> Unit,
    onDislikeAsset: (String) -> Unit,
    onStarAsset: (String) -> Unit,
    onEquipAsset: (String) -> Unit,
    onCloudSync: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(VaultTab.ALL) }

    val filteredAssets = remember(assets, selectedTab) {
        when (selectedTab) {
            VaultTab.ALL -> assets
            VaultTab.FIRE -> assets.filter { it.category == "TURRET_FIRE" }
            VaultTab.UPGRADE -> assets.filter { it.category == "TURRET_UPGRADE" }
            VaultTab.BGM_SFX -> assets.filter { it.category == "BACKGROUND_MUSIC" || it.category == "SFX" }
            VaultTab.ARCHIVED -> assets.filter { it.category == "ARCHIVED_WAV" }
            VaultTab.TURRET_SKINS -> assets.filter { it.category == "TURRET_SKIN" }
            VaultTab.MAP_THEMES -> assets.filter { it.category == "MAP_THEME" }
            VaultTab.LIKED -> assets.filter { it.voteState == 1 || it.isFavoriteStar }
            VaultTab.DISLIKED -> assets.filter { it.voteState == -1 }
        }
    }

    Dialog(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(22.dp))
                .testTag("asset_vault_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0D1117)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "SOUND & SKIN VAULT",
                            color = Color(0xFF00E5FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$syncStatusText • ${assets.size} assets",
                            color = Color(0xFF8B949E),
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SquircleActionButton(
                            label = "SYNC",
                            icon = Icons.Filled.CloudUpload,
                            bgColor = Color(0xFF1F2937),
                            borderColor = Color(0xFF38BDF8),
                            textColor = Color(0xFF38BDF8),
                            modifier = Modifier.testTag("asset_vault_cloud_sync"),
                            onClick = onCloudSync
                        )
                        SquircleActionButton(
                            label = "CLOSE",
                            icon = Icons.Filled.Close,
                            bgColor = Color(0xFF2D161D),
                            borderColor = Color(0xFFFF5252),
                            textColor = Color(0xFFFF8A80),
                            modifier = Modifier.testTag("asset_vault_close"),
                            onClick = {
                                onStopPreview()
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VaultTab.entries.forEach { tab ->
                        val active = (selectedTab == tab)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) Color(0xFF00E5FF).copy(alpha = 0.20f) else Color(0xFF161B22))
                                .border(
                                    width = 1.dp,
                                    color = if (active) Color(0xFF00E5FF) else Color(0xFF30363D),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tab.label,
                                color = if (active) Color(0xFF00E5FF) else Color(0xFFC9D1D9),
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredAssets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No assets in this filter yet.",
                            color = Color(0xFF8B949E),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredAssets, key = { it.id }) { asset ->
                            AssetVaultCard(
                                asset = asset,
                                onPreview = { onPreviewAsset(asset.id) },
                                onLike = { onLikeAsset(asset.id) },
                                onDislike = { onDislikeAsset(asset.id) },
                                onStar = { onStarAsset(asset.id) },
                                onEquip = { onEquipAsset(asset.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetVaultCard(
    asset: CustomAssetRecord,
    onPreview: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onStar: () -> Unit,
    onEquip: () -> Unit
) {
    val borderCol = when {
        asset.isEquipped -> Color(0xFF00E5FF)
        asset.voteState == 1 -> Color(0xFF00E676)
        asset.voteState == -1 -> Color(0xFFFF5252)
        else -> Color(0xFF262C36)
    }

    val isEquippable = asset.category in listOf(
        "TURRET_FIRE",
        "TURRET_UPGRADE",
        "BACKGROUND_MUSIC",
        "TURRET_SKIN",
        "MAP_THEME"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161B22))
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Preview Badge + Title + Category/ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AssetPreviewBadge(asset)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (asset.metadata.isNotBlank()) "${asset.category} • ${asset.metadata}" else "${asset.category} • ${asset.id}",
                    color = Color(0xFF8B949E),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Row 2: Action Controls (fits cleanly on narrow 360dp portrait screens)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (asset.type == "SOUND") {
                SquircleActionButton(
                    label = "PLAY",
                    icon = Icons.Filled.PlayArrow,
                    bgColor = Color(0xFF112233),
                    borderColor = Color(0xFF00E5FF),
                    textColor = Color(0xFF00E5FF),
                    onClick = onPreview
                )
            }

            if (isEquippable) {
                SquircleActionButton(
                    label = if (asset.isEquipped) "EQUIPPED" else "USE",
                    bgColor = if (asset.isEquipped) Color(0xFF0D3321) else Color(0xFF21262D),
                    borderColor = if (asset.isEquipped) Color(0xFF00E676) else Color(0xFF484F58),
                    textColor = if (asset.isEquipped) Color(0xFF00E676) else Color(0xFFC9D1D9),
                    onClick = onEquip
                )
            }

            SquircleActionButton(
                label = if (asset.voteState == 1) "👍 LIKED" else "👍",
                bgColor = if (asset.voteState == 1) Color(0xFF0D3321) else Color(0xFF21262D),
                borderColor = if (asset.voteState == 1) Color(0xFF00E676) else Color(0xFF30363D),
                textColor = if (asset.voteState == 1) Color(0xFF00E676) else Color(0xFF8B949E),
                modifier = Modifier.testTag("vault_like_${asset.id}"),
                onClick = onLike
            )

            SquircleActionButton(
                label = if (asset.voteState == -1) "👎 DISLIKED" else "👎",
                bgColor = if (asset.voteState == -1) Color(0xFF3B1219) else Color(0xFF21262D),
                borderColor = if (asset.voteState == -1) Color(0xFFFF5252) else Color(0xFF30363D),
                textColor = if (asset.voteState == -1) Color(0xFFFF5252) else Color(0xFF8B949E),
                modifier = Modifier.testTag("vault_dislike_${asset.id}"),
                onClick = onDislike
            )

            SquircleActionButton(
                label = if (asset.isFavoriteStar) "★ STAR" else "☆",
                bgColor = if (asset.isFavoriteStar) Color(0xFF332B00) else Color(0xFF21262D),
                borderColor = if (asset.isFavoriteStar) Color(0xFFFFD600) else Color(0xFF30363D),
                textColor = if (asset.isFavoriteStar) Color(0xFFFFD600) else Color(0xFF8B949E),
                modifier = Modifier.testTag("vault_star_${asset.id}"),
                onClick = onStar
            )
        }
    }
}

@Composable
private fun AssetPreviewBadge(asset: CustomAssetRecord) {
    when (asset.category) {
        "TURRET_SKIN" -> {
            TurretSkinPreviewThumbnail(
                skinSlot = TurretSkinSlot.fromSlot(asset.slotIndex),
                isSelected = asset.isEquipped
            )
        }
        "MAP_THEME" -> {
            val (bg, laneA, laneB) = when (MapThemeSlot.fromSlot(asset.slotIndex)) {
                MapThemeSlot.SLOT_1_DEFAULT -> Triple(Color(0xFF090E17), Color(0xFF00F0FF), Color(0xFFFF6B00))
                MapThemeSlot.SLOT_2_BLUEPRINT -> Triple(Color(0xFF0B192C), Color(0xFF00E5FF), Color(0xFFFFD600))
                MapThemeSlot.SLOT_3_OUTPOST -> Triple(Color(0xFF1C1917), Color(0xFFF59E0B), Color(0xFF06B6D4))
                MapThemeSlot.SLOT_4_VOID -> Triple(Color(0xFF090514), Color(0xFFA855F7), Color(0xFFF43F5E))
                MapThemeSlot.SLOT_5_MATRIX -> Triple(Color(0xFF020B05), Color(0xFF10B981), Color(0xFF6366F1))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .border(1.5.dp, if (asset.isEquipped) Color(0xFF00FF9D) else Color(0xFF475569), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(laneA))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(laneB))
                }
            }
        }
        "BACKGROUND_MUSIC" -> {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF231736))
                    .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F2430))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SquircleActionButton(
    label: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
