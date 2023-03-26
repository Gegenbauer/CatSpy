package me.gegenbauer.logviewer.ui.combobox

import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.ui.MainUI
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.basic.BasicComboBoxRenderer
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent

class FilterComboBox(private val mode: Mode, val useColorTag: Boolean) : JComboBox<String>() {

    interface IFilterComboBoxMode {
        val editor: HighlighterEditor

        val editorComponent: JTextComponent

        fun configureEditorComponent(editorComponent: JTextComponent)

    }

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
                val editorComponent = editor.editorComponent as HighlighterTextField
                editorComponent.setEnableHighlighter(false)
                this.editorComponent = editorComponent
            }

            Mode.SINGLE_LINE_HIGHLIGHT -> {
                editor = HighlighterSingleLineEditor()
                val editorComponent = editor.editorComponent as HighlighterTextField
                editorComponent.setEnableHighlighter(true)
                this.editorComponent = editorComponent
            }

            Mode.MULTI_LINE -> {
                editor = HighlighterMultiLineEditor()
                val editorComponent = editor.editorComponent as HighlighterTextArea
                editorComponent.setComboBox(this)
                editorComponent.setEnableHighlighter(false)
                this.editorComponent = editorComponent
            }

            Mode.MULTI_LINE_HIGHLIGHT -> {
                editor = HighlighterMultiLineEditor()
                val editorComponent = editor.editorComponent as HighlighterTextArea
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

    // TODO 在将 xml 配置文件改成 json 配置文件后，需要整改
    fun isExistItem(item: String): Boolean {
        for (idx in 0 until itemCount) {
            if (getItemAt(idx).toString() == item) {
                return true
            }
        }
        return false
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

    private fun updateHighlight() {
        when (mode) {
            Mode.SINGLE_LINE_HIGHLIGHT -> {
                val editorComponent = editorComponent as HighlighterTextField
                editorComponent.setUpdateHighlighter(true)
            }

            Mode.MULTI_LINE_HIGHLIGHT -> {
                val editorComponent = editorComponent as HighlighterTextArea
                editorComponent.setUpdateHighlighter(true)
            }

            else -> {
                // do nothing
            }
        }
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

    private class KeyHandler : KeyAdapter()

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

    abstract inner class HighlighterEditor : ComboBoxEditor {
        fun updateHighlighter(textComponent: JTextComponent) {
            if (textComponent.selectedText == null) {
                val painterInclude: Highlighter.HighlightPainter =
                    DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleInclude)
                val painterExclude: Highlighter.HighlightPainter =
                    DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleExclude)
                val painterSeparator: Highlighter.HighlightPainter =
                    DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleSeparator)
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
                                    Color.decode(ColorManager.filterTableColor.strFilteredBGs[text[startPos + 1].digitToInt()])
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

    private abstract inner class BaseHighlighterEditor<T : JTextComponent>(protected val textComponent: T) :
        HighlighterEditor() {
        protected val textComponentWrapper = HighlightTextComponentWrapper(textComponent)

        override fun getEditorComponent(): Component {
            return textComponent
        }

        override fun setItem(item: Any?) {
            if (item is String) {
                textComponent.text = item
                textComponentWrapper.setUpdateHighlighter(true)
            } else {
                textComponent.text = ""
            }
        }

        override fun getItem(): Any {
            return textComponent.text
        }

        override fun selectAll() {
            textComponent.selectAll()
        }

        inner class HighlightTextComponentWrapper(private val textComponent: T) {
            private var enableHighlighter = false
            private var updateHighlighter = false
            private val fgColor = textComponent.foreground

            init {
                (textComponent.highlighter as DefaultHighlighter).drawsLayeredHighlights = false

                textComponent.addFocusListener(object : FocusListener {
                    override fun focusGained(e: FocusEvent) {
                        // do nothing
                    }

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

                ColorManager.addFilterStyleEventListener(colorEventListener)
                ColorManager.addColorEventListener(colorEventListener)
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

            fun paint() {
                textComponent.foreground = if (errorMsg.isNotEmpty()) {
                    if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
                        Color(0xC0, 0x70, 0x70)
                    } else {
                        Color(0xFF, 0x00, 0x00)
                    }
                } else {
                    fgColor
                }

                if (enableHighlighter && updateHighlighter) {
                    updateHighlighter(textComponent)
                    updateHighlighter = false
                }
            }
        }
    }

    private inner class HighlighterSingleLineEditor :
        BaseHighlighterEditor<HighlighterTextField>(HighlighterTextField()) {

        init {
            textComponent.setTextComponentWrapper(textComponentWrapper)
        }

        override fun addActionListener(l: ActionListener) {
            textComponent.addActionListener(l)
        }

        override fun removeActionListener(l: ActionListener) {
            textComponent.removeActionListener(l)
        }
    }

    private inner class HighlighterTextField : JTextField() {
        private lateinit var textComponentWrapper: BaseHighlighterEditor<HighlighterTextField>.HighlightTextComponentWrapper

        init {
            addKeyListener(object : KeyListener {
                override fun keyTyped(e: KeyEvent) {
                    // do nothing
                }

                override fun keyPressed(e: KeyEvent) {
                    textComponentWrapper.setUpdateHighlighter(true)
                }

                override fun keyReleased(e: KeyEvent) {
                    // do nothing
                }
            })
        }

        fun setEnableHighlighter(enable: Boolean) {
            textComponentWrapper.setEnableHighlighter(enable)
        }

        fun setUpdateHighlighter(updateHighlighter: Boolean) {
            textComponentWrapper.setUpdateHighlighter(updateHighlighter)
        }

        override fun paint(graphics: Graphics) {
            textComponentWrapper.paint()
            super.paint(graphics)
        }

        fun setTextComponentWrapper(textComponentWrapper: BaseHighlighterEditor<HighlighterTextField>.HighlightTextComponentWrapper) {
            this.textComponentWrapper = textComponentWrapper
        }
    }

    private inner class HighlighterMultiLineEditor : BaseHighlighterEditor<HighlighterTextArea>(HighlighterTextArea()) {

        init {
            textComponent.setTextComponentWrapper(textComponentWrapper)
        }

        override fun addActionListener(l: ActionListener) {
            textComponent.addActionListener(l)
        }

        override fun removeActionListener(l: ActionListener) {
            textComponent.removeActionListener(l)
        }
    }

    private class HighlighterTextArea : JTextArea() {
        private val actionListeners = ArrayList<ActionListener>()
        private var prevCaret = 0
        private lateinit var combo: FilterComboBox
        private lateinit var textComponentWrapper: BaseHighlighterEditor<HighlighterTextArea>.HighlightTextComponentWrapper

        init {
            lineWrap = true

            addKeyListener(object : KeyListener {
                override fun keyTyped(e: KeyEvent) {
                    // do nothing
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
        }

        fun setTextComponentWrapper(textComponentWrapper: BaseHighlighterEditor<HighlighterTextArea>.HighlightTextComponentWrapper) {
            this.textComponentWrapper = textComponentWrapper
        }

        fun setUpdateHighlighter(updateHighlighter: Boolean) {
            textComponentWrapper.setUpdateHighlighter(updateHighlighter)
        }

        fun setEnableHighlighter(enable: Boolean) {
            textComponentWrapper.setEnableHighlighter(enable)
        }

        override fun paint(g: Graphics) {
            textComponentWrapper.paint()
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
