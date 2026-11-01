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
}
