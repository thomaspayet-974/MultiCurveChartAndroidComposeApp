package com.example.multicurvechartandroidcomposeapp.core.chart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartSeries
import com.example.multicurvechartandroidcomposeapp.core.chart.renderer.compose.ComposeCanvasMultiCurveChart
import com.example.multicurvechartandroidcomposeapp.core.chart.renderer.mpandroid.MPAndroidMultiCurveChart
import com.example.multicurvechartandroidcomposeapp.core.chart.state.MultiCurveChartState
import com.example.multicurvechartandroidcomposeapp.core.chart.state.rememberMultiCurveChartState


/* ============================================================
 * CHART RENDERER
 * ============================================================ */

enum class ChartRenderer {
    ComposeCanvas,
    MPAndroidChart
}

/* ============================================================
 * PUBLIC ENTRY POINT
 * ============================================================ */

@Composable
fun MultiCurveChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    state: MultiCurveChartState = rememberMultiCurveChartState(),
    renderer: ChartRenderer = ChartRenderer.ComposeCanvas,
    showAxes: Boolean = true,
    showGrid: Boolean = true,
    showCrosshair: Boolean = true
) {

    if (series.isEmpty()) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucune série active")
        }
        return
    }

    val globalMinX = series.minOf { it.points.first().x }.toFloat()
    val globalMaxX = series.maxOf { it.points.last().x }.toFloat()

    state.viewport.setDataBounds(globalMinX, globalMaxX)

    when (renderer) {
        ChartRenderer.ComposeCanvas -> {
            ComposeCanvasMultiCurveChart(
                series = series,
                modifier = modifier,
                state = state,
                showAxes = showAxes,
                showGrid = showGrid,
                showCrosshair = showCrosshair
            )
        }

        ChartRenderer.MPAndroidChart -> {
            MPAndroidMultiCurveChart(
                series = series,
                modifier = modifier,
                state = state,
                showAxes = showAxes,
                showGrid = showGrid
            )
        }
    }
}
