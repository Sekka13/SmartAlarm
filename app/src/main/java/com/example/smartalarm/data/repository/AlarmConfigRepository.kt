package com.example.smartalarm.data.repository

import com.example.smartalarm.data.db.AlarmConfigDao
import com.example.smartalarm.data.model.AlarmConfig

class AlarmConfigRepository(
    private val dao: AlarmConfigDao
) {
    suspend fun insert(alarm: AlarmConfig): Long = dao.insert(alarm)

    suspend fun update(alarm: AlarmConfig) = dao.update(alarm)

    suspend fun delete(alarm: AlarmConfig) = dao.delete(alarm)

    suspend fun getAll(): List<AlarmConfig> = dao.getAll()

    suspend fun getById(alarmId: Long): AlarmConfig? = dao.getById(alarmId)

    suspend fun getEnabled(): List<AlarmConfig> = dao.getEnabled()
}