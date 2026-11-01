package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import kotlin.math.floor

/**
 * Computes page-wide rule lines. Underlines are not tied to glyphs; they span
 * the whole text area so a page looks like ruled paper even where a line is
 * short or empty.
 */
object PageUnderlines {
    fun horizontalYs(metrics: PageMetrics): List<Int> {
        val count = floor(metrics.contentHeight / metrics.lineHeight)
            .toInt()
            .coerceAtLeast(1)
        return (0 until count).map { line ->
            (metrics.marginTop + (line + 1) * metrics.lineHeight - 2f).toInt()
        }
    }

    fun verticalXs(metrics: PageMetrics): List<Int> {
        val columnPitch = metrics.fontSize * metrics.verticalColumnFactor
        val count = floor(metrics.contentWidth / columnPitch)
            .toInt()
            .coerceAtLeast(1)
        val right = metrics.marginLeft + metrics.contentWidth
        return (0 until count).map { column ->
            (right - (column + 1) * columnPitch +
                (columnPitch - metrics.fontSize) / 2f + metrics.fontSize + 1f)
                .toInt()
                .coerceIn(metrics.marginLeft, right)
        }
    }
}
