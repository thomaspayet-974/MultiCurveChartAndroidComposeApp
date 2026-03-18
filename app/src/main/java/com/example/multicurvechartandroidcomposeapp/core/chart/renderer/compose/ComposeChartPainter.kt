package com.example.multicurvechartandroidcomposeapp.core.chart.renderer.compose

import android.graphics.Color as AndroidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

import com.example.multicurvechartandroidcomposeapp.core.chart.domain.formatCompact

fun DrawScope.drawGrid(
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float,
    xTicks: Int,
    yTicks: Int
) {
    val gridColor = Color(0xFFB0B8C4).copy(alpha = 0.22f)
    val stroke = 1.dp.toPx()

    for (i in 0..xTicks) {
        val x = plotLeft + (i / xTicks.toFloat()) * plotWidth
        drawLine(
            color = gridColor,
            start = Offset(x, plotTop),
            end = Offset(x, plotTop + plotHeight),
            strokeWidth = stroke
        )
    }

    for (i in 0..yTicks) {
        val y = plotTop + (i / yTicks.toFloat()) * plotHeight
        drawLine(
            color = gridColor,
            start = Offset(plotLeft, y),
            end = Offset(plotLeft + plotWidth, y),
            strokeWidth = stroke
        )
    }
}

fun DrawScope.drawAxes(
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float
) {
    val axisColor = Color(0xFF7C8593)
    val stroke = 1.2.dp.toPx()

    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotTop + plotHeight),
        end = Offset(plotLeft + plotWidth, plotTop + plotHeight),
        strokeWidth = stroke
    )
    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotTop),
        end = Offset(plotLeft, plotTop + plotHeight),
        strokeWidth = stroke
    )
}

fun DrawScope.drawAxisLabels(
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float
) {
    val textPaint = android.graphics.Paint().apply {
        color = AndroidColor.argb(180, 90, 98, 110)
        textSize = 11.dp.toPx()
        isAntiAlias = true
    }

    val xTicks = 6
    val yTicks = 5

    for (i in 0..xTicks) {
        val ratio = i / xTicks.toFloat()
        val value = xMin + (xMax - xMin) * ratio
        val x = plotLeft + ratio * plotWidth
        drawContext.canvas.nativeCanvas.drawText(
            formatCompact(value),
            x - 18.dp.toPx(),
            plotTop + plotHeight + 16.dp.toPx(),
            textPaint
        )
    }

    for (i in 0..yTicks) {
        val ratio = i / yTicks.toFloat()
        val value = yMax - (yMax - yMin) * ratio
        val y = plotTop + ratio * plotHeight
        drawContext.canvas.nativeCanvas.drawText(
            formatCompact(value),
            4.dp.toPx(),
            y + 4.dp.toPx(),
            textPaint
        )
    }
}
