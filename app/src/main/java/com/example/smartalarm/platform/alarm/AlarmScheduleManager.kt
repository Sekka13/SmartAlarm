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

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

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
        val daysLabel = if (alarm.repeatDays.isBlank()) {
            "One time"
        } else {
            alarm.repeatDays
        }

        val triggerAtMillis = computeNextTriggerAtMillis(alarm)
        val relativeLabel = buildRelativeTimeUntil(triggerAtMillis)

        return "Next: ${formatAlarmTime(alarm.hour24, alarm.minute)} • $relativeLabel | " +
                "Window: ${alarm.smartWindowMinutes} min | $daysLabel"
    }

    fun buildRelativeTimeUntil(
        triggerAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val remainingMillis = (triggerAtMillis - nowMillis).coerceAtLeast(0L)
        val totalMinutes = if (remainingMillis == 0L) {
            0L
        } else {
            (remainingMillis + 59_999L) / 60_000L
        }
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L

        return when {
            hours > 0L -> "Rings in ${hours} h ${minutes} min"
            minutes > 0L -> "Rings in ${minutes} min"
            else -> "Rings in less than 1 min"
        }
    }

    fun buildRelativeTimeUntil(
        alarm: AlarmConfig,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        return buildRelativeTimeUntil(
            triggerAtMillis = computeNextTriggerAtMillis(alarm),
            nowMillis = nowMillis
        )
    }

    fun buildSetupPreview(alarm: AlarmConfig, nowMillis: Long = System.currentTimeMillis()): String {
        val triggerAtMillis = computeNextTriggerAtMillis(alarm)
        val relativeLabel = buildRelativeTimeUntil(triggerAtMillis, nowMillis)
            .replaceFirst("Rings", "Alarm will ring")

        return "$relativeLabel | ${buildOccurrenceLabel(triggerAtMillis, nowMillis)}"
    }

    fun formatAlarmTime(hour24: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour24, minute)
    }

    fun buildOccurrenceLabel(
        triggerAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val now = Calendar.getInstance().apply {
            timeInMillis = nowMillis
        }
        val trigger = Calendar.getInstance().apply {
            timeInMillis = triggerAtMillis
        }

        val dayLabel = when (dayDistance(now, trigger)) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> dayFormat.format(Date(triggerAtMillis))
        }

        return "$dayLabel ${timeFormat.format(Date(triggerAtMillis))}"
    }

    private fun dayDistance(now: Calendar, trigger: Calendar): Int {
        val nowStart = now.clone() as Calendar
        nowStart.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val triggerStart = trigger.clone() as Calendar
        triggerStart.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return ((triggerStart.timeInMillis - nowStart.timeInMillis) / 86_400_000L).toInt()
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
