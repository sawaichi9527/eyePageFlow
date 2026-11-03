package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.render.EmptyStateRenderer
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/** 產生應用程式圖示 PNG（透明背景），供打包腳本與桌面捷徑使用。 */
object AppIcon {
    private const val DEFAULT_SIZE = 512

    fun write(path: Path) {
        System.setProperty("java.awt.headless", "true")
        path.parent?.let { Files.createDirectories(it) }
        val image = BufferedImage(DEFAULT_SIZE, DEFAULT_SIZE, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()
        EmptyStateRenderer.renderIcon(g2, DEFAULT_SIZE)
        g2.dispose()
        ImageIO.write(image, "png", path.toFile())
        println("icon written: $path (${DEFAULT_SIZE}x$DEFAULT_SIZE)")
    }
}
