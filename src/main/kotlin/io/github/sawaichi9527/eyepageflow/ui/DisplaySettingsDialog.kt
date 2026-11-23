package io.github.sawaichi9527.eyepageflow.ui

import io.github.sawaichi9527.eyepageflow.core.ColorSchemeCatalog
import io.github.sawaichi9527.eyepageflow.core.DisplaySettings
import io.github.sawaichi9527.eyepageflow.core.GutterColorCatalog
import io.github.sawaichi9527.eyepageflow.core.LayoutMode
import io.github.sawaichi9527.eyepageflow.core.PageTurnAnimation
import io.github.sawaichi9527.eyepageflow.core.PaginationEngine
import io.github.sawaichi9527.eyepageflow.core.VerticalPunctuation
import io.github.sawaichi9527.eyepageflow.core.WritingMode
import io.github.sawaichi9527.eyepageflow.render.BookRenderer
import io.github.sawaichi9527.eyepageflow.render.PatternCatalog
import io.github.sawaichi9527.eyepageflow.render.PatternOption
import io.github.sawaichi9527.eyepageflow.render.Theme
import io.github.sawaichi9527.eyepageflow.render.Typeface
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Frame
import java.awt.Graphics
import java.awt.GraphicsEnvironment
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JColorChooser
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

class DisplaySettingsDialog(
    owner: Frame,
    initial: DisplaySettings,
    private val previewText: String,
    private val onApply: (DisplaySettings) -> Unit,
) : JDialog(owner, "顯示設定", false) {
    private var draft = initial
    private var updatingControls = false
    private val preview = PreviewPanel()

    private val desktopPatterns: List<PatternOption> = PatternCatalog.desktop
    private val coverPatterns: List<PatternOption> = PatternCatalog.cover
    private val pagePatterns: List<PatternOption> = PatternCatalog.page
    private val desktopPatternCombo = JComboBox(
        desktopPatterns.map { it.displayName }.toTypedArray(),
    )
    private val coverPatternCombo = JComboBox(
        coverPatterns.map { it.displayName }.toTypedArray(),
    )
    private val pagePatternCombo = JComboBox(
        pagePatterns.map { it.displayName }.toTypedArray(),
    )
    private val colorSchemes = ColorSchemeCatalog.schemes
    private val colorSchemeCombo = JComboBox(
        (colorSchemes.map { it.displayName } + "自訂").toTypedArray(),
    )
    private val gutterPresets = GutterColorCatalog.presets
    private val gutterPresetCombo = JComboBox(
        (gutterPresets.map { it.displayName } + "自訂").toTypedArray(),
    )

    private val themeCombo = JComboBox(
        arrayOf(Theme.REALISTIC.displayName, Theme.MODERN.displayName, "自訂"),
    )
    private var desktopColor = Color(initial.desktopColor)
    private var coverColor = Color(initial.coverColor)
    private var pageColor = Color(initial.pageColor)
    private var textColor = Color(initial.textColor)
    private var underlineColor = Color(initial.underlineColor)
    private var gutterColor = Color(initial.gutterColor)
    private var desktopImage = initial.desktopImage
    private var coverImage = initial.coverImage
    private var pageImage = initial.pageImage
    private val desktopImageLabel = JLabel(imageName(initial.desktopImage))
    private val coverImageLabel = JLabel(imageName(initial.coverImage))
    private val pageImageLabel = JLabel(imageName(initial.pageImage))
    private val desktopColorButton = colorButton(desktopColor) {
        desktopColor = it
        markColorSchemeCustom()
        markCustom()
        updateDraft()
    }
    private val coverColorButton = colorButton(coverColor) {
        coverColor = it
        markColorSchemeCustom()
        markCustom()
        updateDraft()
    }
    private val pageColorButton = colorButton(pageColor) {
        pageColor = it
        markColorSchemeCustom()
        markCustom()
        updateDraft()
    }
    private val textColorButton = colorButton(textColor) {
        textColor = it
        markColorSchemeCustom()
        markCustom()
        updateDraft()
    }
    private val underlineColorButton = colorButton(underlineColor) {
        underlineColor = it
        markCustom()
        updateDraft()
    }
    private val gutterColorButton = colorButton(gutterColor) {
        gutterColor = it
        markGutterPresetCustom()
        markColorSchemeCustom()
        markCustom()
        updateDraft()
    }
    private val layoutCombo = JComboBox(
        LayoutMode.entries.map { it.label }.toTypedArray(),
    )
    private val showBookCheck = JCheckBox("顯示書本和桌面", initial.showBookAndDesktop)
    private val fontFamilies: Array<String> =
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .availableFontFamilyNames
            .sorted()
            .toTypedArray()
            .let { families ->
                if (initial.fontFamily in families) families else arrayOf(initial.fontFamily) + families
            }
    private val fontCombo = JComboBox(fontFamilies).apply {
        selectedItem = initial.fontFamily
    }
    private val pageWidthSpinner = JSpinner(
        SpinnerNumberModel(initial.pageWidth, 600, 1600, 10),
    )
    private val fontSizeSpinner = JSpinner(
        SpinnerNumberModel(initial.fontSize.toInt(), 10, 100, 1),
    )
    private val lineHeightSpinner = JSpinner(
        SpinnerNumberModel((initial.lineHeightFactor * 100).roundToInt(), 100, 300, 5),
    )
    private val boldCheck = JCheckBox("加粗", initial.bold)
    private val underlineCheck = JCheckBox("顯示下劃線", initial.underline)
    private val justifyCheck = JCheckBox("左右對齊（橫排）", initial.justify)
    private val underlineWidthLabels = arrayOf("淡", "普通", "粗黑")
    private val underlineWidthValues = floatArrayOf(0.5f, 1.0f, 1.5f)
    private val underlineWidthCombo = JComboBox(underlineWidthLabels)
    private val punctuationCombo = JComboBox(
        VerticalPunctuation.entries.map { it.label }.toTypedArray(),
    )
    private val animationCombo = JComboBox(
        PageTurnAnimation.entries.map { it.label }.toTypedArray(),
    )

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        add(buildForm(), BorderLayout.WEST)
        add(preview, BorderLayout.CENTER)
        preview.settings = initial
        syncControls()
        installListeners()
        minimumSize = Dimension(900, 600)
        size = Dimension(1150, 720)
        setLocationRelativeTo(owner)
    }

    private fun buildForm(): JPanel {
        val form = JPanel(GridBagLayout())
        form.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        var row = 0

        fun section(title: String) {
            val label = JLabel(title).apply {
                font = font.deriveFont(Font.BOLD)
            }
            form.add(
                label,
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = row++
                    gridwidth = 2
                    anchor = GridBagConstraints.WEST
                    insets = Insets(12, 0, 4, 0)
                },
            )
        }

        fun row(label: String, component: java.awt.Component) {
            form.add(
                JLabel(label),
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = row
                    anchor = GridBagConstraints.WEST
                    insets = Insets(3, 0, 3, 8)
                },
            )
            form.add(
                component,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = row++
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = Insets(3, 0, 3, 0)
                },
            )
        }

        section("主題")
        row("主題", themeCombo)

        section("頁面布局")
        row("顯示模式", layoutCombo)
        row("", showBookCheck)
        row("單頁書籍寬度", pageWidthSpinner)

        section("文字")
        row("字體", fontCombo)
        row("大小（點）", fontSizeSpinner)
        row("高度（%）", lineHeightSpinner)
        row("直排標點", punctuationCombo)
        row("", justifyCheck)
        row("", boldCheck)
        row("", underlineCheck)
        row("下劃線顏色", underlineColorButton)
        row("下劃線粗細", underlineWidthCombo)

        section("材質")
        row("桌面圖案", desktopPatternCombo)
        row("桌面圖片", imageControls(desktopImageLabel) { desktopImage = it })
        row("封面圖案", coverPatternCombo)
        row("封面圖片", imageControls(coverImageLabel) { coverImage = it })
        row("頁面圖案", pagePatternCombo)
        row("頁面圖片", imageControls(pageImageLabel) { pageImage = it })

        section("配色方案")
        row("方案", colorSchemeCombo)

        section("顏色")
        row("中縫預設", gutterPresetCombo)
        row("桌面", desktopColorButton)
        row("封面", coverColorButton)
        row("頁面", pageColorButton)
        row("文字", textColorButton)
        row("中縫", gutterColorButton)

        section("翻頁")
        row("動畫", animationCombo)

        val filler = JPanel()
        form.add(
            filler,
            GridBagConstraints().apply {
                gridx = 0
                gridy = row
                gridwidth = 2
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            },
        )

        val buttons = JPanel(FlowLayout(FlowLayout.LEFT))
        val cancel = JButton("取消").apply {
            addActionListener { dispose() }
        }
        val ok = JButton("確定").apply {
            addActionListener {
                onApply(draft)
                dispose()
            }
        }
        buttons.add(cancel)
        buttons.add(ok)

        val panel = JPanel(BorderLayout())
        val scroll = JScrollPane(form)
        scroll.border = BorderFactory.createEmptyBorder()
        panel.add(scroll, BorderLayout.CENTER)
        panel.add(buttons, BorderLayout.SOUTH)
        panel.preferredSize = Dimension(320, 0)
        return panel
    }

    private fun syncControls() {
        themeCombo.selectedIndex = when (draft.themeId) {
            Theme.MODERN.id -> 1
            "custom" -> 2
            else -> 0
        }
        layoutCombo.selectedIndex = LayoutMode.entries.indexOf(draft.layoutMode)
        showBookCheck.isSelected = draft.showBookAndDesktop
        fontCombo.selectedItem = draft.fontFamily
        desktopPatternCombo.selectedIndex = desktopPatterns.indexOfFirst {
            it.id == draft.desktopPatternId
        }.coerceAtLeast(0)
        coverPatternCombo.selectedIndex = coverPatterns.indexOfFirst {
            it.id == draft.coverPatternId
        }.coerceAtLeast(0)
        pagePatternCombo.selectedIndex = pagePatterns.indexOfFirst {
            it.id == draft.pagePatternId
        }.coerceAtLeast(0)
        val schemeIndex = colorSchemes.indexOfFirst {
            it.pageColor == draft.pageColor &&
                it.textColor == draft.textColor &&
                it.gutterColor == draft.gutterColor
        }
        colorSchemeCombo.selectedIndex = if (schemeIndex >= 0) schemeIndex else colorSchemes.size
        val gutterIndex = gutterPresets.indexOfFirst { it.color == draft.gutterColor }
        gutterPresetCombo.selectedIndex = if (gutterIndex >= 0) gutterIndex else gutterPresets.size
        pageWidthSpinner.value = draft.pageWidth
        desktopImageLabel.text = imageName(draft.desktopImage)
        coverImageLabel.text = imageName(draft.coverImage)
        pageImageLabel.text = imageName(draft.pageImage)
        fontSizeSpinner.value = draft.fontSize.toInt()
        lineHeightSpinner.value = (draft.lineHeightFactor * 100).roundToInt()
        boldCheck.isSelected = draft.bold
        underlineCheck.isSelected = draft.underline
        justifyCheck.isSelected = draft.justify
        underlineWidthCombo.selectedIndex = nearestUnderlineWidthIndex(draft.underlineWidth)
        punctuationCombo.selectedIndex = VerticalPunctuation.entries.indexOf(draft.punctuationPlacement)
        animationCombo.selectedIndex = PageTurnAnimation.entries.indexOf(draft.animation)
        desktopColor = Color(draft.desktopColor)
        coverColor = Color(draft.coverColor)
        pageColor = Color(draft.pageColor)
        textColor = Color(draft.textColor)
        underlineColor = Color(draft.underlineColor)
        gutterColor = Color(draft.gutterColor)
        updateColorButtons()
    }

    private fun installListeners() {
        themeCombo.addActionListener {
            applyThemePreset(themeCombo.selectedIndex)
            updateDraft()
        }
        layoutCombo.addActionListener { updateDraft() }
        showBookCheck.addActionListener { updateDraft() }
        fontCombo.addActionListener { updateDraft() }
        desktopPatternCombo.addActionListener {
            if (!updatingControls) patternChanged(desktopPatterns, desktopPatternCombo.selectedIndex) {
                desktopColor = it
            }
        }
        coverPatternCombo.addActionListener {
            if (!updatingControls) patternChanged(coverPatterns, coverPatternCombo.selectedIndex) {
                coverColor = it
            }
        }
        pagePatternCombo.addActionListener {
            if (!updatingControls) patternChanged(pagePatterns, pagePatternCombo.selectedIndex) {
                pageColor = it
            }
        }
        colorSchemeCombo.addActionListener {
            if (!updatingControls) applyColorScheme(colorSchemeCombo.selectedIndex)
        }
        gutterPresetCombo.addActionListener {
            if (!updatingControls) applyGutterPreset(gutterPresetCombo.selectedIndex)
        }
        pageWidthSpinner.addChangeListener { updateDraft() }
        fontSizeSpinner.addChangeListener { updateDraft() }
        lineHeightSpinner.addChangeListener { updateDraft() }
        boldCheck.addActionListener { updateDraft() }
        underlineCheck.addActionListener { updateDraft() }
        justifyCheck.addActionListener { updateDraft() }
        underlineWidthCombo.addActionListener { updateDraft() }
        punctuationCombo.addActionListener { updateDraft() }
        animationCombo.addActionListener { updateDraft() }
    }

    private fun patternChanged(
        options: List<PatternOption>,
        index: Int,
        applyColor: (Color) -> Unit,
    ) {
        val option = options.getOrNull(index) ?: return
        applyColor(Color(option.defaultColor))
        updateColorButtons()
        markCustom()
        updateDraft()
    }

    private fun applyThemePreset(index: Int) {
        val preset = when (index) {
            0 -> Theme.REALISTIC
            1 -> Theme.MODERN
            else -> return
        }
        updatingControls = true
        try {
            val desktopId = if (preset.id == Theme.MODERN.id) "desktop.linen" else "desktop.wood-dark"
            val coverId = if (preset.id == Theme.MODERN.id) "cover.solid" else "cover.leather-brown"
            val pageId = if (preset.id == Theme.MODERN.id) "page.paper-white" else "page.paper-cream"
            desktopPatternCombo.selectedIndex = desktopPatterns.indexOfFirst {
                it.id == desktopId
            }.coerceAtLeast(0)
            coverPatternCombo.selectedIndex = coverPatterns.indexOfFirst {
                it.id == coverId
            }.coerceAtLeast(0)
            pagePatternCombo.selectedIndex = pagePatterns.indexOfFirst {
                it.id == pageId
            }.coerceAtLeast(0)
            desktopColor = preset.desktopColor
            coverColor = preset.coverColor
            pageColor = preset.pageColor
            textColor = preset.textColor
            underlineColor = preset.underlineColor
            gutterColor = preset.gutterColor
            colorSchemeCombo.selectedIndex = colorSchemes.size
            val gutterIndex = gutterPresets.indexOfFirst {
                it.color == (preset.gutterColor.rgb and 0xFFFFFF)
            }
            gutterPresetCombo.selectedIndex = if (gutterIndex >= 0) gutterIndex else gutterPresets.size
            updateColorButtons()
        } finally {
            updatingControls = false
        }
    }

    private fun applyColorScheme(index: Int) {
        val scheme = colorSchemes.getOrNull(index) ?: return
        pageColor = Color(scheme.pageColor)
        textColor = Color(scheme.textColor)
        gutterColor = Color(scheme.gutterColor)
        updateColorButtons()
        markGutterPresetCustom()
        markCustom()
        updateDraft()
    }

    private fun applyGutterPreset(index: Int) {
        val preset = gutterPresets.getOrNull(index) ?: return
        gutterColor = Color(preset.color)
        updateColorButtons()
        markColorSchemeCustom()
        markCustom()
        updateDraft()
    }

    private fun markGutterPresetCustom() {
        if (gutterPresetCombo.selectedIndex != gutterPresets.size) {
            gutterPresetCombo.selectedIndex = gutterPresets.size
        }
    }

    private fun markColorSchemeCustom() {
        if (colorSchemeCombo.selectedIndex != colorSchemes.size) {
            colorSchemeCombo.selectedIndex = colorSchemes.size
        }
    }

    private fun markCustom() {
        if (themeCombo.selectedIndex != 2) {
            themeCombo.selectedIndex = 2
        }
    }

    private fun imageControls(label: JLabel, apply: (String) -> Unit): JPanel {
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        buttons.add(
            JButton("選擇...").apply {
                addActionListener { chooseImage(label, apply) }
            },
        )
        buttons.add(
            JButton("清除").apply {
                addActionListener {
                    apply("")
                    label.text = imageName("")
                    updateDraft()
                }
            },
        )
        val panel = JPanel(BorderLayout(6, 0))
        panel.add(buttons, BorderLayout.WEST)
        panel.add(label, BorderLayout.CENTER)
        return panel
    }

    private fun chooseImage(label: JLabel, apply: (String) -> Unit) {
        val chooser = JFileChooser()
        chooser.dialogTitle = "選擇圖片"
        chooser.fileFilter = FileNameExtensionFilter(
            "圖片",
            "png",
            "jpg",
            "jpeg",
            "gif",
            "bmp",
            "webp",
        )
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        val path = chooser.selectedFile?.absolutePath ?: return
        apply(path)
        label.text = imageName(path)
        markCustom()
        updateDraft()
    }

    private fun imageName(path: String): String =
        if (path.isBlank()) "（無）" else File(path).name

    private fun colorButton(initialColor: Color, onChoose: (Color) -> Unit): JButton =
        JButton().apply {
            preferredSize = Dimension(64, 22)
            isOpaque = true
            background = initialColor
            addActionListener {
                val chosen = JColorChooser.showDialog(
                    this@DisplaySettingsDialog,
                    "選擇顏色",
                    background,
                )
                if (chosen != null) {
                    background = chosen
                    onChoose(chosen)
                }
            }
        }

    private fun updateColorButtons() {
        desktopColorButton.background = desktopColor
        coverColorButton.background = coverColor
        pageColorButton.background = pageColor
        textColorButton.background = textColor
        underlineColorButton.background = underlineColor
        gutterColorButton.background = gutterColor
        listOf(
            desktopColorButton,
            coverColorButton,
            pageColorButton,
            textColorButton,
            underlineColorButton,
            gutterColorButton,
        ).forEach { it.repaint() }
    }

    private fun nearestUnderlineWidthIndex(width: Float): Int =
        underlineWidthValues.indices.minByOrNull {
            kotlin.math.abs(underlineWidthValues[it] - width)
        } ?: 1

    private fun updateDraft() {
        draft = draft.copy(
            themeId = when (themeCombo.selectedIndex) {
                0 -> Theme.REALISTIC.id
                1 -> Theme.MODERN.id
                else -> "custom"
            },
            layoutMode = LayoutMode.entries[layoutCombo.selectedIndex],
            showBookAndDesktop = showBookCheck.isSelected,
            fontFamily = fontCombo.selectedItem as String,
            pageWidth = pageWidthSpinner.value as Int,
            fontSize = (fontSizeSpinner.value as Int).toFloat(),
            lineHeightFactor = (lineHeightSpinner.value as Int) / 100f,
            bold = boldCheck.isSelected,
            underline = underlineCheck.isSelected,
            animation = PageTurnAnimation.entries[animationCombo.selectedIndex],
            desktopPatternId = desktopPatterns[desktopPatternCombo.selectedIndex].id,
            coverPatternId = coverPatterns[coverPatternCombo.selectedIndex].id,
            pagePatternId = pagePatterns[pagePatternCombo.selectedIndex].id,
            desktopImage = desktopImage,
            coverImage = coverImage,
            pageImage = pageImage,
            desktopColor = desktopColor.rgb and 0xFFFFFF,
            coverColor = coverColor.rgb and 0xFFFFFF,
            pageColor = pageColor.rgb and 0xFFFFFF,
            textColor = textColor.rgb and 0xFFFFFF,
            underlineColor = underlineColor.rgb and 0xFFFFFF,
            underlineWidth = underlineWidthValues[underlineWidthCombo.selectedIndex],
            punctuationPlacement = VerticalPunctuation.entries[punctuationCombo.selectedIndex],
            justify = justifyCheck.isSelected,
            gutterColor = gutterColor.rgb and 0xFFFFFF,
        )
        preview.settings = draft
        preview.repaint()
    }

    private inner class PreviewPanel : JPanel() {
        var settings: DisplaySettings = DisplaySettings()

        init {
            background = Color.BLACK
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            val g2 = graphics.create() as Graphics2D
            val metrics = settings.toPageMetrics()
            val font = Typeface.pageFont(metrics, settings.fontFamily)
            val engine = PaginationEngine(previewText, metrics, WritingMode.VERTICAL, font)
            val pagination = engine.paginate()
            BookRenderer.render(
                g2 = g2,
                targetWidth = width,
                targetHeight = height,
                engine = engine,
                pagination = pagination,
                spreadStart = 0,
                theme = Theme.fromSettings(settings),
                doublePage = settings.usesDoublePage(),
                showBookAndDesktop = settings.showBookAndDesktop,
                font = font,
            )
            g2.dispose()
        }
    }
}
