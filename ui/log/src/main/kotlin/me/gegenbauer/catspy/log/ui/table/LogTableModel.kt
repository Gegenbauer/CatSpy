package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.datasource.LogViewModel
import me.gegenbauer.catspy.log.flag
import me.gegenbauer.catspy.log.model.LogcatFilter
import me.gegenbauer.catspy.log.model.LogcatItem
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.log.ui.panel.LogPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.isEmpty
import me.gegenbauer.catspy.view.table.PageMetadata
import me.gegenbauer.catspy.view.table.Pageable
import me.gegenbauer.catspy.view.table.Searchable
import java.util.*
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel

fun interface LogTableModelListener {
    fun onLogDataChanged(event: TableModelEvent)
}

open class LogTableModel(
    val viewModel: LogViewModel,
    override val contexts: Contexts = Contexts.default
) : AbstractTableModel(), Context, Searchable, Pageable<LogcatItem>, ILogTableModel {
    override var highlightFilterItem: FilterItem = FilterItem.emptyItem
        set(value) {
            field = value.takeIf { it != field }?.also { contexts.getContext(LogTable::class.java)?.repaint() } ?: value
        }
    override var searchFilterItem: FilterItem = FilterItem.emptyItem
        set(value) {
            field = value.takeIf { it != field }?.also { contexts.getContext(LogTable::class.java)?.repaint() } ?: value
        }

    override var searchMatchCase: Boolean = false

    override val pageMetaData: ObservableViewModelProperty<PageMetadata> = ObservableViewModelProperty()

    override val pageSize: Int
        get() = DEFAULT_PAGE_SIZE

    override val boldTag: Boolean
        get() = getBindings()?.boldTag?.value ?: false
    override val boldPid: Boolean
        get() = getBindings()?.boldPid?.value ?: false
    override val boldTid: Boolean
        get() = getBindings()?.boldTid?.value ?: false
    override var selectedRows: List<Int>
        get() = viewModel.fullTableSelectedRows
        set(value) {
            viewModel.fullTableSelectedRows = value
        }

    protected val scope = ViewModelScope()
    protected var logItems = mutableListOf<LogcatItem>()
    private val eventListeners = Collections.synchronizedList(ArrayList<LogTableModelListener>())

    override fun configureContext(context: Context) {
        super.configureContext(context)
        viewModel.setContexts(contexts)
        collectLogItems()
    }

    protected open fun collectLogItems() {
        scope.launch(Dispatchers.UI) {
            viewModel.fullLogItemsFlow.collect {
                logItems = it.toMutableList()
                fireTableDataChanged()
            }
        }
    }

    private fun getBindings(): LogPanel.LogPanelBinding? {
        return contexts.getContext(LogPanel::class.java)?.binding
    }

    override fun addLogTableModelListener(eventListener: LogTableModelListener) {
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

            -1
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

        val selectedRow = viewModel.fullTableSelectedRows.firstOrNull() ?: -1
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
                searchFilterItem.positiveFilter.matcher(getItemInCurrentPage(it).message).find()
            } ?: -1
        } else {
            (targetRow downTo 0).firstOrNull {
                searchFilterItem.positiveFilter.matcher(getItemInCurrentPage(it).message).find()
            } ?: -1
        }

        if (idxFound >= 0) {
            table.moveToRow(idxFound, true)
        } else {
            mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
        }
    }

    override fun getItemInCurrentPage(row: Int): LogcatItem {
        return accessPageData(currentPage) { it[row] }
    }

    override fun getLogFilter(): LogcatFilter {
        return viewModel.highlightFilter
    }

    override fun fireTableDataChanged() {
        val mainUI = contexts.getContext(LogTabPanel::class.java)
        mainUI ?: return

        recalculatePage() // 在表格 UI 更新之前重新计算页数信息，因为 UI 更新需要用到页数信息
        val logPanel = contexts.getContext(LogPanel::class.java)
        logPanel?.repaint()

        if (logItems.isEmpty()) {
            val bookmarkManager = ServiceManager.getContextService(mainUI, BookmarkManager::class.java)
            bookmarkManager.clear()
            Runtime.getRuntime().gc()
        }
        super.fireTableDataChanged()
    }

    private fun recalculatePage() {
        val pageCount = (logItems.size + pageSize - 1) / pageSize
        val currentPage = minOf(currentPage, pageCount - 1).coerceAtLeast(0)
        val dataSize = logItems.size

        pageMetaData.updateValue(PageMetadata(currentPage, pageCount, pageSize, dataSize))
    }

    override fun getRowIndex(lineNumber: Int): Int {
        return (0 until rowCount).map { it to getItemInCurrentPage(it) }
            .firstOrNull { it.second.num >= lineNumber }?.first ?: 0
    }

    override fun <R> accessPageData(page: Int, action: (List<LogcatItem>) -> R): R {
        val start = page * pageSize
        val end = minOf((page + 1) * pageSize, logItems.size)
        if (start > end) return action(emptyList())
        return action(logItems.subList(start, end))
    }

    override fun nextPage() {
        if (currentPage >= pageCount - 1) return
        pageMetaData.updateValue(PageMetadata(currentPage + 1, pageCount, pageSize, dataSize))
        fireTableDataChanged()
    }

    override fun previousPage() {
        if (currentPage <= 0) return
        pageMetaData.updateValue(PageMetadata(currentPage - 1, pageCount, pageSize, dataSize))
        fireTableDataChanged()
    }

    override fun firstPage() {
        pageMetaData.updateValue(PageMetadata(0, pageCount, pageSize, dataSize))
        fireTableDataChanged()
    }

    override fun lastPage() {
        pageMetaData.updateValue(PageMetadata(pageCount - 1, pageCount, pageSize, dataSize))
        fireTableDataChanged()
    }

    override fun gotoPage(page: Int) {
        if (page < 0 || page >= pageCount) return
        if (currentPage != page) {
            pageMetaData.updateValue(PageMetadata(page, pageCount, pageSize, dataSize))
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
        private const val DEFAULT_PAGE_SIZE = 10 * 10000
    }

}
