package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Test

class SettingsStoreTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `settings survive a save and load`() {
        val file = tempDir.resolve("settings.properties")
        val store = SettingsStore(file)
        val display = DisplaySettings(
            themeId = "custom",
            layoutMode = LayoutMode.SINGLE,
            showBookAndDesktop = false,
            pageWidth = 720,
            fontFamily = "Noto Sans CJK TC",
            fontSize = 30f,
            lineHeightFactor = 1.8f,
            paragraphIndentEm = 1.5f,
            bold = true,
            underline = true,
            underlineColor = 0x112233,
            animation = PageTurnAnimation.SLIDE,
            desktopPatternId = "desktop.linen",
            coverPatternId = "cover.solid",
            pagePatternId = "page.paper-white",
            desktopColor = 0x101010,
            coverColor = 0x202020,
            pageColor = 0xFAFAFA,
            textColor = 0x010101,
            gutterColor = 0x303030,
        )
        val rule = ChapterRuleCatalog.custom("""^§.*$""")!!

        store.saveDisplaySettings(display)
        store.saveChapterRule(rule)

        val loaded = store.load()
        assertEquals(display, loaded.display)
        assertEquals("custom", loaded.chapterRule.id)
        assertTrue(loaded.chapterRule.pattern.matches("§ 測試"))
    }

    @Test
    fun `missing settings use defaults`() {
        val file = tempDir.resolve("missing.properties")
        val loaded = SettingsStore(file).load()
        assertEquals(DisplaySettings(), loaded.display)
        assertEquals(ChapterRuleCatalog.DEFAULT.id, loaded.chapterRule.id)
    }

    @Test
    fun `invalid chapter regex falls back to the default rule`() {
        val file = tempDir.resolve("invalid.properties")
        Files.writeString(file, "chapter.pattern=[\n")
        val loaded = SettingsStore(file).load()
        assertEquals(ChapterRuleCatalog.DEFAULT.id, loaded.chapterRule.id)
    }

    @Test
    fun `legacy typography settings are upgraded to the new baseline`() {
        val file = tempDir.resolve("legacy.properties")
        Files.writeString(
            file,
            "display.fontSize=24.0\ndisplay.lineHeight=1.5\ndisplay.theme=modern\n",
        )
        val loaded = SettingsStore(file).load()
        assertEquals(DisplaySettings().fontSize, loaded.display.fontSize)
        assertEquals(DisplaySettings().lineHeightFactor, loaded.display.lineHeightFactor)
        assertEquals("modern", loaded.display.themeId)
    }

    @Test
    fun `saved typography survives a reload`() {
        val file = tempDir.resolve("typography.properties")
        val store = SettingsStore(file)
        store.saveDisplaySettings(DisplaySettings(fontSize = 32f, lineHeightFactor = 1.7f))
        val loaded = store.load()
        assertEquals(32f, loaded.display.fontSize)
        assertEquals(1.7f, loaded.display.lineHeightFactor)
    }

    @Test
    fun `underline width and punctuation placement survive a reload`() {
        val file = tempDir.resolve("underline.properties")
        val store = SettingsStore(file)
        val display = DisplaySettings(
            underline = true,
            underlineWidth = 2.5f,
            punctuationPlacement = VerticalPunctuation.TOP_RIGHT,
        )
        store.saveDisplaySettings(display)
        val loaded = store.load()
        assertEquals(2.5f, loaded.display.underlineWidth)
        assertEquals(VerticalPunctuation.TOP_RIGHT, loaded.display.punctuationPlacement)
    }
}
