package com.example.smartalarm.domain.heartrate

interface HeartRateSource {

    fun start(onBpmReceived: (bpm: Int, timestamp: Long) -> Unit)

    fun stop()
}