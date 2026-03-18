package com.example.multicurvechartandroidcomposeapp.core.chart.domain

import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartSeries
import com.example.multicurvechartandroidcomposeapp.core.chart.model.SelectedPointInfo
import com.example.multicurvechartandroidcomposeapp.core.chart.model.SeriesSelection

fun buildSelectionInfo(
    series: List<ChartSeries>,
    x: Double
): SelectedPointInfo? {
    val selected = series.mapNotNull { s ->
        val point = ChartDownsampler.nearestPointByX(s.points, x) ?: return@mapNotNull null
        SeriesSelection(
            seriesId = s.id,
            seriesName = s.name,
            color = s.color,
            point = point
        )
    }

    if (selected.isEmpty()) return null

    // Cursor aligned on X for the first selected point.
    val anchorX = selected.first().point.x
    return SelectedPointInfo(x = anchorX, perSeries = selected)
}