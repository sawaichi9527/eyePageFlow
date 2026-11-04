package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.core.AppPaths
import io.github.sawaichi9527.eyepageflow.core.Bookmark
import io.github.sawaichi9527.eyepageflow.core.BookmarkStore
import io.github.sawaichi9527.eyepageflow.core.ChapterParser
import io.github.sawaichi9527.eyepageflow.core.ChapterRule
import io.github.sawaichi9527.eyepageflow.core.ChapterRuleCatalog
import io.github.sawaichi9527.eyepageflow.core.DisplaySettings
import io.github.sawaichi9527.eyepageflow.core.Library
import io.github.sawaichi9527.eyepageflow.core.LibraryStore
import io.github.sawaichi9527.eyepageflow.core.DocumentDecodeException
import io.github.sawaichi9527.eyepageflow.core.DocumentIO
import io.github.sawaichi9527.eyepageflow.core.LayoutMode
import io.github.sawaichi9527.eyepageflow.core.PageTurnAnimation
import io.github.sawaichi9527.eyepageflow.core.Pagination
import io.github.sawaichi9527.eyepageflow.core.PaginationEngine
import io.github.sawaichi9527.eyepageflow.core.RecentFilesStore
import io.github.sawaichi9527.eyepageflow.core.ReadingPositionStore
import io.github.sawaichi9527.eyepageflow.core.SaveResult
import io.github.sawaichi9527.eyepageflow.core.SettingsStore
import io.github.sawaichi9527.eyepageflow.core.TextDocument
import io.github.sawaichi9527.eyepageflow.core.TextEncoding
import io.github.sawaichi9527.eyepageflow.core.WritingMode
import io.github.sawaichi9527.eyepageflow.platform.TrayService
import io.github.sawaichi9527.eyepageflow.render.BookRenderer
import io.github.sawaichi9527.eyepageflow.render.EmptyStateRenderer
import io.github.sawaichi9527.eyepageflow.render.PageCurl
import io.github.sawaichi9527.eyepageflow.render.Theme
import io.github.sawaichi9527.eyepageflow.render.Typeface
import io.github.sawaichi9527.eyepageflow.ui.BookmarkListDialog
import io.github.sawaichi9527.eyepageflow.ui.ChapterListDialog
import io.github.sawaichi9527.eyepageflow.ui.ChapterRuleDialog
import io.github.sawaichi9527.eyepageflow.ui.DisplaySettingsDialog
import io.github.sawaichi9527.eyepageflow.ui.EditorPane
import io.github.sawaichi9527.eyepageflow.ui.FileListDialog
import io.github.sawaichi9527.eyepageflow.ui.FindReplaceDialog
import io.github.sawaichi9527.eyepageflow.ui.LibraryDialog
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JToolBar
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.TransferHandler
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource
import javax.swing.filechooser.FileNameExtensionFilter
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf

object ReaderWindow {
    /** UI 字級下限；FlatLaf 在部分平台會算出更小的值（JBR Wayland 為 13pt）。 */
    private const val MIN_UI_FONT_SIZE = 15

    private var activePanel: ReaderPanel? = null
    private val pendingPaths = mutableListOf<Path>()

    fun launch(
        initialPath: Path? = null,
        openSettings: Boolean = false,
        demo: Boolean = false,
        openEditor: Boolean = false,
        openFind: Boolean = false,
        openChapters: Boolean = false,
        openChapterRule: Boolean = false,
        openBookmarks: Boolean = false,
        openLibrary: Boolean = false,
        openFiles: Boolean = false,
    ) {
        System.setProperty("awt.useSystemAAFontSettings", "on")
        try {
            Typeface.resolveUiFamily()?.let { FlatLaf.setPreferredFontFamily(it) }
            FlatLightLaf.setup()
            enforceMinimumUiFontSize()
        } catch (_: Exception) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }

        SwingUtilities.invokeLater {
            printStartupDiagnostics()
            val frame = JFrame("eyePageFlow")
            frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
            val reader = ReaderPanel(initialPath)
            activePanel = reader
            synchronized(pendingPaths) {
                pendingPaths.forEach(reader::openExternal)
                pendingPaths.clear()
            }
            frame.addWindowListener(
                object : java.awt.event.WindowAdapter() {
                    override fun windowClosing(event: java.awt.event.WindowEvent) {
                        reader.requestClose()
                    }
                },
            )
            frame.jMenuBar = reader.menuBar
            frame.contentPane.add(reader)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
            val tray = TrayService(frame, onExit = reader::requestClose)
            tray.install()
            reader.updateTitle()
            reader.requestCanvasFocus()
            if (openSettings) {
                reader.openSettingsDialog()
            }
            if (demo) {
                reader.startPageTurnDemo()
            }
            if (openEditor) {
                reader.enterEditor()
            }
            if (openFind) {
                reader.openFindDialog(replaceMode = false)
            }
            if (openChapters) {
                reader.openChapterList()
            }
            if (openChapterRule) {
                reader.openChapterRuleDialog()
            }
            if (openBookmarks) {
                reader.openBookmarkManager()
            }
            if (openLibrary) {
                reader.openLibraryManager()
            }
            if (openFiles) {
                reader.openFileList()
            }
        }
    }

    fun openExternal(path: Path) {
        SwingUtilities.invokeLater {
            val panel = activePanel
            if (panel == null) {
                synchronized(pendingPaths) { pendingPaths.add(path) }
            } else {
                panel.openExternal(path)
            }
        }
    }

    /**
     * UI 字級下限。
     *
     * JBR 的 Wayland toolkit 回報的 DPI 讓 FlatLaf 只算出 13pt，中文在 13px 會又硬又擠；
     * 這裡只往上調、不往下調，HiDPI 環境仍沿用 FlatLaf 自己算出的較大值。
     */
    private fun enforceMinimumUiFontSize() {
        val base = UIManager.getFont("defaultFont") ?: return
        if (base.size >= MIN_UI_FONT_SIZE) return
        UIManager.put(
            "defaultFont",
            FontUIResource(base.family, base.style, MIN_UI_FONT_SIZE),
        )
        FlatLightLaf.setup()
    }

    private fun printStartupDiagnostics() {
        println("eyePageFlow")
        println("session=${System.getenv("XDG_SESSION_TYPE") ?: "unknown"}")
        println(
            "java=${System.getProperty("java.version")} " +
                "(${System.getProperty("java.vendor")})",
        )
        println("toolkit=${Toolkit.getDefaultToolkit().javaClass.name}")
        UIManager.getFont("defaultFont")?.let { println("ui font: ${it.family} ${it.size}pt") }
        println(
            "page font: ${Typeface.pageFont(io.github.sawaichi9527.eyepageflow.core.PageMetrics()).name} " +
                "(embedded=${Typeface.hasEmbeddedFont})",
        )

    }
}

