package com.example.multicurvechartandroidcomposeapp.core.chart.renderer.mpandroid

import android.graphics.Canvas
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class CustomLineChartHighlightRenderer(
    chart: LineChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : LineChartRenderer(chart, animator, viewPortHandler) {

    private val highlightPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {
        super.drawHighlighted(c, indices)

        val lineData = mChart.lineData ?: return

        for (high in indices) {
            val set = lineData.getDataSetByIndex(high.dataSetIndex)
            val entry = set.getEntryForXValue(high.x, high.y)
            val color = set.color

            if (entry != null) {
                val pt = mChart.getTransformer(set.axisDependency)
                    .getPixelForValues(entry.x, entry.y)

                highlightPaint.color = color

                // Draw a circle on every curve for selected X (highlighted)
                c.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 8f, highlightPaint)
            }
        }
    }
}
