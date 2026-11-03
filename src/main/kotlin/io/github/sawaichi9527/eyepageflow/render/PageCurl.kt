package io.github.sawaichi9527.eyepageflow.render

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

/**
 * 捲頁動畫的實際繪製（取代原本的「平移＋陰影」近似）。
 *
 * 做法：把目前頁面（[old]）摺線右側的內容鏡射到摺線左側，模擬翻起來的紙背，
 * 再疊上摺線陰影與紙張高光。純 2D 近似，不需要 3D 貼圖。
 */
object PageCurl {
    /** 翻頁時捲起來的紙帶寬度（像素）。 */
    fun bandWidth(width: Int): Float = (width * 0.12f).coerceAtMost(160f)

    fun paint(
        g2: Graphics2D,
        old: BufferedImage,
        new: BufferedImage,
        progress: Float,
    ) {
        val t = progress.coerceIn(0f, 1f)
        val width = new.width
        val height = new.height
        if (width <= 0 || height <= 0) return

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // 底層：新頁面。
        g2.drawImage(new, 0, 0, null)
        if (t <= 0f) {
            g2.drawImage(old, 0, 0, null)
            return
        }
        if (t >= 1f) return

        // 摺線：從右緣往左移動。
        val foldX = width * (1f - t)
        val band = bandWidth(width)

        // 還沒翻到的部分：直接沿用舊頁面。
        val left = (foldX + band).coerceAtMost(width.toFloat())
        if (left > 0f) {
            val clip = g2.clip
            g2.clipRect(0, 0, left.toInt(), height)
            g2.drawImage(old, 0, 0, null)
            g2.clip = clip
        }

        // 紙背：把摺線右側的內容以摺線為軸鏡射，並漸暗表示彎曲。
        val showWidth = (width - foldX).coerceAtMost(band)
        if (showWidth > 0.5f) {
            val back = BufferedImage(old.width, old.height, BufferedImage.TYPE_INT_ARGB)
            val backGraphics = back.createGraphics()
            backGraphics.drawImage(old, 0, 0, null)
            backGraphics.color = Color(0, 0, 0, 45)
            backGraphics.fillRect(0, 0, back.width, back.height)
            backGraphics.dispose()

            val clip = g2.clip
            g2.clipRect(foldX.toInt(), 0, showWidth.toInt().coerceAtLeast(1), height)
            val mirror = AffineTransform()
            mirror.translate(2.0 * foldX, 0.0)
            mirror.scale(-1.0, 1.0)
            g2.drawImage(back, mirror, null)
            g2.clip = clip
        }

        // 摺線陰影：讓摺線右側的紙背看起來有厚度。
        if (showWidth > 0.5f) {
            g2.paint = GradientPaint(
                foldX + showWidth * 0.55f,
                0f,
                Color(0, 0, 0, 0),
                foldX + showWidth,
                0f,
                Color(0, 0, 0, 95),
            )
            g2.fillRect(
                (foldX + showWidth * 0.55f).toInt(),
                0,
                (showWidth * 0.45f).toInt().coerceAtLeast(1),
                height,
            )
            g2.paint = Color.WHITE
        }

        // 捲起的紙邊高光。
        g2.color = Color(255, 255, 255, 60)
        g2.drawLine(foldX.toInt(), 0, foldX.toInt(), height)
        g2.color = Color.WHITE

        // 底層新頁面在翻頁處稍微壓暗，增加層次。
        val composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f * (1f - t))
        val shade = g2.create() as Graphics2D
        shade.composite = composite
        shade.color = Color.BLACK
        shade.fillRect(foldX.toInt(), 0, (width - foldX).toInt().coerceAtLeast(0), height)
        shade.dispose()
    }
}
