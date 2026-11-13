package com.example.audio

import com.example.audio.AudioSynthesizer.noiseAt
import com.example.audio.AudioSynthesizer.render
import com.example.model.TowerClass
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural SFX Synthesizer for turret gunfire variants, tower upgrade chimes, and UI sound effects.
 */
internal object ProceduralAudioSynthesizer {

    fun synthesizeGeneric(fx: SoundFx, profile: SoundProfile): ShortArray = when (profile) {
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

    fun synthesizeKineticVariant(slot: Int): ShortArray = when (slot) {
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

    fun synthesizeElementalVariant(slot: Int): ShortArray = when (slot) {
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

    fun synthesizeSiegeVariant(slot: Int): ShortArray = when (slot) {
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

    fun synthesizeStellarVariant(slot: Int): ShortArray = when (slot) {
        1 -> render(durationMs = 190) { t, _ ->
            val f1 = 880.0 * exp(-t * 10.0) + 440.0
            val f2 = f1 * 1.5
            val shine = (sin(2 * PI * f1 * t) * 0.6 + sin(2 * PI * f2 * t) * 0.4)
            shine * exp(-t * 10.0)
        }
        2 -> render(durationMs = 220) { t, _ ->
            val f = 660.0 + sin(2 * PI * 120.0 * t) * 150.0
            sin(2 * PI * f * t) * 0.8 * exp(-t * 9.0)
        }
        3 -> render(durationMs = 200) { t, n ->
            val click = noiseAt(n) * 0.3 * exp(-t * 60.0)
            val tone = sin(2 * PI * 1046.5 * t) * 0.7 * exp(-t * 11.0)
            (click + tone) * exp(-t * 8.0)
        }
        4 -> render(durationMs = 240) { t, _ ->
            val swell = if (t < 0.06) (t / 0.06) else exp(-(t - 0.06) * 10.0)
            val f = 523.25 + t * 800.0
            sin(2 * PI * f * t) * swell * 0.85
        }
        else -> render(durationMs = 260) { t, _ ->
            val sub = sin(2 * PI * 120.0 * t) * exp(-t * 7.0) * 0.7
            val ping = sin(2 * PI * 1318.5 * t) * exp(-t * 16.0) * 0.4
            (sub + ping) * 0.85
        }
    }

    fun synthesizeTowerUpgrade(towerClass: TowerClass, skinSlot: Int): ShortArray = when (towerClass) {
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
            1 -> render(durationMs = 600) { t, _ ->
                val env = exp(-t * 4.5) * minOf(1.0, t * 100.0)
                val f1 = 659.25
                val f2 = 987.77
                val chime = (sin(2 * PI * f1 * t) * 0.5 + sin(2 * PI * f2 * t) * 0.5) * env
                chime * 0.85
            }
            2 -> render(durationMs = 520) { t, _ ->
                val f = 880.0 + sin(2 * PI * 20.0 * t) * 60.0
                sin(2 * PI * f * t) * exp(-t * 5.0) * 0.75
            }
            3 -> render(durationMs = 580) { t, n ->
                val strike = (sin(2 * PI * 220.0 * t) * 0.8 + noiseAt(n) * 0.3) * exp(-t * 20.0)
                val ring = sin(2 * PI * 1318.5 * t) * exp(-t * 6.0) * 0.4
                (strike + ring) * 0.85
            }
            4 -> render(durationMs = 650) { t, _ ->
                val sub = sin(2 * PI * 75.0 * t) * exp(-t * 4.0) * 0.75
                val flare = sin(2 * PI * 1174.66 * t) * exp(-t * 6.0) * 0.4
                (sub + flare) * 0.85
            }
            else -> render(durationMs = 460) { t, _ ->
                val step = ((t * 18.0).toInt()).coerceIn(0, 3)
                val f = when (step) { 0 -> 440.0; 1 -> 554.37; 2 -> 659.25; else -> 880.0 }
                val s = sin(2 * PI * f * t)
                (if (s >= 0) 0.65 else -0.65) * exp(-t * 4.0)
            }
        }
    }
}
