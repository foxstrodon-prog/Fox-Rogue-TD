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

/**
 * Lightweight SoundPool wrapper. Tones are synthesised to 16-bit mono PCM, written once as tiny WAV
 * files into the app cache directory and loaded into a [SoundPool]. [play] is allocation-free and
 * rate-limited per effect so dense bullet storms never flood the mixer.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext
    private val soundPool: SoundPool
    private val soundIds = IntArray(SoundFx.values().size)
    private val loaded = BooleanArray(SoundFx.values().size)
    private val lastPlayNs = LongArray(SoundFx.values().size)

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var masterVolume: Float = 0.7f

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        val idToIndex = HashMap<Int, Int>()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            val idx = idToIndex[sampleId]
            if (idx != null && status == 0) loaded[idx] = true
        }

        for (fx in SoundFx.values()) {
            try {
                val file = File(appContext.cacheDir, "sfx_${fx.name.lowercase()}_v1.wav")
                if (!file.exists() || file.length() < 64L) {
                    FileOutputStream(file).use { it.write(toWav(synthesize(fx))) }
                }
                val id = soundPool.load(file.absolutePath, 1)
                soundIds[fx.ordinal] = id
                idToIndex[id] = fx.ordinal
            } catch (_: Exception) {
                soundIds[fx.ordinal] = 0
            }
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
        val v = (volume * masterVolume).coerceIn(0f, 1f)
        soundPool.play(soundIds[idx], v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun release() {
        soundPool.release()
    }

    // ---------------------------------------------------------------------------------------------
    // Synthesis
    // ---------------------------------------------------------------------------------------------

    private fun synthesize(fx: SoundFx): ShortArray = when (fx) {
        // Short low sine drop + noise burst: a dull "thud"
        SoundFx.PLACE -> render(durationMs = 140) { t, n ->
            val f = 150.0 * exp(-t * 18.0) + 55.0
            val env = exp(-t * 22.0)
            (sin(2 * PI * f * t) * 0.9 + noiseAt(n) * 0.25 * exp(-t * 60.0)) * env
        }
        // Two rising sine partials with slight shimmer: a bright "chime"
        SoundFx.UPGRADE -> render(durationMs = 420) { t, _ ->
            val env = exp(-t * 6.5) * minOf(1.0, t * 80.0)
            val a = sin(2 * PI * 880.0 * t)
            val b = sin(2 * PI * 1318.5 * (t - 0.09).coerceAtLeast(0.0)) * (if (t > 0.09) 1.0 else 0.0)
            val c = sin(2 * PI * 1760.0 * t) * 0.35
            (a * 0.55 + b * 0.55 + c) * env
        }
        // Fast downward square-ish "pew"
        SoundFx.FIRE_KINETIC -> render(durationMs = 90) { t, _ ->
            val f = 1500.0 * exp(-t * 28.0) + 420.0
            val env = exp(-t * 30.0)
            val s = sin(2 * PI * f * t)
            (if (s >= 0) 0.6 else -0.6) * env
        }
        // FM zap: wobbling carrier
        SoundFx.FIRE_ELEMENTAL -> render(durationMs = 170) { t, _ ->
            val mod = sin(2 * PI * 90.0 * t) * 6.0
            val f = 700.0 + 900.0 * exp(-t * 14.0)
            val env = exp(-t * 14.0)
            sin(2 * PI * f * t + mod) * 0.8 * env
        }
        // Deep boom: very low sine plus rumbling noise tail
        SoundFx.FIRE_SIEGE -> render(durationMs = 300) { t, n ->
            val f = 95.0 * exp(-t * 7.0) + 38.0
            val env = exp(-t * 9.0)
            (sin(2 * PI * f * t) * 0.9 + noiseAt(n) * 0.35 * exp(-t * 16.0)) * env
        }
        // Harsh descending square buzz
        SoundFx.INVALID -> render(durationMs = 190) { t, _ ->
            val f = 190.0 - 40.0 * (t / 0.19)
            val s = sin(2 * PI * f * t)
            val gate = if (((t * 28.0).toInt() % 2) == 0) 1.0 else 0.35
            (if (s >= 0) 0.55 else -0.55) * gate * exp(-t * 5.5)
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
            // Gentle fade-out in the last 5 ms to avoid clicks
            val tailSamples = SAMPLE_RATE / 200
            if (i > count - tailSamples) v *= (count - i).toDouble() / tailSamples
            v = v.coerceIn(-1.0, 1.0)
            out[i] = (v * 0.8 * Short.MAX_VALUE).toInt().toShort()
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
