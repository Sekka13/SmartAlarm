package com.example.smartalarm.ui.view



import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.example.smartalarm.domain.algorithm.SleepPhaseDetector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class SleepChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class ChartPoint(
        val timeMillis: Long,
        val bpm: Int,
        val phase: SleepPhaseDetector.Phase,

        // NUEVO:
        // fase interna del simulador; null si no existe
        val simulatedPhase: String? = null
    )

    private val points = mutableListOf<ChartPoint>()

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // NUEVO:
    // borde del punto para mostrar fase simulada
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = 26f
        color = Color.BLACK
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val leftPaddingPx = 110f
    private val rightPaddingPx = 40f
    private val topPaddingPx = 90f
    private val bottomPaddingPx = 90f

    fun addPoint(
        timeMillis: Long,
        bpm: Int,
        phase: SleepPhaseDetector.Phase,

        // NUEVO:
        simulatedPhase: String? = null
    ) {
        points.add(
            ChartPoint(
                timeMillis = timeMillis,
                bpm = bpm,
                phase = phase,
                simulatedPhase = simulatedPhase
            )
        )
        invalidate()
    }

    fun clearData() {
        points.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val chartLeft = leftPaddingPx
        val chartTop = topPaddingPx
        val chartRight = width - rightPaddingPx
        val chartBottom = height - bottomPaddingPx

        if (chartRight <= chartLeft || chartBottom <= chartTop) return

        drawAxes(canvas, chartLeft, chartTop, chartRight, chartBottom)
        drawLegend(canvas)

        if (points.isEmpty()) {
            canvas.drawText("No data yet", chartLeft + 20f, chartTop + 50f, textPaint)
            return
        }

        val minBpmRaw = points.minOf { it.bpm }
        val maxBpmRaw = points.maxOf { it.bpm }

        val minBpm = max(30, minBpmRaw - 5)
        val maxBpm = min(180, maxBpmRaw + 5)
        val bpmRange = max(1, maxBpm - minBpm)

        val minTime = points.first().timeMillis
        val maxTime = points.last().timeMillis
        val timeRange = max(1L, maxTime - minTime)

        drawHorizontalGridAndLabels(
            canvas = canvas,
            chartLeft = chartLeft,
            chartTop = chartTop,
            chartRight = chartRight,
            chartBottom = chartBottom,
            minBpm = minBpm,
            maxBpm = maxBpm
        )

        drawTimeLabels(
            canvas = canvas,
            chartLeft = chartLeft,
            chartRight = chartRight,
            chartBottom = chartBottom,
            minTime = minTime,
            maxTime = maxTime
        )

        val path = Path()

        points.forEachIndexed { index, point ->
            val x = chartLeft + ((point.timeMillis - minTime).toFloat() / timeRange.toFloat()) * (chartRight - chartLeft)
            val y = chartBottom - ((point.bpm - minBpm).toFloat() / bpmRange.toFloat()) * (chartBottom - chartTop)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, linePaint)

        points.forEach { point ->
            val x = chartLeft + ((point.timeMillis - minTime).toFloat() / timeRange.toFloat()) * (chartRight - chartLeft)
            val y = chartBottom - ((point.bpm - minBpm).toFloat() / bpmRange.toFloat()) * (chartBottom - chartTop)

            // Fase detectada = relleno
            pointPaint.color = phaseColor(point.phase)
            canvas.drawCircle(x, y, 8f, pointPaint)

            // NUEVO:
            // Fase simulada = borde
            point.simulatedPhase?.let { phaseName ->
                outlinePaint.color = simulatedPhaseColor(phaseName)
                canvas.drawCircle(x, y, 13f, outlinePaint)
            }
        }
    }

    private fun drawAxes(
        canvas: Canvas,
        chartLeft: Float,
        chartTop: Float,
        chartRight: Float,
        chartBottom: Float
    ) {
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)

        canvas.drawText("BPM", 20f, chartTop + 20f, textPaint)
        canvas.drawText("Time", chartRight - 60f, height - 20f, textPaint)
    }

    private fun drawHorizontalGridAndLabels(
        canvas: Canvas,
        chartLeft: Float,
        chartTop: Float,
        chartRight: Float,
        chartBottom: Float,
        minBpm: Int,
        maxBpm: Int
    ) {
        val steps = 4

        for (i in 0..steps) {
            val ratio = i / steps.toFloat()
            val y = chartBottom - ratio * (chartBottom - chartTop)
            val bpmLabel = (minBpm + ratio * (maxBpm - minBpm)).toInt()

            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            canvas.drawText(bpmLabel.toString(), 15f, y + 8f, textPaint)
        }
    }

    private fun drawTimeLabels(
        canvas: Canvas,
        chartLeft: Float,
        chartRight: Float,
        chartBottom: Float,
        minTime: Long,
        maxTime: Long
    ) {
        val midTime = minTime + (maxTime - minTime) / 2L

        val startLabel = timeFormat.format(Date(minTime))
        val midLabel = timeFormat.format(Date(midTime))
        val endLabel = timeFormat.format(Date(maxTime))

        canvas.drawText(startLabel, chartLeft, chartBottom + 45f, textPaint)
        canvas.drawText(midLabel, (chartLeft + chartRight) / 2f - 35f, chartBottom + 45f, textPaint)
        canvas.drawText(endLabel, chartRight - 70f, chartBottom + 45f, textPaint)
    }

    private fun drawLegend(canvas: Canvas) {
        val startX = leftPaddingPx
        val y = 35f
        val spacing = 150f

        drawLegendItem(canvas, startX, y, SleepPhaseDetector.Phase.WAKE, "WAKE")
        drawLegendItem(canvas, startX + spacing, y, SleepPhaseDetector.Phase.LIGHT, "LIGHT")
        drawLegendItem(canvas, startX + spacing * 2, y, SleepPhaseDetector.Phase.DEEP, "DEEP")
        drawLegendItem(canvas, startX + spacing * 3, y, SleepPhaseDetector.Phase.REM, "REM")
    }

    private fun drawLegendItem(
        canvas: Canvas,
        x: Float,
        y: Float,
        phase: SleepPhaseDetector.Phase,
        label: String
    ) {
        pointPaint.color = phaseColor(phase)
        canvas.drawCircle(x, y, 10f, pointPaint)
        canvas.drawText(label, x + 18f, y + 8f, legendPaint)
    }

    private fun phaseColor(phase: SleepPhaseDetector.Phase): Int {
        return when (phase) {
            SleepPhaseDetector.Phase.WAKE -> Color.RED
            SleepPhaseDetector.Phase.LIGHT -> Color.rgb(66, 133, 244)
            SleepPhaseDetector.Phase.DEEP -> Color.rgb(15, 82, 186)
            SleepPhaseDetector.Phase.REM -> Color.MAGENTA
        }
    }

    // NUEVO:
    // Colores de la fase simulada
    private fun simulatedPhaseColor(phase: String): Int {
        return when (phase) {
            "WAKE" -> Color.RED
            "LIGHT" -> Color.rgb(66, 133, 244)
            "DEEP" -> Color.rgb(15, 82, 186)
            "REM" -> Color.MAGENTA
            else -> Color.GRAY
        }
    }
}