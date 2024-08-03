package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.log.serialize.ColumnModel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.IdGenerator
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.table.DefaultTableModel

class ColumnsEditPanel : BaseEditableTablePanel<ColumnModel>() {

    override val tableName: String = STRINGS.ui.tableColumnInfo

    override val hint: String
        get() = STRINGS.ui.tableColumnInfoHint

    override val columnParams: List<ColumnParam> = listOf(
        ColumnParam(
            STRINGS.ui.columnName,
            java.lang.String::class.java,
            tooltip = STRINGS.toolTip.columnName,
            editorVerifier = NameVerifier()
        ),
        ColumnParam(
            STRINGS.ui.supportFilter,
            java.lang.Boolean::class.java,
            tooltip = STRINGS.toolTip.supportFilter,
            editableWhenBuiltIn = true,
        ),
        ColumnParam(
            STRINGS.ui.charLength,
            java.lang.Integer::class.java,
            tooltip = STRINGS.toolTip.charLength,
            editableWhenBuiltIn = true,
            editorVerifier = IntVerifier(1, 1000)
        ),
        ColumnParam(
            STRINGS.ui.columnShouldShow,
            java.lang.Boolean::class.java,
            tooltip = STRINGS.toolTip.columnShouldShow,
            editableWhenBuiltIn = true
        ),
        ColumnParam(
            STRINGS.ui.isMessageColumn,
            java.lang.Boolean::class.java,
            tooltip = STRINGS.toolTip.isMessageColumn,
        ),
        ColumnParam(
            STRINGS.ui.isLevelColumn,
            java.lang.Boolean::class.java,
            tooltip = STRINGS.toolTip.isLevelColumn,
        ),
        ColumnParam(
            STRINGS.ui.columnIndex,
            java.lang.Integer::class.java,
            tooltip = STRINGS.toolTip.columnIndex,
            neverEditable = true
        ),
        ColumnParam("Id", java.lang.Integer::class.java, hide = true),
        ColumnParam("isParsed", java.lang.Boolean::class.java, hide = true),
    )

    override val customActions: List<EditableTableActionPanel.CustomAction> = listOf(
        EditableTableActionPanel.CustomAction(STRINGS.ui.moveColumnToLeft) {
            moveSelectedColumn(true)
        },
        EditableTableActionPanel.CustomAction(STRINGS.ui.moveColumnToRight) {
            moveSelectedColumn(false)
        }
    )

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        super.setLogMetadata(metadata)
        items = metadata.model.columns
    }

    override fun configure() {
        super.configure()
        table
            .getDefaultEditor(java.lang.Boolean::class.java)
            .addCellEditorListener(SingleSelectionColumnHandler(setOf(4, 5), setOf(4, 5)))
    }

    override fun getUpdatedItems(): List<ColumnModel> {
        val tableModel = table.model as DefaultTableModel
        val columns = mutableListOf<ColumnModel>()

        for (i in 0 until tableModel.rowCount) {
            val name = tableModel.getValueAt(i, 0) as String
            val supportFilter = tableModel.getValueAt(i, 1) as Boolean
            val charLen = tableModel.getValueAt(i, 2) as Int
            val isHidden = !(tableModel.getValueAt(i, 3) as Boolean)
            val isMessage = tableModel.getValueAt(i, 4) as Boolean
            val isLevel = tableModel.getValueAt(i, 5) as Boolean
            val index = tableModel.getValueAt(i, 6) as Int
            val id = tableModel.getValueAt(i, 7) as Int
            val isParsed = tableModel.getValueAt(i, 8) as Boolean

            val columnUIConf = ColumnModel.ColumnUIConf(index, charLen, isHidden)
            val filterUIConf = ColumnModel.FilterUIConf.default
            val column = ColumnModel(
                name,
                supportFilter,
                isParsed,
                ColumnModel.UIConf(columnUIConf, filterUIConf),
                i,
                isMessage,
                isLevel,
                id
            )

            columns.add(column)
        }

        return columns
    }

    override fun createNewItem(): ColumnModel {
        return ColumnModel.default.copy(
            id = IdGenerator.generateId(),
            partIndex = items.size,
            uiConf = ColumnModel.UIConf(
                ColumnModel.ColumnUIConf(items.size, 10, false),
                ColumnModel.FilterUIConf.default
            ),
            name = "New Column"
        )
    }

    override fun ColumnModel.toRowItem(): List<Any> {
        return listOf(
            name,
            supportFilter,
            uiConf.column.charLen,
            uiConf.column.isHidden.not(),
            isMessage,
            isLevel,
            uiConf.column.index,
            id,
            isParsed
        )
    }

    override fun isContentModified(): Boolean {
        val currentItems = items
        if (originalItems.size != currentItems.size) return true
        for (original in originalItems) {
            val current = currentItems.find { it.id == original.id } ?: return true
            if (original != current) {
                return current.copy(uiConf = current.uiConf.copy(filter = original.uiConf.filter)) != original
            }
        }

        return false
    }

    private fun moveSelectedColumn(toLeftOrRight: Boolean) {
        val selectedRow = table.selectedRow
        if (selectedRow == -1) {
            return
        }

        val model = table.model as DefaultTableModel
        // 修改第 6 列的值，同时再修改其他行该列的值，确保无重复
        val index = model.getValueAt(selectedRow, 6) as Int
        val newIndex = (index + if (toLeftOrRight) -1 else 1)
            .coerceIn(0, model.rowCount - 1)
        model.setValueAt(newIndex, selectedRow, 6)
        for (i in 0 until model.rowCount) {
            if (i != selectedRow) {
                val otherIndex = model.getValueAt(i, 6) as Int
                if (otherIndex == newIndex) {
                    model.setValueAt(index, i, 6)
                }
            }
        }
        table.setRowSelectionInterval(selectedRow, selectedRow)
    }

    private inner class SingleSelectionColumnHandler(
        private val booleanColumnIndexes: Set<Int>,
        private val conflictedColumnIndexes: Set<Int>
    ) : CellEditorListener {
        override fun editingStopped(e: ChangeEvent) {
            val table = table
            val model = table.model as DefaultTableModel
            val selectedRow = table.selectedRow
            val selectedColumn = table.selectedColumn

            if (selectedRow == -1 || selectedColumn == -1) {
                return
            }

            if (booleanColumnIndexes.contains(selectedColumn)) {
                for (i in 0 until model.rowCount) {
                    if (i != selectedRow) {
                        model.setValueAt(false, i, selectedColumn)
                    }
                }
            }
            for (i in conflictedColumnIndexes) {
                if (i != selectedColumn) {
                    model.setValueAt(false, selectedRow, i)
                }
            }
        }

        override fun editingCanceled(e: ChangeEvent?) {
            // do nothing
        }
    }
}