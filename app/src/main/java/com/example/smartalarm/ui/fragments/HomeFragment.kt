package com.example.smartalarm.ui.fragments

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smartalarm.R
import com.example.smartalarm.data.model.SleepSession
import com.example.smartalarm.data.repository.SleepSessionRepository
import com.example.smartalarm.domain.heartrate.HeartRateSource
import com.example.smartalarm.domain.heartrate.SimulationHeartRateSource
// import com.example.smartalarm.domain.heartrate.BleHeartRateSource
import com.example.smartalarm.domain.session.SleepSessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.smartalarm.ui.view.SleepChartView

class HomeFragment(
    private val repository: SleepSessionRepository
) : Fragment() {

    // Cambia esta línea cuando quieras usar BLE real
    private val source: HeartRateSource = SimulationHeartRateSource()
    // private val source: HeartRateSource = BleHeartRateSource()

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

    private lateinit var sessionManager: SleepSessionManager

    private var alarmTime: Long = 0L
    private var alarmWindow: Long = 30 * 60_000L

    private var running = false

    // Último timestamp recibido de la fuente (simulado o real)
    private var currentSampleTime: Long = 0L

    // Momento en que se disparó la alarma
    private var alarmTriggeredAt: Long? = null

    // Primer timestamp mostrado para calcular duración visible
    private var sessionStartDisplayTime: Long = 0L


    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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

        sessionManager = SleepSessionManager(
            repository = repository,
            scope = viewLifecycleOwner.lifecycleScope
        )

        setupButtons()
        updateAlarmInfo()
        resetSessionTexts()

        return view
    }

    private fun setupButtons() {
        buttonSetAlarm.setOnClickListener {
            showAlarmPicker()
        }

        buttonSetWindow.setOnClickListener {
            showWindowPicker()
        }

        buttonStart.setOnClickListener {
            if (!running) {
                startSession(source)
            } else {
                stopSession()
            }
        }
    }

    private fun showAlarmPicker() {
        val now = Calendar.getInstance()

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                // Si la hora ya pasó hoy, se mueve a mañana
                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                alarmTime = cal.timeInMillis
                updateAlarmInfo()
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        ).show()
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

    private fun resetSessionTexts() {
        textCurrentTime.text = "Current time: --"
        textAlarmTriggeredAt.text = "Alarm triggered at: --"
        textDuration.text = "Duration: --"
        textBpm.text = "BPM: --"
        textPhase.text = "Phase: --"
        textAlarm.text = "Alarm: OFF"
    }

    private fun startSession(source: HeartRateSource) {
        sleepChart.clearData()
        running = true
        currentSampleTime = 0L
        alarmTriggeredAt = null
        sessionStartDisplayTime = 0L

        buttonStart.text = "Stop"
        resetSessionTexts()

        sessionManager.startSession(
            source = source,
            alarmTime = alarmTime,
            alarmWindow = alarmWindow,
            onUpdate = { phase, bpm, alarmTriggered, timestamp ->
                activity?.runOnUiThread {
                    currentSampleTime = timestamp

                    if (sessionStartDisplayTime == 0L) {
                        sessionStartDisplayTime = timestamp
                    }

                    val durationMinutes =
                        ((timestamp - sessionStartDisplayTime) / 60_000L).toInt()

                    textCurrentTime.text =
                        "Current time: ${timeFormat.format(Date(timestamp))}"

                    textDuration.text = "Duration: $durationMinutes min"
                    textBpm.text = "BPM: $bpm"
                    textPhase.text = "Phase: ${phase.name}"
                    textAlarm.text = if (alarmTriggered) "Alarm: ON" else "Alarm: OFF"

                    val simulatedPhase: String? = null

                    sleepChart.addPoint(
                        timeMillis = timestamp,
                        bpm = bpm,
                        phase = phase,
                        simulatedPhase = simulatedPhase
                    )

                    if (alarmTriggered && alarmTriggeredAt == null) {
                        alarmTriggeredAt = timestamp
                        textAlarmTriggeredAt.text =
                            "Alarm triggered at: ${timeFormat.format(Date(timestamp))}"
                    }
                }
            },
            onFinished = { session ->
                activity?.runOnUiThread {
                    handleFinishedSession(session)
                }
            }
        )
    }

    private fun stopSession() {
        running = false
        buttonStart.text = "Start"
        sessionManager.stopSession()
        textAlarm.text = "Alarm: OFF"
    }

    private fun handleFinishedSession(session: SleepSession) {
        running = false
        buttonStart.text = "Start"

        val durationMinutes = ((session.endTime - session.startTime) / 60_000L).toInt()

        textDuration.text = "Duration: $durationMinutes min"
        textCurrentTime.text =
            "Current time: ${timeFormat.format(Date(session.endTime))}"

        if (alarmTriggeredAt != null) {
            textAlarm.text = "Alarm: ON"
            textAlarmTriggeredAt.text =
                "Alarm triggered at: ${timeFormat.format(Date(alarmTriggeredAt!!))}"
        } else {
            textAlarm.text = "Alarm: OFF"
            textAlarmTriggeredAt.text = "Alarm triggered at: Natural wake-up"
        }

        textPhase.text =
            "Session finished | Avg: ${session.bpmAvg} | Min: ${session.bpmMin} | Max: ${session.bpmMax}"
    }

    override fun onDestroyView() {
        sessionManager.stopSession()
        super.onDestroyView()
    }
}