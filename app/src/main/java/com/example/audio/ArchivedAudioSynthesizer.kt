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
 * Enables lazy on-demand generation on Dispatchers.IO for Build #60, Build #61,
 * and every subsequent buildCode (>= 62) without eager startup synthesis.
 */
internal object ArchivedAudioSynthesizer {

    fun synthesizeByAssetId(rawAssetId: String): ShortArray? {
        val assetId = BuildArchiveCatalog.migrateLegacyAssetId(rawAssetId)

        val matchedBgm = BuildArchiveCatalog.bgmById[assetId]
            ?: if (assetId.contains("_bgm_slot_")) BuildArchiveCatalog.resolveBgmTrack(assetId) else null
        if (matchedBgm != null && matchedBgm.id == assetId) {
            val legacy = matchedBgm.legacyTrackName?.let { name ->
                runCatching { MusicTrack.valueOf(name) }.getOrNull()
            }
            if (legacy != null) {
                val target = if (legacy == MusicTrack.ROLLING_PLAYLIST) MusicTrack.CYBER_AMBIENT else legacy
                return AudioSynthesizer.synthesizeMusic(target)
            }
            return synthesizeMusicForBuild(matchedBgm.buildCode, matchedBgm.id, matchedBgm.slotIndex)
        }

        val matchedPack = BuildArchiveCatalog.sfxPacksById[assetId]
            ?: if (assetId.contains("_sfx_pack_")) BuildArchiveCatalog.resolveSfxPack(assetId) else null
        if (matchedPack != null && matchedPack.id == assetId) {
            if (matchedPack.buildCode == B60_BUILD_CODE || matchedPack.isDefault) {
                val prof = runCatching { SoundProfile.valueOf(matchedPack.legacyProfileName) }.getOrDefault(SoundProfile.CYBER_SYNTH)
                return ProceduralAudioSynthesizer.synthesizeGeneric(SoundFx.UPGRADE, prof)
            }
            return synthesizeSfxPackPreviewForBuild(matchedPack.buildCode, matchedPack.id, matchedPack.slotIndex)
        }

        if (assetId.startsWith("default_sfx_")) {
            val fxName = assetId.removePrefix("default_sfx_").uppercase()
            val fx = SoundFx.entries.firstOrNull { it.name == fxName } ?: return null
            return ProceduralAudioSynthesizer.synthesizeGeneric(fx, SoundProfile.CYBER_SYNTH)
        }

        val audioDef = BuildArchiveCatalog.resolveTurretAudio(assetId) ?: return null
        return when (audioDef.section) {
            ArchiveSection.TURRET_FIRE -> synthesizeFireForBuild(audioDef.buildCode, audioDef.towerClass, audioDef.slotIndex)
            ArchiveSection.TURRET_UPGRADE -> synthesizeUpgradeForBuild(audioDef.buildCode, audioDef.towerClass, audioDef.slotIndex)
            else -> null
        }
    }

