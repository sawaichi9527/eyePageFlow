package io.github.sawaichi9527.eyepageflow.render

import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.Shape
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

/**
 * 顯示設定選的「自訂圖片」。
 *
 * 圖片依路徑快取（最多 8 張），每次重繪都只做縮放；縮放採 cover，
 * 先依短邊放大、再置中裁切，避免桌面／封面／頁面被拉成不對的比例。
 * 檔案不存在或不是圖片時回傳 false，呼叫端退回材質或純色。
 */
object ImagePainter {
    private const val MAX_ENTRIES = 8

    private val cache = object : LinkedHashMap<String, BufferedImage>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, BufferedImage>?,
        ): Boolean = size > MAX_ENTRIES
    }

    fun paint(g2: Graphics2D, rect: Rectangle, path: String?): Boolean {
        if (rect.width <= 0 || rect.height <= 0) return false
        val image = image(path) ?: return false
        val clip = g2.clip
        g2.clipRect(rect.x, rect.y, rect.width, rect.height)
        drawCover(g2, image, rect)
        g2.clip = clip
        return true
    }

    fun paintRounded(g2: Graphics2D, shape: Shape, path: String?): Boolean {
        val image = image(path) ?: return false
        val clip = g2.clip
        g2.clip(shape)
        drawCover(g2, image, shape.bounds)
        g2.clip = clip
        return true
    }

    /** 測試與診斷用。 */
    fun clearCache() {
        synchronized(cache) { cache.clear() }
    }

    private fun drawCover(g2: Graphics2D, image: BufferedImage, rect: Rectangle) {
        if (image.width <= 0 || image.height <= 0) return
        val scale = max(
            rect.width.toDouble() / image.width,
            rect.height.toDouble() / image.height,
        )
        val width = (image.width * scale).toInt().coerceAtLeast(1)
        val height = (image.height * scale).toInt().coerceAtLeast(1)
        val x = rect.x + (rect.width - width) / 2
        val y = rect.y + (rect.height - height) / 2
        g2.drawImage(image, x, y, width, height, null)
    }

    private fun image(path: String?): BufferedImage? {
        if (path.isNullOrBlank()) return null
        synchronized(cache) {
            cache[path]?.let { return it }
        }
        val loaded = try {
            ImageIO.read(File(path))
        } catch (_: Exception) {
            null
        } ?: return null
        synchronized(cache) { cache[path] = loaded }
        return loaded
    }
}
