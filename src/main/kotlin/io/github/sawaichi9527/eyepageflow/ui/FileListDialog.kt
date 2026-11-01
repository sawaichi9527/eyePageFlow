package io.github.sawaichi9527.eyepageflow.ui

import io.github.sawaichi9527.eyepageflow.core.LibraryFiles
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel

class FileListDialog(
    owner: Frame,
    initialDirectory: Path,
    private val onOpen: (Path) -> Unit,
) : JDialog(owner, "檔案列表", false) {
    private var directory: Path = initialDirectory
    private val listModel = DefaultListModel<String>()
    private val list = JList(listModel)
    private val header = JLabel()
    private var files: List<Path> = emptyList()

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        header.border = BorderFactory.createEmptyBorder(10, 12, 6, 12)

        val center = JPanel(BorderLayout())
        center.add(header, BorderLayout.NORTH)
        center.add(JScrollPane(list), BorderLayout.CENTER)

        val buttons = JPanel()
        val open = JButton("開啟")
        val up = JButton("上一層")
        val close = JButton("關閉")
        buttons.add(open)
        buttons.add(up)
        buttons.add(close)
        open.addActionListener { openSelected() }
        up.addActionListener { goUp() }
        close.addActionListener { dispose() }
        list.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (event.clickCount == 2) {
                        openSelected()
                    }
                }
            },
        )

        add(center, BorderLayout.CENTER)
        add(buttons, BorderLayout.SOUTH)
        preferredSize = Dimension(620, 520)
        pack()
        setLocationRelativeTo(owner)
        rebuild()
        addWindowListener(
            object : WindowAdapter() {
                override fun windowOpened(event: WindowEvent) {
                    list.requestFocusInWindow()
                }
            },
        )
    }

    private fun rebuild() {
        files = LibraryFiles.listTextFiles(directory)
        listModel.clear()
        files.forEach { listModel.addElement(it.fileName.toString()) }
        if (files.isNotEmpty()) {
            list.selectedIndex = 0
        }
        header.text = if (files.isEmpty()) {
            "${directory.toAbsolutePath()}（沒有支援的純文字檔）"
        } else {
            "${directory.toAbsolutePath()}（${files.size} 個檔案）"
        }
    }

    private fun openSelected() {
        val index = list.selectedIndex
        if (index !in files.indices) return
        onOpen(files[index])
        dispose()
    }

    private fun goUp() {
        val parent = directory.parent ?: return
        if (Files.isDirectory(parent)) {
            directory = parent
            rebuild()
        }
    }
}
