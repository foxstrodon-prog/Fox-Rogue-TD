package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.MusicTrack
import com.example.audio.SoundManager
import com.example.model.TowerClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
