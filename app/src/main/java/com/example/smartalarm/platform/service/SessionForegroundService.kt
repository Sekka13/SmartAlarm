package com.example.smartalarm.platform.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.smartalarm.R
import com.example.smartalarm.data.db.AppDatabase
import com.example.smartalarm.data.model.SleepSession
import com.example.smartalarm.data.repository.HeartRateSampleRepository
import com.example.smartalarm.data.repository.SleepSessionRepository
import com.example.smartalarm.domain.heartrate.HeartRateSource
import com.example.smartalarm.domain.heartrate.SimulationHeartRateSource
import com.example.smartalarm.domain.session.SessionController
import com.example.smartalarm.domain.session.SessionEventListener
import com.example.smartalarm.domain.session.SessionReplayMode
import com.example.smartalarm.domain.session.SessionState
import com.example.smartalarm.domain.session.SleepSessionManager
import com.example.smartalarm.platform.alarm.AlarmActivity
import com.example.smartalarm.platform.alarm.AlarmScheduler
import com.example.smartalarm.platform.alarm.AlarmUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SessionForegroundService : Service(), SessionEventListener {

    companion object {
        private const val CHANNEL_ID = "smart_alarm_session_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_SESSION = "com.example.smartalarm.action.START_SESSION"
        const val ACTION_STOP_SESSION = "com.example.smartalarm.action.STOP_SESSION"

        const val EXTRA_ALARM_TIME = "extra_alarm_time"
        const val EXTRA_ALARM_WINDOW = "extra_alarm_window"
        const val EXTRA_SOUND_NAME = "extra_sound_name"
        const val EXTRA_VOLUME_PERCENT = "extra_volume_percent"
        const val EXTRA_VIBRATION_MODE = "extra_vibration_mode"
        const val EXTRA_REPLAY_MODE = "extra_replay_mode"

        @Volatile
        var latestState: SessionState = SessionState()
            private set

        fun startSession(
            context: Context,
            alarmTime: Long,
            alarmWindow: Long,
            soundName: String,
            volumePercent: Int,
            vibrationMode: String,
            replayMode: SessionReplayMode
        ) {
            val intent = Intent(context, SessionForegroundService::class.java).apply {
                action = ACTION_START_SESSION
                putExtra(EXTRA_ALARM_TIME, alarmTime)
                putExtra(EXTRA_ALARM_WINDOW, alarmWindow)
                putExtra(EXTRA_SOUND_NAME, soundName)
                putExtra(EXTRA_VOLUME_PERCENT, volumePercent)
                putExtra(EXTRA_VIBRATION_MODE, vibrationMode)
                putExtra(EXTRA_REPLAY_MODE, replayMode.name)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopSession(context: Context) {
            val intent = Intent(context, SessionForegroundService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
            context.startService(intent)
        }
    }

    private lateinit var sessionController: SessionController
    private lateinit var heartRateSource: HeartRateSource
    private lateinit var serviceScope: CoroutineScope

    private var alarmAlreadyLaunched = false
    private var currentSoundName: String = "Default"
    private var currentVolumePercent: Int = 100
    private var currentVibrationMode: String = "Normal"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> handleStartSession(intent)
            ACTION_STOP_SESSION -> handleStopSession()
        }
        return START_STICKY
    }

    private fun handleStartSession(intent: Intent) {
        ensureInitialized()
        startSessionForeground("Session running")

        val alarmTime = intent.getLongExtra(EXTRA_ALARM_TIME, 0L)
        val alarmWindow = intent.getLongExtra(EXTRA_ALARM_WINDOW, 30 * 60_000L)
        val replayMode = SessionReplayMode.fromName(intent.getStringExtra(EXTRA_REPLAY_MODE))

        currentSoundName = intent.getStringExtra(EXTRA_SOUND_NAME) ?: "Default"
        currentVolumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, 100)
        currentVibrationMode = intent.getStringExtra(EXTRA_VIBRATION_MODE) ?: "Normal"

        alarmAlreadyLaunched = false
        latestState = SessionState()
        heartRateSource = buildHeartRateSource(replayMode)

        if (!sessionController.isRunning()) {
            sessionController.startSession(
                source = heartRateSource,
                alarmTime = alarmTime,
                alarmWindow = alarmWindow,
                replayMode = replayMode
            )
        }
    }

    private fun handleStopSession() {
        if (::sessionController.isInitialized && sessionController.isRunning()) {
            sessionController.stopSession()
        }

        AlarmScheduler(applicationContext).cancelExactAlarm()
        AlarmUtils.dismissAlarmNotification(applicationContext)

        latestState = latestState.copy(isRunning = false)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureInitialized() {
        if (::sessionController.isInitialized) return

        val db = AppDatabase.getDatabase(applicationContext)
        val sessionRepository = SleepSessionRepository(db.sleepSessionDao())
        val sampleRepository = HeartRateSampleRepository(db.heartRateDao())

        serviceScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

        val sessionManager = SleepSessionManager(
            sessionRepository = sessionRepository,
            sampleRepository = sampleRepository,
            scope = serviceScope
        )

        sessionController = SessionController(sessionManager)
        sessionController.setListener(this)
    }

    private fun buildHeartRateSource(replayMode: SessionReplayMode): HeartRateSource {
        return SimulationHeartRateSource(
            context = applicationContext,
            replayMode = replayMode
        )
    }

    override fun onSessionStateChanged(state: SessionState) {
        latestState = state

        if (state.alarmTriggered && !alarmAlreadyLaunched) {
            launchAlarm()
            return
        }

        updateSessionNotification(state)
    }

    override fun onSessionFinished(session: SleepSession) {
        latestState = latestState.copy(isRunning = false)

        if (!alarmAlreadyLaunched && session.alarmTriggeredAt != null) {
            launchAlarm()
            return
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun launchAlarm() {
        alarmAlreadyLaunched = true

        AlarmScheduler(applicationContext).cancelExactAlarm()

        if (isAppInForeground()) {
            AlarmActivity.launch(
                context = applicationContext,
                soundName = currentSoundName,
                volumePercent = currentVolumePercent,
                vibrationMode = currentVibrationMode
            )
        } else {
            AlarmUtils.showAlarmNotification(
                context = applicationContext,
                soundName = currentSoundName,
                volumePercent = currentVolumePercent,
                vibrationMode = currentVibrationMode
            )
        }

        if (::sessionController.isInitialized && sessionController.isRunning()) {
            sessionController.stopSession()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.STARTED)
    }

    private fun updateSessionNotification(state: SessionState) {
        val text = if (state.isRunning) {
            val bpm = state.currentBpm?.toString() ?: "--"
            val phase = state.currentPhase ?: "--"
            "BPM $bpm · $phase"
        } else {
            "Session idle"
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun startSessionForeground(contentText: String) {
        val notification = buildNotification(contentText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SmartAlarm session")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartAlarm Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the sleep session active in background"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (::sessionController.isInitialized) {
            sessionController.setListener(null)
        }
        if (::serviceScope.isInitialized) {
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
