package com.example.smartalarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val bpmMin: Int,
    val bpmMax: Int,
    val bpmAvg: Int,
    val lightMinutes: Int = 0,
    val deepMinutes: Int = 0,
    val remMinutes: Int = 0,
    val wakeMinutes: Int = 0,

    val alarmTriggeredAt: Long? = null,
    val triggerReason: String? = null,
    val replayMode: String = "LEGACY_UNKNOWN"
)
