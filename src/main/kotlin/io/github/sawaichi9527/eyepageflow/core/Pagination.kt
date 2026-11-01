package io.github.sawaichi9527.eyepageflow.core

import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.floor

enum class WritingMode {
    HORIZONTAL,
    VERTICAL,
}

data class PageMetrics(
    val pageWidth: Int = 900,
    val pageHeight: Int = 850,
    val marginTop: Int = 42,
    val marginBottom: Int = 58,
    val marginLeft: Int = 44,
    val marginRight: Int = 44,
    val fontSize: Float = 24f,
    val lineHeightFactor: Float = 1.5f,
    val verticalColumnFactor: Float = 1.2f,
    val paragraphIndentEm: Float = 2f,
    val bold: Boolean = false,
    val underline: Boolean = false,
) {
    val contentWidth: Int
        get() = pageWidth - marginLeft - marginRight
    val contentHeight: Int
        get() = pageHeight - marginTop - marginBottom
    val lineHeight: Float
        get() = fontSize * lineHeightFactor
}

data class PageSpan(
    val index: Int,
    val startOffset: Int,
    val endOffset: Int,
    val lineCount: Int,
    val columnCount: Int,
)

data class Pagination(
    val mode: WritingMode,
    val metrics: PageMetrics,
    val totalChars: Int,
    val pages: List<PageSpan>,
) {
    val pageCount: Int
        get() = pages.size

    fun page(index: Int): PageSpan = pages[index.coerceIn(0, pages.lastIndex)]
}

data class PlacedGlyph(
    val char: Char,
    val x: Float,
    val y: Float,
    val cellWidth: Float,
    val cellHeight: Float,
    val rotationDegrees: Float = 0f,
    val underline: Boolean = false,
    val drawChar: Char = char,
)

data class PageContent(
    val span: PageSpan,
    val glyphs: List<PlacedGlyph>,
)

/**
 * Width-table based pagination. The page is laid out once to compute page
 * boundaries; glyph positions are produced again only for the page that is
 * actually painted. This keeps a 10 MB document cheap to paginate.
 */
