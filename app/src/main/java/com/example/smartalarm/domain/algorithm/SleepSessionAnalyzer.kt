package com.example.smartalarm.domain.algorithm

import com.example.smartalarm.data.model.SleepSession

class SleepSessionAnalyzer {

    companion object {
        private const val MIN_SESSION_MINUTES_BEFORE_NATURAL_WAKE = 90L
        private const val REQUIRED_SLEEP_STABILITY_MS = 3 * 60_000L
        private const val REQUIRED_WAKE_STABILITY_MS = 20 * 60_000L
    }

    private var sessionStart: Long = 0L
    private var candidateSleepStart: Long = 0L
    private var wakeStableSince: Long = 0L

    private val candidateBpms = mutableListOf<Int>()
    private val sessionBpms = mutableListOf<Int>()

    fun onSample(
        bpm: Int,
        phase: SleepPhaseDetector.Phase,
        now: Long
    ): SleepSession? {

        if (sessionStart == 0L) {
            if (phase != SleepPhaseDetector.Phase.WAKE) {
                if (candidateSleepStart == 0L) {
                    candidateSleepStart = now
                    candidateBpms.clear()
                }

                candidateBpms.add(bpm)

                if ((now - candidateSleepStart) >= REQUIRED_SLEEP_STABILITY_MS) {
                    sessionStart = candidateSleepStart
                    sessionBpms.clear()
                    sessionBpms.addAll(candidateBpms)
                    candidateBpms.clear()
                }
            } else {
                resetCandidate()
            }

            return null
        }

        sessionBpms.add(bpm)

        if (phase == SleepPhaseDetector.Phase.WAKE) {
            if (wakeStableSince == 0L) {
                wakeStableSince = now
            }
        } else {
            wakeStableSince = 0L
        }

        val sessionDurationMinutes = (now - sessionStart) / 60_000L
        if (sessionDurationMinutes < MIN_SESSION_MINUTES_BEFORE_NATURAL_WAKE) {
            return null
        }

        if (wakeStableSince != 0L && (now - wakeStableSince) >= REQUIRED_WAKE_STABILITY_MS) {
            return buildSessionAndReset(now)
        }

        return null
    }

    fun finishSession(now: Long): SleepSession? {
        if (sessionStart == 0L || sessionBpms.isEmpty()) {
            return null
        }

        return buildSessionAndReset(now)
    }

    fun reset() {
        sessionStart = 0L
        wakeStableSince = 0L
        resetCandidate()
        sessionBpms.clear()
    }

    private fun resetCandidate() {
        candidateSleepStart = 0L
        candidateBpms.clear()
    }

    private fun buildSessionAndReset(endTime: Long): SleepSession {
        val bpmMin = sessionBpms.minOrNull() ?: 0
        val bpmMax = sessionBpms.maxOrNull() ?: 0
        val bpmAvg = if (sessionBpms.isNotEmpty()) {
            sessionBpms.average().toInt()
        } else {
            0
        }

        val session = SleepSession(
            startTime = sessionStart,
            endTime = endTime,
            bpmMin = bpmMin,
            bpmMax = bpmMax,
            bpmAvg = bpmAvg
        )

        reset()
        return session
    }
}
