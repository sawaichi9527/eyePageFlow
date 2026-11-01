package io.github.sawaichi9527.eyepageflow.render

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmptyStateRendererTest {
    @Test
    fun `renders an empty state for both themes`() {
        val outputDir = Path.of("build", "test-output")
        Files.createDirectories(outputDir)

        listOf(Theme.REALISTIC, Theme.MODERN).forEach { theme ->
            val image = BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            EmptyStateRenderer.render(graphics, image.width, image.height, theme)
            graphics.dispose()

            val path = outputDir.resolve("empty-${theme.id}.png")
            ImageIO.write(image, "png", path.toFile())
            assertTrue(Files.size(path) > 0)
            assertTrue(distinctColors(image) > 10)
        }
    }

    private fun distinctColors(image: BufferedImage): Int {
        val colors = HashSet<Int>()
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                colors.add(image.getRGB(x, y))
                x += 9
            }
            y += 9
        }
        return colors.size
    }
}
