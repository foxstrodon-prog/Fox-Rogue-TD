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
import com.example.audio.BuildArchiveCatalog
import com.example.audio.CustomAssetRecord
import com.example.model.ArchiveSection

private enum class VaultTab(val label: String) {
    ALL("All"),
    FIRE("Turret Fire (20)"),
    UPGRADE("Upgrades (20)"),
    BGM_SFX("BGM & SFX"),
    ARCHIVES("Archives"),
    TURRET_SKINS("Turret Skins (5)"),
    MAP_THEMES("Map Themes (5)"),
    LIKED("Liked / Starred"),
    DISLIKED("Disliked")
}

/**
 * In-Game Sound & Skin Vault Dialog (Sam's responsive squircle layout).
 * Supports build-versioned active & archived catalogs, section filters inside "Archives",
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
    var selectedArchiveSection by remember { mutableStateOf(ArchiveSection.ALL) }

    val filteredAssets = remember(assets, selectedTab, selectedArchiveSection) {
        when (selectedTab) {
            VaultTab.ALL -> assets
            VaultTab.FIRE -> assets.filter { it.category == "TURRET_FIRE" && !it.isArchived }
            VaultTab.UPGRADE -> assets.filter { it.category == "TURRET_UPGRADE" && !it.isArchived }
            VaultTab.BGM_SFX -> assets.filter { (it.category == "BACKGROUND_MUSIC" || it.category == "SFX") && !it.isArchived }
            VaultTab.ARCHIVES -> assets.filter { item ->
                item.isArchived && (selectedArchiveSection == ArchiveSection.ALL || item.archiveSection == selectedArchiveSection.id)
            }
            VaultTab.TURRET_SKINS -> assets.filter { it.category == "TURRET_SKIN" && !it.isArchived }
            VaultTab.MAP_THEMES -> assets.filter { it.category == "MAP_THEME" && !it.isArchived }
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
                            text = "${assets.size} assets • Build #61 + Archives • Firebase sync",
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

                Spacer(modifier = Modifier.height(6.dp))

                val isSynced = syncStatusText.startsWith("Synced", ignoreCase = true) ||
                    syncStatusText.startsWith("Cleared", ignoreCase = true)
                val isPendingOrSyncing = syncStatusText.contains("Syncing", ignoreCase = true) ||
                    syncStatusText.contains("Pending", ignoreCase = true) ||
                    syncStatusText.startsWith("Ratings auto-saved", ignoreCase = true) ||
                    syncStatusText.startsWith("No votes", ignoreCase = true)
                val statusAccent = when {
                    isSynced -> Color(0xFF00E676)
                    isPendingOrSyncing -> Color(0xFF38BDF8)
                    else -> Color(0xFFFF6B6B)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusAccent.copy(alpha = 0.10f))
                        .border(1.dp, statusAccent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = syncStatusText,
                        color = statusAccent,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("asset_vault_sync_status")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Top-level Category Filter Tabs
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
                                .testTag("vault_tab_${tab.name}")
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

                // Sub-section Filter Chips inside "Archives"
                if (selectedTab == VaultTab.ARCHIVES) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ArchiveSection.entries.forEach { section ->
                            val active = (selectedArchiveSection == section)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) Color(0xFFA855F7).copy(alpha = 0.22f) else Color(0xFF111827))
                                    .border(
                                        width = 1.dp,
                                        color = if (active) Color(0xFFA855F7) else Color(0xFF30363D),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .testTag("archive_section_${section.id}")
                                    .clickable { selectedArchiveSection = section }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = section.label,
                                    color = if (active) Color(0xFFE9D5FF) else Color(0xFF9CA3AF),
                                    fontSize = 10.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("asset_vault_list"),
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
        "MAP_THEME",
        "ARCHIVE"
    ) || (asset.category == "SFX" && asset.id.contains("_sfx_pack_")) ||
        (asset.category == "ARCHIVED_WAV" && (
            asset.archiveSection == ArchiveSection.TURRET_FIRE.id ||
                asset.archiveSection == ArchiveSection.TURRET_UPGRADE.id ||
                asset.id.contains("bgm")
            ))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161B22))
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Preview Badge + Title + Build Label Badge + Metadata
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AssetPreviewBadge(asset)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = asset.name,
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (asset.buildLabel.isNotBlank()) {
                        BuildTagBadge(asset)
                    }
                }
                val sectionTag = asset.archiveSection.ifBlank { asset.category }
                Text(
                    text = if (asset.metadata.isNotBlank()) "$sectionTag • ${asset.metadata}" else "$sectionTag • ${asset.id}",
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
                    modifier = Modifier.testTag("vault_play_${asset.id}"),
                    onClick = onPreview
                )
            }

            if (isEquippable) {
                SquircleActionButton(
                    label = if (asset.isEquipped) "EQUIPPED" else "USE",
                    bgColor = if (asset.isEquipped) Color(0xFF0D3321) else Color(0xFF21262D),
                    borderColor = if (asset.isEquipped) Color(0xFF00E676) else Color(0xFF484F58),
                    textColor = if (asset.isEquipped) Color(0xFF00E676) else Color(0xFFC9D1D9),
                    modifier = Modifier.testTag("vault_use_${asset.id}"),
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
private fun BuildTagBadge(asset: CustomAssetRecord) {
    val badgeColor = when {
        asset.isDefault -> Color(0xFF00E676)
        asset.buildCode == 0 -> Color(0xFFF59E0B)
        asset.buildCode == 60 -> Color(0xFFA855F7)
        else -> Color(0xFF00E5FF)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.14f))
            .border(1.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
    ) {
        Text(
            text = asset.buildLabel,
            color = badgeColor,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun AssetPreviewBadge(asset: CustomAssetRecord) {
    val effectiveSection = when (asset.category) {
        "TURRET_SKIN" -> ArchiveSection.TURRET_SKIN.id
        "MAP_THEME" -> ArchiveSection.MAP_THEME.id
        "BACKGROUND_MUSIC" -> "BACKGROUND_MUSIC"
        else -> asset.archiveSection
    }

    when {
        effectiveSection == ArchiveSection.TURRET_SKIN.id -> {
            val skinDef = BuildArchiveCatalog.resolveTurretSkin(asset.id, asset.slotIndex)
            TurretSkinPreviewThumbnail(
                skinDef = skinDef,
                isSelected = asset.isEquipped
            )
        }
        effectiveSection == ArchiveSection.MAP_THEME.id -> {
            val themeDef = BuildArchiveCatalog.resolveMapTheme(asset.id, asset.slotIndex)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(themeDef.groundColor))
                    .border(
                        1.5.dp,
                        if (asset.isEquipped) Color(0xFF00FF9D) else Color(0xFF475569),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(themeDef.laneAlphaColor)))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(themeDef.laneBetaColor)))
                }
            }
        }
        asset.category == "BACKGROUND_MUSIC" || asset.id.contains("_bgm_") -> {
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
