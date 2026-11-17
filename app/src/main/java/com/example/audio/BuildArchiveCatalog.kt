package com.example.audio

import com.example.model.ArchiveSection
import com.example.model.BgmTrackDef
import com.example.model.MapThemeDef
import com.example.model.SfxPackDef
import com.example.model.TowerClass
import com.example.model.TurretAudioDef
import com.example.model.TurretSfxSlot
import com.example.model.TurretSkinDef
import com.example.model.TurretSkinSlot

/**
 * Central build-versioned asset registry for Pre-Build #60, Build #60 (v0.0.1.039),
 * and Build #61 (v0.0.1.040). Guarantees immutable build-scoped IDs, Default (Slot 1)
 * preservation, and seamless legacy vote/slot migration.
 */
internal object BuildArchiveCatalog {

    val allClasses: List<TowerClass> = listOf(
        TowerClass.KINETIC_BALLISTIC,
        TowerClass.ELEMENTAL_VOID,
        TowerClass.HEAVY_SIEGE,
        TowerClass.STELLAR_COSMIC
    )

    // -------------------------------------------------------------------------
    // Background Music (Default + 4 new Build #61 + Build #60 Snapshot)
    // -------------------------------------------------------------------------
    val build61BgmTracks: List<BgmTrackDef> = listOf(
        BgmTrackDef(
            id = "default_bgm_rolling_playlist",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = "Default (Core)",
            slotIndex = 0,
            displayName = MusicTrack.ROLLING_PLAYLIST.label,
            description = "Cycles active ambient & combat tracks automatically",
            wavFileName = "bgm_rolling_playlist_v5_lowpass.wav",
            legacyTrackName = MusicTrack.ROLLING_PLAYLIST.name,
            isRollingPlaylist = true,
            isDefault = true
        ),
        BgmTrackDef(
            id = "default_bgm_cyber_ambient",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = "Default (Core)",
            slotIndex = 1,
            displayName = "Track 1: Sector Zero Synth (Default)",
            description = "Canonical brooding arpeggio & atmospheric sub-bass",
            wavFileName = "bgm_cyber_ambient_v5_lowpass.wav",
            legacyTrackName = MusicTrack.CYBER_AMBIENT.name,
            isDefault = true
        ),
        BgmTrackDef(
            id = "b61_bgm_orbital_surge",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 2,
            displayName = "Track 2: Orbital Surge",
            description = "Driving minor-key kinetic pulse built for multi-front waves",
            wavFileName = "b61_bgm_orbital_surge.wav"
        ),
        BgmTrackDef(
            id = "b61_bgm_cryo_lattice",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 3,
            displayName = "Track 3: Cryo Lattice",
            description = "Crystalline bell harmonics drifting over glacial sub-drones",
            wavFileName = "b61_bgm_cryo_lattice.wav"
        ),
        BgmTrackDef(
            id = "b61_bgm_ion_horizon",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 4,
            displayName = "Track 4: Ion Horizon",
            description = "Syncopated warm synth brass with deep cosmic undertones",
            wavFileName = "b61_bgm_ion_horizon.wav"
        ),
        BgmTrackDef(
            id = "b61_bgm_quantum_bastion",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 5,
            displayName = "Track 5: Quantum Bastion",
            description = "High-energy endgame fortress anthem with layered fifths",
            wavFileName = "b61_bgm_quantum_bastion.wav"
        )
    )

    val build60ArchivedBgmTracks: List<BgmTrackDef> = MusicTrack.entries
        .filter { it != MusicTrack.NONE }
        .mapIndexed { idx, track ->
            val labelSuffix = if (track == MusicTrack.CYBER_AMBIENT || track == MusicTrack.ROLLING_PLAYLIST) {
                " [B60 Snapshot]"
            } else {
                ""
            }
            BgmTrackDef(
                id = "b60_bgm_${track.name.lowercase()}",
                buildCode = B60_BUILD_CODE,
                buildLabel = B60_BUILD_LABEL,
                slotIndex = idx,
                displayName = "${track.label}$labelSuffix",
                description = "Archived Build #60 soundtrack synthesis",
                wavFileName = "bgm_${track.name.lowercase()}_v5_lowpass.wav",
                legacyTrackName = track.name,
                isRollingPlaylist = (track == MusicTrack.ROLLING_PLAYLIST),
                isArchived = true
            )
        }

