package io.github.sawaichi9527.eyepageflow.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChapterParserTest {
    @Test
    fun `parses traditional and numbered chapter lines with offsets`() {
        val text = "序章\n說明\n\n第一章 開始\r\n內容\n第2章 結束\n"
        val chapters = ChapterParser.parse(text)
        assertEquals(3, chapters.size)
        assertEquals("序章", chapters[0].title)
        assertEquals(0, chapters[0].offset)
        assertEquals(1, chapters[0].lineNumber)
        assertEquals("第一章 開始", chapters[1].title)
        assertEquals(text.indexOf("第一章"), chapters[1].offset)
        assertEquals(4, chapters[1].lineNumber)
        assertEquals("第2章 結束", chapters[2].title)
        assertEquals(text.indexOf("第2章"), chapters[2].offset)
    }

    @Test
    fun `ignores ordinary prose`() {
        val text = "這是一段普通文字。\n沒有任何章節標題。\n"
        assertTrue(ChapterParser.parse(text).isEmpty())
    }

    @Test
    fun `limits the number of chapters`() {
        val text = (1..10).joinToString("\n") { "第${it}章 測試" }
        val chapters = ChapterParser.parse(text, limit = 5)
        assertEquals(5, chapters.size)
    }
}
