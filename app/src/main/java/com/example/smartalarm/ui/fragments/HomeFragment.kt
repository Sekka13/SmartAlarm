package com.example.smartalarm.ui.fragments

import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smartalarm.R
import com.example.smartalarm.data.model.SleepSession
import com.example.smartalarm.data.repository.SleepSessionRepository
import com.example.smartalarm.domain.algorithm.SmartAlarmAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment(private val sleepRepo: SleepSessionRepository) : Fragment() {

    private lateinit var textCurrentTime: TextView
    private lateinit var textBpm: TextView
    private lateinit var textPhase: TextView
    private lateinit var textAlarm: TextView
    private lateinit var textAlarmInfo: TextView

    private lateinit var buttonStart: Button
    private lateinit var buttonSetAlarm: Button
    private lateinit var buttonSetWindow: Button

    private var running = false

    private var alarmTime: Long = 0
    private var alarmWindow: Long = 30 * 60_000L   // default 30 min

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        textCurrentTime = view.findViewById(R.id.text_current_time)
        textBpm = view.findViewById(R.id.text_bpm)
        textPhase = view.findViewById(R.id.text_phase)
        textAlarm = view.findViewById(R.id.text_alarm)
        textAlarmInfo = view.findViewById(R.id.text_alarm_info)

        buttonStart = view.findViewById(R.id.button_start)
        buttonSetAlarm = view.findViewById(R.id.button_set_alarm)
        buttonSetWindow = view.findViewById(R.id.button_set_window)

        startClock()
        setupButtons()
        setupSimulationButton()

        return view
    }

    // ---------------- Clock ----------------
    private fun startClock() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val now = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                    .apply { timeZone = TimeZone.getDefault() }
                    .format(System.currentTimeMillis())
                textCurrentTime.text = "Current time: $now"
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    // ---------------- Buttons ----------------
    private fun setupButtons() {

        buttonSetAlarm.setOnClickListener {
            val now = Calendar.getInstance()
            val tp = TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    alarmTime = cal.timeInMillis
                    updateAlarmInfo()
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
            )
            tp.show()
        }

        buttonSetWindow.setOnClickListener {
            val options = arrayOf("10 min", "20 min", "30 min", "45 min", "60 min")
            val values = arrayOf(10, 20, 30, 45, 60)
            AlertDialog.Builder(requireContext())
                .setTitle("Select smart window")
                .setItems(options) { _: DialogInterface, which: Int ->
                    alarmWindow = values[which] * 60_000L
                    updateAlarmInfo()
                }
                .show()
        }
    }

    private fun updateAlarmInfo() {
        if (alarmTime == 0L) return
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        val startWindow = alarmTime - alarmWindow
        textAlarmInfo.text =
            "Alarm ${sdf.format(Date(alarmTime))} | Window starts ${sdf.format(Date(startWindow))}"
    }

    // ---------------- Simulation ----------------
    private fun setupSimulationButton() {
        buttonStart.setOnClickListener {
            if (!running) {
                running = true
                buttonStart.text = "Stop Simulation"
                lifecycleScope.launch { simulateSleepSession(20_000L) } // 20s demo
            } else {
                running = false
                buttonStart.text = "Start Simulation"
            }
        }
    }

    private suspend fun simulateSleepSession(duration: Long) {
        val phases = listOf("Deep", "Light", "REM")
        val startTime = System.currentTimeMillis()
        val endTime = startTime + duration

        var currentTime = startTime

        while (currentTime <= endTime && running) {
            val bpm = when (val phase = phases.random()) {
                "Deep" -> (50..60).random()
                "Light" -> (60..70).random()
                else -> (65..75).random()
            }

            val phase = phases.random()
            updateUIAndCheckAlarm(bpm, phase)

            delay(1000)  // 1s per "minute" in simulation
            currentTime += 1000
        }

        // Guardar sesión completa
        val session = SleepSession(
            startTime = startTime,
            endTime = endTime,
            bpmMin = 50,
            bpmMax = 75,
            bpmAvg = 62
        )

        lifecycleScope.launch(Dispatchers.IO) {
            sleepRepo.insert(session)
        }
    }

    private fun updateUIAndCheckAlarm(bpm: Int, phase: String) {
        textBpm.text = "BPM: $bpm"
        textPhase.text = "Phase: $phase"

        val result = SmartAlarmAlgorithm.checkAlarm(
            bpm,
            phase,
            System.currentTimeMillis(),
            alarmTime,
            alarmWindow
        )

        textAlarm.text =
            "Alarm: ${if (result.shouldTrigger) "ON" else "OFF"} (${result.reason})"
    }
}