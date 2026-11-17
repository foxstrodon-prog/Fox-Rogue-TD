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
import kotlin.math.abs

/**
 * Central build-versioned asset registry for Pre-Build #60, Build #60 (v0.0.1.039),
 * Build #61 (v0.0.1.040), and every subsequent `versionCode` increment.
 * Guarantees immutable build-scoped IDs, Default (Slot 1) preservation, automatic
 * 4-variant generation per build, multi-build archiving (`60 until CURRENT_BUILD_CODE`),
 * and seamless legacy vote/slot migration.
 */
internal object BuildArchiveCatalog {

    val allClasses: List<TowerClass> = listOf(
        TowerClass.KINETIC_BALLISTIC,
        TowerClass.ELEMENTAL_VOID,
        TowerClass.HEAVY_SIEGE,
        TowerClass.STELLAR_COSMIC
    )

    private val buildScopedIdRegex = Regex("^b(\\d+)_.*")
    private val dynamicBgmRegex = Regex("^b(\\d+)_bgm_slot_(\\d+)$")
    private val dynamicSfxRegex = Regex("^b(\\d+)_sfx_pack_(\\d+)$")
    private val dynamicFireRegex = Regex("^b(\\d+)_fire_([a-z]+)_(\\d+)$")
    private val dynamicUpgradeRegex = Regex("^b(\\d+)_upgrade_([a-z_]+)_(\\d+)$")

    private val bgmPrefixes = listOf(
        "Orbital", "Cryo", "Ion", "Quantum", "Aether", "Solaris", "Vortex", "Pulse",
        "Chrono", "Astral", "Hyperion", "Titan", "Nebula", "Obsidian", "Horizon", "Zenith",
        "Cobalt", "Helios", "Prism", "Kinetic", "Vector", "Apex", "Rift"
    )
    private val bgmSuffixes = listOf(
        "Surge", "Lattice", "Horizon", "Bastion", "Drift", "Conduit", "Protocol", "Echo",
        "Cascade", "Vector", "Resonance", "Citadel", "Matrix", "Overdrive", "Signal", "Pulse",
        "Orbit", "Sector", "Flux"
    )
    private val sfxPackNames = listOf(
        "Titan Pulse", "Cryo Chime", "Plasma Coil", "Hyper Chip",
        "Vortex Snap", "Cobalt Latch", "Aether Bell", "Arc Capacitor",
        "Obsidian Thud", "Ion Relay", "Prism Click", "Vector Chime",
        "Nova Lock", "Quantum Ping", "Apex Harmonic", "Chrono Switch"
    )
    private val kineticFirePrefixes = listOf(
        "Vulcan", "Hyper", "Magneto", "Tachyon", "Rotary", "Helix", "Razor", "Vector",
        "Cobalt", "Apex", "Sabot", "Kinetic", "Tungsten", "Vortex", "Turbo", "Pulse", "Rail"
    )
    private val kineticFireSuffixes = listOf("Shredder", "Flechette", "Slug", "Burst", "Autocannon", "Repeater", "Lance", "Stream", "Driver")
    private val elementalFirePrefixes = listOf(
        "Cryo", "Aether", "Quantum", "Prismatic", "Void", "Plasma", "Ion", "Arc",
        "Flux", "Chrono", "Astral", "Solaris", "Rift", "Spectral", "Nebula", "Polar", "Echo"
    )
    private val elementalFireSuffixes = listOf("Lance", "Surge", "Fracture", "Arc", "Beam", "Conduit", "Torrent", "Wave", "Disruptor")
    private val siegeFirePrefixes = listOf(
        "Seismic", "Thermobaric", "Bunker", "Gravimetric", "Obsidian", "Titan", "Magma", "Impact",
        "Orbital", "Tectonic", "Hammer", "Artillery", "Bastion", "Meteor", "Crater", "Shock", "Iron"
    )
    private val siegeFireSuffixes = listOf("Thumper", "Shell", "Breaker", "Cannon", "Howitzer", "Mortar", "Payload", "Salvo", "Siege")
    private val stellarFirePrefixes = listOf(
        "Quasar", "Corona", "Nova", "Singularity", "Celestial", "Zenith", "Photon", "Pulsar",
        "Eclipse", "Starfire", "Comet", "Supernova", "Astral", "Radiant", "Horizon", "Voidstar", "Solar"
    )
    private val stellarFireSuffixes = listOf("Dart", "Beam", "Ripple", "Pulse", "Flare", "Core", "Ray", "Cascade", "Burst")

