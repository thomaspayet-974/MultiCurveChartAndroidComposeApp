package com.example.multicurvechartandroidcomposeapp.core.chart.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.math.min


@Stable
class ChartViewportState {
    var dataMinX by mutableFloatStateOf(0f)
        private set
    var dataMaxX by mutableFloatStateOf(1f)
        private set

    var visibleMinX by mutableFloatStateOf(0f)
        set
    var visibleMaxX by mutableFloatStateOf(1f)
        set

    var initialized by mutableStateOf(false)
        private set

    fun setDataBounds(minX: Float, maxX: Float, reset: Boolean = false) {
        val safeMin = min(minX, maxX)
        val safeMax = max(maxX, minX + 1e-6f)

        dataMinX = safeMin
        dataMaxX = safeMax

        if (!initialized || reset) {
            visibleMinX = safeMin
            visibleMaxX = safeMax
            initialized = true
        } else {
            clampToBounds()
        }
    }

    fun visibleWidth(): Float = max(visibleMaxX - visibleMinX, 1e-6f)

    fun reset() {
        visibleMinX = dataMinX
        visibleMaxX = dataMaxX
        clampToBounds()
    }

    fun panBy(deltaXDomain: Float) {
        visibleMinX += deltaXDomain
        visibleMaxX += deltaXDomain
        clampToBounds()
    }

    fun zoomBy(scaleFactorX: Float, focusX: Float) {
        val currentWidth = visibleWidth()
        val newWidth = (currentWidth / scaleFactorX)
            .coerceAtLeast((dataMaxX - dataMinX) / 10_000f)
            .coerceAtMost(dataMaxX - dataMinX)

        val ratio = ((focusX - visibleMinX) / currentWidth).coerceIn(0f, 1f)

        visibleMinX = focusX - ratio * newWidth
        visibleMaxX = visibleMinX + newWidth
        clampToBounds()
    }

    private fun clampToBounds() {
        val total = max(dataMaxX - dataMinX, 1e-6f)
        val width = visibleWidth().coerceIn(total / 10_000f, total)

        if (width >= total) {
            visibleMinX = dataMinX
            visibleMaxX = dataMaxX
            return
        }

        if (visibleMinX < dataMinX) {
            visibleMinX = dataMinX
            visibleMaxX = visibleMinX + width
        }
        if (visibleMaxX > dataMaxX) {
            visibleMaxX = dataMaxX
            visibleMinX = visibleMaxX - width
        }

        visibleMinX = visibleMinX.coerceIn(dataMinX, dataMaxX)
        visibleMaxX = visibleMaxX.coerceIn(dataMinX, dataMaxX)
    }
}

/* ============================================================
 * VIEWPORT STATE
 * ============================================================ */

@Composable
fun rememberChartViewportState(): ChartViewportState = remember { ChartViewportState() }