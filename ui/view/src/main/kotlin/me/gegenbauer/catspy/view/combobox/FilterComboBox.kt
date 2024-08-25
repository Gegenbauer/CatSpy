package me.gegenbauer.catspy.view.combobox

import me.gegenbauer.catspy.cache.with
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.filter.ui.enableAutoComplete
import me.gegenbauer.catspy.render.LabelRenderer
import me.gegenbauer.catspy.render.Tag
import me.gegenbauer.catspy.render.HtmlStringBuilder
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.DefaultDocumentListener
import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.utils.ui.registerStrokeWhenFocused
import me.gegenbauer.catspy.view.combobox.highlight.CustomEditorDarkComboBoxUI
import me.gegenbauer.catspy.view.combobox.highlight.HighlighterEditor
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.isEmpty
import java.awt.Color
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.ToolTipManager
import javax.swing.UIManager
import javax.swing.plaf.ComboBoxUI
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.JTextComponent
import javax.swing.undo.UndoManager

class FilterComboBox(
    items: List<String>,
    private val highlightEnabled: Boolean = true,
    private val tooltip: String? = null
) : JComboBox<String>(FilterComboBoxModel(items)) {

    /**
     * invoked via reflection
     */
    var filterItem: FilterItem = FilterItem.EMPTY_ITEM
        set(value) {
            field = value
            updateTooltip(true)
        }
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

    var tooltipEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                updateTooltip()
            }
        }
    private val undo = UndoManager()

    private val errorForeground: Color
        get() = UIManager.getColor("CatSpy.accent.red") ?: Color.RED
    private val positiveForeground: Color
        get() = UIManager.getColor("CatSpy.accent.yellow") ?: Color.GREEN
    private val negativeForeground: Color
        get() = UIManager.getColor("CatSpy.accent.red") ?: Color.RED
    private val normalTooltipForeground: Color
        get() = UIManager.getColor("ToolTip.foreground") ?: Color.WHITE

    init {
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
        updateTooltip()
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
        editorComponent.enableAutoComplete(getAllItems().map { it }) {
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
        val errorMessage = filterItem.errorMessage
        if (filterItem.isEmpty() && errorMessage.isEmpty()) {
            return
        }
        val renderedContent = HtmlStringBuilder()
        if (errorMessage.isNotEmpty()) {
            val errorLine = renderLineWithBreak(errorMessage, errorForeground)
            renderedContent.append(errorLine)
        } else {
            val positive = filterItem.positiveFilter.pattern()
            val negative = filterItem.negativeFilter.pattern()
            val positiveLine = "INCLUDE : $positive"
            val negativeLine = "EXCLUDE : $negative"
            if (tooltip != null) {
                renderedContent.append(renderLineWithBreak(tooltip, normalTooltipForeground))
                renderedContent.addSingleTag(Tag.LINE_BREAK)
            }
            renderedContent.append(renderLine(positiveLine, positiveForeground))
            renderedContent.addSingleTag(Tag.LINE_BREAK)
            renderedContent.append(renderLine(negativeLine, negativeForeground))
        }
        editorComponent.toolTipText = renderedContent.build()

        if (isShow) {
            showTooltip()
        }
    }

    private fun showTooltip() {
        if (tooltipEnabled && !isPopupVisible) {
            ToolTipManager.sharedInstance()
                .mouseMoved(MouseEvent(editorComponent, 0, 0, 0, 0, 0, 0, false))
        }
    }

    private fun renderLineWithBreak(content: String, foreground: Color): String {
        val lines = content.lines()
        val renderedContent = HtmlStringBuilder(false)
        lines.forEachIndexed { index, line ->
            renderedContent.append(renderLine(line, foreground))
            if (index != lines.lastIndex) {
                renderedContent.addSingleTag(Tag.LINE_BREAK)
            }
        }
        return renderedContent.build()
    }

    private fun renderLine(content: String, foreground: Color): String {
        return LabelRenderer.obtain().with { renderer ->
            renderer.updateRaw(content)
            renderer.foreground(foreground)
            renderer.bold()
            renderer.renderWithoutTags()
        }
    }

    private inner class DocumentHandler : DefaultDocumentListener() {
        override fun contentUpdate(content: String) {
            if (tooltipEnabled && !isPopupVisible) {
                updateTooltip()
            }
        }
    }

    private fun getAllItems(): List<String> {
        return mutableListOf<String>().apply {
            for (i in 0 until itemCount) {
                add(getItemAt(i))
            }
        }
    }

    override fun addItem(item: String?) {
        if (item.isNullOrEmpty()) return
        super.addItem(item)
        editorComponent.enableAutoComplete(getAllItems().map { it }) {
            hidePopup()
        }
    }
}

fun filterComboBox(
    items: List<String> = emptyList(),
    enableHighlight: Boolean = true,
    tooltip: String? = null
): FilterComboBox {
    val finalTooltip = ("$tooltip\n".takeIf { tooltip != null } ?: "") + STRINGS.toolTip.filter
    val comboBox = FilterComboBox(items, enableHighlight, finalTooltip)
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
