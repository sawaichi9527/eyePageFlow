package io.github.sawaichi9527.eyepageflow.core

import io.github.sawaichi9527.eyepageflow.SampleText
import io.github.sawaichi9527.eyepageflow.render.Typeface
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
    fun `vertical cjk punctuation keeps its cell origin`() {
        val glyphs = verticalGlyphs("檔，檔")
        val first = glyphs[0]
        val comma = glyphs[1]
        assertEquals(first.x, comma.x, 0.01f)
        assertEquals(first.y + metrics.fontSize, comma.y, 0.01f)
    }

    private fun verticalGlyphs(source: String): List<PlacedGlyph> {
        val engine = PaginationEngine(source, metrics, WritingMode.VERTICAL, font)
        val pagination = engine.paginate()
        return engine.compose(pagination.page(0)).glyphs
    }
}
