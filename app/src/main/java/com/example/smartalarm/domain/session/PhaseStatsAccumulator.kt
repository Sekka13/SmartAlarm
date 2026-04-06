package com.example.smartalarm.domain.session

class PhaseStatsAccumulator {

    private var lastTimestamp: Long? = null
    private var lastPhase: String? = null

    private var lightMillis: Long = 0
    private var deepMillis: Long = 0
    private var remMillis: Long = 0
    private var wakeMillis: Long = 0

    fun addSample(timestamp: Long, phase: String) {
        val previousTimestamp = lastTimestamp
        val previousPhase = lastPhase

        if (previousTimestamp != null && previousPhase != null && timestamp > previousTimestamp) {
            val delta = timestamp - previousTimestamp
            when (previousPhase) {
                "LIGHT" -> lightMillis += delta
                "DEEP" -> deepMillis += delta
                "REM" -> remMillis += delta
                "WAKE" -> wakeMillis += delta
            }
        }

        lastTimestamp = timestamp
        lastPhase = phase
    }

    fun getLightMinutes(): Int = (lightMillis / 60_000L).toInt()
    fun getDeepMinutes(): Int = (deepMillis / 60_000L).toInt()
    fun getRemMinutes(): Int = (remMillis / 60_000L).toInt()
    fun getWakeMinutes(): Int = (wakeMillis / 60_000L).toInt()

    fun reset() {
        lastTimestamp = null
        lastPhase = null
        lightMillis = 0L
        deepMillis = 0L
        remMillis = 0L
        wakeMillis = 0L
    }
}