    val allBgmTracks: List<BgmTrackDef> = build61BgmTracks + build60ArchivedBgmTracks
    val bgmById: Map<String, BgmTrackDef> = allBgmTracks.associateBy { it.id }

    // -------------------------------------------------------------------------
    // SFX Packs (Default + 4 new Build #61 + Build #60 Snapshot)
    // -------------------------------------------------------------------------
    val build61SfxPacks: List<SfxPackDef> = listOf(
        SfxPackDef(
            id = "default_sfx_pack_cyber_synth",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = "Default (Core)",
            slotIndex = 1,
            displayName = "Cyber Synth SFX Pack (Default)",
            description = "Canonical crisp tactical clicks, chimes, and placement thuds",
            legacyProfileName = SoundProfile.CYBER_SYNTH.name,
            previewWavFileName = "sfx_cyber_synth_upgrade_v4.wav",
            isDefault = true
        ),
        SfxPackDef(
            id = "b61_sfx_pack_2",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 2,
            displayName = "Titan Pulse SFX Pack",
            description = "Heavy sub-harmonic placement slams and warm brass confirm chimes",
            legacyProfileName = SoundProfile.DEEP_ACOUSTIC.name,
            previewWavFileName = "b61_sfx_pack_2_preview.wav"
        ),
        SfxPackDef(
            id = "b61_sfx_pack_3",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 3,
            displayName = "Cryo Chime SFX Pack",
            description = "Glacial harmonic chimes and crystalline UI feedback",
            legacyProfileName = SoundProfile.CYBER_SYNTH.name,
            previewWavFileName = "b61_sfx_pack_3_preview.wav"
        ),
        SfxPackDef(
            id = "b61_sfx_pack_4",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 4,
            displayName = "Plasma Coil SFX Pack",
            description = "High-voltage sci-fi sweeps and ionized UI snaps",
            legacyProfileName = SoundProfile.SCI_FI_LASER.name,
            previewWavFileName = "b61_sfx_pack_4_preview.wav"
        ),
        SfxPackDef(
            id = "b61_sfx_pack_5",
            buildCode = CURRENT_BUILD_CODE,
            buildLabel = CURRENT_BUILD_LABEL,
            slotIndex = 5,
            displayName = "Hyper Chip SFX Pack",
            description = "Bright 8-bit chiptune arpeggio confirm and crisp square clicks",
            legacyProfileName = SoundProfile.RETRO_8BIT.name,
            previewWavFileName = "b61_sfx_pack_5_preview.wav"
        )
    )

    val build60ArchivedSfxPacks: List<SfxPackDef> = SoundProfile.entries.mapIndexed { idx, profile ->
        SfxPackDef(
            id = "b60_sfx_${profile.name.lowercase()}",
            buildCode = B60_BUILD_CODE,
            buildLabel = B60_BUILD_LABEL,
            slotIndex = idx + 1,
            displayName = "${profile.label} [B60]",
            description = "Build #60 procedural UI & gameplay SFX profile",
            legacyProfileName = profile.name,
            previewWavFileName = "sfx_${profile.name.lowercase()}_upgrade_v4.wav",
            isArchived = true
        )
    }

    val allSfxPacks: List<SfxPackDef> = build61SfxPacks + build60ArchivedSfxPacks
    val sfxPacksById: Map<String, SfxPackDef> = allSfxPacks.associateBy { it.id }

