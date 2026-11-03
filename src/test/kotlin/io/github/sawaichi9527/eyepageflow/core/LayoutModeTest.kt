package io.github.sawaichi9527.eyepageflow.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LayoutModeTest {
    private fun settings(mode: LayoutMode) = DisplaySettings(layoutMode = mode)

    @Test
    fun `auto picks single page on a tall window`() {
        assertFalse(settings(LayoutMode.AUTO).doublePageFor(900, 900))
    }

    @Test
    fun `auto picks double page on a wide window`() {
        assertTrue(settings(LayoutMode.AUTO).doublePageFor(1600, 900))
    }

    @Test
    fun `auto threshold is the documented ratio`() {
        val width = (1000 * DisplaySettings.AUTO_DOUBLE_RATIO).toInt()
        assertTrue(settings(LayoutMode.AUTO).doublePageFor(width, 1000))
        assertFalse(settings(LayoutMode.AUTO).doublePageFor(width - 2, 1000))
    }

    @Test
    fun `fixed modes ignore the window size`() {
        assertFalse(settings(LayoutMode.SINGLE).doublePageFor(4000, 600))
        assertTrue(settings(LayoutMode.DOUBLE).doublePageFor(400, 400))
        assertTrue(settings(LayoutMode.EQUAL_HEIGHT).doublePageFor(400, 400))
    }

    @Test
    fun `equal height keeps two pages`() {
        assertEquals(true, settings(LayoutMode.EQUAL_HEIGHT).usesDoublePage())
        assertEquals(false, settings(LayoutMode.SINGLE).usesDoublePage())
    }
}
