package io.github.sawaichi9527.eyepageflow.render

import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TexturePainterTest {
    private val page = Rectangle(0, 0, 120, 90)
    private val paper = Color(0xF3, 0xE9, 0xD2)

    private fun render(rect: Rectangle, color: Color, kind: TextureKind): BufferedImage {
        val image = BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()
        TexturePainter.paint(g2, rect, color, kind)
        g2.dispose()
        return image
    }

    @Test
    fun `same parameters produce identical textures`() {
        TexturePainter.clearCache()
        val first = render(page, paper, TextureKind.PAPER)
        val second = render(page, paper, TextureKind.PAPER)
        assertEquals(first.getRGB(3, 7), second.getRGB(3, 7))
        assertEquals(first.getRGB(60, 40), second.getRGB(60, 40))
        assertEquals(first.getRGB(119, 89), second.getRGB(119, 89))
    }

    @Test
    fun `cache is keyed by colour and size`() {
        TexturePainter.clearCache()
        val paperTexture = render(page, paper, TextureKind.PAPER)
        val darkTexture = render(page, Color(0x22, 0x22, 0x22), TextureKind.PAPER)
        assertNotEquals(
            paperTexture.getRGB(10, 10),
            darkTexture.getRGB(10, 10),
            "不同底色不能共用同一份快取",
        )

        val other = render(Rectangle(0, 0, 60, 45), paper, TextureKind.PAPER)
        assertEquals(60, other.width)
        assertEquals(45, other.height)
    }

    @Test
    fun `none texture is a flat fill`() {
        val texture = render(page, Color(0x12, 0x34, 0x56), TextureKind.NONE)
        assertEquals(Color(0x12, 0x34, 0x56).rgb, texture.getRGB(0, 0))
        assertEquals(Color(0x12, 0x34, 0x56).rgb, texture.getRGB(119, 89))
    }

    @Test
    fun `texture paints at the requested position`() {
        val image = BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()
        TexturePainter.paint(g2, Rectangle(50, 60, 40, 30), Color(0x11, 0x22, 0x33), TextureKind.NONE)
        g2.dispose()
        assertEquals(Color(0x11, 0x22, 0x33).rgb, image.getRGB(50, 60))
        assertEquals(0, image.getRGB(0, 0), "字格外的區域不應該被填色")
    }
}