    // -------------------------------------------------------------------------
    // Turret Fire & Upgrade Catalogs (Build #61 Active + Build #60 Snapshot)
    // -------------------------------------------------------------------------
    private fun b61FireVariantLabel(towerClass: TowerClass, slot: Int): String = when (towerClass) {
        TowerClass.KINETIC_BALLISTIC -> when (slot) {
            1 -> TurretSfxSlot.SLOT_1_DEFAULT.kineticName
            2 -> "Vulcan Shredder"
            3 -> "Hyper Flechette"
            4 -> "Magneto Slug"
            else -> "Tachyon Burst"
        }
        TowerClass.ELEMENTAL_VOID -> when (slot) {
            1 -> TurretSfxSlot.SLOT_1_DEFAULT.elementalName
            2 -> "Cryo Lance"
            3 -> "Aether Surge"
            4 -> "Quantum Fracture"
            else -> "Prismatic Arc"
        }
        TowerClass.HEAVY_SIEGE -> when (slot) {
            1 -> TurretSfxSlot.SLOT_1_DEFAULT.siegeName
            2 -> "Seismic Thumper"
            3 -> "Thermobaric Shell"
            4 -> "Bunker Breaker"
            else -> "Gravimetric Cannon"
        }
        TowerClass.STELLAR_COSMIC -> when (slot) {
            1 -> TurretSfxSlot.SLOT_1_DEFAULT.stellarName
            2 -> "Quasar Dart"
            3 -> "Corona Beam"
            4 -> "Nova Ripple"
            else -> "Singularity Pulse"
        }
    }

    fun getBuild61FireShortLabel(towerClass: TowerClass, slot: Int): String =
        b61FireVariantLabel(towerClass, slot).substringBefore(" ")

    val build61TurretFireDefs: List<TurretAudioDef> = buildList {
        for (cls in allClasses) {
            val tag = towerClassShortTag(cls)
            for (slot in 1..5) {
                val isDef = (slot == 1)
                val id = if (isDef) "default_fire_$tag" else "b61_fire_${tag}_$slot"
                val ver = if (cls == TowerClass.STELLAR_COSMIC) "v1" else "v2"
                val wav = if (isDef) "sfx_gunfire_${tag}_slot1_${ver}.wav" else "b61_sfx_gunfire_${tag}_slot${slot}.wav"
                add(
                    TurretAudioDef(
                        id = id,
                        section = ArchiveSection.TURRET_FIRE,
                        towerClass = cls,
                        buildCode = CURRENT_BUILD_CODE,
                        buildLabel = if (isDef) "Default (Core)" else CURRENT_BUILD_LABEL,
                        slotIndex = slot,
                        displayName = "${towerClassLabel(cls)} Fire #$slot: ${b61FireVariantLabel(cls, slot)}",
                        description = if (isDef) "Canonical Slot 1 firing waveform" else "Build #61 firing synthesis variant",
                        wavFileName = wav,
                        isDefault = isDef
                    )
                )
            }
        }
    }

    val build60ArchivedFireDefs: List<TurretAudioDef> = buildList {
        for (cls in allClasses) {
            val tag = towerClassShortTag(cls)
            for (slot in 1..5) {
                val ver = if (cls == TowerClass.STELLAR_COSMIC) "v1" else "v2"
                val slotLabel = gunfireVariantLabelB60(cls, slot)
                val suffix = if (slot == 1) " [B60 Snapshot]" else ""
                add(
                    TurretAudioDef(
                        id = "b60_fire_${tag}_$slot",
                        section = ArchiveSection.TURRET_FIRE,
                        towerClass = cls,
                        buildCode = B60_BUILD_CODE,
                        buildLabel = B60_BUILD_LABEL,
                        slotIndex = slot,
                        displayName = "${towerClassLabel(cls)} Fire #$slot: $slotLabel$suffix",
                        description = "Build #60 archived gunfire waveform",
                        wavFileName = "sfx_gunfire_${tag}_slot${slot}_${ver}.wav",
                        isArchived = true
                    )
                )
            }
        }
    }

    val build61TurretUpgradeDefs: List<TurretAudioDef> = buildList {
        val skinNames = BuildArchiveVisualCatalog.build61TurretSkins.associateBy { it.slotIndex }
        for (cls in allClasses) {
            for (slot in 1..5) {
                val isDef = (slot == 1)
                val id = if (isDef) "default_upgrade_${cls.name.lowercase()}" else "b61_upgrade_${cls.name.lowercase()}_$slot"
                val wav = if (isDef) "sfx_upgrade_${cls.name.lowercase()}_slot1_v2.wav" else "b61_sfx_upgrade_${cls.name.lowercase()}_slot${slot}.wav"
                val skinName = skinNames[slot]?.displayName ?: "Variant $slot"
                add(
                    TurretAudioDef(
                        id = id,
                        section = ArchiveSection.TURRET_UPGRADE,
                        towerClass = cls,
                        buildCode = CURRENT_BUILD_CODE,
                        buildLabel = if (isDef) "Default (Core)" else CURRENT_BUILD_LABEL,
                        slotIndex = slot,
                        displayName = "${towerClassLabel(cls)} Upgrade #$slot ($skinName)",
                        description = if (isDef) "Canonical Slot 1 upgrade chime" else "Build #61 upgrade synthesis chime",
                        wavFileName = wav,
                        isDefault = isDef
                    )
                )
            }
        }
    }

