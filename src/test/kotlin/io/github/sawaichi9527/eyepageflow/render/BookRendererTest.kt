package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.SampleText
import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import io.github.sawaichi9527.eyepageflow.core.PaginationEngine
import io.github.sawaichi9527.eyepageflow.core.WritingMode
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BookRendererTest {
    @Test
    fun `renders vertical and horizontal spreads`() {
        val outputDir = Path.of("build", "test-output")
        Files.createDirectories(outputDir)
        val metrics = PageMetrics(
            pageWidth = 400,
            pageHeight = 360,
            marginTop = 24,
            marginBottom = 32,
            marginLeft = 26,
            marginRight = 26,
            fontSize = 16f,
            lineHeightFactor = 1.45f,
        )
        val font = Typeface.pageFont(metrics)
        val text = SampleText.longText(repeat = 2)

        WritingMode.entries.forEach { mode ->
            val engine = PaginationEngine(text, metrics, mode, font)
            val pagination = engine.paginate()
            val image = BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            BookRenderer.render(
                g2 = graphics,
                targetWidth = image.width,
                targetHeight = image.height,
                engine = engine,
                pagination = pagination,
                spreadStart = 0,
                theme = Theme.REALISTIC,
                doublePage = true,
                showBookAndDesktop = true,
                font = font,
            )
            graphics.dispose()

            val path = outputDir.resolve("render-${mode.name.lowercase()}.png")
            ImageIO.write(image, "png", path.toFile())
            assertTrue(Files.size(path) > 0)
            assertTrue(distinctColors(image) > 10, "render should not be a flat color")
        }
    }

    private fun distinctColors(image: BufferedImage): Int {
        val colors = HashSet<Int>()
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                colors.add(image.getRGB(x, y))
                x += 7
            }
            y += 7
        }
        return colors.size
    }
}
