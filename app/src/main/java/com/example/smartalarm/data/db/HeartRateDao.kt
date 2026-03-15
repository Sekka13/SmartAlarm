package com.example.smartalarm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.smartalarm.data.model.HeartRateSample

@Dao
interface HeartRateDao {
    @Insert
    suspend fun insert(sample: HeartRateSample)

    @Query("SELECT * FROM heart_rate_samples ORDER BY timestamp ASC")
    suspend fun getAll(): List<HeartRateSample>
}