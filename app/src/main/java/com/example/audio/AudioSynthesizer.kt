package com.example.audio

import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Core PCM sample renderer, WAV container encoder, and ambient music synthesizer.
 * Decoupled from [SoundManager] to adhere to the 750-Line Architectural Modularity Law.
 */
internal object AudioSynthesizer {

    const val SAMPLE_RATE = 22050
    private val noiseRandom = Random(0xC0FFEE)

    fun noiseAt(@Suppress("UNUSED_PARAMETER") n: Int): Double = noiseRandom.nextDouble() * 2.0 - 1.0

    inline fun render(
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

    fun toWav(pcm: ShortArray): ByteArray {
        val dataLen = pcm.size * 2
        val bos = ByteArrayOutputStream(44 + dataLen)
        fun writeInt(v: Int) {
            bos.write(v and 0xFF)
            bos.write((v shr 8) and 0xFF)
            bos.write((v shr 16) and 0xFF)
            bos.write((v shr 24) and 0xFF)
        }
        fun writeShort(v: Int) {
            bos.write(v and 0xFF)
            bos.write((v shr 8) and 0xFF)
        }
        bos.write("RIFF".toByteArray())
        writeInt(36 + dataLen)
        bos.write("WAVE".toByteArray())
        bos.write("fmt ".toByteArray())
        writeInt(16)
        writeShort(1)
        writeShort(1)
        writeInt(SAMPLE_RATE)
        writeInt(SAMPLE_RATE * 2)
        writeShort(2)
        writeShort(16)
        bos.write("data".toByteArray())
        writeInt(dataLen)
        for (s in pcm) writeShort(s.toInt())
        return bos.toByteArray()
    }

    fun synthesizeMusic(track: MusicTrack): ShortArray = when (track) {
        MusicTrack.NONE, MusicTrack.ROLLING_PLAYLIST -> ShortArray(SAMPLE_RATE)
        MusicTrack.CYBER_AMBIENT -> render(durationMs = 45000, taperFade = true) { t, _ ->
            val chordSec = 11.25
            val chordIdx = ((t / chordSec).toInt()) % 4
            val (rootFreq, thirdFreq, fifthFreq) = when (chordIdx) {
                0 -> Triple(110.00, 130.81, 164.81)
                1 -> Triple(87.31, 110.00, 130.81)
                2 -> Triple(65.41, 82.41, 98.00)
                else -> Triple(98.00, 123.47, 146.83)
            }
            val chorus = sin(2 * PI * 0.25 * t) * 1.0
            val padRoot = sin(2 * PI * (rootFreq + chorus) * t) * 0.35
            val padThird = sin(2 * PI * thirdFreq * t) * 0.24
            val padFifth = sin(2 * PI * fifthFreq * t) * 0.22
            val sub = sin(2 * PI * (rootFreq * 0.5) * t) * 0.30
            val step = ((t / 0.5).toInt()) % 4
            val arpFreq = rootFreq * when (step) { 0 -> 1.0; 1 -> 1.25; 2 -> 1.5; else -> 1.25 }
            val arpPhase = (t % 0.5) / 0.5
            val arpEnv = (1.0 - arpPhase) * exp(-arpPhase * 3.5)
            val pulse = sin(2 * PI * arpFreq * t) * arpEnv * 0.18
            (padRoot + padThird + padFifth + sub + pulse) * 0.65
        }
        MusicTrack.PULSE_ARPEGGIO -> render(durationMs = 45000, taperFade = true) { t, _ ->
            val barDuration = 1.875
            val progressionCycle = (t / (barDuration * 2.0)).toInt() % 4
            val rootNote = when (progressionCycle) {
                0 -> 73.42
                1 -> 58.27
                2 -> 87.31
                else -> 65.41
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
            val chordDur = 11.25
            val cIdx = ((t / chordDur).toInt()) % 4
            val (baseF, pF) = when (cIdx) {
                0 -> Pair(65.41, 98.00)
                1 -> Pair(51.91, 77.78)
                2 -> Pair(77.78, 116.54)
                else -> Pair(49.00, 73.42)
            }
            val lfo = sin(2 * PI * 0.10 * t) * 2.0
            val sub = sin(2 * PI * (baseF * 0.5) * t) * 0.38
            val pad1 = sin(2 * PI * (baseF + lfo) * t) * 0.36
            val pad2 = sin(2 * PI * (pF - lfo * 0.5) * t) * 0.30
            (sub + pad1 + pad2) * 0.65
        }
        MusicTrack.NEON_OVERDRIVE -> render(durationMs = 45000, taperFade = true) { t, _ ->
            val barLen = 1.777
            val step4 = ((t / (barLen * 2.0)).toInt()) % 4
            val bassRoot = when (step4) {
                0 -> 92.50
                1 -> 73.42
                2 -> 110.00
                else -> 82.41
            }
            val bPhase = (t % (barLen / 4.0)) / (barLen / 4.0)
            val bEnv = exp(-bPhase * 4.5)
            val bass = (sin(2 * PI * bassRoot * t) + sin(2 * PI * bassRoot * 2.0 * t) * 0.3) * bEnv * 0.45
            val chord = sin(2 * PI * (bassRoot * 1.5) * t) * 0.25 * exp(-t * 0.5)
            (bass + chord) * 0.65
        }
        MusicTrack.CELESTIAL_BASTION -> render(durationMs = 45000, taperFade = true) { t, _ ->
            val barLen = 2.0
            val cIdx = ((t / (barLen * 2.0)).toInt()) % 4
            val emRoot = when (cIdx) {
                0 -> 82.41
                1 -> 65.41
                2 -> 98.00
                else -> 73.42
            }
            val drone = (sin(2 * PI * emRoot * t) * 0.38 + sin(2 * PI * (emRoot * 1.5) * t) * 0.28)
            val warmFifth = sin(2 * PI * (emRoot * 2.0) * t) * 0.22
            (drone + warmFifth) * 0.65
        }
    }
}
