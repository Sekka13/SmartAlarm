package com.example.smartalarm.domain.algorithm

object SmartAlarmAlgorithm {

    /**
     * Decide si conviene disparar la alarma en este instante.
     *
     * Reglas:
     * - Si aún no hemos entrado en la ventana, no despertamos
     * - Dentro de la ventana, preferimos despertar en LIGHT o WAKE
     * - Si ya hemos alcanzado la hora exacta de la alarma, despertamos sí o sí
     */
    fun shouldTrigger(
        phase: SleepPhaseDetector.Phase,
        now: Long,
        alarmTime: Long,
        alarmWindow: Long
    ): Boolean {
        if (alarmTime <= 0L || alarmTime == Long.MAX_VALUE) {
            return false
        }

        val windowStart = alarmTime - alarmWindow

        if (now < windowStart) {
            return false
        }

        if (now >= alarmTime) {
            return true
        }

        return phase == SleepPhaseDetector.Phase.LIGHT ||
                phase == SleepPhaseDetector.Phase.WAKE
    }
}