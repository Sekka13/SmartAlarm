package com.example.smartalarm.ui.fragments

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smartalarm.R
import com.example.smartalarm.data.db.AppDatabase
import com.example.smartalarm.data.model.AlarmConfig
import com.example.smartalarm.data.repository.AlarmConfigRepository
import com.google.android.material.chip.Chip
import com.shawnlin.numberpicker.NumberPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import com.example.smartalarm.platform.alarm.AlarmSoundCatalog
class AlarmSetupFragment : Fragment() {

    interface Listener {
        fun onAlarmSetupCancelled()
        fun onAlarmSaved()
    }

    companion object {
        private const val ARG_ALARM_ID = "arg_alarm_id"

        fun newInstance(alarmId: Long? = null): AlarmSetupFragment {
            return AlarmSetupFragment().apply {
                arguments = Bundle().apply {
                    if (alarmId != null) {
                        putLong(ARG_ALARM_ID, alarmId)
                    }
                }
            }
        }
    }

    private lateinit var alarmRepository: AlarmConfigRepository

    private lateinit var buttonBack: ImageButton
    private lateinit var buttonSetAlarm: Button

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var ampmPicker: NumberPicker

    private lateinit var spinnerSmartWindow: Spinner

    private lateinit var chipMon: Chip
    private lateinit var chipTue: Chip
    private lateinit var chipWed: Chip
    private lateinit var chipThu: Chip
    private lateinit var chipFri: Chip
    private lateinit var chipSat: Chip
    private lateinit var chipSun: Chip

    private lateinit var spinnerSound: Spinner
    private lateinit var seekBarVolume: SeekBar
    private lateinit var textVolumeValue: TextView

    private lateinit var spinnerVibration: Spinner

    private lateinit var checkBoxSnoozeEnabled: CheckBox
    private lateinit var spinnerSnoozeMinutes: Spinner
    private lateinit var spinnerSnoozeCount: Spinner
    private lateinit var textSnoozeMinutesLabel: TextView
    private lateinit var textSnoozeCountLabel: TextView

    private var editingAlarmId: Long? = null
    private var editingAlarm: AlarmConfig? = null

    private var availableSounds: List<AlarmSoundCatalog.AlarmSound> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingAlarmId = if (arguments?.containsKey(ARG_ALARM_ID) == true) {
            arguments?.getLong(ARG_ALARM_ID)
        } else {
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_alarm_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        alarmRepository = AlarmConfigRepository(db.alarmConfigDao())

        bindViews(view)
        setupTimePickers()
        setupSmartWindowSection()
        setupRepeatDayChips()
        setupSoundSection()
        setupVibrationSection()
        setupSnoozeSection()
        setupButtons()

        if (editingAlarmId == null) {
            populateInitialTime()
            buttonSetAlarm.text = "Set alarm"
        } else {
            buttonSetAlarm.text = "Save changes"
            loadAlarmForEdit()
        }
    }

    private fun bindViews(view: View) {
        buttonBack = view.findViewById(R.id.button_back_alarm_setup)
        buttonSetAlarm = view.findViewById(R.id.button_set_alarm_setup)

        hourPicker = view.findViewById(R.id.hourPicker)
        minutePicker = view.findViewById(R.id.minutePicker)
        ampmPicker = view.findViewById(R.id.ampmPicker)

        spinnerSmartWindow = view.findViewById(R.id.spinner_smart_window)

        chipMon = view.findViewById(R.id.chip_mon)
        chipTue = view.findViewById(R.id.chip_tue)
        chipWed = view.findViewById(R.id.chip_wed)
        chipThu = view.findViewById(R.id.chip_thu)
        chipFri = view.findViewById(R.id.chip_fri)
        chipSat = view.findViewById(R.id.chip_sat)
        chipSun = view.findViewById(R.id.chip_sun)

        spinnerSound = view.findViewById(R.id.spinner_alarm_sound)
        seekBarVolume = view.findViewById(R.id.seekbar_alarm_volume)
        textVolumeValue = view.findViewById(R.id.text_volume_value)

        spinnerVibration = view.findViewById(R.id.spinner_vibration_mode)

        checkBoxSnoozeEnabled = view.findViewById(R.id.checkbox_snooze_enabled)
        spinnerSnoozeMinutes = view.findViewById(R.id.spinner_snooze_minutes)
        spinnerSnoozeCount = view.findViewById(R.id.spinner_snooze_count)
        textSnoozeMinutesLabel = view.findViewById(R.id.text_snooze_minutes_label)
        textSnoozeCountLabel = view.findViewById(R.id.text_snooze_count_label)
    }

