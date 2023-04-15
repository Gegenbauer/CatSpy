package me.gegenbauer.logviewer.ui.combobox.highlight

import java.awt.Component
import javax.swing.JComboBox
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter
import javax.swing.text.JTextComponent

abstract class BaseHighlighterEditor<T : JTextComponent>(protected val textComponent: T, protected val comboBox: JComboBox<*>) : HighlighterEditor() {
    protected val textComponentWrapper = HighlightTextComponentWrapper(textComponent)

    override fun getEditorComponent(): Component {
        return textComponent
    }

    override fun setItem(item: Any?) {
        if (item is String) {
            if (item != textComponent.text) {
                textComponent.text = item
                updateHighlighter()
            }
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

    init {
        textComponent.addCaretListener {
            if (it.dot == it.mark) {
                updateHighlighter()
            } else {
                removeHighlighter()
            }
        }
        textComponent.document.addDocumentListener(object : DocumentListener {
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

    inner class HighlightTextComponentWrapper(textComponent: T) {
        init {
            (textComponent.highlighter as DefaultHighlighter).drawsLayeredHighlights = false
        }

        fun enableHighlighter(enable: Boolean) {
            isHighlightEnabled = enable
            updateHighlighter()
        }
    }
}