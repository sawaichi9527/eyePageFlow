package io.github.sawaichi9527.eyepageflow.render

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

/**
 * 捲頁動畫的實際繪製（取代原本的「平移＋陰影」近似）。
 *
 * 做法：摺線把舊頁面分成「還沒翻到」與「已經翻過去」兩段，已翻過去的那段以摺線為軸
 * 鏡射到另一側當紙背（背面），再疊上摺線陰影與紙張高光。純 2D 近似，不需要 3D 貼圖。
 *
 * [forward] 決定摺線方向：`true` 是摺線由右緣往左跑，`false` 是由左緣往右跑。
 * 這在橫排代表「下一頁／上一頁」，直排（右翻書）則是相反，
 * 由呼叫端（`BookCanvas.animateTo`）依寫作模式換算。
 *
 * [region] 限制捲頁發生的範圍（預設整個畫面）。雙頁直排只掀左半那張紙，
 * 所以會傳入左半頁的矩形；範圍外直接顯示新頁面。
 */
object PageCurl {
    /** 翻頁時捲起來的紙帶寬度（像素）。 */
    fun bandWidth(width: Int): Float = (width * 0.12f).coerceAtMost(160f)

    fun paint(
        g2: Graphics2D,
        old: BufferedImage,
        new: BufferedImage,
        progress: Float,
        forward: Boolean = true,
        region: Rectangle = Rectangle(0, 0, new.width, new.height),
    ) {
        val t = progress.coerceIn(0f, 1f)
        val height = new.height
        val width = region.width
        if (width <= 0 || height <= 0 || new.width <= 0) return

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // 底層：新頁面（整個畫布）。雙頁時「不是正在翻的那半頁」會立刻換成新頁面，
        // 這與實體書掀開一張紙後底下的跨頁已經換掉一致。
        g2.drawImage(new, 0, 0, null)
        if (t <= 0f) {
            g2.drawImage(old, 0, 0, null)
            return
        }
        if (t >= 1f) return

        // 只在 region 內做捲頁，座標系平移到 region 左上角。
        val layer = g2.create() as Graphics2D
        layer.translate(-region.x.toDouble(), -region.y.toDouble())
        layer.clipRect(0, 0, width, height)

        // 摺線位置：往前翻由右往左移動，往回翻由左往右移動。
        val foldX = if (forward) width * (1f - t) else width * t
        val band = bandWidth(width)

        // 紙背佔用的橫向範圍：往前翻在摺線左側，往回翻在摺線右側。
        val backFrom = if (forward) (foldX - band).coerceAtLeast(0f) else foldX
        val backTo = if (forward) foldX else (foldX + band).coerceAtMost(width.toFloat())

        drawFlatRemainder(layer, old, width, height, forward, backFrom, backTo)
        drawPaperBack(layer, old, height, foldX, backFrom, backTo)
        drawCreaseShadow(layer, height, foldX, backFrom, backTo, forward)
        drawBaseShade(layer, width, height, foldX, t, forward)
        layer.dispose()
    }

    /** 還沒翻到的舊頁面：往前翻時留在左側，往回翻時留在右側。 */
    private fun drawFlatRemainder(
        g2: Graphics2D,
        old: BufferedImage,
        width: Int,
        height: Int,
        forward: Boolean,
        backFrom: Float,
        backTo: Float,
    ) {
        val clip = g2.clip
        if (forward) {
            if (backFrom > 0f) {
                g2.clipRect(0, 0, backFrom.toInt(), height)
                g2.drawImage(old, 0, 0, null)
            }
        } else if (backTo < width) {
            val x = backTo.toInt().coerceAtLeast(0)
            g2.clipRect(x, 0, (width - x).coerceAtLeast(1), height)
            g2.drawImage(old, 0, 0, null)
        }
        g2.clip = clip
    }

    /** 紙背：把已翻過去的那一段以摺線為軸鏡射到另一側，並壓暗表示彎曲。 */
    private fun drawPaperBack(
        g2: Graphics2D,
        old: BufferedImage,
        height: Int,
        foldX: Float,
        backFrom: Float,
        backTo: Float,
    ) {
        if (backTo - backFrom <= 0.5f) return

        val back = BufferedImage(old.width, old.height, BufferedImage.TYPE_INT_ARGB)
        val backGraphics = back.createGraphics()
        backGraphics.drawImage(old, 0, 0, null)
        backGraphics.color = Color(0, 0, 0, 45)
        backGraphics.fillRect(0, 0, back.width, back.height)
        backGraphics.dispose()

        val clip = g2.clip
        g2.clipRect(
            backFrom.toInt(),
            0,
            (backTo - backFrom).toInt().coerceAtLeast(1),
            height,
        )
        // x' = 2 * foldX - x：兩個方向都繞著摺線鏡射，只是取樣的來源邊不同。
        val mirror = AffineTransform()
        mirror.translate(2.0 * foldX, 0.0)
        mirror.scale(-1.0, 1.0)
        g2.drawImage(back, mirror, null)
        g2.clip = clip
    }

    /** 摺線陰影：越靠摺線越暗，讓紙背看起來有厚度。 */
    private fun drawCreaseShadow(
        g2: Graphics2D,
        height: Int,
        foldX: Float,
        backFrom: Float,
        backTo: Float,
        forward: Boolean,
    ) {
        if (backTo - backFrom <= 0.5f) return

        val clear = Color(0, 0, 0, 0)
        val dark = Color(0, 0, 0, 95)
        g2.paint = if (forward) {
            GradientPaint(backFrom, 0f, clear, foldX, 0f, dark)
        } else {
            GradientPaint(foldX, 0f, dark, backTo, 0f, clear)
        }
        g2.fillRect(
            backFrom.toInt(),
            0,
            (backTo - backFrom).toInt().coerceAtLeast(1),
            height,
        )
        g2.paint = Color.WHITE

        // 捲起的紙邊高光。
        g2.color = Color(255, 255, 255, 60)
        g2.drawLine(foldX.toInt(), 0, foldX.toInt(), height)
        g2.color = Color.WHITE
    }

    /** 已經露出的新頁面靠近摺線處稍微壓暗，增加層次。 */
    private fun drawBaseShade(
        g2: Graphics2D,
        width: Int,
        height: Int,
        foldX: Float,
        t: Float,
        forward: Boolean,
    ) {
        val composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f * (1f - t))
        val shade = g2.create() as Graphics2D
        shade.composite = composite
        shade.color = Color.BLACK
        if (forward) {
            val x = foldX.toInt().coerceAtLeast(0)
            shade.fillRect(x, 0, (width - x).coerceAtLeast(0), height)
        } else {
            shade.fillRect(0, 0, foldX.toInt().coerceAtLeast(0), height)
        }
        shade.dispose()
    }
}
