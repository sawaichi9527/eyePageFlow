package io.github.sawaichi9527.eyepageflow.render

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
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
        val markSize = (210 * scale).toInt().coerceAtLeast(96)
        AppMark.draw(g2, centerX - markSize / 2.0, centerY - markSize / 2.0, markSize)

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

    private fun drawCentered(g2: Graphics2D, text: String, centerX: Double, baseline: Double) {
        val width = g2.fontMetrics.stringWidth(text)
        g2.drawString(text, (centerX - width / 2.0).toFloat(), baseline.toFloat())
    }
}
