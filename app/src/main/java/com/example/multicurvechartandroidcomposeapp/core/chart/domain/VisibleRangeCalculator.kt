package com.example.multicurvechartandroidcomposeapp.core.chart.domain

import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartSeries
import com.example.multicurvechartandroidcomposeapp.core.chart.model.VisibleYRange
import kotlin.collections.forEach
import kotlin.math.abs

fun computeVisibleYRange(
    series: List<ChartSeries>,
    visibleMinX: Double,
    visibleMaxX: Double
): VisibleYRange {
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY

    series.forEach { s ->
        val start = ChartDownsampler.lowerBound(s.points, visibleMinX)
        val end = ChartDownsampler.upperBound(s.points, visibleMaxX)

        if (start < end) {
            for (i in start until end) {
                val y = s.points[i].y
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    if (!minY.isFinite() || !maxY.isFinite()) {
        minY = 0.0
        maxY = 1.0
    }

    if (abs(maxY - minY) < 1e-12) {
        val pad = if (abs(minY) < 1.0) 1.0 else abs(minY) * 0.05
        minY -= pad
        maxY += pad
    } else {
        val pad = (maxY - minY) * 0.08
        minY -= pad
        maxY += pad
    }

    return VisibleYRange(minY, maxY)
}