package com.example.audio

import com.example.audio.AudioSynthesizer.noiseAt
import com.example.audio.AudioSynthesizer.render
import com.example.model.ArchiveSection
import com.example.model.TowerClass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural audio synthesis registry keyed by immutable build-scoped asset ID.
 * Enables lazy on-demand generation on Dispatchers.IO for Build #61+ tracks/SFX
 * as well as Build #60 archived recipes without eager startup synthesis.
 */
internal object ArchivedAudioSynthesizer {

    fun synthesizeByAssetId(rawAssetId: String): ShortArray? {
        val assetId = BuildArchiveCatalog.migrateLegacyAssetId(rawAssetId)

        BuildArchiveCatalog.bgmById[assetId]?.let { bgm ->
            val legacy = bgm.legacyTrackName?.let { name ->
                runCatching { MusicTrack.valueOf(name) }.getOrNull()
            }
            if (legacy != null) {
                val target = if (legacy == MusicTrack.ROLLING_PLAYLIST) MusicTrack.CYBER_AMBIENT else legacy
                return AudioSynthesizer.synthesizeMusic(target)
            }
            return synthesizeB61Music(assetId)
        }

        BuildArchiveCatalog.sfxPacksById[assetId]?.let { pack ->
            if (pack.buildCode == B60_BUILD_CODE || pack.isDefault) {
                val prof = runCatching { SoundProfile.valueOf(pack.legacyProfileName) }.getOrDefault(SoundProfile.CYBER_SYNTH)
                return ProceduralAudioSynthesizer.synthesizeGeneric(SoundFx.UPGRADE, prof)
            }
            return synthesizeB61SfxPackPreview(assetId)
        }

        if (assetId.startsWith("default_sfx_")) {
            val fxName = assetId.removePrefix("default_sfx_").uppercase()
            val fx = SoundFx.entries.firstOrNull { it.name == fxName } ?: return null
            return ProceduralAudioSynthesizer.synthesizeGeneric(fx, SoundProfile.CYBER_SYNTH)
        }

        val audioDef = BuildArchiveCatalog.turretAudioById[assetId] ?: return null
        return when (audioDef.section) {
            ArchiveSection.TURRET_FIRE -> {
                if (audioDef.buildCode == CURRENT_BUILD_CODE && !audioDef.isDefault) {
                    synthesizeB61Fire(audioDef.towerClass, audioDef.slotIndex)
                } else {
                    synthesizeLegacyFire(audioDef.towerClass, audioDef.slotIndex)
                }
            }
            ArchiveSection.TURRET_UPGRADE -> {
                if (audioDef.buildCode == CURRENT_BUILD_CODE && !audioDef.isDefault) {
                    synthesizeB61Upgrade(audioDef.towerClass, audioDef.slotIndex)
                } else {
                    ProceduralAudioSynthesizer.synthesizeTowerUpgrade(audioDef.towerClass, audioDef.slotIndex)
                }
            }
            else -> null
        }
    }

