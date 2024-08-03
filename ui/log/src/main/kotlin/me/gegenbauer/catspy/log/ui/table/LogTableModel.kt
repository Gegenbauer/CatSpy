package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.datasource.LogProducerManager
import me.gegenbauer.catspy.log.datasource.LogViewModel
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.datasource.LogItem
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
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
) : AbstractTableModel(), Context, Searchable, Pageable<LogItem>, ILogTableModel {
    override var searchFilterItem: FilterItem = FilterItem.EMPTY_ITEM
        set(value) {
            field = value.takeIf { it != field }?.also { getLogTable()?.repaint() } ?: value
        }

    override val pageMetaData: ObservableValueProperty<PageMetadata> = ObservableValueProperty()

    override val pageSize: Int
        get() = DEFAULT_PAGE_SIZE

    override var selectedLogRows: List<Int>
        get() = viewModel.fullTableSelectedRows
        set(value) {
            viewModel.fullTableSelectedRows = value
        }
    override val logObservables: LogProducerManager.LogObservables
        get() = viewModel.fullLogObservables

    private val logConf: LogConfiguration
        get() = contexts.getContext(LogConfiguration::class.java) ?: LogConfiguration.default

    private val scope = ViewModelScope()
    private var logItems = mutableListOf<LogItem>()
    private val eventListeners = Collections.synchronizedList(ArrayList<LogTableModelListener>())

    override fun configureContext(context: Context) {
        super.configureContext(context)
        viewModel.setParent(this)
        collectLogItems()
        observeRepaintEvent()
    }

    private fun collectLogItems() {
        scope.launch(Dispatchers.UI) {
            logObservables.itemsFlow.collect { items ->
                val oldItems = logItems
                logItems = items.toMutableList()
                fireChangeEvent(oldItems, logItems)
            }
        }
    }

    private fun observeRepaintEvent() {
        scope.launch(Dispatchers.UI) {
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
            resetSelectedRows()
        }

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
        selectedLogRows = emptyList()
    }

    private fun resetSelectedRows() {
        if (selectedLogRows.isNotEmpty() && selectedLogRows.firstOrNull() in 0 until rowCount
            && selectedLogRows.lastOrNull() in 0 until rowCount
        ) {
            val selectedRowStart = selectedLogRows.first()
            val selectedRowEnd = selectedLogRows.last()
            runCatching {
                getLogTable()?.setRowSelectionInterval(selectedRowStart, selectedRowEnd)
            }.onFailure {
                GLog.e(
                    TAG, "[collectLogItems] selectedRows=$selectedLogRows, " +
                            "rowCount=$rowCount, itemsSize=${logItems.size}", it
                )
            }
        }
    }

    private fun getLogTable(): LogTable? {
        return contexts.getContext(LogTable::class.java)
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
        return logConf.getColumnCount()
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        // 0 列是行号，从 1 列开始是日志内容
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

    override fun moveToNextSearchResult(): String {
        return moveToSearch(true)
    }

    override fun moveToPreviousSearchResult(): String {
        return moveToSearch(false)
    }

    private fun moveToSearch(isNext: Boolean): String {
        if (searchFilterItem.isEmpty()) return ""

        val selectedRow = selectedLogRows.firstOrNull() ?: -1
        val mainUI = contexts.getContext(BaseLogMainPanel::class.java)
        mainUI ?: return ""
        val table = getLogTable()
        table ?: return ""

        val targetRow = selectedRow.run { if (isNext) this + 1 else this - 1 }
        val shouldReturn = targetRow.run { if (isNext) this >= rowCount else this < 0 }

        if (shouldReturn) {
            return "\"$searchFilterItem\" ${STRINGS.ui.notFound}"
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
            table.moveRowToCenter(idxFound, true)
            return ""
        }
        return "\"$searchFilterItem\" ${STRINGS.ui.notFound}"
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

        pageMetaData.updateValue(PageMetadata(currentPage, pageCount, pageSize, dataSize))
    }

    override fun getRowIndexInAllPages(lineNumber: Int): Int {
        for (logItem in logItems) {
            if (logItem.num >= lineNumber) {
                return logItems.indexOf(logItem)
            }
        }
        return -1
    }

    override fun <R> accessPageData(page: Int, action: (List<LogItem>) -> R): R {
        val start = page * pageSize
        val end = minOf((page + 1) * pageSize, logItems.size)
        if (start > end) return action(emptyList())
        return action(logItems.subList(start, end))
    }

    override fun nextPage() {
        if (currentPage >= pageCount - 1) return
        pageMetaData.updateValue(PageMetadata(currentPage + 1, pageCount, pageSize, dataSize))
        onPageChanged()
    }

    override fun previousPage() {
        if (currentPage <= 0) return
        pageMetaData.updateValue(PageMetadata(currentPage - 1, pageCount, pageSize, dataSize))
        onPageChanged()
    }

    override fun firstPage() {
        pageMetaData.updateValue(PageMetadata(0, pageCount, pageSize, dataSize))
        onPageChanged()
    }

    override fun lastPage() {
        pageMetaData.updateValue(PageMetadata(pageCount - 1, pageCount, pageSize, dataSize))
        onPageChanged()
    }

    override fun gotoPage(page: Int) {
        if (page < 0 || page >= pageCount) return
        if (currentPage != page) {
            pageMetaData.updateValue(PageMetadata(page, pageCount, pageSize, dataSize))
            onPageChanged()
        }
    }

    private fun onPageChanged() {
        clearSelectedRows()
        fireTableDataChanged()
    }

    companion object {
        const val COLUMN_NUM = 0
        private const val DEFAULT_PAGE_SIZE = 10 * 10000

        private const val TAG = "LogTableModel"
    }

}
