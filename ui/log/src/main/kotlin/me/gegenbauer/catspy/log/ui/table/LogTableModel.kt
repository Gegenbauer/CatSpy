package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.common.log.FilterItem
import me.gegenbauer.catspy.common.log.FilterItem.Companion.isEmpty
import me.gegenbauer.catspy.common.log.flag
import me.gegenbauer.catspy.common.ui.state.StatefulPanel
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.log.model.LogcatLogItem
import me.gegenbauer.catspy.log.model.LogcatRealTimeFilter
import me.gegenbauer.catspy.log.repo.FilteredLogcatRepository
import me.gegenbauer.catspy.log.repo.FullLogcatRepository
import me.gegenbauer.catspy.log.repo.LogRepository
import me.gegenbauer.catspy.log.task.LogTaskManager
import me.gegenbauer.catspy.log.ui.LogMainUI
import me.gegenbauer.catspy.log.ui.panel.LogPanel
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskListener
import java.util.regex.Pattern
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel

fun interface LogTableModelListener {
    fun tableChanged(event: TableModelEvent)
}

/**
 * TODO 整理任务停止和启动的逻辑
 */
class LogTableModel(
    internal val logRepository: LogRepository,
    override val contexts: Contexts = Contexts.default
) : AbstractTableModel(), LogRepository.LogChangeListener, Context, TaskListener {
    var selectionChanged = false
    var highlightFilterItem: FilterItem = FilterItem.emptyItem
        set(value) {
            field = value.takeIf { it != field }?.also { fireTableDataChanged() } ?: value
        }
    var searchFilterItem: FilterItem = FilterItem.emptyItem

    var searchMatchCase: Boolean = false
        set(value) {
            searchPatternCase = Pattern.CASE_INSENSITIVE.takeIf { !value } ?: 0
            field = value
        }

    val boldTag: Boolean
        get() = getViewModel()?.boldTag?.value ?: false
    val boldPid: Boolean
        get() = getViewModel()?.boldPid?.value ?: false
    val boldTid: Boolean
        get() = getViewModel()?.boldTid?.value ?: false

    val isFiltering: Boolean
        get() = logRepository.isFiltering

    val state = ObservableViewModelProperty(StatefulPanel.State.NORMAL)

    private var searchPatternCase = Pattern.CASE_INSENSITIVE
    private val eventListeners = ArrayList<LogTableModelListener>()

    override fun configureContext(context: Context) {
        super.configureContext(context)
        getViewModel()?.bookmarkMode?.addObserver {
            logRepository.bookmarkMode = it ?: false
        }
        getViewModel()?.fullMode?.addObserver {
            logRepository.fullMode = it ?: false
        }
        logRepository.addLogChangeListener(this)
        val logMainUI = contexts.getContext(LogMainUI::class.java)
        logMainUI?.let {
            val taskManager = ServiceManager.getContextService(logMainUI, LogTaskManager::class.java)
            taskManager.addListener(this)
        }
    }

    override fun onStart(task: Task) {
        if (task is FilteredLogcatRepository.UpdateFilterTask) {
            state.updateValue(StatefulPanel.State.LOADING)
        }
    }

    override fun onCancel(task: Task) {
        if (task is FilteredLogcatRepository.UpdateFilterTask) {
            state.updateValue(StatefulPanel.State.NORMAL)
        }
    }

    override fun onError(task: Task, t: Throwable) {
        if (task is FilteredLogcatRepository.UpdateFilterTask) {
            state.updateValue(StatefulPanel.State.NORMAL)
        }
    }

    override fun onFinalResult(task: Task, data: Any) {
        if (task is FilteredLogcatRepository.UpdateFilterTask) {
            state.updateValue(StatefulPanel.State.NORMAL)
        }
    }

    private fun getViewModel(): LogPanel.LogPanelViewModel? {
        return contexts.getContext(LogPanel::class.java)?.viewModel
    }

    fun addLogTableModelListener(eventListener: LogTableModelListener) {
        eventListeners.add(eventListener)
    }

    override fun fireTableChanged(e: TableModelEvent) {
        super.fireTableChanged(e)
        eventListeners.forEach { it.tableChanged(e) }
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
                return@accessLogItems when (columnIndex) {
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

    private fun moveToSearch(isNext: Boolean) {
        if (searchFilterItem.isEmpty()) return

        val selectedRow = logRepository.selectedRow
        val mainUI = contexts.getContext(LogMainUI::class.java)
        mainUI ?: return

        val targetRow = selectedRow.run { if (isNext) this + 1 else this - 1 }
        val shouldReturn = targetRow.run { if (isNext) this >= rowCount - 1 else this < 0 }

        if (shouldReturn) {
            mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
            return
        }

        val idxFound = if (isNext) {
            (targetRow until rowCount).firstOrNull {
                searchFilterItem.positiveFilter.matcher(getItem(it).logLine).find()
            } ?: -1
        } else {
            (targetRow downTo 0).firstOrNull {
                searchFilterItem.positiveFilter.matcher(getItem(it).logLine).find()
            } ?: -1
        }

        if (idxFound >= 0) {
            val logPanel =
                if (logRepository is FullLogcatRepository) mainUI.splitLogPane.fullLogPanel else mainUI.splitLogPane.filteredLogPanel
            logPanel.goToRow(idxFound, 0)
        } else {
            mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
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

    fun getRowIndex(lineNumber: Int): Int {
        return (0 until rowCount).map { it to getItem(it) }.firstOrNull { it.second.num >= lineNumber }?.first ?: 0
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
