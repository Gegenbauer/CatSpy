package me.gegenbauer.catspy.ui.combobox.highlight

import me.gegenbauer.catspy.ui.ColorScheme
import me.gegenbauer.catspy.ui.FilterComboBoxTheme.fontBackgroundInclude
import me.gegenbauer.catspy.ui.combobox.HistoryItem
import org.fife.ui.rsyntaxtextarea.SquiggleUnderlineHighlightPainter
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

    private var isHighlightEnabled = true
    private val customHighlighters = arrayListOf<Any>()
    private val textEditor = editorComponent as JTextComponent
    private val errorHighlightPainter = SquiggleUnderlineHighlightPainter(Color.RED)
    private val excludeHighlightPainter = DefaultHighlighter.DefaultHighlightPainter(ColorScheme.filterStyleExclude)
    private val includeHighlightPainter = DefaultHighlighter.DefaultHighlightPainter(fontBackgroundInclude)
    private val operatorHighlightPainter = DefaultHighlighter.DefaultHighlightPainter(ColorScheme.filterStyleSeparator)

    override fun setItem(item: Any?) {
        if (item is String || item is HistoryItem<*>) {
            if (item.toString() != textEditor.text) {
                textEditor.text = item.toString()
                updateHighlighter()
            }
        } else {
            textEditor.text = ""
        }
    }

    override fun createEditorComponent(): JTextField {
        return super.createEditorComponent().apply {
            //enableAutoComplete()
        }
    }

    override fun getItem(): Any {
        return textEditor.text
    }

    init {
        textEditor.addCaretListener {
            if (it.dot != it.mark) {
                removeHighlighter()
            } else {
                updateHighlighter()
            }
        }
        textEditor.addMouseListener(object : MouseAdapter() {
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
        textEditor.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                updateHighlighter()
            }
        })
        textEditor.document.addDocumentListener(object : DocumentListener {
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
        val painterInclude: Highlighter.HighlightPainter =
            DefaultHighlighter.DefaultHighlightPainter(fontBackgroundInclude)
        val painterExclude: Highlighter.HighlightPainter =
            DefaultHighlighter.DefaultHighlightPainter(ColorScheme.filterStyleExclude)
        val painterSeparator: Highlighter.HighlightPainter =
            DefaultHighlighter.DefaultHighlightPainter(ColorScheme.filterStyleSeparator)
        val text = textEditor.text
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
                        customHighlighters.add(textEditor.highlighter.addHighlight(startPos, endPos, painterExclude))
                    } else {
                        customHighlighters.add(textEditor.highlighter.addHighlight(startPos, endPos, painterInclude))
                    }
                }
                if (separatorPos >= 0) {
                    customHighlighters.add(
                        textEditor.highlighter.addHighlight(
                            separatorPos,
                            separatorPos + 1,
                            painterSeparator
                        )
                    )
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