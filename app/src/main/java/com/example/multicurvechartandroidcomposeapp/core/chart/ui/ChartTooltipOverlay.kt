package com.example.multicurvechartandroidcomposeapp.core.chart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.multicurvechartandroidcomposeapp.core.chart.domain.formatCompact
import com.example.multicurvechartandroidcomposeapp.core.chart.model.SelectedPointInfo

@Composable
fun TooltipOverlay(
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
