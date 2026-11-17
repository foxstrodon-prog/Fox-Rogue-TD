package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.model.TowerClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
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
class SoundManager @JvmOverloads constructor(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val appContext = context.applicationContext
    private val archiveStore = SoundArchiveStore(appContext)
    private val soundPoolLock = Any()
    private val profileLoadLock = Any()
    private val trackSwitchLock = Any()
    private val ioScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val libraryController = SoundAssetLibraryController(
        appContext = appContext,
        archiveStore = archiveStore,
        ioScope = ioScope,
        ioDispatcher = ioDispatcher,
        isReleased = { isReleased }
    )

    @Volatile
    private var isReleased: Boolean = false

    @Volatile
    internal var initLoadJob: Job? = null

    @Volatile
    internal var profileLoadJob: Job? = null

    @Volatile
    internal var trackSwitchJob: Job? = null

    @Volatile
    internal var migrationJob: Job? = null

    @Volatile
    internal var finalFlushJob: Job? = null

    private var profileGeneration: Long = 0L

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

    val equippedTurretSkinSlot: Int
        get() = libraryController.equippedTurretSkinSlot

    val equippedMapThemeSlot: Int
        get() = libraryController.equippedMapThemeSlot

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
                requestPlayTrackAsync(activeMusicTrack, resetPlaylist = false)
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
            val gen = synchronized(profileLoadLock) {
                profileLoadJob?.cancel()
                ++profileGeneration
            }
            profileLoadJob = ioScope.launch {
                if (isReleased) return@launch
                if (synchronized(profileLoadLock) { profileGeneration } != gen) return@launch
                populateAssetLibrary()
                if (isReleased) return@launch
                loadAllSfx(value, gen)
            }
        }

    @Volatile
    var activeMusicTrack: MusicTrack = MusicTrack.ROLLING_PLAYLIST
        set(value) {
            if (field == value) return
            field = value
            requestPlayTrackAsync(value, resetPlaylist = true)
            updateMusicEquippedStateInMemory(value)
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
        getActiveMusicTrack = { activeMusicTrack },
        ioDispatcher = ioDispatcher
    )

    val assetLibraryFlow: StateFlow<List<CustomAssetRecord>> = libraryController.assetLibraryFlow

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

        val initialProfile = activeProfile
        val initialProfileGen = synchronized(profileLoadLock) { profileGeneration }
        initLoadJob = ioScope.launch {
            if (isReleased) return@launch
            populateAssetLibrary()
            if (isReleased) return@launch
            if (synchronized(profileLoadLock) { profileGeneration } == initialProfileGen) {
                loadAllSfx(initialProfile, initialProfileGen)
            }
        }

        requestPlayTrackAsync(activeMusicTrack, resetPlaylist = false)

        migrationJob = ioScope.launch {
            val migratedCount = archiveStore.migrateLegacyCacheWavsToArchive()
            if (migratedCount > 0 && !isReleased) {
                populateAssetLibrary()
            }
        }
    }

    private fun requestPlayTrackAsync(track: MusicTrack, resetPlaylist: Boolean) {
        synchronized(trackSwitchLock) {
            if (isReleased) return
            trackSwitchJob?.cancel()
            if (resetPlaylist) {
                musicPlayer.resetPlaylistElapsed()
            }
            val expectedGen = musicPlayer.nextTrackGeneration()
            if (track == MusicTrack.NONE) {
                musicPlayer.stopMusic()
                trackSwitchJob = null
                return
            }
            trackSwitchJob = ioScope.launch {
                if (!isReleased) {
                    musicPlayer.playTrack(track, expectedGeneration = expectedGen)
                }
            }
        }
    }

    private fun updateMusicEquippedStateInMemory(musicTrack: MusicTrack) =
        libraryController.updateMusicEquippedStateInMemory(musicTrack)

    internal suspend fun awaitPendingIoForTest() {
        initLoadJob?.join()
        profileLoadJob?.join()
        migrationJob?.join()
        trackSwitchJob?.join()
        libraryController.awaitPendingIo()
        finalFlushJob?.join()
    }

    fun getArchiveDirectory(): File = archiveStore.archiveDir

    fun getEquippedFireSlot(towerClass: TowerClass): Int =
        libraryController.getEquippedFireSlot(towerClass)

    fun getEquippedUpgradeSlot(towerClass: TowerClass): Int =
        libraryController.getEquippedUpgradeSlot(towerClass)

    fun populateAssetLibrary() =
        libraryController.populateAssetLibrary(activeProfile, activeMusicTrack)

    fun importLibraryJson() = libraryController.importLibraryJson()

    fun syncEquippedSlots(
        kineticSlot: Int,
        elementalSlot: Int,
        siegeSlot: Int,
        stellarSlot: Int,
        turretSkinSlot: Int,
        mapThemeSlot: Int,
        musicTrack: MusicTrack
    ) = libraryController.syncEquippedSlots(
        kineticSlot = kineticSlot,
        elementalSlot = elementalSlot,
        siegeSlot = siegeSlot,
        stellarSlot = stellarSlot,
        turretSkinSlot = turretSkinSlot,
        mapThemeSlot = mapThemeSlot,
        musicTrack = musicTrack
    )

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

    fun setAssetVote(assetId: String, targetVote: Int): Int =
        libraryController.setAssetVote(assetId, targetVote)

    fun toggleAssetFavoriteHeart(assetId: String): Boolean = setAssetVote(assetId, 1) == 1

    fun toggleAssetFavoriteStar(assetId: String): Boolean =
        libraryController.toggleAssetFavoriteStar(assetId)

    fun exportLibraryJsonAsync() = libraryController.exportLibraryJsonAsync()

    fun exportLibraryJson(context: Context = appContext): String =
        libraryController.exportLibraryJson(context)

    // ---------------------------------------------------------------------------------------------
    // SFX Loading & Playback
    // ---------------------------------------------------------------------------------------------

    private fun writeWavAtomicallyIfNeeded(file: File, synthesize: () -> ShortArray) {
        if (file.exists() && file.length() >= 64L) return
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val tmpFile = File(file.parentFile, "${file.name}.${System.nanoTime()}.tmp")
        try {
            val wavBytes = AudioSynthesizer.toWav(synthesize())
            FileOutputStream(tmpFile).use { it.write(wavBytes) }
            if (!tmpFile.renameTo(file)) {
                if (file.exists()) {
                    file.delete()
                }
                if (!tmpFile.renameTo(file)) {
                    FileOutputStream(file).use { it.write(wavBytes) }
                }
            }
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    @Synchronized
    private fun loadAllSfx(profile: SoundProfile, expectedProfileGen: Long? = null) {
        if (isReleased) return
        if (expectedProfileGen != null && synchronized(profileLoadLock) { profileGeneration } != expectedProfileGen) {
            return
        }

        synchronized(soundPoolLock) {
            if (isReleased) return
            for (i in genericSoundIds.indices) {
                val prevId = genericSoundIds[i]
                genericLoaded[i] = false
                if (prevId != 0) {
                    sfxLoadTargetMap.remove(prevId)
                    try { soundPool.unload(prevId) } catch (_: Exception) {}
                    genericSoundIds[i] = 0
                }
            }
        }

        for (fx in SoundFx.entries) {
            if (isReleased) return
            if (expectedProfileGen != null && synchronized(profileLoadLock) { profileGeneration } != expectedProfileGen) {
                return
            }
            try {
                val file = archiveStore.getGenericSfxFile(profile, fx)
                writeWavAtomicallyIfNeeded(file) {
                    ProceduralAudioSynthesizer.synthesizeGeneric(fx, profile)
                }
                synchronized(soundPoolLock) {
                    if (isReleased) return
                    val id = soundPool.load(file.absolutePath, 1)
                    genericSoundIds[fx.ordinal] = id
                    sfxLoadTargetMap[id] = Triple(0, fx.ordinal, 0)
                }
            } catch (_: Exception) {
                genericSoundIds[fx.ordinal] = 0
            }
        }

        if (turretAssetsLoaded) return
        turretAssetsLoaded = true

        for (slot in 1..5) {
            if (isReleased) return
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
                if (isReleased) return
                try {
                    val file = archiveStore.getUpgradeFile(tClass, slot)
                    writeWavAtomicallyIfNeeded(file) {
                        ProceduralAudioSynthesizer.synthesizeTowerUpgrade(tClass, slot)
                    }
                    synchronized(soundPoolLock) {
                        if (isReleased) return
                        val id = soundPool.load(file.absolutePath, 1)
                        upgradeSoundIds[c][slot - 1] = id
                        sfxLoadTargetMap[id] = Triple(5, c, slot - 1)
                    }
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
        crossinline synthesize: () -> ShortArray
    ) {
        try {
            writeWavAtomicallyIfNeeded(file) { synthesize() }
            synchronized(soundPoolLock) {
                if (isReleased) return
                val id = soundPool.load(file.absolutePath, 1)
                targetIds[slotIdx] = id
                sfxLoadTargetMap[id] = Triple(categoryCode, slotIdx, 0)
            }
        } catch (_: Exception) {
            targetIds[slotIdx] = 0
        }
    }

    fun play(fx: SoundFx, volume: Float = 1f, rate: Float = 1f) {
        if (isReleased || !enabled || isMuted || isAudioSuspended) return
        val idx = fx.ordinal
        if (idx !in genericLoaded.indices || !genericLoaded[idx]) return
        val now = System.nanoTime()
        if (now - lastGenericPlayNs[idx] < fx.minIntervalMs * 1_000_000L) return
        lastGenericPlayNs[idx] = now
        val effectiveChannelVol = if (fx == SoundFx.UPGRADE) upgradeVolume else sfxVolume
        val v = (volume * masterVolume * effectiveChannelVol).coerceIn(0f, 1f)
        try {
            synchronized(soundPoolLock) {
                if (!isReleased) {
                    soundPool.play(genericSoundIds[idx], v, v, 1, 0, rate.coerceIn(0.5f, 2f))
                }
            }
        } catch (_: Exception) {}
    }

    fun playPlacement() {
        play(SoundFx.PLACE)
    }

    fun playTurretFire(towerClass: TowerClass, slot: Int = getEquippedFireSlot(towerClass)) {
        if (isReleased || !enabled || isMuted || isAudioSuspended) return
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
                synchronized(soundPoolLock) {
                    if (!isReleased) {
                        soundPool.play(soundId, v, v, 2, 0, jitterRate)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun playTurretTestSound(towerClass: TowerClass, slot: Int) {
        if (isReleased || !enabled || isMuted) return
        val targetSlot = (slot - 1).coerceIn(0, 4)
        val (soundId, isLoaded) = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> Pair(kineticSoundIds[targetSlot], kineticLoaded[targetSlot])
            TowerClass.ELEMENTAL_VOID -> Pair(elementalSoundIds[targetSlot], elementalLoaded[targetSlot])
            TowerClass.HEAVY_SIEGE -> Pair(siegeSoundIds[targetSlot], siegeLoaded[targetSlot])
            TowerClass.STELLAR_COSMIC -> Pair(stellarSoundIds[targetSlot], stellarLoaded[targetSlot])
        }
        try {
            synchronized(soundPoolLock) {
                if (isReleased) return
                soundPool.autoResume()
                if (soundId != 0 && isLoaded) {
                    val v = (masterVolume * sfxVolume).coerceIn(0f, 1f)
                    soundPool.play(soundId, v, v, 3, 0, 1.0f)
                }
            }
        } catch (_: Exception) {}
    }

    fun playTowerUpgrade(towerClass: TowerClass, skinSlot: Int = getEquippedUpgradeSlot(towerClass)) {
        if (isReleased || !enabled || isMuted || isAudioSuspended) return
        val c = towerClass.ordinal.coerceIn(0, 3)
        val s = (skinSlot - 1).coerceIn(0, 4)
        val soundId = upgradeSoundIds[c][s]
        val isLoaded = upgradeLoaded[c][s]
        if (soundId != 0 && isLoaded) {
            val v = (masterVolume * upgradeVolume).coerceIn(0f, 1f)
            try {
                synchronized(soundPoolLock) {
                    if (!isReleased) {
                        soundPool.play(soundId, v, v, 2, 0, 1.0f)
                    }
                }
            } catch (_: Exception) {}
        } else {
            play(SoundFx.UPGRADE)
        }
    }

    fun playTowerUpgradeTestSound(towerClass: TowerClass, skinSlot: Int = 1) {
        if (isReleased || !enabled || isMuted) return
        val c = towerClass.ordinal.coerceIn(0, 3)
        val s = (skinSlot - 1).coerceIn(0, 4)
        val soundId = upgradeSoundIds[c][s]
        val isLoaded = upgradeLoaded[c][s]
        try {
            synchronized(soundPoolLock) {
                if (isReleased) return
                soundPool.autoResume()
                if (soundId != 0 && isLoaded) {
                    val v = (masterVolume * upgradeVolume).coerceIn(0f, 1f)
                    soundPool.play(soundId, v, v, 3, 0, 1.0f)
                    return
                }
            }
        } catch (_: Exception) {}
        play(SoundFx.UPGRADE)
    }

    fun suspendAllAudio() {
        isAudioSuspended = true
        musicPlayer.suspendMusic()
        try {
            synchronized(soundPoolLock) {
                if (!isReleased) {
                    soundPool.autoPause()
                }
            }
        } catch (_: Exception) {}
    }

    fun resumeAudio() {
        isAudioSuspended = false
        if (isReleased || !enabled || isMuted) return
        try {
            synchronized(soundPoolLock) {
                if (!isReleased) {
                    soundPool.autoResume()
                }
            }
        } catch (_: Exception) {}
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
        synchronized(soundPoolLock) {
            if (isReleased) return
            isReleased = true
            try { soundPool.release() } catch (_: Exception) {}
        }
        ioScope.cancel()
        musicPlayer.release()
        finalFlushJob = libraryController.flushPendingExportsOnRelease()
    }
}
