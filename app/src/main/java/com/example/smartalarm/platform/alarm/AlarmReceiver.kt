package com.example.smartalarm.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val soundName = intent?.getStringExtra(AlarmScheduler.EXTRA_SOUND_NAME) ?: "Default"
        val volumePercent = intent?.getIntExtra(AlarmScheduler.EXTRA_VOLUME_PERCENT, 100) ?: 100
        val vibrationMode = intent?.getStringExtra(AlarmScheduler.EXTRA_VIBRATION_MODE) ?: "Normal"

        AlarmUtils.showAlarmNotification(
            context = context,
            soundName = soundName,
            volumePercent = volumePercent,
            vibrationMode = vibrationMode
        )
    }
}

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AlarmUtils.dismissAlarmNotification(context)
    }
}