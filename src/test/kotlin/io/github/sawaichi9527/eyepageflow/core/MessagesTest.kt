package io.github.sawaichi9527.eyepageflow.core

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessagesTest {
    @AfterEach
    fun reset() {
        Messages.init("zh-TW")
    }

    @Test
    fun `english is used when requested`() {
        Messages.init("en")
        assertEquals("en", Messages.currentLanguage)
        assertEquals("File", Messages["menu.file"])
    }

    @Test
    fun `japanese is used when requested`() {
        Messages.init("ja")
        assertEquals("ja", Messages.currentLanguage)
        assertEquals("ファイル(F)", Messages["menu.file"])
    }

    @Test
    fun `unknown languages fall back to traditional chinese`() {
        Messages.init("fr")
        assertEquals("zh-TW", Messages.currentLanguage)
        assertEquals("文件(F)", Messages["menu.file"])
    }

    @Test
    fun `every key exists in every bundle`() {
        val base = load("/i18n/messages.properties")
        listOf("/i18n/messages_en.properties", "/i18n/messages_ja.properties").forEach { bundle ->
            val other = load(bundle)
            val missing = base.stringPropertyNames() - other.stringPropertyNames()
            assertTrue(missing.isEmpty(), "$bundle 缺少：$missing")
        }
    }

    @Test
    fun `format fills the placeholders`() {
        Messages.init("zh-TW")
        val text = Messages.format("about.body", "0.1.0", "21")
        assertTrue(text.contains("0.1.0"))
        assertTrue(text.contains("21"))
    }

    @Test
    fun `unknown keys return the key itself`() {
        Messages.init("zh-TW")
        assertEquals("no.such.key", Messages["no.such.key"])
    }

    private fun load(path: String): Properties {
        val stream = MessagesTest::class.java.getResourceAsStream(path)!!
        return Properties().apply {
            InputStreamReader(stream, StandardCharsets.UTF_8).use { load(it) }
        }
    }
}
