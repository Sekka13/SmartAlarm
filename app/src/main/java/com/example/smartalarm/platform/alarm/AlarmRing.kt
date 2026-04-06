package com.example.smartalarm.platform.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object AlarmRing {

    private const val VIBRATION_NORMAL = "Normal"
    private const val VIBRATION_SOFT = "Soft"
    private const val VIBRATION_STRONG = "Strong"
    private const val VIBRATION_OFF = "Off"

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ringing: Boolean = false

    fun start(
        context: Context,
        soundName: String,
        volumePercent: Int = 100,
        vibrationMode: String = VIBRATION_NORMAL
    ) {
        if (ringing) return

        val appContext = context.applicationContext
        val sound = AlarmSoundCatalog.getSoundByKey(soundName) ?: AlarmSoundCatalog.getDefaultSound()

        if (sound != null) {
            mediaPlayer = MediaPlayer.create(appContext, sound.resId)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }

                isLooping = true

                val volume = (volumePercent.coerceIn(0, 100) / 100f)
                setVolume(volume, volume)

                start()
            }
        }

        @Suppress("DEPRECATION")
        vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        startVibration(vibrationMode)

        ringing = true
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()

        mediaPlayer = null
        vibrator = null
        ringing = false
    }

    fun isRinging(): Boolean = ringing

    private fun startVibration(vibrationMode: String) {
        if (vibrationMode == VIBRATION_OFF) return

        val pattern = when (vibrationMode) {
            VIBRATION_SOFT -> longArrayOf(0, 200, 700, 200)
            VIBRATION_STRONG -> longArrayOf(0, 700, 250, 700)
            else -> longArrayOf(0, 500, 500, 500)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }
}