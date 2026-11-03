package io.github.sawaichi9527.eyepageflow.core

import io.github.sawaichi9527.eyepageflow.SampleText
import io.github.sawaichi9527.eyepageflow.render.Typeface
import java.awt.image.BufferedImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PaginationTest {
    private val text = SampleText.longText(repeat = 4)
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

    @Test
    fun `pages cover the whole document without gaps`() {
        WritingMode.entries.forEach { mode ->
            val pagination = PaginationEngine(text, metrics, mode, font).paginate()
            assertTrue(pagination.pageCount > 1)
            assertEquals(0, pagination.pages.first().startOffset)
            assertEquals(text.length, pagination.pages.last().endOffset)
            pagination.pages.zipWithNext().forEach { (left, right) ->
                assertEquals(left.endOffset, right.startOffset)
                assertTrue(right.startOffset < right.endOffset)
            }
        }
    }

    @Test
    fun `vertical and horizontal pagination differ`() {
        val horizontal = PaginationEngine(text, metrics, WritingMode.HORIZONTAL, font).paginate()
        val vertical = PaginationEngine(text, metrics, WritingMode.VERTICAL, font).paginate()
        assertNotEquals(horizontal.pageCount, vertical.pageCount)
    }

    @Test
    fun `composed page has glyphs`() {
        val engine = PaginationEngine(text, metrics, WritingMode.VERTICAL, font)
        val pagination = engine.paginate()
        val content = engine.compose(pagination.page(0))
        assertTrue(content.glyphs.isNotEmpty())
        content.glyphs.forEach { glyph ->
            assertTrue(glyph.x.isFinite())
            assertTrue(glyph.y.isFinite())
        }
    }

    @Test
    fun `vertical cjk keeps one em advance`() {
        val glyphs = verticalGlyphs("檔檔")
        assertEquals(metrics.fontSize, glyphs[1].y - glyphs[0].y, 0.01f)
    }

    @Test
    fun `vertical latin advances by glyph width`() {
        val glyphs = verticalGlyphs("ee")
        val advance = glyphs[1].y - glyphs[0].y
        assertTrue(advance > 0f)
        assertTrue(
            advance < metrics.fontSize,
            "latin advance $advance should be narrower than one em ${metrics.fontSize}",
        )
        assertEquals(advance, glyphs[0].cellHeight, 0.01f)
    }

    @Test
    fun `vertical ascii hyphen is rotated with the latin run`() {
        val glyphs = verticalGlyphs("UTF-8")
        val hyphen = glyphs[3]
        assertEquals('-', hyphen.char)
        assertEquals(90f, hyphen.rotationDegrees, 0.01f)
        assertEquals(glyphs[4].y - hyphen.y, hyphen.cellHeight, 0.01f)
    }

    @Test
    fun `vertical cjk punctuation keeps its cell origin`() {
        val glyphs = verticalGlyphs("檔，檔")
        val first = glyphs[0]
        val comma = glyphs[1]
        assertEquals(first.x, comma.x, 0.01f)
        assertEquals(first.y + metrics.fontSize, comma.y, 0.01f)
    }

    @Test
    fun `vertical punctuation can sit at the top right`() {
        val topRight = metrics.copy(punctuationPlacement = VerticalPunctuation.TOP_RIGHT)
        val engine = PaginationEngine("檔，檔", topRight, WritingMode.VERTICAL, font)
        val glyphs = engine.compose(engine.paginate().page(0)).glyphs
        val cell = glyphs[0]
        val comma = glyphs[1]

        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.font = font
        val fontMetrics = graphics.fontMetrics
        val bounds = font.createGlyphVector(graphics.fontRenderContext, "，").visualBounds
        graphics.dispose()

        val inkCenterX = (comma.x + bounds.centerX).toFloat()
        val inkCenterY = (comma.y + fontMetrics.ascent + bounds.centerY).toFloat()
        val cellTop = cell.y + topRight.fontSize
        assertEquals(cell.x + topRight.fontSize * 0.75f, inkCenterX, 0.5f)
        assertEquals(cellTop + topRight.fontSize * 0.22f, inkCenterY, 0.5f)
        assertEquals(topRight.fontSize, comma.cellHeight, 0.01f)
    }

    @Test
    fun `kinsoku keeps closing punctuation off line and column starts`() {
        val source = buildString {
            repeat(120) {
                append("測試文字，段落內容。還有（引號）與「書名」！全形標點：，。！？：；「」『』《》（）。")
            }
        }
        WritingMode.entries.forEach { mode ->
            val engine = PaginationEngine(source, metrics, mode, font)
            val pagination = engine.paginate()
            val badStarts = ArrayList<String>()
            val badEnds = ArrayList<String>()
            pagination.pages.forEach { page ->
                val glyphs = engine.compose(page).glyphs
                if (glyphs.isEmpty()) return@forEach
                val groups = if (mode == WritingMode.HORIZONTAL) {
                    glyphs.groupBy { it.y }
                } else {
                    glyphs.groupBy { it.x }
                }
                groups.values.forEach { line ->
                    val ordered = if (mode == WritingMode.HORIZONTAL) {
                        line.sortedBy { it.x }
                    } else {
                        line.sortedBy { it.y }
                    }
                    val first = ordered.first()
                    val last = ordered.last()
                    if (first.char in NO_LINE_START) {
                        val sample = ordered.take(14).joinToString("") { it.char.toString() }
                        badStarts += "${mode.name} p${page.index} 行首為 '${first.char}' [$sample]"
                    }
                    if (last.char in NO_LINE_END) {
                        badEnds += "${mode.name} p${page.index} 行尾為 '${last.char}'"
                    }
                }
            }
            assertTrue(badStarts.isEmpty(), badStarts.take(5).joinToString("; "))
            assertTrue(badEnds.isEmpty(), badEnds.take(5).joinToString("; "))
        }
    }

    @Test
    fun `horizontal wrapping lines are justified to the content edge`() {
        val source = buildString { repeat(80) { append("測試文字段落內容") } }
        val engine = PaginationEngine(source, metrics, WritingMode.HORIZONTAL, font)
        val pagination = engine.paginate()
        val lines = engine.compose(pagination.page(0)).glyphs
            .groupBy { it.y }
            .values
            .map { line -> line.sortedBy { it.x } }

        assertTrue(lines.size > 2, "樣本應該產生多行")
        val contentRight = metrics.marginLeft + metrics.contentWidth
        lines.forEach { line ->
            val right = line.last().x + line.last().cellWidth
            assertEquals(contentRight.toFloat(), right, 1.0f)
        }
    }

    @Test
    fun `justify can be disabled`() {
        val source = buildString { repeat(80) { append("測試文字段落內容") } }
        val ragged = metrics.copy(justify = false)
        val engine = PaginationEngine(source, ragged, WritingMode.HORIZONTAL, font)
        val pagination = engine.paginate()
        val lines = engine.compose(pagination.page(0)).glyphs
            .groupBy { it.y }
            .values
            .map { line -> line.sortedBy { it.x } }

        val contentRight = ragged.marginLeft + ragged.contentWidth
        val shortLines = lines.count { line ->
            line.last().x + line.last().cellWidth < contentRight - 1f
        }
        assertTrue(shortLines > lines.size / 2, "關閉對齊後多數行應留自然行尾空白")
    }

    @Test
    fun `paragraph last line is not stretched`() {
        val engine = PaginationEngine("測試文字", metrics, WritingMode.HORIZONTAL, font)
        val glyphs = engine.compose(engine.paginate().page(0)).glyphs
        val last = glyphs.last()
        assertTrue(last.x + last.cellWidth < metrics.marginLeft + metrics.contentWidth - 1f)
    }

    private fun verticalGlyphs(source: String): List<PlacedGlyph> {
        val engine = PaginationEngine(source, metrics, WritingMode.VERTICAL, font)
        val pagination = engine.paginate()
        return engine.compose(pagination.page(0)).glyphs
    }
}
