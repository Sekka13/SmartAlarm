package com.example.smartalarm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartalarm.data.model.HeartRateSample

@Dao
interface HeartRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: HeartRateSample): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<HeartRateSample>)

    @Query("SELECT * FROM heart_rate_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionId(sessionId: Long): List<HeartRateSample>

    @Query("DELETE FROM heart_rate_samples")
    suspend fun deleteAll()
}