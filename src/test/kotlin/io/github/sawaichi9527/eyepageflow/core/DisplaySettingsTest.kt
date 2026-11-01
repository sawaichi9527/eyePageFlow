package io.github.sawaichi9527.eyepageflow.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisplaySettingsTest {
    @Test
    fun `metrics follow display settings`() {
        val settings = DisplaySettings(
            pageWidth = 720,
            fontSize = 30f,
            lineHeightFactor = 1.8f,
            paragraphIndentEm = 1.5f,
            bold = true,
            underline = true,
        )
        val metrics = settings.toPageMetrics()
        assertEquals(720, metrics.pageWidth)
        assertEquals(30f, metrics.fontSize)
        assertEquals(1.8f, metrics.lineHeightFactor)
        assertEquals(1.5f, metrics.paragraphIndentEm)
        assertTrue(metrics.bold)
        assertTrue(metrics.underline)
    }

    @Test
    fun `single page mode disables the second page`() {
        assertFalse(DisplaySettings(layoutMode = LayoutMode.SINGLE).usesDoublePage())
        assertTrue(DisplaySettings(layoutMode = LayoutMode.DOUBLE).usesDoublePage())
        assertTrue(DisplaySettings(layoutMode = LayoutMode.AUTO).usesDoublePage())
        assertTrue(DisplaySettings(layoutMode = LayoutMode.EQUAL_HEIGHT).usesDoublePage())
    }
}