class PaginationEngine(
    private val text: String,
    private val metrics: PageMetrics,
    private val mode: WritingMode,
    private val font: Font,
) {
    private val probeImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    private val probeGraphics: Graphics2D = probeImage.createGraphics()
    private val fontMetrics: FontMetrics

    init {
        probeGraphics.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
        )
        probeGraphics.font = font
        fontMetrics = probeGraphics.fontMetrics
    }

    private val widthCache = HashMap<Char, Float>()

    fun paginate(): Pagination {
        val pages = ArrayList<PageSpan>()
        var offset = 0
        var index = 0
        while (offset < text.length) {
            val result = walkPage(offset, collectGlyphs = false)
            val end = if (result.endOffset <= offset) offset + 1 else result.endOffset
            pages += PageSpan(
                index = index,
                startOffset = offset,
                endOffset = end,
                lineCount = result.lineCount,
                columnCount = result.columnCount,
            )
            offset = end
            index += 1
        }
        if (pages.isEmpty()) {
            pages += PageSpan(0, 0, 0, 0, 0)
        }
        return Pagination(mode, metrics, text.length, pages)
    }

    fun compose(span: PageSpan): PageContent {
        val result = walkPage(span.startOffset, collectGlyphs = true, limitEnd = span.endOffset)
        return PageContent(span, result.glyphs)
    }

    private fun walkPage(
        startOffset: Int,
        collectGlyphs: Boolean,
        limitEnd: Int = text.length,
    ): WalkResult = when (mode) {
        WritingMode.HORIZONTAL -> walkHorizontal(startOffset, collectGlyphs, limitEnd)
        WritingMode.VERTICAL -> walkVertical(startOffset, collectGlyphs, limitEnd)
    }

    private fun walkHorizontal(
        start: Int,
        collect: Boolean,
        limitEnd: Int,
    ): WalkResult {
        val maxLines = floor(metrics.contentHeight / metrics.lineHeight).toInt().coerceAtLeast(1)
        val glyphs: MutableList<PlacedGlyph>? = if (collect) ArrayList() else null
        var index = start
        var line = 0
        var x = metrics.marginLeft.toFloat()
        var lineHasContent = false
        var indentNextLine = start > 0 && text[start - 1] == '\n'
        val lineHeight = metrics.lineHeight
        val pageTop = metrics.marginTop.toFloat()
        val indent = metrics.fontSize * metrics.paragraphIndentEm

        while (index < limitEnd && line < maxLines) {
            val c = text[index]
            if (c == '\r' || c == '\n') {
                index += if (c == '\r' && index + 1 < limitEnd && text[index + 1] == '\n') 2 else 1
                line += 1
                x = metrics.marginLeft.toFloat()
                lineHasContent = false
                indentNextLine = true
                continue
            }

            if (!lineHasContent && indentNextLine) {
                x += indent
                indentNextLine = false
            }

            val width = charWidth(c)
            if (lineHasContent && x - metrics.marginLeft + width > metrics.contentWidth) {
                line += 1
                if (line >= maxLines) break
                x = metrics.marginLeft.toFloat()
                lineHasContent = false
                indentNextLine = false
                continue
            }

            glyphs?.add(
                PlacedGlyph(
                    char = c,
                    x = x,
                    y = pageTop + line * lineHeight,
                    cellWidth = width,
                    cellHeight = lineHeight,
                    underline = metrics.underline,
                ),
            )
            x += width
            index += 1
            lineHasContent = true
        }

        return WalkResult(index, glyphs ?: emptyList(), line, 0)
    }

    private fun walkVertical(
        start: Int,
        collect: Boolean,
        limitEnd: Int,
    ): WalkResult {
        val cellHeight = metrics.fontSize
        val columnPitch = metrics.fontSize * metrics.verticalColumnFactor
        val maxColumns = floor(metrics.contentWidth / columnPitch).toInt().coerceAtLeast(1)
        val rows = floor(metrics.contentHeight / cellHeight).toInt().coerceAtLeast(1)
        val glyphs: MutableList<PlacedGlyph>? = if (collect) ArrayList() else null
        var index = start
        var column = 0
        var row = 0
        var indentNextColumn = start > 0 && text[start - 1] == '\n'
        val indentRows = metrics.paragraphIndentEm.toInt().coerceAtLeast(0)
        val right = metrics.marginLeft + metrics.contentWidth

        while (index < limitEnd && column < maxColumns) {
            val c = text[index]
            if (c == '\r' || c == '\n') {
                index += if (c == '\r' && index + 1 < limitEnd && text[index + 1] == '\n') 2 else 1
                column += 1
                row = 0
                indentNextColumn = true
                continue
            }

            if (row == 0 && indentNextColumn) {
                row = indentRows.coerceAtMost(rows - 1)
                indentNextColumn = false
            }

            val x = right - (column + 1) * columnPitch +
                (columnPitch - metrics.fontSize) / 2f
            val y = metrics.marginTop + row * cellHeight
            val style = verticalStyle(c)
            glyphs?.add(
                PlacedGlyph(
                    char = c,
                    x = x + style.xOffset,
                    y = y + style.yOffset,
                    cellWidth = metrics.fontSize,
                    cellHeight = cellHeight,
                    rotationDegrees = style.rotationDegrees,
                    underline = metrics.underline,
                    drawChar = style.drawChar ?: c,
                ),
            )
            index += 1
            row += 1
            if (row >= rows) {
                column += 1
                row = 0
                indentNextColumn = false
            }
        }

        return WalkResult(index, glyphs ?: emptyList(), 0, column)
    }

    private fun charWidth(c: Char): Float = widthCache.getOrPut(c) {
        when {
            c == '\t' -> fontMetrics.charWidth(' ') * 4f
            c.code < 0x20 -> 0f
            else -> fontMetrics.charWidth(c).toFloat()
        }
    }

    private fun verticalStyle(c: Char): VerticalStyle {
        val verticalForm = VERTICAL_FORMS[c]
        if (verticalForm != null && font.canDisplay(verticalForm)) {
            return VerticalStyle(drawChar = verticalForm)
        }
        return when (c) {
            '。', '、', '，', '．', '：', '；' ->
                VerticalStyle(xOffset = metrics.fontSize * 0.58f, yOffset = -metrics.fontSize * 0.62f)
            '「', '」', '『', '』', '（', '）', '《', '》', '〈', '〉' ->
                VerticalStyle(rotationDegrees = 90f)
            '…', '—', '～' ->
                VerticalStyle(rotationDegrees = 90f)
            else ->
                if (isLatinOrDigit(c)) VerticalStyle(rotationDegrees = 90f) else VerticalStyle()
        }
    }

    private fun isLatinOrDigit(c: Char): Boolean =
        c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9'

    private data class VerticalStyle(
        val xOffset: Float = 0f,
        val yOffset: Float = 0f,
        val rotationDegrees: Float = 0f,
        val drawChar: Char? = null,
    )

    private companion object {
        val VERTICAL_FORMS: Map<Char, Char> = mapOf(
            '。' to '\uFE12',
            '、' to '\uFE11',
            '，' to '\uFE10',
            '．' to '\uFE12',
            '：' to '\uFE13',
            '；' to '\uFE14',
            '！' to '\uFE15',
            '？' to '\uFE16',
            '「' to '\uFE41',
            '」' to '\uFE42',
            '『' to '\uFE43',
            '』' to '\uFE44',
            '（' to '\uFE35',
            '）' to '\uFE36',
            '《' to '\uFE39',
            '》' to '\uFE3A',
            '〈' to '\uFE37',
            '〉' to '\uFE38',
            '…' to '\uFE19',
            '—' to '\uFE31',
        )
    }

    private data class WalkResult(
        val endOffset: Int,
        val glyphs: List<PlacedGlyph>,
        val lineCount: Int,
        val columnCount: Int,
    )
}
