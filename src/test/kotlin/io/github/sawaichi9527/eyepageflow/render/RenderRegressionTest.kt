package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import io.github.sawaichi9527.eyepageflow.core.PaginationEngine
import io.github.sawaichi9527.eyepageflow.core.PlacedGlyph
import io.github.sawaichi9527.eyepageflow.core.VerticalPunctuation
import io.github.sawaichi9527.eyepageflow.core.WritingMode
import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * 書頁渲染的回歸測試。
 *
 * 這裡刻意用「像素性質」而不是整張 golden image 比對：Windows 與 Linux 的
 * CJK 字型不同（新細明體 vs Noto），整張比對會因平台而紅。改為驗證幾個
 * 與字型無關、但先前真的壞過的性質：字色、字級、窄字不溢出、標點位置。
 */
class RenderRegressionTest {
    private val metrics = PageMetrics(
        pageWidth = 240,
        pageHeight = 300,
        marginTop = 20,
        marginBottom = 20,
        marginLeft = 20,
        marginRight = 20,
        fontSize = 40f,
        lineHeightFactor = 1.4f,
        verticalColumnFactor = 1.5f,
        paragraphIndentEm = 0f,
        underline = true,
        underlineWidth = 1f,
    )
    private val font = Typeface.pageFont(metrics)

    private val theme = Theme.REALISTIC.copy(
        pageColor = Color.WHITE,
        pageTexture = TextureKind.NONE,
        textColor = Color(0x11, 0x22, 0x33),
        underlineColor = Color(0x00, 0x66, 0xFF),
        pageNumberColor = Color(0x88, 0x88, 0x88),
    )

    @Test
    fun `text keeps its own colour while underlines are drawn`() {
        assumeTrue(font.canDisplay('檔'), "需要能顯示 CJK 的字型")
        val page = render("檔檔檔檔", metrics)

        var textPixels = 0
        var linePixels = 0
        for (y in 0 until page.image.height) {
            for (x in 0 until page.image.width) {
                val rgb = page.image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                if (r < 100 && g < 100 && b < 120) textPixels++
                if (b > 180 && r < 120 && g < 170) linePixels++
            }
        }

        assertTrue(linePixels > 20, "下劃線應以下劃線顏色繪製")
        assertTrue(
            textPixels > 50,
            "文字必須使用文字顏色；若沿用下劃線顏色，深色像素會是 0（實測 $textPixels）",
        )
    }

    @Test
    fun `glyph ink matches the requested font size`() {
        assumeTrue(font.canDisplay('檔'), "需要能顯示 CJK 的字型")
        val page = render("檔檔", metrics)
        val glyph = page.glyphs.first()
        val cell = cellOf(page, glyph)

        val ink = inkBounds(page.image, cell)
        assertNotNull(ink, "字應該要畫出來")
        val expected = metrics.fontSize * 0.6f
        assertTrue(
            ink!!.height >= expected,
            "字墨高度 ${ink.height}px 應接近字級 ${metrics.fontSize}px（下限 ${expected}px）",
        )
    }

    @Test
    fun `narrow ascii glyph stays inside its advance box`() {
        val page = render("UTF-8", metrics)
        val hyphen = page.glyphs.firstOrNull { it.char == '-' }
        assertNotNull(hyphen, "應包含連字號")
        val cell = cellOf(page, hyphen!!)
        val expanded = Rectangle(cell.x - 1, cell.y - 1, cell.width + 2, cell.height + 2)

        val ink = inkBounds(page.image, expanded)
        assertNotNull(ink, "連字號應該要畫出來")
        assertTrue(
            ink!!.y >= cell.y - 1 && ink.y + ink.height <= cell.y + cell.height + 1,
            "連字號墨跡 ${ink.y}..${ink.y + ink.height} 超出字格 ${cell.y}..${cell.y + cell.height}",
        )
    }

    @Test
    fun `top right punctuation sits in the cell's upper right quadrant`() {
        assumeTrue(font.canDisplay('，'), "需要能顯示 CJK 的字型")
        val topRight = metrics.copy(punctuationPlacement = VerticalPunctuation.TOP_RIGHT)
        val page = render("檔，檔", topRight)
        val comma = page.glyphs.first { it.char == '，' }
        val cell = cellOf(page, comma)

        val ink = inkBounds(page.image, cell)
        assertNotNull(ink, "標點應該要畫在字格內")
        val centerX = ink!!.x + ink.width / 2
        val centerY = ink.y + ink.height / 2
        assertTrue(centerX > cell.x + cell.width / 2, "標點應靠右 ($centerX vs ${cell.x + cell.width / 2})")
        assertTrue(centerY < cell.y + cell.height / 2, "標點應靠上 ($centerY vs ${cell.y + cell.height / 2})")
    }

