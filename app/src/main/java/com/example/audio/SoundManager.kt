package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.model.TowerClass
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

/** Procedurally synthesised sound effects. No bundled audio assets are required. */
enum class SoundFx(val label: String, val minIntervalMs: Long) {
    PLACE("Placement Thud", 40L),
    UPGRADE("Upgrade Chime", 80L),
    FIRE_KINETIC("Kinetic Fire (Default)", 65L),
    FIRE_ELEMENTAL("Elemental Fire (Default)", 80L),
    FIRE_SIEGE("Siege Fire (Default)", 110L),
    FIRE_STELLAR("Stellar Cosmic Fire", 75L),
    INVALID("Invalid Tap Buzz", 120L),
    SELL("Salvage Scrap Return", 60L)
}

enum class SoundProfile(val label: String) {
    CYBER_SYNTH("Cyber Synth (Default)"),
    RETRO_8BIT("Retro 8-Bit Arcade"),
    DEEP_ACOUSTIC("Heavy Acoustic Impact"),
    SCI_FI_LASER("Sci-Fi Laser Grid")
}

/**
 * 5-Track Music Selection plus Rolling Cyclical Playlist and Mute.
 */
enum class MusicTrack(val label: String) {
    NONE("Mute Music"),
    ROLLING_PLAYLIST("Rolling Playlist (Cycles 1-5)"),
    CYBER_AMBIENT("Track 1: Sector Zero Synth"),
    PULSE_ARPEGGIO("Track 2: Dual Front Pulse"),
    DEEP_SPACE("Track 3: Sub-Void Drone"),
    NEON_OVERDRIVE("Track 4: Neon Overdrive"),
    CELESTIAL_BASTION("Track 5: Celestial Bastion")
}

/**
 * Procedural Asset Record for Sound and Skin Library.
 * Supports Heart/Star favorites and JSON delta export.
 */
data class CustomAssetRecord(
    val id: String,
    val type: String, // "SKIN" or "SOUND"
    val name: String,
    val category: String,
    var isFavoriteHeart: Boolean = false,
    var isFavoriteStar: Boolean = false,
    val unlockScore: Int = 0,
    val metadata: String = ""
)

