package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.MusicAndPreviewPlayer
import com.example.audio.MusicTrack
import com.example.audio.SoundArchiveStore
import com.example.audio.SoundManager
import com.example.model.TowerClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.io.File
import java.io.IOException

/**
 * Quinn (Quality Engineer) & Charles (Audio Director)
 * Unit Test Suite for Audio Pipeline: volume attenuation, ducking, pause levels, and track switching.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AudioPipelineTest {

    private lateinit var context: Context
    private lateinit var soundManager: SoundManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        soundManager = SoundManager(context)
    }

    @After
    fun tearDown() {
        soundManager.release()
    }

    @Test
    fun `SoundManager clamps all volume channels between 0 and 1`() {
        soundManager.masterVolume = 1.5f
        assertEquals(1.0f, soundManager.masterVolume, 0.001f)

        soundManager.masterVolume = -0.5f
        assertEquals(0.0f, soundManager.masterVolume, 0.001f)

        soundManager.musicVolume = 2.0f
        assertEquals(1.0f, soundManager.musicVolume, 0.001f)

        soundManager.sfxVolume = -1.0f
        assertEquals(0.0f, soundManager.sfxVolume, 0.001f)

        soundManager.upgradeVolume = 1.8f
        assertEquals(1.0f, soundManager.upgradeVolume, 0.001f)
    }

    @Test
    fun `Pause menu attenuation drops background music by exactly 50 percent`() {
        soundManager.masterVolume = 1.0f
        soundManager.musicVolume = 1.0f
        soundManager.isMuted = false
        soundManager.activeMusicTrack = MusicTrack.CYBER_AMBIENT
        soundManager.isPaused = false

        val unpausedVol = soundManager.calculateEffectiveMusicVolume()
        assertEquals(1.0f, unpausedVol, 0.001f)

        // Entering pause menu
        soundManager.isPaused = true
        val pausedVol = soundManager.calculateEffectiveMusicVolume()
        assertEquals("Paused music must be attenuated to 50% (-6dB)", 0.50f, pausedVol, 0.001f)

        // Resuming gameplay
        soundManager.isPaused = false
        val resumedVol = soundManager.calculateEffectiveMusicVolume()
        assertEquals(1.0f, resumedVol, 0.001f)
    }

    @Test
    fun `Combat ducking drops background music by 50 percent during heavy action`() {
        soundManager.masterVolume = 1.0f
        soundManager.musicVolume = 0.8f
        soundManager.isPaused = false
        soundManager.isMuted = false
        soundManager.activeMusicTrack = MusicTrack.CYBER_AMBIENT

        val normalVol = soundManager.calculateEffectiveMusicVolume()
        assertEquals(0.8f, normalVol, 0.001f)

        // Trigger combat ducking
        soundManager.triggerCombatDucking(durationMs = 500L)
        assertTrue(soundManager.isDucked)

        val duckedVol = soundManager.calculateEffectiveMusicVolume()
        assertEquals(0.8f * 0.5f, duckedVol, 0.001f)

        // Advance tick past ducking duration
        soundManager.updateTick(nowMs = System.currentTimeMillis() + 600L)
        assertFalse(soundManager.isDucked)
        assertEquals(0.8f, soundManager.calculateEffectiveMusicVolume(), 0.001f)
    }

    @Test
    fun `Muting or setting track to NONE forces effective music volume to 0`() {
        soundManager.masterVolume = 0.8f
        soundManager.musicVolume = 0.8f
        soundManager.activeMusicTrack = MusicTrack.PULSE_ARPEGGIO

        assertTrue(soundManager.calculateEffectiveMusicVolume() > 0f)

        soundManager.isMuted = true
        assertEquals(0f, soundManager.calculateEffectiveMusicVolume(), 0.001f)

        soundManager.isMuted = false
        soundManager.activeMusicTrack = MusicTrack.NONE
        assertEquals(0f, soundManager.calculateEffectiveMusicVolume(), 0.001f)
    }

    @Test
    fun `Lifecycle suspendAllAudio drops volume to 0 and resumeAudio restores it`() {
        soundManager.masterVolume = 0.8f
        soundManager.musicVolume = 0.8f
        soundManager.activeMusicTrack = MusicTrack.CELESTIAL_BASTION

        soundManager.suspendAllAudio()
        assertTrue(soundManager.isAudioSuspended)
        assertEquals(0f, soundManager.calculateEffectiveMusicVolume(), 0.001f)

        soundManager.resumeAudio()
        assertFalse(soundManager.isAudioSuspended)
        assertTrue(soundManager.calculateEffectiveMusicVolume() > 0f)
    }

    @Test
    fun `Upgrade SFX can be triggered safely across all 4 tower classes and 5 skin slots`() {
        for (cls in listOf(TowerClass.KINETIC_BALLISTIC, TowerClass.ELEMENTAL_VOID, TowerClass.HEAVY_SIEGE, TowerClass.STELLAR_COSMIC)) {
            for (skin in 1..5) {
                // Must execute without throwing exceptions
                soundManager.playTowerUpgrade(cls, skin)
            }
        }
    }

    @Test
    fun `playTransientWavFile handles corrupted wav failure cleanly without throwing and respects zero volume`() {
        val archiveStore = SoundArchiveStore(context)
        var suspended = false
        var masterVol = 0.8f
        var sfxVol = 0.8f
        val player = MusicAndPreviewPlayer(
            archiveStore = archiveStore,
            isEnabled = { true },
            isMuted = { false },
            isSuspended = { suspended },
            isPaused = { false },
            getMasterVolume = { masterVol },
            getSfxVolume = { sfxVol },
            getMusicVolume = { 0.5f },
            getActiveMusicTrack = { MusicTrack.CYBER_AMBIENT }
        )

        val bgmFile = player.getOrCreateMusicFile(MusicTrack.CYBER_AMBIENT)
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(bgmFile.absolutePath), ShadowMediaPlayer.MediaInfo(45_000, 0))
        player.playTrack(MusicTrack.CYBER_AMBIENT)
        assertTrue(player.hasActiveMediaPlayer())

        val badWav = File(context.cacheDir, "corrupted_preview_test.wav").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))
        }
        ShadowMediaPlayer.addException(DataSource.toDataSource(badWav.absolutePath), IOException("Corrupted WAV"))

        // Must catch exception, release candidate MediaPlayer, and run resumeBgmAfterPreview cleanly
        player.playTransientWavFile(badWav)
        assertTrue(player.hasActiveMediaPlayer())
        assertFalse(player.hasActivePreviewPlayer())

        // Invalidated generation or suspended state must be a no-op
        val validWav = player.getOrCreateMusicFile(MusicTrack.PULSE_ARPEGGIO)
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(validWav.absolutePath), ShadowMediaPlayer.MediaInfo(45_000, 0))

        val staleGen = player.nextPreviewGeneration()
        player.stopPreview()
        player.playTransientWavFile(validWav, expectedGeneration = staleGen)
        assertFalse("Stale preview generation must not start preview playback", player.hasActivePreviewPlayer())

        suspended = true
        player.playTransientWavFile(validWav)
        assertFalse("playTransientWavFile must be a no-op while suspended", player.hasActivePreviewPlayer())

        suspended = false
        player.playTransientWavFile(validWav)
        assertTrue("Valid preview should start when active and not suspended", player.hasActivePreviewPlayer())
        player.stopPreview()
        assertFalse(player.hasActivePreviewPlayer())

        // Zero master or zero SFX volume must silence preview MediaPlayer (0f, not 0.15f)
        masterVol = 0f
        sfxVol = 0.8f
        player.playTransientWavFile(validWav)
        val previewAfterZeroMaster = player.getPreviewPlayerForTest()!!
        val shadowZeroMaster = Shadows.shadowOf(previewAfterZeroMaster)
        assertEquals(0f, shadowZeroMaster.leftVolume, 0.0001f)
        assertEquals(0f, shadowZeroMaster.rightVolume, 0.0001f)
        player.stopPreview()

        masterVol = 0.8f
        sfxVol = 0f
        player.playTransientWavFile(validWav)
        val previewAfterZeroSfx = player.getPreviewPlayerForTest()!!
        val shadowZeroSfx = Shadows.shadowOf(previewAfterZeroSfx)
        assertEquals(0f, shadowZeroSfx.leftVolume, 0.0001f)
        assertEquals(0f, shadowZeroSfx.rightVolume, 0.0001f)
        player.stopPreview()

        player.release()
        badWav.delete()
    }

    @Test
    fun `Concurrent getOrCreateMusicFile and rapid activeMusicTrack changes converge safely`() = runBlocking {
        val archiveStore = SoundArchiveStore(context)
        listOf(
            MusicTrack.CYBER_AMBIENT,
            MusicTrack.PULSE_ARPEGGIO,
            MusicTrack.DEEP_SPACE,
            MusicTrack.NEON_OVERDRIVE,
            MusicTrack.CELESTIAL_BASTION
        ).forEach { track ->
            val file = archiveStore.getMusicFile(track)
            if (file.exists()) file.delete()
            ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(file.absolutePath), ShadowMediaPlayer.MediaInfo(45_000, 0))
        }

        val player = MusicAndPreviewPlayer(
            archiveStore = archiveStore,
            isEnabled = { true },
            isMuted = { false },
            isSuspended = { false },
            isPaused = { false },
            getMasterVolume = { 0.8f },
            getSfxVolume = { 0.8f },
            getMusicVolume = { 0.5f },
            getActiveMusicTrack = { MusicTrack.DEEP_SPACE }
        )

        val generatedFiles = (1..8).map { idx ->
            async(Dispatchers.IO) {
                val track = if (idx % 2 == 0) MusicTrack.DEEP_SPACE else MusicTrack.NEON_OVERDRIVE
                player.getOrCreateMusicFile(track)
            }
        }.awaitAll()

        generatedFiles.forEach { wav ->
            assertTrue("Generated WAV must exist: ${wav.name}", wav.exists())
            assertTrue("Generated WAV must exceed header size", wav.length() > 256L)
            val header = wav.inputStream().use { input ->
                ByteArray(12).also { input.read(it) }
            }
            assertEquals("RIFF", String(header, 0, 4))
            assertEquals("WAVE", String(header, 8, 4))
        }
        val leftoverTmps = archiveStore.archiveDir.listFiles { f -> f.name.endsWith(".tmp") } ?: emptyArray()
        assertEquals("No .tmp files may remain after atomic WAV generation", 0, leftoverTmps.size)
        player.release()

        // Rapid activeMusicTrack switches must converge to the final selected track
        val sm = SoundManager(context)
        sm.initLoadJob?.join()
        sm.activeMusicTrack = MusicTrack.CYBER_AMBIENT
        sm.activeMusicTrack = MusicTrack.PULSE_ARPEGGIO
        sm.activeMusicTrack = MusicTrack.DEEP_SPACE
        sm.activeMusicTrack = MusicTrack.NEON_OVERDRIVE
        sm.activeMusicTrack = MusicTrack.CELESTIAL_BASTION
        sm.awaitPendingIoForTest()

        assertEquals(MusicTrack.CELESTIAL_BASTION, sm.activeMusicTrack)
        val bgmRecords = sm.assetLibraryFlow.value.filter { it.category == "BACKGROUND_MUSIC" }
        assertTrue(bgmRecords.first { it.id == "bgm_celestial_bastion" }.isEquipped)
        assertEquals(1, bgmRecords.count { it.isEquipped })
        sm.release()
        sm.awaitPendingIoForTest()
    }

    @Test
    fun `resumeMusic and updateTick dispatch track preparation asynchronously off caller thread`() {
        val archiveStore = SoundArchiveStore(context)
        val ioDispatcher = StandardTestDispatcher()
        val rollingFirstFile = archiveStore.getMusicFile(MusicTrack.CYBER_AMBIENT)
        val rollingSecondFile = archiveStore.getMusicFile(MusicTrack.PULSE_ARPEGGIO)
        rollingFirstFile.delete()
        rollingSecondFile.delete()
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(rollingFirstFile.absolutePath), ShadowMediaPlayer.MediaInfo(45_000, 0))
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(rollingSecondFile.absolutePath), ShadowMediaPlayer.MediaInfo(45_000, 0))

        val player = MusicAndPreviewPlayer(
            archiveStore = archiveStore,
            isEnabled = { true },
            isMuted = { false },
            isSuspended = { false },
            isPaused = { false },
            getMasterVolume = { 0.8f },
            getSfxVolume = { 0.8f },
            getMusicVolume = { 0.5f },
            getActiveMusicTrack = { MusicTrack.ROLLING_PLAYLIST },
            ioDispatcher = ioDispatcher
        )

        // resumeMusic with no active MediaPlayer must schedule async playback rather than blocking caller
        player.resumeMusic()
        assertFalse("resumeMusic must not block caller thread to create WAV or prepare MediaPlayer", player.hasActiveMediaPlayer())
        assertFalse(rollingFirstFile.exists())

        ioDispatcher.scheduler.advanceUntilIdle()
        assertTrue(rollingFirstFile.exists())
        assertTrue(player.hasActiveMediaPlayer())

        // Advancing rolling playlist window in updateTick must also dispatch async
        player.updateTick(nowMs = 60_000L, dtMs = 60_000L)
        assertFalse("updateTick must not synthesize next track synchronously on Main thread", rollingSecondFile.exists())

        ioDispatcher.scheduler.advanceUntilIdle()
        assertTrue(rollingSecondFile.exists())
        assertTrue(player.hasActiveMediaPlayer())

        player.release()
    }
}
