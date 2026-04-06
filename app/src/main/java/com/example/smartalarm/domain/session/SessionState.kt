package com.example.smartalarm.domain.session

import com.example.smartalarm.ui.charts.SleepChartPoint

data class SessionState(
    val isRunning: Boolean = false,
    val currentBpm: Int? = null,
    val currentPhase: String? = null,
    val currentTimestamp: Long = 0L,
    val alarmTriggered: Boolean = false,
    val alarmTriggeredAt: Long? = null,
    val sessionStartDisplayTime: Long = 0L,
    val chartPoints: List<SleepChartPoint> = emptyList()
)