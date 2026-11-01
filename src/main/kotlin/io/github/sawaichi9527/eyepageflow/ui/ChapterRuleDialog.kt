package io.github.sawaichi9527.eyepageflow.ui

import io.github.sawaichi9527.eyepageflow.core.ChapterParser
import io.github.sawaichi9527.eyepageflow.core.ChapterRule
import io.github.sawaichi9527.eyepageflow.core.ChapterRuleCatalog
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ChapterRuleDialog(
    owner: Frame,
    current: ChapterRule,
    private val sampleText: String,
    private val onApply: (ChapterRule) -> Unit,
) : JDialog(owner, "章節規則", false) {
    private val presets = ChapterRuleCatalog.presets
    private val presetCombo = JComboBox(
        (presets.map { it.displayName } + "自訂").toTypedArray(),
    )
    private val patternField = JTextField(current.pattern.pattern, 40)
    private val preview = JLabel()
    private var updating = false

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()

        val form = JPanel(GridBagLayout())
        form.border = BorderFactory.createEmptyBorder(12, 12, 6, 12)

        form.add(
            JLabel("規則"),
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                insets = Insets(3, 0, 3, 8)
            },
        )
        form.add(
            presetCombo,
            GridBagConstraints().apply {
                gridx = 1
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(3, 0, 3, 0)
            },
        )
        form.add(
            JLabel("正規表達式"),
            GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                anchor = GridBagConstraints.WEST
                insets = Insets(8, 0, 3, 8)
            },
        )
        form.add(
            patternField,
            GridBagConstraints().apply {
                gridx = 1
                gridy = 1
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(8, 0, 3, 0)
            },
        )
        form.add(
            preview,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 2
                gridwidth = 2
                anchor = GridBagConstraints.WEST
                insets = Insets(8, 0, 0, 0)
            },
        )

        val buttons = JPanel(FlowLayout(FlowLayout.LEFT))
        val cancel = JButton("取消")
        val apply = JButton("套用")
        buttons.add(cancel)
        buttons.add(apply)
        cancel.addActionListener { dispose() }
        apply.addActionListener { applyRule() }

        add(form, BorderLayout.CENTER)
        add(buttons, BorderLayout.SOUTH)
        preferredSize = Dimension(680, 260)
        pack()
        setLocationRelativeTo(owner)
        syncPresetSelection(current)
        updatePreview()
        installListeners()
        addWindowListener(
            object : WindowAdapter() {
                override fun windowOpened(event: WindowEvent) {
                    patternField.requestFocusInWindow()
                }
            },
        )
    }

    private fun installListeners() {
        presetCombo.addActionListener {
            if (updating) return@addActionListener
            val index = presetCombo.selectedIndex
            if (index in presets.indices) {
                patternField.text = presets[index].pattern.pattern
                updatePreview()
            }
        }
        patternField.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = updatePreview()
                override fun removeUpdate(event: DocumentEvent) = updatePreview()
                override fun changedUpdate(event: DocumentEvent) = updatePreview()
            },
        )
    }

    private fun syncPresetSelection(rule: ChapterRule) {
        updating = true
        try {
            val index = presets.indexOfFirst { it.id == rule.id }
            presetCombo.selectedIndex = if (index >= 0) index else presets.size
            patternField.text = rule.pattern.pattern
        } finally {
            updating = false
        }
    }

    private fun updatePreview() {
        val rule = ChapterRuleCatalog.custom(patternField.text)
        if (rule == null) {
            preview.text = "正規表達式無效"
            return
        }
        val count = ChapterParser.parse(sampleText, rule).size
        preview.text = "預覽：$count 個章節"
    }

    private fun applyRule() {
        val rule = ChapterRuleCatalog.custom(patternField.text)
        if (rule == null) {
            preview.text = "正規表達式無效，請修正後再套用"
            return
        }
        onApply(rule)
        dispose()
    }
}
