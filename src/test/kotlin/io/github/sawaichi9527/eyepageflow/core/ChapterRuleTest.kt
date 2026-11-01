package io.github.sawaichi9527.eyepageflow.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChapterRuleTest {
    @Test
    fun `presets compile and custom rules can be built`() {
        ChapterRuleCatalog.presets.forEach { rule ->
            assertNotNull(rule.pattern)
            assertTrue(rule.displayName.isNotBlank())
        }
        val custom = ChapterRuleCatalog.custom("""^§.*$""")
        assertNotNull(custom)
        assertEquals("custom", custom?.id)
    }

    @Test
    fun `invalid custom regex is rejected`() {
        assertNull(ChapterRuleCatalog.custom("["))
    }

    @Test
    fun `parser accepts a custom rule`() {
        val rule = ChapterRuleCatalog.custom("""^§.*$""")!!
        val text = "§ 開頭\n普通文字\n§ 第二節\n"
        val chapters = ChapterParser.parse(text, rule)
        assertEquals(2, chapters.size)
        assertEquals("§ 開頭", chapters[0].title)
        assertEquals("§ 第二節", chapters[1].title)
    }
}
