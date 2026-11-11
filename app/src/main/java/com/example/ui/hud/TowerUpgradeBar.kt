package com.example.ui.hud

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TowerUpgradeManager
import com.example.model.*
import com.example.ui.GameUIState
import java.util.Locale

/**
 * Minimal Floating Bottom Command Dock / Tower Upgrade Bar for Fox-Rogue-TD.
 * Provides build drawers, branching tower inspection, vector branching, and Tier 3 auras.
 */
@Composable
fun MinimalBottomDock(
    state: GameUIState,
    onDeployWave: () -> Unit,
    onToggleAutoWave: () -> Unit,
    onBuildTower: (MapTile, TowerType) -> Unit,
    onBuildTowerClass: (MapTile, TowerClass) -> Unit = { _, _ -> },
    onUpgradeToVector: (TowerInstance, TowerVector) -> Unit = { _, _ -> },
    onUpgradeToArchetype: (TowerInstance, TowerArchetype) -> Unit = { _, _ -> },
    onUpgradeToTier3Global: (TowerInstance, Int) -> Unit = { _, _ -> },
    onUpgradeToTier1: (TowerInstance, String) -> Unit = { _, _ -> },
    onUpgradeToFinalForm: (TowerInstance, String) -> Unit = { _, _ -> },
    onUpgradePath: (TowerInstance, Int) -> Unit = { _, _ -> },
    onAscendTower: (TowerInstance) -> Unit = {},
    onSellTower: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDrawerActive = state.selectedTower != null || (state.selectedTile != null && state.selectedTile.type == TileType.EMPTY)
    if (!isDrawerActive) return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    Surface(
        color = Color(0xFF0A0F1D).copy(alpha = 0.96f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .then(if (isLandscape) Modifier.fillMaxHeight().verticalScroll(scrollState) else Modifier.fillMaxWidth())
                .padding(8.dp)
        ) {
            when {
                // Placed Tower selected -> 3-Tier Hierarchy Inspector & Detailed Stat Sheet
                state.selectedTower != null -> {
                    BranchingTowerInspector(
                        tower = state.selectedTower,
                        allTowers = state.towers,
                        scrap = state.scrap,
                        activeSynergies = state.activeGlobalSynergies,
                        isLandscape = isLandscape,
                        onUpgradeToVector = { onUpgradeToVector(state.selectedTower, it) },
                        onUpgradeToArchetype = { onUpgradeToArchetype(state.selectedTower, it) },
                        onUpgradeToTier3Global = { auraIdx -> onUpgradeToTier3Global(state.selectedTower, auraIdx) },
                        onUpgradeToTier1 = { onUpgradeToTier1(state.selectedTower, it) },
                        onUpgradeToFinalForm = { onUpgradeToFinalForm(state.selectedTower, it) },
                        onSell = onSellTower,
                        onChangePriority = onChangePriority,
                        onClose = onDeselect
                    )
                }

                // Empty Buildable Tile selected -> Deploy 3-Class Tower Drawer
                state.selectedTile != null && state.selectedTile.type == TileType.EMPTY -> {
                    BuildTowerDrawer(
                        tile = state.selectedTile,
                        allTowers = state.towers,
                        scrap = state.scrap,
                        isLandscape = isLandscape,
                        onBuildClass = { onBuildTowerClass(state.selectedTile, it) },
                        onBuild = { onBuildTower(state.selectedTile, it) },
                        onClose = onDeselect
                    )
                }
            }
        }
    }
}

