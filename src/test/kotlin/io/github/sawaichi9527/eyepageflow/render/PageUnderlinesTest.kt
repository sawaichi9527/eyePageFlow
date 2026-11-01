package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PageUnderlinesTest {
    private val metrics = PageMetrics(
        pageWidth = 500,
        pageHeight = 700,
        marginTop = 30,
        marginBottom = 30,
        marginLeft = 30,
        marginRight = 30,
        fontSize = 18f,
        lineHeightFactor = 1.4f,
        verticalColumnFactor = 1.2f,
    )

    @Test
    fun `horizontal rules span the text area`() {
        val ys = PageUnderlines.horizontalYs(metrics)
        assertTrue(ys.size >= 20)
        assertEquals(ys.size, ys.toSet().size)
        assertTrue(ys.first() >= metrics.marginTop)
        assertTrue(ys.last() <= metrics.marginTop + metrics.contentHeight)
        assertTrue(ys.last() - ys.first() > metrics.contentHeight * 0.8)
    }

    @Test
    fun `vertical rules fill every column slot`() {
        val xs = PageUnderlines.verticalXs(metrics)
        assertTrue(xs.size >= 15)
        assertEquals(xs.size, xs.toSet().size)
        assertTrue(xs.all { it in metrics.marginLeft..(metrics.marginLeft + metrics.contentWidth) })
        assertTrue(xs.first() > xs.last())
    }
}
