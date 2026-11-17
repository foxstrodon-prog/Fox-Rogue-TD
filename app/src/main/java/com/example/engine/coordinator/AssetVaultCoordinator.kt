package com.example.engine.coordinator

import com.example.audio.BuildArchiveCatalog
import com.example.audio.CustomAssetRecord
import com.example.audio.MusicTrack
import com.example.audio.SoundManager
import com.example.audio.SoundProfile
import com.example.engine.TelemetryManager
import com.example.engine.network.AssetVoteSyncManager
import com.example.model.ArchiveSection
import com.example.model.TowerClass
import com.example.ui.GameUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates the Sound & Skin Vault between [SoundManager], [GameUIState], [TelemetryManager],
 * and [AssetVoteSyncManager] while preserving strict UDF and keeping `RogueTDViewModel` compact.
 */
class AssetVaultCoordinator(
    private val uiStateFlow: MutableStateFlow<GameUIState>,
    private val soundManager: SoundManager,
    private val telemetryManager: TelemetryManager,
    private val voteSyncManager: AssetVoteSyncManager,
    private val scope: CoroutineScope
) {
    val assetLibraryFlow: StateFlow<List<CustomAssetRecord>> = soundManager.assetLibraryFlow

    private val _syncStatusFlow = MutableStateFlow(
        if (voteSyncManager.hasPendingSync()) {
            "Saved locally • Pending cloud sync"
        } else {
            "Ratings auto-saved locally"
        }
    )
    val syncStatusFlow: StateFlow<String> = _syncStatusFlow.asStateFlow()

    private val syncLock = Any()
    private var autoSyncJob: Job? = null
    private var queuedFollowUpForce: Boolean? = null

    fun onVaultOpened() {
        syncEquippedFromState()
        syncPendingVotesIfNeeded()
    }

    fun syncPendingVotesIfNeeded() {
        if (voteSyncManager.hasPendingSync()) {
            syncVotesToCloud(force = false)
        }
    }

    fun syncEquippedFromState() {
        val s = uiStateFlow.value
        soundManager.syncEquippedSlots(
            kineticSlot = s.kineticSfxSlot,
            elementalSlot = s.elementalSfxSlot,
            siegeSlot = s.siegeSfxSlot,
            stellarSlot = s.stellarSfxSlot,
            turretSkinSlot = s.turretSkinSlot,
            mapThemeSlot = s.mapThemeSlot,
            musicTrack = s.musicTrack,
            kineticFireId = s.kineticFireId,
            elementalFireId = s.elementalFireId,
            siegeFireId = s.siegeFireId,
            stellarFireId = s.stellarFireId,
            turretSkinId = s.turretSkinId,
            mapThemeId = s.mapThemeId,
            bgmTrackId = s.bgmTrackId,
            sfxPackId = s.sfxPackId
        )
    }

    fun previewAsset(assetId: String) {
        soundManager.previewAsset(assetId)
    }

    fun stopPreview() {
        soundManager.stopPreview()
    }

    fun setAssetVote(assetId: String, targetVote: Int) {
        soundManager.setAssetVote(assetId, targetVote)
        voteSyncManager.markPendingSync()
        _syncStatusFlow.value = "Saved locally • Pending cloud sync"
        scheduleAutoSync()
    }

    fun toggleAssetStar(assetId: String) {
        soundManager.toggleAssetFavoriteStar(assetId)
        voteSyncManager.markPendingSync()
        _syncStatusFlow.value = "Saved locally • Pending cloud sync"
        scheduleAutoSync()
    }

    private fun scheduleAutoSync(debounceMs: Long = 1_500L) {
        synchronized(syncLock) {
            autoSyncJob?.cancel()
            autoSyncJob = scope.launch {
                val cooldownRemaining = voteSyncManager.getCooldownRemainingMs()
                val waitMs = maxOf(debounceMs, if (cooldownRemaining > 0L) cooldownRemaining + 150L else 0L)
                delay(waitMs)
                syncVotesToCloud(force = false)
            }
        }
    }

    fun equipAsset(rawAssetId: String) {
        val canonicalId = BuildArchiveCatalog.migrateLegacyAssetId(rawAssetId)
        val asset = assetLibraryFlow.value.firstOrNull { it.id == canonicalId || it.id == rawAssetId } ?: return
        val slot = asset.slotIndex.coerceIn(1, 5)
        val effectiveSection = when (asset.category) {
            "TURRET_FIRE" -> ArchiveSection.TURRET_FIRE.id
            "TURRET_UPGRADE" -> ArchiveSection.TURRET_UPGRADE.id
            "BACKGROUND_MUSIC", "SFX" -> ArchiveSection.BGM_SFX.id
            "TURRET_SKIN" -> ArchiveSection.TURRET_SKIN.id
            "MAP_THEME" -> ArchiveSection.MAP_THEME.id
            else -> asset.archiveSection.ifBlank { ArchiveSection.BGM_SFX.id }
        }

        when {
            effectiveSection == ArchiveSection.TURRET_FIRE.id -> {
                val towerClass = inferTowerClass(asset)
                when (towerClass) {
                    TowerClass.KINETIC_BALLISTIC -> updateSlotState { cfg, st ->
                        st.copy(
                            kineticSfxSlot = slot,
                            kineticFireId = asset.id,
                            balanceConfig = cfg.copy(kineticSfxSlot = slot, kineticFireId = asset.id)
                        )
                    }
                    TowerClass.ELEMENTAL_VOID -> updateSlotState { cfg, st ->
                        st.copy(
                            elementalSfxSlot = slot,
                            elementalFireId = asset.id,
                            balanceConfig = cfg.copy(elementalSfxSlot = slot, elementalFireId = asset.id)
                        )
                    }
                    TowerClass.HEAVY_SIEGE -> updateSlotState { cfg, st ->
                        st.copy(
                            siegeSfxSlot = slot,
                            siegeFireId = asset.id,
                            balanceConfig = cfg.copy(siegeSfxSlot = slot, siegeFireId = asset.id)
                        )
                    }
                    TowerClass.STELLAR_COSMIC -> updateSlotState { cfg, st ->
                        st.copy(
                            stellarSfxSlot = slot,
                            stellarFireId = asset.id,
                            balanceConfig = cfg.copy(stellarSfxSlot = slot, stellarFireId = asset.id)
                        )
                    }
                }
            }
            effectiveSection == ArchiveSection.TURRET_SKIN.id || effectiveSection == ArchiveSection.TURRET_UPGRADE.id -> {
                val resolvedSkinId = if (effectiveSection == ArchiveSection.TURRET_SKIN.id) {
                    asset.id
                } else if (asset.isArchived && asset.buildCode >= 60) {
                    if (slot == 1 && asset.buildCode >= 61) "default_skin_1" else "b${asset.buildCode}_skin_$slot"
                } else {
                    BuildArchiveCatalog.buildActiveSkinId(slot)
                }
                updateSlotState { cfg, st ->
                    st.copy(
                        turretSkinSlot = slot,
                        turretSkinId = resolvedSkinId,
                        balanceConfig = cfg.copy(turretSkinSlot = slot, turretSkinId = resolvedSkinId)
                    )
                }
            }
            effectiveSection == ArchiveSection.MAP_THEME.id -> {
                updateSlotState { cfg, st ->
                    st.copy(
                        mapThemeSlot = slot,
                        mapThemeId = asset.id,
                        balanceConfig = cfg.copy(mapThemeSlot = slot, mapThemeId = asset.id)
                    )
                }
            }
            effectiveSection == ArchiveSection.BGM_SFX.id && (asset.category == "BACKGROUND_MUSIC" || asset.id.contains("_bgm_")) -> {
                val bgmDef = BuildArchiveCatalog.resolveBgmTrack(asset.id)
                val track = bgmDef.legacyTrackName?.let { name ->
                    runCatching { MusicTrack.valueOf(name) }.getOrNull()
                } ?: MusicTrack.CYBER_AMBIENT
                updateSlotState { cfg, st ->
                    st.copy(
                        musicTrack = track,
                        bgmTrackId = asset.id,
                        balanceConfig = cfg.copy(musicTrackName = track.name, bgmTrackId = asset.id)
                    )
                }
                syncEquippedFromState()
                soundManager.playBgmByAssetId(asset.id)
                soundManager.exportLibraryJsonAsync()
                return
            }
            effectiveSection == ArchiveSection.BGM_SFX.id && (asset.id.contains("_sfx")) -> {
                val pack = BuildArchiveCatalog.resolveSfxPack(asset.id)
                val profile = runCatching { SoundProfile.valueOf(pack.legacyProfileName) }.getOrDefault(SoundProfile.CYBER_SYNTH)
                soundManager.activeProfile = profile
                updateSlotState { cfg, st ->
                    st.copy(
                        soundProfile = profile,
                        sfxPackId = asset.id,
                        balanceConfig = cfg.copy(sfxPackId = asset.id)
                    )
                }
            }
        }
        syncEquippedFromState()
        soundManager.exportLibraryJsonAsync()
    }

    private fun inferTowerClass(asset: CustomAssetRecord): TowerClass {
        if (asset.towerClassOrdinal in 0..3) {
            return TowerClass.entries[asset.towerClassOrdinal]
        }
        val lower = asset.id.lowercase()
        return when {
            lower.contains("elemental") -> TowerClass.ELEMENTAL_VOID
            lower.contains("siege") -> TowerClass.HEAVY_SIEGE
            lower.contains("stellar") -> TowerClass.STELLAR_COSMIC
            else -> TowerClass.KINETIC_BALLISTIC
        }
    }

    private inline fun updateSlotState(crossinline transform: (com.example.model.DevBalanceConfig, GameUIState) -> GameUIState) {
        uiStateFlow.update { prev ->
            val next = transform(prev.balanceConfig, prev)
            telemetryManager.saveBalanceConfig(next.balanceConfig)
            next
        }
    }

    fun syncVotesToCloud(force: Boolean = false) {
        synchronized(syncLock) {
            autoSyncJob?.cancel()
            autoSyncJob = null
        }
        if (force) {
            _syncStatusFlow.value = "Syncing to Firebase..."
        }
        voteSyncManager.syncVotesToFirestore(
            scope = scope,
            records = assetLibraryFlow.value,
            force = force
        ) { ok, msg ->
            val hasPending = voteSyncManager.hasPendingSync()
            if (!ok && msg.contains("already in progress", ignoreCase = true)) {
                synchronized(syncLock) {
                    queuedFollowUpForce = (queuedFollowUpForce ?: false) || force
                }
                if (hasPending) {
                    _syncStatusFlow.value = "Saved locally • Pending cloud sync"
                }
                return@syncVotesToFirestore
            }

            _syncStatusFlow.value = if (ok && hasPending) {
                "Saved locally • Pending cloud sync"
            } else {
                msg
            }

            val followUpForce = synchronized(syncLock) {
                val queued = queuedFollowUpForce
                queuedFollowUpForce = null
                queued
            }

            if (followUpForce == true && (ok || hasPending)) {
                syncVotesToCloud(force = true)
            } else if (hasPending && (followUpForce != null || ok || (!force && msg.contains("cooldown", ignoreCase = true)))) {
                scheduleAutoSync(debounceMs = 0L)
            }
        }
    }
}
