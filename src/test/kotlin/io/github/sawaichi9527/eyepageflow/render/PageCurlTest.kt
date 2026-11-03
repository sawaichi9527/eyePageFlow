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

    private fun render(progress: Float): BufferedImage {
        val target = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = target.createGraphics()
        PageCurl.paint(g, page(Color.RED), page(Color.BLUE), progress)
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
    fun `progress zero shows the old page`() {
        val image = render(0f)
        assertTrue(isRed(image.getRGB(10, 50)))
        assertTrue(isRed(image.getRGB(190, 50)))
    }

    @Test
    fun `progress one shows the new page`() {
        val image = render(1f)
        assertTrue(isBlue(image.getRGB(10, 50)))
        assertTrue(isBlue(image.getRGB(190, 50)))
    }

    @Test
    fun `halfway keeps the left side old and reveals the new page on the right`() {
        val image = render(0.5f)
        assertTrue(isRed(image.getRGB(20, 50)), "左半仍然是舊頁面")
        assertTrue(isBlue(image.getRGB(180, 50)), "最右側已經是新頁面")
    }

    @Test
    fun `the curled band is darker than the flat page`() {
        val image = render(0.5f)
        val flat = image.getRGB(20, 50)
        val curled = image.getRGB(width - 30, 50)
        assertNotEquals(flat, curled, "紙背應該有陰影，不會和平面頁面相同")
    }

    @Test
    fun `progress is clamped`() {
        val image = render(2f)
        assertTrue(isBlue(image.getRGB(10, 50)))
        assertEquals(width, image.width)
    }
}
