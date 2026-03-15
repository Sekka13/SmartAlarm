package com.example.smartalarm.domain.session

import com.example.smartalarm.data.model.SleepSession
import com.example.smartalarm.data.repository.SleepSessionRepository
import com.example.smartalarm.domain.algorithm.SleepPhaseDetector
import com.example.smartalarm.domain.algorithm.SleepSessionAnalyzer
import com.example.smartalarm.domain.algorithm.SmartAlarmAlgorithm
import com.example.smartalarm.domain.heartrate.HeartRateSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SleepSessionManager(
    private val repository: SleepSessionRepository,
    private val scope: CoroutineScope
) {

    private val analyzer = SleepSessionAnalyzer()

    private var heartRateSource: HeartRateSource? = null
    private var running = false

    fun startSession(
        source: HeartRateSource,
        alarmTime: Long,
        alarmWindow: Long,
        onUpdate: (SleepPhaseDetector.Phase, Int, Boolean, Long) -> Unit,
        onFinished: (SleepSession) -> Unit
    ) {

        if (running) return

        running = true
        heartRateSource = source

        SleepPhaseDetector.reset()
        analyzer.reset()

        val effectiveAlarmTime =
            if (alarmTime > 0) alarmTime else Long.MAX_VALUE

        source.start { bpm, timestamp ->

            val phase = SleepPhaseDetector.detectPhase(bpm)

            val sessionCandidate =
                analyzer.onSample(bpm, phase, timestamp)

            val alarmTriggered =
                SmartAlarmAlgorithm.shouldTrigger(
                    phase,
                    timestamp,
                    effectiveAlarmTime,
                    alarmWindow
                )

            scope.launch {
                onUpdate(phase, bpm, alarmTriggered, timestamp)
            }

            val completedSession = when {
                sessionCandidate != null -> sessionCandidate
                alarmTriggered -> analyzer.finishSession(timestamp)
                else -> null
            }

            if (completedSession != null) {

                stopSession()

                scope.launch(Dispatchers.IO) {
                    repository.insert(completedSession)
                }

                scope.launch {
                    onFinished(completedSession)
                }
            }
        }
    }

    fun stopSession() {
        running = false
        heartRateSource?.stop()
        heartRateSource = null

        SleepPhaseDetector.reset()
        analyzer.reset()
    }
}