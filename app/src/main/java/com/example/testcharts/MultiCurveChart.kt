@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.example.testcharts

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.coerceAtMost

/* ============================================================
 * DATA MODELS
 * ============================================================ */

@Immutable
data class ChartPoint(
    val x: Double,
    val y: Double
)

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

@Immutable
data class SelectedPointInfo(
    val x: Double,
    val perSeries: List<SeriesSelection>
)

@Immutable
data class SeriesSelection(
    val seriesId: String,
    val seriesName: String,
    val color: Color,
    val point: ChartPoint
)

enum class ChartRenderer {
    ComposeCanvas,
    MPAndroidChart
}

/* ============================================================
 * VIEWPORT STATE
 * ============================================================ */

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
        var width = visibleWidth().coerceIn(total / 10_000f, total)

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

@Composable
fun rememberChartViewportState(): ChartViewportState = remember { ChartViewportState() }

/* ============================================================
 * DOWN SAMPLING
 * ============================================================ */

object ChartDownsampler {

    /**
     * Downsampling min/max bucket.
     *
     * Idée:
     * - On limite le rendu au nombre de pixels utiles.
     * - Pour chaque bucket horizontal, on conserve les points extrêmes (min / max Y)
     *   afin de préserver les pics et creux.
     * - Les données étant triées par X, on fait d’abord un slicing rapide par binary search.
     */
    fun sampleMinMax(
        points: List<ChartPoint>,
        visibleMinX: Double,
        visibleMaxX: Double,
        targetBuckets: Int
    ): List<ChartPoint> {
        if (points.isEmpty()) return emptyList()
        if (targetBuckets <= 0) return emptyList()

        val start = lowerBound(points, visibleMinX).coerceAtLeast(0)
        val endExclusive = upperBound(points, visibleMaxX).coerceAtMost(points.size)

        if (start >= endExclusive) return emptyList()

        val visibleCount = endExclusive - start
        if (visibleCount <= targetBuckets * 2) {
            return points.subList(start, endExclusive)
        }

        val bucketSize = ceil(visibleCount / targetBuckets.toDouble()).toInt().coerceAtLeast(1)
        val result = ArrayList<ChartPoint>(targetBuckets * 2 + 2)

        result.add(points[start])

        var bucketStart = start
        while (bucketStart < endExclusive) {
            val bucketEnd = min(bucketStart + bucketSize, endExclusive)

            var minPoint = points[bucketStart]
            var maxPoint = points[bucketStart]

            for (i in bucketStart until bucketEnd) {
                val p = points[i]
                if (p.y < minPoint.y) minPoint = p
                if (p.y > maxPoint.y) maxPoint = p
            }

            if (minPoint.x <= maxPoint.x) {
                if (result.lastOrNull() != minPoint) result.add(minPoint)
                if (result.lastOrNull() != maxPoint) result.add(maxPoint)
            } else {
                if (result.lastOrNull() != maxPoint) result.add(maxPoint)
                if (result.lastOrNull() != minPoint) result.add(minPoint)
            }

            bucketStart = bucketEnd
        }

        val last = points[endExclusive - 1]
        if (result.lastOrNull() != last) result.add(last)

        return result
    }

    fun lowerBound(points: List<ChartPoint>, x: Double): Int {
        var low = 0
        var high = points.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (points[mid].x < x) low = mid + 1 else high = mid
        }
        return low
    }

    fun upperBound(points: List<ChartPoint>, x: Double): Int {
        var low = 0
        var high = points.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (points[mid].x <= x) low = mid + 1 else high = mid
        }
        return low
    }

    /**
     * Recherche du point le plus proche sur l’axe X.
     * Complexité O(log n).
     */
    fun nearestPointByX(points: List<ChartPoint>, x: Double): ChartPoint? {
        if (points.isEmpty()) return null
        val idx = lowerBound(points, x)

        return when {
            idx <= 0 -> points.first()
            idx >= points.lastIndex -> points.last()
            else -> {
                val a = points[idx - 1]
                val b = points[idx]
                if (abs(a.x - x) <= abs(b.x - x)) a else b
            }
        }
    }
}

/* ============================================================
 * CHART STATE
 * ============================================================ */

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

@Composable
fun rememberMultiCurveChartState(
    viewport: ChartViewportState = rememberChartViewportState()
): MultiCurveChartState = remember(viewport) { MultiCurveChartState(viewport) }

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

