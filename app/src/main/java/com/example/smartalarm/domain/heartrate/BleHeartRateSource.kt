package com.example.smartalarm.domain.heartrate

class BleHeartRateSource : HeartRateSource {

    override fun start(onSampleReceived: (bpm: Int, timestamp: Long) -> Unit) {
        // Implementación BLE (pendiente)

        // onSampleReceived(bpm, System.currentTimeMillis())
        //onSampleReceived(bpm, timestamp, null)
    }

    override fun stop() {
        //Cerrar conexion BLE
    }
}