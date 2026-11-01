package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextDocumentTest {
    @Test
    fun `text edits set the dirty flag and saving clears it`() {
        val document = TextDocument(
            path = Path.of("novel.txt"),
            text = "第一行\n",
            encoding = TextEncoding.UTF8,
            hadBom = false,
            lineEnding = LineEnding.LF,
        )
        assertFalse(document.modified)
        val edited = document.withText("第一行\n第二行\n")
        assertTrue(edited.modified)
        val saved = edited.markSaved(Path.of("novel.txt"), TextEncoding.UTF8, false)
        assertFalse(saved.modified)
        assertTrue(saved.text.contains("第二行"))
    }
}