    fun extractBuildCodeFromId(id: String): Int? =
        buildScopedIdRegex.matchEntire(id)?.groupValues?.getOrNull(1)?.toIntOrNull()

    fun isArchivedAssetId(id: String, currentBuildCode: Int = CURRENT_BUILD_CODE): Boolean {
        if (id.isBlank() || id.startsWith("default_")) return false
        if (id.startsWith("pre60_")) return true
        val b = extractBuildCodeFromId(id) ?: return false
        return b < currentBuildCode
    }

    // -------------------------------------------------------------------------
    // Background Music (Default Core + Active Build + Multi-Build Archives)
    // -------------------------------------------------------------------------
    private fun coreBgmTracks(buildCode: Int): List<BgmTrackDef> = listOf(
        BgmTrackDef(
            id = "default_bgm_rolling_playlist",
            buildCode = buildCode,
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
            buildCode = buildCode,
            buildLabel = "Default (Core)",
            slotIndex = 1,
            displayName = "Track 1: Sector Zero Synth (Default)",
            description = "Canonical brooding arpeggio & atmospheric sub-bass",
            wavFileName = "bgm_cyber_ambient_v5_lowpass.wav",
            legacyTrackName = MusicTrack.CYBER_AMBIENT.name,
            isDefault = true
        )
    )

    private val build61CuratedBgmVariants: List<BgmTrackDef> = listOf(
        BgmTrackDef(
            id = "b61_bgm_orbital_surge",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
            slotIndex = 2,
            displayName = "Track 2: Orbital Surge",
            description = "Driving minor-key kinetic pulse built for multi-front waves",
            wavFileName = "b61_bgm_orbital_surge.wav"
        ),
        BgmTrackDef(
            id = "b61_bgm_cryo_lattice",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
            slotIndex = 3,
            displayName = "Track 3: Cryo Lattice",
            description = "Crystalline bell harmonics drifting over glacial sub-drones",
            wavFileName = "b61_bgm_cryo_lattice.wav"
        ),
        BgmTrackDef(
            id = "b61_bgm_ion_horizon",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
            slotIndex = 4,
            displayName = "Track 4: Ion Horizon",
            description = "Syncopated warm synth brass with deep cosmic undertones",
            wavFileName = "b61_bgm_ion_horizon.wav"
        ),
        BgmTrackDef(
            id = "b61_bgm_quantum_bastion",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
            slotIndex = 5,
            displayName = "Track 5: Quantum Bastion",
            description = "High-energy endgame fortress anthem with layered fifths",
            wavFileName = "b61_bgm_quantum_bastion.wav"
        )
    )

    fun bgmVariantsForBuild(buildCode: Int, isArchived: Boolean): List<BgmTrackDef> {
        if (buildCode <= B60_BUILD_CODE) return emptyList()
        if (buildCode == B61_BUILD_CODE) {
            return build61CuratedBgmVariants.map { it.copy(isArchived = isArchived) }
        }
        val label = buildLabelFor(buildCode)
        return (2..5).map { slot ->
            val pIdx = abs((buildCode * 5 + (slot - 2) * 6 + 2) % bgmPrefixes.size)
            val sIdx = abs((buildCode * 3 + (slot - 2) * 5 + 7) % bgmSuffixes.size)
            val title = "${bgmPrefixes[pIdx]} ${bgmSuffixes[sIdx]}"
            BgmTrackDef(
                id = "b${buildCode}_bgm_slot_$slot",
                buildCode = buildCode,
                buildLabel = label,
                slotIndex = slot,
                displayName = "Track $slot: $title",
                description = "Build #$buildCode procedural tactical synthwave progression",
                wavFileName = "b${buildCode}_bgm_slot_${slot}.wav",
                isArchived = isArchived
            )
        }
    }

