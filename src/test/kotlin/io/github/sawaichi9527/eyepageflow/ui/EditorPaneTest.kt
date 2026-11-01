package io.github.sawaichi9527.eyepageflow.ui

import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EditorPaneTest {
    @Test
    fun `programmatic set is clean but typing marks dirty`() {
        val dirty = AtomicBoolean(false)
        SwingUtilities.invokeAndWait {
            val pane = EditorPane { dirty.set(true) }
            pane.setDocumentText("測試文字", 0)
            assertFalse(dirty.get())
            assertTrue(pane.text() == "測試文字")
            pane.insertAtCaret("，新增")
            assertTrue(dirty.get())
            assertTrue(pane.text().contains("新增"))
        }
    }

    @Test
    fun `undo history is capped at one hundred edits`() {
        SwingUtilities.invokeAndWait {
            val pane = EditorPane { }
            pane.setDocumentText("", 0)
            repeat(130) {
                pane.insertAtCaret("x")
            }
            var count = 0
            while (pane.canUndo() && count < 200) {
                pane.undo()
                count += 1
            }
            assertTrue(count in 1..100)
        }
    }

    @Test
    fun `find and replace work on the editor document`() {
        SwingUtilities.invokeAndWait {
            val pane = EditorPane { }
            pane.setDocumentText("abc abc", 0)
            assertTrue(pane.find("abc"))
            assertTrue(pane.replace("abc", "x"))
            assertTrue(pane.text().startsWith("x "))
            assertTrue(pane.replaceAll("abc", "y") == 1)
            assertTrue(pane.text().endsWith(" y"))
        }
    }
}
