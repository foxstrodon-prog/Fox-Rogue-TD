package com.example.audio

import android.content.Context
import com.example.BuildConfig
import com.example.model.MapThemeSlot
import com.example.model.TowerClass
import com.example.model.TurretSfxSlot
import com.example.model.TurretSkinSlot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistent Sound & Skin Archive Store.
 * Manages permanent `.wav` storage under `filesDir/sound_archive`, migrates legacy `cacheDir` `.wav` files,
 * builds the full asset catalog, and handles JSON import/export for Like/Dislike/Star ratings.
 */
internal class SoundArchiveStore(context: Context) {

    private val appContext = context.applicationContext

    val archiveDir: File = File(appContext.filesDir, "sound_archive").apply {
        if (!exists()) mkdirs()
    }

    val libraryJsonFile: File = File(appContext.filesDir, "exported_skins_sounds_library.json")

    fun getMusicFile(track: MusicTrack): File =
        File(archiveDir, "bgm_${track.name.lowercase()}_v5_lowpass.wav")

    fun getGenericSfxFile(profile: SoundProfile, fx: SoundFx): File =
        File(archiveDir, "sfx_${profile.name.lowercase()}_${fx.name.lowercase()}_v4.wav")

    fun getGunfireFile(towerClass: TowerClass, slot: Int): File {
        val tag = towerClassShortTag(towerClass)
        val ver = if (towerClass == TowerClass.STELLAR_COSMIC) "v1" else "v2"
        return File(archiveDir, "sfx_gunfire_${tag}_slot${slot}_${ver}.wav")
    }

    fun getUpgradeFile(towerClass: TowerClass, slot: Int): File =
        File(archiveDir, "sfx_upgrade_${towerClass.name.lowercase()}_slot${slot}_v2.wav")

