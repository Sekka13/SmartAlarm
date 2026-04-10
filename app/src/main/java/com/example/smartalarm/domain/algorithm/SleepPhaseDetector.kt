package com.example.smartalarm.domain.algorithm

import kotlin.math.sqrt

object SleepPhaseDetector {

    enum class Phase {
        WAKE,
        LIGHT,
        DEEP,
        REM
    }

    private data class TimedBpmSample(
        val bpm: Int,
        val timestamp: Long
    )

    private val bpmWindow = mutableListOf<TimedBpmSample>()
    private const val WINDOW_DURATION_MS = 30 * 60_000L
    private const val MIN_STABLE_DURATION_MS = 5 * 60_000L

    fun detectPhase(bpm: Int, timestamp: Long): Phase {
        bpmWindow.add(
            TimedBpmSample(
                bpm = bpm,
                timestamp = timestamp
            )
        )
        trimWindow(timestamp)

        val oldestTimestamp = bpmWindow.firstOrNull()?.timestamp ?: return Phase.LIGHT
        if ((timestamp - oldestTimestamp) < MIN_STABLE_DURATION_MS) {
            return Phase.LIGHT
        }

        val avg = bpmWindow.map { it.bpm }.average()
        val std = calculateStdDev()
        val hrv = calculateHRV()

        val z = if (std > 0) {
            (bpm - avg) / std
        } else {
            0.0
        }

        return when {
            z > 1.5 -> Phase.WAKE
            z < -1.5 && hrv < 2.0 -> Phase.DEEP
            z < -0.5 -> Phase.REM
            else -> Phase.LIGHT
        }
    }

    private fun calculateStdDev(): Double {
        if (bpmWindow.size < 2) return 0.0

        val mean = bpmWindow.map { it.bpm }.average()
        val variance = bpmWindow
            .map { (it.bpm - mean) * (it.bpm - mean) }
            .average()

        return sqrt(variance)
    }

    private fun calculateHRV(): Double {
        if (bpmWindow.size < 3) return 0.0

        val squaredDiffs = mutableListOf<Double>()

        for (i in 1 until bpmWindow.size) {
            val diff = (bpmWindow[i].bpm - bpmWindow[i - 1].bpm).toDouble()
            squaredDiffs.add(diff * diff)
        }

        return sqrt(squaredDiffs.average())
    }

    private fun trimWindow(now: Long) {
        val minTimestamp = now - WINDOW_DURATION_MS
        while (bpmWindow.isNotEmpty() && bpmWindow.first().timestamp < minTimestamp) {
            bpmWindow.removeAt(0)
        }
    }

    fun reset() {
        bpmWindow.clear()
    }
}
