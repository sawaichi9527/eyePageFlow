package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class DocumentIOTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `big5 round trip preserves bytes`() {
        val text = "測試文字，書頁。\r\n第二行\r\n"
        val original = text.toByteArray(TextEncoding.BIG5.charset)
        val source = tempDir.resolve("sample-big5.txt")
        Files.write(source, original)

        val document = DocumentIO.read(source)
        assertEquals(TextEncoding.BIG5, document.encoding)
        assertEquals(text, document.text)
        assertEquals(LineEnding.CRLF, document.lineEnding)

        val target = tempDir.resolve("sample-big5-out.txt")
        val result = DocumentIO.save(document, target)
        assertTrue(result is SaveResult.Success)
        assertArrayEquals(original, Files.readAllBytes(target))
        assertFalse((result as SaveResult.Success).document.modified)
    }

    @Test
    fun `utf8 with bom round trip preserves bytes`() {
        val text = "測試文字\n第二行\n"
        val original = TextEncoding.UTF8.bom!! + text.toByteArray(Charsets.UTF_8)
        val source = tempDir.resolve("sample-utf8-bom.txt")
        Files.write(source, original)

        val document = DocumentIO.read(source)
        assertEquals(TextEncoding.UTF8, document.encoding)
        assertTrue(document.hadBom)
        assertEquals(text, document.text)

        val target = tempDir.resolve("sample-utf8-bom-out.txt")
        assertTrue(DocumentIO.save(document, target) is SaveResult.Success)
        assertArrayEquals(original, Files.readAllBytes(target))
    }

    @Test
    fun `utf16le with bom round trip preserves bytes`() {
        val text = "測試文字\r\n第二行\r\n"
        val original = TextEncoding.UTF16LE.bom!! + text.toByteArray(Charsets.UTF_16LE)
        val source = tempDir.resolve("sample-utf16le.txt")
        Files.write(source, original)

        val document = DocumentIO.read(source)
        assertEquals(TextEncoding.UTF16LE, document.encoding)
        assertTrue(document.hadBom)

        val target = tempDir.resolve("sample-utf16le-out.txt")
        assertTrue(DocumentIO.save(document, target) is SaveResult.Success)
        assertArrayEquals(original, Files.readAllBytes(target))
    }

    @Test
    fun `unmappable big5 save asks for an override`() {
        val document = TextDocument(
            path = null,
            text = "測試😀",
            encoding = TextEncoding.BIG5,
            hadBom = false,
            lineEnding = LineEnding.LF,
        )
        val target = tempDir.resolve("unmappable.txt")
        val result = DocumentIO.save(document, target)
        assertTrue(result is SaveResult.Unmappable)
        assertTrue((result as SaveResult.Unmappable).characters.isNotEmpty())
        assertFalse(Files.exists(target))

        val fallback = DocumentIO.save(
            document = document,
            targetPath = target,
            encodingOverride = TextEncoding.UTF8,
        )
        assertTrue(fallback is SaveResult.Success)
        val saved = fallback as SaveResult.Success
        assertEquals(TextEncoding.UTF8, saved.document.encoding)
        assertFalse(saved.document.hadBom)
        assertEquals("測試😀", Files.readString(target, Charsets.UTF_8))
    }

    @Test
    fun `invalid bytes are rejected instead of silently replaced`() {
        val source = tempDir.resolve("invalid.txt")
        Files.write(source, byteArrayOf(0x81.toByte(), 0x20))
        assertThrows<DocumentDecodeException> {
            DocumentIO.read(source)
        }
    }
}