@Composable
fun BuildTowerDrawer(
    tile: MapTile,
    allTowers: List<TowerInstance> = emptyList(),
    scrap: Int,
    isLandscape: Boolean = false,
    onBuildClass: (TowerClass) -> Unit = {},
    onBuild: (TowerType) -> Unit = {},
    onClose: () -> Unit
) {
    val classes = TowerUpgradeManager.getTowerClasses()

    val renderCard: @Composable (TowerClass, Modifier) -> Unit = { towerClass, cardModifier ->
        val isUnlocked = TowerUpgradeManager.isClassUnlocked(towerClass, allTowers)
        val cost = towerClass.baseCost
        val canAfford = scrap >= cost
        val isEnabled = isUnlocked && canAfford
        val themeColor = Color(towerClass.color)

        val unlockCondition = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> "Unlocked"
            TowerClass.ELEMENTAL_VOID -> "Unlock: 1 Tier 3"
            TowerClass.HEAVY_SIEGE -> "Unlock: 2 Tier 3"
            TowerClass.STELLAR_COSMIC -> "Unlock: 3 Tier 3"
        }

        Card(
            onClick = {
                if (isEnabled) {
                    onBuildClass(towerClass)
                    if (towerClass == TowerClass.KINETIC_BALLISTIC) {
                        onBuild(TowerType.GATLING)
                    } else if (towerClass == TowerClass.STELLAR_COSMIC) {
                        onBuild(TowerType.STELLAR)
                    }
                }
            },
            enabled = isEnabled,
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) Color(0xFF131F33) else Color(0xFF0C1322),
                disabledContainerColor = Color(0xFF0C1322)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = cardModifier
                .testTag(if (towerClass == TowerClass.KINETIC_BALLISTIC) "build_basic_turret" else "build_class_${towerClass.name}")
                .border(
                    1.dp,
                    if (isEnabled) themeColor.copy(alpha = 0.8f) else Color(0xFF1E293B),
                    RoundedCornerShape(8.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = towerClass.shapeSymbol,
                            color = Color(towerClass.color),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = towerClass.displayName,
                            color = if (isUnlocked) (if (canAfford) Color.White else Color(0xFF94A3B8)) else Color(0xFF64748B),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!isUnlocked) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFFF2A55),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val statsPreview = when (towerClass) {
                    TowerClass.KINETIC_BALLISTIC -> "DMG: 26 | SPD: 4.4x | RNG: 2.6u\n+18% Global Speed Aura"
                    TowerClass.ELEMENTAL_VOID -> "DMG: 45 | SPD: 2.8x | RNG: 3.2u\nFreezing Slow & Armor Shred"
                    TowerClass.HEAVY_SIEGE -> "DMG: 145 | SPD: 0.8x | RNG: 4.8u\nAoE Splash & Boss Buster"
                    TowerClass.STELLAR_COSMIC -> "DMG: 180 | SPD: 2.0x | RNG: 5.5u\nCosmic Radiance & Apex Pulsar"
                }

                Text(
                    text = if (!isUnlocked) unlockCondition else statsPreview,
                    color = if (!isUnlocked) Color(0xFFFF2A55) else Color(0xFF00FF9D),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Diamond,
                            contentDescription = null,
                            tint = if (canAfford && isUnlocked) Color(0xFFFFD700) else Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$cost c",
                            color = if (canAfford && isUnlocked) Color(0xFFFFD700) else Color(0xFF64748B),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = if (!isUnlocked) "LOCKED" else if (canAfford) "BUILD" else "NEED C",
                        color = if (isEnabled) themeColor else Color(0xFF64748B),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DEPLOY DEFENSE TOWER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
                if (tile.buff != TileBuff.NONE) {
                    Text(
                        text = "TERRAIN BONUS: ${tile.buff.label} (${tile.buff.desc})",
                        color = Color(tile.buff.badgeColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isLandscape) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                classes.forEach { towerClass ->
                    renderCard(towerClass, Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                classes.chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowPair.forEach { towerClass ->
                            renderCard(towerClass, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BranchingTowerInspector(
    tower: TowerInstance,
    allTowers: List<TowerInstance>,
    scrap: Int,
    activeSynergies: List<GlobalAuraSynergy> = emptyList(),
    isLandscape: Boolean = false,
    onUpgradeToVector: (TowerVector) -> Unit = {},
    onUpgradeToArchetype: (TowerArchetype) -> Unit = {},
    onUpgradeToTier3Global: (Int) -> Unit = {},
    onUpgradeToTier1: (String) -> Unit = {},
    onUpgradeToFinalForm: (String) -> Unit = {},
    onSell: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onClose: () -> Unit
) {
    val sellValue = tower.getSellValue()
    val arch = tower.archetype

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (tower.isTier3) Color(0xFFFFB800) else Color(arch?.color ?: tower.towerClass.color))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tower.towerClass.shapeSymbol,
                            color = Color(tower.towerClass.color),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tower.getDisplayName(),
                            color = if (tower.isTier3) Color(0xFFFFB800) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = when (tower.tier) {
                            3 -> "TIER 3 [${tower.towerClass.shapeName}]: ${tower.activeAura?.name ?: "GLOBAL AURA"} ONLINE"
                            2 -> "TIER 2 [${tower.towerClass.shapeName}]: SPECIALIZED ARCHETYPE LOCKED"
                            1 -> "TIER 1 [${tower.towerClass.shapeName}]: ${tower.vector?.displayName ?: "VECTOR"} SPECIALIZED"
                            else -> "TIER 0 [${tower.towerClass.shapeName}]: ${tower.towerClass.displayName} (SELECT VECTOR)"
                        },
                        color = when (tower.tier) {
                            3 -> Color(0xFFFFB800)
                            2 -> Color(0xFF00F0FF)
                            1 -> Color(0xFF00FF9D)
                            else -> Color(0xFF94A3B8)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TargetPriority.values().forEach { prio ->
                    val isSel = tower.targetPriority == prio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) Color(0xFF00F0FF) else Color(0xFF1E293B))
                            .clickable { onChangePriority(prio) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = prio.label,
                            color = if (isSel) Color.Black else Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- DETAILED STAT SHEET (3 Sections: Base Stats, Local Upgrades, Incoming Global Multipliers) ---
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.85f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section 1: Effective Combat Stats with accurate DPS
                val effDmg = tower.getEffectiveDamage()
                val effSpd = tower.getEffectiveFireRate()
                val effDps = tower.getEffectiveDps()
                val effRng = tower.getEffectiveCombatRange()
                Column {
                    Text(text = "COMBAT STATS (LIVE DPS)", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "DMG: ${effDmg.toInt()} | SPD: ${String.format(Locale.US, "%.1f", effSpd)}/s | DPS: ${effDps.toInt()} | RNG: ${effRng.toInt()}u", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                // Section 2: Local Upgrades
                Column {
                    Text(text = "LOCAL TIER", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "Tier ${tower.tier}/3 (${tower.vector?.displayName ?: "Base"})", color = Color(0xFF00FF9D), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
                // Section 3: Incoming Global Buffs
                Column {
                    Text(text = "GLOBAL AURAS", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text(text = "${activeSynergies.size} Active Multipliers", color = Color(0xFFFFB800), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // --- TIER PROGRESSION SELECTION CARDS ---
        when (tower.tier) {
            // TIER 0: Pick 1 of 3 Vectors (Offensive, Defensive, Support) for this class
            0 -> {
                val vectors = TowerUpgradeManager.getTier1Vectors()
                val cost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
                val canAfford = scrap >= cost

                val renderVectorCard: @Composable (TowerVector, Modifier) -> Unit = { vec, cardModifier ->
                    val statBoost = when (vec) {
                        TowerVector.OFFENSIVE -> "+45% DMG  |  +15% CRIT  |  Heavy Penetration"
                        TowerVector.DEFENSIVE -> "+35% RNG  |  +20% Slow  |  Pushback & Stun"
                        TowerVector.SUPPORT -> "+40% SPD  |  +25% SCRAP |  Range Uplink Aura"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
                            .border(1.dp, if (canAfford) Color(vec.color).copy(alpha = 0.8f) else Color(0xFF26334D), RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = vec.displayName.uppercase(),
                                    color = Color(vec.color),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${cost} c",
                                    color = if (canAfford) Color(0xFFFFD700) else Color(0xFF64748B),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = statBoost,
                                color = Color(0xFF00FF9D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                lineHeight = 11.sp,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { if (canAfford) onUpgradeToVector(vec) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(vec.color),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .testTag("upgrade_vector_${vec.idName}")
                            ) {
                                Text("SELECT ${vec.displayName.uppercase()}", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                if (isLandscape) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        vectors.forEach { vec ->
                            renderVectorCard(vec, Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        vectors.forEach { vec ->
                            renderVectorCard(vec, Modifier.weight(1f))
                        }
                    }
                }
            }

            // TIER 1: Pick 1 of 3 Archetypes for the selected Vector and Tower Class
            1 -> {
                val vec = tower.vector ?: TowerVector.OFFENSIVE
                val archetypes = TowerUpgradeManager.getTier2ArchetypesForClass(tower.towerClass, vec)

                val renderArchetypeCard: @Composable (TowerArchetype, Modifier) -> Unit = { archetype, cardModifier ->
                    val cost = TowerUpgradeManager.getTier2Cost(archetype)
                    val canAfford = scrap >= cost

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
                            .border(1.dp, if (canAfford) Color(archetype.color).copy(alpha = 0.8f) else Color(0xFF26334D), RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = archetype.displayName,
                                    color = Color(archetype.color),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${cost} c",
                                    color = if (canAfford) Color(0xFFFFD700) else Color(0xFF64748B),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "DMG: ${archetype.baseDamage.toInt()} | SPD: ${archetype.baseFireRate}x | RNG: ${archetype.baseRange}u",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp
                            )

                            Surface(
                                color = Color(archetype.color).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Auras: ${archetype.primaryAura.shortBadge} / ${archetype.secondaryAura.shortBadge}",
                                    color = Color(archetype.color),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Button(
                                onClick = { if (canAfford) onUpgradeToArchetype(archetype) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(archetype.color),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .testTag("upgrade_archetype_${archetype.archetypeId}")
                            ) {
                                Text("LOCK IDENTITY", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                if (isLandscape) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        archetypes.forEach { archetype ->
                            renderArchetypeCard(archetype, Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        archetypes.forEach { archetype ->
                            renderArchetypeCard(archetype, Modifier.weight(1f))
                        }
                    }
                }
            }

            // TIER 2: Upgrade to Tier 3 Global Aura Projection (Choose between 2 unique auras!)
            2 -> {
                val archetype = tower.archetype ?: mapTypeToArchetype(tower.type)
                val cost = TowerUpgradeManager.getTier3GlobalCost(tower, allTowers)
                val canAfford = scrap >= cost

                val renderAuraCard: @Composable (AuraOption, Int, Modifier) -> Unit = { aura, auraIndex, cardModifier ->
                    val auraColor = if (auraIndex == 0) Color(0xFFFFB800) else Color(0xFF00FF9D)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F33)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = cardModifier
                            .border(
                                1.dp,
                                if (canAfford) auraColor else Color(0xFF26334D),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = aura.name,
                                    color = auraColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = auraColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = aura.shortBadge,
                                        color = auraColor,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = aura.description,
                                color = Color.White,
                                fontSize = 7.5.sp,
                                lineHeight = 9.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { if (canAfford) onUpgradeToTier3Global(auraIndex) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = auraColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .testTag(if (auraIndex == 0) "upgrade_tier3_global" else "upgrade_tier3_aura_1")
                            ) {
                                Text("ACTIVATE AURA", fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CHOOSE TIER 3 GLOBAL AURA PROJECTION (${cost} c)",
                        color = Color(0xFFFFB800),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    if (isLandscape) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            renderAuraCard(archetype.primaryAura, 0, Modifier.fillMaxWidth())
                            renderAuraCard(archetype.secondaryAura, 1, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            renderAuraCard(archetype.primaryAura, 0, Modifier.weight(1f))
                            renderAuraCard(archetype.secondaryAura, 1, Modifier.weight(1f))
                        }
                    }
                }
            }

            // TIER 3: Apex Form Active with Selected Aura
            3 -> {
                val aura = tower.activeAura ?: (tower.archetype ?: mapTypeToArchetype(tower.type)).primaryAura
                Surface(
                    color = Color(0xFFFFB800).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌟 ${aura.name} ACTIVE",
                                color = Color(0xFFFFB800),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                            Text(
                                text = aura.description,
                                color = Color.White,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF00FF9D).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "[${aura.shortBadge}]",
                                color = Color(0xFF00FF9D),
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sell Button & Combat Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kills: ${tower.kills} | Dmg: ${tower.totalDamage.toInt()}",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp
            )

            OutlinedButton(
                onClick = onSell,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("sell_tower_button")
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("SELL (+$sellValue)", fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
            }
        }
    }
}

@Composable
fun ThreePathTowerInspector(
    tower: TowerInstance,
    allTowers: List<TowerInstance>,
    scrap: Int,
    onUpgradePath: (Int) -> Unit,
    onAscend: () -> Unit,
    onSell: () -> Unit,
    onChangePriority: (TargetPriority) -> Unit,
    onClose: () -> Unit
) {
    BranchingTowerInspector(
        tower = tower,
        allTowers = allTowers,
        scrap = scrap,
        onUpgradeToTier1 = { branch ->
            val idx = when (branch) { "A" -> 0; "B" -> 1; else -> 2 }
            onUpgradePath(idx)
        },
        onUpgradeToFinalForm = {
            onAscend()
        },
        onSell = onSell,
        onChangePriority = onChangePriority,
        onClose = onClose
    )
}