    @Test
    fun `vertical cjk ink stays inside its cell`() {
        // 直排字格高度 = 字號，但字型的 ascent + descent 更大；
        // 沿用基線會讓墨跡往下溢，壓到下一個字（`第10段` 的數字）。
        // 一次只畫一個字，避免掃描範圍吃到相鄰字的墨跡。
        listOf('第', '檔', '段').forEach { c ->
            assumeTrue(font.canDisplay(c), "需要能顯示 $c 的字型")
            val page = render(c.toString(), metrics)
            val glyph = page.glyphs.single()
            val slot = slotOf(page, glyph)
            val search = Rectangle(slot.x - 6, slot.y - 6, slot.width + 12, slot.height + 12)
            val ink = inkBounds(page.image, search)
            assertNotNull(ink, "'$c' 應該要畫在字格內")
            assertTrue(
                ink!!.y >= slot.y - 1 && ink.y + ink.height <= slot.y + slot.height + 1,
                "'$c' 墨跡 ${ink.y}..${ink.y + ink.height} 超出字格 ${slot.y}..${slot.y + slot.height}",
            )
        }
    }

    @Test
    fun `rotated narrow digit ink fits its advance slot`() {
        // 26px 是預設字級；`1` 的字寬不到 0.3em，前進量若只取 0.3em，
        // 旋倒後的墨跡會比字格高，直接壓到前後的中文字（第1段）。
        val narrow = metrics.copy(fontSize = 26f)
        val page = render("1", narrow)
        val glyph = page.glyphs.single()
        assertTrue(glyph.rotationDegrees != 0f, "數字應該被旋倒")

        val slot = slotOf(page, glyph)
        val search = Rectangle(slot.x - 6, slot.y - 6, slot.width + 12, slot.height + 12)
        val ink = inkBounds(page.image, search)
        assertNotNull(ink, "旋倒的 '1' 應該要畫在字格內")
        assertTrue(
            ink!!.y >= slot.y - 1 && ink.y + ink.height <= slot.y + slot.height + 1,
            "旋倒的 '1' 墨跡 ${ink.y}..${ink.y + ink.height} 超出字格 ${slot.y}..${slot.y + slot.height}",
        )
    }

    // --- helpers -----------------------------------------------------------

    /**
     * 單一字的字格：寬度固定一個字，高度用該字實際的前進量。
     * 旋倒的半角字只有 `cellHeight`（約半個字），不能套 `fontSize`。
     */
    private fun slotOf(page: RenderedPage, glyph: PlacedGlyph): Rectangle {
        val first = page.glyphs.first()
        var top = first.y
        var index = 0
        while (index < page.glyphs.size && page.glyphs[index] !== glyph) {
            top += page.glyphs[index].cellHeight
            index++
        }
        return Rectangle(
            PAGE_MARGIN + first.x.toInt(),
            PAGE_MARGIN + top.toInt(),
            glyph.cellWidth.toInt(),
            glyph.cellHeight.toInt(),
        )
    }

    private data class RenderedPage(
        val image: BufferedImage,
        val glyphs: List<PlacedGlyph>,
    )

    private fun render(
        text: String,
        pageMetrics: PageMetrics,
    ): RenderedPage {
        val engine = PaginationEngine(text, pageMetrics, WritingMode.VERTICAL, font)
        val pagination = engine.paginate()
        val image = BufferedImage(
            pageMetrics.pageWidth + PAGE_MARGIN * 2,
            pageMetrics.pageHeight + PAGE_MARGIN * 2,
            BufferedImage.TYPE_INT_ARGB,
        )
        val graphics = image.createGraphics()
        BookRenderer.render(
            g2 = graphics,
            targetWidth = image.width,
            targetHeight = image.height,
            engine = engine,
            pagination = pagination,
            spreadStart = 0,
            theme = theme,
            doublePage = false,
            showBookAndDesktop = false,
            font = font,
        )
        graphics.dispose()
        val glyphs = engine.compose(pagination.page(0)).glyphs
        return RenderedPage(image, glyphs)
    }

    /**
     * 推算某個字形在畫面中的字格。低調模式的縮放固定為 1、頁面置中，
     * 所以位移量就是 [PAGE_MARGIN]。欄內位置由每個字的前進量累加得出，
     * 這樣標點即使有位移也能還原它真正的字格。
     */
    private fun cellOf(page: RenderedPage, glyph: PlacedGlyph): Rectangle {
        val fontSize = metrics.fontSize.toInt()
        val first = page.glyphs.first()
        var top = first.y
        var index = 0
        while (index < page.glyphs.size && page.glyphs[index] !== glyph) {
            top += page.glyphs[index].cellHeight
            index++
        }
        return Rectangle(PAGE_MARGIN + first.x.toInt(), PAGE_MARGIN + top.toInt(), fontSize, fontSize)
    }

    private fun inkBounds(image: BufferedImage, area: Rectangle): Rectangle? {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        val left = area.x.coerceAtLeast(0)
        val top = area.y.coerceAtLeast(0)
        val right = (area.x + area.width).coerceAtMost(image.width)
        val bottom = (area.y + area.height).coerceAtMost(image.height)
        for (y in top until bottom) {
            for (x in left until right) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                if (r < 100 && g < 100 && b < 120) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return null
        return Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    private companion object {
        /** renderLowKey 會保留 24px 邊界，且此時縮放剛好是 1。 */
        const val PAGE_MARGIN = 24
    }
}
