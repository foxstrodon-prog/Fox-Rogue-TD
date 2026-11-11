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
import kotlin.math.cos
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
 * 100% procedurally synthesized, zero bundled asset footprint, allocation-free play loops.
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
        // Sound assets
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
        // Turret Skins
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
        val idToTarget = HashMap<Int, Triple<Int, Int, Int>>() // sampleId -> (type, index1, index2)

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
                    FileOutputStream(file).use { it.write(toWav(synthesizeGeneric(fx, profile))) }
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
                    FileOutputStream(file).use { it.write(toWav(synthesizeKineticVariant(slot))) }
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
                    FileOutputStream(file).use { it.write(toWav(synthesizeElementalVariant(slot))) }
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
                    FileOutputStream(file).use { it.write(toWav(synthesizeSiegeVariant(slot))) }
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
                    FileOutputStream(file).use { it.write(toWav(synthesizeStellarVariant(slot))) }
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
                        FileOutputStream(file).use { it.write(toWav(synthesizeTowerUpgrade(tClass, slot))) }
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

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Audio Suspension & Termination
    // ---------------------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------------------
    // Music Engine (MediaPlayer) & Gradual Fades (600 - 1000ms)
    // ---------------------------------------------------------------------------------------------

    fun triggerCombatDucking(durationMs: Long = 450L) {
        duckUntilMs = System.currentTimeMillis() + durationMs
        if (!isDucked) {
            isDucked = true
            updateMusicVolumeTarget()
        }
    }

    fun updateTick(nowMs: Long = System.currentTimeMillis(), dtMs: Long = 16L) {
        // Combat ducking recovery
        if (isDucked && nowMs >= duckUntilMs) {
            isDucked = false
            updateMusicVolumeTarget()
        }

        // Smooth gradual volume interpolation over 600 - 1000ms
        val fadeStep = (dtMs / 800f).coerceIn(0.015f, 1.0f)
        if (kotlin.math.abs(targetMusicVolume - currentMusicVolume) > 0.005f) {
            currentMusicVolume += (targetMusicVolume - currentMusicVolume) * fadeStep
            try {
                mediaPlayer?.setVolume(currentMusicVolume, currentMusicVolume)
            } catch (_: Exception) {}
        } else {
            currentMusicVolume = targetMusicVolume
        }

        // Rolling playlist cyclical progress: target between 45s and 1m15s
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

    // ---------------------------------------------------------------------------------------------
    // Procedural SFX & Music Synthesis
    // ---------------------------------------------------------------------------------------------

    private fun getOrCreateMusicFile(track: MusicTrack): File {
        val filename = "bgm_${track.name.lowercase()}_v5_lowpass.wav"
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
            SoundFx.FIRE_STELLAR -> synthesizeStellarVariant(1)
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
            SoundFx.FIRE_STELLAR -> synthesizeStellarVariant(3)
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
            SoundFx.FIRE_STELLAR -> synthesizeStellarVariant(2)
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
            SoundFx.FIRE_STELLAR -> synthesizeStellarVariant(4)
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

    private fun synthesizeKineticVariant(slot: Int): ShortArray = when (slot) {
        1 -> render(durationMs = 85) { t, _ ->
            val f = 1500.0 * exp(-t * 30.0) + 420.0
            val s = sin(2 * PI * f * t)
            (if (s >= 0) 0.65 else -0.65) * exp(-t * 32.0)
        }
        2 -> render(durationMs = 110) { t, n ->
            val f = 180.0 * exp(-t * 26.0) + 75.0
            val body = sin(2 * PI * f * t) * 0.85
            val clack = noiseAt(n) * 0.35 * exp(-t * 70.0)
            (body + clack) * exp(-t * 24.0)
        }
        3 -> render(durationMs = 75) { t, _ ->
            val f = 2200.0 * exp(-t * 40.0) + 300.0
            val s = sin(2 * PI * f * t)
            (if (s >= 0) 0.7 else -0.7) * exp(-t * 36.0)
        }
        4 -> render(durationMs = 70) { t, n ->
            val f = 3200.0 * exp(-t * 50.0) + 600.0
            val ping = sin(2 * PI * f * t) * 0.8
            val burst = noiseAt(n) * 0.4 * exp(-t * 80.0)
            (ping + burst) * exp(-t * 38.0)
        }
        else -> render(durationMs = 95) { t, n ->
            val f = 850.0 + sin(2 * PI * 180.0 * t) * 200.0
            val crack = noiseAt(n) * 0.9 * exp(-t * 90.0)
            val ring = sin(2 * PI * f * t) * 0.45 * exp(-t * 22.0)
            (crack + ring) * exp(-t * 26.0)
        }
    }

    private fun synthesizeElementalVariant(slot: Int): ShortArray = when (slot) {
        1 -> render(durationMs = 160) { t, _ ->
            val mod = sin(2 * PI * 90.0 * t) * 6.0
            val f = 700.0 + 900.0 * exp(-t * 14.0)
            sin(2 * PI * f * t + mod) * 0.8 * exp(-t * 14.0)
        }
        2 -> render(durationMs = 180) { t, _ ->
            val sizzle = sin(2 * PI * 110.0 * t) * 12.0
            val carrier = sin(2 * PI * 480.0 * t + sizzle) * 0.75
            carrier * exp(-t * 12.0)
        }
        3 -> render(durationMs = 150) { t, n ->
            val f1 = 1200.0 + noiseAt(n) * 80.0
            val f2 = 850.0
            val arc = (sin(2 * PI * f1 * t) * 0.5 + sin(2 * PI * f2 * t) * 0.5)
            arc * exp(-t * 16.0)
        }
        4 -> render(durationMs = 210) { t, _ ->
            val f = 180.0 * exp(-t * 8.0) + 60.0
            val sub = sin(2 * PI * f * t) * 0.85
            val harmonic = sin(2 * PI * f * 2.5 * t) * 0.35
            (sub + harmonic) * exp(-t * 9.0)
        }
        else -> render(durationMs = 190) { t, _ ->
            val f = 1400.0 + t * 4500.0
            val shimmer = sin(2 * PI * f * t) * 0.75 + sin(2 * PI * (f * 1.5) * t) * 0.3
            shimmer * exp(-t * 13.0)
        }
    }

    private fun synthesizeSiegeVariant(slot: Int): ShortArray = when (slot) {
        1 -> render(durationMs = 280) { t, n ->
            val f = 95.0 * exp(-t * 7.0) + 38.0
            (sin(2 * PI * f * t) * 0.9 + noiseAt(n) * 0.35 * exp(-t * 16.0)) * exp(-t * 9.0)
        }
        2 -> render(durationMs = 340) { t, n ->
            val f = 50.0 * exp(-t * 5.0) + 32.0
            val sub = sin(2 * PI * f * t) * 0.95
            val rumble = noiseAt(n) * 0.3 * exp(-t * 10.0)
            (sub + rumble) * exp(-t * 6.5)
        }
        3 -> render(durationMs = 260) { t, n ->
            val noise = noiseAt(n)
            val crunch = (sin(2 * PI * 130.0 * t) * 0.5 + noise * 0.8)
            crunch * exp(-t * 11.0)
        }
        4 -> render(durationMs = 320) { t, n ->
            val f = 75.0 * exp(-t * 6.0) + 30.0
            val thud = (sin(2 * PI * f * t) * 0.95 + noiseAt(n) * 0.45 * exp(-t * 12.0))
            thud * exp(-t * 7.5)
        }
        else -> render(durationMs = 310) { t, n ->
            val whine = if (t < 0.08) sin(2 * PI * (220.0 + t * 6000.0) * t) * 0.6 else 0.0
            val f = 110.0 * exp(-(t - 0.08).coerceAtLeast(0.0) * 8.0) + 40.0
            val boom = if (t >= 0.08) (sin(2 * PI * f * t) * 0.85 + noiseAt(n) * 0.4) * exp(-(t - 0.08) * 8.0) else 0.0
            (whine + boom)
        }
    }

    /** Stellar Cosmic (Star): 5 distinct radiant celestial profiles */
    private fun synthesizeStellarVariant(slot: Int): ShortArray = when (slot) {
        1 -> render(durationMs = 190) { t, _ -> // Stellar Radiant Pulse: shimmering harmonic sine wave + celestial chord
            val f1 = 880.0 * exp(-t * 10.0) + 440.0
            val f2 = f1 * 1.5
            val shine = (sin(2 * PI * f1 * t) * 0.6 + sin(2 * PI * f2 * t) * 0.4)
            shine * exp(-t * 10.0)
        }
        2 -> render(durationMs = 220) { t, _ -> // Cosmic Beam: dense oscillating energy sweep
            val f = 660.0 + sin(2 * PI * 120.0 * t) * 150.0
            sin(2 * PI * f * t) * 0.8 * exp(-t * 9.0)
        }
        3 -> render(durationMs = 200) { t, n -> // Pulsar Harmonic: rhythmic celestial click + radiant overtone
            val click = noiseAt(n) * 0.3 * exp(-t * 60.0)
            val tone = sin(2 * PI * 1046.5 * t) * 0.7 * exp(-t * 11.0)
            (click + tone) * exp(-t * 8.0)
        }
        4 -> render(durationMs = 240) { t, _ -> // Supernova Flare: swelling radiant flare with exponential decay
            val swell = if (t < 0.06) (t / 0.06) else exp(-(t - 0.06) * 10.0)
            val f = 523.25 + t * 800.0
            sin(2 * PI * f * t) * swell * 0.85
        }
        else -> render(durationMs = 260) { t, _ -> // Graviton Lance: deep celestial gravity warp + high astral ping
            val sub = sin(2 * PI * 120.0 * t) * exp(-t * 7.0) * 0.7
            val ping = sin(2 * PI * 1318.5 * t) * exp(-t * 16.0) * 0.4
            (sub + ping) * 0.85
        }
    }

    private fun synthesizeTowerUpgrade(towerClass: TowerClass, skinSlot: Int): ShortArray = when (towerClass) {
        TowerClass.KINETIC_BALLISTIC -> when (skinSlot) {
            1 -> render(durationMs = 450) { t, _ ->
                val env = exp(-t * 7.0) * minOf(1.0, t * 120.0)
                val mech = sin(2 * PI * 180.0 * t) * exp(-t * 28.0) * 0.4
                val a = sin(2 * PI * 880.0 * t) * 0.5
                val b = sin(2 * PI * 1320.0 * (t - 0.08).coerceAtLeast(0.0)) * (if (t > 0.08) 0.6 else 0.0)
                (mech + (a + b) * env) * 0.85
            }
            2 -> render(durationMs = 420) { t, _ ->
                val step = ((t * 18.0).toInt()).coerceIn(0, 2)
                val freq = when (step) { 0 -> 1046.5; 1 -> 1318.5; else -> 1567.98 }
                val p = (t % (1.0 / 18.0)) * 18.0
                val env = exp(-p * 8.0)
                sin(2 * PI * freq * t) * env * 0.75
            }
            3 -> render(durationMs = 500) { t, n ->
                val clank1 = (sin(2 * PI * 320.0 * t) * 0.7 + noiseAt(n) * 0.4) * exp(-t * 35.0)
                val clank2 = if (t > 0.12) (sin(2 * PI * 220.0 * (t - 0.12)) * 0.8 + noiseAt(n) * 0.3) * exp(-(t - 0.12) * 25.0) else 0.0
                val hiss = noiseAt(n) * 0.25 * exp(-t * 8.0)
                (clank1 + clank2 + hiss) * 0.85
            }
            4 -> render(durationMs = 480) { t, _ ->
                val f1 = 660.0 + sin(2 * PI * 12.0 * t) * 40.0
                val f2 = 990.0
                val a = sin(2 * PI * f1 * t) * exp(-t * 5.5) * 0.6
                val b = sin(2 * PI * f2 * t) * exp(-t * 4.5) * 0.5
                (a + b) * 0.8
            }
            else -> render(durationMs = 400) { t, _ ->
                val step = ((t * 20.0).toInt()).coerceIn(0, 3)
                val f = when (step) { 0 -> 523.25; 1 -> 659.25; 2 -> 783.99; else -> 1046.5 }
                val s = sin(2 * PI * f * t)
                (if (s >= 0) 0.65 else -0.65) * exp(-t * 4.0)
            }
        }
        TowerClass.ELEMENTAL_VOID -> when (skinSlot) {
            1 -> render(durationMs = 550) { t, _ ->
                val f = 523.25 + t * 2400.0
                val shimmer = sin(2 * PI * f * t) * exp(-t * 5.0) * 0.6 + sin(2 * PI * (f * 1.5) * t) * exp(-t * 6.0) * 0.35
                shimmer * 0.85
            }
            2 -> render(durationMs = 500) { t, _ ->
                val f = 1200.0 * exp(-t * 4.0) + 800.0
                val mod = sin(2 * PI * 440.0 * t) * 0.3
                sin(2 * PI * f * t + mod) * exp(-t * 6.0) * 0.75
            }
            3 -> render(durationMs = 520) { t, n ->
                val whine = sin(2 * PI * (200.0 + t * 1800.0) * t) * (if (t < 0.2) t * 5.0 else exp(-(t - 0.2) * 12.0)) * 0.5
                val snap = if (t > 0.2) (sin(2 * PI * 850.0 * (t - 0.2)) + noiseAt(n) * 0.4) * exp(-(t - 0.2) * 30.0) else 0.0
                (whine + snap) * 0.85
            }
            4 -> render(durationMs = 600) { t, _ ->
                val sub = sin(2 * PI * 95.0 * t) * exp(-t * 4.0) * 0.7
                val astral = sin(2 * PI * 1480.0 * t) * sin(2 * PI * 8.0 * t) * exp(-t * 5.0) * 0.45
                (sub + astral) * 0.85
            }
            else -> render(durationMs = 440) { t, _ ->
                val f = 440.0 + t * 1600.0
                val s = sin(2 * PI * f * t + sin(2 * PI * 32.0 * t) * 1.5)
                (if (s >= 0) 0.6 else -0.6) * exp(-t * 4.5)
            }
        }
        TowerClass.HEAVY_SIEGE -> when (skinSlot) {
            1 -> render(durationMs = 650) { t, _ ->
                val sub = sin(2 * PI * 110.0 * t) * exp(-t * 4.5) * 0.75
                val chord = (sin(2 * PI * 440.0 * t) + sin(2 * PI * 554.37 * t)) * exp(-t * 6.0) * 0.35
                (sub + chord) * 0.85
            }
            2 -> render(durationMs = 550) { t, _ ->
                val servo = sin(2 * PI * 350.0 * t) * exp(-t * 7.0) * 0.55
                val lock = if (t > 0.15) sin(2 * PI * 700.0 * (t - 0.15)) * exp(-(t - 0.15) * 18.0) * 0.6 else 0.0
                (servo + lock) * 0.85
            }
            3 -> render(durationMs = 700) { t, n ->
                val thud = (sin(2 * PI * 65.0 * t) * 0.9 + noiseAt(n) * 0.4) * exp(-t * 8.0)
                val metal = if (t > 0.08) (sin(2 * PI * 480.0 * (t - 0.08)) + noiseAt(n) * 0.3) * exp(-(t - 0.08) * 22.0) * 0.6 else 0.0
                (thud + metal) * 0.9
            }
            4 -> render(durationMs = 680) { t, _ ->
                val rumble = sin(2 * PI * 55.0 * t) * exp(-t * 3.5) * 0.8
                val bell = sin(2 * PI * 370.0 * t) * exp(-t * 5.0) * 0.4
                (rumble + bell) * 0.85
            }
            else -> render(durationMs = 480) { t, _ ->
                val step = ((t * 16.0).toInt()).coerceIn(0, 3)
                val f = when (step) { 0 -> 146.83; 1 -> 220.00; 2 -> 293.66; else -> 440.00 }
                val s = sin(2 * PI * f * t)
                (if (s >= 0) 0.7 else -0.7) * exp(-t * 3.8)
            }
        }
        TowerClass.STELLAR_COSMIC -> when (skinSlot) {
            1 -> render(durationMs = 600) { t, _ -> // Stellar Radiant Star: celestial choir chime + golden harmonic shimmer
                val env = exp(-t * 4.5) * minOf(1.0, t * 100.0)
                val f1 = 659.25 // E5
                val f2 = 987.77 // B5
                val chime = (sin(2 * PI * f1 * t) * 0.5 + sin(2 * PI * f2 * t) * 0.5) * env
                chime * 0.85
            }
            2 -> render(durationMs = 520) { t, _ -> // Blueprint Star: cosmic vector resonance triad
                val f = 880.0 + sin(2 * PI * 20.0 * t) * 60.0
                sin(2 * PI * f * t) * exp(-t * 5.0) * 0.75
            }
            3 -> render(durationMs = 580) { t, n -> // Heavy Astral: celestial anvil strike + star resonance
                val strike = (sin(2 * PI * 220.0 * t) * 0.8 + noiseAt(n) * 0.3) * exp(-t * 20.0)
                val ring = sin(2 * PI * 1318.5 * t) * exp(-t * 6.0) * 0.4
                (strike + ring) * 0.85
            }
            4 -> render(durationMs = 650) { t, _ -> // Void Singularity Star: deep cosmic pulsar pulse
                val sub = sin(2 * PI * 75.0 * t) * exp(-t * 4.0) * 0.75
                val flare = sin(2 * PI * 1174.66 * t) * exp(-t * 6.0) * 0.4
                (sub + flare) * 0.85
            }
            else -> render(durationMs = 460) { t, _ -> // Retro Star Fanfare: 8-bit victorious star power-up
                val step = ((t * 18.0).toInt()).coerceIn(0, 3)
                val f = when (step) { 0 -> 440.0; 1 -> 554.37; 2 -> 659.25; else -> 880.0 }
                val s = sin(2 * PI * f * t)
                (if (s >= 0) 0.65 else -0.65) * exp(-t * 4.0)
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Procedural Ambient Music Synthesis (Low-Pass Filtered < 650Hz, Warm, Subtle)
    // ---------------------------------------------------------------------------------------------

    private fun synthesizeMusic(track: MusicTrack): ShortArray = when (track) {
        MusicTrack.NONE, MusicTrack.ROLLING_PLAYLIST -> ShortArray(SAMPLE_RATE)
        MusicTrack.CYBER_AMBIENT -> render(durationMs = 45000, taperFade = true) { t, _ ->
            // Track 1: Sector Zero Synth (45s) - Am -> F -> C -> G warm analog pads (< 500Hz)
            val chordSec = 11.25
            val chordIdx = ((t / chordSec).toInt()) % 4
            val (rootFreq, thirdFreq, fifthFreq) = when (chordIdx) {
                0 -> Triple(110.00, 130.81, 164.81) // Am (A2, C3, E3)
                1 -> Triple(87.31, 110.00, 130.81)  // F  (F2, A2, C3)
                2 -> Triple(65.41, 82.41, 98.00)   // C  (C2, E2, G2)
                else -> Triple(98.00, 123.47, 146.83) // G (G2, B2, D3)
            }

            // Warm chorused pad drone
            val chorus = sin(2 * PI * 0.25 * t) * 1.0
            val padRoot = sin(2 * PI * (rootFreq + chorus) * t) * 0.35
            val padThird = sin(2 * PI * thirdFreq * t) * 0.24
            val padFifth = sin(2 * PI * fifthFreq * t) * 0.22
            val sub = sin(2 * PI * (rootFreq * 0.5) * t) * 0.30

            // Low-pass gentle melodic pulse (< 400Hz)
            val step = ((t / 0.5).toInt()) % 4
            val arpFreq = rootFreq * when (step) { 0 -> 1.0; 1 -> 1.25; 2 -> 1.5; else -> 1.25 }
            val arpPhase = (t % 0.5) / 0.5
            val arpEnv = (1.0 - arpPhase) * exp(-arpPhase * 3.5)
            val pulse = sin(2 * PI * arpFreq * t) * arpEnv * 0.18

            (padRoot + padThird + padFifth + sub + pulse) * 0.65
        }

        MusicTrack.PULSE_ARPEGGIO -> render(durationMs = 45000, taperFade = true) { t, _ ->
            // Track 2: Dual Front Pulse (45s) - Dm -> Bb -> F -> C warm bass pulse (< 450Hz)
            val barDuration = 1.875
            val progressionCycle = (t / (barDuration * 2.0)).toInt() % 4
            val rootNote = when (progressionCycle) {
                0 -> 73.42 // D2
                1 -> 58.27 // Bb1
                2 -> 87.31 // F2
                else -> 65.41 // C2
            }

            val beatPhase = (t % (barDuration / 4.0)) / (barDuration / 4.0)
            val bassEnv = exp(-beatPhase * 5.0)
            val bass = (sin(2 * PI * rootNote * t) + sin(2 * PI * rootNote * 2.0 * t) * 0.35) * bassEnv * 0.45

            val arpStep = ((t / (barDuration / 8.0)).toInt()) % 8
            val noteMult = when (arpStep % 4) { 0 -> 2.0; 1 -> 2.4; 2 -> 3.0; else -> 2.4 }
            val aPhase = (t % (barDuration / 8.0)) / (barDuration / 8.0)
            val aEnv = exp(-aPhase * 5.0)
            val subLead = sin(2 * PI * (rootNote * noteMult) * t) * aEnv * 0.22

            (bass + subLead) * 0.70
        }

        MusicTrack.DEEP_SPACE -> render(durationMs = 45000, taperFade = true) { t, _ ->
            // Track 3: Sub-Void Drone (45s) - Deep meditative space drone (< 350Hz)
            val chordDur = 11.25
            val cIdx = ((t / chordDur).toInt()) % 4
            val (baseF, pF) = when (cIdx) {
                0 -> Pair(65.41, 98.00)   // C2 & G2
                1 -> Pair(51.91, 77.78)   // Ab1 & Eb2
                2 -> Pair(77.78, 116.54)  // Eb2 & Bb2
                else -> Pair(49.00, 73.42) // G1 & D2
            }

            val lfo = sin(2 * PI * 0.10 * t) * 2.0
            val sub = sin(2 * PI * (baseF * 0.5) * t) * 0.38
            val pad1 = sin(2 * PI * (baseF + lfo) * t) * 0.36
            val pad2 = sin(2 * PI * (pF - lfo * 0.5) * t) * 0.30

            (sub + pad1 + pad2) * 0.65
        }

        MusicTrack.NEON_OVERDRIVE -> render(durationMs = 45000, taperFade = true) { t, _ ->
            // Track 4: Neon Overdrive (45s) - Warm analog bassline (< 400Hz)
            val barLen = 1.777
            val step4 = ((t / (barLen * 2.0)).toInt()) % 4
            val bassRoot = when (step4) {
                0 -> 92.50 // F#2
                1 -> 73.42 // D2
                2 -> 110.00 // A2
                else -> 82.41 // E2
            }

            val bStep = ((t / (barLen / 4.0)).toInt()) % 4
            val bPhase = (t % (barLen / 4.0)) / (barLen / 4.0)
            val bEnv = exp(-bPhase * 4.5)
            val bass = (sin(2 * PI * bassRoot * t) + sin(2 * PI * bassRoot * 2.0 * t) * 0.3) * bEnv * 0.45

            val chord = sin(2 * PI * (bassRoot * 1.5) * t) * 0.25 * exp(-t * 0.5)

            (bass + chord) * 0.65
        }

        MusicTrack.CELESTIAL_BASTION -> render(durationMs = 45000, taperFade = true) { t, _ ->
            // Track 5: Celestial Bastion (45s) - Em -> C -> G -> D warm space hymn (< 500Hz)
            val barLen = 2.0
            val cIdx = ((t / (barLen * 2.0)).toInt()) % 4
            val emRoot = when (cIdx) {
                0 -> 82.41 // Em
                1 -> 65.41 // C
                2 -> 98.00 // G
                else -> 73.42 // D
            }

            val drone = (sin(2 * PI * emRoot * t) * 0.38 + sin(2 * PI * (emRoot * 1.5) * t) * 0.28)
            val warmFifth = sin(2 * PI * (emRoot * 2.0) * t) * 0.22

            (drone + warmFifth) * 0.65
        }
    }

    private val noiseRandom = Random(0xC0FFEE)
    private fun noiseAt(@Suppress("UNUSED_PARAMETER") n: Int): Double = noiseRandom.nextDouble() * 2.0 - 1.0

    private inline fun render(
        durationMs: Int,
        taperFade: Boolean = false,
        sample: (t: Double, n: Int) -> Double
    ): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        val out = ShortArray(count)
        val taperSamples = if (taperFade) (SAMPLE_RATE * 2).coerceAtMost(count / 4) else (SAMPLE_RATE / 100)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            var v = sample(t, i)
            if (taperFade && i < taperSamples) {
                val fadeIn = 0.5 * (1.0 - cos(PI * i / taperSamples))
                v *= fadeIn
            }
            if (i > count - taperSamples) {
                val fadeOut = 0.5 * (1.0 + cos(PI * (count - i) / taperSamples))
                v *= fadeOut
            }
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
