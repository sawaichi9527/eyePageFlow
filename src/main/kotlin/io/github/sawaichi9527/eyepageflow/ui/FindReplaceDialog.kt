package io.github.sawaichi9527.eyepageflow.ui

import java.awt.BorderLayout
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class FindReplaceDialog(
    owner: Frame,
    private val editor: EditorPane,
    replaceMode: Boolean,
) : JDialog(owner, if (replaceMode) "取代" else "尋找", false) {
    private val findField = JTextField(24)
    private val replaceField = JTextField(24)
    private val matchCase = JCheckBox("大小寫")
    private val wholeWord = JCheckBox("全字")
    private val regex = JCheckBox("正規表達式")
    private val backward = JCheckBox("向上")
    private val status = JLabel(" ")

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()

        val form = JPanel(GridBagLayout())
        form.border = BorderFactory.createEmptyBorder(12, 12, 4, 12)

        fun row(rowIndex: Int, label: String, field: JTextField) {
            form.add(
                JLabel(label),
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = rowIndex
                    anchor = GridBagConstraints.WEST
                    insets = Insets(3, 0, 3, 8)
                },
            )
            form.add(
                field,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = rowIndex
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = Insets(3, 0, 3, 0)
                },
            )
        }

        row(0, "尋找", findField)
        row(1, "取代為", replaceField)

        val options = JPanel()
        options.add(matchCase)
        options.add(wholeWord)
        options.add(regex)
        options.add(backward)
        form.add(
            options,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 2
                gridwidth = 2
                anchor = GridBagConstraints.WEST
                insets = Insets(6, 0, 0, 0)
            },
        )

        val buttons = JPanel()
        val findButton = JButton("找下一個")
        val replaceButton = JButton("取代")
        val replaceAllButton = JButton("全部取代")
        val closeButton = JButton("關閉")
        buttons.add(findButton)
        buttons.add(replaceButton)
        buttons.add(replaceAllButton)
        buttons.add(closeButton)

        findButton.addActionListener { findNextMatch() }
        replaceButton.addActionListener { replaceOne() }
        replaceAllButton.addActionListener { replaceEverywhere() }
        closeButton.addActionListener { dispose() }
        findField.addActionListener { findNextMatch() }
        replaceField.addActionListener { replaceOne() }
        rootPane.defaultButton = findButton

        val south = JPanel(BorderLayout())
        south.add(buttons, BorderLayout.NORTH)
        south.add(status, BorderLayout.SOUTH)

        add(form, BorderLayout.CENTER)
        add(south, BorderLayout.SOUTH)
        pack()
        setLocationRelativeTo(owner)
        addWindowListener(
            object : WindowAdapter() {
                override fun windowOpened(event: WindowEvent) {
                    findField.requestFocusInWindow()
                }
            },
        )
    }

    fun findNextMatch() {
        val found = editor.find(
            searchFor = findField.text,
            matchCase = matchCase.isSelected,
            wholeWord = wholeWord.isSelected,
            regex = regex.isSelected,
            forward = !backward.isSelected,
        )
        status.text = if (found) "找到" else "找不到"
    }

    private fun replaceOne() {
        val replaced = editor.replace(
            searchFor = findField.text,
            replacement = replaceField.text,
            matchCase = matchCase.isSelected,
            wholeWord = wholeWord.isSelected,
            regex = regex.isSelected,
            forward = !backward.isSelected,
        )
        status.text = if (replaced) "已取代" else "找不到"
    }

    private fun replaceEverywhere() {
        val count = editor.replaceAll(
            searchFor = findField.text,
            replacement = replaceField.text,
            matchCase = matchCase.isSelected,
            wholeWord = wholeWord.isSelected,
            regex = regex.isSelected,
        )
        status.text = if (count > 0) "已取代 $count 處" else "找不到"
    }
}
