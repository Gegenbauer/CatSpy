package me.gegenbauer.logviewer.ui.combobox.highlight

import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.ui.FilterComboBox.fontBackgroundInclude
import java.awt.Color
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTextField
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
    private val customHighlighters = arrayListOf<Any>()

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
            if (it.dot != it.mark) {
                removeHighlighter()
            } else {
                updateHighlighter()
            }
        }
        editor.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                removeHighlighter()
            }

            override fun mouseReleased(e: MouseEvent?) {
                val selectedText = (e?.source as? JTextField)?.selectedText
                if (selectedText != null) {
                    removeHighlighter()
                } else {
                    updateHighlighter()
                }
            }
        })
        editor.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                updateHighlighter()
            }
        })
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
        if (!isHighlightEnabled) {
            return
        }
        val painterInclude: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(fontBackgroundInclude)
        val painterExclude: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleExclude)
        val painterSeparator: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleSeparator)
        val text = editor.text
        val separator = "|"
        try {
            removeHighlighter()
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
                        customHighlighters.add(editor.highlighter.addHighlight(startPos, endPos, painterExclude))
                    } else if (useColorTag && text[startPos] == '#' && startPos < (endPos - 1) && text[startPos + 1].isDigit()) {
                        val color =
                            Color.decode(ColorManager.filterTableColor.strFilteredBGs[text[startPos + 1].digitToInt()])
                        val painterColor: Highlighter.HighlightPainter =
                            DefaultHighlighter.DefaultHighlightPainter(color)
                        customHighlighters.add(editor.highlighter.addHighlight(startPos, startPos + 2, painterColor))
                        customHighlighters.add(editor.highlighter.addHighlight(startPos + 2, endPos, painterInclude))
                    } else {
                        customHighlighters.add(editor.highlighter.addHighlight(startPos, endPos, painterInclude))
                    }
                }
                if (separatorPos >= 0) {
                    customHighlighters.add(editor.highlighter.addHighlight(separatorPos, separatorPos + 1, painterSeparator))
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

    private fun removeHighlighter() {
        val textComponent = editorComponent as JTextComponent
        customHighlighters.forEach(textComponent.highlighter::removeHighlight)
    }

}