    /**
     * Copies any legacy `.wav` files stranded in `cacheDir` into persistent `filesDir/sound_archive`.
     * Designed to run on `Dispatchers.IO`. Returns the number of files migrated.
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
     * Builds the complete immutable asset catalog without synthesizing any audio on the caller thread.
     */
    fun buildAssetLibrary(
        activeProfile: SoundProfile,
        activeMusicTrack: MusicTrack,
        equippedFireSlots: IntArray,
        equippedUpgradeSlots: IntArray,
        equippedTurretSkinSlot: Int,
        equippedMapThemeSlot: Int
    ): List<CustomAssetRecord> {
        val records = mutableListOf<CustomAssetRecord>()
        val knownFilenames = mutableSetOf<String>()

        // 1. Background Music Tracks
        for (track in MusicTrack.values()) {
            if (track != MusicTrack.NONE) {
                val file = getMusicFile(track)
                knownFilenames.add(file.name)
                records.add(
                    CustomAssetRecord(
                        id = "bgm_${track.name.lowercase()}",
                        type = "SOUND",
                        name = track.label,
                        category = "BACKGROUND_MUSIC",
                        filePath = file.absolutePath,
                        isEquipped = (activeMusicTrack == track)
                    )
                )
            }
        }

        // 2. Generic Game SFX (for active SoundProfile)
        for (fx in SoundFx.values()) {
            val file = getGenericSfxFile(activeProfile, fx)
            knownFilenames.add(file.name)
            records.add(
                CustomAssetRecord(
                    id = "sfx_${fx.name.lowercase()}",
                    type = "SOUND",
                    name = "${fx.label} (${activeProfile.name})",
                    category = "SFX",
                    slotIndex = fx.ordinal,
                    filePath = file.absolutePath
                )
            )
        }

        // 3. 20 Turret Gunfire Variants (4 Classes x 5 Slots)
        val classes = listOf(
            TowerClass.KINETIC_BALLISTIC,
            TowerClass.ELEMENTAL_VOID,
            TowerClass.HEAVY_SIEGE,
            TowerClass.STELLAR_COSMIC
        )
        classes.forEachIndexed { classIdx, tClass ->
            val shortTag = towerClassShortTag(tClass)
            for (slot in 1..5) {
                val file = getGunfireFile(tClass, slot)
                knownFilenames.add(file.name)
                val slotLabel = gunfireVariantLabel(tClass, slot)
                records.add(
                    CustomAssetRecord(
                        id = "fire_${shortTag}_slot_${slot}",
                        type = "SOUND",
                        name = "${towerClassLabel(tClass)} Fire #$slot: $slotLabel",
                        category = "TURRET_FIRE",
                        towerClassOrdinal = classIdx,
                        slotIndex = slot,
                        filePath = file.absolutePath,
                        isEquipped = (equippedFireSlots.getOrElse(classIdx) { 1 } == slot)
                    )
                )
            }
        }

        // 4. 20 Tower Upgrade Variants (4 Classes x 5 Skin Slots)
        classes.forEachIndexed { classIdx, tClass ->
            for (slot in 1..5) {
                val file = getUpgradeFile(tClass, slot)
                knownFilenames.add(file.name)
                val skinName = TurretSkinSlot.fromSlot(slot).displayName
                records.add(
                    CustomAssetRecord(
                        id = "upgrade_${tClass.name.lowercase()}_slot_${slot}",
                        type = "SOUND",
                        name = "${towerClassLabel(tClass)} Upgrade #$slot ($skinName)",
                        category = "TURRET_UPGRADE",
                        towerClassOrdinal = classIdx,
                        slotIndex = slot,
                        filePath = file.absolutePath,
                        isEquipped = (equippedUpgradeSlots.getOrElse(classIdx) { 1 } == slot)
                    )
                )
            }
        }

        // 5. Legacy / Archived WAV files discovered on disk (previous versions, other profiles, etc.)
        try {
            archiveDir.listFiles { f -> f.isFile && f.name.endsWith(".wav", ignoreCase = true) }
                ?.sortedBy { it.name }
                ?.forEach { wavFile ->
                    if (wavFile.name !in knownFilenames) {
                        val cleanId = "archive_${wavFile.nameWithoutExtension.lowercase()}"
                        records.add(
                            CustomAssetRecord(
                                id = cleanId,
                                type = "SOUND",
                                name = "Archived: ${wavFile.nameWithoutExtension}",
                                category = "ARCHIVED_WAV",
                                filePath = wavFile.absolutePath
                            )
                        )
                    }
                }
        } catch (_: Exception) {}

        // 6. 5 Turret Visual Skins (aligned with TurretSkinSlot)
        TurretSkinSlot.entries.forEach { skin ->
            records.add(
                CustomAssetRecord(
                    id = "skin_slot_${skin.slot}",
                    type = "SKIN",
                    name = skin.displayName,
                    category = "TURRET_SKIN",
                    slotIndex = skin.slot,
                    unlockScore = (skin.slot - 1) * 5000,
                    metadata = skin.description,
                    isEquipped = (equippedTurretSkinSlot == skin.slot)
                )
            )
        }

        // 7. 5 Map & Lane Themes (aligned with MapThemeSlot)
        MapThemeSlot.entries.forEach { theme ->
            records.add(
                CustomAssetRecord(
                    id = "theme_slot_${theme.slot}",
                    type = "SKIN",
                    name = theme.displayName,
                    category = "MAP_THEME",
                    slotIndex = theme.slot,
                    unlockScore = (theme.slot - 1) * 5000,
                    metadata = theme.description,
                    isEquipped = (equippedMapThemeSlot == theme.slot)
                )
            )
        }

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
                val id = obj.optString("id", "")
                if (id.isBlank()) continue
                val heart = obj.optBoolean("heart", false)
                val vote = obj.optInt("voteState", if (heart) 1 else 0).coerceIn(-1, 1)
                val star = obj.optBoolean("star", false)
                val updatedAt = obj.optLong("updatedAtEpochMs", 0L)
                if (vote != 0 || star || updatedAt > 0L) {
                    result[id] = PersistedAssetRating(vote, star, updatedAt)
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
        try {
            File(context.filesDir, "exported_skins_sounds_library.json").writeText(jsonStr)
        } catch (_: Exception) {}
        return jsonStr
    }

    private fun towerClassShortTag(towerClass: TowerClass): String = when (towerClass) {
        TowerClass.KINETIC_BALLISTIC -> "kinetic"
        TowerClass.ELEMENTAL_VOID -> "elemental"
        TowerClass.HEAVY_SIEGE -> "siege"
        TowerClass.STELLAR_COSMIC -> "stellar"
    }

    private fun towerClassLabel(towerClass: TowerClass): String = when (towerClass) {
        TowerClass.KINETIC_BALLISTIC -> "Kinetic"
        TowerClass.ELEMENTAL_VOID -> "Elemental"
        TowerClass.HEAVY_SIEGE -> "Siege"
        TowerClass.STELLAR_COSMIC -> "Stellar"
    }

    private fun gunfireVariantLabel(towerClass: TowerClass, slot: Int): String {
        val s = TurretSfxSlot.fromSlot(slot)
        return when (towerClass) {
            TowerClass.KINETIC_BALLISTIC -> s.kineticName
            TowerClass.ELEMENTAL_VOID -> s.elementalName
            TowerClass.HEAVY_SIEGE -> s.siegeName
            TowerClass.STELLAR_COSMIC -> when (slot) {
                1 -> "Starlight Lance (Default)"
                2 -> "Nebula Pulse"
                3 -> "Solar Flare"
                4 -> "Pulsar Beam"
                else -> "Supernova Burst"
            }
        }
    }
}
