package com.example.smartalarm.domain.session

import com.example.smartalarm.data.model.HeartRateSample
import com.example.smartalarm.data.model.SleepSession
import com.example.smartalarm.data.repository.HeartRateSampleRepository
import com.example.smartalarm.data.repository.SleepSessionRepository
import com.example.smartalarm.domain.algorithm.SleepPhaseDetector
import com.example.smartalarm.domain.algorithm.SleepSessionAnalyzer
import com.example.smartalarm.domain.algorithm.SmartAlarmAlgorithm
import com.example.smartalarm.domain.heartrate.HeartRateSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SleepSessionManager(
    private val sessionRepository: SleepSessionRepository,
    private val sampleRepository: HeartRateSampleRepository,
    private val scope: CoroutineScope
) {

    private val analyzer = SleepSessionAnalyzer()

    private var heartRateSource: HeartRateSource? = null
    private var running = false

    private val phaseAccumulator = PhaseStatsAccumulator()
    private val pendingSamples = mutableListOf<PendingSample>()

    private var alarmTriggeredAt: Long? = null
    private var triggerReason: String? = null

    private data class PendingSample(
        val bpm: Int,
        val timestamp: Long,
        val phase: String
    )

    fun startSession(
        source: HeartRateSource,
        alarmTime: Long,
        alarmWindow: Long,
        replayMode: SessionReplayMode,
        onUpdate: (SleepPhaseDetector.Phase, Int, Boolean, Long) -> Unit,
        onFinished: (SleepSession) -> Unit
    ) {
        if (running) return

        running = true
        heartRateSource = source

        SleepPhaseDetector.reset()
        analyzer.reset()
        phaseAccumulator.reset()
        pendingSamples.clear()
        alarmTriggeredAt = null
        triggerReason = null

        val effectiveAlarmTime =
            if (alarmTime > 0) alarmTime else Long.MAX_VALUE

        source.start { bpm, timestamp ->
            val phase = SleepPhaseDetector.detectPhase(
                bpm = bpm,
                timestamp = timestamp
            )
            val phaseName = phase.name

            phaseAccumulator.addSample(timestamp, phaseName)

            pendingSamples.add(
                PendingSample(
                    bpm = bpm,
                    timestamp = timestamp,
                    phase = phaseName
                )
            )

            val sessionCandidate = analyzer.onSample(bpm, phase, timestamp)

            val alarmTriggered = SmartAlarmAlgorithm.shouldTrigger(
                phase = phase,
                now = timestamp,
                alarmTime = effectiveAlarmTime,
                alarmWindow = alarmWindow
            )

            if (alarmTriggered && alarmTriggeredAt == null) {
                alarmTriggeredAt = timestamp
                triggerReason = if (timestamp < effectiveAlarmTime) {
                    "SMART_WINDOW"
                } else {
                    "MAX_TIME"
                }
            }

            scope.launch {
                onUpdate(phase, bpm, alarmTriggered, timestamp)
            }

            val baseCompletedSession = when {
                sessionCandidate != null -> {
                    if (alarmTriggeredAt == null) {
                        triggerReason = "NATURAL_WAKEUP"
                    }
                    sessionCandidate
                }
                alarmTriggered -> analyzer.finishSession(timestamp)
                else -> null
            }

            if (baseCompletedSession != null) {
                running = false
                heartRateSource?.stop()
                heartRateSource = null

                val finalAlarmTriggeredAt = alarmTriggeredAt
                val finalTriggerReason = triggerReason
                val finalLightMinutes = phaseAccumulator.getLightMinutes()
                val finalDeepMinutes = phaseAccumulator.getDeepMinutes()
                val finalRemMinutes = phaseAccumulator.getRemMinutes()
                val finalWakeMinutes = phaseAccumulator.getWakeMinutes()
                val finalSamples = pendingSamples.toList()

                val completedSession = baseCompletedSession.copy(
                    lightMinutes = finalLightMinutes,
                    deepMinutes = finalDeepMinutes,
                    remMinutes = finalRemMinutes,
                    wakeMinutes = finalWakeMinutes,
                    alarmTriggeredAt = finalAlarmTriggeredAt,
                    triggerReason = finalTriggerReason,
                    replayMode = replayMode.name
                )

                scope.launch(Dispatchers.IO) {
                    val sessionId = sessionRepository.insert(completedSession)

                    val samples = finalSamples.map {
                        HeartRateSample(
                            sessionId = sessionId,
                            bpm = it.bpm,
                            timestamp = it.timestamp,
                            phase = it.phase
                        )
                    }

                    sampleRepository.insertAll(samples)
                }

                scope.launch {
                    onFinished(completedSession)
                }

                resetInternalState()
            }
        }
    }

    fun stopSession() {
        running = false
        heartRateSource?.stop()
        heartRateSource = null
        resetInternalState()
    }

    private fun resetInternalState() {
        pendingSamples.clear()
        phaseAccumulator.reset()
        alarmTriggeredAt = null
        triggerReason = null

        SleepPhaseDetector.reset()
        analyzer.reset()
    }
}
