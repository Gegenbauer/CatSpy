package me.gegenbauer.logviewer.ui.combobox.highlight

import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.ui.FilterComboBox.fontBackgroundInclude
import java.awt.Color
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.UIResource
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent

class HighlighterEditor : BasicComboBoxEditor(), Highlightable, UIResource {

    var useColorTag: Boolean = true
    private var isHighlightEnabled = true

    override fun setItem(item: Any?) {
        if (item is String) {
            if (item != editor.text) {
                editor.text = item
                updateHighlighter()
            }
        } else {
            editor.text = ""
        }
    }

    override fun getItem(): Any {
        return editor.text
    }

    init {
        editor.addCaretListener {
            if (it.dot == it.mark) {
                updateHighlighter()
            } else {
                removeHighlighter()
            }
        }
        editor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                updateHighlighter()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                updateHighlighter()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                updateHighlighter()
            }
        })
    }

    override fun updateHighlighter() {
        val textComponent = editorComponent as JTextComponent
        val painterInclude: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(fontBackgroundInclude)
        val painterExclude: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleExclude)
        val painterSeparator: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleSeparator)
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

    override fun setEnableHighlighter(enable: Boolean) {
        isHighlightEnabled = enable
        if (enable) {
            updateHighlighter()
        } else {
            removeHighlighter()
        }
    }

    protected fun removeHighlighter() {
        val textComponent = editorComponent as JTextComponent
        textComponent.highlighter.removeAllHighlights()
    }

}