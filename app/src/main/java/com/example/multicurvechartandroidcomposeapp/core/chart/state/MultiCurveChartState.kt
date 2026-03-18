package com.example.multicurvechartandroidcomposeapp.core.chart.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class MultiCurveChartState(
    val viewport: ChartViewportState = ChartViewportState()
) {
    var selectedX by mutableStateOf<Double?>(null)
    var lastTapTimestamp by mutableLongStateOf(0L)

    fun clearSelection() {
        selectedX = null
    }

    fun resetZoom() {
        viewport.reset()
    }

    fun zoomBy(scaleFactorX: Float, focusX: Float) {
        viewport.zoomBy(scaleFactorX, focusX)
    }

    fun pan(deltaX: Float) {
        viewport.panBy(deltaX)
    }
}

/* ============================================================
 * CHART STATE
 * ============================================================ */

@Composable
fun rememberMultiCurveChartState(
    viewport: ChartViewportState = rememberChartViewportState()
): MultiCurveChartState = remember(viewport) { MultiCurveChartState(viewport) }