/**
 * Hybrid Audio Engine:
 * - [MediaPlayer] for background music streaming: enables reliable track switching,
 *   cyclical rolling playlists, gradual fades (600-1000ms), low-pass filtered ambient synths (<650Hz),
 *   and smooth combat ducking.
 * - [SoundPool] for low-latency SFX: placement, upgrades, and 20 turret firing audio variants
 *   (5 slots across 4 archetypes: Kinetic, Elemental, Siege, Stellar) with voice limiting.
 *
 * 100% procedurally synthesized via [ProceduralAudioSynthesizer], zero bundled asset footprint.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext

    // --- SoundPool SFX Engine ---
    private var soundPool: SoundPool
    private val genericSoundIds = IntArray(SoundFx.values().size)
    private val genericLoaded = BooleanArray(SoundFx.values().size)
    private val lastGenericPlayNs = LongArray(SoundFx.values().size)

    // 5 SFX Variants per Turret Archetype (Kinetic, Elemental, Siege, Stellar)
    private val kineticSoundIds = IntArray(5)
    private val kineticLoaded = BooleanArray(5)
    private val elementalSoundIds = IntArray(5)
    private val elementalLoaded = BooleanArray(5)
    private val siegeSoundIds = IntArray(5)
    private val siegeLoaded = BooleanArray(5)
    private val stellarSoundIds = IntArray(5)
    private val stellarLoaded = BooleanArray(5)

    // 20 Upgrade SFX Variants: 4 Turret Classes x 5 Skin Slots
    private val upgradeSoundIds = Array(4) { IntArray(5) }
    private val upgradeLoaded = Array(4) { BooleanArray(5) }

    // Concurrency / Voice limiting per turret archetype (4 classes)
    private val lastTurretFirePlayMs = LongArray(4)
    private val MIN_TURRET_FIRE_INTERVAL_MS = 65L

    // --- MediaPlayer Background Music Engine ---
    private var mediaPlayer: MediaPlayer? = null
    private val rollingPlaylistTracks = listOf(
        MusicTrack.CYBER_AMBIENT,
        MusicTrack.PULSE_ARPEGGIO,
        MusicTrack.DEEP_SPACE,
        MusicTrack.NEON_OVERDRIVE,
        MusicTrack.CELESTIAL_BASTION
    )
    private var currentRollingIndex: Int = 0
    private var playlistTrackElapsedMs: Long = 0L
    private val PLAYLIST_TRACK_WINDOW_MS = 60_000L

    // Smooth gradual volume interpolation (600 - 1000ms)
    private var currentMusicVolume: Float = 0f
    private var targetMusicVolume: Float = 0f

    // Dynamic Volume & Attenuation State
    @Volatile
    var enabled: Boolean = true

    @Volatile
    var isMuted: Boolean = false
        set(value) {
            field = value
            updateMusicVolumeTarget()
        }

    @Volatile
    var masterVolume: Float = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateMusicVolumeTarget()
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
            updateMusicVolumeTarget()
        }

    @Volatile
    var isPaused: Boolean = false
        set(value) {
            field = value
            updateMusicVolumeTarget()
            if (!isAudioSuspended && enabled && !isMuted && activeMusicTrack != MusicTrack.NONE) {
                try {
                    if (mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                } catch (_: Exception) {}
            }
        }

    @Volatile
    var isAudioSuspended: Boolean = false

    @Volatile
    var isDucked: Boolean = false

    private var duckUntilMs: Long = 0L
    private val DUCK_FACTOR = 0.50f              // Music ducks by 50% during heavy combat
    private val PAUSE_ATTENUATION_FACTOR = 0.50f // Music drops naturally by 50% (-6dB) in Pause Menu

    @Volatile
    var activeProfile: SoundProfile = SoundProfile.CYBER_SYNTH
        set(value) {
            field = value
            loadAllSfx(value)
        }

    @Volatile
    var activeMusicTrack: MusicTrack = MusicTrack.ROLLING_PLAYLIST
        set(value) {
            field = value
            playlistTrackElapsedMs = 0L
            playTrack(value)
        }

    // Asset Library with Heart/Star Favorites
    val assetLibrary = mutableListOf<CustomAssetRecord>()

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(24)
            .setAudioAttributes(attrs)
            .build()

        populateAssetLibrary()
        loadAllSfx(activeProfile)
        playTrack(activeMusicTrack)
    }

    private fun populateAssetLibrary() {
        assetLibrary.clear()
        for (track in MusicTrack.values()) {
            if (track != MusicTrack.NONE) {
                assetLibrary.add(
                    CustomAssetRecord(
                        id = "bgm_${track.name.lowercase()}",
                        type = "SOUND",
                        name = track.label,
                        category = "BACKGROUND_MUSIC",
                        unlockScore = 0
                    )
                )
            }
        }
        for (fx in SoundFx.values()) {
            assetLibrary.add(
                CustomAssetRecord(
                    id = "sfx_${fx.name.lowercase()}",
                    type = "SOUND",
                    name = fx.label,
                    category = "SFX",
                    unlockScore = 0
                )
            )
        }
        val skins = listOf("Vector Minimalist", "Blueprint CAD", "Industrial Rivet", "Void Rift", "Pixel 8-Bit")
        skins.forEachIndexed { idx, name ->
            assetLibrary.add(
                CustomAssetRecord(
                    id = "skin_slot_${idx + 1}",
                    type = "SKIN",
                    name = name,
                    category = "TURRET_SKIN",
                    unlockScore = idx * 5000
                )
            )
        }
    }

    fun toggleAssetFavoriteHeart(assetId: String): Boolean {
        val asset = assetLibrary.firstOrNull { it.id == assetId } ?: return false
        asset.isFavoriteHeart = !asset.isFavoriteHeart
        return asset.isFavoriteHeart
    }

    fun toggleAssetFavoriteStar(assetId: String): Boolean {
        val asset = assetLibrary.firstOrNull { it.id == assetId } ?: return false
        asset.isFavoriteStar = !asset.isFavoriteStar
        return asset.isFavoriteStar
    }

    fun exportLibraryJson(context: Context): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": \"0.0.1.002\",\n")
        sb.append("  \"exportedAtEpochMs\": ${System.currentTimeMillis()},\n")
        sb.append("  \"assets\": [\n")
        assetLibrary.forEachIndexed { index, item ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${item.id}\",\n")
            sb.append("      \"type\": \"${item.type}\",\n")
            sb.append("      \"name\": \"${item.name}\",\n")
            sb.append("      \"category\": \"${item.category}\",\n")
            sb.append("      \"heart\": ${item.isFavoriteHeart},\n")
            sb.append("      \"star\": ${item.isFavoriteStar},\n")
            sb.append("      \"unlockScore\": ${item.unlockScore}\n")
            sb.append("    }${if (index < assetLibrary.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        val jsonStr = sb.toString()
        try {
            val file = File(context.filesDir, "exported_skins_sounds_library.json")
            file.writeText(jsonStr)
        } catch (_: Exception) {}
        return jsonStr
    }

    // ---------------------------------------------------------------------------------------------
    // SFX Loading & Voice Limiting
    // ---------------------------------------------------------------------------------------------

    private fun loadAllSfx(profile: SoundProfile) {
        val idToTarget = HashMap<Int, Triple<Int, Int, Int>>()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                val target = idToTarget[sampleId]
                if (target != null) {
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

        // 1. Generic game SFX
        for (fx in SoundFx.values()) {
            try {
                val filename = "sfx_${profile.name.lowercase()}_${fx.name.lowercase()}_v4.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use {
                        it.write(AudioSynthesizer.toWav(ProceduralAudioSynthesizer.synthesizeGeneric(fx, profile)))
                    }
                }
                val id = soundPool.load(file.absolutePath, 1)
                genericSoundIds[fx.ordinal] = id
                idToTarget[id] = Triple(0, fx.ordinal, 0)
            } catch (_: Exception) {
                genericSoundIds[fx.ordinal] = 0
            }
        }

        // 2. 5 Kinetic Gunfire Variants
        for (slot in 1..5) {
            try {
                val filename = "sfx_gunfire_kinetic_slot${slot}_v2.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use {
                        it.write(AudioSynthesizer.toWav(ProceduralAudioSynthesizer.synthesizeKineticVariant(slot)))
                    }
                }
                val id = soundPool.load(file.absolutePath, 1)
                kineticSoundIds[slot - 1] = id
                idToTarget[id] = Triple(1, slot - 1, 0)
            } catch (_: Exception) {
                kineticSoundIds[slot - 1] = 0
            }
        }

        // 3. 5 Elemental Gunfire Variants
        for (slot in 1..5) {
            try {
                val filename = "sfx_gunfire_elemental_slot${slot}_v2.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use {
                        it.write(AudioSynthesizer.toWav(ProceduralAudioSynthesizer.synthesizeElementalVariant(slot)))
                    }
                }
                val id = soundPool.load(file.absolutePath, 1)
                elementalSoundIds[slot - 1] = id
                idToTarget[id] = Triple(2, slot - 1, 0)
            } catch (_: Exception) {
                elementalSoundIds[slot - 1] = 0
            }
        }

        // 4. 5 Siege Gunfire Variants
        for (slot in 1..5) {
            try {
                val filename = "sfx_gunfire_siege_slot${slot}_v2.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use {
                        it.write(AudioSynthesizer.toWav(ProceduralAudioSynthesizer.synthesizeSiegeVariant(slot)))
                    }
                }
                val id = soundPool.load(file.absolutePath, 1)
                siegeSoundIds[slot - 1] = id
                idToTarget[id] = Triple(3, slot - 1, 0)
            } catch (_: Exception) {
                siegeSoundIds[slot - 1] = 0
            }
        }

        // 5. 5 Stellar Gunfire Variants
        for (slot in 1..5) {
            try {
                val filename = "sfx_gunfire_stellar_slot${slot}_v1.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use {
                        it.write(AudioSynthesizer.toWav(ProceduralAudioSynthesizer.synthesizeStellarVariant(slot)))
                    }
                }
                val id = soundPool.load(file.absolutePath, 1)
                stellarSoundIds[slot - 1] = id
                idToTarget[id] = Triple(4, slot - 1, 0)
            } catch (_: Exception) {
                stellarSoundIds[slot - 1] = 0
            }
        }

        // 6. 20 Dedicated Tower Upgrade Sounds (4 Classes x 5 Skins)
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
                    val filename = "sfx_upgrade_${tClass.name.lowercase()}_slot${slot}_v2.wav"
                    val file = File(appContext.cacheDir, filename)
                    if (!file.exists() || file.length() < 64L) {
                        FileOutputStream(file).use {
                            it.write(AudioSynthesizer.toWav(ProceduralAudioSynthesizer.synthesizeTowerUpgrade(tClass, slot)))
                        }
                    }
                    val id = soundPool.load(file.absolutePath, 1)
                    upgradeSoundIds[c][slot - 1] = id
                    idToTarget[id] = Triple(5, c, slot - 1)
                } catch (_: Exception) {
                    upgradeSoundIds[c][slot - 1] = 0
                }
            }
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

    fun playTurretFire(towerClass: TowerClass, slot: Int = 1) {
        if (!enabled || isMuted || isAudioSuspended) return
        val archetypeIdx = towerClass.ordinal.coerceIn(0, 3)
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastTurretFirePlayMs[archetypeIdx] < MIN_TURRET_FIRE_INTERVAL_MS) return
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

    fun playTowerUpgrade(towerClass: TowerClass, skinSlot: Int = 1) {
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
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
        try {
            soundPool.autoPause()
        } catch (_: Exception) {}
    }

    fun resumeAudio() {
        isAudioSuspended = false
        if (!enabled || isMuted) return
        try {
            soundPool.autoResume()
        } catch (_: Exception) {}
        if (activeMusicTrack != MusicTrack.NONE) {
            try {
                if (mediaPlayer?.isPlaying == false) {
                    val vol = calculateEffectiveMusicVolume()
                    currentMusicVolume = vol
                    targetMusicVolume = vol
                    mediaPlayer?.setVolume(vol, vol)
                    mediaPlayer?.start()
                }
            } catch (_: Exception) {}
        }
    }

    fun triggerCombatDucking(durationMs: Long = 450L) {
        duckUntilMs = System.currentTimeMillis() + durationMs
        if (!isDucked) {
            isDucked = true
            updateMusicVolumeTarget()
        }
    }

    fun updateTick(nowMs: Long = System.currentTimeMillis(), dtMs: Long = 16L) {
        if (isDucked && nowMs >= duckUntilMs) {
            isDucked = false
            updateMusicVolumeTarget()
        }

        val fadeStep = (dtMs / 800f).coerceIn(0.015f, 1.0f)
        if (kotlin.math.abs(targetMusicVolume - currentMusicVolume) > 0.005f) {
            currentMusicVolume += (targetMusicVolume - currentMusicVolume) * fadeStep
            try {
                mediaPlayer?.setVolume(currentMusicVolume, currentMusicVolume)
            } catch (_: Exception) {}
        } else {
            currentMusicVolume = targetMusicVolume
        }

        if (activeMusicTrack == MusicTrack.ROLLING_PLAYLIST && enabled && !isMuted && !isPaused && !isAudioSuspended) {
            playlistTrackElapsedMs += dtMs
            if (playlistTrackElapsedMs >= PLAYLIST_TRACK_WINDOW_MS) {
                advanceRollingPlaylist()
            }
        }
    }

    private fun advanceRollingPlaylist() {
        currentRollingIndex = (currentRollingIndex + 1) % rollingPlaylistTracks.size
        playlistTrackElapsedMs = 0L
        val nextTrack = rollingPlaylistTracks[currentRollingIndex]
        playTrackInternal(nextTrack, loop = true)
    }

    private fun playTrack(track: MusicTrack) {
        if (track == MusicTrack.NONE) {
            stopMusic()
            return
        }
        if (track == MusicTrack.ROLLING_PLAYLIST) {
            val target = rollingPlaylistTracks[currentRollingIndex]
            playTrackInternal(target, loop = true)
        } else {
            playTrackInternal(track, loop = true)
        }
    }

    private fun playTrackInternal(targetTrack: MusicTrack, loop: Boolean) {
        try {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) mp.stop()
                } catch (_: Exception) {}
                mp.release()
            }
            mediaPlayer = null

            if (!enabled || isMuted || isAudioSuspended || targetTrack == MusicTrack.NONE) return

            val file = getOrCreateMusicFile(targetTrack)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                isLooping = loop
                prepare()
                targetMusicVolume = calculateEffectiveMusicVolume()
                currentMusicVolume = 0f
                setVolume(0f, 0f)
                if (!isAudioSuspended) {
                    start()
                }
            }
        } catch (_: Exception) {
            mediaPlayer = null
        }
    }

    fun stopMusic() {
        try {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) mp.stop()
                } catch (_: Exception) {}
                mp.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun calculateEffectiveMusicVolume(): Float {
        if (!enabled || isMuted || isAudioSuspended || activeMusicTrack == MusicTrack.NONE) return 0f
        val base = (masterVolume * musicVolume).coerceIn(0f, 1f)
        val pauseFactor = if (isPaused) PAUSE_ATTENUATION_FACTOR else 1.0f
        val duckFactor = if (isDucked && !isPaused) DUCK_FACTOR else 1.0f
        return (base * pauseFactor * duckFactor).coerceIn(0f, 1f)
    }

    private fun updateMusicVolumeTarget() {
        targetMusicVolume = calculateEffectiveMusicVolume()
    }

    fun updateMusicVolume() {
        updateMusicVolumeTarget()
        currentMusicVolume = targetMusicVolume
        try {
            mediaPlayer?.setVolume(currentMusicVolume, currentMusicVolume)
        } catch (_: Exception) {}
    }

    fun setMasterVolumeLevel(level: Float) {
        masterVolume = level
    }

    fun setSfxVolumeLevel(level: Float) {
        sfxVolume = level
    }

    fun setUpgradeVolumeLevel(level: Float) {
        upgradeVolume = level
    }

    fun setMusicVolumeLevel(level: Float) {
        musicVolume = level
    }

    fun setMutedState(muted: Boolean) {
        isMuted = muted
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun release() {
        stopMusic()
        try {
            soundPool.release()
        } catch (_: Exception) {}
    }

    private fun getOrCreateMusicFile(track: MusicTrack): File {
        val filename = "bgm_${track.name.lowercase()}_v5_lowpass.wav"
        val file = File(appContext.cacheDir, filename)
        if (!file.exists() || file.length() < 256L) {
            FileOutputStream(file).use {
                it.write(AudioSynthesizer.toWav(AudioSynthesizer.synthesizeMusic(track)))
            }
        }
        return file
    }
}
