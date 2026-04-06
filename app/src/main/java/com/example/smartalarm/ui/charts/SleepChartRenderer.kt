package com.example.smartalarm.ui.charts

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class SleepChartRenderer(
    chart: LineChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : LineChartRenderer(chart, animator, viewPortHandler) {

    private val phasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val segmentRect = RectF()

    private var segments: List<SleepPhaseSegment> = emptyList()
    private var showPhaseOverlay: Boolean = true

    fun setPhaseSegments(segments: List<SleepPhaseSegment>) {
        this.segments = segments
    }

    fun setShowPhaseOverlay(show: Boolean) {
        this.showPhaseOverlay = show
    }

    override fun drawData(c: Canvas) {
        if (showPhaseOverlay) {
            drawPhaseBackgrounds(c)
        }
        super.drawData(c)
    }

    private fun drawPhaseBackgrounds(canvas: Canvas) {
        val chart = mChart as? LineChart ?: return
        if (segments.isEmpty()) return

        val transformer = chart.getTransformer(chart.axisLeft.axisDependency)
        val content = mViewPortHandler.contentRect

        for (segment in segments) {
            val pts = floatArrayOf(
                segment.startX, 0f,
                segment.endX, 0f
            )
            transformer.pointValuesToPixel(pts)

            val left = pts[0].coerceIn(content.left, content.right)
            val right = pts[2].coerceIn(content.left, content.right)

            if (right <= left) continue

            phasePaint.color = getPhaseColor(segment.phase)

            segmentRect.set(
                left,
                content.top,
                right,
                content.bottom
            )

            canvas.drawRect(segmentRect, phasePaint)
        }
    }

    private fun getPhaseColor(phase: String): Int {
        return when (phase.uppercase()) {
            "DEEP" -> Color.parseColor("#33231EBB")
            "LIGHT" -> Color.parseColor("#33C1C1FF")
            "REM" -> Color.parseColor("#33602B9D")
            "WAKE" -> Color.parseColor("#33FFB74D")
            else -> Color.parseColor("#2234343B")
        }
    }
}