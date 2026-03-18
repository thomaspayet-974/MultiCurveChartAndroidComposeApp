package com.example.multicurvechartandroidcomposeapp.core.chart.renderer.mpandroid

import com.github.mikephil.charting.charts.LineChart

import com.example.multicurvechartandroidcomposeapp.core.chart.state.MultiCurveChartState


fun LineChart.enforceHorizontalOnlyViewport() {
    val matrix = viewPortHandler.matrixTouch
    val values = FloatArray(9)
    matrix.getValues(values)

    values[android.graphics.Matrix.MSCALE_Y] = 1f
    values[android.graphics.Matrix.MSKEW_Y] = 0f
    values[android.graphics.Matrix.MSKEW_X] = 0f
    values[android.graphics.Matrix.MTRANS_Y] = 0f

    matrix.setValues(values)
    viewPortHandler.refresh(matrix, this, true)
}

fun LineChart.updateVisibleYRange() {
    val lineData = data ?: return
    val dataSet = lineData.getDataSetByIndex(0) ?: return
    if (dataSet.entryCount == 0) return

    val lowestVisibleX = lowestVisibleX
    val highestVisibleX = highestVisibleX

    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    for (dataSetIndex in 0 until lineData.dataSetCount) {
        val dataSet = lineData.getDataSetByIndex(dataSetIndex)
        for (i in 0 until dataSet.entryCount) {
            val e = dataSet.getEntryForIndex(i)
            if (e.x in lowestVisibleX..highestVisibleX) {
                if (e.y < minY) minY = e.y
                if (e.y > maxY) maxY = e.y
            }
        }
    }

    if (minY == Float.POSITIVE_INFINITY || maxY == Float.NEGATIVE_INFINITY) {
        return
    }

    val range = (maxY - minY).takeIf { it > 0f } ?: 1f
    val padding = range * 0.1f

    axisLeft.axisMinimum = minY - padding
    axisLeft.axisMaximum = maxY + padding

    notifyDataSetChanged()
    invalidate()
}

fun syncViewportFromChart(chart: LineChart, state: MultiCurveChartState) {
    if (chart.lowestVisibleX.isFinite() && chart.highestVisibleX.isFinite()) {
        val minX = chart.lowestVisibleX
        val maxX = chart.highestVisibleX
        if (maxX > minX) {
            state.viewport.setDataBounds(state.viewport.dataMinX, state.viewport.dataMaxX)
            state.viewport.panBy(0f) // clamp
            val currentWidth = state.viewport.visibleWidth()
            val targetWidth = (maxX - minX)
            if (targetWidth > 0f) {
                state.viewport.visibleMinX = minX
                state.viewport.visibleMaxX = maxX
            }
        }
    }
}