    val build60ArchivedUpgradeDefs: List<TurretAudioDef> = buildList {
        for (cls in allClasses) {
            for (slot in 1..5) {
                val skinName = TurretSkinSlot.fromSlot(slot).displayName
                val suffix = if (slot == 1) " [B60 Snapshot]" else ""
                add(
                    TurretAudioDef(
                        id = "b60_upgrade_${cls.name.lowercase()}_$slot",
                        section = ArchiveSection.TURRET_UPGRADE,
                        towerClass = cls,
                        buildCode = B60_BUILD_CODE,
                        buildLabel = B60_BUILD_LABEL,
                        slotIndex = slot,
                        displayName = "${towerClassLabel(cls)} Upgrade #$slot ($skinName)$suffix",
                        description = "Build #60 archived upgrade chime",
                        wavFileName = "sfx_upgrade_${cls.name.lowercase()}_slot${slot}_v2.wav",
                        isArchived = true
                    )
                )
            }
        }
    }

    val allTurretAudioDefs: List<TurretAudioDef> =
        build61TurretFireDefs + build60ArchivedFireDefs + build61TurretUpgradeDefs + build60ArchivedUpgradeDefs
    val turretAudioById: Map<String, TurretAudioDef> = allTurretAudioDefs.associateBy { it.id }

    // -------------------------------------------------------------------------
    // Resolvers & Legacy ID Migration
    // -------------------------------------------------------------------------
    fun resolveTurretSkin(skinId: String?, legacySlot: Int = 1): TurretSkinDef {
        val canonical = skinId?.let { migrateLegacyAssetId(it) }
        if (canonical != null) {
            BuildArchiveVisualCatalog.skinsById[canonical]?.let { return it }
        }
        return BuildArchiveVisualCatalog.build61TurretSkins.firstOrNull { it.slotIndex == legacySlot }
            ?: BuildArchiveVisualCatalog.defaultTurretSkin
    }

    fun resolveMapTheme(themeId: String?, legacySlot: Int = 1): MapThemeDef {
        val canonical = themeId?.let { migrateLegacyAssetId(it) }
        if (canonical != null) {
            BuildArchiveVisualCatalog.themesById[canonical]?.let { return it }
        }
        return BuildArchiveVisualCatalog.build61MapThemes.firstOrNull { it.slotIndex == legacySlot }
            ?: BuildArchiveVisualCatalog.defaultMapTheme
    }

    fun resolveBgmTrack(bgmId: String?): BgmTrackDef {
        val canonical = bgmId?.let { migrateLegacyAssetId(it) }
        if (canonical != null) {
            bgmById[canonical]?.let { return it }
        }
        return build61BgmTracks.first()
    }

    fun resolveSfxPack(packId: String?): SfxPackDef {
        val canonical = packId?.let { migrateLegacyAssetId(it) }
        if (canonical != null) {
            sfxPacksById[canonical]?.let { return it }
        }
        return build61SfxPacks.first()
    }

    fun buildActiveFireId(towerClass: TowerClass, slot: Int): String {
        val s = slot.coerceIn(1, 5)
        val tag = towerClassShortTag(towerClass)
        return if (s == 1) "default_fire_$tag" else "b61_fire_${tag}_$s"
    }

    fun buildActiveUpgradeId(towerClass: TowerClass, slot: Int): String {
        val s = slot.coerceIn(1, 5)
        return if (s == 1) "default_upgrade_${towerClass.name.lowercase()}" else "b61_upgrade_${towerClass.name.lowercase()}_$s"
    }

    fun buildActiveSkinId(slot: Int): String {
        val s = slot.coerceIn(1, 5)
        return if (s == 1) "default_skin_1" else "b61_skin_$s"
    }

