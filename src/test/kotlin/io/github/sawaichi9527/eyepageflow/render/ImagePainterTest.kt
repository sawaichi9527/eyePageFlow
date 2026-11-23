package io.github.sawaichi9527.eyepageflow.render

import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ImagePainterTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `paints a loaded image over the rectangle`() {
        val path = writeImage(width = 4, height = 4)
        val target = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        val graphics = target.createGraphics()
        val painted = ImagePainter.paint(graphics, Rectangle(0, 0, 100, 100), path.toString())
        graphics.dispose()

        assertTrue(painted)
        assertEquals(0xFFFF0000.toInt(), target.getRGB(50, 50))
    }

    @Test
    fun `cover scaling fills the whole rectangle without bars`() {
        val path = writeImage(width = 8, height = 2)
        val target = BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB)
        val graphics = target.createGraphics()
        ImagePainter.paint(graphics, Rectangle(0, 0, 60, 60), path.toString())
        graphics.dispose()

        // 2:1? No: 8x2 的短邊放大到 60，長邊會超出並置中裁切，四個角落都還是圖片。
        listOf(0 to 0, 59 to 0, 0 to 59, 59 to 59).forEach { (x, y) ->
            assertEquals(0xFFFF0000.toInt(), target.getRGB(x, y), "($x,$y) 應被圖片覆蓋")
        }
    }

    @Test
    fun `missing files and blank paths fall back`() {
        val target = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        val graphics = target.createGraphics()
        try {
            assertFalse(ImagePainter.paint(graphics, Rectangle(0, 0, 10, 10), ""))
            assertFalse(ImagePainter.paint(graphics, Rectangle(0, 0, 10, 10), null))
            assertFalse(
                ImagePainter.paint(
                    graphics,
                    Rectangle(0, 0, 10, 10),
                    tempDir.resolve("missing.png").toString(),
                ),
            )
        } finally {
            graphics.dispose()
        }
    }

    private fun writeImage(width: Int, height: Int): Path {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = java.awt.Color.RED
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        val path = tempDir.resolve("sample-${width}x$height.png")
        ImageIO.write(image, "png", path.toFile())
        return path
    }
}
