package com.example.smartalarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_samples")
data class HeartRateSample(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bpm: Int,
    val timestamp: Long
)