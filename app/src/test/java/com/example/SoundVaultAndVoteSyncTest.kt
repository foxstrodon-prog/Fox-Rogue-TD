package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.audio.MusicTrack
import com.example.audio.SoundManager
import com.example.engine.TelemetryManager
import com.example.engine.coordinator.AssetVaultCoordinator
import com.example.engine.network.AssetVoteSyncManager
import com.example.engine.network.AuthRepository
import com.example.model.TowerClass
import com.example.model.TurretSfxSlot
import com.example.ui.GameUIState
import com.example.ui.RogueTDViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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
        context.getSharedPreferences("rogue_td_auth", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("asset_vote_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
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
        val mockHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                val body = when {
                    url.contains("identitytoolkit.googleapis.com") -> JSONObject()
                        .put("localId", "uid_cooldown")
                        .put("idToken", "token_cooldown")
                        .put("refreshToken", "refresh_cooldown")
                        .put("expiresIn", "3600")
                        .toString()
                    url.contains("firestore.googleapis.com") -> "{}"
                    else -> error("Unexpected URL: $url")
                }
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody())
                    .build()
            }
            .build()
        val authRepo = AuthRepository(context = context, auth = null, httpClient = mockHttp, apiKey = "test_api_key_123")
        val syncManager = AssetVoteSyncManager(
            context = context,
            authRepository = authRepo,
            httpClient = mockHttp,
            projectId = "test-project",
            apiKey = "test_api_key_123"
        )

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

        // First sync completes a Firestore request and triggers cooldown
        val firstSyncDone = CountDownLatch(1)
        syncManager.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true) { ok, _ ->
            assertTrue(ok)
            firstSyncDone.countDown()
        }
        val deadline = System.currentTimeMillis() + 5_000L
        while (firstSyncDone.count > 0 && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertEquals(0L, firstSyncDone.count)

        // Immediate second sync (force = false) is blocked by Spark guard while preserving pending status
        var secondResultMsg = ""
        syncManager.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = false) { ok, msg ->
            assertFalse(ok)
            secondResultMsg = msg
        }
        assertTrue(secondResultMsg.contains("cooldown", ignoreCase = true))
        assertTrue(secondResultMsg.contains("Pending cloud sync", ignoreCase = true))

        sm.release()
    }

    @Test
    fun `AssetVoteSyncManager skips cooldown on unconfigured or offline failure and deduplicates concurrent syncs`() {
        val sm = SoundManager(context)
        sm.setAssetVote("fire_kinetic_slot_2", 1)

        // 1. Unconfigured failure must NOT trigger cooldown
        val unconfiguredAuth = AuthRepository(context = context, auth = null, apiKey = "")
        val unconfiguredSync = AssetVoteSyncManager(
            context = context,
            authRepository = unconfiguredAuth,
            projectId = "",
            apiKey = ""
        )
        val unconfiguredFirst = AtomicReference<Pair<Boolean, String>?>(null)
        unconfiguredSync.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true) { ok, msg ->
            unconfiguredFirst.set(ok to msg)
        }
        val deadline1 = System.currentTimeMillis() + 5_000L
        while (unconfiguredFirst.get() == null && System.currentTimeMillis() < deadline1) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertFalse(unconfiguredFirst.get()!!.first)

        val unconfiguredSecond = AtomicReference<Pair<Boolean, String>?>(null)
        unconfiguredSync.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = false) { ok, msg ->
            unconfiguredSecond.set(ok to msg)
        }
        val deadline2 = System.currentTimeMillis() + 5_000L
        while (unconfiguredSecond.get() == null && System.currentTimeMillis() < deadline2) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertFalse(
            "Unconfigured failure must not activate cooldown: ${unconfiguredSecond.get()?.second}",
            unconfiguredSecond.get()!!.second.contains("cooldown", ignoreCase = true)
        )

        // 2. Offline failure (UnknownHostException) must NOT trigger cooldown
        val offlineClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                if (url.contains("identitytoolkit.googleapis.com")) {
                    val respJson = JSONObject()
                        .put("localId", "offline_uid")
                        .put("idToken", "offline_token")
                        .put("refreshToken", "offline_refresh")
                        .put("expiresIn", "3600")
                        .toString()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(respJson.toResponseBody())
                        .build()
                } else {
                    throw UnknownHostException("offline")
                }
            }
            .build()
        val offlineAuth = AuthRepository(context = context, auth = null, httpClient = offlineClient, apiKey = "test_key")
        val offlineSync = AssetVoteSyncManager(
            context = context,
            authRepository = offlineAuth,
            httpClient = offlineClient,
            projectId = "test-project",
            apiKey = "test_key"
        )
        val offlineFirst = AtomicReference<Pair<Boolean, String>?>(null)
        offlineSync.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true) { ok, msg ->
            offlineFirst.set(ok to msg)
        }
        val deadline3 = System.currentTimeMillis() + 5_000L
        while (offlineFirst.get() == null && System.currentTimeMillis() < deadline3) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(offlineFirst.get()!!.second.contains("Offline", ignoreCase = true))

        val offlineSecond = AtomicReference<Pair<Boolean, String>?>(null)
        offlineSync.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = false) { ok, msg ->
            offlineSecond.set(ok to msg)
        }
        val deadline4 = System.currentTimeMillis() + 5_000L
        while (offlineSecond.get() == null && System.currentTimeMillis() < deadline4) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertFalse(
            "Offline failure must not activate cooldown: ${offlineSecond.get()?.second}",
            offlineSecond.get()!!.second.contains("cooldown", ignoreCase = true)
        )

        // 3. Concurrent sync calls must be deduplicated via in-flight AtomicBoolean
        val patchEntered = CountDownLatch(1)
        val allowPatchFinish = CountDownLatch(1)
        val patchCalls = AtomicInteger(0)
        val concurrentClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                if (url.contains("identitytoolkit.googleapis.com")) {
                    val respJson = JSONObject()
                        .put("localId", "conc_uid")
                        .put("idToken", "conc_token")
                        .put("refreshToken", "conc_refresh")
                        .put("expiresIn", "3600")
                        .toString()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(respJson.toResponseBody())
                        .build()
                } else {
                    patchCalls.incrementAndGet()
                    patchEntered.countDown()
                    allowPatchFinish.await(5, TimeUnit.SECONDS)
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{}".toResponseBody())
                        .build()
                }
            }
            .build()
        val concAuth = AuthRepository(context = context, auth = null, httpClient = concurrentClient, apiKey = "test_key")
        val concSync = AssetVoteSyncManager(
            context = context,
            authRepository = concAuth,
            httpClient = concurrentClient,
            projectId = "test-project",
            apiKey = "test_key"
        )
        val firstResult = AtomicReference<Pair<Boolean, String>?>(null)
        concSync.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true) { ok, msg ->
            firstResult.set(ok to msg)
        }
        assertTrue("First sync should reach Firestore PATCH", patchEntered.await(5, TimeUnit.SECONDS))

        var duplicateMsg = ""
        concSync.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true) { ok, msg ->
            assertFalse(ok)
            duplicateMsg = msg
        }
        assertTrue(duplicateMsg.contains("already in progress", ignoreCase = true))
        assertEquals(1, patchCalls.get())

        allowPatchFinish.countDown()
        val deadline5 = System.currentTimeMillis() + 5_000L
        while (firstResult.get() == null && System.currentTimeMillis() < deadline5) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(firstResult.get()!!.first)

        // In-flight guard must be released in finally so a subsequent forced sync executes
        val followUpResult = AtomicReference<Pair<Boolean, String>?>(null)
        concSync.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true) { ok, msg ->
            followUpResult.set(ok to msg)
        }
        val deadline6 = System.currentTimeMillis() + 5_000L
        while (followUpResult.get() == null && System.currentTimeMillis() < deadline6) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(followUpResult.get()!!.first)
        assertEquals(2, patchCalls.get())

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

    @Test
    fun `AssetVoteSyncManager uploads asset_votes over Firestore REST with REST auth fallback`() {
        val sm = SoundManager(context)
        sm.setAssetVote("fire_kinetic_slot_2", 1)
        sm.toggleAssetFavoriteStar("skin_slot_4")

        var capturedMethod = ""
        var capturedUrl = ""
        var capturedAuthHeader = ""
        var capturedBody = ""

        val mockHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                when {
                    url.contains("identitytoolkit.googleapis.com") -> {
                        val respJson = JSONObject()
                            .put("localId", "rest_uid_42")
                            .put("idToken", "rest_token_abc")
                            .put("refreshToken", "rest_refresh_xyz")
                            .put("expiresIn", "3600")
                            .toString()
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(respJson.toResponseBody())
                            .build()
                    }
                    url.contains("firestore.googleapis.com") -> {
                        capturedMethod = req.method
                        capturedUrl = url
                        capturedAuthHeader = req.header("Authorization").orEmpty()
                        val buf = Buffer()
                        req.body?.writeTo(buf)
                        capturedBody = buf.readUtf8()
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("{}".toResponseBody())
                            .build()
                    }
                    else -> error("Unexpected URL: $url")
                }
            }
            .build()

        val authRepo = AuthRepository(
            context = context,
            auth = null,
            httpClient = mockHttp,
            apiKey = "test_api_key_123"
        )
        val syncManager = AssetVoteSyncManager(
            context = context,
            authRepository = authRepo,
            httpClient = mockHttp,
            projectId = "test-project",
            apiKey = "test_api_key_123"
        )

        val resultRef = AtomicReference<Pair<Boolean, String>?>(null)
        syncManager.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true) { ok, msg ->
            resultRef.set(ok to msg)
        }

        val deadline = System.currentTimeMillis() + 5_000L
        while (resultRef.get() == null && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }

        val result = resultRef.get()
        assertNotNull("Expected sync callback", result)
        assertTrue("Expected REST vote sync to succeed: ${result?.second}", result!!.first)
        assertEquals("PATCH", capturedMethod)
        assertTrue(capturedUrl.contains("/databases/(default)/documents/asset_votes/rest_uid_42"))
        assertFalse("Full overwrite PATCH must not include updateMask", capturedUrl.contains("updateMask"))
        assertEquals("Bearer rest_token_abc", capturedAuthHeader)

        val fields = JSONObject(capturedBody).getJSONObject("fields")
        assertEquals("rest_uid_42", fields.getJSONObject("userId").getString("stringValue"))
        assertTrue(fields.has("versionCode"))
        assertTrue(fields.has("syncedAtEpochMs"))
        val voteFields = fields.getJSONObject("votes").getJSONObject("mapValue").getJSONObject("fields")
        assertTrue(voteFields.has("fire_kinetic_slot_2"))
        assertTrue(voteFields.has("skin_slot_4"))

        sm.release()
    }

    @Test
    fun `AssetVoteSyncManager refreshes token on 403 and surfaces PERMISSION_DENIED guidance`() {
        val sm = SoundManager(context)
        sm.setAssetVote("fire_kinetic_slot_2", 1)

        var firestoreCalls = 0
        var refreshCalls = 0

        val mockHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                when {
                    url.contains("identitytoolkit.googleapis.com") -> {
                        val respJson = JSONObject()
                            .put("localId", "rest_uid_99")
                            .put("idToken", "initial_token")
                            .put("refreshToken", "refresh_99")
                            .put("expiresIn", "3600")
                            .toString()
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(respJson.toResponseBody())
                            .build()
                    }
                    url.contains("securetoken.googleapis.com") -> {
                        refreshCalls++
                        val respJson = JSONObject()
                            .put("user_id", "rest_uid_99")
                            .put("id_token", "refreshed_token")
                            .put("refresh_token", "refresh_99")
                            .put("expires_in", "3600")
                            .toString()
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(respJson.toResponseBody())
                            .build()
                    }
                    url.contains("firestore.googleapis.com") -> {
                        firestoreCalls++
                        val errJson = """{"error":{"code":403,"message":"Missing or insufficient permissions.","status":"PERMISSION_DENIED"}}"""
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(403)
                            .message("Forbidden")
                            .body(errJson.toResponseBody())
                            .build()
                    }
                    else -> error("Unexpected URL: $url")
                }
            }
            .build()

        val authRepo = AuthRepository(
            context = context,
            auth = null,
            httpClient = mockHttp,
            apiKey = "test_api_key_123"
        )
        val syncManager = AssetVoteSyncManager(
            context = context,
            authRepository = authRepo,
            httpClient = mockHttp,
            projectId = "test-project",
            apiKey = "test_api_key_123"
        )

        val resultRef = AtomicReference<Pair<Boolean, String>?>(null)
        syncManager.syncVotesToFirestore(testScope, sm.assetLibraryFlow.value, force = true) { ok, msg ->
            resultRef.set(ok to msg)
        }

        val deadline = System.currentTimeMillis() + 5_000L
        while (resultRef.get() == null && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }

        val result = resultRef.get()
        assertNotNull(result)
        assertFalse(result!!.first)
        assertEquals("Expected token refresh on 403", 1, refreshCalls)
        assertEquals("Expected retry after refresh", 2, firestoreCalls)
        assertTrue(result.second.contains("PERMISSION_DENIED"))

        sm.release()
    }

    @Test
    fun `openPauseMenu does not stack over AssetVaultDialog and startNewRun resets isAssetVaultOpen`() {
        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
        )[RogueTDViewModel::class.java]
        try {
            vm.openPauseMenu()
            assertTrue(vm.uiState.value.isPauseMenuOpen)

            vm.openAssetVaultFromPauseMenu()
            assertFalse(vm.uiState.value.isPauseMenuOpen)
            assertTrue(vm.uiState.value.isAssetVaultOpen)

            // Simulating MainActivity.onStop() calling openPauseMenu() while Asset Vault is open
            vm.openPauseMenu()
            assertFalse("PauseMenuDialog must not open over AssetVaultDialog", vm.uiState.value.isPauseMenuOpen)
            assertTrue("AssetVaultDialog must remain open", vm.uiState.value.isAssetVaultOpen)

            // Starting a new run must clear isAssetVaultOpen
            vm.startNewRun()
            assertFalse(vm.uiState.value.isAssetVaultOpen)
            assertFalse(vm.uiState.value.isPauseMenuOpen)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `TurretSfxSlot stellarName matches SoundArchiveStore record labels`() {
        val sm = SoundManager(context)
        val records = sm.assetLibraryFlow.value
        for (slot in 1..5) {
            val expectedName = TurretSfxSlot.fromSlot(slot).stellarName
            val record = records.first { it.id == "fire_stellar_slot_$slot" }
            assertTrue(
                "Record '${record.name}' should end with '$expectedName'",
                record.name.endsWith(expectedName)
            )
        }
        sm.release()
    }
}
