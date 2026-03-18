package com.example.multicurvechartandroidcomposeapp.core.chart.domain

fun formatCompact(v: Double): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 1_000_000 -> String.format("%.3fM", v / 1_000_000.0)
        abs >= 1_000 -> String.format("%.3fk", v / 1_000.0)
        abs >= 10 -> String.format("%.3f", v)
        else -> String.format("%.5f", v)
    }
}