    fun activeBgmTracksFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<BgmTrackDef> =
        coreBgmTracks(targetBuildCode) + bgmVariantsForBuild(targetBuildCode, isArchived = false)

    fun archivedBgmTracksFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<BgmTrackDef> =
        buildList {
            if (targetBuildCode > B60_BUILD_CODE) {
                addAll(build60ArchivedBgmTracks)
            }
            for (b in B61_BUILD_CODE until targetBuildCode) {
                addAll(bgmVariantsForBuild(b, isArchived = true))
            }
        }

    val activeBgmTracks: List<BgmTrackDef>
        get() = activeBgmTracksFor(CURRENT_BUILD_CODE)

    val archivedBgmTracks: List<BgmTrackDef>
        get() = archivedBgmTracksFor(CURRENT_BUILD_CODE)

    @Deprecated("Use activeBgmTracks", ReplaceWith("activeBgmTracks"))
    val build61BgmTracks: List<BgmTrackDef>
        get() = activeBgmTracks

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

    val allBgmTracks: List<BgmTrackDef>
        get() = activeBgmTracks + archivedBgmTracks

    val bgmById: Map<String, BgmTrackDef>
        get() = allBgmTracks.associateBy { it.id }

    // -------------------------------------------------------------------------
    // SFX Packs (Default Core + Active Build + Multi-Build Archives)
    // -------------------------------------------------------------------------
    private fun defaultSfxPack(buildCode: Int) = SfxPackDef(
        id = "default_sfx_pack_cyber_synth",
        buildCode = buildCode,
        buildLabel = "Default (Core)",
        slotIndex = 1,
        displayName = "Cyber Synth SFX Pack (Default)",
        description = "Canonical crisp tactical clicks, chimes, and placement thuds",
        legacyProfileName = SoundProfile.CYBER_SYNTH.name,
        previewWavFileName = "sfx_cyber_synth_upgrade_v4.wav",
        isDefault = true
    )

    private val build61CuratedSfxPacks: List<SfxPackDef> = listOf(
        SfxPackDef(
            id = "b61_sfx_pack_2",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
            slotIndex = 2,
            displayName = "Titan Pulse SFX Pack",
            description = "Heavy sub-harmonic placement slams and warm brass confirm chimes",
            legacyProfileName = SoundProfile.DEEP_ACOUSTIC.name,
            previewWavFileName = "b61_sfx_pack_2_preview.wav"
        ),
        SfxPackDef(
            id = "b61_sfx_pack_3",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
            slotIndex = 3,
            displayName = "Cryo Chime SFX Pack",
            description = "Glacial harmonic chimes and crystalline UI feedback",
            legacyProfileName = SoundProfile.CYBER_SYNTH.name,
            previewWavFileName = "b61_sfx_pack_3_preview.wav"
        ),
        SfxPackDef(
            id = "b61_sfx_pack_4",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
            slotIndex = 4,
            displayName = "Plasma Coil SFX Pack",
            description = "High-voltage sci-fi sweeps and ionized UI snaps",
            legacyProfileName = SoundProfile.SCI_FI_LASER.name,
            previewWavFileName = "b61_sfx_pack_4_preview.wav"
        ),
        SfxPackDef(
            id = "b61_sfx_pack_5",
            buildCode = B61_BUILD_CODE,
            buildLabel = B61_BUILD_LABEL,
            slotIndex = 5,
            displayName = "Hyper Chip SFX Pack",
            description = "Bright 8-bit chiptune arpeggio confirm and crisp square clicks",
            legacyProfileName = SoundProfile.RETRO_8BIT.name,
            previewWavFileName = "b61_sfx_pack_5_preview.wav"
        )
    )

