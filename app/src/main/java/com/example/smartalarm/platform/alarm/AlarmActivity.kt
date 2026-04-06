package com.example.smartalarm.platform.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.smartalarm.R

class AlarmActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SOUND_NAME = "extra_sound_name"
        private const val EXTRA_VOLUME_PERCENT = "extra_volume_percent"
        private const val EXTRA_VIBRATION_MODE = "extra_vibration_mode"

        fun launch(
            context: Context,
            soundName: String = "Default",
            volumePercent: Int = 100,
            vibrationMode: String = "Normal"
        ) {
            val intent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SOUND_NAME, soundName)
                putExtra(EXTRA_VOLUME_PERCENT, volumePercent)
                putExtra(EXTRA_VIBRATION_MODE, vibrationMode)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm)

        val soundName = intent.getStringExtra(EXTRA_SOUND_NAME) ?: "Default"
        val volumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, 100)
        val vibrationMode = intent.getStringExtra(EXTRA_VIBRATION_MODE) ?: "Normal"

        val buttonStop = findViewById<Button>(R.id.button_stop_alarm)

        AlarmRing.start(
            context = this,
            soundName = soundName,
            volumePercent = volumePercent,
            vibrationMode = vibrationMode
        )

        buttonStop.setOnClickListener {
            AlarmRing.stop()
            AlarmUtils.dismissAlarmNotification(this)
            finish()
        }
    }
}