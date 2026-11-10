package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.model.TowerClass
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Procedurally synthesised sound effects. No bundled audio assets are required. */
enum class SoundFx(val label: String, val minIntervalMs: Long) {
    PLACE("Placement Thud", 40L),
    UPGRADE("Upgrade Chime", 80L),
    FIRE_KINETIC("Kinetic Fire (Default)", 65L),
    FIRE_ELEMENTAL("Elemental Fire (Default)", 80L),
    FIRE_SIEGE("Siege Fire (Default)", 110L),
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
 * Hybrid Audio Engine:
 * - [MediaPlayer] for background music streaming: enables reliable track switching,
 *   cyclical rolling playlists, smooth combat ducking, and pause menu attenuation.
 * - [SoundPool] for low-latency SFX: placement, upgrades, and 15 turret firing audio variants
 *   (5 slots across 3 archetypes) with voice limiting and concurrency throttling.
 *
 * 100% procedurally synthesized, zero bundled asset footprint, allocation-free play loops.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext

    // --- SoundPool SFX Engine ---
    private var soundPool: SoundPool
    private val genericSoundIds = IntArray(SoundFx.values().size)
    private val genericLoaded = BooleanArray(SoundFx.values().size)
    private val lastGenericPlayNs = LongArray(SoundFx.values().size)

    // 5 SFX Variants per Turret Archetype (Kinetic, Elemental, Siege)
    private val kineticSoundIds = IntArray(5)
    private val kineticLoaded = BooleanArray(5)
    private val elementalSoundIds = IntArray(5)
    private val elementalLoaded = BooleanArray(5)
    private val siegeSoundIds = IntArray(5)
    private val siegeLoaded = BooleanArray(5)

    // Concurrency / Voice limiting per turret archetype
    private val lastTurretFirePlayMs = LongArray(3)
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
    private val PLAYLIST_TRACK_WINDOW_MS = 60_000L // 1.0 minute per song (targets 45s to 1m15s)

    // Dynamic Volume & Attenuation State
    @Volatile
    var enabled: Boolean = true

    @Volatile
    var isMuted: Boolean = false
        set(value) {
            field = value
            updateMusicVolume()
        }

