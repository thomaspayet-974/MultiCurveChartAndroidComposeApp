package com.example.multicurvechartandroidcomposeapp.core.chart.renderer.mpandroid

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.highlight.Highlight

fun LineChart.buildHighlightsForX(x: Float): Array<Highlight> {
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
