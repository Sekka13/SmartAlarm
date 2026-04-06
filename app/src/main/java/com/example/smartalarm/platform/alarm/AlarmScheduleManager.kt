package com.example.smartalarm.domain.alarm

import com.example.smartalarm.data.model.AlarmConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NextAlarmResult(
    val alarm: AlarmConfig,
    val triggerAtMillis: Long
)

class AlarmScheduleManager {

    fun resolveNextActiveAlarm(alarms: List<AlarmConfig>): NextAlarmResult? {
        val enabledAlarms = alarms.filter { it.isEnabled }
        if (enabledAlarms.isEmpty()) return null

        return enabledAlarms
            .map { alarm ->
                NextAlarmResult(
                    alarm = alarm,
                    triggerAtMillis = computeNextTriggerAtMillis(alarm)
                )
            }
            .minByOrNull { it.triggerAtMillis }
    }

    fun computeNextTriggerAtMillis(alarm: AlarmConfig): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, alarm.hour24)
            set(Calendar.MINUTE, alarm.minute)
        }

        val repeatDays = parseRepeatDays(alarm.repeatDays)

        if (repeatDays.isEmpty()) {
            if (candidate.timeInMillis <= now.timeInMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }

        for (offset in 0..7) {
            val test = Calendar.getInstance().apply {
                timeInMillis = candidate.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }

            val dayMatches = test.get(Calendar.DAY_OF_WEEK) in repeatDays
            val isFuture = test.timeInMillis > now.timeInMillis

            if (dayMatches && isFuture) {
                return test.timeInMillis
            }
        }

        candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate.timeInMillis
    }

    fun buildAlarmSummary(alarm: AlarmConfig): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour24)
            set(Calendar.MINUTE, alarm.minute)
        }

        val daysLabel = if (alarm.repeatDays.isBlank()) {
            "One time"
        } else {
            alarm.repeatDays
        }

        return "Next: ${timeFormat.format(Date(calendar.timeInMillis))} | " +
                "Window: ${alarm.smartWindowMinutes} min | $daysLabel"
    }

    private fun parseRepeatDays(repeatDays: String): Set<Int> {
        if (repeatDays.isBlank()) return emptySet()

        return repeatDays.split(",")
            .mapNotNull { day ->
                when (day.trim()) {
                    "SUN" -> Calendar.SUNDAY
                    "MON" -> Calendar.MONDAY
                    "TUE" -> Calendar.TUESDAY
                    "WED" -> Calendar.WEDNESDAY
                    "THU" -> Calendar.THURSDAY
                    "FRI" -> Calendar.FRIDAY
                    "SAT" -> Calendar.SATURDAY
                    else -> null
                }
            }
            .toSet()
    }
}