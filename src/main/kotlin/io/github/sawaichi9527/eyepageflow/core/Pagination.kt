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

/** 不可出現在行首／欄首的字元（關閉類標點）。 */
internal val NO_LINE_START: Set<Char> = setOf(
    '。', '、', '，', '．', '：', '；', '！', '？',
    '）', '」', '』', '】', '》', '〉', '〕', '］', '｝',
    '…', '—', '～', '・',
)

/** 不可出現在行尾／欄尾的字元（開啟類標點）。 */
internal val NO_LINE_END: Set<Char> = setOf(
    '（', '「', '『', '【', '《', '〈', '〔', '［', '｛',
)

/** 查表版：避免每次禁則判斷都對 Char 裝箱。 */
private val NO_LINE_START_TABLE = BooleanArray(Char.MAX_VALUE.code + 1).also { table ->
    NO_LINE_START.forEach { table[it.code] = true }
}

private val NO_LINE_END_TABLE = BooleanArray(Char.MAX_VALUE.code + 1).also { table ->
    NO_LINE_END.forEach { table[it.code] = true }
}

private fun isNoLineStart(c: Char): Boolean = NO_LINE_START_TABLE[c.code]

private fun isNoLineEnd(c: Char): Boolean = NO_LINE_END_TABLE[c.code]

/** 只有「下一個字是行首禁則」或「本字是行尾禁則」時才需要做禁則前瞻。 */
private fun isPunctuationCandidate(
    current: Char,
    next: Char?,
    limitEnd: Int,
    index: Int,
): Boolean {
    if (index + 1 >= limitEnd || next == null) return false
    return isNoLineStart(next) || isNoLineEnd(current)
}

/** 直排時 CJK 標點（。、，：；）在字格內的位置。 */
enum class VerticalPunctuation(val label: String) {
    CENTER("置中"),
    TOP_RIGHT("右上"),
}

