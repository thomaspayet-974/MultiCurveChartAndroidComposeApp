package com.example.multicurvechartandroidcomposeapp.core.chart.renderer.mpandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.multicurvechartandroidcomposeapp.core.chart.domain.buildSelectionInfo
import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartSeries
import com.example.multicurvechartandroidcomposeapp.core.chart.state.MultiCurveChartState
import com.example.multicurvechartandroidcomposeapp.core.chart.ui.TooltipOverlay
import com.github.mikephil.charting.charts.LineChart


/* ============================================================
 * MPANDROIDCHART IMPLEMENTATION
 * ============================================================ */

@Composable
fun MPAndroidMultiCurveChart(
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
                chart.setVisibleXRangeMinimum(20f)
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
