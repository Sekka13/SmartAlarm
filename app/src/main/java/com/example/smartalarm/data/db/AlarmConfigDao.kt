package com.example.smartalarm.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.smartalarm.data.model.AlarmConfig

@Dao
interface AlarmConfigDao {

    @Insert
    suspend fun insert(alarm: AlarmConfig): Long

    @Update
    suspend fun update(alarm: AlarmConfig)

    @Delete
    suspend fun delete(alarm: AlarmConfig)

    @Query("SELECT * FROM alarm_configs ORDER BY hour24 ASC, minute ASC, createdAt DESC")
    suspend fun getAll(): List<AlarmConfig>

    @Query("SELECT * FROM alarm_configs WHERE id = :alarmId LIMIT 1")
    suspend fun getById(alarmId: Long): AlarmConfig?

    @Query("SELECT * FROM alarm_configs WHERE isEnabled = 1 ORDER BY hour24 ASC, minute ASC, createdAt DESC")
    suspend fun getEnabled(): List<AlarmConfig>
}