package me.gegenbauer.catspy.view.combobox.highlight

import com.formdev.flatlaf.FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.utils.ui.DefaultDocumentListener
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTextField
import javax.swing.UIManager
import javax.swing.plaf.UIResource
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter.HighlightPainter
import javax.swing.text.JTextComponent

class HighlighterEditor : BasicComboBoxEditor.UIResource(), Highlightable, UIResource {

    override val painterInclude: HighlightPainter
        get() = DefaultHighlightPainter(UIManager.getColor("ComboBox.editableBackground"))
    override val painterExclude: HighlightPainter
        get() = DefaultHighlightPainter(UIManager.getColor("FilterComboBox.excludeFilterBackground"))
    override val painterSeparator: HighlightPainter
        get() = DefaultHighlightPainter(UIManager.getColor("FilterComboBox.orSeparatorBackground"))

    private var isHighlightEnabled = true
    private val customHighlighters = arrayListOf<Any>()
    private val textEditor = editorComponent as JTextComponent

    override fun setItem(item: Any?) {
        if (item is String) {
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
            putClientProperty(TEXT_FIELD_SHOW_CLEAR_BUTTON, true)
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
            override fun mousePressed(e: MouseEvent) {
                removeHighlighter()
            }

            override fun mouseReleased(e: MouseEvent) {
                val selectedText = (e.source as? JTextField)?.selectedText
                if (selectedText != null) {
                    removeHighlighter()
                } else {
                    updateHighlighter()
                }
            }
        })
        textEditor.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                updateHighlighter()
            }
        })
        textEditor.document.addDocumentListener(object : DefaultDocumentListener() {
            override fun contentUpdate(content: String) {
                updateHighlighter()
            }
        })
    }

    override fun updateHighlighter() {
        val text = textEditor.text
        if (!isHighlightEnabled || text.isBlank()) {
            removeHighlighter()
            return
        }
        runCatching {
            removeHighlighter()
            val filters = text.split(SEPARATOR)
            var curIndex = 0

            filters.forEach { filter ->
                val startIndex = curIndex
                val endIndex = curIndex + filter.length
                if (filter.isNotEmpty()) {
                    val painter = if (filter[0] == NEGATIVE_PREFIX) painterExclude else painterInclude
                    customHighlighters.add(textEditor.highlighter.addHighlight(startIndex, endIndex, painter))
                }
                customHighlighters.add(textEditor.highlighter.addHighlight(endIndex, endIndex + 1, painterSeparator))
                curIndex = endIndex + 1
            }
        }.onFailure {
            GLog.e(TAG, "[updateHighlighter]", it)
        }
    }

    override fun setEnableHighlighter(enable: Boolean) {
        isHighlightEnabled = enable
        enable.takeIf { true }?.run { updateHighlighter() } ?: removeHighlighter()
    }

    private fun removeHighlighter() {
        val textComponent = editorComponent as JTextComponent
        customHighlighters.forEach(textComponent.highlighter::removeHighlight)
    }

    companion object {
        private const val TAG = "HighlighterEditor"
        const val SEPARATOR = "|"
        const val NEGATIVE_PREFIX = '-'
    }
}