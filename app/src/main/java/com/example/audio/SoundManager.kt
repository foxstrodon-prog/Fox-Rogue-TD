package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
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
    FIRE_KINETIC("Kinetic Fire", 70L),
    FIRE_ELEMENTAL("Elemental Fire", 90L),
    FIRE_SIEGE("Siege Fire", 120L),
    INVALID("Invalid Tap Buzz", 120L)
}

enum class SoundProfile(val label: String) {
    CYBER_SYNTH("Cyber Synth (Default)"),
    RETRO_8BIT("Retro 8-Bit Arcade"),
    DEEP_ACOUSTIC("Heavy Acoustic Impact"),
    SCI_FI_LASER("Sci-Fi Laser Grid")
}

enum class MusicTrack(val label: String) {
    NONE("Mute Music"),
    CYBER_AMBIENT("Sector Zero Synth"),
    PULSE_ARPEGGIO("Dual Front Pulse"),
    DEEP_SPACE("Sub-Void Drone")
}

/**
 * Lightweight SoundPool wrapper. Tones are synthesised to 16-bit mono PCM, written once as tiny WAV
 * files into the app cache directory and loaded into a [SoundPool]. [play] is allocation-free and
 * rate-limited per effect so dense bullet storms never flood the mixer.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext
    private var soundPool: SoundPool
    private val soundIds = IntArray(SoundFx.values().size)
    private val loaded = BooleanArray(SoundFx.values().size)
    private val lastPlayNs = LongArray(SoundFx.values().size)

    private var musicSoundId: Int = 0
    private var musicLoaded: Boolean = false
    private var musicStreamId: Int = 0

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var masterVolume: Float = 0.7f

    @Volatile
    var sfxVolume: Float = 0.8f

    @Volatile
    var musicVolume: Float = 0.45f

    @Volatile
    var activeProfile: SoundProfile = SoundProfile.CYBER_SYNTH
        set(value) {
            field = value
            loadEffectsForProfile(value)
        }

    @Volatile
    var activeMusicTrack: MusicTrack = MusicTrack.CYBER_AMBIENT
        set(value) {
            field = value
            updateMusicTrack(value)
        }

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()

        loadEffectsForProfile(activeProfile)
        setupMusicTrack(activeMusicTrack)
    }

    private fun loadEffectsForProfile(profile: SoundProfile) {
        val idToIndex = HashMap<Int, Int>()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            val idx = idToIndex[sampleId]
            if (idx != null && status == 0) loaded[idx] = true
            if (sampleId == musicSoundId && status == 0) {
                musicLoaded = true
                if (activeMusicTrack != MusicTrack.NONE && enabled) {
                    startMusicStream()
                }
            }
        }

        for (fx in SoundFx.values()) {
            try {
                val filename = "sfx_${profile.name.lowercase()}_${fx.name.lowercase()}_v2.wav"
                val file = File(appContext.cacheDir, filename)
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use { it.write(toWav(synthesize(fx, profile))) }
                }
                val id = soundPool.load(file.absolutePath, 1)
                soundIds[fx.ordinal] = id
                idToIndex[id] = fx.ordinal
            } catch (_: Exception) {
                soundIds[fx.ordinal] = 0
            }
        }
    }

    private fun setupMusicTrack(track: MusicTrack) {
        if (track == MusicTrack.NONE) {
            stopMusicStream()
            return
        }
        try {
            val filename = "bgm_${track.name.lowercase()}_v1.wav"
            val file = File(appContext.cacheDir, filename)
            if (!file.exists() || file.length() < 256L) {
                FileOutputStream(file).use { it.write(toWav(synthesizeMusic(track))) }
            }
            musicLoaded = false
            musicSoundId = soundPool.load(file.absolutePath, 2)
        } catch (_: Exception) {
            musicSoundId = 0
        }
    }

    private fun updateMusicTrack(track: MusicTrack) {
        stopMusicStream()
        if (track != MusicTrack.NONE) {
            setupMusicTrack(track)
        }
    }

    fun startMusicStream() {
        if (!enabled || !musicLoaded || musicSoundId == 0 || activeMusicTrack == MusicTrack.NONE) return
        if (musicStreamId != 0) {
            soundPool.stop(musicStreamId)
        }
        val vol = (masterVolume * musicVolume).coerceIn(0f, 1f)
        musicStreamId = soundPool.play(musicSoundId, vol, vol, 2, -1, 1.0f)
    }

    fun stopMusicStream() {
        if (musicStreamId != 0) {
            soundPool.stop(musicStreamId)
            musicStreamId = 0
        }
    }

    fun setMasterVolumeLevel(level: Float) {
        masterVolume = level.coerceIn(0f, 1f)
        updateStreamVolumes()
    }

    fun setSfxVolumeLevel(level: Float) {
        sfxVolume = level.coerceIn(0f, 1f)
    }

    fun setMusicVolumeLevel(level: Float) {
        musicVolume = level.coerceIn(0f, 1f)
        updateStreamVolumes()
    }

    private fun updateStreamVolumes() {
        if (musicStreamId != 0) {
            val vol = (masterVolume * musicVolume).coerceIn(0f, 1f)
            soundPool.setVolume(musicStreamId, vol, vol)
        }
    }

    /**
     * Plays an effect. [rate] (0.5..2.0) varies pitch; [volume] is 0..1 relative to the master volume.
     */
    fun play(fx: SoundFx, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled) return
        val idx = fx.ordinal
        if (!loaded[idx]) return
        val now = System.nanoTime()
        if (now - lastPlayNs[idx] < fx.minIntervalMs * 1_000_000L) return
        lastPlayNs[idx] = now
        val v = (volume * masterVolume * sfxVolume).coerceIn(0f, 1f)
        soundPool.play(soundIds[idx], v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun release() {
        stopMusicStream()
        soundPool.release()
    }

    // ---------------------------------------------------------------------------------------------
    // Procedural SFX & Music Synthesis (100% Asset-Free, Zero Copyright Risk)
    // ---------------------------------------------------------------------------------------------

    private fun synthesize(fx: SoundFx, profile: SoundProfile): ShortArray = when (profile) {
        SoundProfile.CYBER_SYNTH -> synthesizeCyber(fx)
        SoundProfile.RETRO_8BIT -> synthesizeRetro(fx)
        SoundProfile.DEEP_ACOUSTIC -> synthesizeAcoustic(fx)
        SoundProfile.SCI_FI_LASER -> synthesizeLaser(fx)
    }

    private fun synthesizeCyber(fx: SoundFx): ShortArray = when (fx) {
        SoundFx.PLACE -> render(durationMs = 140) { t, n ->
            val f = 150.0 * exp(-t * 18.0) + 55.0
            val env = exp(-t * 22.0)
            (sin(2 * PI * f * t) * 0.9 + noiseAt(n) * 0.25 * exp(-t * 60.0)) * env
        }
        SoundFx.UPGRADE -> render(durationMs = 420) { t, _ ->
            val env = exp(-t * 6.5) * minOf(1.0, t * 80.0)
            val a = sin(2 * PI * 880.0 * t)
            val b = sin(2 * PI * 1318.5 * (t - 0.09).coerceAtLeast(0.0)) * (if (t > 0.09) 1.0 else 0.0)
            val c = sin(2 * PI * 1760.0 * t) * 0.35
            (a * 0.55 + b * 0.55 + c) * env
        }
        SoundFx.FIRE_KINETIC -> render(durationMs = 90) { t, _ ->
            val f = 1500.0 * exp(-t * 28.0) + 420.0
            val env = exp(-t * 30.0)
            val s = sin(2 * PI * f * t)
            (if (s >= 0) 0.6 else -0.6) * env
        }
        SoundFx.FIRE_ELEMENTAL -> render(durationMs = 170) { t, _ ->
            val mod = sin(2 * PI * 90.0 * t) * 6.0
            val f = 700.0 + 900.0 * exp(-t * 14.0)
            val env = exp(-t * 14.0)
            sin(2 * PI * f * t + mod) * 0.8 * env
        }
        SoundFx.FIRE_SIEGE -> render(durationMs = 300) { t, n ->
            val f = 95.0 * exp(-t * 7.0) + 38.0
            val env = exp(-t * 9.0)
            (sin(2 * PI * f * t) * 0.9 + noiseAt(n) * 0.35 * exp(-t * 16.0)) * env
        }
        SoundFx.INVALID -> render(durationMs = 190) { t, _ ->
            val f = 190.0 - 40.0 * (t / 0.19)
            val s = sin(2 * PI * f * t)
            val gate = if (((t * 28.0).toInt() % 2) == 0) 1.0 else 0.35
            (if (s >= 0) 0.55 else -0.55) * gate * exp(-t * 5.5)
        }
    }

    private fun synthesizeRetro(fx: SoundFx): ShortArray = when (fx) {
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
        SoundFx.FIRE_KINETIC -> render(durationMs = 70) { t, _ ->
            val f = 900.0 - t * 7000.0
            val s = sin(2 * PI * f.coerceAtLeast(100.0) * t)
            (if (s >= 0) 0.7 else -0.7) * exp(-t * 35.0)
        }
        SoundFx.FIRE_ELEMENTAL -> render(durationMs = 140) { t, _ ->
            val f = 400.0 + sin(2 * PI * 60.0 * t) * 200.0
            val s = sin(2 * PI * f * t)
            (if (s >= 0) 0.65 else -0.65) * exp(-t * 12.0)
        }
        SoundFx.FIRE_SIEGE -> render(durationMs = 260) { t, n ->
            val noise = noiseAt(n)
            val env = exp(-t * 10.0)
            noise * 0.85 * env
        }
        SoundFx.INVALID -> render(durationMs = 160) { t, _ ->
            val s = sin(2 * PI * 130.0 * t)
            (if (s >= 0) 0.7 else -0.7) * exp(-t * 6.0)
        }
    }

    private fun synthesizeAcoustic(fx: SoundFx): ShortArray = when (fx) {
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
        SoundFx.FIRE_KINETIC -> render(durationMs = 110) { t, n ->
            val f = 200.0 * exp(-t * 30.0) + 80.0
            (sin(2 * PI * f * t) * 0.8 + noiseAt(n) * 0.3) * exp(-t * 25.0)
        }
        SoundFx.FIRE_ELEMENTAL -> render(durationMs = 190) { t, _ ->
            val a = sin(2 * PI * 330.0 * t) * exp(-t * 9.0)
            val b = sin(2 * PI * 660.0 * t) * 0.5 * exp(-t * 12.0)
            (a + b) * 0.7
        }
        SoundFx.FIRE_SIEGE -> render(durationMs = 380) { t, n ->
            val f = 75.0 * exp(-t * 5.0) + 30.0
            (sin(2 * PI * f * t) * 0.95 + noiseAt(n) * 0.45 * exp(-t * 10.0)) * exp(-t * 7.0)
        }
        SoundFx.INVALID -> render(durationMs = 210) { t, _ ->
            val f = 110.0
            sin(2 * PI * f * t) * 0.8 * exp(-t * 8.0)
        }
    }

    private fun synthesizeLaser(fx: SoundFx): ShortArray = when (fx) {
        SoundFx.PLACE -> render(durationMs = 130) { t, _ ->
            val f = 300.0 * exp(-t * 15.0)
            sin(2 * PI * f * t) * 0.8 * exp(-t * 18.0)
        }
        SoundFx.UPGRADE -> render(durationMs = 400) { t, _ ->
            val f = 600.0 + t * 1400.0
            sin(2 * PI * f * t) * 0.7 * exp(-t * 6.0)
        }
        SoundFx.FIRE_KINETIC -> render(durationMs = 85) { t, _ ->
            val f = 2400.0 * exp(-t * 40.0) + 400.0
            sin(2 * PI * f * t) * 0.85 * exp(-t * 32.0)
        }
        SoundFx.FIRE_ELEMENTAL -> render(durationMs = 160) { t, _ ->
            val f = 1800.0 * exp(-t * 20.0) + 600.0
            val fm = sin(2 * PI * 120.0 * t) * 8.0
            sin(2 * PI * f * t + fm) * 0.8 * exp(-t * 15.0)
        }
        SoundFx.FIRE_SIEGE -> render(durationMs = 290) { t, n ->
            val f = 180.0 * exp(-t * 10.0) + 45.0
            (sin(2 * PI * f * t) * 0.8 + noiseAt(n) * 0.3) * exp(-t * 12.0)
        }
        SoundFx.INVALID -> render(durationMs = 180) { t, _ ->
            val f = 350.0 - t * 800.0
            sin(2 * PI * f.coerceAtLeast(80.0) * t) * 0.7 * exp(-t * 10.0)
        }
    }

    private fun synthesizeMusic(track: MusicTrack): ShortArray = when (track) {
        MusicTrack.NONE -> ShortArray(SAMPLE_RATE)
        MusicTrack.CYBER_AMBIENT -> render(durationMs = 4000) { t, _ ->
            // Atmospheric looping minor 7th chord drone + subtle arpeggio
            val root = sin(2 * PI * 110.0 * t) * 0.35 // A2
            val fifth = sin(2 * PI * 164.81 * t) * 0.25 // E3
            val minorThird = sin(2 * PI * 130.81 * t) * 0.20 // C3
            val sub = sin(2 * PI * 55.0 * t) * 0.30 // A1

            // 8-step gentle pulsing arpeggio at 120 BPM (0.5s per beat, 0.25s per note)
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
            // Faster electronic pulse arpeggio in D minor
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
            // Dark meditative ambient pad with slow pitch modulation
            val mod = sin(2 * PI * 0.25 * t) * 4.0
            val pad1 = sin(2 * PI * (65.41 + mod) * t) * 0.40 // C2
            val pad2 = sin(2 * PI * 98.00 * t) * 0.30 // G2
            val shimmer = sin(2 * PI * 523.25 * t) * 0.08 * sin(2 * PI * 0.5 * t)
            (pad1 + pad2 + shimmer) * 0.65
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
            // Gentle fade-out in the last 10 ms to avoid clicks
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
