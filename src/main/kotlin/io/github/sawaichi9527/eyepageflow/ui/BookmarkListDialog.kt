package io.github.sawaichi9527.eyepageflow.ui

import io.github.sawaichi9527.eyepageflow.core.Bookmark
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

class BookmarkListDialog(
    owner: Frame,
    bookmarks: List<Bookmark>,
    private val onJump: (Bookmark) -> Unit,
    private val onDelete: (Bookmark) -> Unit,
) : JDialog(owner, "書籤管理", false) {
    private val entries = bookmarks.toMutableList()
    private val listModel = DefaultListModel<String>()
    private val list = JList(listModel)
    private val header = JLabel()

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        entries.forEach { bookmark ->
            listModel.addElement("${bookmark.title} — ${bookmark.path.fileName}")
        }
        if (entries.isNotEmpty()) {
            list.selectedIndex = 0
        }
        updateHeader()
        header.border = BorderFactory.createEmptyBorder(10, 12, 6, 12)

        val center = JPanel(BorderLayout())
        center.add(header, BorderLayout.NORTH)
        center.add(JScrollPane(list), BorderLayout.CENTER)

        val buttons = JPanel()
        val jump = JButton("跳至")
        val delete = JButton("刪除")
        val close = JButton("關閉")
        buttons.add(jump)
        buttons.add(delete)
        buttons.add(close)
        jump.addActionListener { jumpToSelected() }
        delete.addActionListener { deleteSelected() }
        close.addActionListener { dispose() }
        list.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (event.clickCount == 2) {
                        jumpToSelected()
                    }
                }
            },
        )
        list.addListSelectionListener {
            val selected = list.selectedIndex >= 0
            jump.isEnabled = selected
            delete.isEnabled = selected
        }
        jump.isEnabled = entries.isNotEmpty()
        delete.isEnabled = entries.isNotEmpty()

        add(center, BorderLayout.CENTER)
        add(buttons, BorderLayout.SOUTH)
        preferredSize = Dimension(560, 520)
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

    private fun updateHeader() {
        header.text = "共 ${entries.size} 個書籤"
    }

    private fun jumpToSelected() {
        val index = list.selectedIndex
        if (index !in entries.indices) return
        onJump(entries[index])
        dispose()
    }

    private fun deleteSelected() {
        val index = list.selectedIndex
        if (index !in entries.indices) return
        val bookmark = entries.removeAt(index)
        listModel.remove(index)
        onDelete(bookmark)
        updateHeader()
        if (entries.isEmpty()) {
            list.clearSelection()
        } else {
            list.selectedIndex = index.coerceAtMost(entries.lastIndex)
        }
    }
}
