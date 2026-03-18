package com.example.multicurvechartandroidcomposeapp.core.chart.renderer.mpandroid

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

import com.example.multicurvechartandroidcomposeapp.core.chart.domain.buildSelectionInfo
import com.example.multicurvechartandroidcomposeapp.core.chart.domain.formatCompact
import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartSeries
import com.example.multicurvechartandroidcomposeapp.core.chart.state.MultiCurveChartState


fun LineChart.setupChart(
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
            val low = lowestVisibleX
            val high = highestVisibleX

            if (high - low <= 40f) {
                moveViewToX(low)
            }
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