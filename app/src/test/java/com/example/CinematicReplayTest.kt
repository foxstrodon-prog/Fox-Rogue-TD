package com.example

import com.example.model.ActiveCinematicState
import com.example.model.CinematicScenario
import org.junit.Assert.*
import org.junit.Test

/**
 * Quinn (Lead QA & Test Architect)
 * Domain Test Suite for In-App 30-Second Scripted Cinematic Engine (Track 4).
 * Verifies deterministic 1,800-frame termination, scenario IDs, highlight timestamps, target filenames, and progress.
 */
class CinematicReplayTest {

    @Test
    fun `Verify exactly 6 unique cinematic showcase scenarios exist`() {
        val scenarios = CinematicScenario.entries
        assertEquals("Must provide exactly 6 scripted showcase scenarios", 6, scenarios.size)

        val uniqueIds = scenarios.map { it.id }.toSet()
        assertEquals("Each cinematic scenario must have a distinct unique ID", 6, uniqueIds.size)
    }

    @Test
    fun `Verify all scenarios define valid titles, subtitles, and positive target waves`() {
        for (scenario in CinematicScenario.entries) {
            assertTrue("Scenario ${scenario.id} title must not be blank", scenario.title.isNotBlank())
            assertTrue("Scenario ${scenario.id} subtitle must not be blank", scenario.subtitle.isNotBlank())
            assertTrue("Scenario ${scenario.id} description must not be blank", scenario.description.isNotBlank())
            assertTrue("Target wave must be >= 30", scenario.targetWave >= 30)
            assertTrue("Thumbnail timestamp must be within 30s window (0..30)", scenario.thumbnailTimestampSec in 10f..28f)
        }
    }

    @Test
    fun `ActiveCinematicState frame timing matches 30 seconds at 60 FPS`() {
        val scenario = CinematicScenario.CIN_01_BOSS_MELT
        val state = ActiveCinematicState(scenario = scenario, currentFrame = 0, totalFrames = 1800)

        assertEquals(0f, state.elapsedSeconds, 0.001f)
        assertEquals(30f, state.remainingSeconds, 0.001f)
        assertEquals(0f, state.progress, 0.001f)
        assertFalse(state.isComplete)

        // Advance to mid-point (15.00s = 900 frames)
        val midState = state.copy(currentFrame = 900)
        assertEquals(15f, midState.elapsedSeconds, 0.001f)
        assertEquals(15f, midState.remainingSeconds, 0.001f)
        assertEquals(0.5f, midState.progress, 0.001f)
        assertFalse(midState.isComplete)

        // Advance to completion (1800 frames = 30.00s)
        val endState = state.copy(currentFrame = 1800)
        assertEquals(30f, endState.elapsedSeconds, 0.001f)
        assertEquals(0f, endState.remainingSeconds, 0.001f)
        assertEquals(1.0f, endState.progress, 0.001f)
        assertTrue("Cinematic must be marked complete at 1800 frames", endState.isComplete)
    }

    @Test
    fun `Specific highlight timestamps match cinematic showcase specifications`() {
        assertEquals(23.40f, CinematicScenario.CIN_01_BOSS_MELT.thumbnailTimestampSec, 0.01f)
        assertEquals(18.20f, CinematicScenario.CIN_02_BULLET_HELL.thumbnailTimestampSec, 0.01f)
        assertEquals(15.30f, CinematicScenario.CIN_03_BOOSTER_WARP.thumbnailTimestampSec, 0.01f)
        assertEquals(21.10f, CinematicScenario.CIN_04_ELEMENTAL_CASCADE.thumbnailTimestampSec, 0.01f)
        assertEquals(16.50f, CinematicScenario.CIN_05_KINETIC_GATLING.thumbnailTimestampSec, 0.01f)
        assertEquals(25.00f, CinematicScenario.CIN_06_EVOLUTION_MONTAGE.thumbnailTimestampSec, 0.01f)
    }

    @Test
    fun `Target filenames follow version and cinematic number pattern`() {
        assertEquals("v001004_Cinematic1.mp4", CinematicScenario.CIN_01_BOSS_MELT.getTargetFilename("v001004"))
        assertEquals("v001004_Cinematic2.mp4", CinematicScenario.CIN_02_BULLET_HELL.getTargetFilename("v001004"))
        assertEquals("v001004_Cinematic3.mp4", CinematicScenario.CIN_03_BOOSTER_WARP.getTargetFilename("v001004"))
        assertEquals("v001004_Cinematic4.mp4", CinematicScenario.CIN_04_ELEMENTAL_CASCADE.getTargetFilename("v001004"))
        assertEquals("v001004_Cinematic5.mp4", CinematicScenario.CIN_05_KINETIC_GATLING.getTargetFilename("v001004"))
        assertEquals("v001004_Cinematic6.mp4", CinematicScenario.CIN_06_EVOLUTION_MONTAGE.getTargetFilename("v001004"))
    }
}
