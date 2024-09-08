package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.getUniqueName
import me.gegenbauer.catspy.log.serialize.ColumnModel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.IdGenerator
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.table.DefaultTableModel
import javax.swing.text.JTextComponent

class ColumnsEditPanel : BaseEditableTablePanel<ColumnModel>() {

    override val tableName: String = STRINGS.ui.tableColumnInfo

    override val hint: String
        get() = STRINGS.ui.tableColumnInfoHint

    override val columnParams: List<ColumnParam> = listOf(
        ColumnParam(
            STRINGS.ui.columnName,
            java.lang.String::class.java,
            tooltip = STRINGS.toolTip.columnName,
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
            .addCellEditorListener(
                SingleSelectionColumnHandler(
                    setOf(INDEX_COLUMN_IS_MESSAGE, INDEX_COLUMN_IS_LEVEL),
                    setOf(INDEX_COLUMN_IS_MESSAGE, INDEX_COLUMN_IS_LEVEL)
                )
            )
    }

    override fun getUpdatedItems(): List<ColumnModel> {
        val tableModel = table.model as DefaultTableModel
        val columns = mutableListOf<ColumnModel>()

        for (i in 0 until tableModel.rowCount) {
            val name = tableModel.getValueAt(i, INDEX_COLUMN_NAME) as String
            val supportFilter = tableModel.getValueAt(i, INDEX_COLUMN_SUPPORT_FILTER) as Boolean
            val charLen = tableModel.getValueAt(i, INDEX_COLUMN_CHAR_LEN) as Int
            val isHidden = !(tableModel.getValueAt(i, INDEX_COLUMN_SHOULD_SHOW) as Boolean)
            val isMessage = tableModel.getValueAt(i, INDEX_COLUMN_IS_MESSAGE) as Boolean
            val isLevel = tableModel.getValueAt(i, INDEX_COLUMN_IS_LEVEL) as Boolean
            val index = tableModel.getValueAt(i, INDEX_COLUMN_INDEX) as Int
            val id = tableModel.getValueAt(i, INDEX_COLUMN_ID) as Int
            val isParsed = tableModel.getValueAt(i, INDEX_COLUMN_IS_PARSED) as Boolean

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
        val items = items
        return ColumnModel.default.copy(
            id = IdGenerator.generateId(),
            partIndex = items.size,
            uiConf = ColumnModel.UIConf(
                ColumnModel.ColumnUIConf(items.size, DEFAULT_MAX_CHAR_LEN, false),
                ColumnModel.FilterUIConf.default
            ),
            name = getUniqueName(DEFAULT_COLUMN_NAME, items.map { it.name }.toSet())
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

    override fun isModified(): Boolean {
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
        val index = model.getValueAt(selectedRow, INDEX_COLUMN_INDEX) as Int
        val newIndex = (index + if (toLeftOrRight) -1 else 1)
            .coerceIn(0, model.rowCount - 1)
        model.setValueAt(newIndex, selectedRow, INDEX_COLUMN_INDEX)
        for (i in 0 until model.rowCount) {
            if (i != selectedRow) {
                val otherIndex = model.getValueAt(i, INDEX_COLUMN_INDEX) as Int
                if (otherIndex == newIndex) {
                    model.setValueAt(index, i, INDEX_COLUMN_INDEX)
                }
            }
        }
        table.setRowSelectionInterval(selectedRow, selectedRow)
    }

    private fun isNameAlreadyUsed(name: String): Boolean {
        val model = table.model as DefaultTableModel
        for (i in 0 until model.rowCount) {
            if (model.getValueAt(i, COLUMN_NAME_INDEX) == name) {
                return true
            }
        }
        return false
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

    companion object {
        private const val COLUMN_NAME_INDEX = 0
        private const val DEFAULT_MAX_CHAR_LEN = 10
        private const val DEFAULT_COLUMN_NAME = "NewColumn"
        private const val INDEX_COLUMN_NAME = 0
        private const val INDEX_COLUMN_SUPPORT_FILTER = 1
        private const val INDEX_COLUMN_CHAR_LEN = 2
        private const val INDEX_COLUMN_SHOULD_SHOW = 3
        private const val INDEX_COLUMN_IS_MESSAGE = 4
        private const val INDEX_COLUMN_IS_LEVEL = 5
        private const val INDEX_COLUMN_INDEX = 6
        private const val INDEX_COLUMN_ID = 7
        private const val INDEX_COLUMN_IS_PARSED = 8
    }
}