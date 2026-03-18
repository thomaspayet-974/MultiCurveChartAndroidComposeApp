package com.example.multicurvechartandroidcomposeapp.core.chart.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SeriesSelection(
    val seriesId: String,
    val seriesName: String,
    val color: Color,
    val point: ChartPoint
)