package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.CPU
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.datasource.LogItem
import me.gegenbauer.catspy.log.datasource.LogProducerManager
import me.gegenbauer.catspy.log.datasource.LogViewModel
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.isEmpty
import me.gegenbauer.catspy.view.filter.matches
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
) : AbstractTableModel(), Context, Searchable, Pageable<LogItem>, ILogTableModel {
    override var searchFilterItem: FilterItem = FilterItem.EMPTY_ITEM
        set(value) {
            field = value.takeIf { it != field }?.also { getLogTable()?.repaint() } ?: value
        }

    override val pageMetadata: ObservableValueProperty<PageMetadata> = ObservableValueProperty()

    override val pageSize: Int
        get() = DEFAULT_PAGE_SIZE

    override val pageCount: Int
        get() = _pageMetadata.pageCount

    override val currentPage: Int
        get() = _pageMetadata.currentPage

    override val dataSize: Int
        get() = _pageMetadata.dataSize

    override val selectedLogRows: MutableSet<Int>
        get() = viewModel.fullTableSelectedRows

    override val logObservables: LogProducerManager.LogObservables
        get() = viewModel.fullLogObservables

    private val logConf: LogConfiguration
        get() = contexts.getContext(LogConfiguration::class.java) ?: LogConfiguration.default

    private var _pageMetadata = PageMetadata()
    private val scope = ViewModelScope()
    private var logItems = mutableListOf<LogItem>()
    private val eventListeners = Collections.synchronizedList(ArrayList<LogTableModelListener>())
    private val pageLogCache = mutableMapOf<Int, List<LogItem>>()

    override fun configureContext(context: Context) {
        super.configureContext(context)
        viewModel.setParent(this)
        collectLogItems()
        observeRepaintEvent()
    }

    private fun collectLogItems() {
        scope.launch {
            logObservables.itemsFlow.collect { items ->
                val oldItems = logItems
                logItems = items.toMutableList()
                fireChangeEvent(oldItems, logItems)
            }
        }
    }

    private fun observeRepaintEvent() {
        scope.launch {
            logObservables.repaintEventFlow.collect {
                getLogTable()?.repaint()
            }
        }
    }

    private fun fireChangeEvent(oldItems: List<LogItem>, newItems: List<LogItem>) {
        recalculatePage() // 在表格 UI 更新之前重新计算页数信息，因为 UI 更新需要用到页数信息
        if (oldItems.firstOrNull()?.num != newItems.firstOrNull()?.num) {
            fireTableDataChanged()
            clearSelectedRows()
        } else if (newItems.isEmpty()) {
            if (oldItems.isNotEmpty()) {
                fireTableChanged(
                    TableModelEvent(
                        this,
                        0,
                        oldItems.lastIndex,
                        TableModelEvent.ALL_COLUMNS,
                        TableModelEvent.DELETE
                    )
                )
            }
            clearBookmark()
            clearSelectedRows()
        } else {
            fireTableChanged(
                TableModelEvent(
                    this,
                    oldItems.lastIndex + 1,
                    newItems.lastIndex,
                    TableModelEvent.ALL_COLUMNS,
                    TableModelEvent.INSERT
                )
            )
        }

        pageMetadata.updateValue(_pageMetadata)

        val logPanel = contexts.getContext(LogPanel::class.java)
        logPanel?.repaint()
    }

    private fun clearBookmark() {
        val mainUI = contexts.getContext(BaseLogMainPanel::class.java)
        mainUI ?: return
        val bookmarkManager = ServiceManager.getContextService(mainUI, BookmarkManager::class.java)
        bookmarkManager.clear()
    }

    private fun clearSelectedRows() {
        selectedLogRows.clear()
    }

    private fun getLogTable(): LogTable? {
        return contexts.getContext(LogTable::class.java)
    }

    private fun checkPageCacheValidity(event: Int, start: Int, end: Int) {
        if (event == TableModelEvent.INSERT) {
            val startPage = start / pageSize
            val endPage = end / pageSize
            for (i in startPage..endPage) {
                pageLogCache.remove(i)
            }
        } else {
            pageLogCache.clear()
        }
    }

    override fun addLogTableModelListener(eventListener: LogTableModelListener) {
        eventListeners.add(eventListener)
    }

    override fun fireTableChanged(e: TableModelEvent) {
        checkPageCacheValidity(e.type, e.firstRow, e.lastRow)
        super.fireTableChanged(e)
        eventListeners.toList().forEach { it.onLogDataChanged(e) }
    }

    override fun getRowCount(): Int {
        return minOf(accessPageData(currentPage) { it.size }, pageSize)
    }

    override fun getColumnCount(): Int {
        return logConf.getColumnCount()
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val columnCount = logConf.getColumnCount()
        if (columnIndex >= columnCount) return EMPTY_STRING

        val columnParam = getColumnParam(columnIndex)
        return accessPageData(currentPage) { logItems ->
            if (rowIndex in 0 until rowCount) {
                val logItem = logItems[rowIndex]
                return@accessPageData if (columnIndex == 0) {
                    logItem.num
                } else {
                    logItem.getPart(columnParam.partIndex)
                }
            }
            -1
        }
    }

    private fun getColumnParam(columnIndex: Int): Column {
        return logConf.getColumn(columnIndex)
    }

    override suspend fun moveToNextSearchResult(): String {
        return moveToSearch(true)
    }

    override suspend fun moveToPreviousSearchResult(): String {
        return moveToSearch(false)
    }

    private suspend fun moveToSearch(isNext: Boolean): String {
        if (searchFilterItem.isEmpty()) return EMPTY_STRING
        val items = logItems.takeIf { it.isNotEmpty() } ?: return EMPTY_STRING

        val selectedRow = selectedLogRows.firstOrNull() ?: 0
        val mainUI = contexts.getContext(BaseLogMainPanel::class.java)
        mainUI ?: return EMPTY_STRING
        val table = getLogTable()
        table ?: return EMPTY_STRING

        val minIndex = 0
        val maxIndex = items.lastIndex
        val selectedLogIndex = currentPage * pageSize + selectedRow
        val targetIndex = selectedLogIndex.let { if (isNext) it + 1 else it - 1 }
        val shouldReturn = targetIndex < minIndex || targetIndex > maxIndex

        if (shouldReturn) {
            return "\"$searchFilterItem\" ${STRINGS.ui.notFound}"
        }

        return withContext(Dispatchers.CPU) {
            val idxFound = if (isNext) {
                (targetIndex until maxIndex).firstOrNull {
                    searchFilterItem.matches(items[it].toString())
                } ?: -1
            } else {
                (targetIndex downTo minIndex).firstOrNull {
                    searchFilterItem.matches(items[it].toString())
                } ?: -1
            }

            if (idxFound >= 0) {
                withContext(Dispatchers.UI) {
                    table.moveRowToCenter(idxFound, true)
                }
                EMPTY_STRING
            } else {
                "\"$searchFilterItem\" ${STRINGS.ui.notFound}"
            }
        }
    }

    override fun getItemInCurrentPage(row: Int): LogItem {
        return accessPageData(currentPage) { it[row] }
    }

    override fun getLogFilter(): LogFilter {
        return viewModel.logFilter
    }

    private fun recalculatePage() {
        val pageCount = (logItems.size + pageSize - 1) / pageSize
        val currentPage = minOf(currentPage, pageCount - 1).coerceAtLeast(0)
        val dataSize = logItems.size

        _pageMetadata = PageMetadata(currentPage, pageCount, pageSize, dataSize)
    }

    override fun getRowIndexInAllPages(lineNumber: Int): Int {
        for (logItem in logItems) {
            if (logItem.num >= lineNumber) {
                return logItems.indexOf(logItem)
            }
        }
        return -1
    }

    override fun getRowIndexInCurrentPage(lineNumber: Int): Int {
        val start = currentPage * pageSize
        val end = minOf((currentPage + 1) * pageSize, logItems.size)
        for (i in start until end) {
            if (logItems[i].num >= lineNumber) {
                return i - start
            }
        }
        return -1
    }

    override fun <R> accessPageData(page: Int, action: (List<LogItem>) -> R): R {
        if (pageLogCache.containsKey(page)) {
            return action(pageLogCache.getOrDefault(page, emptyList()))
        }
        val start = page * pageSize
        val end = minOf((page + 1) * pageSize, logItems.size)
        if (start > end) return action(emptyList())
        val pageData = logItems.subList(start, end)
        pageLogCache[page] = pageData
        return action(pageData)
    }

    override fun nextPage() {
        if (currentPage >= pageCount - 1) return
        _pageMetadata = PageMetadata(currentPage + 1, pageCount, pageSize, dataSize)
        onPageChanged()
    }

    override fun previousPage() {
        if (currentPage <= 0) return
        _pageMetadata = PageMetadata(currentPage - 1, pageCount, pageSize, dataSize)
        onPageChanged()
    }

    override fun firstPage() {
        _pageMetadata = PageMetadata(0, pageCount, pageSize, dataSize)
        onPageChanged()
    }

    override fun lastPage() {
        _pageMetadata = PageMetadata(pageCount - 1, pageCount, pageSize, dataSize)
        onPageChanged()
    }

    override fun gotoPage(page: Int) {
        if (page < 0 || page >= pageCount) return
        if (currentPage != page) {
            _pageMetadata = PageMetadata(page, pageCount, pageSize, dataSize)
            onPageChanged()
        }
    }

    private fun onPageChanged() {
        clearSelectedRows()
        pageMetadata.updateValue(_pageMetadata)
        super.fireTableChanged(TableModelEvent(this))
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
        pageLogCache.clear()
        logItems.clear()
        eventListeners.clear()
    }

    companion object {
        const val COLUMN_NUM = 0
        private const val DEFAULT_PAGE_SIZE = 10 * 10000

        private const val TAG = "LogTableModel"
    }

}
