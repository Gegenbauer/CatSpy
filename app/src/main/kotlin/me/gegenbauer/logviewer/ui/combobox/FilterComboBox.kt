package me.gegenbauer.logviewer.ui.combobox

import me.gegenbauer.logviewer.databinding.componentName
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.combobox.FilterComboBox.Companion.isHighlight
import me.gegenbauer.logviewer.ui.combobox.highlight.Highlightable
import me.gegenbauer.logviewer.ui.combobox.highlight.HighlighterMultiLineEditor
import me.gegenbauer.logviewer.ui.combobox.highlight.HighlighterSingleLineEditor
import java.awt.event.MouseEvent
import javax.swing.JComboBox
import javax.swing.ToolTipManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.ComboBoxUI
import javax.swing.text.JTextComponent

class FilterComboBox(private val mode: Mode, val useColorTag: Boolean) : JComboBox<String>() {

    enum class Mode {
        SINGLE_LINE_HIGHLIGHT,
        MULTI_LINE_HIGHLIGHT,
        SINGLE_LINE,
        MULTI_LINE,
    }

    val editorComponent: JTextComponent
        get() = (getEditor().editorComponent as JTextComponent).apply {
            toolTipText = this@FilterComboBox.toolTipText
            componentName = this@FilterComboBox.componentName
        }

    var enabledTfTooltip = false
        set(value) {
            field = value
            if (value) {
                updateTooltip()
            }
        }
    var errorMsg: String = ""
        set(value) {
            field = value
            updateTooltip(value.isNotEmpty())
        }

    override fun setUI(ui: ComboBoxUI?) {
        super.setUI(CustomEditorDarkComboBoxUI())
    }

    fun addTooltipUpdateListener() {
        editorComponent.document.addDocumentListener(DocumentHandler())
    }

    fun removeAllColorTags() {
        val textSplit = editorComponent.text.split("|")
        var prevPatternIdx = -1
        var result = ""

        for (item in textSplit) {
            if (prevPatternIdx != -1) {
                result += "|"
                result += item

                if (item.isEmpty() || item.substring(item.length - 1) != "\\") {
                    prevPatternIdx = -1
                }
                continue
            }

            if (item.isNotEmpty()) {
                if (item[0] != '-') {
                    if (result.isNotEmpty()) {
                        result += "|"
                    }

                    result += if (2 < item.length && item[0] == '#' && item[1].isDigit()) {
                        item.substring(2)
                    } else {
                        item
                    }

                    if (item.substring(item.length - 1) == "\\") {
                        prevPatternIdx = 0
                    }
                } else {
                    if (result.isNotEmpty()) {
                        result += "|"
                    }

                    result += if (3 < item.length && item[1] == '#' && item[2].isDigit()) {
                        item.substring(3)
                    } else {
                        item.substring(1)
                    }

                    if (item.substring(item.length - 1) == "\\") {
                        prevPatternIdx = 1
                    }
                }
            }
        }

        editorComponent.text = result

        updateHighlight()
        return
    }

    fun updateHighlight(mode: Mode = this.mode) {
        val editorComponent = editorComponent as Highlightable<*>
        editorComponent.setEnableHighlighter(mode.isHighlight())
    }

    fun setHighlightEnabled(enabled: Boolean) {
        val editorComponent = editorComponent as Highlightable<*>
        editorComponent.setEnableHighlighter(enabled)
    }

    fun removeColorTag() {
        val text = editorComponent.selectedText
        if (text != null && 2 <= text.length && text[0] == '#' && text[1].isDigit()) {
            editorComponent.replaceSelection(text.substring(2))
        }

        updateHighlight()
        return
    }

    fun addColorTag(tag: String) {
        val text = editorComponent.selectedText
        if (text != null) {
            if (2 <= text.length && text[0] == '#' && text[1].isDigit()) {
                editorComponent.replaceSelection(tag + text.substring(2))
            } else {
                editorComponent.replaceSelection(tag + text)
            }
        }

        updateHighlight()
        return
    }

