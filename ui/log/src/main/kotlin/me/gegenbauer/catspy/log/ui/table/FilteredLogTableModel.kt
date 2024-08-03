package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.log.datasource.LogProducerManager
import me.gegenbauer.catspy.log.datasource.LogViewModel

class FilteredLogTableModel(viewModel: LogViewModel) : LogTableModel(viewModel) {

    override var selectedLogRows: List<Int>
        get() = viewModel.filteredTableSelectedRows
        set(value) {
            viewModel.filteredTableSelectedRows = value
        }

    override val logObservables: LogProducerManager.LogObservables
        get() = viewModel.filteredLogObservables
}