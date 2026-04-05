package com.example.smartalarm.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_samples",
foreignKeys = [
ForeignKey(
entity = SleepSession::class,
parentColumns = ["id"],
childColumns = ["sessionId"],
onDelete = ForeignKey.CASCADE
)
],
indices = [Index("sessionId")]
)
data class HeartRateSample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val bpm: Int,
    val timestamp: Long,
    val phase: String
)