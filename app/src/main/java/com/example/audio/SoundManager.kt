package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.model.TowerClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Hybrid Audio Engine:
 * - [MusicAndPreviewPlayer] for BGM streaming, rolling playlist, fades, ducking, and archive previews.
 * - [SoundPool] for low-latency SFX: placement, upgrades, and 20 turret firing audio variants.
 * - [SoundArchiveStore] for permanent `.wav` persistence under `filesDir/sound_archive`,
 *   legacy `cacheDir` migration, and local Like/Dislike/Star JSON persistence.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext
    private val archiveStore = SoundArchiveStore(appContext)
    private val libraryLock = Any()
    private val exportLock = Any()
    private val exportWriteLock = Any()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    internal var migrationJob: Job? = null

    private var latestScheduledSeq: Long = 0L
    private var lastWrittenSeq: Long = 0L
    private var latestExportSnapshot: List<CustomAssetRecord>? = null
    private var activeExportJob: Job? = null

    // --- SoundPool SFX Engine ---
    private var soundPool: SoundPool
    private val genericSoundIds = IntArray(SoundFx.entries.size)
    private val genericLoaded = BooleanArray(SoundFx.entries.size)
    private val lastGenericPlayNs = LongArray(SoundFx.entries.size)

    private val kineticSoundIds = IntArray(5)
    private val kineticLoaded = BooleanArray(5)
    private val elementalSoundIds = IntArray(5)
    private val elementalLoaded = BooleanArray(5)
    private val siegeSoundIds = IntArray(5)
    private val siegeLoaded = BooleanArray(5)
    private val stellarSoundIds = IntArray(5)
    private val stellarLoaded = BooleanArray(5)

    private val upgradeSoundIds = Array(4) { IntArray(5) }
    private val upgradeLoaded = Array(4) { BooleanArray(5) }

    private val equippedFireSlotByClass = IntArray(4) { 1 }
    private val equippedUpgradeSlotByClass = IntArray(4) { 1 }

    @Volatile
    var equippedTurretSkinSlot: Int = 1
        private set

    @Volatile
    var equippedMapThemeSlot: Int = 1
        private set

    private val lastTurretFirePlayMs = LongArray(4)
    private val minTurretFireIntervalMs = 65L
    private val sfxLoadTargetMap = ConcurrentHashMap<Int, Triple<Int, Int, Int>>()

    @Volatile
    private var turretAssetsLoaded: Boolean = false

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var isMuted: Boolean = false
        set(value) {
            val wasMuted = field
            field = value
            musicPlayer.updateMusicVolume()
            if (!value && wasMuted && !musicPlayer.hasActiveMediaPlayer() && enabled && !isAudioSuspended && activeMusicTrack != MusicTrack.NONE) {
                val trackToPlay = activeMusicTrack
                ioScope.launch {
                    musicPlayer.playTrack(trackToPlay)
                }
            }
        }

    @Volatile
    var masterVolume: Float = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            musicPlayer.updateMusicVolume()
        }

    @Volatile
    var sfxVolume: Float = 0.8f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    @Volatile
    var upgradeVolume: Float = 0.85f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    @Volatile
    var musicVolume: Float = 0.45f
        set(value) {
            field = value.coerceIn(0f, 1f)
            musicPlayer.updateMusicVolume()
        }

    @Volatile
    var isPaused: Boolean = false
        set(value) {
            field = value
            musicPlayer.updateMusicVolumeTarget()
            musicPlayer.ensureBgmPlayingIfEligible()
        }

    @Volatile
    var isAudioSuspended: Boolean = false

    var isDucked: Boolean
        get() = musicPlayer.isDucked
        set(value) {
            musicPlayer.isDucked = value
        }

    @Volatile
    var activeProfile: SoundProfile = SoundProfile.CYBER_SYNTH
        set(value) {
            if (field == value) return
            field = value
            loadAllSfx(value)
            populateAssetLibrary()
        }

    @Volatile
    var activeMusicTrack: MusicTrack = MusicTrack.ROLLING_PLAYLIST
        set(value) {
            if (field == value) return
            field = value
            musicPlayer.resetPlaylistElapsed()
            ioScope.launch {
                musicPlayer.playTrack(value)
            }
            populateAssetLibrary()
        }

    private val musicPlayer = MusicAndPreviewPlayer(
        archiveStore = archiveStore,
        isEnabled = { enabled },
        isMuted = { isMuted },
        isSuspended = { isAudioSuspended },
        isPaused = { isPaused },
        getMasterVolume = { masterVolume },
        getSfxVolume = { sfxVolume },
        getMusicVolume = { musicVolume },
        getActiveMusicTrack = { activeMusicTrack }
    )

    private var assetLibrary: List<CustomAssetRecord> = emptyList()

    private val _assetLibraryFlow = MutableStateFlow<List<CustomAssetRecord>>(emptyList())
    val assetLibraryFlow: StateFlow<List<CustomAssetRecord>> = _assetLibraryFlow.asStateFlow()

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(24)
            .setAudioAttributes(attrs)
            .build()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                sfxLoadTargetMap[sampleId]?.let { target ->
                    when (target.first) {
                        0 -> genericLoaded[target.second] = true
                        1 -> kineticLoaded[target.second] = true
                        2 -> elementalLoaded[target.second] = true
                        3 -> siegeLoaded[target.second] = true
                        4 -> stellarLoaded[target.second] = true
                        5 -> upgradeLoaded[target.second][target.third] = true
                    }
                }
            }
        }

        loadAllSfx(activeProfile)
        populateAssetLibrary()

        val initialTrack = activeMusicTrack
        ioScope.launch {
            if (activeMusicTrack == initialTrack) {
                musicPlayer.playTrack(initialTrack)
            }
        }

        migrationJob = ioScope.launch {
            val migratedCount = archiveStore.migrateLegacyCacheWavsToArchive()
            if (migratedCount > 0) {
                populateAssetLibrary()
            }
        }
    }

    fun getArchiveDirectory(): File = archiveStore.archiveDir

    fun getEquippedFireSlot(towerClass: TowerClass): Int =
        synchronized(libraryLock) { equippedFireSlotByClass[towerClass.ordinal.coerceIn(0, 3)] }

    fun getEquippedUpgradeSlot(towerClass: TowerClass): Int =
        synchronized(libraryLock) { equippedUpgradeSlotByClass[towerClass.ordinal.coerceIn(0, 3)] }

    fun populateAssetLibrary() {
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

    fun previewAsset(assetId: String) {
        val asset = assetLibraryFlow.value.firstOrNull { it.id == assetId } ?: return
        when (asset.category) {
            "TURRET_FIRE" -> {
                musicPlayer.stopPreview()
                val tClass = TowerClass.entries.getOrElse(asset.towerClassOrdinal) { TowerClass.KINETIC_BALLISTIC }
                playTurretTestSound(tClass, asset.slotIndex)
            }
            "TURRET_UPGRADE" -> {
                musicPlayer.stopPreview()
                val tClass = TowerClass.entries.getOrElse(asset.towerClassOrdinal) { TowerClass.KINETIC_BALLISTIC }
                playTowerUpgradeTestSound(tClass, asset.slotIndex)
            }
            "SFX" -> {
                musicPlayer.stopPreview()
                SoundFx.entries.getOrNull(asset.slotIndex)?.let { play(it) }
            }
            "BACKGROUND_MUSIC" -> {
                val track = MusicTrack.entries.firstOrNull { "bgm_${it.name.lowercase()}" == asset.id } ?: return
                val targetTrack = if (track == MusicTrack.ROLLING_PLAYLIST) musicPlayer.getCurrentRollingTrack() else track
                val generation = musicPlayer.nextPreviewGeneration()
                ioScope.launch {
                    val file = musicPlayer.getOrCreateMusicFile(targetTrack)
                    musicPlayer.playTransientWavFile(file, expectedGeneration = generation)
                }
            }
            "ARCHIVED_WAV" -> {
                val path = asset.filePath ?: return
                val generation = musicPlayer.nextPreviewGeneration()
                ioScope.launch {
                    musicPlayer.playTransientWavFile(File(path), expectedGeneration = generation)
                }
            }
        }
    }

    fun stopPreview() = musicPlayer.stopPreview()

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

    fun toggleAssetFavoriteHeart(assetId: String): Boolean = setAssetVote(assetId, 1) == 1

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

    private fun scheduleExportLibraryJson(records: List<CustomAssetRecord>) {
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

    // ---------------------------------------------------------------------------------------------
    // SFX Loading & Playback
    // ---------------------------------------------------------------------------------------------

    @Synchronized
    private fun loadAllSfx(profile: SoundProfile) {
        for (i in genericSoundIds.indices) {
            val prevId = genericSoundIds[i]
            genericLoaded[i] = false
            if (prevId != 0) {
                sfxLoadTargetMap.remove(prevId)
                try { soundPool.unload(prevId) } catch (_: Exception) {}
                genericSoundIds[i] = 0
            }
        }

        for (fx in SoundFx.entries) {
            try {
                val file = archiveStore.getGenericSfxFile(profile, fx)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use {
                        it.write(AudioSynthesizer.toWav(ProceduralAudioSynthesizer.synthesizeGeneric(fx, profile)))
                    }
                }
                val id = soundPool.load(file.absolutePath, 1)
                genericSoundIds[fx.ordinal] = id
                sfxLoadTargetMap[id] = Triple(0, fx.ordinal, 0)
            } catch (_: Exception) {
                genericSoundIds[fx.ordinal] = 0
            }
        }

        if (turretAssetsLoaded) return
        turretAssetsLoaded = true

        for (slot in 1..5) {
            loadVariantSfx(archiveStore.getGunfireFile(TowerClass.KINETIC_BALLISTIC, slot), kineticSoundIds, slot - 1, 1) {
                ProceduralAudioSynthesizer.synthesizeKineticVariant(slot)
            }
            loadVariantSfx(archiveStore.getGunfireFile(TowerClass.ELEMENTAL_VOID, slot), elementalSoundIds, slot - 1, 2) {
                ProceduralAudioSynthesizer.synthesizeElementalVariant(slot)
            }
            loadVariantSfx(archiveStore.getGunfireFile(TowerClass.HEAVY_SIEGE, slot), siegeSoundIds, slot - 1, 3) {
                ProceduralAudioSynthesizer.synthesizeSiegeVariant(slot)
            }
            loadVariantSfx(archiveStore.getGunfireFile(TowerClass.STELLAR_COSMIC, slot), stellarSoundIds, slot - 1, 4) {
                ProceduralAudioSynthesizer.synthesizeStellarVariant(slot)
            }
        }

        val classes = listOf(
            TowerClass.KINETIC_BALLISTIC,
            TowerClass.ELEMENTAL_VOID,
            TowerClass.HEAVY_SIEGE,
            TowerClass.STELLAR_COSMIC
        )
        for (c in classes.indices) {
            val tClass = classes[c]
            for (slot in 1..5) {
                try {
                    val file = archiveStore.getUpgradeFile(tClass, slot)
                    if (!file.exists() || file.length() < 64L) {
                        FileOutputStream(file).use {
                            it.write(AudioSynthesizer.toWav(ProceduralAudioSynthesizer.synthesizeTowerUpgrade(tClass, slot)))
                        }
                    }
                    val id = soundPool.load(file.absolutePath, 1)
                    upgradeSoundIds[c][slot - 1] = id
                    sfxLoadTargetMap[id] = Triple(5, c, slot - 1)
                } catch (_: Exception) {
                    upgradeSoundIds[c][slot - 1] = 0
                }
            }
        }
    }

    private inline fun loadVariantSfx(
        file: File,
        targetIds: IntArray,
        slotIdx: Int,
        categoryCode: Int,
        synthesize: () -> ShortArray
    ) {
        try {
            if (!file.exists() || file.length() < 64L) {
                FileOutputStream(file).use {
                    it.write(AudioSynthesizer.toWav(synthesize()))
                }
            }
            val id = soundPool.load(file.absolutePath, 1)
            targetIds[slotIdx] = id
            sfxLoadTargetMap[id] = Triple(categoryCode, slotIdx, 0)
        } catch (_: Exception) {
            targetIds[slotIdx] = 0
        }
    }

    fun play(fx: SoundFx, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled || isMuted || isAudioSuspended) return
        val idx = fx.ordinal
        if (idx !in genericLoaded.indices || !genericLoaded[idx]) return
        val now = System.nanoTime()
        if (now - lastGenericPlayNs[idx] < fx.minIntervalMs * 1_000_000L) return
        lastGenericPlayNs[idx] = now
        val effectiveChannelVol = if (fx == SoundFx.UPGRADE) upgradeVolume else sfxVolume
        val v = (volume * masterVolume * effectiveChannelVol).coerceIn(0f, 1f)
        try {
            soundPool.play(genericSoundIds[idx], v, v, 1, 0, rate.coerceIn(0.5f, 2f))
        } catch (_: Exception) {}
    }

    fun playPlacement() {
        play(SoundFx.PLACE)
    }

    fun playTurretFire(towerClass: TowerClass, slot: Int = getEquippedFireSlot(towerClass)) {
        if (!enabled || isMuted || isAudioSuspended) return
        val archetypeIdx = towerClass.ordinal.coerceIn(0, 3)
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastTurretFirePlayMs[archetypeIdx] < minTurretFireIntervalMs) return
        lastTurretFirePlayMs[archetypeIdx] = nowMs

        triggerCombatDucking(durationMs = 450L)

        val targetSlot = (slot - 1).coerceIn(0, 4)
        val (soundId, isLoaded) = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> Pair(kineticSoundIds[targetSlot], kineticLoaded[targetSlot])
            TowerClass.ELEMENTAL_VOID -> Pair(elementalSoundIds[targetSlot], elementalLoaded[targetSlot])
            TowerClass.HEAVY_SIEGE -> Pair(siegeSoundIds[targetSlot], siegeLoaded[targetSlot])
            TowerClass.STELLAR_COSMIC -> Pair(stellarSoundIds[targetSlot], stellarLoaded[targetSlot])
        }

        if (soundId != 0 && isLoaded) {
            val v = (masterVolume * sfxVolume).coerceIn(0f, 1f)
            val jitterRate = 0.96f + Random.nextFloat() * 0.08f
            try {
                soundPool.play(soundId, v, v, 2, 0, jitterRate)
            } catch (_: Exception) {}
        }
    }

    fun playTurretTestSound(towerClass: TowerClass, slot: Int) {
        if (!enabled || isMuted) return
        try { soundPool.autoResume() } catch (_: Exception) {}
        val targetSlot = (slot - 1).coerceIn(0, 4)
        val (soundId, isLoaded) = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> Pair(kineticSoundIds[targetSlot], kineticLoaded[targetSlot])
            TowerClass.ELEMENTAL_VOID -> Pair(elementalSoundIds[targetSlot], elementalLoaded[targetSlot])
            TowerClass.HEAVY_SIEGE -> Pair(siegeSoundIds[targetSlot], siegeLoaded[targetSlot])
            TowerClass.STELLAR_COSMIC -> Pair(stellarSoundIds[targetSlot], stellarLoaded[targetSlot])
        }
        if (soundId != 0 && isLoaded) {
            val v = (masterVolume * sfxVolume).coerceIn(0f, 1f)
            try {
                soundPool.play(soundId, v, v, 3, 0, 1.0f)
            } catch (_: Exception) {}
        }
    }

    fun playTowerUpgrade(towerClass: TowerClass, skinSlot: Int = getEquippedUpgradeSlot(towerClass)) {
        if (!enabled || isMuted || isAudioSuspended) return
        val c = towerClass.ordinal.coerceIn(0, 3)
        val s = (skinSlot - 1).coerceIn(0, 4)
        val soundId = upgradeSoundIds[c][s]
        val isLoaded = upgradeLoaded[c][s]
        if (soundId != 0 && isLoaded) {
            val v = (masterVolume * upgradeVolume).coerceIn(0f, 1f)
            try {
                soundPool.play(soundId, v, v, 2, 0, 1.0f)
            } catch (_: Exception) {}
        } else {
            play(SoundFx.UPGRADE)
        }
    }

    fun playTowerUpgradeTestSound(towerClass: TowerClass, skinSlot: Int = 1) {
        if (!enabled || isMuted) return
        try { soundPool.autoResume() } catch (_: Exception) {}
        val c = towerClass.ordinal.coerceIn(0, 3)
        val s = (skinSlot - 1).coerceIn(0, 4)
        val soundId = upgradeSoundIds[c][s]
        val isLoaded = upgradeLoaded[c][s]
        if (soundId != 0 && isLoaded) {
            val v = (masterVolume * upgradeVolume).coerceIn(0f, 1f)
            try {
                soundPool.play(soundId, v, v, 3, 0, 1.0f)
            } catch (_: Exception) {}
        } else {
            play(SoundFx.UPGRADE)
        }
    }

    fun suspendAllAudio() {
        isAudioSuspended = true
        musicPlayer.suspendMusic()
        try { soundPool.autoPause() } catch (_: Exception) {}
    }

    fun resumeAudio() {
        isAudioSuspended = false
        if (!enabled || isMuted) return
        try { soundPool.autoResume() } catch (_: Exception) {}
        musicPlayer.resumeMusic()
    }

    fun triggerCombatDucking(durationMs: Long = 450L) = musicPlayer.triggerCombatDucking(durationMs)

    fun updateTick(nowMs: Long = System.currentTimeMillis(), dtMs: Long = 16L) = musicPlayer.updateTick(nowMs, dtMs)

    fun stopMusic() = musicPlayer.stopMusic()

    fun calculateEffectiveMusicVolume(): Float = musicPlayer.calculateEffectiveMusicVolume()

    fun updateMusicVolume() = musicPlayer.updateMusicVolume()

    fun setMasterVolumeLevel(level: Float) {
        masterVolume = level
        musicPlayer.updateMusicVolume()
    }

    fun setSfxVolumeLevel(level: Float) {
        sfxVolume = level
    }

    fun setUpgradeVolumeLevel(level: Float) {
        upgradeVolume = level
    }

    fun setMusicVolumeLevel(level: Float) {
        musicVolume = level
        musicPlayer.updateMusicVolume()
    }

    fun setMutedState(muted: Boolean) {
        isMuted = muted
        musicPlayer.updateMusicVolume()
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        musicPlayer.updateMusicVolume()
        return isMuted
    }

    fun release() {
        ioScope.cancel()
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
        musicPlayer.release()
        try { soundPool.release() } catch (_: Exception) {}
    }
}
