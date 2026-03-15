package com.example.smartalarm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.smartalarm.data.model.SleepSession

@Dao
interface SleepSessionDao {
    @Insert
    suspend fun insert(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<SleepSession>
}