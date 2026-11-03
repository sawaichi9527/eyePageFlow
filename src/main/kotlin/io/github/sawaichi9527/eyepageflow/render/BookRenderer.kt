package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import io.github.sawaichi9527.eyepageflow.core.PageSpan
import io.github.sawaichi9527.eyepageflow.core.Pagination
import io.github.sawaichi9527.eyepageflow.core.PaginationEngine
import io.github.sawaichi9527.eyepageflow.core.PlacedGlyph
import io.github.sawaichi9527.eyepageflow.core.WritingMode
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import kotlin.math.max
import kotlin.math.min

object BookRenderer {
    /** 與畫面轉換無關的 FRC，用來量字形墨跡（旋轉字置中需要）。 */
    private val INK_FRC = java.awt.font.FontRenderContext(null, true, true)

    fun render(
        g2: Graphics2D,
        targetWidth: Int,
        targetHeight: Int,
        engine: PaginationEngine,
        pagination: Pagination,
        spreadStart: Int,
        theme: Theme,
        doublePage: Boolean,
        showBookAndDesktop: Boolean,
        font: Font,
    ) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
        )

        if (!showBookAndDesktop) {
            renderLowKey(
                g2 = g2,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                engine = engine,
                pagination = pagination,
                pageIndex = spreadStart,
                theme = theme,
                font = font,
            )
            return
        }

        val metrics = pagination.metrics
        val gutter = 10f
        val logicalBookWidth = if (doublePage) {
            metrics.pageWidth * 2f + gutter
        } else {
            metrics.pageWidth.toFloat()
        }
        val logicalBookHeight = metrics.pageHeight.toFloat()
        val desktopMargin = 28f
        val scale = min(
            (targetWidth - desktopMargin * 2f) / logicalBookWidth,
            (targetHeight - desktopMargin * 2f) / logicalBookHeight,
        )
        if (scale <= 0f) return

        val bookWidth = logicalBookWidth * scale
        val bookHeight = logicalBookHeight * scale
        val bookX = (targetWidth - bookWidth) / 2f
        val bookY = (targetHeight - bookHeight) / 2f
        val coverPad = 14f * scale

        TexturePainter.paint(
            g2,
            Rectangle(0, 0, targetWidth, targetHeight),
            theme.desktopColor,
            theme.desktopTexture,
        )

        g2.color = Color(
            theme.shadowColor.red,
            theme.shadowColor.green,
            theme.shadowColor.blue,
            90,
        )
        g2.fillRoundRect(
            (bookX - coverPad + 8f).toInt(),
            (bookY - coverPad + 12f).toInt(),
            (bookWidth + coverPad * 2f).toInt(),
            (bookHeight + coverPad * 2f).toInt(),
            24,
            24,
        )

        val cover = RoundRectangle2D.Float(
            bookX - coverPad,
            bookY - coverPad,
            bookWidth + coverPad * 2f,
            bookHeight + coverPad * 2f,
            16f,
            16f,
        )
        TexturePainter.paintRounded(g2, cover, theme.coverColor, theme.coverTexture)
        g2.color = theme.coverColor.darker()
        g2.stroke = BasicStroke(1.5f)
        g2.draw(cover)

        val pageWidth = metrics.pageWidth * scale
        val pageHeight = metrics.pageHeight * scale
        val gutterWidth = gutter * scale
        val leftPageRect = Rectangle(
            bookX.toInt(),
            bookY.toInt(),
            pageWidth.toInt(),
            pageHeight.toInt(),
        )
        val rightPageRect = Rectangle(
            (bookX + pageWidth + gutterWidth).toInt(),
            bookY.toInt(),
            pageWidth.toInt(),
            pageHeight.toInt(),
        )

        val pageIndices = pageIndicesForSpread(pagination, spreadStart, doublePage)
        paintPageBackground(g2, leftPageRect, theme, isLeft = true)
        if (doublePage) {
            paintPageBackground(g2, rightPageRect, theme, isLeft = false)
            drawGutter(g2, leftPageRect, gutterWidth, theme, pageHeight)
        }

        pageIndices.forEach { entry ->
            val pageIndex = entry.index
            if (pageIndex !in 0 until pagination.pageCount) return@forEach
            val rect = if (entry.left) leftPageRect else rightPageRect
            drawPageContent(
                g2 = g2,
                engine = engine,
                span = pagination.page(pageIndex),
                pageRect = rect,
                scale = scale,
                font = font,
                theme = theme,
                metrics = pagination.metrics,
                vertical = pagination.mode == WritingMode.VERTICAL,
            )
            drawPageNumber(
                g2 = g2,
                pageRect = rect,
                pageNumber = pageIndex + 1,
                theme = theme,
                font = font,
                scale = scale,
                left = entry.left,
                center = !doublePage,
            )
        }
    }

    private fun renderLowKey(
        g2: Graphics2D,
        targetWidth: Int,
        targetHeight: Int,
        engine: PaginationEngine,
        pagination: Pagination,
        pageIndex: Int,
        theme: Theme,
        font: Font,
    ) {
        TexturePainter.paint(
            g2,
            Rectangle(0, 0, targetWidth, targetHeight),
            theme.pageColor,
            theme.pageTexture,
        )
        if (pageIndex !in 0 until pagination.pageCount) return

        val metrics = pagination.metrics
        val scale = min(
            (targetWidth - 48f) / metrics.pageWidth,
            (targetHeight - 48f) / metrics.pageHeight,
        )
        if (scale <= 0f) return
        val contentWidth = metrics.pageWidth * scale
        val contentHeight = metrics.pageHeight * scale
        val x = (targetWidth - contentWidth) / 2f
        val y = (targetHeight - contentHeight) / 2f
        val pageRect = Rectangle(
            x.toInt(),
            y.toInt(),
            contentWidth.toInt(),
            contentHeight.toInt(),
        )
        drawPageContent(
            g2 = g2,
            engine = engine,
            span = pagination.page(pageIndex),
            pageRect = pageRect,
            scale = scale,
            font = font,
            theme = theme,
            metrics = pagination.metrics,
            vertical = pagination.mode == WritingMode.VERTICAL,
        )
        drawPageNumber(
            g2 = g2,
            pageRect = pageRect,
            pageNumber = pageIndex + 1,
            theme = theme,
            font = font,
            scale = scale,
            left = true,
            center = true,
        )
    }

    private fun pageIndicesForSpread(
        pagination: Pagination,
        spreadStart: Int,
        doublePage: Boolean,
    ): List<PageSlot> {
        if (!doublePage) {
            return listOf(PageSlot(spreadStart, left = true))
        }
        return if (pagination.mode == WritingMode.VERTICAL) {
            // Vertical text reads right to left: page 1 is the right page,
            // page 2 is the left page.
            listOf(
                PageSlot(spreadStart, left = false),
                PageSlot(spreadStart + 1, left = true),
            )
        } else {
            listOf(
                PageSlot(spreadStart, left = true),
                PageSlot(spreadStart + 1, left = false),
            )
        }
    }

    private fun paintPageBackground(
        g2: Graphics2D,
        rect: Rectangle,
        theme: Theme,
        isLeft: Boolean,
    ) {
        TexturePainter.paint(g2, rect, theme.pageColor, theme.pageTexture)

        val shadeWidth = (rect.width * 0.12f).toInt().coerceAtLeast(8)
        val x = if (isLeft) rect.x + rect.width - shadeWidth else rect.x
        val transparent = Color(0, 0, 0, 0)
        val dark = Color(0, 0, 0, 46)
        val gradient = if (isLeft) {
            java.awt.GradientPaint(
                x.toFloat(),
                0f,
                transparent,
                (x + shadeWidth).toFloat(),
                0f,
                dark,
            )
        } else {
            java.awt.GradientPaint(
                x.toFloat(),
                0f,
                dark,
                (x + shadeWidth).toFloat(),
                0f,
                transparent,
            )
        }
        g2.paint = gradient
        g2.fillRect(x, rect.y, shadeWidth, rect.height)
        g2.color = Color(0, 0, 0, 36)
        g2.stroke = BasicStroke(1f)
        g2.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1)
    }

    private fun drawGutter(
        g2: Graphics2D,
        leftPage: Rectangle,
        gutterWidth: Float,
        theme: Theme,
        pageHeight: Float,
    ) {
        val x = leftPage.x + leftPage.width
        g2.color = theme.gutterColor
        g2.fillRect(x, leftPage.y, gutterWidth.toInt().coerceAtLeast(1), pageHeight.toInt())
        g2.color = Color(0, 0, 0, 70)
        g2.drawLine(x, leftPage.y, x, leftPage.y + pageHeight.toInt())
    }

    private fun drawPageContent(
        g2: Graphics2D,
        engine: PaginationEngine,
        span: PageSpan,
        pageRect: Rectangle,
        scale: Float,
        font: Font,
        theme: Theme,
        metrics: PageMetrics,
        vertical: Boolean,
    ) {
        val content = engine.compose(span)

        val pageGraphics = g2.create() as Graphics2D
        pageGraphics.translate(pageRect.x, pageRect.y)
        pageGraphics.scale(scale.toDouble(), scale.toDouble())
        pageGraphics.clipRect(0, 0, metrics.pageWidth, metrics.pageHeight)
        pageGraphics.font = font
        pageGraphics.color = theme.textColor
        pageGraphics.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
        )

        if (metrics.underline) {
            drawPageUnderlines(pageGraphics, metrics, theme, vertical)
            // drawPageUnderlines 會把畫筆改成下劃線顏色，畫字前必須還原，
            // 否則文字會跟著變成下劃線顏色。
            pageGraphics.color = theme.textColor
        }
        if (content.glyphs.isEmpty()) {
            pageGraphics.dispose()
            return
        }

        val fontMetrics = pageGraphics.fontMetrics
        content.glyphs.forEach { glyph ->
            drawGlyph(pageGraphics, glyph, fontMetrics, vertical)
        }
        pageGraphics.dispose()
    }

    private fun drawPageUnderlines(
        g2: Graphics2D,
        metrics: PageMetrics,
        theme: Theme,
        vertical: Boolean,
    ) {
        g2.color = theme.underlineColor
        g2.stroke = BasicStroke(metrics.underlineWidth.coerceAtLeast(0.5f))
        if (!vertical) {
            val left = metrics.marginLeft
            val right = metrics.marginLeft + metrics.contentWidth
            PageUnderlines.horizontalYs(metrics).forEach { y ->
                g2.drawLine(left, y, right, y)
            }
        } else {
            val top = metrics.marginTop
            val bottom = metrics.marginTop + metrics.contentHeight
            PageUnderlines.verticalXs(metrics).forEach { x ->
                g2.drawLine(x, top, x, bottom)
            }
        }
    }

    private fun drawGlyph(
        g2: Graphics2D,
        glyph: PlacedGlyph,
        metrics: FontMetrics,
        vertical: Boolean,
    ) {
        val text = glyph.drawText
        // 直排時字格高度 = 字號，但字型的 ascent + descent 通常更大（Noto Serif CJK 26px 是 38px），
        // 若直接沿用基線，整個字會往下溢 6px，壓到下一個字（`第10段` 的數字就是這樣被壓到）。
        // 這裡把「行框」在字格內垂直置中，只調整垂直方向、不動水平位置。
        val lineBoxExcess = (metrics.ascent + metrics.descent) - glyph.cellHeight
        val verticalOffset = if (vertical && glyph.rotationDegrees == 0f && lineBoxExcess > 0f) {
            lineBoxExcess / 2f
        } else {
            0f
        }
        // 窄字（半角標點等）若沿用基線，墨跡會溢到下一格；把 em 框在字格內置中。
        val baseline = if (glyph.rotationDegrees == 0f && glyph.cellHeight < glyph.cellWidth) {
            glyph.y + glyph.cellHeight / 2f + (metrics.ascent - metrics.descent) / 2f
        } else {
            glyph.y + metrics.ascent - verticalOffset
        }
        if (glyph.rotationDegrees == 0f) {
            val baseFont = g2.font
            val fallback = if (baseFont.canDisplayUpTo(text) >= 0) {
                Typeface.symbolFont(text.codePointAt(0), baseFont.size)
            } else {
                null
            }
            if (fallback != null) {
                // emoji 等頁面字型沒有的字：換字型，用墨跡邊界縮放並置中，
                // 避免字面高度超過 1em 格子而和上下字重疊。
                val frc = g2.fontRenderContext
                var symbol = fallback
                var bounds = symbol.createGlyphVector(frc, text).visualBounds
                val maxWidth = glyph.cellWidth * 0.92
                val maxHeight = glyph.cellHeight * 0.92
                if (!bounds.isEmpty) {
                    if (bounds.width > maxWidth || bounds.height > maxHeight) {
                        val ratio = minOf(
                            (maxWidth / bounds.width).toFloat(),
                            (maxHeight / bounds.height).toFloat(),
                            1f,
                        )
                        symbol = fallback.deriveFont(fallback.size * ratio)
                        bounds = symbol.createGlyphVector(frc, text).visualBounds
                    }
                    g2.font = symbol
                    val inkX = glyph.x + ((glyph.cellWidth - bounds.width) / 2f).toFloat()
                    val inkY = glyph.y + ((glyph.cellHeight - bounds.height) / 2f).toFloat()
                    g2.drawString(
                        text,
                        inkX - bounds.minX.toFloat(),
                        inkY - bounds.minY.toFloat(),
                    )
                    g2.font = baseFont
                } else {
                    val fm = g2.getFontMetrics(fallback)
                    g2.font = fallback
                    g2.drawString(
                        text,
                        glyph.x + (glyph.cellWidth - fm.stringWidth(text)) / 2f,
                        glyph.y + (glyph.cellHeight - fm.height) / 2f + fm.ascent,
                    )
                    g2.font = baseFont
                }
            } else {
                g2.drawString(text, glyph.x, baseline)
            }
        } else {
            val old = g2.transform
            g2.translate(
                (glyph.x + glyph.cellWidth / 2f).toDouble(),
                (glyph.y + glyph.cellHeight / 2f).toDouble(),
            )
            g2.rotate(Math.toRadians(glyph.rotationDegrees.toDouble()))
            // 旋轉後「沿欄方向」對應 x 軸、「跨欄方向」對應 y 軸。
            // 這裡用墨跡邊界把字置中，不能靠基線推算：字型的 side bearing 與字高
            // （ascent + descent 可能大於字格）會讓數字、拉丁字的墨跡偏出字格，
            // 壓到前一個中文字，看起來就是「數字與文字重疊」。
            // 邊界要用與轉換無關的 FRC 量，否則會量到旋轉後的座標。
            val bounds = g2.font
                .createGlyphVector(INK_FRC, text)
                .visualBounds
            g2.drawString(
                text,
                -bounds.centerX.toFloat(),
                -bounds.centerY.toFloat(),
            )
            g2.transform = old
        }
    }

    private fun drawPageNumber(
        g2: Graphics2D,
        pageRect: Rectangle,
        pageNumber: Int,
        theme: Theme,
        font: Font,
        scale: Float,
        left: Boolean,
        center: Boolean,
    ) {
        val numberFont = Font(
            font.family,
            Font.PLAIN,
            max(9, (14f * scale).toInt()),
        )
        g2.font = numberFont
        g2.color = theme.pageNumberColor
        val text = pageNumber.toString()
        val width = g2.fontMetrics.stringWidth(text)
        val x = when {
            center -> pageRect.x + (pageRect.width - width) / 2
            left -> pageRect.x + (14f * scale).toInt()
            else -> pageRect.x + pageRect.width - width - (14f * scale).toInt()
        }
        val y = pageRect.y + pageRect.height - (12f * scale).toInt()
        g2.drawString(text, x, y)
    }

    private data class PageSlot(
        val index: Int,
        val left: Boolean,
    )
}
