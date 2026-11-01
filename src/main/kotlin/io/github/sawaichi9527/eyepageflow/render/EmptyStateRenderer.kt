package io.github.sawaichi9527.eyepageflow.render

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.MultipleGradientPaint
import java.awt.RadialGradientPaint
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import kotlin.math.min

object EmptyStateRenderer {
    fun render(
        g2: Graphics2D,
        width: Int,
        height: Int,
        theme: Theme,
        fontFamily: String = Typeface.DEFAULT_FAMILY,
    ) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
        )
        TexturePainter.paint(
            g2,
            Rectangle(0, 0, width, height),
            theme.desktopColor,
            theme.desktopTexture,
        )

        val scale = min(width / 1600.0, height / 900.0).coerceAtLeast(0.55)
        val centerX = width / 2.0
        val centerY = height / 2.0 - 30 * scale
        drawGlow(g2, centerX, centerY, 120 * scale)
        drawBook(g2, centerX, centerY, scale, theme)

        val family = Typeface.resolveFamily(fontFamily)
        g2.font = Font(family, Font.BOLD, (52 * scale).toInt().coerceAtLeast(18))
        g2.color = Color(0xF6E7B8)
        drawCentered(g2, "eyePageFlow", centerX, centerY + 150 * scale)

        g2.font = Font(family, Font.PLAIN, (22 * scale).toInt().coerceAtLeast(11))
        g2.color = Color(0xD8C9A8)
        drawCentered(g2, "簡單，就好。", centerX, centerY + 195 * scale)

        g2.font = Font(family, Font.PLAIN, (16 * scale).toInt().coerceAtLeast(10))
        g2.color = Color(255, 255, 255, 120)
        drawCentered(g2, "雙擊開啟檔案，或將檔案拖曳到此處", centerX, centerY + 250 * scale)
    }

    private fun drawGlow(g2: Graphics2D, centerX: Double, centerY: Double, radius: Double) {
        val paint = RadialGradientPaint(
            centerX.toFloat(),
            centerY.toFloat(),
            radius.toFloat().coerceAtLeast(1f),
            floatArrayOf(0f, 1f),
            arrayOf(
                Color(255, 226, 150, 130),
                Color(255, 226, 150, 0),
            ),
            MultipleGradientPaint.CycleMethod.NO_CYCLE,
        )
        g2.paint = paint
        val diameter = (radius * 2).toInt()
        g2.fillOval(
            (centerX - radius).toInt(),
            (centerY - radius).toInt(),
            diameter,
            diameter,
        )
        g2.paint = Color.WHITE
    }

    private fun drawBook(
        g2: Graphics2D,
        centerX: Double,
        centerY: Double,
        scale: Double,
        theme: Theme,
    ) {
        val pageWidth = 82 * scale
        val pageHeight = 112 * scale
        val gap = 3 * scale

        val left = Path2D.Double().apply {
            moveTo(centerX - gap, centerY - pageHeight / 2)
            curveTo(
                centerX - pageWidth * 0.45,
                centerY - pageHeight * 0.62,
                centerX - pageWidth,
                centerY - pageHeight * 0.54,
                centerX - pageWidth,
                centerY - pageHeight * 0.46,
            )
            lineTo(centerX - pageWidth, centerY + pageHeight * 0.46)
            curveTo(
                centerX - pageWidth * 0.55,
                centerY + pageHeight * 0.58,
                centerX - pageWidth * 0.25,
                centerY + pageHeight * 0.54,
                centerX - gap,
                centerY + pageHeight / 2,
            )
            closePath()
        }
        val right = Path2D.Double().apply {
            moveTo(centerX + gap, centerY - pageHeight / 2)
            curveTo(
                centerX + pageWidth * 0.45,
                centerY - pageHeight * 0.62,
                centerX + pageWidth,
                centerY - pageHeight * 0.54,
                centerX + pageWidth,
                centerY - pageHeight * 0.46,
            )
            lineTo(centerX + pageWidth, centerY + pageHeight * 0.46)
            curveTo(
                centerX + pageWidth * 0.55,
                centerY + pageHeight * 0.58,
                centerX + pageWidth * 0.25,
                centerY + pageHeight * 0.54,
                centerX + gap,
                centerY + pageHeight / 2,
            )
            closePath()
        }

        g2.color = theme.pageColor.darker()
        g2.fill(left)
        g2.fill(right)
        g2.color = theme.coverColor
        g2.fill(
            RoundRectangle2D.Double(
                centerX - gap * 1.5,
                centerY - pageHeight / 2,
                gap * 3,
                pageHeight,
                2.0,
                2.0,
            ),
        )

        g2.color = Color(0, 0, 0, 40)
        g2.draw(left)
        g2.draw(right)
    }

    private fun drawCentered(g2: Graphics2D, text: String, centerX: Double, baseline: Double) {
        val width = g2.fontMetrics.stringWidth(text)
        g2.drawString(text, (centerX - width / 2.0).toFloat(), baseline.toFloat())
    }
}
