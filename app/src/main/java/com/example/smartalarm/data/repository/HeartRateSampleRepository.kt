package com.example.smartalarm.data.repository

import com.example.smartalarm.data.db.HeartRateDao
import com.example.smartalarm.data.model.HeartRateSample

class HeartRateSampleRepository(
    private val dao: HeartRateDao
) {
    suspend fun insert(sample: HeartRateSample): Long = dao.insert(sample)

    suspend fun insertAll(samples: List<HeartRateSample>) = dao.insertAll(samples)

    suspend fun getBySessionId(sessionId: Long): List<HeartRateSample> = dao.getBySessionId(sessionId)
}