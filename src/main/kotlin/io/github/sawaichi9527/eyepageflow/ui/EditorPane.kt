package io.github.sawaichi9527.eyepageflow.ui

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import io.github.sawaichi9527.eyepageflow.render.Theme
import io.github.sawaichi9527.eyepageflow.render.Typeface
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.undo.UndoManager
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import org.fife.ui.rtextarea.SearchContext
import org.fife.ui.rtextarea.SearchEngine

class EditorPane(
    private val onDirty: () -> Unit,
) : JPanel(BorderLayout()) {
    private val textArea = RSyntaxTextArea(1, 1)
    private val scrollPane = RTextScrollPane(textArea)
    private val pagePanel = JPanel(BorderLayout())
    private var updating = false

    init {
        border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
        pagePanel.add(scrollPane, BorderLayout.CENTER)
        add(pagePanel, BorderLayout.CENTER)
        configureUndoLimit()
        textArea.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE
        textArea.isCodeFoldingEnabled = false
        textArea.antiAliasingEnabled = true
        textArea.lineWrap = true
        textArea.wrapStyleWord = false
        textArea.highlightCurrentLine = false
        textArea.tabSize = 4
        scrollPane.lineNumbersEnabled = false
        scrollPane.border = javax.swing.BorderFactory.createEmptyBorder()

        textArea.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = dirty()
                override fun removeUpdate(event: DocumentEvent) = dirty()
                override fun changedUpdate(event: DocumentEvent) = dirty()
            },
        )

    }

    private fun configureUndoLimit() {
        try {
            val field = RTextArea::class.java.getDeclaredField("undoManager")
            field.isAccessible = true
            (field.get(textArea) as? UndoManager)?.limit = 100
        } catch (_: Exception) {
            // RTextArea's default UndoManager already uses a 100-edit limit.
        }
    }

    fun canUndo(): Boolean = textArea.canUndo()

    fun canRedo(): Boolean = textArea.canRedo()

    fun text(): String = textArea.text

    fun caretOffset(): Int = textArea.caretPosition

    fun setCaretOffset(offset: Int) {
        textArea.caretPosition = offset.coerceIn(0, textArea.document.length)
        textArea.requestFocusInWindow()
    }

    fun insertAtCaret(value: String) {
        textArea.replaceSelection(value)
    }

    fun setDocumentText(value: String, caret: Int) {
        updating = true
        try {
            textArea.text = value
            textArea.caretPosition = caret.coerceIn(0, textArea.document.length)
            textArea.discardAllEdits()
        } finally {
            updating = false
        }
    }

    fun focusEditor() {
        textArea.requestFocusInWindow()
    }

    fun undo() {
        if (textArea.canUndo()) {
            textArea.undoLastAction()
        }
    }

    fun redo() {
        if (textArea.canRedo()) {
            textArea.redoLastAction()
        }
    }

    fun cut() {
        textArea.cut()
    }

    fun copy() {
        textArea.copy()
    }

    fun paste() {
        textArea.paste()
    }

    fun selectAll() {
        textArea.selectAll()
    }

    fun find(
        searchFor: String,
        matchCase: Boolean = false,
        wholeWord: Boolean = false,
        regex: Boolean = false,
        forward: Boolean = true,
    ): Boolean {
        if (searchFor.isEmpty()) return false
        return try {
            SearchEngine.find(textArea, searchContext(searchFor, matchCase, wholeWord, regex, forward))
                .wasFound()
        } catch (_: Exception) {
            false
        }
    }

    fun replace(
        searchFor: String,
        replacement: String,
        matchCase: Boolean = false,
        wholeWord: Boolean = false,
        regex: Boolean = false,
        forward: Boolean = true,
    ): Boolean {
        if (searchFor.isEmpty()) return false
        return try {
            val context = searchContext(searchFor, matchCase, wholeWord, regex, forward)
            context.replaceWith = replacement
            SearchEngine.replace(textArea, context).wasFound()
        } catch (_: Exception) {
            false
        }
    }

    fun replaceAll(
        searchFor: String,
        replacement: String,
        matchCase: Boolean = false,
        wholeWord: Boolean = false,
        regex: Boolean = false,
    ): Int {
        if (searchFor.isEmpty()) return 0
        return try {
            val context = searchContext(searchFor, matchCase, wholeWord, regex, true)
            context.replaceWith = replacement
            SearchEngine.replaceAll(textArea, context).count
        } catch (_: Exception) {
            0
        }
    }

    private fun searchContext(
        searchFor: String,
        matchCase: Boolean,
        wholeWord: Boolean,
        regex: Boolean,
        forward: Boolean,
    ): SearchContext = SearchContext(searchFor).apply {
        setMatchCase(matchCase)
        setWholeWord(wholeWord)
        setRegularExpression(regex)
        setSearchForward(forward)
        setSearchWrap(true)
    }

    fun bindToggleEditor(action: () -> Unit) {
        bindKey("control E", "toggle-editor-mode", action)
    }

    fun bindFind(action: () -> Unit) {
        bindKey("control F", "find-text", action)
    }

    fun bindReplace(action: () -> Unit) {
        bindKey("control R", "replace-text", action)
    }

    fun bindEscape(action: () -> Unit) {
        bindKey("ESCAPE", "boss-key-editor", action)
    }

    /**
     * 空白鍵＝下一頁（與原作快捷鍵相容）；Shift+空白不攔截，仍插入空白。
     *
     * 綁在 KEY_PRESSED 的 VK_SPACE 上，所以 Shift 版本是不同的 KeyStroke，
     * 會落到元件的預設輸入處理，正常插入空白。
     */
    fun bindNextPage(action: () -> Unit) {
        textArea.inputMap.put(
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
            "next-page-space",
        )
        textArea.actionMap.put(
            "next-page-space",
            object : AbstractAction() {
                override fun actionPerformed(event: ActionEvent) {
                    action()
                }
            },
        )
    }

    private fun bindKey(key: String, actionKey: String, action: () -> Unit) {
        textArea.inputMap.put(KeyStroke.getKeyStroke(key), actionKey)
        textArea.actionMap.put(
            actionKey,
            object : AbstractAction() {
                override fun actionPerformed(event: ActionEvent) {
                    action()
                }
            },
        )
    }

    fun applyTheme(theme: Theme, metrics: PageMetrics, fontFamily: String) {
        val family = Typeface.resolveFamily(fontFamily)
        textArea.font = Font(family, Font.PLAIN, metrics.fontSize.toInt())
        textArea.background = theme.pageColor
        textArea.foreground = theme.textColor
        textArea.caretColor = theme.textColor
        textArea.selectionColor = Color(
            theme.textColor.red,
            theme.textColor.green,
            theme.textColor.blue,
            70,
        )
        textArea.selectedTextColor = theme.textColor
        textArea.margin = Insets(
            (metrics.marginTop * 0.6f).toInt(),
            metrics.marginLeft,
            (metrics.marginBottom * 0.6f).toInt(),
            metrics.marginRight,
        )
        scrollPane.background = theme.pageColor
        scrollPane.viewport.background = theme.pageColor
        pagePanel.background = theme.pageColor
        pagePanel.border = CompoundBorder(
            LineBorder(theme.coverColor, 10, true),
            EmptyBorder(8, 8, 8, 8),
        )
        background = theme.desktopColor
        pagePanel.repaint()
        scrollPane.repaint()
    }

    private fun dirty() {
        if (!updating) {
            onDirty()
        }
    }
}
