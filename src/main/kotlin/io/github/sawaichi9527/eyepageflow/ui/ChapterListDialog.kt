package io.github.sawaichi9527.eyepageflow.ui

import io.github.sawaichi9527.eyepageflow.core.Chapter
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel

class ChapterListDialog(
    owner: Frame,
    chapters: List<Chapter>,
    currentOffset: Int,
    private val onJump: (Chapter) -> Unit,
) : JDialog(owner, "章節列表", false) {
    private val listModel = DefaultListModel<String>()
    private val list = JList(listModel)

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        chapters.forEach { chapter ->
            listModel.addElement("${chapter.lineNumber}: ${chapter.title}")
        }
        val currentIndex = chapters.indexOfLast { it.offset <= currentOffset }
            .let { if (it < 0) 0 else it }
        if (chapters.isNotEmpty()) {
            list.selectedIndex = currentIndex
            list.ensureIndexIsVisible(currentIndex)
        }

        val currentTitle = chapters.getOrNull(currentIndex)?.title
        val header = JLabel(
            "共 ${chapters.size} 個章節" +
                if (currentTitle != null) " · 目前：$currentTitle" else "",
        )
        header.border = BorderFactory.createEmptyBorder(10, 12, 6, 12)

        val center = JPanel(BorderLayout())
        center.add(header, BorderLayout.NORTH)
        center.add(JScrollPane(list), BorderLayout.CENTER)

        val buttons = JPanel()
        val jump = JButton("跳至")
        val close = JButton("關閉")
        buttons.add(jump)
        buttons.add(close)
        jump.addActionListener { jumpToSelected(chapters) }
        close.addActionListener { dispose() }
        list.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (event.clickCount == 2) {
                        jumpToSelected(chapters)
                    }
                }
            },
        )
        list.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                jump.isEnabled = list.selectedIndex >= 0
            }
        }
        jump.isEnabled = chapters.isNotEmpty()

        add(center, BorderLayout.CENTER)
        add(buttons, BorderLayout.SOUTH)
        preferredSize = Dimension(520, 600)
        pack()
        setLocationRelativeTo(owner)
        addWindowListener(
            object : WindowAdapter() {
                override fun windowOpened(event: WindowEvent) {
                    list.requestFocusInWindow()
                }
            },
        )
    }

    private fun jumpToSelected(chapters: List<Chapter>) {
        val index = list.selectedIndex
        if (index !in chapters.indices) return
        onJump(chapters[index])
        dispose()
    }
}
