package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.DisplayedLevel
import me.gegenbauer.catspy.log.serialize.ColumnModel
import me.gegenbauer.catspy.strings.STRINGS
import java.awt.Component
import java.awt.Toolkit
import javax.swing.DefaultCellEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JTable
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.text.JTextComponent

class FiltersEditPanel : BaseEditableTablePanel<ColumnModel.FilterUIConf>() {

    override val tableName: String = STRINGS.ui.tableFilterInfo

    override val columnParams: List<ColumnParam> = listOf(
        ColumnParam(
            STRINGS.ui.filterName,
            java.lang.String::class.java,
            editableWhenBuiltIn = false,
            tooltip = STRINGS.toolTip.filterName,
            editorVerifier = {
                val name = NameVerifier()
                val nameValidResult = name.verify(it)
                if (nameValidResult.isValid.not()) {
                    return@ColumnParam nameValidResult
                }
                if (isNameAlreadyUsed((it as JTextComponent).text)) {
                    return@ColumnParam ParamVerifier.Result.Invalid(STRINGS.toolTip.nameUsedWarning)
                }
                ParamVerifier.Result.Valid
            }
        ),
        ColumnParam(
            STRINGS.ui.correspondingColumnName,
            java.lang.String::class.java,
            tooltip = STRINGS.toolTip.correspondingColumnName,
            neverEditable = true
        ),
        ColumnParam(
            STRINGS.ui.filterLayoutWidthType,
            java.lang.String::class.java,
            editableWhenBuiltIn = true,
            tooltip = STRINGS.toolTip.filterLayoutWidthType
        ),
        ColumnParam(
            STRINGS.ui.filterWidth,
            java.lang.Double::class.java,
            editableWhenBuiltIn = true,
            tooltip = STRINGS.toolTip.filterWidth,
            editorVerifier = DoubleVerifier(0.0, Toolkit.getDefaultToolkit().screenSize.width.toDouble()),
        ),
        ColumnParam(
            STRINGS.ui.filterRowOrder,
            java.lang.Integer::class.java,
            editableWhenBuiltIn = true,
            tooltip = STRINGS.toolTip.filterRowOrder,
            neverEditable = true
        ),
        ColumnParam("ColumnId", java.lang.Integer::class.java, hide = true)
    )

    override val actionVisibilityParam: EditableTableActionPanel.ActionVisibilityParam =
        EditableTableActionPanel.ActionVisibilityParam(
            moveRowAction = EditableTableActionPanel.StateControlParam(alwaysTrue = true),
            addOrRemoveRowAction = EditableTableActionPanel.StateControlParam(false),
            loadTemplateAction = EditableTableActionPanel.StateControlParam(false)
        )

    override val customActions: List<EditableTableActionPanel.CustomAction>
        get() = listOf(
            EditableTableActionPanel.CustomAction(STRINGS.ui.lowerFilterOrder) { changeRowOrder(false) },
            EditableTableActionPanel.CustomAction(STRINGS.ui.raiseFilterOrder) { changeRowOrder(true) }
        )

    override val hint: String
        get() = STRINGS.ui.filterInfoTableHint

