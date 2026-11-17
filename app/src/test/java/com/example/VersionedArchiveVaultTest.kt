package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ArchivedAudioSynthesizer
import com.example.audio.BuildArchiveCatalog
import com.example.audio.CustomAssetRecord
import com.example.audio.MusicTrack
import com.example.audio.SoundArchiveStore
import com.example.audio.SoundManager
import com.example.audio.SoundProfile
import com.example.engine.TelemetryManager
import com.example.engine.coordinator.AssetVaultCoordinator
import com.example.engine.network.AssetVoteSyncManager
import com.example.engine.network.AuthRepository
import com.example.model.ArchiveSection
import com.example.model.TowerDrawStyle
import com.example.ui.GameUIState
import com.example.ui.menu.AssetVaultDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VersionedArchiveVaultTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        File(context.filesDir, "exported_skins_sounds_library.json").delete()
        File(context.filesDir, "sound_archive").deleteRecursively()
        context.cacheDir.listFiles { f -> f.name.endsWith(".wav") }?.forEach { it.delete() }
        context.getSharedPreferences("rogue_td_telemetry", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `Default Core stays untouched while current buildCode automatically generates 4 new variants across all categories`() = runBlocking {
        val sm = SoundManager(context)
        sm.awaitPendingIoForTest()
        val records = sm.assetLibraryFlow.value
        val currentBuild = BuildConfig.VERSION_CODE

        // Default Slot 1 / Core items are preserved untouched
        assertTrue(records.first { it.id == "default_skin_1" }.isDefault)
        assertTrue(records.first { it.id == "default_theme_1" }.isDefault)
        assertTrue(records.first { it.id == "default_bgm_cyber_ambient" }.isDefault)
        assertTrue(records.first { it.id == "default_sfx_pack_cyber_synth" }.isDefault)
        assertTrue(records.first { it.id == "default_fire_kinetic" }.isDefault)
        assertTrue(records.first { it.id == "default_upgrade_kinetic_ballistic" }.isDefault)

        // Current build automatically adds 4 new active variants per category
        val activeCurrent = records.filter { !it.isArchived && !it.isDefault && it.buildCode == currentBuild }
        assertEquals(16, activeCurrent.count { it.category == "TURRET_FIRE" })
        assertEquals(16, activeCurrent.count { it.category == "TURRET_UPGRADE" })
        assertEquals(4, activeCurrent.count { it.category == "BACKGROUND_MUSIC" })
        assertEquals(4, activeCurrent.count { it.category == "SFX" })
        assertEquals(4, activeCurrent.count { it.category == "TURRET_SKIN" })
        assertEquals(4, activeCurrent.count { it.category == "MAP_THEME" })

        // Prior builds (60 until currentBuild) are automatically archived without duplicates
        val archivedBuilds = records.filter { it.isArchived && it.buildCode >= 60 }.map { it.buildCode }.toSet()
        for (priorBuild in 60 until currentBuild) {
            assertTrue("Prior build $priorBuild must be auto-archived", archivedBuilds.contains(priorBuild))
        }

        sm.release()
        sm.awaitPendingIoForTest()
    }

    @Test
    fun `Build 60 snapshot and Pre-Build 60 legacy presets and WAVs appear in Archives with build tags`() = runBlocking {
        val archiveDir = File(context.filesDir, "sound_archive").apply { mkdirs() }
        File(archiveDir, "legacy_cannon_fire.wav").writeBytes(ByteArray(128) { 2 })
        File(archiveDir, "old_upgrade_chime.wav").writeBytes(ByteArray(128) { 3 })
        File(archiveDir, "menu_synth_bgm.wav").writeBytes(ByteArray(128) { 4 })

        val sm = SoundManager(context)
        sm.awaitPendingIoForTest()
        val archived = sm.assetLibraryFlow.value.filter { it.isArchived }

        // Build #60 snapshot covers all 5 sections
        val b60 = archived.filter { it.buildCode == 60 }
        assertTrue(b60.all { it.buildLabel == "Build #60 (v0.0.1.039)" })
        assertEquals(20, b60.count { it.archiveSection == ArchiveSection.TURRET_FIRE.id })
        assertEquals(20, b60.count { it.archiveSection == ArchiveSection.TURRET_UPGRADE.id })
        assertEquals(5, b60.count { it.archiveSection == ArchiveSection.TURRET_SKIN.id })
        assertEquals(5, b60.count { it.archiveSection == ArchiveSection.MAP_THEME.id })
        assertTrue(b60.count { it.archiveSection == ArchiveSection.BGM_SFX.id } >= 10)

        // Pre-Build #60 preserves legacy SkinThemes + unrecognized on-disk WAVs with inferred sections
        val pre60 = archived.filter { it.buildCode == 0 }
        assertTrue(pre60.all { it.buildLabel == "Pre-Build #60" })
        assertTrue(pre60.any { it.id == "pre60_skin_retro_arcade" && it.archiveSection == ArchiveSection.TURRET_SKIN.id })
        assertTrue(pre60.any { it.id == "pre60_theme_retro_arcade" && it.archiveSection == ArchiveSection.MAP_THEME.id })
        assertTrue(pre60.any { it.id == "pre60_wav_legacy_cannon_fire" && it.archiveSection == ArchiveSection.TURRET_FIRE.id })
        assertTrue(pre60.any { it.id == "pre60_wav_old_upgrade_chime" && it.archiveSection == ArchiveSection.TURRET_UPGRADE.id })
        assertTrue(pre60.any { it.id == "pre60_wav_menu_synth_bgm" && it.archiveSection == ArchiveSection.BGM_SFX.id })

        sm.release()
        sm.awaitPendingIoForTest()
    }

    @Test
    fun `Legacy v0 0 1 039 JSON votes migrate cleanly to immutable Build 60 and Default IDs without collision`() = runBlocking {
        val legacyJson = JSONObject().put(
            "assets",
            JSONArray()
                .put(JSONObject().put("id", "skin_slot_1").put("voteState", 1).put("star", true).put("updatedAtEpochMs", 500L))
                .put(JSONObject().put("id", "skin_slot_4").put("voteState", 1).put("star", true).put("updatedAtEpochMs", 600L))
                .put(JSONObject().put("id", "fire_kinetic_slot_2").put("voteState", -1).put("star", false).put("updatedAtEpochMs", 700L))
                .put(JSONObject().put("id", "theme_slot_3").put("voteState", 1).put("star", false).put("updatedAtEpochMs", 800L))
                .put(JSONObject().put("id", "bgm_neon_overdrive").put("voteState", 1).put("star", true).put("updatedAtEpochMs", 900L))
        )
        File(context.filesDir, "exported_skins_sounds_library.json").writeText(legacyJson.toString())

        val sm = SoundManager(context)
        sm.awaitPendingIoForTest()
        val byId = sm.assetLibraryFlow.value.associateBy { it.id }

        assertEquals(1, byId["default_skin_1"]?.voteState)
        assertTrue(byId["default_skin_1"]?.isFavoriteStar == true)
        assertEquals(1, byId["b60_skin_4"]?.voteState)
        assertTrue(byId["b60_skin_4"]?.isFavoriteStar == true)
        assertEquals(-1, byId["b60_fire_kinetic_2"]?.voteState)
        assertEquals(1, byId["b60_theme_3"]?.voteState)
        assertEquals(1, byId["b60_bgm_neon_overdrive"]?.voteState)

        // Build #61 variants under same slots must stay neutral (no cross-build vote collision)
        assertEquals(0, byId["b61_skin_4"]?.voteState)
        assertFalse(byId["b61_skin_4"]?.isFavoriteStar == true)
        assertEquals(0, byId["b61_fire_kinetic_2"]?.voteState)
        assertEquals(0, byId["b61_theme_3"]?.voteState)

        sm.release()
        sm.awaitPendingIoForTest()
    }

    @Test
    fun `Equipping archived assets persists immutable IDs across TelemetryManager and resolves visuals`() = runBlocking {
        val sm = SoundManager(context)
        sm.initLoadJob?.join()
        val tm1 = TelemetryManager(context)
        val authRepo = AuthRepository(context = context, auth = null, apiKey = "")
        val syncManager = AssetVoteSyncManager(context, authRepo)
        val stateFlow = MutableStateFlow(GameUIState())
        val coordinator = AssetVaultCoordinator(stateFlow, sm, tm1, syncManager, testScope)

        coordinator.equipAsset("b60_fire_kinetic_3")
        coordinator.equipAsset("pre60_skin_retro_arcade")
        coordinator.equipAsset("b60_theme_4")
        coordinator.equipAsset("b60_bgm_deep_space")

        val st = stateFlow.value
        assertEquals("b60_fire_kinetic_3", st.kineticFireId)
        assertEquals("pre60_skin_retro_arcade", st.turretSkinId)
        assertEquals("b60_theme_4", st.mapThemeId)
        assertEquals("b60_bgm_deep_space", st.bgmTrackId)
        assertEquals(MusicTrack.DEEP_SPACE, st.musicTrack)

        // Reload TelemetryManager from SharedPreferences and verify all immutable IDs survived
        val tm2 = TelemetryManager(context)
        val reloaded = tm2.getBalanceConfig()
        assertEquals("b60_fire_kinetic_3", reloaded.kineticFireId)
        assertEquals("pre60_skin_retro_arcade", reloaded.turretSkinId)
        assertEquals("b60_theme_4", reloaded.mapThemeId)
        assertEquals("b60_bgm_deep_space", reloaded.bgmTrackId)

        // Verify data-driven skin & map theme resolvers support archived IDs accurately
        val resolvedSkin = BuildArchiveCatalog.resolveTurretSkin(reloaded.turretSkinId, reloaded.turretSkinSlot)
        assertEquals("pre60_skin_retro_arcade", resolvedSkin.id)
        assertEquals(TowerDrawStyle.PIXEL_BLOCK, resolvedSkin.drawStyle)

        val resolvedTheme = BuildArchiveCatalog.resolveMapTheme(reloaded.mapThemeId, reloaded.mapThemeSlot)
        assertEquals("b60_theme_4", resolvedTheme.id)
        assertEquals(0xFF090514L, resolvedTheme.groundColor)

        sm.release()
        sm.awaitPendingIoForTest()
    }

    @Test
    fun `AssetVoteSyncManager payload scales beyond 200 entries up to 1500 with buildCode`() {
        val authRepo = AuthRepository(context = context, auth = null, apiKey = "")
        val syncManager = AssetVoteSyncManager(context, authRepo, projectId = "p", apiKey = "k")
        val records = (1..260).map { i ->
            CustomAssetRecord(
                id = "b${60 + (i % 3)}_asset_$i",
                type = "SOUND",
                name = "Variant $i",
                category = "ARCHIVE",
                voteState = if (i % 2 == 0) 1 else -1,
                isFavoriteStar = (i % 5 == 0),
                buildCode = if (i % 3 == 0) 0 else 60 + (i % 2),
                archiveSection = ArchiveSection.TURRET_FIRE.id,
                isArchived = true,
                updatedAtEpochMs = 1_000L + i
            )
        }

        val payload = syncManager.buildVotePayload("uid_scale", records, 50_000L)
        @Suppress("UNCHECKED_CAST")
        val votes = payload["votes"] as Map<String, Map<String, Any>>
        assertEquals(260, votes.size)
        assertEquals(61, votes["b61_asset_1"]?.get("buildCode"))

        val docJson = syncManager.buildFirestoreDocumentJson(payload)
        val firstEntry = docJson.getJSONObject("fields")
            .getJSONObject("votes").getJSONObject("mapValue")
            .getJSONObject("fields").getJSONObject("b61_asset_1")
            .getJSONObject("mapValue").getJSONObject("fields")
        assertEquals("61", firstEntry.getJSONObject("buildCode").getString("integerValue"))
    }

    @Test
    fun `AssetVaultDialog renders Archives tab, 6 section filter chips, and build badges`() = runBlocking {
        val sm = SoundManager(context)
        sm.awaitPendingIoForTest()
        val assets = sm.assetLibraryFlow.value

        composeRule.setContent {
            AssetVaultDialog(
                assets = assets,
                syncStatusText = "Ratings auto-saved locally",
                onPreviewAsset = {},
                onStopPreview = {},
                onLikeAsset = {},
                onDislikeAsset = {},
                onStarAsset = {},
                onEquipAsset = {},
                onCloudSync = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithTag("vault_tab_ARCHIVES").performScrollTo().assertIsDisplayed().performClick()
        ArchiveSection.entries.forEach { sec ->
            composeRule.onNodeWithTag("archive_section_${sec.id}").performScrollTo().assertIsDisplayed()
        }

        composeRule.onNodeWithTag("archive_section_${ArchiveSection.TURRET_SKIN.id}").performScrollTo().performClick()
        assertTrue(composeRule.onAllNodesWithText("Build #60 (v0.0.1.039)").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithTag("asset_vault_list")
            .performScrollToNode(hasText("Retro Arcade (Legacy Skin)"))
        composeRule.onNodeWithText("Retro Arcade (Legacy Skin)").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Pre-Build #60").fetchSemanticsNodes().isNotEmpty())

        composeRule.onNodeWithTag("archive_section_${ArchiveSection.TURRET_FIRE.id}").performScrollTo().performClick()
        composeRule.onNodeWithTag("asset_vault_list")
            .performScrollToNode(hasTestTag("vault_use_b60_fire_kinetic_2"))
        composeRule.onNodeWithTag("vault_play_b60_fire_kinetic_2").assertIsDisplayed()
        composeRule.onNodeWithTag("vault_use_b60_fire_kinetic_2").assertIsDisplayed()

        sm.release()
        sm.awaitPendingIoForTest()
    }

    @Test
    fun `Simulating consecutive build increments 62 63 64 generates 4 distinct skins themes and audio per build and archives prior builds`() {
        val store = SoundArchiveStore(context)
        val libs = (62..64).associateWith { b ->
            store.buildAssetLibrary(
                activeProfile = SoundProfile.CYBER_SYNTH,
                equippedBgmId = "default_bgm_rolling_playlist",
                equippedSfxPackId = "default_sfx_pack_cyber_synth",
                equippedFireIds = Array(4) { BuildArchiveCatalog.buildActiveFireId(BuildArchiveCatalog.allClasses[it], 1, b) },
                equippedUpgradeIds = Array(4) { BuildArchiveCatalog.buildActiveUpgradeId(BuildArchiveCatalog.allClasses[it], 1, b) },
                equippedTurretSkinId = "default_skin_1",
                equippedMapThemeId = "default_theme_1",
                targetBuildCode = b
            )
        }

        // Each build b generates 4 new distinct active variants per category with unique IDs and names
        for (b in 62..64) {
            val active = libs[b]!!.filter { !it.isArchived && !it.isDefault && it.buildCode == b }
            assertEquals(16, active.count { it.category == "TURRET_FIRE" })
            assertEquals(16, active.count { it.category == "TURRET_UPGRADE" })
            assertEquals(4, active.count { it.category == "BACKGROUND_MUSIC" })
            assertEquals(4, active.count { it.category == "SFX" })
            assertEquals(4, active.count { it.category == "TURRET_SKIN" })
            assertEquals(4, active.count { it.category == "MAP_THEME" })
            assertTrue(active.all { it.id.startsWith("b${b}_") })

            // Every prior build (60 until b) must be archived
            val archived = libs[b]!!.filter { it.isArchived }
            for (prior in 60 until b) {
                assertTrue("Build $prior skins must be archived in Build $b", archived.any { it.buildCode == prior && it.archiveSection == ArchiveSection.TURRET_SKIN.id })
                assertTrue("Build $prior fire audio must be archived in Build $b", archived.any { it.buildCode == prior && it.archiveSection == ArchiveSection.TURRET_FIRE.id })
            }
            // Build #61 curated Crimson Vanguard & Orbital Surge must be preserved in Archives
            assertTrue(archived.any { it.id == "b61_skin_2" && it.name == "Crimson Vanguard" })
            assertTrue(archived.any { it.id == "b61_bgm_orbital_surge" && it.name.contains("Orbital Surge") })
        }

        // Consecutive builds must produce distinct skin palettes/names and distinct synthesized PCM waveforms
        val skin62 = BuildArchiveCatalog.resolveTurretSkin("b62_skin_2", 2)
        val skin63 = BuildArchiveCatalog.resolveTurretSkin("b63_skin_2", 2)
        val skin64 = BuildArchiveCatalog.resolveTurretSkin("b64_skin_2", 2)
        assertNotEquals(skin62.displayName, skin63.displayName)
        assertNotEquals(skin63.displayName, skin64.displayName)
        assertNotEquals(skin62.frameColor, skin63.frameColor)
        assertNotEquals(skin63.frameColor, skin64.frameColor)

        val pcm62 = ArchivedAudioSynthesizer.synthesizeByAssetId("b62_fire_kinetic_2")!!
        val pcm63 = ArchivedAudioSynthesizer.synthesizeByAssetId("b63_fire_kinetic_2")!!
        assertFalse("Consecutive builds must synthesize distinct firing PCM waveforms", pcm62.contentEquals(pcm63))
    }

    @Test
    fun `Archived Build 61 and Build 62 assets remain equippable and resolve distinct audio and visuals`() = runBlocking {
        val sm = SoundManager(context)
        sm.awaitPendingIoForTest()
        val tm1 = TelemetryManager(context)
        val authRepo = AuthRepository(context = context, auth = null, apiKey = "")
        val syncManager = AssetVoteSyncManager(context, authRepo)
        val stateFlow = MutableStateFlow(GameUIState())
        val coordinator = AssetVaultCoordinator(stateFlow, sm, tm1, syncManager, testScope)

        coordinator.equipAsset("b61_fire_kinetic_2")
        coordinator.equipAsset("b61_skin_2")
        coordinator.equipAsset("b62_theme_3")
        coordinator.equipAsset("b61_bgm_orbital_surge")

        val st = stateFlow.value
        assertEquals("b61_fire_kinetic_2", st.kineticFireId)
        assertEquals("b61_skin_2", st.turretSkinId)
        assertEquals("b62_theme_3", st.mapThemeId)
        assertEquals("b61_bgm_orbital_surge", st.bgmTrackId)

        val resolvedSkin = BuildArchiveCatalog.resolveTurretSkin(st.turretSkinId, st.turretSkinSlot)
        assertEquals("Crimson Vanguard", resolvedSkin.displayName)
        assertEquals(61, resolvedSkin.buildCode)

        val resolvedTheme = BuildArchiveCatalog.resolveMapTheme(st.mapThemeId, st.mapThemeSlot)
        assertEquals("b62_theme_3", resolvedTheme.id)
        assertEquals(62, resolvedTheme.buildCode)

        // Distinct WAV synthesis works cleanly for archived Build #61 & Build #62 IDs
        val archiveStore = SoundArchiveStore(context)
        val wav61 = archiveStore.getWavFileForAsset("b61_fire_kinetic_2")
        val wav62 = archiveStore.getWavFileForAsset("b62_fire_kinetic_2")
        assertNotNull(wav61)
        assertNotNull(wav62)
        assertNotEquals(wav61!!.name, wav62!!.name)

        sm.release()
        sm.awaitPendingIoForTest()
    }
}
