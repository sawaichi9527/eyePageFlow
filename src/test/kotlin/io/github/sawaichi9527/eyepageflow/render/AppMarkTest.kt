package io.github.sawaichi9527.eyepageflow.render

import java.awt.image.BufferedImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppMarkTest {
    @Test
    fun `large mark has a transparent corner and both brand colours`() {
        val image = AppMark.render(256)

        assertEquals(0, image.getRGB(0, 0) ushr 24, "圓角外應為透明")
        assertTrue(cyanPixels(image) > 500, "眼眶應為青色（實測 ${cyanPixels(image)}）")
        assertTrue(creamPixels(image) > 300, "字應為米白（實測 ${creamPixels(image)}）")
    }

    @Test
    fun `small marks keep the eye and the glyph visible`() {
        listOf(16, 24, 32).forEach { size ->
            val image = AppMark.render(size)
            assertTrue(cyanPixels(image) > 10, "${size}px 的眼眶應存在（${cyanPixels(image)}）")
            assertTrue(creamPixels(image) > 0, "${size}px 應保留字的核心像素（${creamPixels(image)}）")
        }
    }

    @Test
    fun `mark renders at every packaged icon size`() {
        listOf(16, 24, 32, 48, 64, 128, 256, 512).forEach { size ->
            val image = AppMark.render(size)
            assertEquals(size, image.width)
            assertEquals(size, image.height)
        }
    }

    @Test
    fun `small sizes use a thicker eye than large sizes`() {
        // 同一設計座標下，小尺寸的眼眶筆畫較粗；以青色像素佔比比較。
        val small = cyanRatio(AppMark.render(32))
        val large = cyanRatio(AppMark.render(128))
        assertTrue(small > large, "32px 的眼眶佔比 ${"%.4f".format(small)} 應高於 128px 的 ${"%.4f".format(large)}")
    }

    private fun cyanPixels(image: BufferedImage): Int = count(image) { r, g, b ->
        b > 190 && g > 150 && r < 130
    }

    private fun creamPixels(image: BufferedImage): Int = count(image) { r, g, b ->
        r > 215 && g > 190 && b > 140 && b < 235
    }

    private fun cyanRatio(image: BufferedImage): Double =
        cyanPixels(image).toDouble() / (image.width * image.height)

    private fun count(image: BufferedImage, predicate: (Int, Int, Int) -> Boolean): Int {
        var found = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val argb = image.getRGB(x, y)
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                if (predicate(r, g, b)) found++
            }
        }
        return found
    }
}
