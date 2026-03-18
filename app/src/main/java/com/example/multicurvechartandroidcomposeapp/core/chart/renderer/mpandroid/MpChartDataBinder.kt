package com.example.multicurvechartandroidcomposeapp.core.chart.renderer.mpandroid

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.toArgb
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.max

import com.example.multicurvechartandroidcomposeapp.core.chart.domain.ChartDownsampler
import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartSeries


fun LineChart.setSeries(series: List<ChartSeries>) {
    val dataSets = series.mapIndexed { index, s ->
        val entries = s.points.map { Entry(it.x.toFloat(), it.y.toFloat()) }
        LineDataSet(entries, s.name).apply {
            color = s.color.toArgb()
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR

            // Vertical cursor + sync selection between curves.
            highLightColor = AndroidColor.DKGRAY
            highlightLineWidth = 1f
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(true)
        }
    }

    data = LineData(dataSets)
    notifyDataSetChanged()

    val firstX = series.firstOrNull()?.points?.firstOrNull()?.x ?: 0f
    val lastX = series.firstOrNull()?.points?.lastOrNull()?.x ?: 100f
    xAxis.axisMinimum = firstX.toFloat()
    xAxis.axisMaximum = lastX.toFloat()

    enforceHorizontalOnlyViewport()
    updateVisibleYRange()
}

fun LineChart.setReducedSeries(series: List<ChartSeries>) {
    val visibleMinX = lowestVisibleX.takeIf { it.isFinite() } ?: (series.firstOrNull()?.points?.firstOrNull()?.x ?: 0f)
    val visibleMaxX = highestVisibleX.takeIf { it.isFinite() } ?: (series.firstOrNull()?.points?.lastOrNull()?.x ?: 0f)
    val contentWidthPx = viewPortHandler.contentRect.width().takeIf { it > 0f } ?: 1000f
    val targetBuckets = max(32, (contentWidthPx / 2f).toInt())

    val range = visibleMaxX.toDouble() - visibleMinX.toDouble()

    val dataSets = series.map { s ->
        // Add range +/- 10% to avoid visual glitches due to series decimation
        val reducedEntries = ChartDownsampler.sampleMinMax(
            points = s.points,
            visibleMinX = visibleMinX.toDouble() - range / 10,
            visibleMaxX = visibleMaxX.toDouble() + range / 10,
            targetBuckets = targetBuckets
        ).map { Entry(it.x.toFloat(), it.y.toFloat()) }

        LineDataSet(reducedEntries, s.name).apply {
            color = s.color.toArgb()
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
            highLightColor = AndroidColor.DKGRAY
            highlightLineWidth = 1f
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(true)
        }
    }

    data = LineData(dataSets)
    notifyDataSetChanged()

    val firstX = series.firstOrNull()?.points?.firstOrNull()?.x ?: 0f
    val lastX = series.firstOrNull()?.points?.lastOrNull()?.x ?: 100f
    xAxis.axisMinimum = firstX.toFloat()
    xAxis.axisMaximum = lastX.toFloat()

    enforceHorizontalOnlyViewport()
}
