package io.github.sawaichi9527.eyepageflow.core

import java.nio.charset.Charset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EncodingDetectorTest {
    @Test
    fun `detects utf-8 without bom`() {
        val text = "測試文字，PageByFlow 2026。"
        val decoded = EncodingDetector.decode(text.toByteArray(Charsets.UTF_8))
        assertEquals(TextEncoding.UTF8, decoded.encoding)
        assertFalse(decoded.hadBom)
        assertEquals(text, decoded.text)
        assertEquals(0, decoded.replacementCount)
    }

    @Test
    fun `detects utf-8 with bom and strips it`() {
        val text = "測試文字"
        val bytes = TextEncoding.UTF8.bom!! + text.toByteArray(Charsets.UTF_8)
        val decoded = EncodingDetector.decode(bytes)
        assertEquals(TextEncoding.UTF8, decoded.encoding)
        assertTrue(decoded.hadBom)
        assertEquals(text, decoded.text)
    }

    @Test
    fun `detects utf-16le with bom`() {
        val text = "測試文字"
        val bytes = TextEncoding.UTF16LE.bom!! + text.toByteArray(Charsets.UTF_16LE)
        val decoded = EncodingDetector.decode(bytes)
        assertEquals(TextEncoding.UTF16LE, decoded.encoding)
        assertTrue(decoded.hadBom)
        assertEquals(text, decoded.text)
    }

    @Test
    fun `detects big5 when not valid utf-8`() {
        val text = "測試文字，書頁。"
        val bytes = text.toByteArray(Charset.forName("Big5"))
        val decoded = EncodingDetector.decode(bytes)
        assertEquals(TextEncoding.BIG5, decoded.encoding)
        assertEquals(text, decoded.text)
        assertEquals(0, decoded.replacementCount)
    }

    @Test
    fun `detects crlf line endings`() {
        val text = "第一行\r\n第二行\r\n"
        val decoded = EncodingDetector.decode(text.toByteArray(Charsets.UTF_8))
        assertEquals(LineEnding.CRLF, decoded.lineEnding)
    }
}
