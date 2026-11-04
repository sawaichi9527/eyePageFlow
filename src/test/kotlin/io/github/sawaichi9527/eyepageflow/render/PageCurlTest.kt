package io.github.sawaichi9527.eyepageflow.render

import java.awt.Color
import java.awt.image.BufferedImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PageCurlTest {
    private val width = 200
    private val height = 100

    private fun page(color: Color): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
            val g = image.createGraphics()
            g.color = color
            g.fillRect(0, 0, width, height)
            g.dispose()
        }

    private fun render(
        progress: Float,
        forward: Boolean = true,
        region: java.awt.Rectangle? = null,
    ): BufferedImage {
        val target = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = target.createGraphics()
        if (region == null) {
            PageCurl.paint(g, page(Color.RED), page(Color.BLUE), progress, forward)
        } else {
            PageCurl.paint(g, page(Color.RED), page(Color.BLUE), progress, forward, region)
        }
        g.dispose()
        return target
    }

    private fun isRed(rgb: Int): Boolean {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return r > 120 && g < 90 && b < 90
    }

    private fun isBlue(rgb: Int): Boolean {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return b > 120 && r < 90 && g < 90
    }

    @Test
    fun `progress zero shows the old page in both directions`() {
        listOf(true, false).forEach { forward ->
            val image = render(0f, forward)
            assertTrue(isRed(image.getRGB(10, 50)), "forward=$forward 左側應為舊頁面")
            assertTrue(isRed(image.getRGB(190, 50)), "forward=$forward 右側應為舊頁面")
        }
    }

    @Test
    fun `progress one shows the new page in both directions`() {
        listOf(true, false).forEach { forward ->
            val image = render(1f, forward)
            assertTrue(isBlue(image.getRGB(10, 50)), "forward=$forward")
            assertTrue(isBlue(image.getRGB(190, 50)), "forward=$forward")
        }
    }

    @Test
    fun `forward turn keeps the left side old and reveals the new page on the right`() {
        val image = render(0.5f, forward = true)
        assertTrue(isRed(image.getRGB(20, 50)), "左半仍然是舊頁面")
        assertTrue(isBlue(image.getRGB(180, 50)), "最右側已經是新頁面")
    }

    @Test
    fun `backward turn reveals the new page on the left and keeps the right side old`() {
        val image = render(0.5f, forward = false)
        assertTrue(isBlue(image.getRGB(20, 50)), "翻回上一頁時左側先換成新頁面")
        assertTrue(isRed(image.getRGB(180, 50)), "右半仍然是舊頁面")
    }

    @Test
    fun `forward and backward turns are mirrored at the same progress`() {
        val forward = render(0.5f, forward = true)
        val backward = render(0.5f, forward = false)
        listOf(10, 50, 190).forEach { x ->
            val left = forward.getRGB(x, 50)
            val right = backward.getRGB(width - 1 - x, 50)
            assertEquals(left, right, "x=$x 的前翻結果應等於鏡射後的回翻結果")
        }
    }

    @Test
    fun `the curled band is darker than the flat page`() {
        val forward = render(0.5f, forward = true)
        val flat = forward.getRGB(20, 50)
        val band = forward.getRGB(88, 50)
        assertTrue(isRed(band), "紙背仍是舊頁面的鏡射內容")
        assertNotEquals(flat, band, "紙背應該比平面頁面暗")

        val backward = render(0.5f, forward = false)
        val backBand = backward.getRGB(112, 50)
        assertTrue(isRed(backBand), "回翻時紙背在摺線右側")
        assertNotEquals(backward.getRGB(180, 50), backBand, "紙背應該比平面頁面暗")
    }

    @Test
    fun `progress is clamped`() {
        listOf(true, false).forEach { forward ->
            val image = render(2f, forward)
            assertTrue(isBlue(image.getRGB(10, 50)), "forward=$forward")
            assertEquals(width, image.width)
        }
    }

    @Test
    fun `a region keeps the turn inside that half of the spread`() {
        // 直排雙頁：只掀左半那張紙。
        val region = java.awt.Rectangle(0, 0, width / 2, height)
        val image = render(0.5f, forward = false, region = region)

        assertTrue(isBlue(image.getRGB(10, 50)), "範圍內左側：新頁面已露出")
        assertTrue(isRed(image.getRGB(80, 50)), "範圍內右側：舊頁面還沒翻完")
        assertTrue(isBlue(image.getRGB(150, 50)), "範圍外：直接顯示新頁面，不參與捲頁")
    }
}
