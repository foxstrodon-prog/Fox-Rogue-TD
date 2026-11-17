package com.example.engine.coordinator

import com.example.audio.CustomAssetRecord
import com.example.audio.MusicTrack
import com.example.audio.SoundManager
import com.example.engine.TelemetryManager
import com.example.engine.network.AssetVoteSyncManager
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
            musicTrack = s.musicTrack
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

    fun equipAsset(assetId: String) {
        val asset = assetLibraryFlow.value.firstOrNull { it.id == assetId } ?: return
        val slot = asset.slotIndex.coerceIn(1, 5)
        when (asset.category) {
            "TURRET_FIRE" -> {
                when (TowerClass.entries.getOrElse(asset.towerClassOrdinal) { TowerClass.KINETIC_BALLISTIC }) {
                    TowerClass.KINETIC_BALLISTIC -> updateSlotState { cfg, st ->
                        st.copy(kineticSfxSlot = slot, balanceConfig = cfg.copy(kineticSfxSlot = slot))
                    }
                    TowerClass.ELEMENTAL_VOID -> updateSlotState { cfg, st ->
                        st.copy(elementalSfxSlot = slot, balanceConfig = cfg.copy(elementalSfxSlot = slot))
                    }
                    TowerClass.HEAVY_SIEGE -> updateSlotState { cfg, st ->
                        st.copy(siegeSfxSlot = slot, balanceConfig = cfg.copy(siegeSfxSlot = slot))
                    }
                    TowerClass.STELLAR_COSMIC -> updateSlotState { cfg, st ->
                        st.copy(stellarSfxSlot = slot, balanceConfig = cfg.copy(stellarSfxSlot = slot))
                    }
                }
            }
            "TURRET_SKIN", "TURRET_UPGRADE" -> {
                updateSlotState { cfg, st ->
                    st.copy(turretSkinSlot = slot, balanceConfig = cfg.copy(turretSkinSlot = slot))
                }
            }
            "MAP_THEME" -> {
                updateSlotState { cfg, st ->
                    st.copy(mapThemeSlot = slot, balanceConfig = cfg.copy(mapThemeSlot = slot))
                }
            }
            "BACKGROUND_MUSIC" -> {
                val track = MusicTrack.entries.firstOrNull { "bgm_${it.name.lowercase()}" == asset.id } ?: return
                soundManager.activeMusicTrack = track
                updateSlotState { cfg, st ->
                    st.copy(musicTrack = track, balanceConfig = cfg.copy(musicTrackName = track.name))
                }
            }
        }
        syncEquippedFromState()
        soundManager.exportLibraryJsonAsync()
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