    fun buildActiveThemeId(slot: Int): String {
        val s = slot.coerceIn(1, 5)
        return if (s == 1) "default_theme_1" else "b61_theme_$s"
    }

    /**
     * Migrates pre-Build #61 (v0.0.1.039) local/vote IDs into immutable build-scoped IDs
     * so existing user ratings carry cleanly into Build #60 Archives & Default Core without collisions.
     */
    fun migrateLegacyAssetId(rawId: String): String {
        if (rawId.isBlank()) return rawId
        if (rawId.startsWith("b60_") || rawId.startsWith("b61_") || rawId.startsWith("pre60_") || rawId.startsWith("default_")) {
            return rawId
        }
        if (rawId == "skin_slot_1") return "default_skin_1"
        if (rawId.startsWith("skin_slot_")) {
            val slot = rawId.removePrefix("skin_slot_").toIntOrNull()
            if (slot != null && slot in 2..5) return "b60_skin_$slot"
        }
        if (rawId == "theme_slot_1") return "default_theme_1"
        if (rawId.startsWith("theme_slot_")) {
            val slot = rawId.removePrefix("theme_slot_").toIntOrNull()
            if (slot != null && slot in 2..5) return "b60_theme_$slot"
        }
        if (rawId == "bgm_rolling_playlist") return "default_bgm_rolling_playlist"
        if (rawId == "bgm_cyber_ambient") return "default_bgm_cyber_ambient"
        if (rawId.startsWith("bgm_")) {
            val suffix = rawId.removePrefix("bgm_")
            val candidate = "b60_bgm_$suffix"
            if (bgmById.containsKey(candidate)) return candidate
        }
        if (rawId.startsWith("fire_")) {
            val parts = rawId.split("_slot_")
            if (parts.size == 2) {
                val tag = parts[0].removePrefix("fire_")
                val slot = parts[1].toIntOrNull()
                if (slot == 1) return "default_fire_$tag"
                if (slot != null && slot in 2..5) return "b60_fire_${tag}_$slot"
            }
        }
        if (rawId.startsWith("upgrade_")) {
            val parts = rawId.split("_slot_")
            if (parts.size == 2) {
                val clsName = parts[0].removePrefix("upgrade_")
                val slot = parts[1].toIntOrNull()
                if (slot == 1) return "default_upgrade_$clsName"
                if (slot != null && slot in 2..5) return "b60_upgrade_${clsName}_$slot"
            }
        }
        if (rawId.startsWith("sfx_")) {
            val fxSlug = rawId.removePrefix("sfx_")
            return "default_sfx_$fxSlug"
        }
        if (rawId.startsWith("archive_")) {
            return "pre60_wav_${rawId.removePrefix("archive_")}"
        }
        return rawId
    }

    fun inferArchiveSectionFromFilename(filename: String): ArchiveSection {
        val lower = filename.lowercase()
        return when {
            lower.contains("gunfire") || lower.contains("fire") -> ArchiveSection.TURRET_FIRE
            lower.contains("upgrade") -> ArchiveSection.TURRET_UPGRADE
            else -> ArchiveSection.BGM_SFX
        }
    }

    fun towerClassShortTag(towerClass: TowerClass): String = when (towerClass) {
        TowerClass.KINETIC_BALLISTIC -> "kinetic"
        TowerClass.ELEMENTAL_VOID -> "elemental"
        TowerClass.HEAVY_SIEGE -> "siege"
        TowerClass.STELLAR_COSMIC -> "stellar"
    }

    fun towerClassLabel(towerClass: TowerClass): String = when (towerClass) {
        TowerClass.KINETIC_BALLISTIC -> "Kinetic"
        TowerClass.ELEMENTAL_VOID -> "Elemental"
        TowerClass.HEAVY_SIEGE -> "Siege"
        TowerClass.STELLAR_COSMIC -> "Stellar"
    }

    private fun gunfireVariantLabelB60(towerClass: TowerClass, slot: Int): String {
        val s = TurretSfxSlot.fromSlot(slot)
        return when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> s.kineticName
            TowerClass.ELEMENTAL_VOID -> s.elementalName
            TowerClass.HEAVY_SIEGE -> s.siegeName
            TowerClass.STELLAR_COSMIC -> s.stellarName
        }
    }
}
