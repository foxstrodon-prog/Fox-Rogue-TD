package com.example.audio

import android.content.Context
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
 * Owns the Sound & Skin Vault asset library state, equipped slot synchronization,
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

    @Volatile
    var equippedTurretSkinSlot: Int = 1
        private set

    @Volatile
    var equippedMapThemeSlot: Int = 1
        private set

    private var assetLibrary: List<CustomAssetRecord> = emptyList()

    private val _assetLibraryFlow = MutableStateFlow<List<CustomAssetRecord>>(emptyList())
    val assetLibraryFlow: StateFlow<List<CustomAssetRecord>> = _assetLibraryFlow.asStateFlow()

    fun getEquippedFireSlot(towerClass: TowerClass): Int =
        synchronized(libraryLock) { equippedFireSlotByClass[towerClass.ordinal.coerceIn(0, 3)] }

    fun getEquippedUpgradeSlot(towerClass: TowerClass): Int =
        synchronized(libraryLock) { equippedUpgradeSlotByClass[towerClass.ordinal.coerceIn(0, 3)] }

    fun updateMusicEquippedStateInMemory(musicTrack: MusicTrack) {
        synchronized(libraryLock) {
            if (assetLibrary.isEmpty()) return
            val updated = assetLibrary.map { item ->
                if (item.category == "BACKGROUND_MUSIC") {
                    item.copy(isEquipped = ("bgm_${musicTrack.name.lowercase()}" == item.id))
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
            val inMemoryMap = assetLibrary.associateBy { it.id }
            val freshRecords = archiveStore.buildAssetLibrary(
                activeProfile = activeProfile,
                activeMusicTrack = activeMusicTrack,
                equippedFireSlots = equippedFireSlotByClass,
                equippedUpgradeSlots = equippedUpgradeSlotByClass,
                equippedTurretSkinSlot = equippedTurretSkinSlot,
                equippedMapThemeSlot = equippedMapThemeSlot
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
        musicTrack: MusicTrack
    ) {
        synchronized(libraryLock) {
            equippedFireSlotByClass[0] = kineticSlot.coerceIn(1, 5)
            equippedFireSlotByClass[1] = elementalSlot.coerceIn(1, 5)
            equippedFireSlotByClass[2] = siegeSlot.coerceIn(1, 5)
            equippedFireSlotByClass[3] = stellarSlot.coerceIn(1, 5)
            for (c in 0..3) {
                equippedUpgradeSlotByClass[c] = turretSkinSlot.coerceIn(1, 5)
            }
            equippedTurretSkinSlot = turretSkinSlot.coerceIn(1, 5)
            equippedMapThemeSlot = mapThemeSlot.coerceIn(1, 5)

            val updated = assetLibrary.map { item ->
                val equippedNow = when (item.category) {
                    "TURRET_FIRE" -> equippedFireSlotByClass.getOrElse(item.towerClassOrdinal) { 1 } == item.slotIndex
                    "TURRET_UPGRADE" -> equippedUpgradeSlotByClass.getOrElse(item.towerClassOrdinal) { 1 } == item.slotIndex
                    "BACKGROUND_MUSIC" -> "bgm_${musicTrack.name.lowercase()}" == item.id
                    "TURRET_SKIN" -> item.slotIndex == equippedTurretSkinSlot
                    "MAP_THEME" -> item.slotIndex == equippedMapThemeSlot
                    else -> item.isEquipped
                }
                item.copy(isEquipped = equippedNow)
            }
            assetLibrary = updated
            _assetLibraryFlow.value = updated
        }
    }

    fun setAssetVote(assetId: String, targetVote: Int): Int {
        val coerced = targetVote.coerceIn(-1, 1)
        var resultingVote = 0
        val snapshotToExport: List<CustomAssetRecord>
        synchronized(libraryLock) {
            val now = System.currentTimeMillis()
            val updated = assetLibrary.map { item ->
                if (item.id == assetId) {
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
        var resultingStar = false
        val snapshotToExport: List<CustomAssetRecord>
        synchronized(libraryLock) {
            val now = System.currentTimeMillis()
            val updated = assetLibrary.map { item ->
                if (item.id == assetId) {
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