    fun sfxPackVariantsForBuild(buildCode: Int, isArchived: Boolean): List<SfxPackDef> {
        if (buildCode <= B60_BUILD_CODE) return emptyList()
        if (buildCode == B61_BUILD_CODE) {
            return build61CuratedSfxPacks.map { it.copy(isArchived = isArchived) }
        }
        val label = buildLabelFor(buildCode)
        val profiles = SoundProfile.entries
        return (2..5).map { slot ->
            val idx = abs((buildCode * 4 + (slot - 2) * 3) % sfxPackNames.size)
            val profile = profiles[(buildCode + slot) % profiles.size]
            SfxPackDef(
                id = "b${buildCode}_sfx_pack_$slot",
                buildCode = buildCode,
                buildLabel = label,
                slotIndex = slot,
                displayName = "${sfxPackNames[idx]} SFX Pack",
                description = "Build #$buildCode ${profile.label} UI & tactical feedback pack",
                legacyProfileName = profile.name,
                previewWavFileName = "b${buildCode}_sfx_pack_${slot}_preview.wav",
                isArchived = isArchived
            )
        }
    }

    fun activeSfxPacksFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<SfxPackDef> =
        listOf(defaultSfxPack(targetBuildCode)) + sfxPackVariantsForBuild(targetBuildCode, isArchived = false)

    fun archivedSfxPacksFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<SfxPackDef> =
        buildList {
            if (targetBuildCode > B60_BUILD_CODE) {
                addAll(build60ArchivedSfxPacks)
            }
            for (b in B61_BUILD_CODE until targetBuildCode) {
                addAll(sfxPackVariantsForBuild(b, isArchived = true))
            }
        }

    val activeSfxPacks: List<SfxPackDef>
        get() = activeSfxPacksFor(CURRENT_BUILD_CODE)

    val archivedSfxPacks: List<SfxPackDef>
        get() = archivedSfxPacksFor(CURRENT_BUILD_CODE)

    @Deprecated("Use activeSfxPacks", ReplaceWith("activeSfxPacks"))
    val build61SfxPacks: List<SfxPackDef>
        get() = activeSfxPacks

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

    val allSfxPacks: List<SfxPackDef>
        get() = activeSfxPacks + archivedSfxPacks

    val sfxPacksById: Map<String, SfxPackDef>
        get() = allSfxPacks.associateBy { it.id }

    // -------------------------------------------------------------------------
    // Turret Fire & Upgrade Catalogs (Active Build + Multi-Build Archives)
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

