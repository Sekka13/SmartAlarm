package com.example.smartalarm.domain.algorithm

import com.example.smartalarm.data.model.SleepSession

class SleepSessionAnalyzer {

    private var sessionStart: Long = 0L

    private var candidateSleepStart: Long = 0L
    private var consecutiveSleepSamples = 0
    private var consecutiveWakeSamples = 0

    private val candidateBpms = mutableListOf<Int>()
    private val sessionBpms = mutableListOf<Int>()

    companion object {
        // NUEVO:
        // 1 muestra = 1 minuto simulado, así que 90 = 90 minutos
        private const val MIN_SESSION_MINUTES_BEFORE_NATURAL_WAKE = 90L

        // NUEVO:
        // Exigimos más estabilidad en WAKE para cerrar la sesión
        private const val REQUIRED_WAKE_SAMPLES = 20
    }

    fun onSample(
        bpm: Int,
        phase: SleepPhaseDetector.Phase,
        now: Long
    ): SleepSession? {

        // Si la sesión aún no ha empezado realmente, buscamos un inicio estable
        if (sessionStart == 0L) {
            if (phase != SleepPhaseDetector.Phase.WAKE) {
                if (consecutiveSleepSamples == 0) {
                    candidateSleepStart = now
                    candidateBpms.clear()
                }

                consecutiveSleepSamples++
                candidateBpms.add(bpm)

                // Consideramos que el usuario se ha dormido
                // cuando hay 3 muestras consecutivas no WAKE
                if (consecutiveSleepSamples >= 3) {
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

        // Si la sesión ya ha empezado, acumulamos BPM
        sessionBpms.add(bpm)

        // Contamos muestras consecutivas en WAKE
        if (phase == SleepPhaseDetector.Phase.WAKE) {
            consecutiveWakeSamples++
        } else {
            consecutiveWakeSamples = 0
        }

        // NUEVO:
        // No permitimos natural wake-up demasiado pronto
        val sessionDurationMinutes = (now - sessionStart) / 60_000L

        if (sessionDurationMinutes < MIN_SESSION_MINUTES_BEFORE_NATURAL_WAKE) {
            return null
        }

        // Solo cerramos la sesión si el despertar es estable
        if (consecutiveWakeSamples >= REQUIRED_WAKE_SAMPLES) {
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
        consecutiveWakeSamples = 0
        resetCandidate()
        sessionBpms.clear()
    }

    private fun resetCandidate() {
        candidateSleepStart = 0L
        consecutiveSleepSamples = 0
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