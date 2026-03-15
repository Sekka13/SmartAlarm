package com.example.smartalarm.domain.algorithm

data class AlarmResult(
    val shouldTrigger: Boolean,
    val reason: String
)

object SmartAlarmAlgorithm {

    fun checkAlarm(
        bpm: Int,
        phase: String,
        now: Long,
        alarmTime: Long,
        window: Long
    ): AlarmResult {

        if (alarmTime == 0L) {
            return AlarmResult(false, "Alarm not set")
        }

        val windowStart = alarmTime - window
        val windowEnd = alarmTime

        if (now < windowStart) {
            return AlarmResult(false, "Outside window")
        }

        if (now in windowStart..windowEnd) {

            if (phase == "Light") {
                return AlarmResult(true, "Optimal wake phase")
            }

            return AlarmResult(false, "Waiting optimal phase")
        }

        if (now >= windowEnd) {
            return AlarmResult(true, "Max alarm time reached")
        }

        return AlarmResult(false, "No trigger")
    }
}