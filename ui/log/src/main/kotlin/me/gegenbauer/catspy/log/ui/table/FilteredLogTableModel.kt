package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.flow.Flow
import me.gegenbauer.catspy.log.datasource.LogViewModel
import me.gegenbauer.catspy.log.model.LogcatItem

class FilteredLogTableModel(viewModel: LogViewModel, deviceMode: Boolean = false) : LogTableModel(viewModel, deviceMode) {

    override var selectedLogRows: List<Int>
        get() = viewModel.filteredTableSelectedRows
        set(value) {
            viewModel.filteredTableSelectedRows = value
        }

    override val logFlow: Flow<List<LogcatItem>>
        get() = viewModel.filteredLogItemsFlow
}