    @Volatile
    var masterVolume: Float = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateMusicVolume()
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
            updateMusicVolume()
        }

    @Volatile
    var isPaused: Boolean = false
        set(value) {
            field = value
            updateMusicVolume()
        }

    @Volatile
    var isAudioSuspended: Boolean = false

    @Volatile
    var isDucked: Boolean = false

    private var duckUntilMs: Long = 0L
    private val DUCK_FACTOR = 0.50f              // Music ducks by 50% during heavy combat
    private val PAUSE_ATTENUATION_FACTOR = 0.50f // Music drops by 50% (-6dB) in Pause Menu

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

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(14)
            .setAudioAttributes(attrs)
            .build()

        loadAllSfx(activeProfile)
        playTrack(activeMusicTrack)
    }

    // ---------------------------------------------------------------------------------------------
    // SFX Loading & Voice Limiting
    // ---------------------------------------------------------------------------------------------

    private fun loadAllSfx(profile: SoundProfile) {
        val idToTarget = HashMap<Int, Pair<Int, Int>>() // sampleId -> (type: 0=generic,1=kinetic,2=elem,3=siege; index)

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                val target = idToTarget[sampleId]
                if (target != null) {
                    when (target.first) {
                        0 -> genericLoaded[target.second] = true
                        1 -> kineticLoaded[target.second] = true
                        2 -> elementalLoaded[target.second] = true
                        3 -> siegeLoaded[target.second] = true
                    }
                }
            }
        }

        // 1. Generic game SFX
        for (fx in SoundFx.values()) {
            try {
                val filename = "sfx_${profile.name.lowercase()}_${fx.name.lowercase()}_v3.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use { it.write(toWav(synthesizeGeneric(fx, profile))) }
                }
                val id = soundPool.load(file.absolutePath, 1)
                genericSoundIds[fx.ordinal] = id
                idToTarget[id] = Pair(0, fx.ordinal)
            } catch (_: Exception) {
                genericSoundIds[fx.ordinal] = 0
            }
        }

        // 2. 5 Kinetic Gunfire Variants
        for (slot in 1..5) {
            try {
                val filename = "sfx_gunfire_kinetic_slot${slot}_v1.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use { it.write(toWav(synthesizeKineticVariant(slot))) }
                }
                val id = soundPool.load(file.absolutePath, 1)
                kineticSoundIds[slot - 1] = id
                idToTarget[id] = Pair(1, slot - 1)
            } catch (_: Exception) {
                kineticSoundIds[slot - 1] = 0
            }
        }

        // 3. 5 Elemental Gunfire Variants
        for (slot in 1..5) {
            try {
                val filename = "sfx_gunfire_elemental_slot${slot}_v1.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use { it.write(toWav(synthesizeElementalVariant(slot))) }
                }
                val id = soundPool.load(file.absolutePath, 1)
                elementalSoundIds[slot - 1] = id
                idToTarget[id] = Pair(2, slot - 1)
            } catch (_: Exception) {
                elementalSoundIds[slot - 1] = 0
            }
        }

        // 4. 5 Siege Gunfire Variants
        for (slot in 1..5) {
            try {
                val filename = "sfx_gunfire_siege_slot${slot}_v1.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use { it.write(toWav(synthesizeSiegeVariant(slot))) }
                }
                val id = soundPool.load(file.absolutePath, 1)
                siegeSoundIds[slot - 1] = id
                idToTarget[id] = Pair(3, slot - 1)
            } catch (_: Exception) {
                siegeSoundIds[slot - 1] = 0
            }
        }
    }

    /**
     * Plays generic game effects (placement, upgrades, invalid buzz).
     * UPGRADE effects use dedicated upgradeVolume.
     */
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

    /**
     * Plays turret gunfire with voice concurrency limiting and micro-pitch jitter.
     * Triggers dynamic combat ducking on the background music player.
     */
    fun playTurretFire(towerClass: TowerClass, slot: Int = 1) {
        if (!enabled || isMuted || isAudioSuspended) return
        val archetypeIdx = towerClass.ordinal
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastTurretFirePlayMs[archetypeIdx] < MIN_TURRET_FIRE_INTERVAL_MS) return
        lastTurretFirePlayMs[archetypeIdx] = nowMs

        // Dynamic combat ducking: reduces music clutter during bullet release
        triggerCombatDucking(durationMs = 450L)

        val targetSlot = (slot - 1).coerceIn(0, 4)
        val (soundId, isLoaded) = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> Pair(kineticSoundIds[targetSlot], kineticLoaded[targetSlot])
            TowerClass.ELEMENTAL_VOID -> Pair(elementalSoundIds[targetSlot], elementalLoaded[targetSlot])
            TowerClass.HEAVY_SIEGE -> Pair(siegeSoundIds[targetSlot], siegeLoaded[targetSlot])
        }

        if (soundId != 0 && isLoaded) {
            val v = (masterVolume * sfxVolume).coerceIn(0f, 1f)
            // Subtle pitch variation (0.96..1.04) prevents machine-gun phase fatigue
            val jitterRate = 0.96f + Random.nextFloat() * 0.08f
            try {
                soundPool.play(soundId, v, v, 2, 0, jitterRate)
            } catch (_: Exception) {}
        }
    }

    /**
     * Preview testing helper for audio variants inside menus.
     */
    fun playTurretTestSound(towerClass: TowerClass, slot: Int) {
        if (!enabled || isMuted || isAudioSuspended) return
        val targetSlot = (slot - 1).coerceIn(0, 4)
        val (soundId, isLoaded) = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> Pair(kineticSoundIds[targetSlot], kineticLoaded[targetSlot])
            TowerClass.ELEMENTAL_VOID -> Pair(elementalSoundIds[targetSlot], elementalLoaded[targetSlot])
            TowerClass.HEAVY_SIEGE -> Pair(siegeSoundIds[targetSlot], siegeLoaded[targetSlot])
        }
        if (soundId != 0 && isLoaded) {
            val v = (masterVolume * sfxVolume).coerceIn(0f, 1f)
            try {
                soundPool.play(soundId, v, v, 3, 0, 1.0f)
            } catch (_: Exception) {}
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Audio Suspension & Termination (TASK 1)
    // ---------------------------------------------------------------------------------------------

    /**
     * Instantly cuts all audio playback when focus is lost (Task view, Home button, minimize).
     * Eliminates lingering background music and trailing SFX/explosions immediately.
     */
    fun suspendAllAudio() {
        isAudioSuspended = true
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
        try {
            soundPool.autoPause()
        } catch (_: Exception) {}
    }

    /**
     * Resumes audio playback only when returning to active focus and not paused in menu.
     */
    fun resumeAudio() {
        isAudioSuspended = false
        if (!enabled || isMuted) return
        try {
            soundPool.autoResume()
        } catch (_: Exception) {}
        if (!isPaused && activeMusicTrack != MusicTrack.NONE) {
            try {
                if (mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
            } catch (_: Exception) {}
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Music Engine (MediaPlayer) & Dynamic Ducking
    // ---------------------------------------------------------------------------------------------

    fun triggerCombatDucking(durationMs: Long = 450L) {
        duckUntilMs = System.currentTimeMillis() + durationMs
        if (!isDucked) {
            isDucked = true
            updateMusicVolume()
        }
    }

    fun updateTick(nowMs: Long = System.currentTimeMillis(), dtMs: Long = 16L) {
        // Combat ducking recovery
        if (isDucked && nowMs >= duckUntilMs) {
            isDucked = false
            updateMusicVolume()
        }

        // Rolling playlist cyclical progress: target between 45s and 1m15s (TASK 2)
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
                val vol = calculateEffectiveMusicVolume()
                setVolume(vol, vol)
                if (!isPaused && !isAudioSuspended) {
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

    fun updateMusicVolume() {
        val vol = calculateEffectiveMusicVolume()
        try {
            mediaPlayer?.setVolume(vol, vol)
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

    // ---------------------------------------------------------------------------------------------
    // Procedural SFX & Music Synthesis (100% Asset-Free, Zero Copyright Risk)
    // ---------------------------------------------------------------------------------------------

    private fun getOrCreateMusicFile(track: MusicTrack): File {
        val filename = "bgm_${track.name.lowercase()}_v3.wav"
        val file = File(appContext.cacheDir, filename)
        if (!file.exists() || file.length() < 256L) {
            FileOutputStream(file).use { it.write(toWav(synthesizeMusic(track))) }
        }
        return file
    }

    private fun synthesizeGeneric(fx: SoundFx, profile: SoundProfile): ShortArray = when (profile) {
        SoundProfile.CYBER_SYNTH -> when (fx) {
            SoundFx.PLACE -> render(durationMs = 140) { t, n ->
                val f = 150.0 * exp(-t * 18.0) + 55.0
                (sin(2 * PI * f * t) * 0.9 + noiseAt(n) * 0.25 * exp(-t * 60.0)) * exp(-t * 22.0)
            }
            SoundFx.UPGRADE -> render(durationMs = 420) { t, _ ->
                val env = exp(-t * 6.5) * minOf(1.0, t * 80.0)
                val a = sin(2 * PI * 880.0 * t)
                val b = sin(2 * PI * 1318.5 * (t - 0.09).coerceAtLeast(0.0)) * (if (t > 0.09) 1.0 else 0.0)
                val c = sin(2 * PI * 1760.0 * t) * 0.35
                (a * 0.55 + b * 0.55 + c) * env
            }
            SoundFx.FIRE_KINETIC -> synthesizeKineticVariant(1)
            SoundFx.FIRE_ELEMENTAL -> synthesizeElementalVariant(1)
            SoundFx.FIRE_SIEGE -> synthesizeSiegeVariant(1)
            SoundFx.INVALID -> render(durationMs = 190) { t, _ ->
                val f = 190.0 - 40.0 * (t / 0.19)
                val s = sin(2 * PI * f * t)
                val gate = if (((t * 28.0).toInt() % 2) == 0) 1.0 else 0.35
                (if (s >= 0) 0.55 else -0.55) * gate * exp(-t * 5.5)
            }
            SoundFx.SELL -> render(durationMs = 160) { t, _ ->
                val f = 400.0 + t * 600.0
                sin(2 * PI * f * t) * 0.7 * exp(-t * 12.0)
            }
        }
        SoundProfile.RETRO_8BIT -> when (fx) {
            SoundFx.PLACE -> render(durationMs = 120) { t, _ ->
                val f = 220.0 * exp(-t * 12.0)
                val s = sin(2 * PI * f * t)
                (if (s >= 0) 0.7 else -0.7) * exp(-t * 15.0)
            }
            SoundFx.UPGRADE -> render(durationMs = 350) { t, _ ->
                val step = ((t * 16.0).toInt() % 4)
                val f = when (step) { 0 -> 440.0; 1 -> 554.37; 2 -> 659.25; else -> 880.0 }
                val s = sin(2 * PI * f * t)
                (if (s >= 0) 0.65 else -0.65) * exp(-t * 4.0)
            }
            SoundFx.FIRE_KINETIC -> synthesizeKineticVariant(3)
            SoundFx.FIRE_ELEMENTAL -> synthesizeElementalVariant(3)
            SoundFx.FIRE_SIEGE -> synthesizeSiegeVariant(3)
            SoundFx.INVALID -> render(durationMs = 160) { t, _ ->
                val s = sin(2 * PI * 130.0 * t)
                (if (s >= 0) 0.7 else -0.7) * exp(-t * 6.0)
            }
            SoundFx.SELL -> render(durationMs = 140) { t, _ ->
                val s = sin(2 * PI * 587.33 * t)
                (if (s >= 0) 0.65 else -0.65) * exp(-t * 8.0)
            }
        }
        SoundProfile.DEEP_ACOUSTIC -> when (fx) {
            SoundFx.PLACE -> render(durationMs = 180) { t, n ->
                val f = 90.0 * exp(-t * 24.0) + 40.0
                (sin(2 * PI * f * t) * 0.9 + noiseAt(n) * 0.15) * exp(-t * 18.0)
            }
            SoundFx.UPGRADE -> render(durationMs = 480) { t, _ ->
                val a = sin(2 * PI * 523.25 * t) * exp(-t * 5.0)
                val b = sin(2 * PI * 659.25 * t) * exp(-t * 4.5)
                val c = sin(2 * PI * 783.99 * t) * exp(-t * 4.0)
                (a * 0.4 + b * 0.4 + c * 0.4)
            }
            SoundFx.FIRE_KINETIC -> synthesizeKineticVariant(2)
            SoundFx.FIRE_ELEMENTAL -> synthesizeElementalVariant(2)
            SoundFx.FIRE_SIEGE -> synthesizeSiegeVariant(4)
            SoundFx.INVALID -> render(durationMs = 210) { t, _ ->
                sin(2 * PI * 110.0 * t) * 0.8 * exp(-t * 8.0)
            }
            SoundFx.SELL -> render(durationMs = 180) { t, _ ->
                sin(2 * PI * 330.0 * t) * 0.75 * exp(-t * 8.0)
            }
        }
        SoundProfile.SCI_FI_LASER -> when (fx) {
            SoundFx.PLACE -> render(durationMs = 130) { t, _ ->
                val f = 300.0 * exp(-t * 15.0)
                sin(2 * PI * f * t) * 0.8 * exp(-t * 18.0)
            }
            SoundFx.UPGRADE -> render(durationMs = 400) { t, _ ->
                val f = 600.0 + t * 1400.0
                sin(2 * PI * f * t) * 0.7 * exp(-t * 6.0)
            }
            SoundFx.FIRE_KINETIC -> synthesizeKineticVariant(4)
            SoundFx.FIRE_ELEMENTAL -> synthesizeElementalVariant(4)
            SoundFx.FIRE_SIEGE -> synthesizeSiegeVariant(5)
            SoundFx.INVALID -> render(durationMs = 180) { t, _ ->
                val f = 350.0 - t * 800.0
                sin(2 * PI * f.coerceAtLeast(80.0) * t) * 0.7 * exp(-t * 10.0)
            }
            SoundFx.SELL -> render(durationMs = 160) { t, _ ->
                val f = 500.0 + t * 900.0
                sin(2 * PI * f * t) * 0.7 * exp(-t * 10.0)
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 5 Turret Gunfire Audio Variants per Archetype
    // ---------------------------------------------------------------------------------------------

    /** Kinetic Ballistics (Circle): 5 distinct punchy firing profiles */
    private fun synthesizeKineticVariant(slot: Int): ShortArray = when (slot) {
        1 -> render(durationMs = 85) { t, _ -> // Rotary Gunfire (Default): 1500Hz -> 420Hz rotary snap
            val f = 1500.0 * exp(-t * 30.0) + 420.0
            val s = sin(2 * PI * f * t)
            (if (s >= 0) 0.65 else -0.65) * exp(-t * 32.0)
        }
        2 -> render(durationMs = 110) { t, n -> // Heavy Autocannon: 85Hz punch + mechanical clack
            val f = 180.0 * exp(-t * 26.0) + 75.0
            val body = sin(2 * PI * f * t) * 0.85
            val clack = noiseAt(n) * 0.35 * exp(-t * 70.0)
            (body + clack) * exp(-t * 24.0)
        }
        3 -> render(durationMs = 75) { t, _ -> // Hyper Pulse: high-tech descending square chirp 2200Hz -> 300Hz
            val f = 2200.0 * exp(-t * 40.0) + 300.0
            val s = sin(2 * PI * f * t)
            (if (s >= 0) 0.7 else -0.7) * exp(-t * 36.0)
        }
        4 -> render(durationMs = 70) { t, n -> // Gauss Needle: metallic high velocity ping + supersonic noise burst
            val f = 3200.0 * exp(-t * 50.0) + 600.0
            val ping = sin(2 * PI * f * t) * 0.8
            val burst = noiseAt(n) * 0.4 * exp(-t * 80.0)
            (ping + burst) * exp(-t * 38.0)
        }
        else -> render(durationMs = 95) { t, n -> // Rail Crack: sharp whip-crack transient + resonant metallic ring
            val f = 850.0 + sin(2 * PI * 180.0 * t) * 200.0
            val crack = noiseAt(n) * 0.9 * exp(-t * 90.0)
            val ring = sin(2 * PI * f * t) * 0.45 * exp(-t * 22.0)
            (crack + ring) * exp(-t * 26.0)
        }
    }

    /** Elemental Void (Triangle): 5 distinct focused energy profiles */
    private fun synthesizeElementalVariant(slot: Int): ShortArray = when (slot) {
        1 -> render(durationMs = 160) { t, _ -> // Phase Beam (Default): 700Hz + 900Hz exponential FM beam hum
            val mod = sin(2 * PI * 90.0 * t) * 6.0
            val f = 700.0 + 900.0 * exp(-t * 14.0)
            sin(2 * PI * f * t + mod) * 0.8 * exp(-t * 14.0)
        }
        2 -> render(durationMs = 180) { t, _ -> // Plasma Burn: 480Hz carrier with 110Hz sizzling modulation
            val sizzle = sin(2 * PI * 110.0 * t) * 12.0
            val carrier = sin(2 * PI * 480.0 * t + sizzle) * 0.75
            carrier * exp(-t * 12.0)
        }
        3 -> render(durationMs = 150) { t, n -> // Ion Discharge: dual 1200Hz & 850Hz arc crackle with micro-frequency jitter
            val f1 = 1200.0 + noiseAt(n) * 80.0
            val f2 = 850.0
            val arc = (sin(2 * PI * f1 * t) * 0.5 + sin(2 * PI * f2 * t) * 0.5)
            arc * exp(-t * 16.0)
        }
        4 -> render(durationMs = 210) { t, _ -> // Void Pulse: deep 180Hz phase-shifted resonant sub pulse
            val f = 180.0 * exp(-t * 8.0) + 60.0
            val sub = sin(2 * PI * f * t) * 0.85
            val harmonic = sin(2 * PI * f * 2.5 * t) * 0.35
            (sub + harmonic) * exp(-t * 9.0)
        }
        else -> render(durationMs = 190) { t, _ -> // Arc Flare: harmonic overtone shimmer sweep 1400Hz -> 2800Hz
            val f = 1400.0 + t * 4500.0
            val shimmer = sin(2 * PI * f * t) * 0.75 + sin(2 * PI * (f * 1.5) * t) * 0.3
            shimmer * exp(-t * 13.0)
        }
    }

    /** Heavy Siege (Square): 5 distinct concussive explosive profiles */
    private fun synthesizeSiegeVariant(slot: Int): ShortArray = when (slot) {
        1 -> render(durationMs = 280) { t, n -> // Artillery Blast (Default): 95Hz -> 38Hz heavy explosion + noise blast
            val f = 95.0 * exp(-t * 7.0) + 38.0
            (sin(2 * PI * f * t) * 0.9 + noiseAt(n) * 0.35 * exp(-t * 16.0)) * exp(-t * 9.0)
        }
        2 -> render(durationMs = 340) { t, n -> // Sonic Detonator: 42Hz sub-bass shockwave with long exponential decay
            val f = 50.0 * exp(-t * 5.0) + 32.0
            val sub = sin(2 * PI * f * t) * 0.95
            val rumble = noiseAt(n) * 0.3 * exp(-t * 10.0)
            (sub + rumble) * exp(-t * 6.5)
        }
        3 -> render(durationMs = 260) { t, n -> // Flak Burst: mid-range explosive crunch with wideband crackle
            val noise = noiseAt(n)
            val crunch = (sin(2 * PI * 130.0 * t) * 0.5 + noise * 0.8)
            crunch * exp(-t * 11.0)
        }
        4 -> render(durationMs = 320) { t, n -> // Concussion Mortar: heavy acoustic pressure impact with hollow reverberation
            val f = 75.0 * exp(-t * 6.0) + 30.0
            val thud = (sin(2 * PI * f * t) * 0.95 + noiseAt(n) * 0.45 * exp(-t * 12.0))
            thud * exp(-t * 7.5)
        }
        else -> render(durationMs = 310) { t, n -> // Plasma Howitzer: rising energy whine (220->750Hz) followed by concussive blast
            val whine = if (t < 0.08) sin(2 * PI * (220.0 + t * 6000.0) * t) * 0.6 else 0.0
            val f = 110.0 * exp(-(t - 0.08).coerceAtLeast(0.0) * 8.0) + 40.0
            val boom = if (t >= 0.08) (sin(2 * PI * f * t) * 0.85 + noiseAt(n) * 0.4) * exp(-(t - 0.08) * 8.0) else 0.0
            (whine + boom)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Procedural Music Synthesis (5 Distinct Tracks)
    // ---------------------------------------------------------------------------------------------

    private fun synthesizeMusic(track: MusicTrack): ShortArray = when (track) {
        MusicTrack.NONE, MusicTrack.ROLLING_PLAYLIST -> ShortArray(SAMPLE_RATE)
        MusicTrack.CYBER_AMBIENT -> render(durationMs = 4000) { t, _ ->
            // Track 1: Sector Zero Synth - A-minor 7th drone + 8-step gentle pulsing arpeggio
            val root = sin(2 * PI * 110.0 * t) * 0.35 // A2
            val fifth = sin(2 * PI * 164.81 * t) * 0.25 // E3
            val minorThird = sin(2 * PI * 130.81 * t) * 0.20 // C3
            val sub = sin(2 * PI * 55.0 * t) * 0.30 // A1

            val step = ((t / 0.25).toInt() % 8)
            val arpFreq = when (step) {
                0 -> 220.0 // A3
                1 -> 261.63 // C4
                2 -> 329.63 // E4
                3 -> 392.00 // G4
                4 -> 440.00 // A4
                5 -> 392.00 // G4
                6 -> 329.63 // E4
                else -> 261.63 // C4
            }
            val arpPhase = (t % 0.25) / 0.25
            val arpEnv = (1.0 - arpPhase) * exp(-arpPhase * 4.0)
            val arp = sin(2 * PI * arpFreq * t) * arpEnv * 0.22

            (root + fifth + minorThird + sub + arp) * 0.6
        }
        MusicTrack.PULSE_ARPEGGIO -> render(durationMs = 3000) { t, _ ->
            // Track 2: Dual Front Pulse - D minor electro pulse arpeggio
            val root = sin(2 * PI * 73.42 * t) * 0.35 // D2
            val step = ((t / 0.1875).toInt() % 8)
            val f = when (step) {
                0 -> 293.66 // D4
                1 -> 349.23 // F4
                2 -> 440.00 // A4
                3 -> 523.25 // C5
                4 -> 587.33 // D5
                5 -> 440.00 // A4
                6 -> 349.23 // F4
                else -> 293.66 // D4
            }
            val phase = (t % 0.1875) / 0.1875
            val env = exp(-phase * 6.0)
            val synth = (sin(2 * PI * f * t) + sin(2 * PI * f * 2.0 * t) * 0.3) * env * 0.35
            (root + synth) * 0.7
        }
        MusicTrack.DEEP_SPACE -> render(durationMs = 4500) { t, _ ->
            // Track 3: Sub-Void Drone - C meditative dark pad with slow pitch LFO
            val mod = sin(2 * PI * 0.25 * t) * 4.0
            val pad1 = sin(2 * PI * (65.41 + mod) * t) * 0.40 // C2
            val pad2 = sin(2 * PI * 98.00 * t) * 0.30 // G2
            val shimmer = sin(2 * PI * 523.25 * t) * 0.08 * sin(2 * PI * 0.5 * t)
            (pad1 + pad2 + shimmer) * 0.65
        }
        MusicTrack.NEON_OVERDRIVE -> render(durationMs = 3500) { t, _ ->
            // Track 4: Neon Overdrive - F# minor synthwave bassline & cyber arpeggio
            val bassRoot = 92.50 // F#2
            val bassBeat = ((t / 0.21875).toInt() % 16)
            val bassFreq = when (bassBeat % 4) {
                0 -> bassRoot
                1 -> bassRoot
                2 -> bassRoot * 1.1892 // A2 (minor third)
                else -> bassRoot * 1.3348 // B2 (fourth)
            }
            val bPhase = (t % 0.21875) / 0.21875
            val bEnv = exp(-bPhase * 5.0)
            val bass = (sin(2 * PI * bassFreq * t) + sin(2 * PI * bassFreq * 2.0 * t) * 0.4) * bEnv * 0.45

            val arpStep = ((t / 0.109375).toInt() % 16)
            val arpNote = when (arpStep) {
                0, 8 -> 370.00  // F#4
                1, 9 -> 440.00  // A4
                2, 10 -> 554.37 // C#5
                3, 11 -> 659.25 // E5
                4, 12 -> 740.00 // F#5
                5, 13 -> 659.25 // E5
                6, 14 -> 554.37 // C#5
                else -> 440.00   // A4
            }
            val aPhase = (t % 0.109375) / 0.109375
            val aEnv = exp(-aPhase * 7.0)
            val lead = sin(2 * PI * arpNote * t) * aEnv * 0.25

            (bass + lead) * 0.65
        }
        MusicTrack.CELESTIAL_BASTION -> render(durationMs = 4000) { t, _ ->
            // Track 5: Celestial Bastion - E minor melodic chiptune & crystal harmonic arpeggio
            val drone = (sin(2 * PI * 82.41 * t) * 0.35 + sin(2 * PI * 123.47 * t) * 0.25)
            val step = ((t / 0.25).toInt() % 16)
            val freq = when (step % 8) {
                0 -> 329.63 // E4
                1 -> 392.00 // G4
                2 -> 493.88 // B4
                3 -> 587.33 // D5
                4 -> 659.25 // E5
                5 -> 587.33 // D5
                6 -> 493.88 // B4
                else -> 392.00 // G4
            }
            val phase = (t % 0.25) / 0.25
            val env = exp(-phase * 4.5) * (1.0 - phase)
            val chime = (sin(2 * PI * freq * t) + sin(2 * PI * freq * 2.0 * t) * 0.35 + sin(2 * PI * freq * 3.0 * t) * 0.15) * env * 0.28
            (drone + chime) * 0.65
        }
    }

    private val noiseRandom = Random(0xC0FFEE)
    private fun noiseAt(@Suppress("UNUSED_PARAMETER") n: Int): Double = noiseRandom.nextDouble() * 2.0 - 1.0

    private inline fun render(durationMs: Int, sample: (t: Double, n: Int) -> Double): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            var v = sample(t, i)
            val tailSamples = SAMPLE_RATE / 100
            if (i > count - tailSamples) v *= (count - i).toDouble() / tailSamples
            v = v.coerceIn(-1.0, 1.0)
            out[i] = (v * 0.75 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun toWav(pcm: ShortArray): ByteArray {
        val dataLen = pcm.size * 2
        val bos = ByteArrayOutputStream(44 + dataLen)
        fun writeInt(v: Int) { bos.write(v and 0xFF); bos.write((v shr 8) and 0xFF); bos.write((v shr 16) and 0xFF); bos.write((v shr 24) and 0xFF) }
        fun writeShort(v: Int) { bos.write(v and 0xFF); bos.write((v shr 8) and 0xFF) }
        bos.write("RIFF".toByteArray()); writeInt(36 + dataLen)
        bos.write("WAVE".toByteArray())
        bos.write("fmt ".toByteArray()); writeInt(16)
        writeShort(1); writeShort(1)                       // PCM, mono
        writeInt(SAMPLE_RATE); writeInt(SAMPLE_RATE * 2)   // sample rate, byte rate
        writeShort(2); writeShort(16)                      // block align, bits per sample
        bos.write("data".toByteArray()); writeInt(dataLen)
        for (s in pcm) writeShort(s.toInt())
        return bos.toByteArray()
    }

    private companion object {
        const val SAMPLE_RATE = 22050
    }
}
