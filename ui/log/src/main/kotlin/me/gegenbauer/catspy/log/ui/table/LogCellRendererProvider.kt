package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.configuration.LogColorScheme
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.DisplayedLevel
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.datasource.LogItem
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Insets
import javax.swing.border.AbstractBorder
import javax.swing.table.TableCellRenderer

interface LogCellRendererProvider {

    fun setLogMetadata(metadata: LogMetadata)

    fun setLevelPartIndex(index: Int)

    fun createRenderer(column: Column): TableCellRenderer

    fun showSelectedRowsInDialog(
        logTable: LogTable,
        rows: List<Int>,
        popupActions: List<LogDetailDialog.PopupAction>
    )
}

abstract class BaseLogCellRendererProvider : LogCellRendererProvider {
    private val levelTagToLevels = mutableMapOf<String, DisplayedLevel>()
    private val columnFilterIndexes = mutableMapOf<Column, Int>()

    private var levelPartIndex = 0

    private val indexColumn = Column.DefaultColumn(
        COLUMN_INDEX_NAME,
        supportFilter = false,
        uiConf = Column.UIConf(Column.ColumnUIConf(charLen = COLUMN_INDEX_CHAR_LEN))
    )

    override fun setLogMetadata(metadata: LogMetadata) {
        updateLevelCache(metadata)

        val displayedColumns = mutableListOf(indexColumn) +
                metadata.columns
                    .filter { it.uiConf.column.isHidden.not() }
                    .sortedBy { it.uiConf.column.index }

        updateColumnFilterIndexesCache(displayedColumns)
        levelPartIndex = displayedColumns.firstOrNull { it is Column.LevelColumn }?.partIndex ?: 0
    }

    protected fun getFilterIndex(column: Column): Int {
        return columnFilterIndexes[column] ?: -1
    }

    private fun updateColumnFilterIndexesCache(columns: List<Column>) {
        columnFilterIndexes.clear()
        val filterableColumns = columns
            .sortedBy { it.partIndex }
            .filter { it.supportFilter && it.uiConf.column.isHidden.not() }
        columnFilterIndexes.putAll(filterableColumns.mapIndexed { index, column -> column to index })
    }

    override fun setLevelPartIndex(index: Int) {
        levelPartIndex = index
    }

    private fun updateLevelCache(logMetadata: LogMetadata) {
        levelTagToLevels.clear()
        levelTagToLevels.putAll(logMetadata.levels.map { it.level.tag to it })
    }

    protected fun getLevel(logItem: LogItem): DisplayedLevel {
        return levelTagToLevels[logItem.getPart(levelPartIndex)] ?: levelTagToLevels.values.minBy { it.level.value }
    }

    class LineNumBorder(var color: Color, private val thickness: Int) : AbstractBorder() {
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            if (width > 0) {
                g.color = color
                for (i in 1..thickness) {
                    g.drawLine(width - i, y, width - i, height)
                }
            }
        }

        override fun getBorderInsets(c: Component): Insets {
            return getBorderInsets(c, Insets(0, 0, 0, thickness))
        }

        override fun getBorderInsets(c: Component?, insets: Insets): Insets {
            return insets.apply { set(0, 0, 0, thickness) }
        }

        override fun isBorderOpaque(): Boolean {
            return true
        }
    }

    companion object {
        private const val COLUMN_INDEX_NAME = "Index"
        private const val COLUMN_INDEX_CHAR_LEN = 7

        fun LogTable.getColumnBackground(num: Int, row: Int): Color {
            val context = contexts.getContext(BaseLogMainPanel::class.java) ?: return LogColorScheme.logBG
            val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
            val isRowSelected = tableModel.selectedLogRows.contains(row)
            return if (bookmarkManager.isBookmark(num)) {
                if (isRowSelected) {
                    LogColorScheme.bookmarkSelectedBG
                } else {
                    LogColorScheme.bookmarkBG
                }
            } else if (isRowSelected) {
                LogColorScheme.selectedBG
            } else {
                LogColorScheme.logBG
            }
        }
    }
}
