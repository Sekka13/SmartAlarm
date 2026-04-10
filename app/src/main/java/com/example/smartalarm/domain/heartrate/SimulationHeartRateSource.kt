package com.example.smartalarm.domain.heartrate

import android.content.Context
import com.example.smartalarm.domain.session.SessionReplayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.max
import kotlin.math.roundToLong

class SimulationHeartRateSource(
    private val context: Context,
    private val replayMode: SessionReplayMode = SessionReplayMode.CSV_REALTIME
) : HeartRateSource {

    private data class CsvHeartRateEntry(
        val offsetMillis: Long,
        val bpm: Int
    )

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var running = false

    private var index = 0
    private var bpmSamples: List<CsvHeartRateEntry> = emptyList()

    override fun start(onBpmReceived: (bpm: Int, timestamp: Long) -> Unit) {
        if (running) return

        running = true
        index = 0

        if (bpmSamples.isEmpty()) {
            bpmSamples = loadBpmFromCsv()
        }

        if (bpmSamples.isEmpty()) {
            running = false
            return
        }

        val sessionStartTime = System.currentTimeMillis()

        job = scope.launch {
            while (running && index < bpmSamples.size) {
                val currentSample = bpmSamples[index]
                val previousSample = bpmSamples.getOrNull(index - 1)

                val waitMillis = if (previousSample == null) {
                    0L
                } else {
                    buildWaitMillis(
                        currentOffsetMillis = currentSample.offsetMillis,
                        previousOffsetMillis = previousSample.offsetMillis
                    )
                }

                if (waitMillis > 0L) {
                    delay(waitMillis)
                }

                val effectiveTimestamp = sessionStartTime + currentSample.offsetMillis
                index++

                onBpmReceived(currentSample.bpm, effectiveTimestamp)
            }

            running = false
        }
    }

    override fun stop() {
        running = false
        job?.cancel()
        job = null
    }

    private fun buildWaitMillis(
        currentOffsetMillis: Long,
        previousOffsetMillis: Long
    ): Long {
        val sourceDelta = (currentOffsetMillis - previousOffsetMillis).coerceAtLeast(0L)
        if (sourceDelta == 0L) return 0L

        return max(
            1L,
            (sourceDelta.toDouble() / replayMode.speedMultiplier.toDouble()).roundToLong()
        )
    }

    private fun loadBpmFromCsv(): List<CsvHeartRateEntry> {
        val result = mutableListOf<CsvHeartRateEntry>()

        try {
            context.assets.open("hrb_sample_1.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->

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
                            val offsetMillis = parseCsvTime(parts[1].trim())
                            val bpm = parts[2].trim().toIntOrNull()

                            if (offsetMillis != null && bpm != null) {
                                result.add(
                                    CsvHeartRateEntry(
                                        offsetMillis = offsetMillis,
                                        bpm = bpm
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun parseCsvTime(value: String): Long? {
        val parts = value.split(":")
        if (parts.size != 3) return null

        val hours = parts[0].toLongOrNull() ?: return null
        val minutes = parts[1].toLongOrNull() ?: return null
        val seconds = parts[2].toLongOrNull() ?: return null

        return ((hours * 60L + minutes) * 60L + seconds) * 1_000L
    }
}
