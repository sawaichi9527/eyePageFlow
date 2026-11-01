package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import io.github.sawaichi9527.eyepageflow.core.PaginationEngine
import io.github.sawaichi9527.eyepageflow.core.WritingMode
import io.github.sawaichi9527.eyepageflow.render.BookRenderer
import io.github.sawaichi9527.eyepageflow.render.Theme
import io.github.sawaichi9527.eyepageflow.render.Typeface
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

object PrototypeRender {
    fun run(outputDir: Path) {
        System.setProperty("java.awt.headless", "true")
        Files.createDirectories(outputDir)

        val text = SampleText.longText(repeat = 10)
        val variants = listOf(
            "prototype" to PageMetrics(),
            "prototype-underline" to PageMetrics(underline = true),
        )

        variants.forEach { (prefix, metrics) ->
            val font = Typeface.pageFont(metrics)
            WritingMode.entries.forEach { mode ->
                val started = System.nanoTime()
                val engine = PaginationEngine(text, metrics, mode, font)
                val pagination = engine.paginate()
                val image = BufferedImage(1920, 1000, BufferedImage.TYPE_INT_ARGB)
                val graphics = image.createGraphics()
                BookRenderer.render(
                    g2 = graphics,
                    targetWidth = image.width,
                    targetHeight = image.height,
                    engine = engine,
                    pagination = pagination,
                    spreadStart = 0,
                    theme = Theme.DEFAULT,
                    doublePage = true,
                    showBookAndDesktop = true,
                    font = font,
                )
                graphics.dispose()
                val path = outputDir.resolve("$prefix-${mode.name.lowercase()}.png")
                ImageIO.write(image, "png", path.toFile())
                val elapsedMs = (System.nanoTime() - started) / 1_000_000.0
                println(
                    "rendered $path (pages=${pagination.pageCount}, " +
                        "chars=${text.length}, elapsed=%.1f ms)".format(elapsedMs),
                )
            }
        }
    }
}
