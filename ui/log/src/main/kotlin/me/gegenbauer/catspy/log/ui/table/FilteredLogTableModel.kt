package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.log.datasource.LogViewModel

class FilteredLogTableModel(viewModel: LogViewModel) : LogTableModel(viewModel) {

    override var selectedRows: List<Int>
        get() = viewModel.filteredTableSelectedRows
        set(value) {
            viewModel.filteredTableSelectedRows = value
        }

    override fun collectLogItems() {
        scope.launch(Dispatchers.UI) {
            viewModel.filteredLogItemsFlow.collect {
                logItems = it.toMutableList()
                fireTableDataChanged()
            }
        }
    }
}