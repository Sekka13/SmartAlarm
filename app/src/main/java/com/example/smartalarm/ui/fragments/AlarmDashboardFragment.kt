package com.example.smartalarm.ui.fragments

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartalarm.R
import com.example.smartalarm.data.db.AppDatabase
import com.example.smartalarm.data.model.AlarmConfig
import com.example.smartalarm.data.repository.AlarmConfigRepository
import com.example.smartalarm.domain.alarm.AlarmScheduleManager
import com.example.smartalarm.domain.session.SessionState
import com.example.smartalarm.platform.alarm.AlarmScheduler
import com.example.smartalarm.platform.service.SessionForegroundService
import com.example.smartalarm.ui.adapters.AlarmConfigAdapter
import com.example.smartalarm.ui.charts.SleepChartMode
import com.example.smartalarm.ui.charts.SleepChartView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlarmDashboardFragment : Fragment() {

    private lateinit var alarmRepository: AlarmConfigRepository
    private lateinit var alarmAdapter: AlarmConfigAdapter
    private val alarmScheduleManager = AlarmScheduleManager()

    private lateinit var textBpm: TextView
    private lateinit var textPhase: TextView
    private lateinit var textAlarm: TextView
    private lateinit var textAlarmInfo: TextView
    private lateinit var textCurrentTime: TextView
    private lateinit var textAlarmTriggeredAt: TextView
    private lateinit var textDuration: TextView

    private lateinit var buttonStart: Button
    private lateinit var buttonSetAlarm: Button
    private lateinit var buttonSetWindow: Button

    private lateinit var recyclerAlarms: RecyclerView
    private lateinit var sleepChart: SleepChartView

    private var nextActiveAlarm: AlarmConfig? = null
    private var alarmTime: Long = 0L
    private var alarmWindow: Long = TimeUnit.MINUTES.toMillis(30)

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val uiHandler = Handler(Looper.getMainLooper())

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                textAlarmInfo.text = "Enable notifications so the alarm can appear in background"
            }
        }

    private val stateUpdater = object : Runnable {
        override fun run() {
            if (isAdded) {
                renderState(SessionForegroundService.latestState)
                uiHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val db = AppDatabase.getDatabase(requireContext())
        alarmRepository = AlarmConfigRepository(db.alarmConfigDao())

        textBpm = view.findViewById(R.id.text_bpm)
        textPhase = view.findViewById(R.id.text_phase)
        textAlarm = view.findViewById(R.id.text_alarm)
        textAlarmInfo = view.findViewById(R.id.text_alarm_info)
        textCurrentTime = view.findViewById(R.id.text_current_time)
        textAlarmTriggeredAt = view.findViewById(R.id.text_alarm_triggered_at)
        textDuration = view.findViewById(R.id.text_duration)

        buttonStart = view.findViewById(R.id.button_start)
        buttonSetAlarm = view.findViewById(R.id.button_set_alarm)
        buttonSetWindow = view.findViewById(R.id.button_set_window)

        recyclerAlarms = view.findViewById(R.id.recycler_alarms)

        sleepChart = view.findViewById(R.id.sleep_chart)
        sleepChart.setChartMode(SleepChartMode.LIVE)
        sleepChart.setShowPhaseOverlay(true)

        setupRecycler()
        setupButtons()
        renderState(SessionForegroundService.latestState)

        return view
    }

    override fun onResume() {
        super.onResume()
        uiHandler.post(stateUpdater)
        loadAlarms()
    }

    override fun onPause() {
        uiHandler.removeCallbacks(stateUpdater)
        super.onPause()
    }

    private fun setupRecycler() {
        alarmAdapter = AlarmConfigAdapter(
            items = emptyList(),
            onAlarmClicked = { alarm ->
                (parentFragment as? AlarmContainerFragment)?.showSetup(alarm.id)
            },
            onAlarmEnabledChanged = { alarm, enabled ->
                updateAlarmEnabled(alarm, enabled)
            },
            onAlarmDeleteClicked = { alarm ->
                deleteAlarm(alarm)
            }
        )

        recyclerAlarms.layoutManager = LinearLayoutManager(requireContext())
        recyclerAlarms.adapter = alarmAdapter
    }

    private fun setupButtons() {
        buttonSetAlarm.text = "Add new alarm"
        buttonSetAlarm.setOnClickListener {
            (parentFragment as? AlarmContainerFragment)?.showSetup()
        }

        buttonSetWindow.visibility = View.GONE

        buttonStart.setOnClickListener {
            if (!SessionForegroundService.latestState.isRunning) {
                startSessionWithChecks()
            } else {
                AlarmScheduler(requireContext()).cancelExactAlarm()
                SessionForegroundService.stopSession(requireContext())
            }
        }
    }

    private fun loadAlarms() {
        lifecycleScope.launch {
            val alarms = withContext(Dispatchers.IO) {
                alarmRepository.getAll()
            }

            alarmAdapter.submitList(alarms)
            applyNextAlarmState(alarms)
        }
    }

    private fun applyNextAlarmState(alarms: List<AlarmConfig>) {
        val nextResult = alarmScheduleManager.resolveNextActiveAlarm(alarms)

        if (nextResult == null) {
            nextActiveAlarm = null
            alarmTime = 0L
            alarmWindow = TimeUnit.MINUTES.toMillis(30)
            textAlarmInfo.text = "No active alarms"
            return
        }

        nextActiveAlarm = nextResult.alarm
        alarmTime = nextResult.triggerAtMillis
        alarmWindow = TimeUnit.MINUTES.toMillis(nextResult.alarm.smartWindowMinutes.toLong())
        textAlarmInfo.text = alarmScheduleManager.buildAlarmSummary(nextResult.alarm)
    }

    private fun updateAlarmEnabled(alarm: AlarmConfig, enabled: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                alarmRepository.update(alarm.copy(isEnabled = enabled))
            }
            loadAlarms()
        }
    }

    private fun deleteAlarm(alarm: AlarmConfig) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                alarmRepository.delete(alarm)
            }
            loadAlarms()
        }
    }

    private fun startSessionWithChecks() {
        if (nextActiveAlarm == null || alarmTime == 0L) {
            textAlarmInfo.text = "Create and activate at least one alarm before starting"
            return
        }

        if (!hasNotificationPermission()) {
            textAlarmInfo.text = "Allow notifications so the alarm can work in background"
            requestNotificationPermission()
            return
        }

        if (!canUseFullScreenIntent()) {
            textAlarmInfo.text = "Allow full-screen alarms for SmartAlarm"
            requestFullScreenIntentPermission()
            return
        }

        val alarm = nextActiveAlarm ?: return

        val scheduled = AlarmScheduler(requireContext()).scheduleExactAlarm(
            triggerAtMillis = alarmTime,
            soundName = alarm.soundName,
            volumePercent = alarm.volumePercent,
            vibrationMode = alarm.vibrationMode
        )
        if (!scheduled) {
            textAlarmInfo.text = "The exact alarm could not be scheduled"
            return
        }



        SessionForegroundService.startSession(
            context = requireContext(),
            alarmTime = alarmTime,
            alarmWindow = alarmWindow,
            soundName = alarm.soundName,
            volumePercent = alarm.volumePercent,
            vibrationMode = alarm.vibrationMode
        )
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val manager = requireContext().getSystemService(NotificationManager::class.java)
            manager.canUseFullScreenIntent()
        } else {
            true
        }
    }

    private fun requestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
        }
    }

    private fun renderState(state: SessionState) {
        buttonStart.text = if (state.isRunning) "Stop" else "Start"

        if (state.currentTimestamp == 0L) {
            textCurrentTime.text = "Current time: --"
            textDuration.text = "Duration: --"
            textBpm.text = "BPM: --"
            textPhase.text = "Phase: --"
            textAlarm.text = "Alarm: OFF"
            if (state.alarmTriggeredAt == null) {
                textAlarmTriggeredAt.text = "Alarm triggered at: --"
            }
            sleepChart.clearData()
            return
        }

        textCurrentTime.text = "Current time: ${timeFormat.format(Date(state.currentTimestamp))}"
        textBpm.text = "BPM: ${state.currentBpm ?: "--"}"
        textPhase.text = "Phase: ${state.currentPhase ?: "--"}"
        textAlarm.text = if (state.alarmTriggered) "Alarm: ON" else "Alarm: OFF"

        if (state.sessionStartDisplayTime > 0L) {
            val durationMinutes =
                ((state.currentTimestamp - state.sessionStartDisplayTime) / 60_000L).toInt()
            textDuration.text = "Duration: $durationMinutes min"
        } else {
            textDuration.text = "Duration: --"
        }

        if (state.alarmTriggeredAt != null) {
            textAlarmTriggeredAt.text =
                "Alarm triggered at: ${timeFormat.format(Date(state.alarmTriggeredAt))}"
        } else {
            textAlarmTriggeredAt.text = "Alarm triggered at: --"
        }

        sleepChart.setSessionData(state.chartPoints)
    }
}