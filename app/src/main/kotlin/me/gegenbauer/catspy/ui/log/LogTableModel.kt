package me.gegenbauer.catspy.ui.log

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.data.model.log.FilterItem
import me.gegenbauer.catspy.data.model.log.LogcatLogItem
import me.gegenbauer.catspy.data.model.log.LogcatRealTimeFilter
import me.gegenbauer.catspy.data.model.log.flag
import me.gegenbauer.catspy.data.repo.log.FullLogcatRepository
import me.gegenbauer.catspy.data.repo.log.LogRepository
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import java.util.regex.Pattern
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel


// TODO refactor

fun interface LogTableModelListener {
    fun tableChanged(event: TableModelEvent)
}

/**
 * TODO 整理任务停止和启动的逻辑
 */
class LogTableModel(
    private val logRepository: LogRepository,
    override val contexts: Contexts = Contexts.default
) : AbstractTableModel(), LogRepository.LogChangeListener, Context {

    private val eventListeners = ArrayList<LogTableModelListener>()

    var selectionChanged = false

    var highlightFilterItem: FilterItem = FilterItem.emptyItem
        set(value) {
            if (field != value) {
                field = value
                fireTableDataChanged()
            }
        }
    var searchFilterItem: FilterItem = FilterItem.emptyItem

    private var searchPatternCase = Pattern.CASE_INSENSITIVE
    var searchMatchCase: Boolean = false
        set(value) {
            if (field != value) {
                searchPatternCase = if (!value) {
                    Pattern.CASE_INSENSITIVE
                } else {
                    0
                }
            }
            field = value
        }

    var boldTag = false
    var boldPid = false
    var boldTid = false
    var bookmarkMode = false
        set(value) {
            field = value
            logRepository.bookmarkMode = value
            if (value) {
                fullMode = false
            }
        }

    var fullMode = false
        set(value) {
            field = value
            logRepository.fullMode = value
            if (value) {
                bookmarkMode = false
            }
        }

    init {
        logRepository.addLogChangeListener(this)
    }

    fun addLogTableModelListener(eventListener: LogTableModelListener) {
        eventListeners.add(eventListener)
    }

    override fun fireTableChanged(e: TableModelEvent) {
        super.fireTableChanged(e)
        for (eventListener in eventListeners) {
            eventListener.tableChanged(e)
        }
    }

    override fun getRowCount(): Int {
        return logRepository.getLogCount()
    }

    override fun getColumnCount(): Int {
        return columns.size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return logRepository.accessLogItems { logItems ->
            if (rowIndex >= 0 && logRepository.getLogCount() > rowIndex) {
                val logItem = logItems[rowIndex]
                return@accessLogItems when(columnIndex) {
                    COLUMN_NUM -> logItem.num
                    COLUMN_TIME -> logItem.time
                    COLUMN_PID -> logItem.pid
                    COLUMN_TID -> logItem.tid
                    COLUMN_LEVEL -> logItem.level.flag
                    COLUMN_TAG -> logItem.tag
                    COLUMN_MESSAGE -> logItem.message
                    else -> ""
                }
            }

            return@accessLogItems -1
        }
    }

    override fun getColumnName(column: Int): String {
        return columns[column].name
    }

    fun moveToNextSearch() {
        moveToSearch(true)
    }

    fun moveToPrevSearch() {
        moveToSearch(false)
    }

    private infix fun Int.toward(to: Int): IntProgression {
        val step = if (this > to) -1 else 1
        return IntProgression.fromClosedRange(this, to, step)
    }

    private fun moveToSearch(isNext: Boolean) {
        val selectedRow = logRepository.selectedRow
        val mainUI = contexts.getContext(LogMainUI::class.java)
        mainUI ?: return
        logRepository.accessLogItems { logItems ->
            var startRow = 0
            var endRow = 0

            if (isNext) {
                startRow = selectedRow + 1
                endRow = logItems.count() - 1
                if (startRow >= endRow) {
                    mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
                    return@accessLogItems
                }
            } else {
                startRow = selectedRow - 1
                endRow = 0

                if (startRow < endRow) {
                    mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
                    return@accessLogItems
                }
            }

            var idxFound = -1
            for (idx in startRow toward endRow) {
                val item = logItems[idx]
                if (searchFilterItem.positiveFilter.matcher(item.logLine).find()) {
                    idxFound = idx
                    break
                }
            }

            if (idxFound >= 0) {
                if (logRepository is FullLogcatRepository) {
                    mainUI.splitLogPane.filteredLogPanel.goToRow(idxFound, 0)
                } else {
                    mainUI.splitLogPane.fullLogPanel.goToRow(idxFound, 0)
                }
            } else {
                mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
            }
        }
    }

    fun getItem(row: Int): LogcatLogItem {
        return logRepository.accessLogItems { logItems ->
            logItems[row]
        }
    }

    fun getLogFilter(): LogcatRealTimeFilter {
        return (logRepository.logFilter as LogcatRealTimeFilter)
    }

    override fun onLogChanged(event: LogRepository.LogChangeEvent) {
        val mainUI = contexts.getContext(LogMainUI::class.java)
        mainUI ?: return
        when (event.type) {
            TableModelEvent.INSERT -> {
                fireTableRowsInserted(event.startRow, event.endRow)
            }

            TableModelEvent.UPDATE -> {
                GLog.d(TAG, "[onLogChanged] UPDATE")
                fireTableRowsUpdated(event.startRow, event.endRow)
            }

            TableModelEvent.DELETE -> {
                if (event.endRow == Int.MAX_VALUE) {
                    val bookmarkManager = ServiceManager.getContextService(mainUI, BookmarkManager::class.java)
                    bookmarkManager.clear()
                    fireTableDataChanged()
                    Runtime.getRuntime().gc()
                }
                fireTableRowsDeleted(event.startRow, event.endRow)
            }
        }
    }

    companion object {
        const val COLUMN_NUM = 0
        const val COLUMN_TIME = 1
        const val COLUMN_PID = 2
        const val COLUMN_TID = 3
        const val COLUMN_LEVEL = 4
        const val COLUMN_TAG = 5
        const val COLUMN_MESSAGE = 6
        private const val TAG = "LogTableModel"
    }

}
