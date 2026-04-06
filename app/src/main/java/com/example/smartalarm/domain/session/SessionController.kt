package com.example.smartalarm.domain.session

import com.example.smartalarm.data.model.SleepSession
import com.example.smartalarm.domain.algorithm.SleepPhaseDetector
import com.example.smartalarm.domain.heartrate.HeartRateSource
import com.example.smartalarm.ui.charts.SleepChartPoint

class SessionController(
    private val sessionManager: SleepSessionManager
) {

    private var listener: SessionEventListener? = null

    private var state = SessionState()

    fun setListener(listener: SessionEventListener?) {
        this.listener = listener
    }

    fun getState(): SessionState = state

    fun isRunning(): Boolean = state.isRunning

    fun startSession(
        source: HeartRateSource,
        alarmTime: Long,
        alarmWindow: Long
    ) {
        if (state.isRunning) return

        state = SessionState(
            isRunning = true,
            currentBpm = null,
            currentPhase = null,
            currentTimestamp = 0L,
            alarmTriggered = false,
            alarmTriggeredAt = null,
            sessionStartDisplayTime = 0L,
            chartPoints = emptyList()
        )
        notifyStateChanged()

        sessionManager.startSession(
            source = source,
            alarmTime = alarmTime,
            alarmWindow = alarmWindow,
            onUpdate = { phase, bpm, alarmTriggered, timestamp ->
                handleUpdate(
                    phase = phase,
                    bpm = bpm,
                    alarmTriggered = alarmTriggered,
                    timestamp = timestamp
                )
            },
            onFinished = { session ->
                handleFinished(session)
            }
        )
    }

    fun stopSession() {
        if (!state.isRunning) return

        sessionManager.stopSession()

        state = state.copy(
            isRunning = false,
            alarmTriggered = false
        )
        notifyStateChanged()
    }

    private fun handleUpdate(
        phase: SleepPhaseDetector.Phase,
        bpm: Int,
        alarmTriggered: Boolean,
        timestamp: Long
    ) {
        val startDisplayTime =
            if (state.sessionStartDisplayTime == 0L) timestamp else state.sessionStartDisplayTime

        val triggeredAt =
            if (alarmTriggered && state.alarmTriggeredAt == null) timestamp else state.alarmTriggeredAt

        val updatedPoints = state.chartPoints + SleepChartPoint(
            timestamp = timestamp,
            bpm = bpm,
            phase = phase.name
        )

        state = state.copy(
            isRunning = true,
            currentBpm = bpm,
            currentPhase = phase.name,
            currentTimestamp = timestamp,
            alarmTriggered = alarmTriggered || state.alarmTriggered,
            alarmTriggeredAt = triggeredAt,
            sessionStartDisplayTime = startDisplayTime,
            chartPoints = updatedPoints
        )

        notifyStateChanged()
    }

    private fun handleFinished(session: SleepSession) {
        state = state.copy(
            isRunning = false
        )
        notifyStateChanged()
        listener?.onSessionFinished(session)
    }

    private fun notifyStateChanged() {
        listener?.onSessionStateChanged(state)
    }
}