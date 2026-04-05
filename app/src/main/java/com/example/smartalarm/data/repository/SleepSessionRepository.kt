package com.example.smartalarm.data.repository

import com.example.smartalarm.data.db.SleepSessionDao
import com.example.smartalarm.data.model.SleepSession

class SleepSessionRepository(
    private val dao: SleepSessionDao
) {
    suspend fun insert(session: SleepSession): Long = dao.insert(session)

    suspend fun getAll(): List<SleepSession> = dao.getAll()

    suspend fun getSessionsFrom(fromTime: Long): List<SleepSession> = dao.getSessionsFrom(fromTime)

    suspend fun getById(id: Long): SleepSession? = dao.getById(id)
}