package com.example.audio

import android.content.Context
import com.example.BuildConfig
import com.example.model.ArchiveSection
import com.example.model.TowerClass
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistent Sound & Skin Archive Store.
 * Manages permanent `.wav` storage under `filesDir/sound_archive`, migrates legacy `cacheDir` `.wav` files,
 * builds the versioned Build-over-Build asset catalog (Pre-Build #60, Build #60, Build #61+),
 * and handles JSON import/export for Like/Dislike/Star ratings with legacy ID migration.
 */
internal class SoundArchiveStore(context: Context) {

    private val appContext = context.applicationContext

    val archiveDir: File = File(appContext.filesDir, "sound_archive").apply {
        if (!exists()) mkdirs()
    }

    val libraryJsonFile: File = File(appContext.filesDir, "exported_skins_sounds_library.json")

    fun getMusicFile(track: MusicTrack): File =
        File(archiveDir, "bgm_${track.name.lowercase()}_v5_lowpass.wav")

    fun getMusicFileForAsset(bgmAssetId: String): File {
        val def = BuildArchiveCatalog.resolveBgmTrack(bgmAssetId)
        return File(archiveDir, def.wavFileName)
    }

    fun getGenericSfxFile(profile: SoundProfile, fx: SoundFx): File =
        File(archiveDir, "sfx_${profile.name.lowercase()}_${fx.name.lowercase()}_v4.wav")

    fun getGunfireFile(towerClass: TowerClass, slot: Int): File {
        val defId = BuildArchiveCatalog.buildActiveFireId(towerClass, slot)
        val def = BuildArchiveCatalog.turretAudioById[defId]
        return if (def != null) File(archiveDir, def.wavFileName)
        else File(archiveDir, "sfx_gunfire_${BuildArchiveCatalog.towerClassShortTag(towerClass)}_slot${slot}_v2.wav")
    }

    fun getUpgradeFile(towerClass: TowerClass, slot: Int): File {
        val defId = BuildArchiveCatalog.buildActiveUpgradeId(towerClass, slot)
        val def = BuildArchiveCatalog.turretAudioById[defId]
        return if (def != null) File(archiveDir, def.wavFileName)
        else File(archiveDir, "sfx_upgrade_${towerClass.name.lowercase()}_slot${slot}_v2.wav")
    }

    fun getWavFileForAsset(rawAssetId: String, activeProfile: SoundProfile = SoundProfile.CYBER_SYNTH): File? {
        val assetId = BuildArchiveCatalog.migrateLegacyAssetId(rawAssetId)
        BuildArchiveCatalog.bgmById[assetId]?.let { return File(archiveDir, it.wavFileName) }
        BuildArchiveCatalog.sfxPacksById[assetId]?.let { pack ->
            if (pack.previewWavFileName.isNotBlank()) return File(archiveDir, pack.previewWavFileName)
        }
        if (assetId.startsWith("default_sfx_")) {
            val fxName = assetId.removePrefix("default_sfx_").uppercase()
            val fx = SoundFx.entries.firstOrNull { it.name == fxName }
            if (fx != null) return getGenericSfxFile(activeProfile, fx)
        }
        BuildArchiveCatalog.turretAudioById[assetId]?.let { return File(archiveDir, it.wavFileName) }
        return null
    }

    fun writeWavAtomicallyIfNeeded(file: File, synthesize: () -> ShortArray) {
        if (file.exists() && file.length() >= 64L) return
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val tmpFile = File(file.parentFile, "${file.name}.${System.nanoTime()}.tmp")
        try {
            val wavBytes = AudioSynthesizer.toWav(synthesize())
            java.io.FileOutputStream(tmpFile).use { it.write(wavBytes) }
            if (!tmpFile.renameTo(file)) {
                if (file.exists()) file.delete()
                if (!tmpFile.renameTo(file)) {
                    java.io.FileOutputStream(file).use { it.write(wavBytes) }
                }
            }
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }
    }

    /**
     * Copies any legacy `.wav` files stranded in `cacheDir` into persistent `filesDir/sound_archive`.
     * Runs on `Dispatchers.IO`. Returns the number of files migrated.
     */
    fun migrateLegacyCacheWavsToArchive(): Int {
        var migrated = 0
        try {
            if (!archiveDir.exists()) archiveDir.mkdirs()
            appContext.cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".wav", ignoreCase = true) }
                ?.forEach { cachedWav ->
                    val dest = File(archiveDir, cachedWav.name)
                    if (!dest.exists() || dest.length() < 64L) {
                        cachedWav.copyTo(dest, overwrite = true)
                        migrated++
                    }
                }
        } catch (_: Exception) {}
        return migrated
    }

    /**
     * Builds the complete immutable versioned asset catalog (Active Build #61 + Default,
     * Build #60 Snapshot, and Pre-Build #60 legacy skins/themes/WAVs) without synthesizing audio.
     */
    fun buildAssetLibrary(
        activeProfile: SoundProfile,
        equippedBgmId: String,
        equippedSfxPackId: String,
        equippedFireIds: Array<String>,
        equippedUpgradeIds: Array<String>,
        equippedTurretSkinId: String,
        equippedMapThemeId: String
    ): List<CustomAssetRecord> {
        val records = mutableListOf<CustomAssetRecord>()
        val knownFilenames = mutableSetOf<String>()

        // Register all known SoundProfile generic WAV names so only truly legacy/unknown WAVs become ARCHIVED_WAV
        SoundProfile.entries.forEach { prof ->
            SoundFx.entries.forEach { fx -> knownFilenames.add(getGenericSfxFile(prof, fx).name) }
        }

        // 1. Active Background Music (Rolling Playlist + Default Track 1 + 4 Build #61 tracks)
        BuildArchiveCatalog.build61BgmTracks.forEach { bgm ->
            val file = File(archiveDir, bgm.wavFileName)
            knownFilenames.add(file.name)
            records.add(
                CustomAssetRecord(
                    id = bgm.id,
                    type = "SOUND",
                    name = bgm.displayName,
                    category = "BACKGROUND_MUSIC",
                    metadata = bgm.description,
                    slotIndex = bgm.slotIndex,
                    filePath = file.absolutePath,
                    isEquipped = (equippedBgmId == bgm.id),
                    buildCode = bgm.buildCode,
                    buildLabel = bgm.buildLabel,
                    archiveSection = ArchiveSection.BGM_SFX.id,
                    isArchived = false,
                    isDefault = bgm.isDefault
                )
            )
        }

        // 2. Active Generic SFX (Default Cyber Synth items) + 4 new Build #61 SFX Packs
        for (fx in SoundFx.entries) {
            val file = getGenericSfxFile(activeProfile, fx)
            knownFilenames.add(file.name)
            records.add(
                CustomAssetRecord(
                    id = "default_sfx_${fx.name.lowercase()}",
                    type = "SOUND",
                    name = "${fx.label} (${activeProfile.name})",
                    category = "SFX",
                    slotIndex = fx.ordinal,
                    filePath = file.absolutePath,
                    buildCode = CURRENT_BUILD_CODE,
                    buildLabel = "Default (Core)",
                    archiveSection = ArchiveSection.BGM_SFX.id,
                    isArchived = false,
                    isDefault = true
                )
            )
        }
        BuildArchiveCatalog.build61SfxPacks.forEach { pack ->
            val file = File(archiveDir, pack.previewWavFileName)
            knownFilenames.add(file.name)
            records.add(
                CustomAssetRecord(
                    id = pack.id,
                    type = "SOUND",
                    name = pack.displayName,
                    category = "SFX",
                    metadata = pack.description,
                    slotIndex = pack.slotIndex,
                    filePath = file.absolutePath,
                    isEquipped = (equippedSfxPackId == pack.id),
                    buildCode = pack.buildCode,
                    buildLabel = pack.buildLabel,
                    archiveSection = ArchiveSection.BGM_SFX.id,
                    isArchived = false,
                    isDefault = pack.isDefault
                )
            )
        }

        // 3. Active Turret Fire (20: 4 classes x (Default + 4 Build #61))
        BuildArchiveCatalog.build61TurretFireDefs.forEach { def ->
            val file = File(archiveDir, def.wavFileName)
            knownFilenames.add(file.name)
            val classIdx = def.towerClass.ordinal
            records.add(
                CustomAssetRecord(
                    id = def.id,
                    type = "SOUND",
                    name = def.displayName,
                    category = "TURRET_FIRE",
                    metadata = def.description,
                    towerClassOrdinal = classIdx,
                    slotIndex = def.slotIndex,
                    filePath = file.absolutePath,
                    isEquipped = (equippedFireIds.getOrElse(classIdx) { "" } == def.id),
                    buildCode = def.buildCode,
                    buildLabel = def.buildLabel,
                    archiveSection = ArchiveSection.TURRET_FIRE.id,
                    isArchived = false,
                    isDefault = def.isDefault
                )
            )
        }

        // 4. Active Turret Upgrades (20: 4 classes x (Default + 4 Build #61))
        BuildArchiveCatalog.build61TurretUpgradeDefs.forEach { def ->
            val file = File(archiveDir, def.wavFileName)
            knownFilenames.add(file.name)
            val classIdx = def.towerClass.ordinal
            records.add(
                CustomAssetRecord(
                    id = def.id,
                    type = "SOUND",
                    name = def.displayName,
                    category = "TURRET_UPGRADE",
                    metadata = def.description,
                    towerClassOrdinal = classIdx,
                    slotIndex = def.slotIndex,
                    filePath = file.absolutePath,
                    isEquipped = (equippedUpgradeIds.getOrElse(classIdx) { "" } == def.id),
                    buildCode = def.buildCode,
                    buildLabel = def.buildLabel,
                    archiveSection = ArchiveSection.TURRET_UPGRADE.id,
                    isArchived = false,
                    isDefault = def.isDefault
                )
            )
        }

        // 5. Active Turret Visual Skins (5: Default + 4 Build #61)
        BuildArchiveVisualCatalog.build61TurretSkins.forEach { skin ->
            records.add(
                CustomAssetRecord(
                    id = skin.id,
                    type = "SKIN",
                    name = skin.displayName,
                    category = "TURRET_SKIN",
                    slotIndex = skin.slotIndex,
                    unlockScore = (skin.slotIndex - 1) * 5000,
                    metadata = skin.description,
                    isEquipped = (equippedTurretSkinId == skin.id),
                    buildCode = skin.buildCode,
                    buildLabel = skin.buildLabel,
                    archiveSection = ArchiveSection.TURRET_SKIN.id,
                    isArchived = false,
                    isDefault = skin.isDefault
                )
            )
        }

        // 6. Active Map Themes (5: Default + 4 Build #61)
        BuildArchiveVisualCatalog.build61MapThemes.forEach { theme ->
            records.add(
                CustomAssetRecord(
                    id = theme.id,
                    type = "SKIN",
                    name = theme.displayName,
                    category = "MAP_THEME",
                    slotIndex = theme.slotIndex,
                    unlockScore = (theme.slotIndex - 1) * 5000,
                    metadata = theme.description,
                    isEquipped = (equippedMapThemeId == theme.id),
                    buildCode = theme.buildCode,
                    buildLabel = theme.buildLabel,
                    archiveSection = ArchiveSection.MAP_THEME.id,
                    isArchived = false,
                    isDefault = theme.isDefault
                )
            )
        }

        // 7. Archives: Build #60 Snapshot (Turret Fire, Upgrades, BGM, SFX, Skins, Map Themes)
        BuildArchiveCatalog.build60ArchivedFireDefs.forEach { def ->
            val file = File(archiveDir, def.wavFileName)
            knownFilenames.add(file.name)
            val classIdx = def.towerClass.ordinal
            records.add(
                CustomAssetRecord(
                    id = def.id,
                    type = "SOUND",
                    name = def.displayName,
                    category = "ARCHIVE",
                    metadata = def.description,
                    towerClassOrdinal = classIdx,
                    slotIndex = def.slotIndex,
                    filePath = file.absolutePath,
                    isEquipped = (equippedFireIds.getOrElse(classIdx) { "" } == def.id),
                    buildCode = def.buildCode,
                    buildLabel = def.buildLabel,
                    archiveSection = ArchiveSection.TURRET_FIRE.id,
                    isArchived = true
                )
            )
        }
        BuildArchiveCatalog.build60ArchivedUpgradeDefs.forEach { def ->
            val file = File(archiveDir, def.wavFileName)
            knownFilenames.add(file.name)
            val classIdx = def.towerClass.ordinal
            records.add(
                CustomAssetRecord(
                    id = def.id,
                    type = "SOUND",
                    name = def.displayName,
                    category = "ARCHIVE",
                    metadata = def.description,
                    towerClassOrdinal = classIdx,
                    slotIndex = def.slotIndex,
                    filePath = file.absolutePath,
                    isEquipped = (equippedUpgradeIds.getOrElse(classIdx) { "" } == def.id),
                    buildCode = def.buildCode,
                    buildLabel = def.buildLabel,
                    archiveSection = ArchiveSection.TURRET_UPGRADE.id,
                    isArchived = true
                )
            )
        }
        BuildArchiveCatalog.build60ArchivedBgmTracks.forEach { bgm ->
            val file = File(archiveDir, bgm.wavFileName)
            knownFilenames.add(file.name)
            records.add(
                CustomAssetRecord(
                    id = bgm.id,
                    type = "SOUND",
                    name = bgm.displayName,
                    category = "ARCHIVE",
                    metadata = bgm.description,
                    slotIndex = bgm.slotIndex,
                    filePath = file.absolutePath,
                    isEquipped = (equippedBgmId == bgm.id),
                    buildCode = bgm.buildCode,
                    buildLabel = bgm.buildLabel,
                    archiveSection = ArchiveSection.BGM_SFX.id,
                    isArchived = true
                )
            )
        }
        BuildArchiveCatalog.build60ArchivedSfxPacks.forEach { pack ->
            val file = File(archiveDir, pack.previewWavFileName)
            knownFilenames.add(file.name)
            records.add(
                CustomAssetRecord(
                    id = pack.id,
                    type = "SOUND",
                    name = pack.displayName,
                    category = "ARCHIVE",
                    metadata = pack.description,
                    slotIndex = pack.slotIndex,
                    filePath = file.absolutePath,
                    isEquipped = (equippedSfxPackId == pack.id),
                    buildCode = pack.buildCode,
                    buildLabel = pack.buildLabel,
                    archiveSection = ArchiveSection.BGM_SFX.id,
                    isArchived = true
                )
            )
        }
        BuildArchiveVisualCatalog.build60ArchivedSkins.forEach { skin ->
            records.add(
                CustomAssetRecord(
                    id = skin.id,
                    type = "SKIN",
                    name = skin.displayName,
                    category = "ARCHIVE",
                    slotIndex = skin.slotIndex,
                    metadata = skin.description,
                    isEquipped = (equippedTurretSkinId == skin.id),
                    buildCode = skin.buildCode,
                    buildLabel = skin.buildLabel,
                    archiveSection = ArchiveSection.TURRET_SKIN.id,
                    isArchived = true
                )
            )
        }
        BuildArchiveVisualCatalog.build60ArchivedThemes.forEach { theme ->
            records.add(
                CustomAssetRecord(
                    id = theme.id,
                    type = "SKIN",
                    name = theme.displayName,
                    category = "ARCHIVE",
                    slotIndex = theme.slotIndex,
                    metadata = theme.description,
                    isEquipped = (equippedMapThemeId == theme.id),
                    buildCode = theme.buildCode,
                    buildLabel = theme.buildLabel,
                    archiveSection = ArchiveSection.MAP_THEME.id,
                    isArchived = true
                )
            )
        }

        // 8. Archives: Pre-Build #60 Legacy SkinTheme Presets & Unrecognized On-Disk WAVs
        BuildArchiveVisualCatalog.pre60ArchivedSkins.forEach { skin ->
            records.add(
                CustomAssetRecord(
                    id = skin.id,
                    type = "SKIN",
                    name = skin.displayName,
                    category = "ARCHIVE",
                    slotIndex = skin.slotIndex,
                    metadata = skin.description,
                    isEquipped = (equippedTurretSkinId == skin.id),
                    buildCode = PRE60_BUILD_CODE,
                    buildLabel = PRE60_BUILD_LABEL,
                    archiveSection = ArchiveSection.TURRET_SKIN.id,
                    isArchived = true
                )
            )
        }
        BuildArchiveVisualCatalog.pre60ArchivedThemes.forEach { theme ->
            records.add(
                CustomAssetRecord(
                    id = theme.id,
                    type = "SKIN",
                    name = theme.displayName,
                    category = "ARCHIVE",
                    slotIndex = theme.slotIndex,
                    metadata = theme.description,
                    isEquipped = (equippedMapThemeId == theme.id),
                    buildCode = PRE60_BUILD_CODE,
                    buildLabel = PRE60_BUILD_LABEL,
                    archiveSection = ArchiveSection.MAP_THEME.id,
                    isArchived = true
                )
            )
        }
        try {
            archiveDir.listFiles { f -> f.isFile && f.name.endsWith(".wav", ignoreCase = true) }
                ?.sortedBy { it.name }
                ?.forEach { wavFile ->
                    if (wavFile.name !in knownFilenames) {
                        val section = BuildArchiveCatalog.inferArchiveSectionFromFilename(wavFile.name)
                        val cleanId = "pre60_wav_${wavFile.nameWithoutExtension.lowercase()}"
                        records.add(
                            CustomAssetRecord(
                                id = cleanId,
                                type = "SOUND",
                                name = "Legacy WAV: ${wavFile.nameWithoutExtension}",
                                category = "ARCHIVED_WAV",
                                metadata = "Imported legacy audio file",
                                filePath = wavFile.absolutePath,
                                isEquipped = equippedFireIds.contains(cleanId) ||
                                    equippedUpgradeIds.contains(cleanId) ||
                                    equippedBgmId == cleanId,
                                buildCode = PRE60_BUILD_CODE,
                                buildLabel = PRE60_BUILD_LABEL,
                                archiveSection = section.id,
                                isArchived = true
                            )
                        )
                    }
                }
        } catch (_: Exception) {}

        return records
    }

    /**
     * Restores persisted Like/Dislike/Star ratings from disk while preserving any newer in-memory ratings.
     */
    fun applyPersistedAndInMemoryStates(
        baseRecords: List<CustomAssetRecord>,
        inMemoryStates: Map<String, CustomAssetRecord>
    ): List<CustomAssetRecord> {
        val persistedMap = readPersistedVoteMap()
        return baseRecords.map { record ->
            val persisted = persistedMap[record.id]
            val inMemory = inMemoryStates[record.id]

            val chosenVote: Int
            val chosenStar: Boolean
            val chosenUpdatedAt: Long

            when {
                inMemory != null && persisted != null -> {
                    if (inMemory.updatedAtEpochMs >= persisted.updatedAtEpochMs) {
                        chosenVote = inMemory.voteState
                        chosenStar = inMemory.isFavoriteStar
                        chosenUpdatedAt = inMemory.updatedAtEpochMs
                    } else {
                        chosenVote = persisted.voteState
                        chosenStar = persisted.isFavoriteStar
                        chosenUpdatedAt = persisted.updatedAtEpochMs
                    }
                }
                inMemory != null -> {
                    chosenVote = inMemory.voteState
                    chosenStar = inMemory.isFavoriteStar
                    chosenUpdatedAt = inMemory.updatedAtEpochMs
                }
                persisted != null -> {
                    chosenVote = persisted.voteState
                    chosenStar = persisted.isFavoriteStar
                    chosenUpdatedAt = persisted.updatedAtEpochMs
                }
                else -> {
                    chosenVote = record.voteState
                    chosenStar = record.isFavoriteStar
                    chosenUpdatedAt = record.updatedAtEpochMs
                }
            }

            val clampedVote = chosenVote.coerceIn(-1, 1)
            record.copy(
                voteState = clampedVote,
                isFavoriteHeart = (clampedVote == 1),
                isFavoriteStar = chosenStar,
                updatedAtEpochMs = chosenUpdatedAt
            )
        }
    }

    private data class PersistedAssetRating(
        val voteState: Int,
        val isFavoriteStar: Boolean,
        val updatedAtEpochMs: Long
    )

    private fun readPersistedVoteMap(): Map<String, PersistedAssetRating> {
        if (!libraryJsonFile.exists()) return emptyMap()
        val result = mutableMapOf<String, PersistedAssetRating>()
        try {
            val root = JSONObject(libraryJsonFile.readText())
            val assetsArr = root.optJSONArray("assets") ?: return emptyMap()
            for (i in 0 until assetsArr.length()) {
                val obj = assetsArr.optJSONObject(i) ?: continue
                val rawId = obj.optString("id", "")
                if (rawId.isBlank()) continue
                val id = BuildArchiveCatalog.migrateLegacyAssetId(rawId)
                val heart = obj.optBoolean("heart", false)
                val vote = obj.optInt("voteState", if (heart) 1 else 0).coerceIn(-1, 1)
                val star = obj.optBoolean("star", false)
                val updatedAt = obj.optLong("updatedAtEpochMs", 0L)
                if (vote != 0 || star || updatedAt > 0L) {
                    val prev = result[id]
                    if (prev == null || updatedAt >= prev.updatedAtEpochMs) {
                        result[id] = PersistedAssetRating(vote, star, updatedAt)
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun exportLibraryJson(context: Context = appContext, records: List<CustomAssetRecord>): String {
        val root = JSONObject()
        root.put("appName", "Dual Front TD")
        root.put("versionName", BuildConfig.VERSION_NAME)
        root.put("versionCode", BuildConfig.VERSION_CODE)
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        val deltasArr = JSONArray()
        records.filter { it.voteState != 0 || it.isFavoriteStar || it.isFavoriteHeart }.forEach { item ->
            deltasArr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("category", item.category)
                    put("archiveSection", item.archiveSection)
                    put("buildCode", item.buildCode)
                    put("buildLabel", item.buildLabel)
                    put("voteState", item.voteState)
                    put("heart", item.isFavoriteHeart)
                    put("star", item.isFavoriteStar)
                    put("updatedAtEpochMs", item.updatedAtEpochMs)
                }
            )
        }
        root.put("deltas", deltasArr)

        val assetsArr = JSONArray()
        records.forEach { item ->
            assetsArr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("name", item.name)
                    put("category", item.category)
                    put("archiveSection", item.archiveSection)
                    put("buildCode", item.buildCode)
                    put("buildLabel", item.buildLabel)
                    put("isArchived", item.isArchived)
                    put("isDefault", item.isDefault)
                    put("voteState", item.voteState)
                    put("heart", item.isFavoriteHeart)
                    put("star", item.isFavoriteStar)
                    put("equipped", item.isEquipped)
                    put("unlockScore", item.unlockScore)
                    put("updatedAtEpochMs", item.updatedAtEpochMs)
                }
            )
        }
        root.put("assets", assetsArr)

        val jsonStr = root.toString(2)
        val targetFile = File(context.filesDir, "exported_skins_sounds_library.json")
        val tmpFile = File(context.filesDir, "exported_skins_sounds_library.json.tmp")
        try {
            targetFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            tmpFile.writeText(jsonStr)
            if (!tmpFile.renameTo(targetFile)) {
                targetFile.writeText(jsonStr)
            }
        } catch (_: Exception) {
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
        return jsonStr
    }
}
