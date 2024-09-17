package me.gegenbauer.catspy.log.ui.customize

import info.clearthought.layout.TableLayout
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.OnScrollToEndListener
import me.gegenbauer.catspy.utils.ui.ScrollToEndListenerSupport
import me.gegenbauer.catspy.utils.ui.addOnScrollToEndListener
import me.gegenbauer.catspy.utils.ui.showWarningDialog
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import me.gegenbauer.catspy.view.scrollpane.SingleDirectionScrollPane
import java.awt.Color
import java.awt.Component
import java.util.*
import javax.swing.AbstractCellEditor
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultCellEditor
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn

interface EditableTablePanel<T> {

    var items: List<T>

    fun configure()
}

abstract class BaseEditableTablePanel<T> : EditableTablePanel<T>, JPanel(), LogMetadataEditor, EditableContainer,
    ScrollToEndListenerSupport, EditEventSource {

    override var items: List<T> = emptyList()
        get() = getUpdatedItems()
        set(value) {
            field = value
            originalItems.clear()
            originalItems.addAll(value)
            setItemsToTable(value)
        }

    override val isEditing: Boolean
        get() = actionPanel.isEditing

    protected val isNightMode: Boolean
        get() = logMetadataEditModel.isDarkMode

    protected abstract val tableName: String

    protected abstract val columnParams: List<ColumnParam>

    protected open var allColumnsEditable: Boolean = true

    protected open val actionVisibilityParam: EditableTableActionPanel.ActionVisibilityParam =
        EditableTableActionPanel.ActionVisibilityParam()

    protected open val actionEnableStateParam: EditableTableActionPanel.ActionEnableStateParam =
        EditableTableActionPanel.ActionEnableStateParam()

    protected open val customActions: List<EditableTableActionPanel.CustomAction> = emptyList()

    protected open val hint: String = EMPTY_STRING

    protected abstract fun createNewItem(): T

    protected abstract fun T.toRowItem(): List<Any?>

    protected abstract fun getUpdatedItems(): List<T>

    protected val table: JTable = JTable()
    private val actionPanel = EditableTableActionPanel()

    private val container = JPanel()
    private val scrollPane = SingleDirectionScrollPane(table, true)
    private val hintComponent = MultilineLabel()

    private val editEventListeners = mutableListOf<EditEventListener>()

    protected val originalItems = mutableListOf<T>()

    private var logMetadataEditModel: LogMetadataEditModel = LogMetadataModel.default.toEditModel()

    private val logMetadataSelectListener = object : OnMetadataChangedListener {
        override fun onLogMetadataSelected(metadata: LogMetadataModel) {
            handleLoadTemplateAction(metadata)
        }
    }

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        logMetadataEditModel = metadata
        allColumnsEditable = !metadata.model.isBuiltIn
        actionVisibilityParam.setBuiltInState(metadata.model.isBuiltIn)
        actionPanel.setActionVisibilityParam(actionVisibilityParam)
        actionPanel.setExcludedMetadata(metadata.model.logType)
    }

    override fun onNightModeChanged(isDark: Boolean) {
        val currentItems = getUpdatedItems()
        logMetadataEditModel = logMetadataEditModel.copy(isDarkMode = isDark)
        setItemsToTable(currentItems)
    }

    override fun configure() {
        initUI()

        hintComponent.text = hint.replace("\\", EMPTY_STRING)

        actionPanel.bindTable(table) { createNewItem().toRowItem() }
        actionPanel.setCustomActions(customActions)
        actionPanel.setActionVisibilityParam(actionVisibilityParam)
        actionPanel.setActionEnableStateParam(actionEnableStateParam)
        actionPanel.addOnSelectedMetadataChangedListener(logMetadataSelectListener)

        configureColumnHeaderRenderer()
        configureColumnEditor()
        configureColorColumns(table)
    }

    private fun configureColorColumns(table: JTable) {
        columnParams.filter { it.columnClass == Color::class.java }.forEach {
            val index = columnParams.indexOf(it)
            table.columnModel.getColumn(index).cellRenderer = ColorRenderer()
            table.columnModel.getColumn(index).cellEditor = ColorEditor()
        }
    }

    private fun initUI() {
        border = BorderFactory.createTitledBorder(tableName)

        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        container.layout = TableLayout(
            doubleArrayOf(TableLayout.FILL),
            doubleArrayOf(
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED
            )
        )
        container.add(actionPanel, "0, 0")
        container.add(scrollPane, "0, 1")
        container.add(hintComponent, "0, 2")

        table.rowSelectionAllowed = true
        table.tableHeader.reorderingAllowed = false
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.model = BaseTableModel(emptyList())

        add(container)
    }

    private fun configureColumnHeaderRenderer() {
        table.tableHeader.defaultRenderer =
            CustomActionInjectedCellRenderer(table.tableHeader.defaultRenderer) { table, component, _, _, column ->
                val columnEditable =
                    isEditing && (0 until table.rowCount).map { table.isCellEditable(it, column) }.any { it }
                component.isEnabled = columnEditable
                component.toolTipText = columnParams[column].tooltip
            }
    }

    private fun configureColumnEditor() {
        columnParams.forEachIndexed { index, columnParam ->
            if (columnParam.columnClass == java.lang.String::class.java
                || columnParam.columnClass == java.lang.Integer::class.java
                || columnParam.columnClass == java.lang.Double::class.java
            ) {
                table.columnModel.getColumn(index).cellEditor =
                    StringCellEditor(columnParam.editorVerifier, columnParam.columnClass, columnParam.tooltip)
            }

            if (columnParam.hide) {
                hideColumn(table.columnModel.getColumn(index))
            }
        }
    }

    protected fun getDarkThemeAwareColor(current: DarkThemeAwareColor, new: Color): DarkThemeAwareColor {
        if (isNightMode) {
            return DarkThemeAwareColor(current.dayColor, new)
        } else {
            return DarkThemeAwareColor(new, current.nightColor)
        }
    }

    protected fun getCurrentColor(current: DarkThemeAwareColor): Color {
        return if (isNightMode) {
            current.nightColor
        } else {
            current.dayColor
        }
    }

    private fun hideColumn(tableColumn: TableColumn) {
        tableColumn.maxWidth = 0
        tableColumn.minWidth = 0
        tableColumn.preferredWidth = 0
        tableColumn.resizable = false
    }

    override fun addOnScrollToEndListener(listener: OnScrollToEndListener) {
        scrollPane.addOnScrollToEndListener(listener)
    }

    override fun isModified(): Boolean {
        return items != originalItems
    }

    private fun handleLoadTemplateAction(logMetadataModel: LogMetadataModel) {
        if (items.isNotEmpty() && !showContentOverrideWarning()) {
            return
        }
        setLogMetadata(
            logMetadataModel.copy(
                isBuiltIn = false,
                logType = logMetadataEditModel.model.logType
            ).toEditModel(isNightMode = logMetadataEditModel.isDarkMode)
        )
        notifyEditDone()
    }

    private fun showContentOverrideWarning(): Boolean {
        val actions = mutableListOf(
            STRINGS.ui.confirmLoadingTemplate to { true },
            STRINGS.ui.cancel to { false }
        )
        return showWarningDialog(
            this,
            EMPTY_STRING,
            STRINGS.ui.loadingTemplateWarning,
            actions
        )
    }

    private fun setItemsToTable(items: List<T>) {
        (table.model as? DefaultTableModel)?.apply {
            dataVector.clear()
            dataVector.addAll(
                items.map { Vector(it.toRowItem()) }.toTypedArray()
            )
            fireTableDataChanged()
        }
    }

    protected open fun customCellEditableCondition(row: Int, column: Int): Boolean {
        return true
    }

    override fun startEditing() {
        actionPanel.startEditing()
        table.tableHeader.repaint()
    }

    override fun stopEditing() {
        table.cellEditor?.cancelCellEditing()
        actionPanel.stopEditing()
        table.tableHeader.repaint()
    }

    override fun addEditEventListener(listener: EditEventListener) {
        table.model.addTableModelListener {
            if (it.type == TableModelEvent.UPDATE && it.lastRow == Int.MAX_VALUE) return@addTableModelListener
            listener.onEditDone(this)
        }
        editEventListeners.add(listener)
    }

    override fun removeEditEventListener(listener: EditEventListener) {
        editEventListeners.remove(listener)
    }

    private fun notifyEditDone() {
        editEventListeners.forEach { it.onEditDone(this) }
    }

    override fun isEditValid(): Boolean {
        return (table.cellEditor as? StringCellEditor)?.isEditValid() ?: true
    }

    private inner class BaseTableModel(items: List<T>) : DefaultTableModel(
        items.map { it.toRowItem().toTypedArray() }.toTypedArray(),
        columnParams.map { it.name }.toTypedArray()
    ) {

        override fun isCellEditable(row: Int, column: Int): Boolean {
            if (column !in columnParams.indices) {
                throw IndexOutOfBoundsException("Column index $column is out of bounds")
            }
            val columnParam = columnParams[column]
            return isEditing && !columnParam.neverEditable && (allColumnsEditable || columnParam.editableWhenBuiltIn)
                    && customCellEditableCondition(row, column)
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            if (columnIndex !in columnParams.indices) {
                throw IndexOutOfBoundsException("Column index $columnIndex is out of bounds")
            }
            return columnParams[columnIndex].columnClass
        }
    }

    protected class ColumnParam(
        val name: String,
        val columnClass: Class<*>,
        val tooltip: String = EMPTY_STRING,
        val editorVerifier: ParamVerifier = ParamVerifier.default,
        val editableWhenBuiltIn: Boolean = false,
        val hide: Boolean = false,
        val neverEditable: Boolean = false,
    )

    private class CustomActionInjectedCellRenderer(
        private val delegate: TableCellRenderer,
        private val action: (JTable, JComponent, Any?, Int, Int) -> Unit
    ) : TableCellRenderer {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            action(table, component as JComponent, value, row, column)
            return component
        }
    }

    private class StringCellEditor(
        verifier: ParamVerifier,
        private val clazz: Class<*>,
        tooltip: String? = null,
    ) : DefaultCellEditor(TableParamEditor(tooltip)) {

        private val editor = component as ParamEditor

        init {
            editor.setVerifier(verifier)
        }

        override fun getCellEditorValue(): Any {
            return when (clazz) {
                java.lang.String::class.java -> editor.text
                java.lang.Integer::class.java -> editor.text.toIntOrNull() ?: 0
                java.lang.Double::class.java -> editor.text.toDoubleOrNull() ?: 0.0
                else -> throw IllegalArgumentException("Unsupported class $clazz")
            }
        }

        override fun stopCellEditing(): Boolean {
            if (editor.isEditValid()) {
                return super.stopCellEditing()
            }
            return false
        }

        fun isEditValid(): Boolean {
            return editor.isEditValid()
        }
    }
}

class ColorRenderer : DefaultTableCellRenderer() {

    init {
        border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, EMPTY_STRING, isSelected, hasFocus, row, column)
        if (value is Color) {
            component.background = value
            component.foreground = value
        }
        return component
    }
}

class ColorEditor : AbstractCellEditor(), TableCellEditor {
    private var currentColor: Color? = null
    private val button = object: JButton() {
        init {
            addActionListener {
                val color = JColorChooser.showDialog(this, STRINGS.ui.colorEditorTitle, currentColor)
                if (color != null) {
                    currentColor = color
                    fireEditingStopped()
                } else {
                    fireEditingCanceled()
                }
            }
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        }

        override fun setText(text: String?) {
            super.setText(EMPTY_STRING)
        }
    }
    override fun getCellEditorValue(): Any? {
        return currentColor
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        currentColor = value as? Color
        button.background = currentColor
        button.foreground = currentColor
        return button
    }
}