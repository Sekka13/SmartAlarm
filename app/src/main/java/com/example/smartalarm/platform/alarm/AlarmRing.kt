package com.example.smartalarm.platform.alarm

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object AlarmRing {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var ringing: Boolean = false

    fun start(context: Context) {
        if (ringing) return

        val appContext = context.applicationContext

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        ringtone = RingtoneManager.getRingtone(appContext, alarmUri)
        ringtone?.play()

        @Suppress("DEPRECATION")
        vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 500, 500),
                    0
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 500, 500), 0)
        }

        ringing = true
    }

    fun stop() {
        ringtone?.stop()
        vibrator?.cancel()

        ringtone = null
        vibrator = null
        ringing = false
    }

    fun isRinging(): Boolean = ringing
}