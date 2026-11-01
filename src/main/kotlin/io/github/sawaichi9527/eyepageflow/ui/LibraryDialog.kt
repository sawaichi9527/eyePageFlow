package io.github.sawaichi9527.eyepageflow.ui

import io.github.sawaichi9527.eyepageflow.core.Library
import io.github.sawaichi9527.eyepageflow.core.LibraryShelf
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel

class LibraryDialog(
    owner: Frame,
    library: Library,
    private val onApply: (Library) -> Unit,
) : JDialog(owner, "書庫管理", false) {
    private val shelves = library.shelves.toMutableList()
    private var defaultShelfPath: Path? = library.defaultShelfPath
    private val listModel = DefaultListModel<String>()
    private val list = JList(listModel)
    private val header = JLabel()

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        header.border = BorderFactory.createEmptyBorder(10, 12, 6, 12)

        val center = JPanel(BorderLayout())
        center.add(header, BorderLayout.NORTH)
        center.add(JScrollPane(list), BorderLayout.CENTER)

        val buttons = JPanel()
        val add = JButton("新增資料夾")
        val makeDefault = JButton("設為預設")
        val remove = JButton("移除")
        val cancel = JButton("取消")
        val apply = JButton("確定")
        buttons.add(add)
        buttons.add(makeDefault)
        buttons.add(remove)
        buttons.add(cancel)
        buttons.add(apply)
        add.addActionListener { addShelf() }
        makeDefault.addActionListener { makeSelectedDefault() }
        remove.addActionListener { removeSelected() }
        cancel.addActionListener { dispose() }
        apply.addActionListener {
            onApply(Library(shelves.toList(), defaultShelfPath))
            dispose()
        }

        add(center, BorderLayout.CENTER)
        add(buttons, BorderLayout.SOUTH)
        preferredSize = Dimension(680, 480)
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
        listModel.clear()
        shelves.forEach { shelf ->
            val marker = if (shelf.path.toAbsolutePath() == defaultShelfPath?.toAbsolutePath()) "★ " else "  "
            listModel.addElement("$marker${shelf.name} — ${shelf.path}")
        }
        if (shelves.isNotEmpty()) {
            list.selectedIndex = list.selectedIndex.coerceIn(0, shelves.lastIndex)
        }
        header.text = "共 ${shelves.size} 個書架"
    }

    private fun addShelf() {
        val chooser = JFileChooser()
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.dialogTitle = "新增書架資料夾"
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        val path = chooser.selectedFile?.toPath() ?: return
        if (!Files.isDirectory(path)) return
        val normalized = path.toAbsolutePath().normalize()
        if (shelves.any { it.path.toAbsolutePath().normalize() == normalized }) return
        val name = normalized.fileName?.toString() ?: "書架"
        shelves += LibraryShelf(name, normalized)
        rebuild()
        list.selectedIndex = shelves.lastIndex
    }

    private fun makeSelectedDefault() {
        val index = list.selectedIndex
        if (index !in shelves.indices) return
        defaultShelfPath = shelves[index].path
        rebuild()
    }

    private fun removeSelected() {
        val index = list.selectedIndex
        if (index !in shelves.indices) return
        val removed = shelves.removeAt(index)
        if (removed.path.toAbsolutePath() == defaultShelfPath?.toAbsolutePath()) {
            defaultShelfPath = null
        }
        rebuild()
    }
}