/* ============================================================
 * COMPOSE CANVAS IMPLEMENTATION
 * ============================================================ */

private data class VisibleYRange(
    val minY: Double,
    val maxY: Double
)

@Composable
private fun ComposeCanvasMultiCurveChart(
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

private fun computeVisibleYRange(
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

private fun DrawScope.drawGrid(
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float,
    xTicks: Int,
    yTicks: Int
) {
    val gridColor = Color(0xFFB0B8C4).copy(alpha = 0.22f)
    val stroke = 1.dp.toPx()

    for (i in 0..xTicks) {
        val x = plotLeft + (i / xTicks.toFloat()) * plotWidth
        drawLine(
            color = gridColor,
            start = Offset(x, plotTop),
            end = Offset(x, plotTop + plotHeight),
            strokeWidth = stroke
        )
    }

    for (i in 0..yTicks) {
        val y = plotTop + (i / yTicks.toFloat()) * plotHeight
        drawLine(
            color = gridColor,
            start = Offset(plotLeft, y),
            end = Offset(plotLeft + plotWidth, y),
            strokeWidth = stroke
        )
    }
}

private fun DrawScope.drawAxes(
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float
) {
    val axisColor = Color(0xFF7C8593)
    val stroke = 1.2.dp.toPx()

    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotTop + plotHeight),
        end = Offset(plotLeft + plotWidth, plotTop + plotHeight),
        strokeWidth = stroke
    )
    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotTop),
        end = Offset(plotLeft, plotTop + plotHeight),
        strokeWidth = stroke
    )
}

private fun DrawScope.drawAxisLabels(
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float
) {
    val textPaint = android.graphics.Paint().apply {
        color = AndroidColor.argb(180, 90, 98, 110)
        textSize = 11.dp.toPx()
        isAntiAlias = true
    }

    val xTicks = 6
    val yTicks = 5

    for (i in 0..xTicks) {
        val ratio = i / xTicks.toFloat()
        val value = xMin + (xMax - xMin) * ratio
        val x = plotLeft + ratio * plotWidth
        drawContext.canvas.nativeCanvas.drawText(
            formatCompact(value),
            x - 18.dp.toPx(),
            plotTop + plotHeight + 16.dp.toPx(),
            textPaint
        )
    }

    for (i in 0..yTicks) {
        val ratio = i / yTicks.toFloat()
        val value = yMax - (yMax - yMin) * ratio
        val y = plotTop + ratio * plotHeight
        drawContext.canvas.nativeCanvas.drawText(
            formatCompact(value),
            4.dp.toPx(),
            y + 4.dp.toPx(),
            textPaint
        )
    }
}

private fun formatCompact(v: Double): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 1_000_000 -> String.format("%.3fM", v / 1_000_000.0)
        abs >= 1_000 -> String.format("%.3fk", v / 1_000.0)
        abs >= 10 -> String.format("%.3f", v)
        else -> String.format("%.5f", v)
    }
}

