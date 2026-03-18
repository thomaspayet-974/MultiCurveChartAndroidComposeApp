package com.example.multicurvechartandroidcomposeapp.core.chart.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ChartSeries(
    val id: String,
    val name: String,
    val color: Color,
    val points: List<ChartPoint>,
    val enabled: Boolean = true
) {
    init {
        // Data sorted with X.
    }
}