    private fun parsePattern(pattern: String): Array<String> {
        val patterns: Array<String> = Array(2) { "" }

        val patternSplit = pattern.split("|")
        var prevPatternIdx = -1

        for (item in patternSplit) {
            if (prevPatternIdx != -1) {
                patterns[prevPatternIdx] += "|"
                patterns[prevPatternIdx] += item

                if (item.isEmpty() || item.substring(item.length - 1) != "\\") {
                    prevPatternIdx = -1
                }
                continue
            }

            if (item.isNotEmpty()) {
                if (item[0] != '-') {
                    if (patterns[0].isNotEmpty()) {
                        patterns[0] += "|"
                    }

                    if (2 < item.length && item[0] == '#' && item[1].isDigit()) {
                        patterns[0] += item.substring(2)
                    } else {
                        patterns[0] += item
                    }

                    if (item.substring(item.length - 1) == "\\") {
                        prevPatternIdx = 0
                    }
                } else {
                    if (patterns[1].isNotEmpty()) {
                        patterns[1] += "|"
                    }

                    if (3 < item.length && item[1] == '#' && item[2].isDigit()) {
                        patterns[1] += item.substring(3)
                    } else {
                        patterns[1] += item.substring(1)
                    }

                    if (item.substring(item.length - 1) == "\\") {
                        prevPatternIdx = 1
                    }
                }
            }
        }

        return patterns
    }

    fun updateTooltip() {
        updateTooltip(false)
    }

    private fun updateTooltip(isShow: Boolean) {
        if (!enabledTfTooltip) {
            return
        }

        if (errorMsg.isNotEmpty()) {
            var tooltip = "<html><b>"
            tooltip += if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
                "<font size=5 color=#C07070>$errorMsg</font>"
            } else {
                "<font size=5 color=#FF0000>$errorMsg</font>"
            }
            tooltip += "</b></html>"
            editorComponent.toolTipText = tooltip
        } else {
            val patterns = parsePattern(editorComponent.text)
            val includeStr = updateToolTipStrToHtml(patterns[0])
            val excludeStr = updateToolTipStrToHtml(patterns[1])

            var tooltip = "<html><b>$toolTipText</b><br>"
            if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
                tooltip += "<font>INCLUDE : </font>\"<font size=5 color=#7070C0>$includeStr</font>\"<br>"
                tooltip += "<font>EXCLUDE : </font>\"<font size=5 color=#C07070>$excludeStr</font>\"<br>"
            } else {
                tooltip += "<font>INCLUDE : </font>\"<font size=5 color=#0000FF>$includeStr</font>\"<br>"
                tooltip += "<font>EXCLUDE : </font>\"<font size=5 color=#FF0000>$excludeStr</font>\"<br>"
            }
            tooltip += "</html>"
            editorComponent.toolTipText = tooltip
        }

        if (isShow) {
            ToolTipManager.sharedInstance().mouseMoved(MouseEvent(editorComponent, 0, 0, 0, 0, 0, 0, false))
        }
    }

    private inner class DocumentHandler : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) {
            if (enabledTfTooltip && !isPopupVisible) {
                updateTooltip()
            }
        }

        override fun removeUpdate(e: DocumentEvent) {
            if (enabledTfTooltip && !isPopupVisible) {
                updateTooltip()
            }
        }

        override fun changedUpdate(e: DocumentEvent) {
            // do nothing
        }

    }

    fun getAllItems(): List<String> {
        return mutableListOf<String>().apply {
            for (i in 0 until itemCount) {
                add(getItemAt(i))
            }
        }
    }

    fun addAllItems(items: List<String>) {
        items.forEach { addItem(it) }
    }

    companion object {
        fun Mode.isMultiLine(): Boolean {
            return this == Mode.MULTI_LINE || this == Mode.MULTI_LINE_HIGHLIGHT
        }

        fun Mode.isHighlight(): Boolean {
            return this == Mode.SINGLE_LINE_HIGHLIGHT || this == Mode.MULTI_LINE_HIGHLIGHT
        }

        private fun updateToolTipStrToHtml(toolTipStr: String): String {
            if (toolTipStr.isEmpty()) return toolTipStr
            return toolTipStr.replace("&#09", "&amp;#09")
                .replace("\t", "&#09;")
                .replace("&nbsp", "&amp;nbsp")
                .replace(" ", "&nbsp;")
                .replace("|", "<font color=#303030><b>|</b></font>")
        }
    }
}

fun getFilterComboBox(mode: FilterComboBox.Mode = FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT, useColorTag: Boolean = true, multiline: Boolean = false): FilterComboBox {
    val comboBox = FilterComboBox(mode, useColorTag)
    comboBox.isEditable = true
    comboBox.editor = (if (multiline) HighlighterMultiLineEditor(comboBox) else HighlighterSingleLineEditor(comboBox))
        .apply { this.useColorTag = useColorTag }
    comboBox.setHighlightEnabled(mode.isHighlight())
    comboBox.addTooltipUpdateListener()
    return comboBox
}
