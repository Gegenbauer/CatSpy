package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.utils.persistence.Preferences
import java.awt.FontMetrics
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JTable
import javax.swing.table.TableColumn

interface ILogRenderer {

    fun setColumns(logMetadata: LogMetadata)

    fun getColumn(columnIndex: Int): Column

    fun getColumnCount(): Int

    fun configureColumn(table: LogTable)

    fun showSelectedRowsInDialog(logTable: LogTable, rows: List<Int>, popupActions: List<LogDetailDialog.PopupAction>)
}

class LogRenderer(override val contexts: Contexts = Contexts.default) : ILogRenderer, Context, PropertyChangeListener {

    private val renderers = mutableMapOf<Int, LogCellRenderer>()

    private val columns = mutableListOf<Column>()
    private val indexColumn = Column.DefaultColumn(
        COLUMN_INDEX_NAME,
        supportFilter = false,
        uiConf = Column.UIConf(Column.ColumnUIConf(charLen = COLUMN_INDEX_CHAR_LEN))
    )
    private var logType: String = EMPTY_STRING

    private val logCellRendererProvider = LabelRendererProvider()
    private val columnWidthManager = ColumnWidthManager()

    override fun setColumns(logMetadata: LogMetadata) {
        if (logMetadata.columns.isEmpty()) return

        logType = logMetadata.logType
        val displayedColumns = mutableListOf(indexColumn) +
                logMetadata.columns
                    .filter { it.uiConf.column.isHidden.not() }
                    .sortedBy { it.uiConf.column.index }
        updateColumnCache(displayedColumns)

        logCellRendererProvider.setLogMetadata(logMetadata)
        val levelPartIndex = displayedColumns.firstOrNull { it is Column.LevelColumn }?.partIndex ?: 0
        logCellRendererProvider.setLevelPartIndex(levelPartIndex)

        updateRendererCache(displayedColumns)
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == PROPERTY_WIDTH) {
            val newWidth = evt.newValue as Int
            val column = evt.source as TableColumn
            val renderer = renderers[column.modelIndex]
            renderer ?: return
            val logTable = renderer.logTable ?: return
            val maxLength = getMaxLength(newWidth, logTable.getFontMetrics(logTable.font))
            renderer.maxLength = maxLength
            columnWidthManager.setMaxLength(logType, columns[column.modelIndex], maxLength)
        }
    }

    private fun getMaxLength(width: Int, fontMetrics: FontMetrics): Int {
        return width / fontMetrics.charWidth(BASE_CHAR)
    }

    private fun updateColumnCache(columns: List<Column>) {
        this.columns.clear()
        this.columns.addAll(columns)
    }

    private fun updateRendererCache(columns: List<Column>) {
        renderers.clear()
        renderers.putAll(columns.mapIndexed { index, column -> index to createRenderer(column) })
    }

    private fun createRenderer(column: Column): LogCellRenderer {
        return logCellRendererProvider.createRenderer(column)
    }

    override fun getColumn(columnIndex: Int): Column {
        return columns[columnIndex]
    }

    override fun getColumnCount(): Int {
        return columns.size
    }

    override fun configureColumn(table: LogTable) {
        if (columns.isEmpty()) return

        fun removeAllColumns(table: JTable) {
            table.columnModel.columns.toList().forEach {
                table.removeColumn(it)
                it.removePropertyChangeListener(this)
            }
        }

        fun addColumns(table: JTable, columns: List<Column>) {
            columns.forEachIndexed { index, column ->
                val tableColumn = TableColumn(index)
                tableColumn.addPropertyChangeListener(this)
                val renderer = renderers[index] as LogCellRenderer
                renderer.logTable = table as LogTable
                tableColumn.setCellRenderer(renderer)
                if (column !is Column.LevelColumn) {
                    tableColumn.headerValue = column.name
                } else {
                    tableColumn.headerValue = EMPTY_STRING
                }
                table.addColumn(tableColumn)
            }
        }

        fun measureColumnWidth(table: JTable, length: Int): Int {
            val fontMetrics = table.getFontMetrics(table.font)
            return fontMetrics.charWidth(BASE_CHAR) * length
        }

        fun configureColumnWidth(table: JTable) {
            table.columnModel.columns.toList().forEachIndexed { index, tableColumn ->
                if (columns[index] is Column.LevelColumn) {
                    val expectColumnWidth = measureColumnWidth(table, 3)
                    tableColumn.minWidth = expectColumnWidth
                    tableColumn.preferredWidth = expectColumnWidth
                    tableColumn.maxWidth = expectColumnWidth
                    return@forEachIndexed
                }
                val expectLen = columnWidthManager.getMaxLength(
                    logType, columns[index], columns[index].uiConf.column.charLen
                )
                val expectColumnWidth = measureColumnWidth(table, expectLen)
                tableColumn.preferredWidth = expectColumnWidth
            }
        }

        removeAllColumns(table)
        addColumns(table, columns)
        configureColumnWidth(table)
    }

    override fun showSelectedRowsInDialog(
        logTable: LogTable,
        rows: List<Int>,
        popupActions: List<LogDetailDialog.PopupAction>
    ) {
        logCellRendererProvider.showSelectedRowsInDialog(logTable, rows, popupActions)
    }

    private class ColumnWidthManager {
        fun setMaxLength(logType: String, column: Column, maxLength: Int) {
            val key = getKey(logType, column)
            Preferences.putInt(key, maxLength)
        }

        fun getMaxLength(logType: String, column: Column, default: Int): Int {
            val key = getKey(logType, column)
            return Preferences.getInt(key, default)
        }

        private fun getKey(logType: String, column: Column): String {
            return "$PREFERENCE_NAME/${KEY_PREFIX}_${logType.hashCode()}_${column.name}"
        }

        companion object {
            private const val PREFERENCE_NAME = "columnMaxLength"
            // key start with integer is not allowed in xml
            private const val KEY_PREFIX = "len"
        }
    }

    companion object {
        private const val COLUMN_INDEX_NAME = "Index"
        private const val COLUMN_INDEX_CHAR_LEN = 7
        private const val PROPERTY_WIDTH = "width"
        private const val BASE_CHAR = 'a'
    }
}