    private var levels: List<DisplayedLevel> = emptyList()

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        super.setLogMetadata(metadata)
        items = metadata.model.columns
            .filter { it.supportFilter && it.uiConf.column.isHidden.not() }
            .map { it.uiConf.filter }
            .sortedBy { it.position.columnIndex }
            .sortedBy { it.position.rowIndex }
        levels = metadata.model.levels
    }

    override fun configure() {
        super.configure()

        val layoutWidthEditor = getLayoutWidthTypeCellEditor()
        table.columnModel.getColumn(COLUMN_INDEX_LAYOUT_WIDTH_TYPE).cellEditor = layoutWidthEditor
        table.columnModel.getColumn(COLUMN_INDEX_LAYOUT_WIDTH_TYPE).cellRenderer = LayoutWidthTypeCellRenderer()
        table.columnModel.getColumn(COLUMN_INDEX_LAYOUT_WIDTH).cellRenderer = LayoutWidthRenderer()

        layoutWidthEditor.addCellEditorListener(LayoutWidthColumnEditorHandler())
    }

    private fun changeRowOrder(isRaise: Boolean) {
        val selectedRow = table.selectedRow
        if (selectedRow == -1) {
            return
        }
        val rowOrderColumnIndex = 4
        val model = table.model as DefaultTableModel
        val selectedRowOrder = model.getValueAt(selectedRow, rowOrderColumnIndex) as Int
        val minRowOrder = model.dataVector.minOfOrNull { it[rowOrderColumnIndex] as Int } ?: 0
        val maxRowOrder = model.dataVector.maxOfOrNull { it[rowOrderColumnIndex] as Int } ?: 0
        val sameOrderCount = model.dataVector.count { it[rowOrderColumnIndex] as Int == selectedRowOrder }
        if (minRowOrder == selectedRowOrder && !isRaise && sameOrderCount == 1) {
            return
        }
        if (maxRowOrder == selectedRowOrder && isRaise && sameOrderCount == 1) {
            return
        }
        val targetRowOrder =
            (if (isRaise) selectedRowOrder + 1 else selectedRowOrder - 1)
                .coerceIn(minRowOrder - 1, maxRowOrder + 1)
                .coerceAtLeast(0)
        model.setValueAt(targetRowOrder, selectedRow, rowOrderColumnIndex)
    }

    private fun getLayoutWidthTypeCellEditor(): TableCellEditor {
        return DefaultCellEditor(
            JComboBox(DefaultComboBoxModel(LayoutWidthType.values().map { it.name }.toTypedArray()))
        )
    }

    /**
     * will not be called for this implementation
     */
    override fun createNewItem(): ColumnModel.FilterUIConf {
        return ColumnModel.FilterUIConf(
            name = "New Filter",
            layoutWidth = Column.LAYOUT_WIDTH_PREFERRED,
            position = Column.FilterPosition(0, 0),
            -1,
            EMPTY_STRING
        )
    }

    override fun getUpdatedItems(): List<ColumnModel.FilterUIConf> {
        val tableModel = table.model as DefaultTableModel
        val items = mutableListOf<ColumnModel.FilterUIConf>()

        val maxColumnIndexOnRow = mutableMapOf<Int, Int>()
        for (i in 0 until tableModel.rowCount) {
            val name = tableModel.getValueAt(i, COLUMN_INDEX_NAME) as String
            val columnName = tableModel.getValueAt(i, COLUMN_INDEX_COLUMN_NAME) as String
            val layoutWidthType =
                getLayoutWidthTypeWithName(tableModel.getValueAt(i, COLUMN_INDEX_LAYOUT_WIDTH_TYPE) as String)
            val layoutWidthValue = (tableModel.getValueAt(i, COLUMN_INDEX_LAYOUT_WIDTH) as? Double) ?: 0.0
            val rowOrder = tableModel.getValueAt(i, COLUMN_INDEX_ROW_ORDER) as Int
            val columnId = tableModel.getValueAt(i, COLUMN_INDEX_COLUMN_ID) as Int
            val columnIndex = if (maxColumnIndexOnRow.containsKey(rowOrder)) {
                maxColumnIndexOnRow[rowOrder] = maxColumnIndexOnRow[rowOrder]!! + 1
                maxColumnIndexOnRow[rowOrder]!!
            } else {
                maxColumnIndexOnRow[rowOrder] = 0
                0
            }
            items.add(
                ColumnModel.FilterUIConf(
                    name = name,
                    layoutWidth = layoutWidthType.toWidth(layoutWidthValue),
                    position = Column.FilterPosition(columnIndex, rowOrder),
                    columnId,
                    columnName
                )
            )
        }
        return items
    }

    private fun getLayoutWidthTypeWithName(name: String): LayoutWidthType {
        return LayoutWidthType.values().first { it.name == name }
    }

    override fun customCellEditableCondition(row: Int, column: Int): Boolean {
        if (column != COLUMN_INDEX_LAYOUT_WIDTH) return true
        val layoutWidthType =
            getLayoutWidthTypeWithName(table.getValueAt(row, COLUMN_INDEX_LAYOUT_WIDTH_TYPE) as String)
        return layoutWidthType == LayoutWidthType.Specified
    }

    override fun ColumnModel.FilterUIConf.toRowItem(): List<Any?> {
        val layoutWidthType = layoutWidth.widthToType()
        val displayedWidth = if (layoutWidthType == LayoutWidthType.Specified) layoutWidth else null
        return listOf(name, columnName, layoutWidthType.name, displayedWidth, position.rowIndex, columnId)
    }

    private fun Double.widthToType(): LayoutWidthType {
        return when (this) {
            Column.LAYOUT_WIDTH_PREFERRED -> LayoutWidthType.Preferred
            Column.LAYOUT_WIDTH_FILL -> LayoutWidthType.Fill
            else -> LayoutWidthType.Specified
        }
    }

    private fun LayoutWidthType.toWidth(value: Double): Double {
        if (this == LayoutWidthType.Specified) {
            return value
        }
        return this.value
    }

    private fun isNameAlreadyUsed(name: String): Boolean {
        val model = table.model as DefaultTableModel
        for (i in 0 until model.rowCount) {
            if (i == table.selectedRow) {
                continue
            }
            if (model.getValueAt(i, COLUMN_INDEX_NAME) == name) {
                return true
            }
        }
        return false
    }

    private enum class LayoutWidthType(val value: Double) {
        Preferred(Column.LAYOUT_WIDTH_PREFERRED), Fill(Column.LAYOUT_WIDTH_FILL), Specified(0.0)
    }

    private class LayoutWidthTypeCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (value is LayoutWidthType) {
                text = value.name
            }
            return component
        }
    }

    private class LayoutWidthRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (value == null || value as? Double == 0.0) {
                text = EMPTY_STRING
            } else {
                text = value.toString()
            }
            return component
        }
    }

    private inner class LayoutWidthColumnEditorHandler : CellEditorListener {
        override fun editingStopped(e: ChangeEvent) {
            val table = table
            val model = table.model as DefaultTableModel
            val selectedRow = table.selectedRow
            val selectedColumn = table.selectedColumn

            if (selectedRow == -1 || selectedColumn == -1) {
                return
            }

            if (selectedColumn == COLUMN_INDEX_LAYOUT_WIDTH_TYPE) {
                val layoutWidthType = model.getValueAt(selectedRow, COLUMN_INDEX_LAYOUT_WIDTH_TYPE) as String
                if (layoutWidthType != LayoutWidthType.Specified.name) {
                    model.setValueAt(0.0, selectedRow, COLUMN_INDEX_LAYOUT_WIDTH)
                }
            }
        }

        override fun editingCanceled(e: ChangeEvent?) {
        }
    }

    companion object {
        private const val TAG = "FiltersPanel"

        private const val COLUMN_INDEX_NAME = 0
        private const val COLUMN_INDEX_COLUMN_NAME = 1
        private const val COLUMN_INDEX_LAYOUT_WIDTH_TYPE = 2
        private const val COLUMN_INDEX_LAYOUT_WIDTH = 3
        private const val COLUMN_INDEX_ROW_ORDER = 4
        private const val COLUMN_INDEX_COLUMN_ID = 5
    }
}