data class PageMetrics(
    val pageWidth: Int = 900,
    val pageHeight: Int = 920,
    val marginTop: Int = 32,
    val marginBottom: Int = 32,
    val marginLeft: Int = 44,
    val marginRight: Int = 44,
    val fontSize: Float = 26f,
    val lineHeightFactor: Float = 1.44f,
    val verticalColumnFactor: Float = 1.5f,
    val paragraphIndentEm: Float = 2f,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val underlineWidth: Float = 1f,
    val punctuationPlacement: VerticalPunctuation = VerticalPunctuation.CENTER,
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
    private val punctuationCache = HashMap<Char, VerticalStyle>()
    private val fontRenderContext = probeGraphics.fontRenderContext

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
            // 禁則：只在「下一個字是行首禁則字」或「本字是行尾禁則字」時才做完整前瞻。
            if (lineHasContent &&
                isPunctuationCandidate(c, text.getOrNull(index + 1), limitEnd, index) &&
                shouldBreakBeforeHorizontal(text, index, limitEnd, x, width)
            ) {
                line += 1
                if (line >= maxLines) break
                x = metrics.marginLeft.toFloat()
                lineHasContent = false
                indentNextLine = false
                continue
            }

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
        val columnHeight = rows * cellHeight
        val glyphs: MutableList<PlacedGlyph>? = if (collect) ArrayList() else null
        var index = start
        var column = 0
        var cursor = 0f
        var indentNextColumn = start > 0 && text[start - 1] == '\n'
        val indent = metrics.paragraphIndentEm * cellHeight
        val right = metrics.marginLeft + metrics.contentWidth

        while (index < limitEnd && column < maxColumns) {
            val c = text[index]
            if (c == '\r' || c == '\n') {
                index += if (c == '\r' && index + 1 < limitEnd && text[index + 1] == '\n') 2 else 1
                column += 1
                cursor = 0f
                indentNextColumn = true
                continue
            }

            if (cursor == 0f && indentNextColumn) {
                cursor = indent.coerceAtMost((columnHeight - cellHeight).coerceAtLeast(0f))
                indentNextColumn = false
            }

            val style = verticalStyle(c)
            val advance = verticalAdvance(c)
            // 禁則：欄底換欄前，先確認下一個字不會變成欄首禁則字。
            if (cursor > 0f &&
                isPunctuationCandidate(c, text.getOrNull(index + 1), limitEnd, index) &&
                shouldBreakBeforeVertical(text, index, limitEnd, cursor, advance, columnHeight)
            ) {
                column += 1
                cursor = 0f
                indentNextColumn = false
                continue
            }
            if (cursor > 0f && cursor + advance > columnHeight) {
                column += 1
                cursor = 0f
                indentNextColumn = false
                continue
            }

            val x = right - (column + 1) * columnPitch +
                (columnPitch - metrics.fontSize) / 2f
            val y = metrics.marginTop + cursor
            glyphs?.add(
                PlacedGlyph(
                    char = c,
                    x = x + style.xOffset,
                    y = y + style.yOffset,
                    cellWidth = metrics.fontSize,
                    cellHeight = advance,
                    rotationDegrees = style.rotationDegrees,
                    underline = metrics.underline,
                    drawChar = style.drawChar ?: c,
                ),
            )
            index += 1
            cursor += advance
        }

        return WalkResult(index, glyphs ?: emptyList(), 0, column)
    }

    /**
     * 直排的前進量：全角字（CJK、全角標點）固定 1em；
     * 半角字（拉丁字母、數字、半角標點）依字寬前進，避免旋倒的
     * 拉丁字串每個字母都被撐成一個全角格子。
     */
    private fun verticalAdvance(c: Char): Float {
        val width = charWidth(c)
        return if (width >= metrics.fontSize * 0.9f) {
            metrics.fontSize
        } else {
            width.coerceAtLeast(metrics.fontSize * 0.3f)
        }
    }

    /**
     * 橫排禁則：若目前這個字放下去之後，緊接的一整段關閉標點（`」！` 這類）
     * 會被擠到下一行，就提早換行，讓這段標點跟著前一個字一起走。
     * 反過來，若目前是開括號，也不能讓它單獨留在行尾。
     */
    private fun shouldBreakBeforeHorizontal(
        text: String,
        index: Int,
        limitEnd: Int,
        x: Float,
        width: Float,
    ): Boolean {
        val next = text.getOrNull(index + 1) ?: return false
        if (index + 1 >= limitEnd || next == '\r' || next == '\n') return false
        val usedBefore = x - metrics.marginLeft + width
        if (isNoLineEnd(text[index])) {
            return usedBefore + charWidth(next) > metrics.contentWidth
        }
        if (!isNoLineStart(next)) return false
        var used = usedBefore
        var cursor = index + 1
        while (cursor < limitEnd && isNoLineStart(text[cursor])) {
            used += charWidth(text[cursor])
            cursor += 1
        }
        return used > metrics.contentWidth
    }

    /** 直排禁則：同橫排，只是把「行」換成「欄」。 */
    private fun shouldBreakBeforeVertical(
        text: String,
        index: Int,
        limitEnd: Int,
        cursor: Float,
        advance: Float,
        columnHeight: Float,
    ): Boolean {
        val next = text.getOrNull(index + 1) ?: return false
        if (index + 1 >= limitEnd || next == '\r' || next == '\n') return false
        val usedBefore = cursor + advance
        if (isNoLineEnd(text[index])) {
            return usedBefore + verticalAdvance(next) > columnHeight
        }
        if (!isNoLineStart(next)) return false
        var used = usedBefore
        var probe = index + 1
        while (probe < limitEnd && isNoLineStart(text[probe])) {
            used += verticalAdvance(text[probe])
            probe += 1
        }
        return used > columnHeight
    }

    private fun charWidth(c: Char): Float = widthCache.getOrPut(c) {
        when {
            c == '\t' -> fontMetrics.charWidth(' ') * 4f
            c.code < 0x20 -> 0f
            else -> fontMetrics.charWidth(c).toFloat()
        }
    }

    private fun verticalStyle(c: Char): VerticalStyle {
        if (c in VERTICAL_PUNCTUATION_CHARS) {
            return when (metrics.punctuationPlacement) {
                VerticalPunctuation.CENTER -> VerticalStyle()
                VerticalPunctuation.TOP_RIGHT -> {
                    val form = VERTICAL_FORMS[c]
                    if (form != null && font.canDisplay(form)) {
                        VerticalStyle(drawChar = form)
                    } else {
                        punctuationCache.getOrPut(c) { topRightPunctuation(c) }
                    }
                }
            }
        }
        val verticalForm = VERTICAL_FORMS[c]
        if (verticalForm != null && font.canDisplay(verticalForm)) {
            return VerticalStyle(drawChar = verticalForm)
        }
        return when (c) {
            '「', '」', '『', '』', '（', '）', '《', '》', '〈', '〉' ->
                VerticalStyle(rotationDegrees = 90f)
            '…', '—', '～' ->
                VerticalStyle(rotationDegrees = 90f)
            else ->
                if (isRotatedLatin(c)) VerticalStyle(rotationDegrees = 90f) else VerticalStyle()
        }
    }

    /**
     * 直排時要旋倒的字元：拉丁字母、數字，以及拉丁字串裡常見的半角標點
     * （例如 `UTF-8` 的連字號）。半角標點若不旋轉，墨跡會落在基線下方而壓到下一格。
     */
    private fun isRotatedLatin(c: Char): Boolean =
        isLatinOrDigit(c) || c in ASCII_ROTATED

    /**
     * 把標點的墨跡移到字格右上角（標準中文直排位置）。
     * 用實際的字形視覺邊界計算位移，避免不同字型的基線差異。
     */
    private fun topRightPunctuation(c: Char): VerticalStyle {
        val bounds = font.createGlyphVector(fontRenderContext, c.toString()).visualBounds
        val centerX = (bounds.minX + bounds.width / 2f).toFloat()
        val centerY = (bounds.minY + bounds.height / 2f).toFloat()
        return VerticalStyle(
            xOffset = metrics.fontSize * 0.75f - centerX,
            yOffset = metrics.fontSize * 0.22f - fontMetrics.ascent - centerY,
        )
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
        const val ASCII_ROTATED = "-.,:;!?'\"()/\\%&+=#@_*<>|~^[]{}"

        val VERTICAL_PUNCTUATION_CHARS: Set<Char> =
            setOf('。', '、', '，', '．', '：', '；')

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
