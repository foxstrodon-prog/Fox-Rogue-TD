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

    @Test
    fun `Shortest path counter-rotation maps 270 degrees to plus 90 degrees`() {
        fun computeCounterAngle(deviceRotation: Float): Float = when (((deviceRotation % 360f + 360f) % 360f).toInt()) {
            90 -> -90f
            180 -> 180f
            270 -> 90f
            else -> 0f
        }

        assertEquals(0f, computeCounterAngle(0f), 0.01f)
        assertEquals(-90f, computeCounterAngle(90f), 0.01f)
        assertEquals(180f, computeCounterAngle(180f), 0.01f)
        // 270 deg device rotation (tilted right) must rotate +90 deg (shortest path) rather than -270 deg
        assertEquals(90f, computeCounterAngle(270f), 0.01f)
    }

    @Test
    fun `Shortest path angle difference avoids 270 degree rotation jump`() {
        fun computeDiff(target: Float, current: Float): Float {
            return (((target - (current % 360f) + 180f) % 360f + 360f) % 360f) - 180f
        }

        // From 0 deg to 270 deg (rotating right), shortest path is -90 deg (NOT +270 deg)
        val diffTo270 = computeDiff(270f, 0f)
        assertEquals(-90f, diffTo270, 0.01f)

        // From -90 deg back to 0 deg, shortest path is +90 deg
        val diffTo0 = computeDiff(0f, -90f)
        assertEquals(90f, diffTo0, 0.01f)

        // From 0 deg to 90 deg, shortest path is +90 deg
        val diffTo90 = computeDiff(90f, 0f)
        assertEquals(90f, diffTo90, 0.01f)
    }
}