    fun synthesizeFireForBuild(buildCode: Int, towerClass: TowerClass, slot: Int): ShortArray {
        if (slot <= 1 || buildCode <= B60_BUILD_CODE) {
            return synthesizeLegacyFire(towerClass, slot)
        }
        if (buildCode == B61_BUILD_CODE) {
            return synthesizeB61Fire(towerClass, slot)
        }
        val bDelta = (buildCode - B61_BUILD_CODE).coerceAtLeast(1)
        val slotShift = (slot - 2).coerceIn(0, 3)
        val pitchFactor = 0.84 + ((bDelta * 7 + slotShift * 5) % 11) * 0.036
        val fmRatio = 1.15 + ((bDelta * 3 + slotShift * 2) % 7) * 0.14
        val modDepth = 1.5 + ((bDelta + slotShift) % 4) * 1.2

        return when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> {
                val dur = 68 + ((bDelta + slotShift * 3) % 4) * 12
                val startFreq = (1450.0 + slotShift * 380.0) * pitchFactor
                val endFreq = (240.0 + slotShift * 75.0) * pitchFactor
                val decay = 24.0 + slotShift * 4.5
                render(durationMs = dur) { t, n ->
                    val f = startFreq * exp(-t * (decay + 8.0)) + endFreq
                    val core = sin(2 * PI * f * t) * 0.70 + sin(2 * PI * f * fmRatio * t) * 0.25
                    val grit = noiseAt(n + bDelta * 131) * (0.28 + 0.04 * slotShift) * exp(-t * 58.0)
                    (core + grit) * exp(-t * decay)
                }
            }
            TowerClass.ELEMENTAL_VOID -> {
                val dur = 165 + ((bDelta + slotShift * 2) % 4) * 14
                val baseFreq = (620.0 + slotShift * 195.0) * pitchFactor
                val sweep = (if ((bDelta + slotShift) % 2 == 0) 1.0 else -0.65) * 1450.0
                val lfoFreq = 45.0 + ((bDelta * 11 + slotShift * 17) % 60)
                render(durationMs = dur) { t, _ ->
                    val f = (baseFreq + sweep * t).coerceAtLeast(220.0)
                    val mod = sin(2 * PI * lfoFreq * t) * modDepth
                    val sig = sin(2 * PI * f * t + mod) * 0.65 + cos(2 * PI * f * fmRatio * t) * 0.28
                    sig * exp(-t * 11.5)
                }
            }
            TowerClass.HEAVY_SIEGE -> {
                val dur = 285 + ((bDelta + slotShift) % 4) * 20
                val subTop = (74.0 + slotShift * 22.0) * (0.9 + (bDelta % 4) * 0.08)
                val subFloor = (32.0 + slotShift * 4.5)
                render(durationMs = dur) { t, n ->
                    val f = subTop * exp(-t * (6.2 + slotShift * 1.1)) + subFloor
                    val boom = sin(2 * PI * f * t) * 0.88 + sin(2 * PI * f * 2.0 * t) * 0.22
                    val blast = noiseAt(n + bDelta * 257) * 0.42 * exp(-t * (10.0 + slotShift * 2.5))
                    (boom + blast) * exp(-t * 7.2)
                }
            }
            TowerClass.STELLAR_COSMIC -> {
                val dur = 195 + ((bDelta * 2 + slotShift) % 4) * 15
                val carrier = (520.0 + slotShift * 165.0) * pitchFactor
                val shimmerFreq = 1320.0 * pitchFactor
                render(durationMs = dur) { t, n ->
                    val f = carrier * exp(-t * 6.5) + carrier * 0.48
                    val beam = sin(2 * PI * f * t) * 0.68 + sin(2 * PI * shimmerFreq * t) * 0.26 * exp(-t * 14.0)
                    val spark = noiseAt(n + bDelta * 313) * 0.18 * exp(-t * 48.0)
                    (beam + spark) * exp(-t * 9.0)
                }
            }
        }
    }

    fun synthesizeUpgradeForBuild(buildCode: Int, towerClass: TowerClass, slot: Int): ShortArray {
        if (slot <= 1 || buildCode <= B60_BUILD_CODE) {
            return ProceduralAudioSynthesizer.synthesizeTowerUpgrade(towerClass, slot)
        }
        if (buildCode == B61_BUILD_CODE) {
            return synthesizeB61Upgrade(towerClass, slot)
        }
        val bDelta = (buildCode - B61_BUILD_CODE).coerceAtLeast(1)
        val slotShift = (slot - 2).coerceIn(0, 3)
        val baseRoot = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> 554.37
            TowerClass.ELEMENTAL_VOID -> 622.25
            TowerClass.HEAVY_SIEGE -> 369.99
            TowerClass.STELLAR_COSMIC -> 739.99
        }
        val semitoneShift = ((bDelta * 3 + slotShift * 2) % 7) - 2
        val root = baseRoot * Math.pow(2.0, semitoneShift / 12.0)
        val stepRate = 13.0 + ((bDelta + slotShift) % 4) * 2.0
        val useFifths = ((bDelta + slotShift) % 2 == 0)

        return render(durationMs = 470) { t, n ->
            val step = ((t * stepRate).toInt()).coerceIn(0, 3)
            val mult = when (step) {
                0 -> 1.0
                1 -> if (useFifths) 1.5 else 1.25
                2 -> if (useFifths) 2.0 else 1.5
                else -> if (useFifths) 2.5 else 2.0
            }
            val lock = (sin(2 * PI * (220.0 + bDelta * 12.0) * t) * 0.55 + noiseAt(n + bDelta * 41) * 0.2) * exp(-t * 28.0)
            val chime = (sin(2 * PI * (root * mult) * t) * 0.65 + sin(2 * PI * (root * mult * 1.5) * t) * 0.25) * exp(-t * 4.8)
            (lock + chime) * 0.84
        }
    }

    fun synthesizeMusicForBuild(buildCode: Int, assetId: String, slot: Int): ShortArray {
        if (buildCode == B61_BUILD_CODE) {
            return synthesizeB61Music(assetId)
        }
        val bDelta = (buildCode - B61_BUILD_CODE).coerceAtLeast(1)
        val slotShift = (slot - 2).coerceIn(0, 3)
        val progressionSeeds = arrayOf(
            doubleArrayOf(73.42, 87.31, 65.41, 82.41),
            doubleArrayOf(61.74, 73.42, 82.41, 55.00),
            doubleArrayOf(69.30, 82.41, 92.50, 61.74),
            doubleArrayOf(82.41, 98.00, 73.42, 65.41)
        )
        val prog = progressionSeeds[(bDelta + slotShift) % progressionSeeds.size]
        val transpose = Math.pow(2.0, (((bDelta * 2 + slotShift) % 5) - 2) / 12.0)
        val tempoDiv = 0.45 + ((bDelta + slotShift) % 4) * 0.12
        val arpMult = when (slotShift) {
            0 -> 2.0
            1 -> 2.5
            2 -> 3.0
            else -> 4.0
        }
        return render(durationMs = 45000, taperFade = true) { t, _ ->
            val barIdx = (t / 7.5).toInt() % 4
            val root = prog[barIdx] * transpose
            val beatPhase = (t % tempoDiv) / tempoDiv
            val bass = sin(2 * PI * root * t) * exp(-beatPhase * 3.8) * 0.44
            val fifth = sin(2 * PI * (root * 1.5) * t) * 0.26
            val arp = sin(2 * PI * (root * arpMult) * t) * exp(-beatPhase * 6.5) * 0.18
            val lfo = 0.78 + 0.22 * cos(2 * PI * (0.25 + slotShift * 0.08) * t)
            (bass + fifth + arp) * lfo * 0.64
        }
    }

    fun synthesizeSfxPackPreviewForBuild(buildCode: Int, assetId: String, slot: Int): ShortArray {
        if (buildCode == B61_BUILD_CODE) {
            return synthesizeB61SfxPackPreview(assetId)
        }
        val bDelta = (buildCode - B61_BUILD_CODE).coerceAtLeast(1)
        val slotShift = (slot - 2).coerceIn(0, 3)
        val f1 = 440.0 + ((bDelta * 55 + slotShift * 120) % 420)
        val f2 = f1 * (1.25 + 0.25 * (slotShift % 3))
        return render(durationMs = 360) { t, n ->
            val thud = (sin(2 * PI * (110.0 * exp(-t * 14.0) + 45.0) * t) * 0.72 + noiseAt(n + bDelta * 19) * 0.18) * exp(-t * 12.0)
            val f = if (t < 0.12) f1 else f2
            val tone = sin(2 * PI * f * t) * 0.56 * exp(-t * 6.2)
            thud + tone
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
