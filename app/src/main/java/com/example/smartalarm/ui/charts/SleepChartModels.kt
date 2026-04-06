package com.example.smartalarm.ui.charts

data class SleepChartPoint(
    val timestamp: Long,
    val bpm: Int,
    val phase: String
)

data class SleepPhaseSegment(
    val startX: Float,
    val endX: Float,
    val phase: String
)

enum class SleepChartMode {
    LIVE,
    DETAIL,
    COMPACT
}