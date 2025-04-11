package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.log.datasource.LogProducerManager
import me.gegenbauer.catspy.log.datasource.LogViewModel

class FilteredLogTableModel(viewModel: LogViewModel) : LogTableModel(viewModel) {

    override val selectedLogRows: MutableSet<Int>
        get() = viewModel.filteredTableSelectedRows

    override val logObservables: LogProducerManager.LogObservables
        get() = viewModel.filteredLogObservables
}