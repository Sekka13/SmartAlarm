package com.example.smartalarm.ui.charts

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import com.example.smartalarm.R

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

import kotlin.math.max

class SleepChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val chart = LineChart(context)
    private val chartRenderer: SleepChartRenderer

    private val points = mutableListOf<SleepChartPoint>()
    private var chartMode: SleepChartMode = SleepChartMode.DETAIL
    private var showPhaseOverlay: Boolean = true

    init {
        addView(
            chart,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        chartRenderer = SleepChartRenderer(
            chart = chart,
            animator = chart.animator,
            viewPortHandler = chart.viewPortHandler
        )

        chart.renderer = chartRenderer
        setupChart()
        applyMode()
    }

    fun setSessionData(newPoints: List<SleepChartPoint>) {
        points.clear()
        points.addAll(newPoints.sortedBy { it.timestamp })
        renderChart()
    }

    fun addLivePoint(point: SleepChartPoint) {
        points.add(point)
        points.sortBy { it.timestamp }
        renderChart(scrollToEnd = true)
    }

    fun clearData() {
        points.clear()
        chart.clear()
        chartRenderer.setPhaseSegments(emptyList())
        chart.invalidate()
    }

    fun setChartMode(mode: SleepChartMode) {
        chartMode = mode
        applyMode()
        renderChart()
    }

    fun setShowPhaseOverlay(show: Boolean) {
        showPhaseOverlay = show
        chartRenderer.setShowPhaseOverlay(show)
        chart.invalidate()
    }

    fun getChart(): LineChart = chart

    private fun setupChart() {
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setNoDataText("No session data available")
        chart.setNoDataTextColor(Color.WHITE)
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setViewPortOffsets(48f, 24f, 24f, 40f)

        chart.axisRight.isEnabled = false

        chart.axisLeft.apply {
            textColor = Color.WHITE
            gridColor = Color.parseColor("#22FFFFFF")
            axisLineColor = Color.parseColor("#44FFFFFF")
            granularity = 10f
            setDrawZeroLine(false)
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.WHITE
            gridColor = Color.TRANSPARENT
            axisLineColor = Color.parseColor("#44FFFFFF")
            granularity = 1f
            setDrawGridLines(false)
            labelRotationAngle = 0f
        }

        chart.legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            orientation = Legend.LegendOrientation.HORIZONTAL
            textColor = Color.WHITE
            isEnabled = false
        }

        chartRenderer.setShowPhaseOverlay(showPhaseOverlay)
    }

    private fun applyMode() {
        when (chartMode) {
            SleepChartMode.LIVE -> {
                chart.setTouchEnabled(false)
                chart.isDragEnabled = false
                chart.setScaleEnabled(false)
                chart.axisLeft.isEnabled = true
                chart.xAxis.isEnabled = true
                chart.setViewPortOffsets(48f, 24f, 24f, 40f)
            }

            SleepChartMode.DETAIL -> {
                chart.setTouchEnabled(false)
                chart.isDragEnabled = false
                chart.setScaleEnabled(false)
                chart.axisLeft.isEnabled = true
                chart.xAxis.isEnabled = true
                chart.setViewPortOffsets(48f, 24f, 24f, 40f)
            }

            SleepChartMode.COMPACT -> {
                chart.setTouchEnabled(false)
                chart.isDragEnabled = false
                chart.setScaleEnabled(false)
                chart.axisLeft.isEnabled = false
                chart.xAxis.isEnabled = false
                chart.legend.isEnabled = false
                chart.setViewPortOffsets(8f, 8f, 8f, 8f)
            }
        }
    }

    private fun renderChart(scrollToEnd: Boolean = false) {
        if (points.isEmpty()) {
            chart.clear()
            chartRenderer.setPhaseSegments(emptyList())
            chart.invalidate()
            return
        }

        val firstTimestamp = points.first().timestamp
        val entries = points.map { point ->
            Entry(
                timeToX(point.timestamp, firstTimestamp),
                point.bpm.toFloat()
            )
        }

        val dataSet = LineDataSet(entries, "BPM").apply {
            color = Color.parseColor("#C1C1FF")
            lineWidth = when (chartMode) {
                SleepChartMode.COMPACT -> 1.8f
                else -> 2.5f
            }
            setDrawCircles(false)
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.18f
            setDrawFilled(false)
            highLightColor = Color.parseColor("#FFB74D")
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(false)
        }

        val lineData = LineData(dataSet)
        chart.data = lineData

        val xMax = entries.last().x
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = max(1f, xMax)

        val minBpm = points.minOf { it.bpm }.toFloat()
        val maxBpm = points.maxOf { it.bpm }.toFloat()
        chart.axisLeft.axisMinimum = max(0f, minBpm - 8f)
        chart.axisLeft.axisMaximum = maxBpm + 8f

        chart.xAxis.valueFormatter = buildXAxisFormatter(firstTimestamp)

        val segments = buildPhaseSegments(points, firstTimestamp)
        chartRenderer.setPhaseSegments(segments)
        chartRenderer.setShowPhaseOverlay(showPhaseOverlay)

        chart.notifyDataSetChanged()
        chart.invalidate()

        if (scrollToEnd && chartMode == SleepChartMode.LIVE) {
            chart.moveViewToX(xMax)
        }
    }

    private fun buildXAxisFormatter(firstTimestamp: Long): ValueFormatter {
        return object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (chartMode) {
                    SleepChartMode.COMPACT -> ""
                    SleepChartMode.LIVE,
                    SleepChartMode.DETAIL -> {
                        val absoluteTime = firstTimestamp + (value * 60_000L).toLong()
                        formatTimeLabel(absoluteTime)
                    }
                }
            }
        }
    }

    private fun buildPhaseSegments(
        source: List<SleepChartPoint>,
        firstTimestamp: Long
    ): List<SleepPhaseSegment> {
        if (source.size < 2) return emptyList()

        val segments = mutableListOf<SleepPhaseSegment>()

        var currentPhase = source.first().phase
        var segmentStartX = timeToX(source.first().timestamp, firstTimestamp)

        for (i in 1 until source.size) {
            val point = source[i]
            val pointX = timeToX(point.timestamp, firstTimestamp)

            if (point.phase != currentPhase) {
                segments.add(
                    SleepPhaseSegment(
                        startX = segmentStartX,
                        endX = pointX,
                        phase = currentPhase
                    )
                )
                currentPhase = point.phase
                segmentStartX = pointX
            }

            if (i == source.lastIndex) {
                segments.add(
                    SleepPhaseSegment(
                        startX = segmentStartX,
                        endX = pointX,
                        phase = currentPhase
                    )
                )
            }
        }

        return segments
    }

    private fun timeToX(timestamp: Long, firstTimestamp: Long): Float {
        return ((timestamp - firstTimestamp) / 60_000f)
    }

    private fun formatTimeLabel(timestamp: Long): String {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }
}