package com.example.multicurvechartandroidcomposeapp.core.chart.model

import androidx.compose.runtime.Immutable

@Immutable
data class SelectedPointInfo(
    val x: Double,
    val perSeries: List<SeriesSelection>
)
