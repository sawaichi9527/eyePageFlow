package io.github.sawaichi9527.eyepageflow.render

import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * 應用程式標記：非對稱眼眶（參考 eyeShell 的眼形）包住「筆」。
 *
 * 兩個尺寸策略，避免小圖示糊成一團：
 * - 48px 以上：標準眼眶（28）＋字高 130。
 * - 48px 以下：加粗眼眶（44）＋略小的字；字在 16px 仍不可能逐筆可讀，
 *   但眼眶會留下品牌識別，不會變成單純的米白方塊。
 *
 * 全部以 512 設計座標繪製，再依輸出的像素尺寸縮放；`筆` 使用專案內嵌的
 * Noto Serif CJK TC（OFL），沒有新增外部素材。
 */
object AppMark {
    const val GLYPH = "筆"

    private const val DESIGN_SIZE = 512.0
    private const val SMALL_THRESHOLD = 48

    private val TILE_TOP = Color(0x17243A)
    private val TILE_BOTTOM = Color(0x0A101C)
    private val CYAN = Color(0x3EC9F2)
    private val CREAM = Color(0xF2E7D0)

    fun render(size: Int): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()
        draw(g2, 0.0, 0.0, size)
        g2.dispose()
        return image
    }

    /** 在指定位置畫一個邊長 [size] 的標記（給空白狀態與圖示共用）。 */
    fun draw(g2: Graphics2D, x: Double, y: Double, size: Int) {
        if (size <= 0) return
        val c = g2.create() as Graphics2D
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        c.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        c.translate(x, y)
        c.scale(size / DESIGN_SIZE, size / DESIGN_SIZE)
        paintDesign(c, size)
        c.dispose()
    }

    private fun paintDesign(g2: Graphics2D, size: Int) {
        paintTile(g2)
        val small = size < SMALL_THRESHOLD
        g2.color = CYAN
        g2.stroke = BasicStroke(
            if (small) 44f else 28f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        g2.draw(scaledEye())
        paintGlyph(g2, if (small) 122.0 else 130.0, if (small) 12f else 6f)
    }

    private fun paintTile(g2: Graphics2D) {
        val tile = RoundRectangle2D.Double(8.0, 8.0, DESIGN_SIZE - 16, DESIGN_SIZE - 16, 112.0, 112.0)
        g2.paint = GradientPaint(0f, 0f, TILE_TOP, 0f, DESIGN_SIZE.toFloat(), TILE_BOTTOM)
        g2.fill(tile)
        g2.color = Color(255, 255, 255, 16)
        g2.stroke = BasicStroke(2f)
        g2.draw(tile)
    }

    /**
     * 眼形取自 eyeShell 的實際幾何：左尖 (65,255)、上緣頂點偏左、
     * 下緣重心偏右、右尖 (452,256)，刻意不是左右對稱的橄欖形。
     */
    private fun eyePath(): Path2D = Path2D.Double().apply {
        moveTo(65.0, 255.0)
        curveTo(140.0, 180.0, 220.0, 150.0, 300.0, 158.0)
        curveTo(380.0, 170.0, 425.0, 215.0, 452.0, 256.0)
        curveTo(405.0, 320.0, 350.0, 385.0, 295.0, 389.0)
        curveTo(200.0, 395.0, 115.0, 330.0, 65.0, 255.0)
        closePath()
    }

    private fun scaledEye(): Shape {
        val transform = AffineTransform()
        transform.translate(258.0, 256.0)
        transform.scale(1.06, 1.06)
        transform.translate(-258.0, -256.0)
        return transform.createTransformedShape(eyePath())
    }

    private fun paintGlyph(g2: Graphics2D, height: Double, stroke: Float) {
        val font = Typeface.iconFont(160f)
        val vector: GlyphVector = font.createGlyphVector(FONT_CONTEXT, GLYPH)
        val outline = vector.getOutline()
        val scale = height / outline.bounds2D.height
        val c = g2.create() as Graphics2D
        c.translate(254.0, 256.0)
        c.scale(scale, scale)
        c.translate(-outline.bounds2D.centerX, -outline.bounds2D.centerY)
        c.color = CREAM
        c.fill(outline)
        c.stroke = BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        c.draw(outline)
        c.dispose()
    }

    private val FONT_CONTEXT = FontRenderContext(null, true, true)
}
