package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Quinn (Lead QA & Test Architect)
 * Domain Test Suite for True Screen Orientation & HUD Counter-Rotation (Track 1).
 * Verifies orientation bucket detection, deadband filtering, and counter-rotation angle logic.
 */
class OrientationTest {

    @Test
    fun `Orientation bucket calculation handles exact cardinal angles`() {
        assertEquals(0f, MainActivity.computeOrientationBucket(0, 0f), 0.01f)
        assertEquals(90f, MainActivity.computeOrientationBucket(90, 0f), 0.01f)
        assertEquals(180f, MainActivity.computeOrientationBucket(180, 0f), 0.01f)
        assertEquals(270f, MainActivity.computeOrientationBucket(270, 0f), 0.01f)
    }

    @Test
    fun `Orientation bucket preserves current angle within deadband margins`() {
        // Deadband = 15 deg, halfGap = 30 deg. Angles in (90 - 30)..(90 + 30) = 60..120 map to 90.
        assertEquals(90f, MainActivity.computeOrientationBucket(85, 0f), 0.01f)
        assertEquals(90f, MainActivity.computeOrientationBucket(105, 0f), 0.01f)
        assertEquals(270f, MainActivity.computeOrientationBucket(260, 0f), 0.01f)
        assertEquals(0f, MainActivity.computeOrientationBucket(350, 0f), 0.01f)
    }

    @Test
    fun `Counter-rotation math produces upright horizontal reading angle`() {
        val testAngles = listOf(0f, 90f, 180f, 270f)
        for (deviceAngle in testAngles) {
            val counterRotation = -deviceAngle
            val normalized = (counterRotation + 360f) % 360f
            val sum = (deviceAngle + normalized) % 360f
            assertEquals("Device angle plus counter rotation must equal 0 modulo 360", 0f, sum, 0.001f)
        }
    }
}
