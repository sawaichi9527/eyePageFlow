package io.github.sawaichi9527.eyepageflow.core

import io.github.sawaichi9527.eyepageflow.SampleText
import io.github.sawaichi9527.eyepageflow.render.Typeface
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrphanControlTest {
    private val metrics = PageMetrics(
        pageWidth = 500,
        pageHeight = 700,
        marginTop = 30,
        marginBottom = 30,
        marginLeft = 30,
        marginRight = 30,
        fontSize = 18f,
        lineHeightFactor = 1.4f,
    )
    private val font = Typeface.pageFont(metrics)

    private fun engineFor(text: String) = PaginationEngine(text, metrics, WritingMode.HORIZONTAL, font)

    @Test
    fun `paragraph first line at the page bottom is moved to the next page`() {
        val text = "第一段內容很長會換行\n第二段開始還有後續內容"
        val engine = engineFor(text)
        val paragraphStart = text.indexOf("第二段開始")
        val pageEnd = paragraphStart + 3
        // 最後一行是第二段的第一行，且第二段還沒結束 → 應該從這一行的起點換頁
        assertEquals(
            paragraphStart,
            engine.avoidOrphanLine(0, pageEnd, listOf(0, paragraphStart)),
        )
    }

    @Test
    fun `line that continues a paragraph stays on the page`() {
        val text = "第一段內容很長會換行\n第二段繼續往下\n"
        val engine = engineFor(text)
        val secondLine = text.indexOf("第二段")
        assertEquals(text.length, engine.avoidOrphanLine(0, text.length, listOf(0, secondLine)))
    }

    @Test
    fun `single line page is never emptied`() {
        val text = "只有一行\n"
        val engine = engineFor(text)
        assertEquals(3, engine.avoidOrphanLine(0, 3, listOf(0)))
    }

    @Test
    fun `page starting with the paragraph's first line is left as is`() {
        val text = "第二段開始\n"
        val engine = engineFor(text)
        assertEquals(text.length - 1, engine.avoidOrphanLine(0, text.length - 1, listOf(0)))
    }

    @Test
    fun `single trailing line of a paragraph is pulled to the next page`() {
        // contentWidth 440 / 18px ≈ 24 字；60 字剛好切成 24 + 24 + 12 三行，
        // 頁面停在第二行結尾時，剩下的 12 字會單獨落在下一頁 → 應該把第二行也帶走。
        val text = "段".repeat(60) + "\n"
        val engine = engineFor(text)
        assertEquals(24, engine.avoidWidowLine(0, 48, listOf(0, 24, 48)))
    }

    @Test
    fun `long trailing text keeps the page break`() {
        val text = "段".repeat(90) + "\n"
        val engine = engineFor(text)
        assertEquals(48, engine.avoidWidowLine(0, 48, listOf(0, 24, 48)))
    }

    @Test
    fun `line that starts a paragraph is left to orphan control`() {
        val text = "第一段\n" + "段".repeat(60) + "\n"
        val engine = engineFor(text)
        val paragraphStart = text.indexOf("段")
        assertEquals(
            paragraphStart + 24,
            engine.avoidWidowLine(0, paragraphStart + 24, listOf(0, paragraphStart, paragraphStart + 24)),
        )
    }

    @Test
    fun `widow line is pulled so the next page keeps two lines`() {
        // 縮排設 0 讓行數可控：頁面 25 行、每行 24 個全角字。
        // 第一段 23 行 + 第二段 3 行：不分頁控制的話第 1 頁會收 23+2 行，
        // 第 2 頁只剩第二段最後一行（寡行）。
        val flat = metrics.copy(paragraphIndentEm = 0f)
        val first = "字".repeat(23 * 24) + "\n"
        val second = "段".repeat(3 * 24) + "\n"
        val text = first + second
        val pagination = PaginationEngine(text, flat, WritingMode.HORIZONTAL, font).paginate()

        val secondStart = first.length
        // 沒有寡行控制時第 1 頁會停在 secondStart + 48（第二段第 2 行結束），
        // 第 2 頁只剩第二段最後一行。控制生效後整段第二段都留到下一頁。
        assertEquals(
            secondStart,
            pagination.pages.first().endOffset,
            "第二段的內容應該整段留到下一頁，不能留下單獨的末行",
        )
    }

    @Test
    fun `no page ends with a paragraph's indented first line`() {
        // 每段都縮排 2 字，所以「頁面最後一行有縮排」就代表它是某段的第一行。
        val source = buildString {
            repeat(30) { i ->
                append("第${i + 1}段：")
                append("這是用來檢查孤行控制的測試文字，長度刻意做成會跨行的段落。".repeat(1 + i % 3))
                append("\n")
            }
        }
        val engine = PaginationEngine(source, metrics, WritingMode.HORIZONTAL, font)
        val pagination = engine.paginate()
        val indent = metrics.fontSize * metrics.paragraphIndentEm

        pagination.pages.dropLast(1).forEach { page ->
            val glyphs = engine.compose(page).glyphs
            if (glyphs.isEmpty()) return@forEach
            val lastLine = glyphs.groupBy { it.y }.entries.maxBy { it.key }.value
            val lineStart = lastLine.minBy { it.x }
            val indented = lineStart.x >= metrics.marginLeft + indent - 0.5f
            val paragraphEnds = source[page.endOffset] == '\n'
            assertTrue(
                !(indented && !paragraphEnds),
                "第 ${page.index} 頁最後一行是段落首行（x=${lineStart.x}）卻被留在頁尾",
            )
        }
    }

    @Test
    fun `pagination still covers the whole document after orphan control`() {
        val text = SampleText.longText(repeat = 4)
        WritingMode.entries.forEach { mode ->
            val pagination = PaginationEngine(text, metrics, mode, font).paginate()
            assertEquals(0, pagination.pages.first().startOffset)
            assertEquals(text.length, pagination.pages.last().endOffset)
            pagination.pages.zipWithNext().forEach { (left, right) ->
                assertEquals(left.endOffset, right.startOffset)
                assertTrue(right.startOffset < right.endOffset)
            }
        }
    }
}
