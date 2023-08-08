package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.common.log.FilterItem
import me.gegenbauer.catspy.common.log.FilterItem.Companion.isEmpty
import me.gegenbauer.catspy.common.log.flag
import me.gegenbauer.catspy.common.ui.state.StatefulPanel
import me.gegenbauer.catspy.common.ui.table.PageMetadata
import me.gegenbauer.catspy.common.ui.table.Pageable
import me.gegenbauer.catspy.common.ui.table.Searchable
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.log.model.LogcatLogItem
import me.gegenbauer.catspy.log.model.LogcatRealTimeFilter
import me.gegenbauer.catspy.log.repo.FilteredLogcatRepository
import me.gegenbauer.catspy.log.repo.LogRepository
import me.gegenbauer.catspy.log.task.LogTaskManager
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.log.ui.panel.LogPanel
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskListener
import java.util.Collections
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel

fun interface LogTableModelListener {
    fun onLogDataChanged(event: TableModelEvent)
}

/**
 * TODO 整理任务停止和启动的逻辑
 */
class LogTableModel(
    internal val logRepository: LogRepository,
    override val contexts: Contexts = Contexts.default
) : AbstractTableModel(), LogRepository.LogChangeListener, Context, TaskListener, Searchable, Pageable<LogcatLogItem> {
    var highlightFilterItem: FilterItem = FilterItem.emptyItem
        set(value) {
            field = value.takeIf { it != field }?.also { contexts.getContext(LogTable::class.java)?.updateUI() } ?: value
        }
    override var searchFilterItem: FilterItem = FilterItem.emptyItem

    override var searchMatchCase: Boolean = false

    override val observablePageMetaData: ObservableViewModelProperty<PageMetadata> = ObservableViewModelProperty()

    override val pageSize: Int
        get() = DEFAULT_PAGE_SIZE

    val boldTag: Boolean
        get() = getViewModel()?.boldTag?.value ?: false
    val boldPid: Boolean
        get() = getViewModel()?.boldPid?.value ?: false
    val boldTid: Boolean
        get() = getViewModel()?.boldTid?.value ?: false

    val state = ObservableViewModelProperty(StatefulPanel.State.NORMAL)

    private val eventListeners = Collections.synchronizedList(ArrayList<LogTableModelListener>())

    override fun configureContext(context: Context) {
        super.configureContext(context)
        getViewModel()?.bookmarkMode?.addObserver {
            logRepository.bookmarkMode = it ?: false
        }
        getViewModel()?.fullMode?.addObserver {
            logRepository.fullMode = it ?: false
        }
        logRepository.addLogChangeListener(this)
        val logMainUI = contexts.getContext(LogTabPanel::class.java)
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
        eventListeners.toList().forEach { it.onLogDataChanged(e) }
    }

    override fun getRowCount(): Int {
        return minOf(accessPageData(currentPage) { it.size }, pageSize)
    }

    override fun getColumnCount(): Int {
        return columns.size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return accessPageData(currentPage) { logItems ->
            if (rowIndex in 0 until rowCount) {
                val logItem = logItems[rowIndex]
                return@accessPageData when (columnIndex) {
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

            return@accessPageData -1
        }
    }

    override fun getColumnName(column: Int): String {
        return columns[column].name
    }

    override fun moveToNextSearchResult() {
        moveToSearch(true)
    }

    override fun moveToPreviousSearchResult() {
        moveToSearch(false)
    }

    private fun moveToSearch(isNext: Boolean) {
        if (searchFilterItem.isEmpty()) return

        val selectedRow = logRepository.selectedRow
        val mainUI = contexts.getContext(LogTabPanel::class.java)
        mainUI ?: return
        val table = contexts.getContext(LogTable::class.java)
        table ?: return

        val targetRow = selectedRow.run { if (isNext) this + 1 else this - 1 }
        val shouldReturn = targetRow.run { if (isNext) this >= rowCount - 1 else this < 0 }

        if (shouldReturn) {
            mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
            return
        }

        val idxFound = if (isNext) {
            (targetRow until rowCount).firstOrNull {
                searchFilterItem.positiveFilter.matcher(getItemInCurrentPage(it).logLine).find()
            } ?: -1
        } else {
            (targetRow downTo 0).firstOrNull {
                searchFilterItem.positiveFilter.matcher(getItemInCurrentPage(it).logLine).find()
            } ?: -1
        }

        if (idxFound >= 0) {
            table.moveToRow(idxFound)
        } else {
            mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
        }
    }

    fun getItemInCurrentPage(row: Int): LogcatLogItem {
        return accessPageData(currentPage) { logItems ->
            logItems[row]
        }
    }

    fun getItemAcrossPage(row: Int): LogcatLogItem {
        return logRepository.accessLogItems {
            it[row]
        }
    }

    fun getLogFilter(): LogcatRealTimeFilter {
        return (logRepository.logFilter as LogcatRealTimeFilter)
    }

    override fun onLogChanged(event: LogRepository.LogChangeEvent) {
        val mainUI = contexts.getContext(LogTabPanel::class.java)
        mainUI ?: return

        recalculatePage() // 在表格 UI 更新之前重新计算页数信息，因为 UI 更新需要用到页数信息

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
                } else {
                    fireTableRowsDeleted(event.startRow, event.endRow)
                }
            }
        }
    }

    private fun recalculatePage() {
        val pageCount = (logRepository.getLogCount() + pageSize - 1) / pageSize
        val currentPage = minOf(currentPage, pageCount - 1).coerceAtLeast(0)
        val dataSize = logRepository.getLogCount()

        observablePageMetaData.updateValue(PageMetadata(currentPage, pageCount, pageSize, dataSize))
    }

    fun getRowIndex(lineNumber: Int): Int {
        return (0 until rowCount).map { it to getItemInCurrentPage(it) }.firstOrNull { it.second.num >= lineNumber }?.first ?: 0
    }

    override fun <R> accessPageData(page: Int, action: (List<LogcatLogItem>) -> R): R {
        assert(page in 0 until pageCount)
        val start = page * pageSize
        val end = minOf((page + 1) * pageSize, logRepository.getLogCount())
        return logRepository.accessLogItems { logItems ->
            runCatching {
                action(logItems.subList(start, end))
            }.getOrElse {
                GLog.e(TAG, "[accessPageData] error", it)
                action(emptyList())
            }
        }
    }

    override fun nextPage() {
        if (currentPage >= pageCount - 1) return
        observablePageMetaData.updateValue(PageMetadata(currentPage + 1, pageCount, pageSize, dataSize))
        fireTableDataChanged()
    }

    override fun previousPage() {
        if (currentPage <= 0) return
        observablePageMetaData.updateValue(PageMetadata(currentPage - 1, pageCount, pageSize, dataSize))
        fireTableDataChanged()
    }

    override fun firstPage() {
        observablePageMetaData.updateValue(PageMetadata(0, pageCount, pageSize, dataSize))
        fireTableDataChanged()
    }

    override fun lastPage() {
        observablePageMetaData.updateValue(PageMetadata(pageCount - 1, pageCount, pageSize, dataSize))
        fireTableDataChanged()
    }

    override fun gotoPage(page: Int) {
        if (page < 0 || page >= pageCount) return
        if (currentPage != page) {
            observablePageMetaData.updateValue(PageMetadata(page, pageCount, pageSize, dataSize))
            fireTableDataChanged()
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
        private const val DEFAULT_PAGE_SIZE = 10 * 10000
    }

}
