package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.LogMetadata
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import kotlin.math.max

interface ILogRenderer {

    fun setColumns(logMetadata: LogMetadata)

    fun getRenderer(columnIndex: Int): TableCellRenderer

    fun getColumn(columnIndex: Int): Column

    fun getColumnCount(): Int

    fun configureColumn(table: LogTable)

    fun showSelectedRowsInDialog(logTable: LogTable, rows: List<Int>, popupActions: List<LogDetailDialog.PopupAction>)
}

class LogRenderer(override val contexts: Contexts = Contexts.default) : ILogRenderer, Context {

    private val renderers = mutableMapOf<Int, TableCellRenderer>()

    private val columns = mutableListOf<Column>()
    private val indexColumn = Column.DefaultColumn(
        COLUMN_INDEX_NAME,
        supportFilter = false,
        uiConf = Column.UIConf(Column.ColumnUIConf(charLen = COLUMN_INDEX_CHAR_LEN))
    )

    private val logCellRendererProvider: LogCellRendererProvider = LabelRendererProvider()

    override fun setColumns(logMetadata: LogMetadata) {
        if (logMetadata.columns.isEmpty()) return

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

    private fun updateColumnCache(columns: List<Column>) {
        this.columns.clear()
        this.columns.addAll(columns)
    }

    private fun updateRendererCache(columns: List<Column>) {
        renderers.clear()
        renderers.putAll(columns.mapIndexed { index, column -> index to createRenderer(column) })
    }

    private fun createRenderer(column: Column): TableCellRenderer {
        return logCellRendererProvider.createRenderer(column)
    }

    override fun getRenderer(columnIndex: Int): TableCellRenderer {
        return renderers[columnIndex]!!
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
            }
        }

        fun addColumns(table: JTable, columns: List<Column>) {
            columns.forEachIndexed { index, column ->
                val tableColumn = TableColumn(index)
                tableColumn.setCellRenderer(renderers[index])
                tableColumn.headerValue = column.name
                table.addColumn(tableColumn)
            }
        }

        fun configureColumnWidth(table: JTable) {
            table.columnModel.columns.toList().forEachIndexed { index, tableColumn ->
                val headerWidth = table.tableHeader.columnModel.getColumn(index).preferredWidth
                val expectLen = columns[index].uiConf.column.charLen
                val fontMetrics = table.getFontMetrics(table.font)
                val expectRowWidth = fontMetrics.charWidth('A') * expectLen
                val targetWidth = max(headerWidth, expectRowWidth)
                tableColumn.minWidth = targetWidth
            }
        }

        removeAllColumns(table)
        addColumns(table, columns)
        configureColumnWidth(table)
    }

    override fun showSelectedRowsInDialog(logTable: LogTable, rows: List<Int>, popupActions: List<LogDetailDialog.PopupAction>) {
        logCellRendererProvider.showSelectedRowsInDialog(logTable, rows, popupActions)
    }

    companion object {
        private const val COLUMN_INDEX_NAME = "Index"
        private const val COLUMN_INDEX_CHAR_LEN = 7
    }
}