    fun synthesizeB61Fire(towerClass: TowerClass, slot: Int): ShortArray {
        if (slot <= 1) return synthesizeLegacyFire(towerClass, 1)
        return when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> when (slot) {
                2 -> render(durationMs = 80) { t, n ->
                    val f = 1850.0 * exp(-t * 34.0) + 340.0
                    (sin(2 * PI * f * t) * 0.75 + noiseAt(n) * 0.35 * exp(-t * 65.0)) * exp(-t * 28.0)
                }
                3 -> render(durationMs = 68) { t, n ->
                    val f = 2600.0 * exp(-t * 48.0) + 520.0
                    (sin(2 * PI * f * t) * 0.65 + cos(2 * PI * f * 1.5 * t) * 0.25 + noiseAt(n) * 0.3 * exp(-t * 90.0)) * exp(-t * 36.0)
                }
                4 -> render(durationMs = 105) { t, n ->
                    val f = 290.0 * exp(-t * 18.0) + 95.0
                    val mag = sin(2 * PI * f * t) * 0.85 + sin(2 * PI * f * 3.0 * t) * 0.25
                    (mag + noiseAt(n) * 0.25 * exp(-t * 50.0)) * exp(-t * 20.0)
                }
                else -> render(durationMs = 90) { t, _ ->
                    val sweep = 3400.0 * exp(-t * 42.0) + 760.0
                    val ring = sin(2 * PI * sweep * t) * 0.7 + sin(2 * PI * 210.0 * t) * 0.35
                    ring * exp(-t * 26.0)
                }
            }
            TowerClass.ELEMENTAL_VOID -> when (slot) {
                2 -> render(durationMs = 175) { t, _ ->
                    val f = 1480.0 - t * 2200.0
                    val glass = sin(2 * PI * f.coerceAtLeast(380.0) * t) * 0.7 + sin(2 * PI * 2960.0 * t) * 0.25 * exp(-t * 20.0)
                    glass * exp(-t * 12.0)
                }
                3 -> render(durationMs = 165) { t, _ ->
                    val mod = sin(2 * PI * 140.0 * t) * 8.0
                    val f = 540.0 + t * 1800.0
                    sin(2 * PI * f * t + mod) * 0.8 * exp(-t * 14.0)
                }
                4 -> render(durationMs = 195) { t, n ->
                    val sub = sin(2 * PI * (240.0 * exp(-t * 9.0) + 75.0) * t) * 0.8
                    val snap = noiseAt(n) * 0.35 * exp(-t * 38.0)
                    (sub + snap) * exp(-t * 10.0)
                }
                else -> render(durationMs = 185) { t, _ ->
                    val f = 920.0 + sin(2 * PI * 65.0 * t) * 240.0
                    (sin(2 * PI * f * t) * 0.55 + sin(2 * PI * f * 1.5 * t) * 0.35) * exp(-t * 11.5)
                }
            }
            TowerClass.HEAVY_SIEGE -> when (slot) {
                2 -> render(durationMs = 310) { t, n ->
                    val f = 68.0 * exp(-t * 6.0) + 34.0
                    (sin(2 * PI * f * t) * 0.92 + noiseAt(n) * 0.38 * exp(-t * 14.0)) * exp(-t * 7.2)
                }
                3 -> render(durationMs = 290) { t, n ->
                    val blast = noiseAt(n) * 0.75 * exp(-t * 9.0)
                    val punch = sin(2 * PI * (135.0 * exp(-t * 12.0) + 42.0) * t) * 0.85
                    (blast + punch) * exp(-t * 7.8)
                }
                4 -> render(durationMs = 330) { t, n ->
                    val shell = sin(2 * PI * (88.0 * exp(-t * 7.5) + 31.0) * t) * 0.9
                    val grit = noiseAt(n) * 0.45 * exp(-t * 16.0)
                    (shell + grit) * exp(-t * 6.8)
                }
                else -> render(durationMs = 300) { t, _ ->
                    val f = 155.0 * exp(-t * 8.5) + 46.0
                    val pulse = sin(2 * PI * f * t) * 0.85 + sin(2 * PI * f * 2.0 * t) * 0.3
                    pulse * exp(-t * 8.0)
                }
            }
            TowerClass.STELLAR_COSMIC -> when (slot) {
                2 -> render(durationMs = 205) { t, _ ->
                    val f = 1180.0 * exp(-t * 11.0) + 520.0
                    (sin(2 * PI * f * t) * 0.65 + sin(2 * PI * f * 1.25 * t) * 0.35) * exp(-t * 10.5)
                }
                3 -> render(durationMs = 225) { t, _ ->
                    val f = 740.0 + t * 1400.0
                    (sin(2 * PI * f * t) * 0.7 + sin(2 * PI * 1480.0 * t) * 0.25) * exp(-t * 9.5)
                }
                4 -> render(durationMs = 215) { t, n ->
                    val shimmer = sin(2 * PI * (987.77 + sin(2 * PI * 85.0 * t) * 140.0) * t) * 0.75
                    val spark = noiseAt(n) * 0.25 * exp(-t * 55.0)
                    (shimmer + spark) * exp(-t * 9.5)
                }
                else -> render(durationMs = 250) { t, _ ->
                    val f = 440.0 * exp(-t * 5.0) + 190.0
                    (sin(2 * PI * f * t) * 0.75 + sin(2 * PI * 1760.0 * t) * exp(-t * 16.0) * 0.4) * exp(-t * 7.5)
                }
            }
        }
    }

    fun synthesizeB61Upgrade(towerClass: TowerClass, slot: Int): ShortArray {
        if (slot <= 1) return ProceduralAudioSynthesizer.synthesizeTowerUpgrade(towerClass, 1)
        val root = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> 587.33
            TowerClass.ELEMENTAL_VOID -> 659.25
            TowerClass.HEAVY_SIEGE -> 392.00
            TowerClass.STELLAR_COSMIC -> 783.99
        }
        return when (slot) {
            2 -> render(durationMs = 460) { t, n ->
                val lock = (sin(2 * PI * 240.0 * t) * 0.65 + noiseAt(n) * 0.25) * exp(-t * 28.0)
                val triad = (sin(2 * PI * root * t) + sin(2 * PI * (root * 1.25) * (t - 0.08).coerceAtLeast(0.0))) *
                    (if (t > 0.08) 0.55 else 0.35) * exp(-t * 5.0)
                (lock + triad) * 0.85
            }
            3 -> render(durationMs = 480) { t, _ ->
                val step = ((t * 15.0).toInt()).coerceIn(0, 2)
                val mult = when (step) { 0 -> 1.0; 1 -> 1.5; else -> 2.0 }
                sin(2 * PI * (root * mult) * t) * exp(-t * 4.5) * 0.78
            }
            4 -> render(durationMs = 540) { t, _ ->
                val sub = sin(2 * PI * (root * 0.25) * t) * exp(-t * 4.0) * 0.65
                val bell = (sin(2 * PI * (root * 1.5) * t) * 0.5 + sin(2 * PI * (root * 2.0) * t) * 0.35) * exp(-t * 5.5)
                (sub + bell) * 0.85
            }
            else -> render(durationMs = 440) { t, _ ->
                val step = ((t * 16.0).toInt()).coerceIn(0, 3)
                val mult = when (step) { 0 -> 1.0; 1 -> 1.25; 2 -> 1.5; else -> 2.0 }
                val sq = if (sin(2 * PI * (root * mult) * t) >= 0) 0.65 else -0.65
                sq * exp(-t * 4.2)
            }
        }
    }

    fun synthesizeB61Music(assetId: String): ShortArray = when (assetId) {
        "b61_bgm_orbital_surge" -> render(durationMs = 45000, taperFade = true) { t, _ ->
            val bar = 1.6
            val cycle = (t / (bar * 2.0)).toInt() % 4
            val root = when (cycle) { 0 -> 82.41; 1 -> 65.41; 2 -> 73.42; else -> 61.74 }
            val stepPhase = (t % (bar / 4.0)) / (bar / 4.0)
            val bass = sin(2 * PI * root * t) * exp(-stepPhase * 4.5) * 0.45
            val fifth = sin(2 * PI * (root * 1.5) * t) * 0.28
            val lead = sin(2 * PI * (root * 2.0) * t) * exp(-stepPhase * 6.0) * 0.22
            (bass + fifth + lead) * 0.65
        }
        "b61_bgm_cryo_lattice" -> render(durationMs = 45000, taperFade = true) { t, _ ->
            val chordIdx = (t / 11.25).toInt() % 4
            val (r, f) = when (chordIdx) {
                0 -> 98.00 to 146.83
                1 -> 77.78 to 116.54
                2 -> 87.31 to 130.81
                else -> 65.41 to 98.00
            }
            val shimmer = sin(2 * PI * 0.2 * t) * 1.5
            val pad = sin(2 * PI * (r + shimmer) * t) * 0.35 + sin(2 * PI * f * t) * 0.26
            val chimeStep = (t % 0.75) / 0.75
            val bell = sin(2 * PI * (f * 4.0) * t) * exp(-chimeStep * 5.0) * 0.16
            (pad + bell) * 0.65
        }
        "b61_bgm_ion_horizon" -> render(durationMs = 45000, taperFade = true) { t, _ ->
            val chordIdx = (t / 9.0).toInt() % 4
            val root = when (chordIdx) { 0 -> 69.30; 1 -> 82.41; 2 -> 61.74; else -> 73.42 }
            val brass = (sin(2 * PI * root * t) * 0.4 + sin(2 * PI * (root * 2.0) * t) * 0.25 + sin(2 * PI * (root * 3.0) * t) * 0.15)
            val lfo = 0.7 + 0.3 * cos(2 * PI * 0.35 * t)
            brass * lfo * 0.62
        }
        else -> render(durationMs = 45000, taperFade = true) { t, _ ->
            val chordIdx = (t / 7.5).toInt() % 4
            val root = when (chordIdx) { 0 -> 87.31; 1 -> 103.83; 2 -> 77.78; else -> 65.41 }
            val beat = (t % 0.5) / 0.5
            val pulse = sin(2 * PI * (root * 0.5) * t) * exp(-beat * 3.2) * 0.42
            val pad = sin(2 * PI * root * t) * 0.30 + sin(2 * PI * (root * 1.5) * t) * 0.24
            (pulse + pad) * 0.65
        }
    }

    fun synthesizeB61SfxPackPreview(assetId: String): ShortArray = when (assetId) {
        "b61_sfx_pack_2" -> render(durationMs = 360) { t, n ->
            val thud = (sin(2 * PI * (120.0 * exp(-t * 14.0) + 45.0) * t) * 0.8 + noiseAt(n) * 0.2) * exp(-t * 11.0)
            val chime = sin(2 * PI * 659.25 * (t - 0.1).coerceAtLeast(0.0)) * (if (t > 0.1) 0.5 else 0.0) * exp(-t * 7.0)
            thud + chime
        }
        "b61_sfx_pack_3" -> render(durationMs = 380) { t, _ ->
            val f = if (t < 0.14) 1046.5 else 1567.98
            sin(2 * PI * f * t) * 0.72 * exp(-t * 6.5)
        }
        "b61_sfx_pack_4" -> render(durationMs = 350) { t, _ ->
            val f = 500.0 + t * 1800.0
            sin(2 * PI * f * t) * 0.72 * exp(-t * 7.0)
        }
        else -> render(durationMs = 340) { t, _ ->
            val step = ((t * 18.0).toInt() % 4)
            val f = when (step) { 0 -> 523.25; 1 -> 659.25; 2 -> 783.99; else -> 1046.5 }
            (if (sin(2 * PI * f * t) >= 0) 0.6 else -0.6) * exp(-t * 4.8)
        }
    }

    fun synthesizeLegacyFire(towerClass: TowerClass, slot: Int): ShortArray = when (towerClass) {
        TowerClass.KINETIC_BALLISTIC -> ProceduralAudioSynthesizer.synthesizeKineticVariant(slot)
        TowerClass.ELEMENTAL_VOID -> ProceduralAudioSynthesizer.synthesizeElementalVariant(slot)
        TowerClass.HEAVY_SIEGE -> ProceduralAudioSynthesizer.synthesizeSiegeVariant(slot)
        TowerClass.STELLAR_COSMIC -> ProceduralAudioSynthesizer.synthesizeStellarVariant(slot)
    }
}
