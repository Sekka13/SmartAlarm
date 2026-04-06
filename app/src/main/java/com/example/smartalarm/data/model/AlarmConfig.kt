package com.example.smartalarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_configs")
data class AlarmConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour24: Int,
    val minute: Int,
    val smartWindowMinutes: Int,
    val repeatDays: String,
    val soundName: String,
    val volumePercent: Int,
    val vibrationMode: String,
    val snoozeEnabled: Boolean,
    val snoozeMinutes: Int,
    val snoozeMaxRepeats: Int,
    val isEnabled: Boolean = true,
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)