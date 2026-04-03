package com.example.smartalarm.domain.algorithm

import kotlin.math.sqrt

object SleepPhaseDetector {

    enum class Phase {
        WAKE,
        LIGHT,
        DEEP,
        REM
    }

    private val bpmWindow = mutableListOf<Int>()
    // Número de muestras que queremos conservar.
    private const val WINDOW_SIZE = 30
    private const val MIN_SAMPLES_FOR_STABLE_CLASSIFICATION = 5

    fun detectPhase(bpm: Int): Phase {

        // Añadimos la nueva muestra a la ventana
        bpmWindow.add(bpm)

        // Si superamos el tamaño máximo, eliminamos la más antigua
        if (bpmWindow.size > WINDOW_SIZE) {
            bpmWindow.removeAt(0)
        }
        if (bpmWindow.size < MIN_SAMPLES_FOR_STABLE_CLASSIFICATION) {
            return Phase.LIGHT
        }
        // Media de BPM recientes.
        // Representa el nivel "normal" reciente del usuario.
        val avg = bpmWindow.average()

        // Desviación estándar de la ventana.
        // Mide cuánto se dispersan los BPM recientes respecto a su media.
        val std = calculateStdDev()

        // HRV aproximada a partir de los cambios entre muestras.
        // Cuanto más estable sea el pulso, menor será este valor.
        val hrv = calculateHRV()

        // z-score:
        // indica cuánto se aleja el BPM actual de la media reciente,
        // expresado en unidades de desviación estándar.
        //
        // Ejemplo:
        // z > 0  -> BPM por encima de la media
        // z < 0  -> BPM por debajo de la media
        // z muy alto -> posible vigilia
        // z muy bajo -> posible sueño más profundo
        val z = if (std > 0) {
            (bpm - avg) / std
        } else {
            // Si std = 0 significa que no hay variación suficiente aún
            // (por ejemplo al principio de la sesión), así que evitamos
            // dividir entre 0 y usamos un valor neutro.
            0.0
        }

        // Clasificación de fase:
        //
        // 1) WAKE:
        //    Si el BPM actual está claramente por encima del patrón reciente,
        //    interpretamos que el usuario está despierto o en activación.
        //
        // 2) DEEP:
        //    Si el BPM está muy por debajo de la media reciente
        //    y además el HRV es bajo, interpretamos sueño profundo:
        //    pulso bajo + señal estable.
        //
        // 3) REM:
        //    Si el BPM está por debajo de la media,
        //    pero no tan extremo como en DEEP,
        //    clasificamos como REM.
        //
        // 4) LIGHT:
        //Todo lo demás se considera sueño ligero
        return when {

            // Pulso claramente por encima del comportamiento reciente
            z > 1.5 -> Phase.WAKE

            // Pulso muy por debajo de la media y bastante estable
            z < -1.5 && hrv < 2.0 -> Phase.DEEP

            // Pulso por debajo de la media pero no tan estable/extremo
            z < -0.5 -> Phase.REM

            // Estado intermedio
            else -> Phase.LIGHT
        }
    }

    /**
     * Calcula la desviación estándar de la ventana de BPM.
     *
     * Cuanto mayor sea, más dispersas están las muestras.
     * Cuanto menor sea, más parecidas son entre sí.
     */
    private fun calculateStdDev(): Double {

        if (bpmWindow.size < 2) return 0.0

        val mean = bpmWindow.average()

        val variance = bpmWindow
            .map { (it - mean) * (it - mean) }
            .average()

        return sqrt(variance)
    }

    /**
     * Calcula una aproximación sencilla de HRV.
     *
     * No usamos intervalos RR reales porque el wearable en esta fase
     * del proyecto solo nos da BPM. Por eso aproximamos la variabilidad
     * cardíaca usando los cambios entre muestras consecutivas de BPM.
     *
     * Cuanto menor sea este valor:
     * - más estable está la señal
     *
     * Cuanto mayor sea:
     * - más oscilaciones hay entre muestras
     */
    private fun calculateHRV(): Double {

        if (bpmWindow.size < 3) return 0.0

        val squaredDiffs = mutableListOf<Double>()

        for (i in 1 until bpmWindow.size) {
            val diff = (bpmWindow[i] - bpmWindow[i - 1]).toDouble()
            squaredDiffs.add(diff * diff)
        }

        return sqrt(squaredDiffs.average())
    }

    /**
     * Reinicia el detector al comenzar una nueva sesión.
     * Es importante para que una sesión no herede el contexto de la anterior.
     */
    fun reset() {
        bpmWindow.clear()
    }
}