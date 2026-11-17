package com.example.audio

import android.content.Context
import com.example.model.ArchiveSection
import com.example.model.TowerClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the Sound & Skin Vault versioned asset library state, active & archived equipped IDs,
 * tri-state voting mutations, and serialized asynchronous JSON exports.
 */
internal class SoundAssetLibraryController(
    private val appContext: Context,
    private val archiveStore: SoundArchiveStore,
    private val ioScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val isReleased: () -> Boolean
) {
    private val libraryLock = Any()
    private val exportLock = Any()
    private val exportWriteLock = Any()

    private var latestScheduledSeq: Long = 0L
    private var lastWrittenSeq: Long = 0L
    private var latestExportSnapshot: List<CustomAssetRecord>? = null
    private var activeExportJob: Job? = null

    @Volatile
    internal var finalFlushJob: Job? = null

    private val equippedFireSlotByClass = IntArray(4) { 1 }
    private val equippedUpgradeSlotByClass = IntArray(4) { 1 }

    private val equippedFireIdByClass = Array(4) { idx ->
        BuildArchiveCatalog.buildActiveFireId(BuildArchiveCatalog.allClasses[idx], 1)
    }
    private val equippedUpgradeIdByClass = Array(4) { idx ->
        BuildArchiveCatalog.buildActiveUpgradeId(BuildArchiveCatalog.allClasses[idx], 1)
    }

    @Volatile
    var equippedTurretSkinSlot: Int = 1
        private set

    @Volatile
    var equippedMapThemeSlot: Int = 1
        private set

    @Volatile
    var equippedTurretSkinId: String = BuildArchiveVisualCatalog.defaultTurretSkin.id
        private set

    @Volatile
    var equippedMapThemeId: String = BuildArchiveVisualCatalog.defaultMapTheme.id
        private set

    @Volatile
    var equippedBgmId: String = "default_bgm_rolling_playlist"
        private set

    @Volatile
    var equippedSfxPackId: String = "default_sfx_pack_cyber_synth"
        private set

    private var assetLibrary: List<CustomAssetRecord> = emptyList()

    private val _assetLibraryFlow = MutableStateFlow<List<CustomAssetRecord>>(emptyList())
    val assetLibraryFlow: StateFlow<List<CustomAssetRecord>> = _assetLibraryFlow.asStateFlow()

    fun getEquippedFireSlot(towerClass: TowerClass): Int =
        synchronized(libraryLock) { equippedFireSlotByClass[towerClass.ordinal.coerceIn(0, 3)] }

    fun getEquippedFireId(towerClass: TowerClass): String =
        synchronized(libraryLock) { equippedFireIdByClass[towerClass.ordinal.coerceIn(0, 3)] }

    fun getEquippedUpgradeSlot(towerClass: TowerClass): Int =
        synchronized(libraryLock) { equippedUpgradeSlotByClass[towerClass.ordinal.coerceIn(0, 3)] }

    fun getEquippedUpgradeId(towerClass: TowerClass): String =
        synchronized(libraryLock) { equippedUpgradeIdByClass[towerClass.ordinal.coerceIn(0, 3)] }

    fun updateMusicEquippedStateInMemory(musicTrack: MusicTrack, bgmAssetId: String? = null) {
        synchronized(libraryLock) {
            val resolvedId = bgmAssetId?.takeIf { it.isNotBlank() }?.let {
                BuildArchiveCatalog.migrateLegacyAssetId(it)
            } ?: when (musicTrack) {
                MusicTrack.NONE -> ""
                MusicTrack.ROLLING_PLAYLIST -> "default_bgm_rolling_playlist"
                MusicTrack.CYBER_AMBIENT -> "default_bgm_cyber_ambient"
                else -> "b60_bgm_${musicTrack.name.lowercase()}"
            }
            equippedBgmId = resolvedId
            if (assetLibrary.isEmpty()) return
            val updated = assetLibrary.map { item ->
                if (item.category == "BACKGROUND_MUSIC" ||
                    (item.category == "ARCHIVE" && item.archiveSection == ArchiveSection.BGM_SFX.id && item.id.contains("_bgm_"))
                ) {
                    item.copy(isEquipped = (resolvedId.isNotEmpty() && item.id == resolvedId))
                } else {
                    item
                }
            }
            assetLibrary = updated
            _assetLibraryFlow.value = updated
        }
    }

    fun populateAssetLibrary(activeProfile: SoundProfile, activeMusicTrack: MusicTrack) {
        synchronized(libraryLock) {
            if (equippedBgmId.isBlank() && activeMusicTrack != MusicTrack.NONE) {
                equippedBgmId = when (activeMusicTrack) {
                    MusicTrack.ROLLING_PLAYLIST -> "default_bgm_rolling_playlist"
                    MusicTrack.CYBER_AMBIENT -> "default_bgm_cyber_ambient"
                    else -> "b60_bgm_${activeMusicTrack.name.lowercase()}"
                }
            }
            val inMemoryMap = assetLibrary.associateBy { it.id }
            val freshRecords = archiveStore.buildAssetLibrary(
                activeProfile = activeProfile,
                equippedBgmId = equippedBgmId,
                equippedSfxPackId = equippedSfxPackId,
                equippedFireIds = equippedFireIdByClass,
                equippedUpgradeIds = equippedUpgradeIdByClass,
                equippedTurretSkinId = equippedTurretSkinId,
                equippedMapThemeId = equippedMapThemeId
            )
            val merged = archiveStore.applyPersistedAndInMemoryStates(freshRecords, inMemoryMap)
            assetLibrary = merged
            _assetLibraryFlow.value = merged
        }
    }

    fun importLibraryJson() {
        synchronized(libraryLock) {
            val merged = archiveStore.applyPersistedAndInMemoryStates(assetLibrary, emptyMap())
            assetLibrary = merged
            _assetLibraryFlow.value = merged
        }
    }

    fun syncEquippedSlots(
        kineticSlot: Int,
        elementalSlot: Int,
        siegeSlot: Int,
        stellarSlot: Int,
        turretSkinSlot: Int,
        mapThemeSlot: Int,
        musicTrack: MusicTrack,
        kineticFireId: String? = null,
        elementalFireId: String? = null,
        siegeFireId: String? = null,
        stellarFireId: String? = null,
        turretSkinId: String? = null,
        mapThemeId: String? = null,
        bgmTrackId: String? = null,
        sfxPackId: String? = null
    ) {
        synchronized(libraryLock) {
            val slots = intArrayOf(
                kineticSlot.coerceIn(1, 5),
                elementalSlot.coerceIn(1, 5),
                siegeSlot.coerceIn(1, 5),
                stellarSlot.coerceIn(1, 5)
            )
            val explicitFireIds = arrayOf(kineticFireId, elementalFireId, siegeFireId, stellarFireId)
            for (c in 0..3) {
                equippedFireSlotByClass[c] = slots[c]
                val explicit = explicitFireIds[c]?.takeIf { it.isNotBlank() }?.let { BuildArchiveCatalog.migrateLegacyAssetId(it) }
                equippedFireIdByClass[c] = explicit ?: BuildArchiveCatalog.buildActiveFireId(BuildArchiveCatalog.allClasses[c], slots[c])
            }

            equippedTurretSkinSlot = turretSkinSlot.coerceIn(1, 5)
            equippedMapThemeSlot = mapThemeSlot.coerceIn(1, 5)

            val resolvedSkinId = turretSkinId?.takeIf { it.isNotBlank() }?.let { BuildArchiveCatalog.migrateLegacyAssetId(it) }
                ?: BuildArchiveCatalog.buildActiveSkinId(equippedTurretSkinSlot)
            equippedTurretSkinId = resolvedSkinId

            val skinBuildCode = BuildArchiveCatalog.extractBuildCodeFromId(resolvedSkinId)
            for (c in 0..3) {
                equippedUpgradeSlotByClass[c] = equippedTurretSkinSlot
                equippedUpgradeIdByClass[c] = if (skinBuildCode != null && skinBuildCode in B60_BUILD_CODE until CURRENT_BUILD_CODE) {
                    if (equippedTurretSkinSlot == 1 && skinBuildCode >= B61_BUILD_CODE) {
                        "default_upgrade_${BuildArchiveCatalog.allClasses[c].name.lowercase()}"
                    } else {
                        "b${skinBuildCode}_upgrade_${BuildArchiveCatalog.allClasses[c].name.lowercase()}_$equippedTurretSkinSlot"
                    }
                } else {
                    BuildArchiveCatalog.buildActiveUpgradeId(BuildArchiveCatalog.allClasses[c], equippedTurretSkinSlot)
                }
            }

            equippedMapThemeId = mapThemeId?.takeIf { it.isNotBlank() }?.let { BuildArchiveCatalog.migrateLegacyAssetId(it) }
                ?: BuildArchiveCatalog.buildActiveThemeId(equippedMapThemeSlot)

            equippedBgmId = bgmTrackId?.takeIf { it.isNotBlank() }?.let { BuildArchiveCatalog.migrateLegacyAssetId(it) }
                ?: when (musicTrack) {
                    MusicTrack.NONE -> ""
                    MusicTrack.ROLLING_PLAYLIST -> "default_bgm_rolling_playlist"
                    MusicTrack.CYBER_AMBIENT -> "default_bgm_cyber_ambient"
                    else -> "b60_bgm_${musicTrack.name.lowercase()}"
                }

            sfxPackId?.takeIf { it.isNotBlank() }?.let {
                equippedSfxPackId = BuildArchiveCatalog.migrateLegacyAssetId(it)
            }

            val updated = assetLibrary.map { item ->
                val equippedNow = when {
                    item.category == "TURRET_FIRE" || (item.archiveSection == ArchiveSection.TURRET_FIRE.id && item.isArchived) -> {
                        val c = item.towerClassOrdinal
                        if (c in 0..3) equippedFireIdByClass[c] == item.id else equippedFireIdByClass.contains(item.id)
                    }
                    item.category == "TURRET_UPGRADE" || (item.archiveSection == ArchiveSection.TURRET_UPGRADE.id && item.isArchived) -> {
                        val c = item.towerClassOrdinal
                        if (c in 0..3) equippedUpgradeIdByClass[c] == item.id else equippedUpgradeIdByClass.contains(item.id)
                    }
                    item.category == "BACKGROUND_MUSIC" || (item.archiveSection == ArchiveSection.BGM_SFX.id && item.id.contains("_bgm_")) ->
                        equippedBgmId.isNotEmpty() && equippedBgmId == item.id
                    item.category == "SFX" || (item.archiveSection == ArchiveSection.BGM_SFX.id && item.id.contains("_sfx")) ->
                        equippedSfxPackId == item.id
                    item.category == "TURRET_SKIN" || item.archiveSection == ArchiveSection.TURRET_SKIN.id ->
                        equippedTurretSkinId == item.id
                    item.category == "MAP_THEME" || item.archiveSection == ArchiveSection.MAP_THEME.id ->
                        equippedMapThemeId == item.id
                    else -> item.isEquipped
                }
                item.copy(isEquipped = equippedNow)
            }
            assetLibrary = updated
            _assetLibraryFlow.value = updated
        }
    }

    fun setAssetVote(assetId: String, targetVote: Int): Int {
        val canonicalId = BuildArchiveCatalog.migrateLegacyAssetId(assetId)
        val coerced = targetVote.coerceIn(-1, 1)
        var resultingVote = 0
        val snapshotToExport: List<CustomAssetRecord>
        synchronized(libraryLock) {
            val now = System.currentTimeMillis()
            val updated = assetLibrary.map { item ->
                if (item.id == canonicalId || item.id == assetId) {
                    resultingVote = if (item.voteState == coerced) 0 else coerced
                    item.copy(
                        voteState = resultingVote,
                        isFavoriteHeart = (resultingVote == 1),
                        updatedAtEpochMs = now
                    )
                } else {
                    item
                }
            }
            assetLibrary = updated
            _assetLibraryFlow.value = updated
            snapshotToExport = updated
        }
        scheduleExportLibraryJson(snapshotToExport)
        return resultingVote
    }

    fun toggleAssetFavoriteStar(assetId: String): Boolean {
        val canonicalId = BuildArchiveCatalog.migrateLegacyAssetId(assetId)
        var resultingStar = false
        val snapshotToExport: List<CustomAssetRecord>
        synchronized(libraryLock) {
            val now = System.currentTimeMillis()
            val updated = assetLibrary.map { item ->
                if (item.id == canonicalId || item.id == assetId) {
                    resultingStar = !item.isFavoriteStar
                    item.copy(
                        isFavoriteStar = resultingStar,
                        updatedAtEpochMs = now
                    )
                } else {
                    item
                }
            }
            assetLibrary = updated
            _assetLibraryFlow.value = updated
            snapshotToExport = updated
        }
        scheduleExportLibraryJson(snapshotToExport)
        return resultingStar
    }

    fun exportLibraryJsonAsync() {
        val snapshot = synchronized(libraryLock) { assetLibrary }
        scheduleExportLibraryJson(snapshot)
    }

    private fun scheduleExportLibraryJson(records: List<CustomAssetRecord>) {
        if (isReleased()) return
        synchronized(exportLock) {
            latestScheduledSeq++
            latestExportSnapshot = records
            if (activeExportJob?.isActive == true) return
            activeExportJob = ioScope.launch {
                drainPendingExports()
            }
        }
    }

    private fun drainPendingExports() {
        while (true) {
            val hasPending = synchronized(exportLock) {
                if (lastWrittenSeq >= latestScheduledSeq || latestExportSnapshot == null) {
                    activeExportJob = null
                    false
                } else {
                    true
                }
            }
            if (!hasPending) return

            synchronized(exportWriteLock) {
                val (currentSeq, currentSnapshot) = synchronized(exportLock) {
                    latestScheduledSeq to latestExportSnapshot
                }
                if (currentSnapshot != null && currentSeq > lastWrittenSeq) {
                    archiveStore.exportLibraryJson(appContext, currentSnapshot)
                    synchronized(exportLock) {
                        if (currentSeq > lastWrittenSeq) {
                            lastWrittenSeq = currentSeq
                        }
                    }
                }
            }
        }
    }

    fun exportLibraryJson(context: Context = appContext): String {
        val snapshot = synchronized(libraryLock) { assetLibrary }
        return synchronized(exportWriteLock) {
            archiveStore.exportLibraryJson(context, snapshot)
        }
    }

    suspend fun awaitPendingIo() {
        activeExportJob?.join()
        finalFlushJob?.join()
    }

    fun flushPendingExportsOnRelease(): Job? {
        val flushSnapshot = synchronized(exportLock) {
            if (latestExportSnapshot != null && latestScheduledSeq > lastWrittenSeq) {
                latestScheduledSeq to latestExportSnapshot
            } else {
                null
            }
        } ?: return null

        val (seqToFlush, recordsToFlush) = flushSnapshot
        if (recordsToFlush == null) return null
        val job = CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            synchronized(exportWriteLock) {
                val shouldWrite = synchronized(exportLock) { seqToFlush > lastWrittenSeq }
                if (shouldWrite) {
                    archiveStore.exportLibraryJson(appContext, recordsToFlush)
                    synchronized(exportLock) {
                        if (seqToFlush > lastWrittenSeq) {
                            lastWrittenSeq = seqToFlush
                        }
                    }
                }
            }
        }
        finalFlushJob = job
        return job
    }
}