private fun buildSelectionInfo(
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

@Composable
private fun TooltipOverlay(
    selection: SelectedPointInfo,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 240.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "x = ${formatCompact(selection.x)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            selection.perSeries.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(s.color, RoundedCornerShape(50))
                    )
                    Text(
                        text = "${s.seriesName} : ${formatCompact(s.point.y)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/* ============================================================
 * MPANDROIDCHART IMPLEMENTATION
 * ============================================================ */

@Composable
private fun MPAndroidMultiCurveChart(
    series: List<ChartSeries>,
    modifier: Modifier,
    state: MultiCurveChartState,
    showAxes: Boolean,
    showGrid: Boolean,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                LineChart(context).apply {
                    setupChart(state, series, showGrid)
                    setReducedSeries(series)
                    renderer = CustomLineChartHighlightRenderer(
                        chart = this,
                        animator = this.animator,
                        viewPortHandler = this.viewPortHandler
                    )
                }
            },
            update = { chart ->
                chart.fitScreen()
                chart.setReducedSeries(series)
                chart.setVisibleXRangeMinimum(40f)
                chart.moveViewToX(series.firstOrNull()?.points?.firstOrNull()?.x?.toFloat() ?: 0f)
                chart.updateVisibleYRange()
                chart.invalidate()
            }
        )
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

private fun LineChart.setupChart(
    state: MultiCurveChartState,
    series: List<ChartSeries>,
    showGrid: Boolean
) {
    description.isEnabled = false
    setNoDataText("Aucune donnée")

    legend.apply {
        isEnabled = true
        verticalAlignment = Legend.LegendVerticalAlignment.TOP
        horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
    }

    setTouchEnabled(true)
    isDragEnabled = true
    setScaleEnabled(true)
    setPinchZoom(false)
    setDoubleTapToZoomEnabled(false)
    isHighlightPerTapEnabled = true
    isHighlightPerDragEnabled = false

    // X pan gestures only
    setDragXEnabled(true)
    setDragYEnabled(false)

    // X zoom gestures only
    setScaleXEnabled(true)
    setScaleYEnabled(false)

    // Ensure the Y scale is coherent with gestures on edges.
    viewPortHandler.setMinimumScaleY(1f)
    viewPortHandler.setMaximumScaleY(1f)

    axisRight.isEnabled = false

    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(showGrid)
        granularity = 1f
        setAvoidFirstLastClipping(true)
        valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return formatCompact(value.toDouble())
            }
        }
    }

    axisLeft.apply {
        setDrawGridLines(showGrid)
        setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return formatCompact(value.toDouble())
            }
        }
    }

    extraTopOffset = 12f
    extraBottomOffset = 8f

    setVisibleXRangeMinimum(40f)

    onChartGestureListener = object : OnChartGestureListener {
        override fun onChartGestureStart(
            me: android.view.MotionEvent?,
            lastPerformedGesture: ChartTouchListener.ChartGesture?
        ) = Unit

        override fun onChartGestureEnd(
            me: android.view.MotionEvent?,
            lastPerformedGesture: ChartTouchListener.ChartGesture?
        ) {
            enforceHorizontalOnlyViewport()
            setReducedSeries(series)
            updateVisibleYRange()
        }

        override fun onChartLongPressed(me: android.view.MotionEvent?) = Unit
        override fun onChartDoubleTapped(me: android.view.MotionEvent?) {
            fitScreen()
            enforceHorizontalOnlyViewport()
            setReducedSeries(series)
            updateVisibleYRange()
        }

        override fun onChartSingleTapped(me: android.view.MotionEvent?) = Unit
        override fun onChartFling(
            me1: android.view.MotionEvent?,
            me2: android.view.MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ) {
            enforceHorizontalOnlyViewport()
            setReducedSeries(series)
            updateVisibleYRange()
        }

        override fun onChartScale(me: android.view.MotionEvent?, scaleX: Float, scaleY: Float) {
            enforceHorizontalOnlyViewport()
            setReducedSeries(series)
            updateVisibleYRange()
        }

        override fun onChartTranslate(me: android.view.MotionEvent?, dX: Float, dY: Float) {
            enforceHorizontalOnlyViewport()
            setReducedSeries(series)
            updateVisibleYRange()
        }
    }

    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
        override fun onValueSelected(e: Entry?, h: Highlight?) {
            val x = e?.x ?: return
            val selection = buildSelectionInfo(series, x.toDouble())
            state.selectedX = selection?.x
            highlightValues(buildHighlightsForX(x))
            setReducedSeries(series)
            updateVisibleYRange()
        }

        override fun onNothingSelected() {
            state.selectedX = null
            highlightValues(null)
        }
    })
}

private fun LineChart.buildHighlightsForX(x: Float): Array<Highlight> {
    val lineData = data ?: return emptyArray()
    val highlights = mutableListOf<Highlight>()

    for (dataSetIndex in 0 until lineData.dataSetCount) {
        val dataSet = lineData.getDataSetByIndex(dataSetIndex)
        val entry = dataSet.getEntryForXValue(x, Float.NaN, com.github.mikephil.charting.data.DataSet.Rounding.CLOSEST)
            ?: continue

        highlights += Highlight(entry.x, entry.y, dataSetIndex)
    }

    return highlights.toTypedArray()
}

private fun LineChart.enforceHorizontalOnlyViewport() {
    val matrix = viewPortHandler.matrixTouch
    val values = FloatArray(9)
    matrix.getValues(values)

    values[android.graphics.Matrix.MSCALE_Y] = 1f
    values[android.graphics.Matrix.MSKEW_Y] = 0f
    values[android.graphics.Matrix.MSKEW_X] = 0f
    values[android.graphics.Matrix.MTRANS_Y] = 0f

    matrix.setValues(values)
    viewPortHandler.refresh(matrix, this, true)
}