    private fun setupButtons() {
        buttonBack.setOnClickListener {
            (parentFragment as? Listener)?.onAlarmSetupCancelled()
        }

        buttonSetAlarm.setOnClickListener {
            saveAlarm()
        }
    }

    private fun loadAlarmForEdit() {
        val alarmId = editingAlarmId ?: return

        lifecycleScope.launch {
            val alarm = withContext(Dispatchers.IO) {
                alarmRepository.getById(alarmId)
            }

            if (alarm == null) {
                populateInitialTime()
                return@launch
            }

            editingAlarm = alarm
            populateAlarmData(alarm)
        }
    }

    private fun populateAlarmData(alarm: AlarmConfig) {
        val hour12 = when {
            alarm.hour24 == 0 -> 12
            alarm.hour24 > 12 -> alarm.hour24 - 12
            else -> alarm.hour24
        }
        val amPm = if (alarm.hour24 >= 12) 1 else 0

        hourPicker.value = hour12
        minutePicker.value = alarm.minute
        ampmPicker.value = amPm

        setSpinnerByLeadingInt(spinnerSmartWindow, alarm.smartWindowMinutes)

        applyRepeatDays(alarm.repeatDays)

        setSoundSpinnerByKey(alarm.soundName)
        seekBarVolume.progress = alarm.volumePercent
        textVolumeValue.text = "${alarm.volumePercent}%"

        setSpinnerByText(spinnerVibration, alarm.vibrationMode)

        checkBoxSnoozeEnabled.isChecked = alarm.snoozeEnabled
        setSpinnerByLeadingInt(spinnerSnoozeMinutes, alarm.snoozeMinutes)
        setSpinnerByLeadingInt(spinnerSnoozeCount, alarm.snoozeMaxRepeats)
        updateSnoozeControls()
    }
    private fun setSoundSpinnerByKey(soundKey: String) {
        val index = availableSounds.indexOfFirst { it.key == soundKey }
        if (index >= 0) {
            spinnerSound.setSelection(index)
        }
    }
    private fun applyRepeatDays(repeatDays: String) {
        val days = repeatDays.split(",").map { it.trim() }.toSet()

        chipMon.isChecked = "MON" in days
        chipTue.isChecked = "TUE" in days
        chipWed.isChecked = "WED" in days
        chipThu.isChecked = "THU" in days
        chipFri.isChecked = "FRI" in days
        chipSat.isChecked = "SAT" in days
        chipSun.isChecked = "SUN" in days
    }

