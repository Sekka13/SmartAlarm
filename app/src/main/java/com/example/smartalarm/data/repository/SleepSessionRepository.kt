package com.example.smartalarm.data.repository

import com.example.smartalarm.data.db.SleepSessionDao
import com.example.smartalarm.data.model.SleepSession

class SleepSessionRepository(
    private val dao: SleepSessionDao
) {

    suspend fun insert(session: SleepSession) {
        dao.insert(session)
    }

    suspend fun getAll(): List<SleepSession> {
        return dao.getAll()
    }
}