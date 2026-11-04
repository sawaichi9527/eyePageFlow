package io.github.sawaichi9527.eyepageflow.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 定案的顯示預設值（2026-11-04）。
 *
 * 這些值是「第一次啟動、還沒有 settings.properties」時看到的樣子，
 * 改動就會改變所有新使用者的第一印象，所以要改之前先改這裡的預期值。
 */
class DisplayDefaultsTest {
    private val defaults = DisplaySettings()

    @Test
    fun `typography defaults match the PageByPage benchmark geometry`() {
        assertEquals(26f, defaults.fontSize)
        assertEquals(1.44f, defaults.lineHeightFactor)
        assertEquals(2f, defaults.paragraphIndentEm)
        assertEquals(900, defaults.pageWidth)
        assertEquals("Noto Serif CJK TC", defaults.fontFamily)
    }

    @Test
    fun `display defaults are a calm paper book in vertical mode`() {
        assertEquals("realistic", defaults.themeId)
        assertEquals(LayoutMode.AUTO, defaults.layoutMode)
        assertEquals(VerticalPunctuation.CENTER, defaults.punctuationPlacement)
        assertTrue(defaults.showBookAndDesktop)
        assertTrue(defaults.justify)
        assertFalse(defaults.bold)
    }

    @Test
    fun `underline defaults to off with a faint rule`() {
        assertFalse(defaults.underline)
        // 2026-11-04 定案：預設改為「淡」（0.5），不是「普通」（1.0）。
        assertEquals(0.5f, defaults.underlineWidth)
        assertEquals(0x2A2622, defaults.underlineColor)
    }

    @Test
    fun `page turn animation is off until the user picks one`() {
        assertEquals(PageTurnAnimation.NONE, defaults.animation)
    }
}
