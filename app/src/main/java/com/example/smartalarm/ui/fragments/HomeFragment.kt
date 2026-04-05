package com.example.smartalarm.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.view.HapticFeedbackConstants
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smartalarm.R
import com.example.smartalarm.domain.session.SessionState
import com.example.smartalarm.platform.service.SessionForegroundService
import com.example.smartalarm.ui.charts.SleepChartMode
import com.example.smartalarm.ui.charts.SleepChartView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shawnlin.numberpicker.NumberPicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.smartalarm.platform.alarm.AlarmScheduler

class HomeFragment : Fragment() {

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

    private lateinit var sleepChart: SleepChartView

    private var alarmTime: Long = 0L
    private var alarmWindow: Long = 30 * 60_000L

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val uiHandler = Handler(Looper.getMainLooper())

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

        sleepChart = view.findViewById(R.id.sleep_chart)
        sleepChart.setChartMode(SleepChartMode.LIVE)
        sleepChart.setShowPhaseOverlay(true)

        setupButtons()
        updateAlarmInfo()
        renderState(SessionForegroundService.latestState)

        return view
    }

    override fun onResume() {
        super.onResume()
        uiHandler.post(stateUpdater)
    }

    override fun onPause() {
        uiHandler.removeCallbacks(stateUpdater)
        super.onPause()
    }

    private fun setupButtons() {
        buttonSetAlarm.setOnClickListener {
            showAlarmPicker()
        }

        buttonSetWindow.setOnClickListener {
            showWindowPicker()
        }

        buttonStart.setOnClickListener {
            if (!SessionForegroundService.latestState.isRunning) {
               AlarmScheduler(requireContext()).scheduleExactAlarm(alarmTime)

                SessionForegroundService.startSession(
                    context = requireContext(),
                    alarmTime = alarmTime,
                    alarmWindow = alarmWindow
                )
            } else {
                AlarmScheduler(requireContext()).cancelExactAlarm()
                SessionForegroundService.stopSession(requireContext())
            }
        }
    }

    private fun showAlarmPicker() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_time_picker, null)

        val hourPicker = dialogView.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = dialogView.findViewById<NumberPicker>(R.id.minutePicker)
        val ampmPicker = dialogView.findViewById<NumberPicker>(R.id.ampmPicker)

        minutePicker.setFormatter { value ->
            String.format(Locale.getDefault(), "%02d", value)
        }

        val ampmValues = arrayOf("AM", "PM")

        ampmPicker.minValue = 0
        ampmPicker.maxValue = ampmValues.size - 1
        ampmPicker.displayedValues = ampmValues
        ampmPicker.wrapSelectorWheel = false

        hourPicker.wrapSelectorWheel = true
        minutePicker.wrapSelectorWheel = true

        val calendar = Calendar.getInstance()
        if (alarmTime > 0L) {
            calendar.timeInMillis = alarmTime
        }

        val currentHour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val currentHour12 = when {
            currentHour24 == 0 -> 12
            currentHour24 > 12 -> currentHour24 - 12
            else -> currentHour24
        }
        val currentAmPm = if (currentHour24 >= 12) 1 else 0

        hourPicker.value = currentHour12
        minutePicker.value = currentMinute
        ampmPicker.value = currentAmPm

        val hapticListener = NumberPicker.OnValueChangeListener { picker, _, _ ->
            picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }

        hourPicker.setOnValueChangedListener(hapticListener)
        minutePicker.setOnValueChangedListener(hapticListener)
        ampmPicker.setOnValueChangedListener(hapticListener)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Accept") { _, _ ->
                val selectedHour12 = hourPicker.value
                val selectedMinute = minutePicker.value
                val selectedAmPm = ampmPicker.value

                val selectedHour24 = when {
                    selectedAmPm == 0 && selectedHour12 == 12 -> 0
                    selectedAmPm == 1 && selectedHour12 != 12 -> selectedHour12 + 12
                    else -> selectedHour12
                }

                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, selectedHour24)
                cal.set(Calendar.MINUTE, selectedMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                alarmTime = cal.timeInMillis
                updateAlarmInfo()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWindowPicker() {
        val options = arrayOf("10 min", "20 min", "30 min", "45 min", "60 min")
        val values = arrayOf(10, 20, 30, 45, 60)

        AlertDialog.Builder(requireContext())
            .setTitle("Select smart window")
            .setItems(options) { _, which ->
                alarmWindow = values[which] * 60_000L
                updateAlarmInfo()
            }
            .show()
    }

    private fun updateAlarmInfo() {
        if (alarmTime == 0L) {
            textAlarmInfo.text =
                "Alarm not configured | Window: ${alarmWindow / 60_000L} min"
            return
        }

        val windowStart = alarmTime - alarmWindow

        textAlarmInfo.text =
            "Alarm: ${timeFormat.format(Date(alarmTime))} | Window starts: ${
                timeFormat.format(Date(windowStart))
            }"
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