private fun LineChart.updateVisibleYRange() {
    val lineData = data ?: return
    val dataSet = lineData.getDataSetByIndex(0) ?: return
    if (dataSet.entryCount == 0) return

    val lowestVisibleX = lowestVisibleX
    val highestVisibleX = highestVisibleX

    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    for (dataSetIndex in 0 until lineData.dataSetCount) {
        val dataSet = lineData.getDataSetByIndex(dataSetIndex)
        for (i in 0 until dataSet.entryCount) {
            val e = dataSet.getEntryForIndex(i)
            if (e.x in lowestVisibleX..highestVisibleX) {
                if (e.y < minY) minY = e.y
                if (e.y > maxY) maxY = e.y
            }
        }
    }

    if (minY == Float.POSITIVE_INFINITY || maxY == Float.NEGATIVE_INFINITY) {
        return
    }

    val range = (maxY - minY).takeIf { it > 0f } ?: 1f
    val padding = range * 0.1f

    axisLeft.axisMinimum = minY - padding
    axisLeft.axisMaximum = maxY + padding

    notifyDataSetChanged()
    invalidate()
}

private fun LineChart.setSeries(series: List<ChartSeries>) {
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

private fun LineChart.setReducedSeries(series: List<ChartSeries>) {
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

private fun syncViewportFromChart(chart: LineChart, state: MultiCurveChartState) {
    if (chart.lowestVisibleX.isFinite() && chart.highestVisibleX.isFinite()) {
        val minX = chart.lowestVisibleX
        val maxX = chart.highestVisibleX
        if (maxX > minX) {
            state.viewport.setDataBounds(state.viewport.dataMinX, state.viewport.dataMaxX)
            state.viewport.panBy(0f) // clamp
            val currentWidth = state.viewport.visibleWidth()
            val targetWidth = (maxX - minX)
            if (targetWidth > 0f) {
                state.viewport.visibleMinX = minX
                state.viewport.visibleMaxX = maxX
            }
        }
    }
}

class CustomLineChartHighlightRenderer(
    chart: LineChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : LineChartRenderer(chart, animator, viewPortHandler) {

    private val highlightPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {
        super.drawHighlighted(c, indices)

        val lineData = mChart.lineData ?: return

        for (high in indices) {
            val set = lineData.getDataSetByIndex(high.dataSetIndex)
            val entry = set.getEntryForXValue(high.x, high.y)
            val color = set.color

            if (entry != null) {
                val pt = mChart.getTransformer(set.axisDependency)
                    .getPixelForValues(entry.x, entry.y)

                highlightPaint.color = color

                // Draw a circle on every curve for selected X (highlighted)
                c.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 8f, highlightPaint)
            }
        }
    }
}

/* ============================================================
 * DYNAMIC DATA SUPPORT
 * ============================================================ */

@Stable
class MutableChartSeriesState(initial: List<ChartSeries>) {
    val series = mutableStateListOf<ChartSeries>().apply { addAll(initial) }

    fun replaceSeries(newSeries: List<ChartSeries>) {
        series.clear()
        series.addAll(newSeries)
    }

    fun appendPoint(seriesId: String, point: ChartPoint) {
        val index = series.indexOfFirst { it.id == seriesId }
        if (index < 0) return
        val old = series[index]
        val newPoints = old.points.toMutableList().apply {
            // Data supposed to be sorted (X).
            add(point)
        }
        series[index] = old.copy(points = newPoints)
    }

    fun toggle(seriesId: String) {
        val index = series.indexOfFirst { it.id == seriesId }
        if (index < 0) return
        val old = series[index]
        series[index] = old.copy(enabled = !old.enabled)
    }
}

/* ============================================================
 * DEMO / EXAMPLE SCREEN
 * ============================================================ */

@Composable
fun MultiCurveChartDemoScreen() {
    val renderer = remember { mutableStateOf(ChartRenderer.ComposeCanvas) }

    val initialSeries = remember {
        generateScientificSeries(
            pointCount = 400_000,
            seriesCount = 3
        )
    }

    val seriesState = remember { MutableChartSeriesState(initialSeries) }
    val chartState = rememberMultiCurveChartState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "MultiCurveChart Demo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                onClick = { renderer.value = ChartRenderer.ComposeCanvas },
                shape = RoundedCornerShape(12.dp),
                tonalElevation = if (renderer.value == ChartRenderer.ComposeCanvas) 4.dp else 0.dp,
                modifier = Modifier.border(
                    1.dp,
                    if (renderer.value == ChartRenderer.ComposeCanvas)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(12.dp)
                )
            ) {
                Text(
                    text = "Compose Canvas",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Surface(
                onClick = { renderer.value = ChartRenderer.MPAndroidChart },
                shape = RoundedCornerShape(12.dp),
                tonalElevation = if (renderer.value == ChartRenderer.MPAndroidChart) 4.dp else 0.dp,
                modifier = Modifier.border(
                    1.dp,
                    if (renderer.value == ChartRenderer.MPAndroidChart)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(12.dp)
                )
            ) {
                Text(
                    text = "MPAndroidChart",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            MultiCurveChart(
                series = seriesState.series.filter{ it.enabled },
                state = chartState,
                renderer = renderer.value,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = "Séries",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(seriesState.series, key = { it.id }) { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = s.enabled,
                        onCheckedChange = { seriesState.toggle(s.id) }
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(s.color, RoundedCornerShape(50))
                    )
                    Text(
                        text = "  ${s.name} (${s.points.size} points)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

fun generateScientificSeries(
    pointCount: Int,
    seriesCount: Int
): List<ChartSeries> {
    require(pointCount > 1)

    val palette = listOf(
        Color(0xFF2E7DFF),
        Color(0xFFFF6B4A),
        Color(0xFF21B573),
        Color(0xFF8E5CFF),
        Color(0xFFF4B400)
    )

    return List(seriesCount) { seriesIndex ->
        val points = ArrayList<ChartPoint>(pointCount)
        val random = Random(seriesIndex + 42)

        for (i in 0 until pointCount) {
            val x = i.toDouble()
            val base = when (seriesIndex) {
                0 -> kotlin.math.sin(i / 350.0) * 50.0
                1 -> kotlin.math.cos(i / 220.0) * 35.0 + kotlin.math.sin(i / 1300.0) * 20.0
                else -> kotlin.math.sin(i / 500.0) * 25.0 + kotlin.math.cos(i / 90.0) * 10.0
            }

            val trend = seriesIndex * 18.0 + i * 0.0008
            val noise = (random.nextDouble() - 0.5) * 2.5
            val spike = if (i % 5000 == 0) random.nextDouble(20.0, 60.0) else 0.0

            points += ChartPoint(
                x = x,
                y = base + trend + noise + spike
            )
        }

        ChartSeries(
            id = "series_$seriesIndex",
            name = "Courbe ${seriesIndex + 1}",
            color = palette[seriesIndex % palette.size],
            points = points,
            enabled = true
        )
    }
}

fun generateScientificSeries(totalPoints: Int, offsetAxisX: Double): List<ChartSeries> {
    val p1 = ArrayList<ChartPoint>(totalPoints)
    val p2 = ArrayList<ChartPoint>(totalPoints)
    val p3 = ArrayList<ChartPoint>(totalPoints)

    val rnd = Random(42)
    for (i in (0 + offsetAxisX.toInt()) until (totalPoints + offsetAxisX.toInt())) {
        val x = i.toDouble()
        val y1 = (
                1.2f * kotlin.math.sin(x * 0.0025f) +
                        0.35f * kotlin.math.sin(x * 0.021f) +
                        0.08f * kotlin.math.cos(x * 0.17f)
                )
        val y2 = (
                0.7f * kotlin.math.cos(x * 0.004f + 0.6f) +
                        0.25f * kotlin.math.sin(x * 0.031f) +
                        (rnd.nextFloat() - 0.5f) * 0.03f
                )
        val y3 = (
                (x / totalPoints.toDouble()) * 1.8f - 0.9f +
                        0.18f * kotlin.math.sin(x * 0.014f) +
                        0.12f * kotlin.math.sin(x * 0.19f)
                )

        p1 += ChartPoint(x, y1)
        p2 += ChartPoint(x, y2)
        p3 += ChartPoint(x, y3)
    }

    return listOf(
        ChartSeries(
            id = "s1",
            name = "Signal A",
            color = Color(0xFF4FC3F7),
            points = p1,
            enabled = true,
        ),
        ChartSeries(
            id = "s2",
            name = "Signal B",
            color = Color(0xFFFFB74D),
            points = p2,
            enabled = true,
        ),
        ChartSeries(
            id = "s3",
            name = "Tendance C",
            color = Color(0xFF81C784),
            points = p3,
            enabled = true,
        ),
    )
}