package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.ui.MainUI
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.basic.BasicComboBoxRenderer
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent


class FilterComboBox(private val mode: Mode, val useColorTag: Boolean) : JComboBox<String>() {
    enum class Mode(val value: Int) {
        SINGLE_LINE(0),
        SINGLE_LINE_HIGHLIGHT(1),
        MULTI_LINE(2),
        MULTI_LINE_HIGHLIGHT(3);
    }

    private var editorComponent: JTextComponent
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
            if (value.isEmpty()) {
                updateTooltip(false)
            } else {
                updateTooltip(true)
            }
        }

    init {
        when (this.mode) {
            Mode.SINGLE_LINE -> {
                editor = HighlighterSingleLineEditor()
                val editorComponent = editor.editorComponent as HighlighterSingleLineEditor.HighlighterTextField
                editorComponent.setEnableHighlighter(false)
                this.editorComponent = editorComponent
            }

            Mode.SINGLE_LINE_HIGHLIGHT -> {
                editor = HighlighterSingleLineEditor()
                val editorComponent = editor.editorComponent as HighlighterSingleLineEditor.HighlighterTextField
                editorComponent.setEnableHighlighter(true)
                this.editorComponent = editorComponent
            }

            Mode.MULTI_LINE -> {
                editor = HighlighterMultiLineEditor()
                val editorComponent = editor.editorComponent as HighlighterMultiLineEditor.HighlighterTextArea
                editorComponent.setComboBox(this)
                editorComponent.setEnableHighlighter(false)
                this.editorComponent = editorComponent
            }

            Mode.MULTI_LINE_HIGHLIGHT -> {
                editor = HighlighterMultiLineEditor()
                val editorComponent = editor.editorComponent as HighlighterMultiLineEditor.HighlighterTextArea
                editorComponent.setComboBox(this)
                editorComponent.setEnableHighlighter(true)
                this.editorComponent = editorComponent
            }
        }
        editorComponent.toolTipText = toolTipText
        editorComponent.addKeyListener(KeyHandler())
        editorComponent.document.addDocumentListener(DocumentHandler())
    }

    fun setEnabledFilter(enabled: Boolean) {
        isEnabled = enabled
        isVisible = !(!enabled && editor.item.toString().isEmpty())
    }

    fun isExistItem(item: String): Boolean {
        var isExist = false
        for (idx in 0 until itemCount) {
            if (getItemAt(idx).toString() == item) {
                isExist = true
                break
            }
        }
        return isExist
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

        when (mode) {
            Mode.SINGLE_LINE_HIGHLIGHT -> {
                val editorComponent = editorComponent as HighlighterSingleLineEditor.HighlighterTextField
                editorComponent.setUpdateHighlighter(true)
            }

            Mode.MULTI_LINE_HIGHLIGHT -> {
                val editorComponent = editorComponent as HighlighterMultiLineEditor.HighlighterTextArea
                editorComponent.setUpdateHighlighter(true)
            }

            else -> {

            }
        }
        return
    }

    fun removeColorTag() {
        val text = editorComponent.selectedText
        if (text != null) {
            if (2 <= text.length && text[0] == '#' && text[1].isDigit()) {
                editorComponent.replaceSelection(text.substring(2))
            }
        }

        when (mode) {
            Mode.SINGLE_LINE_HIGHLIGHT -> {
                val editorComponent = editorComponent as HighlighterSingleLineEditor.HighlighterTextField
                editorComponent.setUpdateHighlighter(true)
            }

            Mode.MULTI_LINE_HIGHLIGHT -> {
                val editorComponent = editorComponent as HighlighterMultiLineEditor.HighlighterTextArea
                editorComponent.setUpdateHighlighter(true)
            }

            else -> {

            }
        }
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

        when (mode) {
            Mode.SINGLE_LINE_HIGHLIGHT -> {
                val editorComponent = editorComponent as HighlighterSingleLineEditor.HighlighterTextField
                editorComponent.setUpdateHighlighter(true)
            }
            Mode.MULTI_LINE_HIGHLIGHT -> {
                val editorComponent = editorComponent as HighlighterMultiLineEditor.HighlighterTextArea
                editorComponent.setUpdateHighlighter(true)
            }
            else -> {

            }
        }
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

    fun updateTooltip(isShow: Boolean) {
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
            var includeStr = patterns[0]
            var excludeStr = patterns[1]

            if (includeStr.isNotEmpty()) {
                includeStr = includeStr.replace("&#09", "&amp;#09")
                includeStr = includeStr.replace("\t", "&#09;")
                includeStr = includeStr.replace("&nbsp", "&amp;nbsp")
                includeStr = includeStr.replace(" ", "&nbsp;")
                includeStr = includeStr.replace("|", "<font color=#303030><b>|</b></font>")
            }

            if (excludeStr.isNotEmpty()) {
                excludeStr = excludeStr.replace("&#09", "&amp;#09")
                excludeStr = excludeStr.replace("\t", "&#09;")
                excludeStr = excludeStr.replace("&nbsp", "&amp;nbsp")
                excludeStr = excludeStr.replace(" ", "&nbsp;")
                excludeStr = excludeStr.replace("|", "<font color=#303030><b>|</b></font>")
            }

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

    internal inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(e: KeyEvent) {
            super.keyReleased(e)
        }
    }

    internal inner class DocumentHandler : DocumentListener {
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
        }

    }

    internal class ComboBoxRenderer : BasicComboBoxRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>, value: Any,
            index: Int, isSelected: Boolean, cellHasFocus: Boolean
        ): Component {
            if (isSelected) {
                background = list.selectionBackground
                foreground = list.selectionForeground
                if (-1 < index) {
                    list.toolTipText = list.selectedValue.toString()
                }
            } else {
                background = list.background
                foreground = list.foreground
            }
            font = list.font
            text = value.toString()
            return this
        }
    }

    internal abstract inner class HighlighterEditor : ComboBoxEditor {
        val colorManager = ColorManager.getInstance()
        fun updateHighlighter(textComponent: JTextComponent) {
            if (textComponent.selectedText == null) {
                val painterInclude: Highlighter.HighlightPainter =
                    DefaultHighlighter.DefaultHighlightPainter(colorManager.filterStyleInclude)
                val painterExclude: Highlighter.HighlightPainter =
                    DefaultHighlighter.DefaultHighlightPainter(colorManager.filterStyleExclude)
                val painterSeparator: Highlighter.HighlightPainter =
                    DefaultHighlighter.DefaultHighlightPainter(colorManager.filterStyleSeparator)
                val text = textComponent.text
                val separator = "|"
                try {
                    textComponent.highlighter.removeAllHighlights()
                    var currPos = 0
                    while (currPos < text.length) {
                        val startPos = currPos
                        val separatorPos = text.indexOf(separator, currPos)
                        var endPos = separatorPos
                        if (separatorPos < 0) {
                            endPos = text.length
                        }
                        if (startPos in 0 until endPos) {
                            if (text[startPos] == '-') {
                                textComponent.highlighter.addHighlight(startPos, endPos, painterExclude)
                            } else if (useColorTag && text[startPos] == '#' && startPos < (endPos - 1) && text[startPos + 1].isDigit()) {
                                val color =
                                    Color.decode(colorManager.filterTableColor.strFilteredBGs[text[startPos + 1].digitToInt()])
                                val painterColor: Highlighter.HighlightPainter =
                                    DefaultHighlighter.DefaultHighlightPainter(color)
                                textComponent.highlighter.addHighlight(startPos, startPos + 2, painterColor)
                                textComponent.highlighter.addHighlight(startPos + 2, endPos, painterInclude)
                            } else {
                                textComponent.highlighter.addHighlight(startPos, endPos, painterInclude)
                            }
                        }
                        if (separatorPos >= 0) {
                            textComponent.highlighter.addHighlight(separatorPos, separatorPos + 1, painterSeparator)
                        }
                        currPos = endPos + 1
                    }
                } catch (ex: BadLocationException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    internal inner class HighlighterSingleLineEditor : HighlighterEditor() {
        private val textEditor: HighlighterTextField = HighlighterTextField()
        override fun getEditorComponent(): Component {
            return textEditor
        }

        override fun setItem(item: Any?) {
            if (item is String) {
                textEditor.text = item
                textEditor.setUpdateHighlighter(true)
            } else {
                textEditor.text = null
            }
        }

        override fun getItem(): Any {
            return textEditor.text
        }

        override fun selectAll() {
            textEditor.selectAll()
        }

        override fun addActionListener(l: ActionListener) {
            textEditor.addActionListener(l)
        }

        override fun removeActionListener(l: ActionListener) {
            textEditor.removeActionListener(l)
        }

        internal inner class HighlighterTextField : JTextField() {
            private val fgColor: Color = foreground

            init {
                (highlighter as DefaultHighlighter).drawsLayeredHighlights = false

                addKeyListener(object : KeyListener {
                    override fun keyTyped(e: KeyEvent) {
                    }

                    override fun keyPressed(e: KeyEvent) {
                        setUpdateHighlighter(true)
                    }

                    override fun keyReleased(e: KeyEvent) {
                    }
                })
                addFocusListener(object : FocusListener {
                    override fun focusGained(e: FocusEvent) {}
                    override fun focusLost(e: FocusEvent) {
                        setUpdateHighlighter(true)
                    }
                })

                val colorEventListener = object : ColorManager.ColorEventListener {
                    override fun colorChanged(event: ColorManager.ColorEvent) {
                        setUpdateHighlighter(true)
                        repaint()
                    }
                }

                colorManager.addFilterStyleEventListener(colorEventListener)
                colorManager.addColorEventListener(colorEventListener)
            }

            fun setUpdateHighlighter(updateHighlighter: Boolean) {
                this.updateHighlighter = updateHighlighter
            }

            private var enableHighlighter = false
            private var updateHighlighter = false
            override fun paint(g: Graphics) {
                foreground = if (errorMsg.isNotEmpty()) {
                    if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
                        Color(0xC0, 0x70, 0x70)
                    } else {
                        Color(0xFF, 0x00, 0x00)
                    }
                } else {
                    fgColor
                }

                if (enableHighlighter && updateHighlighter) {
                    updateHighlighter(this)
                    updateHighlighter = false
                }
                super.paint(g)
            }

            fun setEnableHighlighter(enable: Boolean) {
                enableHighlighter = enable
            }
        }
    }

    internal inner class HighlighterMultiLineEditor : HighlighterEditor() {
        private val textEditor: HighlighterTextArea = HighlighterTextArea()
        override fun getEditorComponent(): Component {
            return textEditor
        }

        override fun setItem(item: Any?) {
            if (item is String) {
                textEditor.text = item
                textEditor.setUpdateHighlighter(true)
            } else {
                textEditor.text = null
            }
        }

        override fun getItem(): Any {
            return textEditor.text
        }

        override fun selectAll() {
            textEditor.selectAll()
        }

        override fun addActionListener(l: ActionListener) {
            textEditor.addActionListener(l)
        }

        override fun removeActionListener(l: ActionListener) {
            textEditor.removeActionListener(l)
        }

        inner class HighlighterTextArea : JTextArea() {
            private var enableHighlighter = false
            private lateinit var combo: FilterComboBox
            private var updateHighlighter = false
            private var prevCaret = 0
            private val actionListeners = ArrayList<ActionListener>()

            private val fgColor = foreground

            init {
                lineWrap = true

                (highlighter as DefaultHighlighter).drawsLayeredHighlights = false
                addKeyListener(object : KeyListener {
                    override fun keyTyped(e: KeyEvent) {
                    }

                    override fun keyPressed(e: KeyEvent) {
                        when (e.keyCode) {
                            KeyEvent.VK_ENTER -> {
                                e.consume()
                            }

                            KeyEvent.VK_DOWN -> {
                                prevCaret = caretPosition
                                return
                            }

                            KeyEvent.VK_UP -> {
                                if (combo.isPopupVisible) {
                                    e.consume()
                                }
                                return
                            }

                            KeyEvent.VK_TAB -> {
                                if (e.modifiersEx > 0) {
                                    transferFocusBackward()
                                } else {
                                    transferFocus()
                                }

                                e.consume()
                            }
                        }

                        setUpdateHighlighter(true)
                    }

                    override fun keyReleased(e: KeyEvent) {
                        when (e.keyCode) {
                            KeyEvent.VK_ENTER -> {
                                e.consume()
                                for (listener in actionListeners) {
                                    listener.actionPerformed(ActionEvent(this, ActionEvent.ACTION_PERFORMED, text))
                                }
                            }

                            KeyEvent.VK_DOWN -> {
                                if (prevCaret == caretPosition) {
                                    e.consume()
                                    if (!combo.isPopupVisible) {
                                        combo.showPopup()
                                    }
                                    if (combo.selectedIndex < (combo.itemCount - 1)) {
                                        combo.selectedIndex++
                                    }
                                }
                                return
                            }

                            KeyEvent.VK_UP -> {
                                if (combo.isPopupVisible) {
                                    e.consume()
                                    if (combo.selectedIndex > 0) {
                                        combo.selectedIndex--
                                    }
                                }
                                return
                            }
                        }

                        if (ConfigManager.LaF == MainUI.CROSS_PLATFORM_LAF) {
                            combo.preferredSize = Dimension(combo.preferredSize.width, preferredSize.height + 6)
                        } else {
                            combo.preferredSize = Dimension(combo.preferredSize.width, preferredSize.height)
                        }
                        combo.parent.revalidate()
                        combo.parent.repaint()
                    }
                })
                addFocusListener(object : FocusListener {
                    override fun focusGained(e: FocusEvent) {}
                    override fun focusLost(e: FocusEvent) {
                        setUpdateHighlighter(true)
                    }
                })

                val colorEventListener = object : ColorManager.ColorEventListener {
                    override fun colorChanged(event: ColorManager.ColorEvent) {
                        setUpdateHighlighter(true)
                        repaint()
                    }
                }

                colorManager.addFilterStyleEventListener(colorEventListener)
                colorManager.addColorEventListener(colorEventListener)
            }

            fun setUpdateHighlighter(updateHighlighter: Boolean) {
                this.updateHighlighter = updateHighlighter
            }

            fun setEnableHighlighter(enable: Boolean) {
                enableHighlighter = enable
                if (enableHighlighter) {
                    setUpdateHighlighter(true)
                    repaint()
                }
            }

            override fun paint(g: Graphics) {
                foreground = if (errorMsg.isNotEmpty()) {
                    if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
                        Color(0xC0, 0x70, 0x70)
                    } else {
                        Color(0xFF, 0x00, 0x00)
                    }
                } else {
                    fgColor
                }

                if (enableHighlighter && updateHighlighter) {
                    updateHighlighter(this)
                    updateHighlighter = false
                }
                super.paint(g)
            }

            fun addActionListener(l: ActionListener) {
                actionListeners.add(l)
            }

            fun removeActionListener(l: ActionListener) {
                actionListeners.remove(l)
            }

            fun setComboBox(filterComboBox: FilterComboBox) {
                combo = filterComboBox
            }

            override fun setText(t: String?) {
                super.setText(t)
                if (t != null) {
                    if (ConfigManager.LaF == MainUI.CROSS_PLATFORM_LAF) {
                        combo.preferredSize = Dimension(combo.preferredSize.width, preferredSize.height + 6)
                    } else {
                        combo.preferredSize = Dimension(combo.preferredSize.width, preferredSize.height)
                    }
                }
            }
        }
    }
}
