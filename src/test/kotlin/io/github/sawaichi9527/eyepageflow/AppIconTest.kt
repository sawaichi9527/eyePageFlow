package io.github.sawaichi9527.eyepageflow

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppIconTest {
    private val pngSignature =
        byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())

    @Test
    fun `ico header lists every size`() {
        val sizes = intArrayOf(16, 32, 256)
        val bytes = AppIcon.icoBytes(sizes)
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(0, header.getShort().toInt()) // reserved
        assertEquals(1, header.getShort().toInt()) // 1 = icon
        assertEquals(sizes.size, header.getShort().toInt())
        assertTrue(bytes.size > 6 + 16 * sizes.size) { "ICO has no payload" }
    }

    @Test
    fun `ico entries point at png payloads`() {
        val sizes = intArrayOf(16, 32, 256)
        val bytes = AppIcon.icoBytes(sizes)

        sizes.forEachIndexed { index, size ->
            val entry = ByteBuffer.wrap(bytes, 6 + 16 * index, 16).order(ByteOrder.LITTLE_ENDIAN)
            val expectedDimension = if (size >= 256) 0 else size
            assertEquals(expectedDimension, entry.get().toInt() and 0xFF)
            assertEquals(expectedDimension, entry.get().toInt() and 0xFF)
            assertEquals(0, entry.get().toInt()) // palette size
            assertEquals(0, entry.get().toInt()) // reserved
            assertEquals(1, entry.getShort().toInt()) // color planes
            assertEquals(32, entry.getShort().toInt()) // bits per pixel
            val length = entry.getInt()
            val offset = entry.getInt()

            assertTrue(length > 0) { "entry $index has no payload" }
            assertTrue(offset + length <= bytes.size) { "entry $index overruns the file" }
            assertArrayEquals(
                pngSignature,
                bytes.copyOfRange(offset, offset + pngSignature.size),
            ) { "entry $index is not a PNG payload" }
        }
    }

    @Test
    fun `ico payloads start after the header and are contiguous`() {
        val sizes = intArrayOf(16, 32)
        val bytes = AppIcon.icoBytes(sizes)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var expectedOffset = 6 + 16 * sizes.size

        sizes.indices.forEach { index ->
            buffer.position(6 + 16 * index + 8)
            val length = buffer.getInt()
            val offset = buffer.getInt()
            assertEquals(expectedOffset, offset)
            expectedOffset += length
        }
        assertEquals(bytes.size, expectedOffset)
    }

    @Test
    fun `writeAs picks ico by extension and png otherwise`() {
        val dir = Path.of("build", "test-output", "icon")
        Files.createDirectories(dir)

        val icoPath = dir.resolve("eyePageFlow.ico")
        val pngPath = dir.resolve("eyePageFlow.png")
        AppIcon.writeAs(icoPath)
        AppIcon.writeAs(pngPath)

        val ico = Files.readAllBytes(icoPath)
        assertArrayEquals(pngSignature, ico.copyOfRange(6 + 16 * 7, 6 + 16 * 7 + 4))
        assertArrayEquals(pngSignature, Files.readAllBytes(pngPath).copyOfRange(0, 4))
    }
}
