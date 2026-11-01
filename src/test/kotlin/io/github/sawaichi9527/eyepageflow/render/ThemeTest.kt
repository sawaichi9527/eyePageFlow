package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.DisplaySettings
import java.awt.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ThemeTest {
    @Test
    fun `custom colors override the theme preset`() {
        val settings = DisplaySettings(
            themeId = "custom",
            desktopColor = 0x112233,
            coverColor = 0x445566,
            pageColor = 0xEEEEEE,
            textColor = 0x111111,
            gutterColor = 0x999999,
        )
        val theme = Theme.fromSettings(settings)
        assertEquals(Color(0x112233), theme.desktopColor)
        assertEquals(Color(0x445566), theme.coverColor)
        assertEquals(Color(0xEEEEEE), theme.pageColor)
        assertEquals(Color(0x111111), theme.textColor)
        assertEquals(Color(0x999999), theme.gutterColor)
    }

    @Test
    fun `pattern ids choose the texture`() {
        val settings = DisplaySettings(
            desktopPatternId = "desktop.linen",
            coverPatternId = "cover.solid",
            pagePatternId = "page.paper-antique",
        )
        val theme = Theme.fromSettings(settings)
        assertEquals(TextureKind.LINEN, theme.desktopTexture)
        assertEquals(TextureKind.NONE, theme.coverTexture)
        assertEquals(TextureKind.PAPER, theme.pageTexture)
    }
}
