package com.example.multicurvechartandroidcomposeapp.core.chart.renderer.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.collections.forEach
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

import com.example.multicurvechartandroidcomposeapp.core.chart.domain.ChartDownsampler
import com.example.multicurvechartandroidcomposeapp.core.chart.domain.buildSelectionInfo
import com.example.multicurvechartandroidcomposeapp.core.chart.domain.computeVisibleYRange
import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartSeries
import com.example.multicurvechartandroidcomposeapp.core.chart.state.MultiCurveChartState
import com.example.multicurvechartandroidcomposeapp.core.chart.ui.TooltipOverlay


/* ============================================================
 * COMPOSE CANVAS IMPLEMENTATION
 * ============================================================ */

@Composable
fun ComposeCanvasMultiCurveChart(
    series: List<ChartSeries>,
    modifier: Modifier,
    state: MultiCurveChartState,
    showAxes: Boolean,
    showGrid: Boolean,
    showCrosshair: Boolean
) {
    val density = LocalDensity.current

    val leftPadding = with(density) { 56.dp.toPx() }
    val rightPadding = with(density) { 16.dp.toPx() }
    val topPadding = with(density) { 16.dp.toPx() }
    val bottomPadding = with(density) { 28.dp.toPx() }

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(series, state) {
                    detectTapGestures(
                        onDoubleTap = {
                            state.resetZoom()
                            state.clearSelection()
                        },
                        onTap = { tapOffset ->
                            val width = size.width
                            val height = size.height

                            val plotLeft = leftPadding
                            val plotRight = width - rightPadding
                            val plotTop = topPadding
                            val plotBottom = height - bottomPadding
                            val plotWidth = max(plotRight - plotLeft, 1f)

                            if (tapOffset.x !in plotLeft..plotRight || tapOffset.y !in plotTop..plotBottom) {
                                state.clearSelection()
                                return@detectTapGestures
                            }

                            val xValue = state.viewport.visibleMinX +
                                    ((tapOffset.x - plotLeft) / plotWidth) * state.viewport.visibleWidth()

                            val selection = buildSelectionInfo(series, xValue.toDouble())
                            state.selectedX = selection?.x
                        }
                    )
                }
                .pointerInput(series, state) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val width = size.width
                        val height = size.height

                        val plotLeft = leftPadding
                        val plotRight = width - rightPadding
                        val plotTop = topPadding
                        val plotBottom = height - bottomPadding
                        val plotWidth = max(plotRight - plotLeft, 1f)

                        if (centroid.x in plotLeft..plotRight && centroid.y in plotTop..plotBottom) {
                            val focusRatio = ((centroid.x - plotLeft) / plotWidth).coerceIn(0f, 1f)
                            val focusX =
                                state.viewport.visibleMinX + focusRatio * state.viewport.visibleWidth()

                            if (zoom.isFinite() && zoom > 0f && abs(zoom - 1f) > 0.001f) {
                                state.viewport.zoomBy(zoom, focusX)
                            }

                            if (abs(pan.x) > 0.01f) {
                                val domainDelta =
                                    -(pan.x / plotWidth) * state.viewport.visibleWidth()
                                state.viewport.panBy(domainDelta)
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val plotLeft = leftPadding
            val plotRight = width - rightPadding
            val plotTop = topPadding
            val plotBottom = height - bottomPadding
            val plotWidth = max(plotRight - plotLeft, 1f)
            val plotHeight = max(plotBottom - plotTop, 1f)

            val visibleXMin = state.viewport.visibleMinX.toDouble()
            val visibleXMax = state.viewport.visibleMaxX.toDouble()

            val visibleY = computeVisibleYRange(series, visibleXMin, visibleXMax)
            val yMin = visibleY.minY
            val yMax = visibleY.maxY
            val ySpan = max((yMax - yMin), 1e-9)

            fun mapX(x: Double): Float {
                return plotLeft + (((x - visibleXMin) / (visibleXMax - visibleXMin)) * plotWidth).toFloat()
            }

            fun mapY(y: Double): Float {
                return plotBottom - (((y - yMin) / ySpan) * plotHeight).toFloat()
            }

            if (showGrid) {
                drawGrid(
                    plotLeft = plotLeft,
                    plotTop = plotTop,
                    plotWidth = plotWidth,
                    plotHeight = plotHeight,
                    xTicks = 6,
                    yTicks = 5
                )
            }

            if (showAxes) {
                drawAxes(
                    plotLeft = plotLeft,
                    plotTop = plotTop,
                    plotWidth = plotWidth,
                    plotHeight = plotHeight
                )
                drawAxisLabels(
                    xMin = visibleXMin,
                    xMax = visibleXMax,
                    yMin = yMin,
                    yMax = yMax,
                    plotLeft = plotLeft,
                    plotTop = plotTop,
                    plotWidth = plotWidth,
                    plotHeight = plotHeight
                )
            }

            val targetBuckets = plotWidth.roundToInt().coerceAtLeast(32)

            series.forEach { s ->
                val sampled = ChartDownsampler.sampleMinMax(
                    points = s.points,
                    visibleMinX = visibleXMin,
                    visibleMaxX = visibleXMax,
                    targetBuckets = targetBuckets
                )
                if (sampled.size >= 2) {
                    val path = Path()
                    sampled.forEachIndexed { index, point ->
                        val px = mapX(point.x)
                        val py = mapY(point.y)
                        if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }

                    drawPath(
                        path = path,
                        color = s.color,
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }
            }

            val selectedX = state.selectedX
            if (showCrosshair && selectedX != null && selectedX in visibleXMin..visibleXMax) {
                val crossX = mapX(selectedX)
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.35f),
                    start = Offset(crossX, plotTop),
                    end = Offset(crossX, plotBottom),
                    strokeWidth = 1.dp.toPx()
                )

                series.forEach { s ->
                    val p = ChartDownsampler.nearestPointByX(s.points, selectedX)
                    if (p != null && p.x in visibleXMin..visibleXMax) {
                        drawCircle(
                            color = s.color,
                            radius = 4.dp.toPx(),
                            center = Offset(mapX(p.x), mapY(p.y))
                        )
                    }
                }
            }

            drawRoundRect(
                color = surfaceVariantColor.copy(alpha = 0.10f),
                topLeft = Offset(plotLeft, plotTop),
                size = Size(plotWidth, plotHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        val selection = remember(series, state.selectedX) {
            state.selectedX?.let { buildSelectionInfo(series, it) }
        }

        if (selection != null) {
            TooltipOverlay(
                selection = selection,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
}
