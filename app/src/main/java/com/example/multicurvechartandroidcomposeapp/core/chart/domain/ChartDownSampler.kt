package com.example.multicurvechartandroidcomposeapp.core.chart.domain

import com.example.multicurvechartandroidcomposeapp.core.chart.model.ChartPoint
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min


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
