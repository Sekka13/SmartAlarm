package com.example.smartalarm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartalarm.data.model.SleepSession

@Dao
interface SleepSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SleepSession): Long

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<SleepSession>

    @Query("SELECT * FROM sleep_sessions WHERE startTime >= :fromTime ORDER BY startTime DESC")
    suspend fun getSessionsFrom(fromTime: Long): List<SleepSession>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SleepSession?
}