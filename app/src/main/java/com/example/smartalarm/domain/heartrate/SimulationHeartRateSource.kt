package com.example.smartalarm.domain.heartrate

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class SimulationHeartRateSource : HeartRateSource {

    companion object {
        private const val REAL_DELAY_MS = 10L
        private const val SIMULATED_STEP_MS = 60_000L
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var running = false

    private var index = 0
    private var bpmSamples: List<Int> = emptyList()

    override fun start(onSample: (bpm: Int, timestamp: Long) -> Unit) {
        if (running) return

        running = true
        index = 0

        // cargar CSV solo una vez
        if (bpmSamples.isEmpty()) {
            bpmSamples = loadBpmFromCsv()
        }

        var simulatedTime = System.currentTimeMillis()

        job = scope.launch {
            while (running && index < bpmSamples.size) {

                delay(REAL_DELAY_MS)

                simulatedTime += SIMULATED_STEP_MS

                val bpm = bpmSamples[index]
                index++

                onSample(bpm, simulatedTime)
            }

            running = false
        }
    }

    override fun stop() {
        running = false
        job?.cancel()
        job = null
    }

    private fun loadBpmFromCsv(): List<Int> {

        val result = mutableListOf<Int>()

        try {

            val inputStream =
                javaClass.classLoader?.getResourceAsStream("assets/hrb_sample_1.csv")
                    ?: return emptyList()

            val reader = BufferedReader(InputStreamReader(inputStream))

            var inDataSection = false

            reader.forEachLine { line ->

                val row = line.trim()

                if (!inDataSection) {
                    if (row.startsWith("Sample rate")) {
                        inDataSection = true
                    }
                    return@forEachLine
                }

                val parts = row.split(",")

                if (parts.size >= 3) {
                    val bpm = parts[2].trim().toIntOrNull()
                    if (bpm != null) {
                        result.add(bpm)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }
}