    fun fireVariantLabelForBuild(buildCode: Int, towerClass: TowerClass, slot: Int): String {
        if (slot <= 1) return b61FireVariantLabel(towerClass, 1)
        if (buildCode <= B60_BUILD_CODE) return gunfireVariantLabelB60(towerClass, slot)
        if (buildCode == B61_BUILD_CODE) return b61FireVariantLabel(towerClass, slot)

        val (prefixes, suffixes) = when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> kineticFirePrefixes to kineticFireSuffixes
            TowerClass.ELEMENTAL_VOID -> elementalFirePrefixes to elementalFireSuffixes
            TowerClass.HEAVY_SIEGE -> siegeFirePrefixes to siegeFireSuffixes
            TowerClass.STELLAR_COSMIC -> stellarFirePrefixes to stellarFireSuffixes
        }
        val pIdx = abs((buildCode * 5 + (slot - 2) * 4 + towerClass.ordinal * 3) % prefixes.size)
        val sIdx = abs((buildCode * 3 + (slot - 2) * 5 + towerClass.ordinal * 2) % suffixes.size)
        return "${prefixes[pIdx]} ${suffixes[sIdx]}"
    }

    fun getActiveFireShortLabel(towerClass: TowerClass, slot: Int, buildCode: Int = CURRENT_BUILD_CODE): String =
        fireVariantLabelForBuild(buildCode, towerClass, slot).substringBefore(" ")

    @Deprecated("Use getActiveFireShortLabel", ReplaceWith("getActiveFireShortLabel(towerClass, slot)"))
    fun getBuild61FireShortLabel(towerClass: TowerClass, slot: Int): String =
        getActiveFireShortLabel(towerClass, slot, CURRENT_BUILD_CODE)

    fun turretFireVariantsForBuild(buildCode: Int, isArchived: Boolean): List<TurretAudioDef> {
        if (buildCode <= B60_BUILD_CODE) return emptyList()
        val label = buildLabelFor(buildCode)
        return buildList {
            for (cls in allClasses) {
                val tag = towerClassShortTag(cls)
                for (slot in 2..5) {
                    val vLabel = fireVariantLabelForBuild(buildCode, cls, slot)
                    add(
                        TurretAudioDef(
                            id = "b${buildCode}_fire_${tag}_$slot",
                            section = ArchiveSection.TURRET_FIRE,
                            towerClass = cls,
                            buildCode = buildCode,
                            buildLabel = label,
                            slotIndex = slot,
                            displayName = "${towerClassLabel(cls)} Fire #$slot: $vLabel",
                            description = "Build #$buildCode firing synthesis variant",
                            wavFileName = "b${buildCode}_sfx_gunfire_${tag}_slot${slot}.wav",
                            isDefault = false,
                            isArchived = isArchived
                        )
                    )
                }
            }
        }
    }

    fun activeTurretFireDefsFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<TurretAudioDef> {
        val variantsByKey = turretFireVariantsForBuild(targetBuildCode, isArchived = false)
            .associateBy { it.towerClass to it.slotIndex }
        return buildList {
            for (cls in allClasses) {
                val tag = towerClassShortTag(cls)
                val ver = if (cls == TowerClass.STELLAR_COSMIC) "v1" else "v2"
                add(
                    TurretAudioDef(
                        id = "default_fire_$tag",
                        section = ArchiveSection.TURRET_FIRE,
                        towerClass = cls,
                        buildCode = targetBuildCode,
                        buildLabel = "Default (Core)",
                        slotIndex = 1,
                        displayName = "${towerClassLabel(cls)} Fire #1: ${fireVariantLabelForBuild(targetBuildCode, cls, 1)}",
                        description = "Canonical Slot 1 firing waveform",
                        wavFileName = "sfx_gunfire_${tag}_slot1_${ver}.wav",
                        isDefault = true,
                        isArchived = false
                    )
                )
                for (slot in 2..5) {
                    variantsByKey[cls to slot]?.let { add(it) }
                }
            }
        }
    }

    fun archivedTurretFireDefsFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<TurretAudioDef> =
        buildList {
            if (targetBuildCode > B60_BUILD_CODE) {
                addAll(build60ArchivedFireDefs)
            }
            for (b in B61_BUILD_CODE until targetBuildCode) {
                addAll(turretFireVariantsForBuild(b, isArchived = true))
            }
        }

    val activeTurretFireDefs: List<TurretAudioDef>
        get() = activeTurretFireDefsFor(CURRENT_BUILD_CODE)

    val archivedTurretFireDefs: List<TurretAudioDef>
        get() = archivedTurretFireDefsFor(CURRENT_BUILD_CODE)

    @Deprecated("Use activeTurretFireDefs", ReplaceWith("activeTurretFireDefs"))
    val build61TurretFireDefs: List<TurretAudioDef>
        get() = activeTurretFireDefs

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

    fun turretUpgradeVariantsForBuild(buildCode: Int, isArchived: Boolean): List<TurretAudioDef> {
        if (buildCode <= B60_BUILD_CODE) return emptyList()
        val label = buildLabelFor(buildCode)
        val skinNames = BuildArchiveVisualCatalog.variantSkinsForBuild(buildCode, isArchived = isArchived)
            .associateBy { it.slotIndex }
        return buildList {
            for (cls in allClasses) {
                for (slot in 2..5) {
                    val skinName = skinNames[slot]?.displayName ?: "Variant $slot"
                    add(
                        TurretAudioDef(
                            id = "b${buildCode}_upgrade_${cls.name.lowercase()}_$slot",
                            section = ArchiveSection.TURRET_UPGRADE,
                            towerClass = cls,
                            buildCode = buildCode,
                            buildLabel = label,
                            slotIndex = slot,
                            displayName = "${towerClassLabel(cls)} Upgrade #$slot ($skinName)",
                            description = "Build #$buildCode upgrade synthesis chime",
                            wavFileName = "b${buildCode}_sfx_upgrade_${cls.name.lowercase()}_slot${slot}.wav",
                            isDefault = false,
                            isArchived = isArchived
                        )
                    )
                }
            }
        }
    }

    fun activeTurretUpgradeDefsFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<TurretAudioDef> {
        val variantsByKey = turretUpgradeVariantsForBuild(targetBuildCode, isArchived = false)
            .associateBy { it.towerClass to it.slotIndex }
        val defaultSkinName = BuildArchiveVisualCatalog.defaultTurretSkin.displayName
        return buildList {
            for (cls in allClasses) {
                add(
                    TurretAudioDef(
                        id = "default_upgrade_${cls.name.lowercase()}",
                        section = ArchiveSection.TURRET_UPGRADE,
                        towerClass = cls,
                        buildCode = targetBuildCode,
                        buildLabel = "Default (Core)",
                        slotIndex = 1,
                        displayName = "${towerClassLabel(cls)} Upgrade #1 ($defaultSkinName)",
                        description = "Canonical Slot 1 upgrade chime",
                        wavFileName = "sfx_upgrade_${cls.name.lowercase()}_slot1_v2.wav",
                        isDefault = true,
                        isArchived = false
                    )
                )
                for (slot in 2..5) {
                    variantsByKey[cls to slot]?.let { add(it) }
                }
            }
        }
    }

    fun archivedTurretUpgradeDefsFor(targetBuildCode: Int = CURRENT_BUILD_CODE): List<TurretAudioDef> =
        buildList {
            if (targetBuildCode > B60_BUILD_CODE) {
                addAll(build60ArchivedUpgradeDefs)
            }
            for (b in B61_BUILD_CODE until targetBuildCode) {
                addAll(turretUpgradeVariantsForBuild(b, isArchived = true))
            }
        }

    val activeTurretUpgradeDefs: List<TurretAudioDef>
        get() = activeTurretUpgradeDefsFor(CURRENT_BUILD_CODE)

    val archivedTurretUpgradeDefs: List<TurretAudioDef>
        get() = archivedTurretUpgradeDefsFor(CURRENT_BUILD_CODE)

    @Deprecated("Use activeTurretUpgradeDefs", ReplaceWith("activeTurretUpgradeDefs"))
    val build61TurretUpgradeDefs: List<TurretAudioDef>
        get() = activeTurretUpgradeDefs

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

    val allTurretAudioDefs: List<TurretAudioDef>
        get() = activeTurretFireDefs + archivedTurretFireDefs + activeTurretUpgradeDefs + archivedTurretUpgradeDefs

    val turretAudioById: Map<String, TurretAudioDef>
        get() = allTurretAudioDefs.associateBy { it.id }

    // -------------------------------------------------------------------------
    // Resolvers & Legacy ID Migration
    // -------------------------------------------------------------------------
    fun resolveTurretSkin(skinId: String?, legacySlot: Int = 1): TurretSkinDef {
        val canonical = skinId?.let { migrateLegacyAssetId(it) }
        if (canonical != null) {
            BuildArchiveVisualCatalog.findOrGenerateSkinById(canonical)?.let { return it }
        }
        return BuildArchiveVisualCatalog.activeTurretSkins.firstOrNull { it.slotIndex == legacySlot }
            ?: BuildArchiveVisualCatalog.defaultTurretSkin
    }

    fun resolveMapTheme(themeId: String?, legacySlot: Int = 1): MapThemeDef {
        val canonical = themeId?.let { migrateLegacyAssetId(it) }
        if (canonical != null) {
            BuildArchiveVisualCatalog.findOrGenerateThemeById(canonical)?.let { return it }
        }
        return BuildArchiveVisualCatalog.activeMapThemes.firstOrNull { it.slotIndex == legacySlot }
            ?: BuildArchiveVisualCatalog.defaultMapTheme
    }

    fun resolveBgmTrack(bgmId: String?): BgmTrackDef {
        val canonical = bgmId?.let { migrateLegacyAssetId(it) }
        if (canonical != null) {
            bgmById[canonical]?.let { return it }
            val match = dynamicBgmRegex.matchEntire(canonical)
            val build = match?.groupValues?.getOrNull(1)?.toIntOrNull()
            val slot = match?.groupValues?.getOrNull(2)?.toIntOrNull()
            if (build != null && build >= B61_BUILD_CODE && slot in 2..5) {
                bgmVariantsForBuild(build, isArchived = build < CURRENT_BUILD_CODE)
                    .firstOrNull { it.slotIndex == slot }?.let { return it }
            }
        }
        return activeBgmTracks.first()
    }

    fun resolveSfxPack(packId: String?): SfxPackDef {
        val canonical = packId?.let { migrateLegacyAssetId(it) }
        if (canonical != null) {
            sfxPacksById[canonical]?.let { return it }
            val match = dynamicSfxRegex.matchEntire(canonical)
            val build = match?.groupValues?.getOrNull(1)?.toIntOrNull()
            val slot = match?.groupValues?.getOrNull(2)?.toIntOrNull()
            if (build != null && build >= B61_BUILD_CODE && slot in 2..5) {
                sfxPackVariantsForBuild(build, isArchived = build < CURRENT_BUILD_CODE)
                    .firstOrNull { it.slotIndex == slot }?.let { return it }
            }
        }
        return activeSfxPacks.first()
    }

    fun resolveTurretAudio(assetId: String): TurretAudioDef? {
        val canonical = migrateLegacyAssetId(assetId)
        turretAudioById[canonical]?.let { return it }
        dynamicFireRegex.matchEntire(canonical)?.let { match ->
            val build = match.groupValues[1].toIntOrNull() ?: return@let
            val tag = match.groupValues[2]
            val slot = match.groupValues[3].toIntOrNull() ?: return@let
            val cls = allClasses.firstOrNull { towerClassShortTag(it) == tag } ?: return@let
            if (build >= B61_BUILD_CODE && slot in 2..5) {
                return turretFireVariantsForBuild(build, isArchived = build < CURRENT_BUILD_CODE)
                    .firstOrNull { it.towerClass == cls && it.slotIndex == slot }
            }
        }
        dynamicUpgradeRegex.matchEntire(canonical)?.let { match ->
            val build = match.groupValues[1].toIntOrNull() ?: return@let
            val clsName = match.groupValues[2]
            val slot = match.groupValues[3].toIntOrNull() ?: return@let
            val cls = allClasses.firstOrNull { it.name.equals(clsName, ignoreCase = true) } ?: return@let
            if (build >= B61_BUILD_CODE && slot in 2..5) {
                return turretUpgradeVariantsForBuild(build, isArchived = build < CURRENT_BUILD_CODE)
                    .firstOrNull { it.towerClass == cls && it.slotIndex == slot }
            }
        }
        return null
    }

    fun buildActiveFireId(towerClass: TowerClass, slot: Int, buildCode: Int = CURRENT_BUILD_CODE): String {
        val s = slot.coerceIn(1, 5)
        val tag = towerClassShortTag(towerClass)
        return if (s == 1) "default_fire_$tag" else "b${buildCode}_fire_${tag}_$s"
    }

    fun buildActiveUpgradeId(towerClass: TowerClass, slot: Int, buildCode: Int = CURRENT_BUILD_CODE): String {
        val s = slot.coerceIn(1, 5)
        return if (s == 1) "default_upgrade_${towerClass.name.lowercase()}" else "b${buildCode}_upgrade_${towerClass.name.lowercase()}_$s"
    }

    fun buildActiveSkinId(slot: Int, buildCode: Int = CURRENT_BUILD_CODE): String {
        val s = slot.coerceIn(1, 5)
        return if (s == 1) "default_skin_1" else "b${buildCode}_skin_$s"
    }

    fun buildActiveThemeId(slot: Int, buildCode: Int = CURRENT_BUILD_CODE): String {
        val s = slot.coerceIn(1, 5)
        return if (s == 1) "default_theme_1" else "b${buildCode}_theme_$s"
    }

    /**
     * Migrates pre-Build #61 (v0.0.1.039) local/vote IDs into immutable build-scoped IDs
     * so existing user ratings carry cleanly into Build #60 Archives & Default Core without collisions.
     */
    fun migrateLegacyAssetId(rawId: String): String {
        if (rawId.isBlank()) return rawId
        if (buildScopedIdRegex.matches(rawId) || rawId.startsWith("pre60_") || rawId.startsWith("default_")) {
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
