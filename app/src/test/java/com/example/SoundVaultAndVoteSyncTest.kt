package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.MusicTrack
import com.example.audio.SoundManager
import com.example.engine.TelemetryManager
import com.example.engine.coordinator.AssetVaultCoordinator
import com.example.engine.network.AssetVoteSyncManager
import com.example.engine.network.AuthRepository
import com.example.model.TowerClass
import com.example.ui.GameUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoundVaultAndVoteSyncTest {

    private lateinit var context: Context
    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        File(context.filesDir, "exported_skins_sounds_library.json").delete()
        File(context.filesDir, "sound_archive").deleteRecursively()
        context.cacheDir.listFiles { f -> f.name.endsWith(".wav") }?.forEach { it.delete() }
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `SoundManager registers all 40 turret variants, BGM, SFX, 5 skins, and 5 map themes in archive`() {
        val sm = SoundManager(context)
        val records = sm.assetLibraryFlow.value

        assertEquals(20, records.count { it.category == "TURRET_FIRE" })
        assertEquals(20, records.count { it.category == "TURRET_UPGRADE" })
        assertEquals(5, records.count { it.category == "TURRET_SKIN" })
        assertEquals(5, records.count { it.category == "MAP_THEME" })
        assertTrue(records.any { it.category == "TURRET_FIRE" && it.towerClassOrdinal == TowerClass.STELLAR_COSMIC.ordinal })
        assertTrue(records.any { it.category == "TURRET_UPGRADE" && it.towerClassOrdinal == TowerClass.STELLAR_COSMIC.ordinal })
        assertEquals(File(context.filesDir, "sound_archive").absolutePath, sm.getArchiveDirectory().absolutePath)
        assertTrue(File(sm.getArchiveDirectory(), "sfx_gunfire_stellar_slot1_v1.wav").exists())

        sm.release()
    }

    @Test
    fun `Legacy cacheDir WAVs migrate asynchronously to sound_archive and emit to StateFlow`() = runBlocking {
        val legacyWav = File(context.cacheDir, "sfx_gunfire_kinetic_slot1_v1.wav")
        legacyWav.writeBytes(ByteArray(128) { 1 })

        val sm = SoundManager(context)
        sm.migrationJob?.join()

        val archivedFile = File(sm.getArchiveDirectory(), "sfx_gunfire_kinetic_slot1_v1.wav")
        assertTrue("Legacy WAV must be copied to filesDir/sound_archive", archivedFile.exists())

        val records = sm.assetLibraryFlow.value
        assertTrue(
            "Migrated legacy WAV must surface in assetLibraryFlow as ARCHIVED_WAV without app restart",
            records.any { it.category == "ARCHIVED_WAV" && it.filePath == archivedFile.absolutePath }
        )

        sm.release()
    }

    @Test
    fun `Tri-state Like Dislike Neutral and Star persist across SoundManager restarts`() {
        val sm1 = SoundManager(context)
        val targetId = "fire_stellar_slot_3"
        val skinId = "skin_slot_4"

        assertEquals(1, sm1.setAssetVote(targetId, 1))
        assertTrue(sm1.assetLibraryFlow.value.first { it.id == targetId }.isFavoriteHeart)

        // Tapping Like again resets to Neutral (0)
        assertEquals(0, sm1.setAssetVote(targetId, 1))
        assertFalse(sm1.assetLibraryFlow.value.first { it.id == targetId }.isFavoriteHeart)

        // Set Dislike (-1) and Star
        assertEquals(-1, sm1.setAssetVote(targetId, -1))
        assertTrue(sm1.toggleAssetFavoriteStar(skinId))
        sm1.release()

        // New SoundManager instance must restore persisted states automatically
        val sm2 = SoundManager(context)
        val restoredFire = sm2.assetLibraryFlow.value.first { it.id == targetId }
        val restoredSkin = sm2.assetLibraryFlow.value.first { it.id == skinId }
        assertEquals(-1, restoredFire.voteState)
        assertFalse(restoredFire.isFavoriteHeart)
        assertTrue(restoredSkin.isFavoriteStar)
        sm2.release()
    }

    @Test
    fun `AssetVoteSyncManager drops neutralized votes from payload and guards Spark cooldown`() {
        val sm = SoundManager(context)
        val authRepo = AuthRepository(context = context, auth = null, apiKey = "")
        val syncManager = AssetVoteSyncManager(context, authRepo)

        sm.setAssetVote("fire_kinetic_slot_2", 1)
        sm.setAssetVote("theme_slot_3", -1)

        val payload1 = syncManager.buildVotePayload("uid_123", sm.assetLibraryFlow.value, 1000L)
        @Suppress("UNCHECKED_CAST")
        val votes1 = payload1["votes"] as Map<String, Any>
        assertTrue(votes1.containsKey("fire_kinetic_slot_2"))
        assertTrue(votes1.containsKey("theme_slot_3"))

        // Reset fire_kinetic_slot_2 back to Neutral (0)
        sm.setAssetVote("fire_kinetic_slot_2", 1)
        val payload2 = syncManager.buildVotePayload("uid_123", sm.assetLibraryFlow.value, 2000L)
        @Suppress("UNCHECKED_CAST")
        val votes2 = payload2["votes"] as Map<String, Any>
        assertFalse("Neutralized vote must be absent from full-overwrite payload", votes2.containsKey("fire_kinetic_slot_2"))
        assertTrue(votes2.containsKey("theme_slot_3"))

        // First sync triggers cooldown; immediate second sync (force = false) is blocked by Spark guard
        syncManager.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true)
        var secondResultMsg = ""
        syncManager.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = false) { ok, msg ->
            assertFalse(ok)
            secondResultMsg = msg
        }
        assertTrue(secondResultMsg.contains("cooldown", ignoreCase = true))

        sm.release()
    }

    @Test
    fun `AssetVaultCoordinator equipAsset syncs GameUIState, TelemetryManager, and SoundManager`() {
        val sm = SoundManager(context)
        val tm = TelemetryManager(context)
        val authRepo = AuthRepository(context = context, auth = null, apiKey = "")
        val syncManager = AssetVoteSyncManager(context, authRepo)
        val stateFlow = MutableStateFlow(GameUIState())
        val coordinator = AssetVaultCoordinator(stateFlow, sm, tm, syncManager, testScope)

        coordinator.equipAsset("fire_stellar_slot_4")
        coordinator.equipAsset("skin_slot_5")
        coordinator.equipAsset("theme_slot_2")
        coordinator.equipAsset("bgm_neon_overdrive")

        val state = stateFlow.value
        assertEquals(4, state.stellarSfxSlot)
        assertEquals(5, state.turretSkinSlot)
        assertEquals(2, state.mapThemeSlot)
        assertEquals(MusicTrack.NEON_OVERDRIVE, state.musicTrack)

        val records = sm.assetLibraryFlow.value
        assertTrue(records.first { it.id == "fire_stellar_slot_4" }.isEquipped)
        assertTrue(records.first { it.id == "skin_slot_5" }.isEquipped)
        assertTrue(records.first { it.id == "theme_slot_2" }.isEquipped)
        assertTrue(records.first { it.id == "bgm_neon_overdrive" }.isEquipped)

        // Verify TelemetryManager persisted slots
        val reloaded = tm.getBalanceConfig()
        assertEquals(4, reloaded.stellarSfxSlot)
        assertEquals(5, reloaded.turretSkinSlot)
        assertEquals(2, reloaded.mapThemeSlot)

        // Verify previewAsset executes safely across categories
        coordinator.previewAsset("fire_stellar_slot_4")
        coordinator.previewAsset("upgrade_stellar_cosmic_slot_2")
        coordinator.previewAsset("sfx_place")
        coordinator.stopPreview()

        sm.release()
    }
}
