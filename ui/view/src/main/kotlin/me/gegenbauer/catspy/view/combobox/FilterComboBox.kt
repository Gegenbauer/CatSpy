package me.gegenbauer.catspy.view.combobox

import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.filter.ui.enableAutoComplete
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.DefaultDocumentListener
import me.gegenbauer.catspy.utils.Key
import me.gegenbauer.catspy.utils.applyTooltip
import me.gegenbauer.catspy.utils.registerStrokeWhenFocused
import me.gegenbauer.catspy.view.combobox.highlight.CustomEditorDarkComboBoxUI
import me.gegenbauer.catspy.view.combobox.highlight.HighlighterEditor
import me.gegenbauer.catspy.view.filter.FilterItem
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComponent
import javax.swing.ToolTipManager
import javax.swing.plaf.ComboBoxUI
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.JTextComponent
import javax.swing.undo.UndoManager

class FilterComboBox(items: List<String>, private val highlightEnabled: Boolean = true, private val tooltip: String? = null) :
    HistoryComboBox<String>(items) {

    var filterItem: FilterItem = FilterItem.EMPTY_ITEM
    var keyListener: KeyListener? = null
        set(value) {
            field = value
            editorComponent.removeKeyListener(field)
            editorComponent.addKeyListener(field)
        }
    var mouseListener: MouseListener? = null
        set(value) {
            field = value
            editorComponent.removeMouseListener(field)
            editorComponent.addMouseListener(field)
        }
    private val editorComponent: JTextComponent // create new instance when theme changed(setUI invoked)
        get() = getEditor().editorComponent as JTextComponent

    var tooltipEnabled = false
        set(value) {
            field = value
            if (value) {
                updateTooltip()
            }
        }
    private var errorMessage: String = ""
        set(value) {
            field = value
            updateTooltip(value.isNotEmpty())
        }
    private val undo = UndoManager()

    init {
        toolTipText = tooltip
        configureEditorComponent(editorComponent)
        updateUI()
    }

    override fun setUI(ui: ComboBoxUI?) {
        val newEditor = if (highlightEnabled) {
            HighlighterEditor()
        } else {
            BasicComboBoxEditor.UIResource()
        }
        super.setUI(CustomEditorDarkComboBoxUI(newEditor.apply {
            if (getEditor() != null) {
                Bindings.rebind(getEditor().editorComponent as JComponent, newEditor.editorComponent as JComponent)
            }
            configureEditorComponent(newEditor.editorComponent as JTextComponent)
            setEditor(newEditor)
            editorComponent.addKeyListener(keyListener)
            editorComponent.addMouseListener(mouseListener)
        }))
    }

    private fun configureEditorComponent(editorComponent: JTextComponent) {
        editorComponent applyTooltip tooltip
        editorComponent.document.addUndoableEditListener {
            undo.addEdit(it.edit)
        }
        editorComponent.registerStrokeWhenFocused(Key.C_Z, "Undo") {
            runCatching {
                if (undo.canUndo()) {
                    undo.undo()
                }
            }
        }
        editorComponent.registerStrokeWhenFocused(Key.C_Y, "Redo") {
            runCatching {
                if (undo.canRedo()) {
                    undo.redo()
                }
            }
        }
        editorComponent.enableAutoComplete(getAllItems().map { it.content }) {
            hidePopup()
        }
    }

    fun addTooltipUpdateListener() {
        editorComponent.document.addDocumentListener(DocumentHandler())
    }

    fun updateTooltip() {
        updateTooltip(false)
    }

    private fun updateTooltip(isShow: Boolean) {
        if (!tooltipEnabled) {
            return
        }

        if (errorMessage.isNotEmpty()) {
            var tooltip = "<html><b>"
            tooltip += if (ThemeManager.isDarkTheme) {
                "<font size=5 color=#C07070>$errorMessage</font>"
            } else {
                "<font size=5 color=#FF0000>$errorMessage</font>"
            }
            tooltip += "</b></html>"
            editorComponent.toolTipText = tooltip
        } else {
            val includeStr = updateToolTipStrToHtml(filterItem.positiveFilter.pattern())
            val excludeStr = updateToolTipStrToHtml(filterItem.negativeFilter.pattern())

            var tooltip = "<html><b>$toolTipText</b><br>"
            if (ThemeManager.isDarkTheme) {
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

    private inner class DocumentHandler : DefaultDocumentListener() {
        override fun contentUpdate(content: String) {
            if (tooltipEnabled && !isPopupVisible) {
                updateTooltip()
            }
        }
    }

    private fun getAllItems(): List<HistoryItem<String>> {
        return mutableListOf<HistoryItem<String>>().apply {
            for (i in 0 until itemCount) {
                add(getItemAt(i))
            }
        }
    }

    override fun addItem(item: HistoryItem<String>?) {
        if (item == null || item.content.isEmpty()) return
        super.addItem(item)
        editorComponent.enableAutoComplete(getAllItems().map { it.content }) {
            hidePopup()
        }
    }

    companion object {

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

fun filterComboBox(items: List<String> = emptyList(), enableHighlight: Boolean = true, tooltip: String? = null): FilterComboBox {
    val comboBox = FilterComboBox(items, enableHighlight, "$tooltip\n${STRINGS.toolTip.comboFilter}")
    comboBox.isEditable = true
    comboBox.addTooltipUpdateListener()
    return comboBox
}

fun darkComboBox(tooltip: String? = null): FilterComboBox {
    return filterComboBox(enableHighlight = false, tooltip = tooltip)
}

fun readOnlyComboBox(tooltip: String? = null, items: List<String> = emptyList()): FilterComboBox {
    return filterComboBox(items, enableHighlight = false, tooltip = tooltip).apply { isEditable = false }
}