private class ReaderPanel(
    private val initialPath: Path?,
) : JPanel(BorderLayout()) {
    private val sampleText = SampleText.longText(repeat = 12)
    private var text = ""
    private var document: TextDocument? = null
    private var lastDirectory: File? = null

    private val settingsStore = SettingsStore(AppPaths.settingsFile())
    private val bookmarkStore = BookmarkStore(AppPaths.bookmarksFile())
    private val recentStore = RecentFilesStore(AppPaths.recentFilesFile())
    private val positionStore = ReadingPositionStore(AppPaths.readingPositionsFile())
    private val libraryStore = LibraryStore(AppPaths.libraryFile())
    private val storedSettings = settingsStore.load()
    private var settings = storedSettings.display
    private var metrics = settings.toPageMetrics()
    private var pageFont = Typeface.pageFont(metrics, settings.fontFamily)
    private var mode = WritingMode.VERTICAL
    private var theme = Theme.fromSettings(settings)
    private var doublePage = settings.usesDoublePage()
    private var equalHeight = settings.layoutMode == LayoutMode.EQUAL_HEIGHT
    private var showBookAndDesktop = settings.showBookAndDesktop
    private var engine = PaginationEngine(text, metrics, mode, pageFont)
    private var pagination: Pagination = engine.paginate()
    private var spreadStart = 0
    private var chapterRule: ChapterRule = storedSettings.chapterRule
    private var bookmarks: List<Bookmark> = bookmarkStore.load()
    private var library: Library = libraryStore.load()

    private val cardLayout = CardLayout()
    private val cards = JPanel(cardLayout)
    private val canvas = BookCanvas()
    private val editorPane = EditorPane { onEditorDirty() }
    private var editing = false
    private var editorDirty = false
    private val statusLeft = JLabel("就緒")
    private val statusRight = JLabel()
    private val toolbar = JToolBar()
    private val recentMenu = JMenu("最近開啟")
    val menuBar: JMenuBar = buildMenuBar()

    init {
        preferredSize = Dimension(1400, 900)
        cards.add(canvas, "reader")
        cards.add(editorPane, "editor")
        add(toolbar, BorderLayout.NORTH)
        add(cards, BorderLayout.CENTER)
        add(createStatusBar(), BorderLayout.SOUTH)
        editorPane.applyTheme(theme, metrics, settings.fontFamily)
        editorPane.bindToggleEditor { toggleEditorMode() }
        editorPane.bindFind { openFindDialog(replaceMode = false) }
        editorPane.bindReplace { openFindDialog(replaceMode = true) }
        editorPane.bindEscape { bossKey() }
        buildToolbar()
        installKeyBindings()
        println(
            "startup typography: fontSize=${metrics.fontSize} " +
                "font=${pageFont.name}/${pageFont.size} " +
                "pageWidth=${metrics.pageWidth} pageHeight=${metrics.pageHeight}",
        )
        rebuildRecentMenu()
        if (initialPath != null) {
            openPath(initialPath)
        }
        updateStatus()
        updateTitle()
    }

    fun titleText(): String {
        val current = document
        if (current == null && !editing && !editorDirty) {
            return "eyePageFlow"
        }
        val name = current?.displayName ?: "未命名"
        val dirty = if (editorDirty || current?.modified == true) " *" else ""
        return "eyePageFlow - $name$dirty"
    }

    fun updateTitle() {
        val frame = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        frame.title = titleText()
    }

    private fun buildMenuBar(): JMenuBar {
        val bar = JMenuBar()

        val fileMenu = JMenu("文件(F)")
        fileMenu.add(menuItem("開啟...", "control O") { openFileDialog() })
        fileMenu.add(menuItem("儲存", "control S") { save() })
        fileMenu.add(menuItem("另存新檔...", "control shift S") { saveAs() })
        fileMenu.addSeparator()
        fileMenu.add(menuItem("檔案列表...", "control W") { openFileList() })
        fileMenu.add(menuItem("預設書架", "control H") { openDefaultShelf() })
        fileMenu.add(menuItem("書庫管理...") { openLibraryManager() })
        fileMenu.addSeparator()
        fileMenu.add(recentMenu)
        fileMenu.addSeparator()
        fileMenu.add(menuItem("結束", "control Q") { requestClose() })
        bar.add(fileMenu)

        val bookmarkMenu = JMenu("書籤(B)")
        bookmarkMenu.add(menuItem("加入書籤", "control B") { addBookmark() })
        bookmarkMenu.add(menuItem("書籤管理", "control G") { openBookmarkManager() })
        bar.add(bookmarkMenu)

        val editMenu = JMenu("編輯(E)")
        editMenu.add(menuItem("復原", "control Z") { editorPane.undo() })
        editMenu.add(menuItem("重做", "control Y") { editorPane.redo() })
        editMenu.addSeparator()
        editMenu.add(menuItem("尋找...", "control F") { openFindDialog(replaceMode = false) })
        editMenu.add(menuItem("取代...", "control R") { openFindDialog(replaceMode = true) })
        editMenu.addSeparator()
        editMenu.add(menuItem("編輯模式", "control E") { toggleEditorMode() })
        bar.add(editMenu)

        val viewMenu = JMenu("檢視(V)")
        viewMenu.add(menuItem("顯示設定...", "control T") { openSettingsDialog() })
        viewMenu.add(menuItem("章節列表...", "control P") { openChapterList() })
        viewMenu.add(menuItem("章節規則...") { openChapterRuleDialog() })
        viewMenu.addSeparator()
        viewMenu.add(menuItem("直排／橫排", "control shift V") { toggleMode() })
        viewMenu.add(menuItem("單頁／雙頁") { toggleDoublePage() })
        viewMenu.add(menuItem("切換主題") { toggleTheme() })
        viewMenu.add(menuItem("低調模式") { toggleLowKey() })
        viewMenu.add(menuItem("老闆鍵（最小化）") { bossKey() })
        bar.add(viewMenu)

        val helpMenu = JMenu("說明(H)")
        helpMenu.add(
            menuItem("關於 eyePageFlow") {
                JOptionPane.showMessageDialog(
                    this,
                    "eyePageFlow 0.1.0\n" +
                        "PageByPage 功能復刻原型\n" +
                        "Java ${System.getProperty("java.version")}",
                    "關於",
                    JOptionPane.INFORMATION_MESSAGE,
                )
            },
        )
        bar.add(helpMenu)
        return bar
    }

    private fun menuItem(
        label: String,
        accelerator: String? = null,
        action: () -> Unit,
    ): JMenuItem = JMenuItem(label).apply {
        if (accelerator != null) {
            this.accelerator = KeyStroke.getKeyStroke(accelerator)
        }
        addActionListener { action() }
    }

    private fun createStatusBar(): JPanel = JPanel(BorderLayout()).apply {
        add(statusLeft, BorderLayout.WEST)
        add(statusRight, BorderLayout.EAST)
    }

    private fun buildToolbar() {
        toolbar.isFloatable = false
        toolbar.add(button("開啟") { openFileDialog() })
        toolbar.add(button("儲存") { save() })
        toolbar.add(button("編輯模式") { toggleEditorMode() })
        toolbar.addSeparator()
        toolbar.add(button("直排／橫排") { toggleMode() })
        toolbar.add(button("單頁／雙頁") { toggleDoublePage() })
        toolbar.addSeparator()
        toolbar.add(button("上一頁") { previousPage() })
        toolbar.add(button("下一頁") { nextPage() })
        toolbar.addSeparator()
        toolbar.add(button("顯示設定") { openSettingsDialog() })
        toolbar.add(button("章節") { openChapterList() })
        toolbar.add(button("書庫") { openLibraryManager() })
        toolbar.add(button("主題") { toggleTheme() })
        toolbar.add(button("低調模式") { toggleLowKey() })
    }

    private fun button(label: String, action: () -> Unit): JButton =
        JButton(label).apply { addActionListener { action() } }

    private fun installKeyBindings() {
        bindGlobal("ESCAPE", "boss-key") { bossKey() }
        // Reader-only single-key shortcuts must not swallow typing in the editor.
        // Ctrl+E is provided by the menu accelerator and the editor's own binding.
        bindReader("RIGHT", "next") { nextPage() }
        bindReader("PAGE_DOWN", "next") { nextPage() }
        bindReader("SPACE", "next") { nextPage() }
        bindReader("LEFT", "prev") { previousPage() }
        bindReader("PAGE_UP", "prev") { previousPage() }
        bindReader("BACK_SPACE", "prev") { previousPage() }
        bindReader("E", "edit-mode-reader") { toggleEditorMode() }
        bindReader("e", "edit-mode-reader-lower") { toggleEditorMode() }
        bindReader("V", "vertical") { toggleMode() }
        bindReader("v", "vertical-lower") { toggleMode() }
        bindReader("D", "double") { toggleDoublePage() }
        bindReader("d", "double-lower") { toggleDoublePage() }
        bindReader("T", "theme") { toggleTheme() }
        bindReader("t", "theme-lower") { toggleTheme() }
        bindReader("B", "lowkey") { toggleLowKey() }
        bindReader("b", "lowkey-lower") { toggleLowKey() }
    }

    private fun bindGlobal(key: String, actionKey: String, action: () -> Unit) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionKey)
        actionMap.put(actionKey, action(action))
    }

    private fun bindReader(key: String, actionKey: String, action: () -> Unit) {
        canvas.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), actionKey)
        canvas.actionMap.put(actionKey, action(action))
    }

    fun bossKey() {
        val frame = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        frame.setState(JFrame.ICONIFIED)
    }

    private fun action(action: () -> Unit): AbstractAction =
        object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                action()
            }
        }

    fun requestCanvasFocus() {
        canvas.requestFocusInWindow()
    }

    private fun currentAnchor(): Int = pagination.page(spreadStart).startOffset

    /** 把目前檔案讀到的位置寫進 `reading-positions.properties`（以字元 offset 記錄）。 */
    private fun saveCurrentPosition() {
        val path = document?.path ?: return
        try {
            positionStore.put(path, currentAnchor())
        } catch (exception: Exception) {
            println("could not save reading position: ${exception.message}")
        }
    }

    /** 開檔後跳到上次讀到的頁；沒有紀錄或位置超出範圍就留在第 1 頁。 */
    private fun resumeSavedPosition(path: Path) {
        val saved = try {
            positionStore.get(path)
        } catch (exception: Exception) {
            println("could not read reading position: ${exception.message}")
            null
        } ?: return
        if (saved <= 0 || pagination.pageCount <= 0) return
        val index = pagination.pageIndexFor(saved)
        spreadStart = if (doublePage) index - index % 2 else index
        spreadStart = spreadStart.coerceIn(0, (pagination.pageCount - 1).coerceAtLeast(0))
        if (spreadStart == 0) return
        updateStatus()
        canvas.repaint()
        println(
            "resumed ${path.fileName} at page ${spreadStart + 1}/${pagination.pageCount} " +
                "offset=$saved",
        )
    }

    private fun rebuild(anchor: Int?) {
        canvas.cancelAnimation()
        val anchorOffset = anchor ?: currentAnchor()
        engine = PaginationEngine(text, metrics, mode, pageFont)
        pagination = engine.paginate()
        spreadStart = if (anchor == null) {
            0
        } else {
            pagination.pageIndexFor(anchorOffset)
        }
        if (doublePage) {
            spreadStart -= spreadStart % 2
        }
        spreadStart = spreadStart.coerceIn(0, (pagination.pageCount - 1).coerceAtLeast(0))
        updateStatus()
        canvas.repaint()
    }

    fun toggleMode() {
        val anchor = currentAnchor()
        mode = if (mode == WritingMode.VERTICAL) WritingMode.HORIZONTAL else WritingMode.VERTICAL
        rebuild(anchor)
    }

    fun toggleDoublePage() {
        val next = if (doublePage) LayoutMode.SINGLE else LayoutMode.DOUBLE
        applySettings(settings.copy(layoutMode = next), currentAnchor())
    }

    fun toggleTheme() {
        val next = if (settings.themeId == Theme.REALISTIC.id) Theme.MODERN else Theme.REALISTIC
        applySettings(settings.withTheme(next), currentAnchor())
    }

    private fun DisplaySettings.withTheme(theme: Theme): DisplaySettings = copy(
        themeId = theme.id,
        desktopPatternId = if (theme.id == Theme.MODERN.id) "desktop.linen" else "desktop.wood-dark",
        coverPatternId = if (theme.id == Theme.MODERN.id) "cover.solid" else "cover.leather-brown",
        pagePatternId = if (theme.id == Theme.MODERN.id) "page.paper-white" else "page.paper-cream",
        desktopColor = theme.desktopColor.rgb and 0xFFFFFF,
        coverColor = theme.coverColor.rgb and 0xFFFFFF,
        pageColor = theme.pageColor.rgb and 0xFFFFFF,
        textColor = theme.textColor.rgb and 0xFFFFFF,
        underlineColor = theme.underlineColor.rgb and 0xFFFFFF,
        gutterColor = theme.gutterColor.rgb and 0xFFFFFF,
    )

    fun toggleLowKey() {
        applySettings(
            settings.copy(showBookAndDesktop = !settings.showBookAndDesktop),
            currentAnchor(),
        )
    }

    fun openSettingsDialog() {
        val owner = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        println(
            "display settings: theme=${settings.themeId} " +
                "layout=${settings.layoutMode} " +
                "pageWidth=${settings.pageWidth} " +
                "fontSize=${settings.fontSize} " +
                "underline=${settings.underline} " +
                "underlineWidth=${settings.underlineWidth} " +
                "punctuation=${settings.punctuationPlacement}",
        )
        val dialog = DisplaySettingsDialog(owner, settings, sampleText) { newSettings ->
            applySettings(newSettings, currentAnchor())
        }
        dialog.isVisible = true
    }

    private fun applySettings(newSettings: DisplaySettings, anchor: Int?) {
        settings = newSettings
        metrics = settings.toPageMetrics()
        pageFont = Typeface.pageFont(metrics, settings.fontFamily)
        theme = Theme.fromSettings(settings)
        doublePage = settings.usesDoublePage()
        equalHeight = settings.layoutMode == LayoutMode.EQUAL_HEIGHT
        showBookAndDesktop = settings.showBookAndDesktop
        editorPane.applyTheme(theme, metrics, settings.fontFamily)
        persistSettings()
        rebuild(anchor)
        println(
            "display settings applied: fontSize=${settings.fontSize} " +
                "lineHeight=${settings.lineHeightFactor} " +
                "layout=${settings.layoutMode} pages=${pagination.pageCount} font=${pageFont.name}",
        )
        updateStatus()
    }

    private fun persistSettings() {
        try {
            settingsStore.saveDisplaySettings(settings)
        } catch (exception: Exception) {
            println("could not save display settings: ${exception.message}")
        }
    }

    private fun persistChapterRule(rule: ChapterRule) {
        try {
            settingsStore.saveChapterRule(rule)
        } catch (exception: Exception) {
            println("could not save chapter rule: ${exception.message}")
        }
    }

    fun toggleEditorMode() {
        if (editing) {
            exitEditor()
        } else {
            enterEditor()
        }
    }

    fun openChapterList() {
        if (editing) {
            syncEditorText()
        }
        val chapters = ChapterParser.parse(text, chapterRule)
        if (chapters.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "目前文件找不到章節標題。",
                "章節列表",
                JOptionPane.INFORMATION_MESSAGE,
            )
            return
        }
        val owner = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        val currentOffset = if (editing) {
            editorPane.caretOffset()
        } else {
            pagination.page(spreadStart).startOffset
        }
        println("chapter list: ${chapters.size} chapters, rule=${chapterRule.id}")
        ChapterListDialog(owner, chapters, currentOffset) { chapter ->
            jumpToOffset(chapter.offset)
        }.isVisible = true
    }

    fun openChapterRuleDialog() {
        val owner = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        println("chapter rule dialog: ${chapterRule.id}")
        ChapterRuleDialog(owner, chapterRule, text) { rule ->
            chapterRule = rule
            persistChapterRule(rule)
            println("chapter rule: ${rule.id}")
        }.isVisible = true
    }

    fun addBookmark() {
        val current = document
        val path = current?.path
        if (current == null || path == null) {
            JOptionPane.showMessageDialog(
                this,
                "請先開啟並儲存檔案，再加入書籤。",
                "加入書籤",
                JOptionPane.INFORMATION_MESSAGE,
            )
            return
        }
        if (editing) {
            syncEditorText()
        }
        val offset = if (editing) {
            editorPane.caretOffset()
        } else {
            pagination.page(spreadStart).startOffset
        }
        val chapters = ChapterParser.parse(text, chapterRule)
        val chapter = chapters.lastOrNull { it.offset <= offset }
        val defaultTitle = chapter?.title ?: "${current.displayName} @ $offset"
        val title = JOptionPane.showInputDialog(this, "書籤名稱", defaultTitle)?.trim()
        if (title.isNullOrBlank()) return
        val bookmark = Bookmark(
            title = title,
            path = path.toAbsolutePath().normalize(),
            offset = offset,
        )
        bookmarks = bookmarks + bookmark
        persistBookmarks()
        println("bookmark added: $title @ $offset")
    }

    fun openBookmarkManager() {
        if (bookmarks.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "目前沒有書籤。",
                "書籤管理",
                JOptionPane.INFORMATION_MESSAGE,
            )
            return
        }
        val owner = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        println("bookmark manager: ${bookmarks.size} bookmarks")
        BookmarkListDialog(
            owner = owner,
            bookmarks = bookmarks,
            onJump = { bookmark -> openBookmark(bookmark) },
            onDelete = { bookmark ->
                bookmarks = bookmarks - bookmark
                persistBookmarks()
            },
        ).isVisible = true
    }

    private fun openBookmark(bookmark: Bookmark) {
        val target = bookmark.path.toAbsolutePath().normalize()
        val current = document?.path?.toAbsolutePath()?.normalize()
        if (current != target) {
            openPath(bookmark.path)
            val opened = document?.path?.toAbsolutePath()?.normalize()
            if (opened != target) return
        }
        jumpToOffset(bookmark.offset)
    }

    private fun persistBookmarks() {
        try {
            bookmarkStore.save(bookmarks)
        } catch (exception: Exception) {
            println("could not save bookmarks: ${exception.message}")
        }
    }

    fun openFileList() {
        val directory = library.defaultShelfPath?.takeIf { Files.isDirectory(it) }
            ?: document?.path?.parent
            ?: lastDirectory?.toPath()
            ?: Path.of(System.getProperty("user.home"))
        if (!Files.isDirectory(directory)) {
            openFileDialog()
            return
        }
        val owner = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        println("file list: $directory")
        FileListDialog(owner, directory) { path -> openPath(path) }.isVisible = true
    }

    fun openDefaultShelf() {
        val directory = library.defaultShelfPath
        if (directory == null || !Files.isDirectory(directory)) {
            openFileList()
            return
        }
        val owner = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        FileListDialog(owner, directory) { path -> openPath(path) }.isVisible = true
    }

    fun openLibraryManager() {
        val owner = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        println("library dialog: ${library.shelves.size} shelves")
        LibraryDialog(owner, library) { updated ->
            library = updated
            try {
                libraryStore.save(updated)
            } catch (exception: Exception) {
                println("could not save library: ${exception.message}")
            }
            println("library: ${updated.shelves.size} shelves")
        }.isVisible = true
    }

    private fun rebuildRecentMenu() {
        recentMenu.removeAll()
        val recent = try {
            recentStore.load()
        } catch (_: Exception) {
            emptyList()
        }
        recentMenu.isEnabled = recent.isNotEmpty()
        recent.forEach { path ->
            recentMenu.add(
                menuItem(path.fileName?.toString() ?: path.toString()) {
                    openPath(path)
                },
            )
        }
    }

    private fun jumpToOffset(offset: Int) {
        if (editing) {
            editorPane.setCaretOffset(offset)
            return
        }
        val pageIndex = pagination.pages.indexOfFirst { offset < it.endOffset }
            .let { if (it < 0) pagination.pageCount - 1 else it }
            .coerceAtLeast(0)
        spreadStart = if (doublePage) pageIndex - pageIndex % 2 else pageIndex
        canvas.repaint()
        updateStatus()
        canvas.requestFocusInWindow()
    }

    fun openFindDialog(replaceMode: Boolean) {
        if (!editing) {
            enterEditor()
        }
        val owner = SwingUtilities.getWindowAncestor(this) as? JFrame ?: return
        println("find dialog: replaceMode=$replaceMode")
        FindReplaceDialog(owner, editorPane, replaceMode).isVisible = true
    }

    fun enterEditor() {
        if (editing) return
        editing = true
        editorDirty = false
        val anchor = pagination.page(spreadStart).startOffset.coerceIn(0, text.length)
        editorPane.applyTheme(theme, metrics, settings.fontFamily)
        editorPane.setDocumentText(text, anchor)
        cardLayout.show(cards, "editor")
        editorPane.focusEditor()
        updateStatus()
        updateTitle()
        println("editor mode: chars=${text.length}")
    }

    fun exitEditor() {
        if (!editing) return
        val caret = editorPane.caretOffset()
        syncEditorText()
        editing = false
        rebuild(caret)
        cardLayout.show(cards, "reader")
        canvas.requestFocusInWindow()
        updateStatus()
        updateTitle()
        println("reader mode: chars=${text.length} pages=${pagination.pageCount}")
    }

    private fun syncEditorText() {
        val newText = editorPane.text()
        if (newText != text) {
            text = newText
            document = (document ?: DocumentIO.newDocument("")).withText(newText)
        }
        editorDirty = false
    }

    private fun onEditorDirty() {
        if (editorDirty) return
        editorDirty = true
        updateTitle()
    }

    private fun confirmDiscardIfDirty(): Boolean {
        if (!editorDirty && document?.modified != true) return true
        val choice = JOptionPane.showConfirmDialog(
            this,
            "目前的變更尚未儲存，要放棄嗎？",
            "未儲存變更",
            JOptionPane.YES_NO_OPTION,
        )
        return choice == JOptionPane.YES_OPTION
    }

    fun requestClose() {
        if (!editorDirty && document?.modified != true) {
            closeWindow()
            return
        }
        val choice = JOptionPane.showConfirmDialog(
            this,
            "目前的變更尚未儲存，要結束嗎？",
            "未儲存變更",
            JOptionPane.YES_NO_OPTION,
        )
        if (choice == JOptionPane.YES_OPTION) {
            closeWindow()
        }
    }

    private fun closeWindow() {
        saveCurrentPosition()
        SwingUtilities.getWindowAncestor(this)?.dispose()
        System.exit(0)
    }

    private fun nextPage() {
        val step = if (doublePage) 2 else 1
        var target = (spreadStart + step).coerceAtMost((pagination.pageCount - 1).coerceAtLeast(0))
        if (doublePage) {
            target -= target % 2
        }
        canvas.animateTo(target)
        updateStatus()
    }

    private fun previousPage() {
        val step = if (doublePage) 2 else 1
        var target = (spreadStart - step).coerceAtLeast(0)
        if (doublePage) {
            target -= target % 2
        }
        canvas.animateTo(target)
        updateStatus()
    }

    fun startPageTurnDemo() {
        applySettings(
            settings.copy(animation = PageTurnAnimation.SLIDE),
            currentAnchor(),
        )
        // 先往前翻三頁、再退回兩頁：兩個方向的動畫都要看得到。
        val moves = listOf(1, 1, 1, -1, -1)
        var index = 0
        val timer = Timer(500) { event ->
            if (index >= moves.size) {
                (event.source as Timer).stop()
                return@Timer
            }
            if (moves[index] > 0) nextPage() else previousPage()
            println("demo page ${spreadStart + 1}/${pagination.pageCount}")
            index += 1
        }
        timer.start()
    }

    fun openFileDialog() {
        val chooser = JFileChooser(lastDirectory)
        chooser.dialogTitle = "開啟純文字檔"
        chooser.fileFilter = FileNameExtensionFilter("純文字檔", "txt", "text", "log", "md")
        chooser.isAcceptAllFileFilterUsed = true
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        val file = chooser.selectedFile ?: return
        lastDirectory = file.parentFile
        openPath(file.toPath())
    }

    fun openExternal(path: Path) {
        openPath(path)
    }

    private fun openPath(path: Path) {
        if (!confirmDiscardIfDirty()) return
        saveCurrentPosition()
        try {
            val loaded = DocumentIO.read(path)
            document = loaded
            text = loaded.text
            editing = false
            editorDirty = false
            cardLayout.show(cards, "reader")
            lastDirectory = path.parent?.toFile()
            rebuild(null)
            resumeSavedPosition(path)
            updateTitle()
            updateStatus()
            canvas.requestFocusInWindow()
            try {
                recentStore.add(path)
                rebuildRecentMenu()
            } catch (exception: Exception) {
                println("could not update recent files: ${exception.message}")
            }
            println(
                "opened ${path.toAbsolutePath()} " +
                    "encoding=${loaded.encoding.displayName} " +
                    "chars=${loaded.text.length} pages=${pagination.pageCount}",
            )
        } catch (exception: DocumentDecodeException) {
            showError("無法解碼檔案", exception.message ?: "未知的解碼錯誤")
        } catch (exception: Exception) {
            showError("無法開啟檔案", exception.message ?: exception.javaClass.simpleName)
        }
    }

    fun save() {
        if (editing) {
            syncEditorText()
        }
        val current = document
        if (current?.path == null) {
            saveAs()
            return
        }
        saveTo(current.path)
    }

    fun saveAs() {
        if (editing) {
            syncEditorText()
        }
        val chooser = JFileChooser(lastDirectory)
        chooser.dialogTitle = "儲存純文字檔"
        chooser.fileFilter = FileNameExtensionFilter("純文字檔", "txt", "text", "log", "md")
        chooser.isAcceptAllFileFilterUsed = true
        val suggested = document?.path?.fileName?.toString() ?: "untitled.txt"
        chooser.selectedFile = File(lastDirectory, suggested)
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return
        val file = chooser.selectedFile ?: return
        if (Files.exists(file.toPath())) {
            val answer = JOptionPane.showConfirmDialog(
                this,
                "檔案已存在，要覆蓋嗎？",
                "確認覆蓋",
                JOptionPane.YES_NO_OPTION,
            )
            if (answer != JOptionPane.YES_OPTION) return
        }
        lastDirectory = file.parentFile
        saveTo(file.toPath())
    }

    private fun saveTo(path: Path, encodingOverride: TextEncoding? = null) {
        val current = document ?: DocumentIO.newDocument(text)
        when (val result = DocumentIO.save(current, path, encodingOverride)) {
            is SaveResult.Success -> {
                document = result.document
                text = result.document.text
                editorDirty = false
                updateTitle()
                updateStatus()
                println(
                    "saved ${path.toAbsolutePath()} " +
                        "encoding=${result.document.encoding.displayName} " +
                        "bom=${result.document.hadBom}",
                )
            }
            is SaveResult.Unmappable -> {
                val chars = result.characters.joinToString(" ") { "U+%04X".format(it.code) }
                val options = arrayOf("另存 UTF-8", "另存 UTF-16LE", "取消")
                val choice = JOptionPane.showOptionDialog(
                    this,
                    "目前編碼 ${result.encoding.displayName} 無法表示下列字元：\n$chars\n\n" +
                        "要改用其他編碼儲存嗎？",
                    "編碼無法表示字元",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0],
                )
                when (choice) {
                    0 -> saveTo(path, TextEncoding.UTF8)
                    1 -> saveTo(path, TextEncoding.UTF16LE)
                }
            }
            is SaveResult.Failure -> showError("儲存失敗", result.message)
        }
    }

    private fun showError(title: String, message: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            title,
            JOptionPane.ERROR_MESSAGE,
        )
    }

    private fun updateStatus() {
        if (editing) {
            statusLeft.text = "編輯模式"
            statusRight.text = ""
            return
        }
        if (document == null) {
            statusLeft.text = "就緒"
            statusRight.text = ""
            return
        }
        val total = pagination.pageCount.coerceAtLeast(1)
        val current = (spreadStart + 1).coerceAtMost(total)
        val percent = current * 100.0 / total
        statusRight.text = String.format(
            Locale.ROOT,
            "%d/%d, %.2f%%",
            current,
            total,
            percent,
        )
        statusLeft.text = if (showBookAndDesktop) "就緒" else "低調模式"
    }

    private inner class BookCanvas : JPanel() {
        init {
            // AUTO：視窗變寬時自動改排雙頁（不必重新分頁，只換顯示）。
            addComponentListener(
                object : java.awt.event.ComponentAdapter() {
                    override fun componentResized(event: java.awt.event.ComponentEvent) {
                        if (settings.layoutMode != LayoutMode.AUTO) return
                        val next = settings.doublePageFor(width, height)
                        if (next == doublePage) return
                        doublePage = next
                        if (next) {
                            spreadStart -= spreadStart % 2
                        }
                        repaint()
                    }
                },
            )
        }

        private var animationTimer: Timer? = null
        private var animationKind = PageTurnAnimation.NONE
        private var animationStartNs = 0L
        private var animationForward = true
        private var oldFrame: BufferedImage? = null
        private var newFrame: BufferedImage? = null

        init {
            preferredSize = Dimension(1400, 820)
            isOpaque = true
            isFocusable = true
            transferHandler = object : TransferHandler() {
                override fun canImport(support: TransferSupport): Boolean =
                    support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

                override fun importData(support: TransferSupport): Boolean {
                    val data = support.transferable.getTransferData(DataFlavor.javaFileListFlavor)
                    val files = data as? List<*> ?: return false
                    val file = files.firstOrNull() as? File ?: return false
                    openPath(file.toPath())
                    return true
                }
            }
            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseClicked(event: MouseEvent) {
                        if (event.clickCount == 2 && document == null) {
                            openFileDialog()
                        }
                    }
                },
            )
        }

        fun animateTo(target: Int) {
            if (target == spreadStart) {
                repaint()
                return
            }
            cancelAnimation()
            if (document == null || settings.animation == PageTurnAnimation.NONE ||
                width <= 0 || height <= 0
            ) {
                spreadStart = target
                repaint()
                return
            }

            oldFrame = capture(spreadStart)
            animationForward = pageTurnForward(mode, spreadStart, target)
            spreadStart = target
            newFrame = capture(target)
            animationKind = settings.animation
            animationStartNs = System.nanoTime()
            animationTimer = Timer(16) {
                val elapsed = System.nanoTime() - animationStartNs
                if (elapsed >= PAGE_TURN_DURATION_NS) {
                    finishAnimation()
                } else {
                    repaint()
                }
            }.apply { start() }
        }

        fun cancelAnimation() {
            animationTimer?.stop()
            animationTimer = null
            animationKind = PageTurnAnimation.NONE
            oldFrame = null
            newFrame = null
        }

        private fun finishAnimation() {
            cancelAnimation()
            repaint()
        }

        private fun capture(index: Int): BufferedImage {
            val image = BufferedImage(
                width.coerceAtLeast(1),
                height.coerceAtLeast(1),
                BufferedImage.TYPE_INT_ARGB,
            )
            val graphics = image.createGraphics()
            renderSpread(graphics, index)
            graphics.dispose()
            return image
        }

        private fun renderSpread(graphics: Graphics2D, index: Int) {
            if (document == null) {
                EmptyStateRenderer.render(
                    g2 = graphics,
                    width = width,
                    height = height,
                    theme = theme,
                )
            } else {
                BookRenderer.render(
                    g2 = graphics,
                    targetWidth = width,
                    targetHeight = height,
                    engine = engine,
                    pagination = pagination,
                    spreadStart = index,
                    theme = theme,
                    doublePage = doublePage,
                    showBookAndDesktop = showBookAndDesktop,
                    font = pageFont,
                    scaleToPageHeight = equalHeight,
                )
            }
        }

        private fun paintAnimation(graphics: Graphics2D) {
            val old = oldFrame ?: return
            val new = newFrame ?: return
            val progress = ((System.nanoTime() - animationStartNs).toDouble() /
                PAGE_TURN_DURATION_NS).coerceIn(0.0, 1.0).toFloat()

            when (animationKind) {
                PageTurnAnimation.FADE -> {
                    graphics.drawImage(old, 0, 0, null)
                    val composite = graphics.create() as Graphics2D
                    composite.composite = AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        progress,
                    )
                    composite.drawImage(new, 0, 0, null)
                    composite.dispose()
                }

                PageTurnAnimation.CURL -> {
                    // 直排雙頁是右翻書：只掀左半那張紙，掃描範圍就是左半，
                    // 掃過中縫會變成「一次翻兩頁」而且方向感被稀釋。
                    val region = if (doublePage && mode == WritingMode.VERTICAL) {
                        Rectangle(0, 0, width / 2, height)
                    } else {
                        Rectangle(0, 0, width, height)
                    }
                    PageCurl.paint(graphics, old, new, progress, animationForward, region)
                }

                PageTurnAnimation.SLIDE -> {
                    val offset = (width * progress).toInt()
                    // 往前翻：舊頁往左推、新頁由右邊進來；往回翻則左右相反。
                    val oldX = if (animationForward) -offset else offset
                    val newX = if (animationForward) width - offset else offset - width
                    graphics.drawImage(old, oldX, 0, null)
                    graphics.drawImage(new, newX, 0, null)
                    val boundary = if (animationForward) (width - offset).toFloat() else offset.toFloat()
                    val shadow = if (animationForward) {
                        GradientPaint(
                            boundary - 18f,
                            0f,
                            Color(0, 0, 0, 0),
                            boundary,
                            0f,
                            Color(0, 0, 0, 90),
                        )
                    } else {
                        GradientPaint(
                            boundary,
                            0f,
                            Color(0, 0, 0, 90),
                            boundary + 18f,
                            0f,
                            Color(0, 0, 0, 0),
                        )
                    }
                    graphics.paint = shadow
                    graphics.fillRect(
                        (if (animationForward) boundary - 18f else boundary).toInt(),
                        0,
                        18,
                        height,
                    )
                    graphics.paint = Color.WHITE
                }

                PageTurnAnimation.NONE -> Unit
            }
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            val g2 = graphics.create() as Graphics2D
            if (animationKind != PageTurnAnimation.NONE && oldFrame != null && newFrame != null) {
                paintAnimation(g2)
            } else {
                renderSpread(g2, spreadStart)
            }
            g2.dispose()
        }

    }
}

private const val PAGE_TURN_DURATION_NS = 220_000_000L

/**
 * 翻頁動畫的摺線方向：`true` 是由右緣往左，`false` 是由左緣往右。
 *
 * 橫排是左翻書，下一頁（頁碼變大）把右邊那張掀到左邊；
 * 直排是右翻書，頁序由右往左，下一頁反而是把左邊那張掀到右邊。
 */
internal fun pageTurnForward(mode: WritingMode, from: Int, to: Int): Boolean =
    if (mode == WritingMode.VERTICAL) to < from else to > from