    private fun setSpinnerByLeadingInt(spinner: Spinner, value: Int) {
        for (i in 0 until spinner.count) {
            val item = spinner.getItemAtPosition(i).toString()
            if (parseLeadingInt(item) == value) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun setSpinnerByText(spinner: Spinner, value: String) {
        for (i in 0 until spinner.count) {
            val item = spinner.getItemAtPosition(i).toString()
            if (item == value) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun saveAlarm() {
        val selectedHour12 = hourPicker.value
        val selectedMinute = minutePicker.value
        val selectedAmPm = ampmPicker.value

        val selectedHour24 = when {
            selectedAmPm == 0 && selectedHour12 == 12 -> 0
            selectedAmPm == 1 && selectedHour12 != 12 -> selectedHour12 + 12
            else -> selectedHour12
        }

        val repeatDays = buildRepeatDaysString()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val existing = editingAlarm

                val alarm = AlarmConfig(
                    id = existing?.id ?: 0,
                    hour24 = selectedHour24,
                    minute = selectedMinute,
                    smartWindowMinutes = parseLeadingInt(spinnerSmartWindow.selectedItem.toString()),
                    repeatDays = repeatDays,
                    soundName = getSelectedSoundKey(),
                    volumePercent = seekBarVolume.progress,
                    vibrationMode = spinnerVibration.selectedItem.toString(),
                    snoozeEnabled = checkBoxSnoozeEnabled.isChecked,
                    snoozeMinutes = parseLeadingInt(spinnerSnoozeMinutes.selectedItem.toString()),
                    snoozeMaxRepeats = parseLeadingInt(spinnerSnoozeCount.selectedItem.toString()),
                    isEnabled = existing?.isEnabled ?: true,
                    isSelected = existing?.isSelected ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )

                if (editingAlarmId == null) {
                    alarmRepository.insert(alarm)
                } else {
                    alarmRepository.update(alarm)
                }
            }

            (parentFragment as? Listener)?.onAlarmSaved()
        }
    }

    private fun buildRepeatDaysString(): String {
        val selectedDays = mutableListOf<String>()

        if (chipMon.isChecked) selectedDays.add("MON")
        if (chipTue.isChecked) selectedDays.add("TUE")
        if (chipWed.isChecked) selectedDays.add("WED")
        if (chipThu.isChecked) selectedDays.add("THU")
        if (chipFri.isChecked) selectedDays.add("FRI")
        if (chipSat.isChecked) selectedDays.add("SAT")
        if (chipSun.isChecked) selectedDays.add("SUN")

        return selectedDays.joinToString(",")
    }

    private fun parseLeadingInt(text: String): Int {
        return text.trim().substringBefore(" ").toIntOrNull() ?: 0
    }

    private fun setupTimePickers() {
        hourPicker.wrapSelectorWheel = true

        minutePicker.wrapSelectorWheel = true
        minutePicker.setFormatter { value ->
            String.format(Locale.getDefault(), "%02d", value)
        }

        val ampmValues = arrayOf("AM", "PM")
        ampmPicker.minValue = 0
        ampmPicker.maxValue = ampmValues.size - 1
        ampmPicker.displayedValues = ampmValues
        ampmPicker.wrapSelectorWheel = false

        val hapticListener = NumberPicker.OnValueChangeListener { picker, _, _ ->
            picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }

        hourPicker.setOnValueChangedListener(hapticListener)
        minutePicker.setOnValueChangedListener(hapticListener)
        ampmPicker.setOnValueChangedListener(hapticListener)
    }

    private fun populateInitialTime() {
        val calendar = Calendar.getInstance()
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
    }

    private fun setupSmartWindowSection() {
        val smartWindows = listOf("10 min", "20 min", "30 min", "45 min", "60 min")
        spinnerSmartWindow.adapter = buildSimpleAdapter(smartWindows)
        spinnerSmartWindow.setSelection(2)
    }

    private fun setupRepeatDayChips() {
        val chips = listOf(chipMon, chipTue, chipWed, chipThu, chipFri, chipSat, chipSun)
        chips.forEach { chip ->
            chip.isCheckable = true
            chip.isChecked = false
        }
    }

    private fun setupSoundSection() {
        availableSounds = AlarmSoundCatalog.getAvailableSounds()

        val soundNames = if (availableSounds.isEmpty()) {
            listOf("No sounds available")
        } else {
            availableSounds.map { it.displayName }
        }

        spinnerSound.adapter = buildSimpleAdapter(soundNames)

        seekBarVolume.max = 100
        seekBarVolume.progress = 80
        textVolumeValue.text = "80%"

        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textVolumeValue.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun getSelectedSoundKey(): String {
        if (availableSounds.isEmpty()) return ""

        val selectedIndex = spinnerSound.selectedItemPosition
        return availableSounds.getOrNull(selectedIndex)?.key ?: availableSounds.first().key
    }

    private fun setupVibrationSection() {
        val vibrations = listOf("Normal", "Soft", "Strong", "Off")
        spinnerVibration.adapter = buildSimpleAdapter(vibrations)
    }

    private fun setupSnoozeSection() {
        val snoozeMinutes = listOf("5 min", "10 min", "15 min", "20 min")
        val snoozeCount = listOf("1 time", "2 times", "3 times", "5 times")

        spinnerSnoozeMinutes.adapter = buildSimpleAdapter(snoozeMinutes)
        spinnerSnoozeCount.adapter = buildSimpleAdapter(snoozeCount)

        checkBoxSnoozeEnabled.isChecked = true
        updateSnoozeControls()

        checkBoxSnoozeEnabled.setOnCheckedChangeListener { _, _ ->
            updateSnoozeControls()
        }
    }

    private fun updateSnoozeControls() {
        val enabled = checkBoxSnoozeEnabled.isChecked

        textSnoozeMinutesLabel.isEnabled = enabled
        textSnoozeCountLabel.isEnabled = enabled
        spinnerSnoozeMinutes.isEnabled = enabled
        spinnerSnoozeCount.isEnabled = enabled

        val alpha = if (enabled) 1f else 0.5f
        textSnoozeMinutesLabel.alpha = alpha
        textSnoozeCountLabel.alpha = alpha
        spinnerSnoozeMinutes.alpha = alpha
        spinnerSnoozeCount.alpha = alpha
    }

    private fun buildSimpleAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}