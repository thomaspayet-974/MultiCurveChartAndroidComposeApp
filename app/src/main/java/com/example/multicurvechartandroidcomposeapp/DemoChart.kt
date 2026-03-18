@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.example.multicurvechartandroidcomposeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.random.Random

import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartPoint
import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartSeries
import com.example.multicurvechartandroidcomposeapp.core.chart.state.rememberMultiCurveChartState
import com.example.multicurvechartandroidcomposeapp.core.chart.ui.ChartRenderer
import com.example.multicurvechartandroidcomposeapp.core.chart.ui.MultiCurveChart

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