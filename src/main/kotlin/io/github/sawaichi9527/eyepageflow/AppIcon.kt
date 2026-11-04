package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.render.EmptyStateRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * 產生應用程式圖示（透明背景），供打包腳本與桌面捷徑使用。
 *
 * PNG 給 Linux 的 `.desktop`／AppImage；ICO 給 Windows 的 `jpackage --icon`
 * （Windows 版 jpackage 只接受 `.ico`）。兩者都是程式自繪，不含外部素材。
 */
object AppIcon {
    private const val DEFAULT_SIZE = 512

    /** ICO 內含的尺寸；Windows 用 16–64 給檔案總管／工作列，256 給高 DPI。 */
    private val ICO_SIZES = intArrayOf(16, 24, 32, 48, 64, 128, 256)

    /** 依副檔名輸出：`.ico` 走 ICO，其餘走 PNG。 */
    fun writeAs(path: Path) {
        if (path.fileName.toString().endsWith(".ico", ignoreCase = true)) {
            writeIco(path)
        } else {
            write(path)
        }
    }

    fun write(path: Path) {
        System.setProperty("java.awt.headless", "true")
        path.parent?.let { Files.createDirectories(it) }
        ImageIO.write(render(DEFAULT_SIZE), "png", path.toFile())
        println("icon written: $path (${DEFAULT_SIZE}x$DEFAULT_SIZE)")
    }

    fun writeIco(path: Path) {
        System.setProperty("java.awt.headless", "true")
        path.parent?.let { Files.createDirectories(it) }
        Files.write(path, icoBytes())
        println("icon written: $path (${ICO_SIZES.joinToString(",")})")
    }

    fun render(size: Int): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()
        EmptyStateRenderer.renderIcon(g2, size)
        g2.dispose()
        return image
    }

    /**
     * 產生 ICO 位元組。
     *
     * 每個尺寸都以 PNG 內嵌（Vista 以後支援），優點是不必自己做
     * BGRA 點陣與 AND mask 的 DIB 編碼，檔案也小得多。
     */
    fun icoBytes(sizes: IntArray = ICO_SIZES): ByteArray {
        val images = sizes.map { pngBytes(render(it)) }
        val headerSize = 6 + 16 * sizes.size
        val header = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(0) // reserved
        header.putShort(1) // 1 = icon
        header.putShort(sizes.size.toShort())

        var offset = headerSize
        sizes.forEachIndexed { index, size ->
            // ICO 以 0 表示 256（欄位只有一個 byte）
            val dimension = if (size >= 256) 0 else size
            header.put(dimension.toByte())
            header.put(dimension.toByte())
            header.put(0) // 調色盤筆數（true color 為 0）
            header.put(0) // reserved
            header.putShort(1) // color planes
            header.putShort(32) // bits per pixel
            header.putInt(images[index].size)
            header.putInt(offset)
            offset += images[index].size
        }

        val out = ByteArrayOutputStream(offset)
        out.write(header.array())
        images.forEach { out.write(it) }
        return out.toByteArray()
    }

    private fun pngBytes(image: BufferedImage): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }
}
