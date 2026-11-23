package io.github.sawaichi9527.eyepageflow.ui

import io.github.sawaichi9527.eyepageflow.core.Messages
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JScrollPane

/** F1 使用說明：自己的文字，內容由 `i18n/messages*.properties` 的 `help.body` 提供。 */
class HelpDialog(owner: Frame) : JDialog(owner, Messages["help.title"], false) {
    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()

        val content = JEditorPane().apply {
            contentType = "text/html"
            text = Messages["help.body"]
            isEditable = false
            background = java.awt.Color.WHITE
        }
        val scroll = JScrollPane(content).apply {
            border = BorderFactory.createEmptyBorder()
        }
        add(scroll, BorderLayout.CENTER)

        val buttons = JPanel()
        buttons.add(
            JButton("關閉").apply {
                addActionListener { dispose() }
            },
        )
        add(buttons, BorderLayout.SOUTH)

        preferredSize = Dimension(560, 520)
        pack()
        setLocationRelativeTo(owner)
    }
}
