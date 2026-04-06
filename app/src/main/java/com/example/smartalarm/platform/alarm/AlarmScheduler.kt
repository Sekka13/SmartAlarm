package com.example.smartalarm.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmScheduler(
    private val context: Context
) {

    fun scheduleExactAlarm(
        triggerAtMillis: Long,
        soundName: String,
        volumePercent: Int,
        vibrationMode: String
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            soundName = soundName,
            volumePercent = volumePercent,
            vibrationMode = vibrationMode
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    return false
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancelExactAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            soundName = "",
            volumePercent = 100,
            vibrationMode = ""
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        private const val REQUEST_CODE = 2001
        const val EXTRA_SOUND_NAME = "extra_sound_name"
        const val EXTRA_VOLUME_PERCENT = "extra_volume_percent"
        const val EXTRA_VIBRATION_MODE = "extra_vibration_mode"

        fun buildAlarmPendingIntent(
            context: Context,
            soundName: String,
            volumePercent: Int,
            vibrationMode: String
        ): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_SOUND_NAME, soundName)
                putExtra(EXTRA_VOLUME_PERCENT, volumePercent)
                putExtra(EXTRA_VIBRATION_MODE, vibrationMode)
            }

            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}