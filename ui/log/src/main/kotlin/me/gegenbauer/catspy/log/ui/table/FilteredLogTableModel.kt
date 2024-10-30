package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.log.datasource.ILogViewModel
import me.gegenbauer.catspy.log.datasource.LogProducerManager
import me.gegenbauer.catspy.log.datasource.LogViewModel

class FilteredLogTableModel(viewModel: ILogViewModel) : LogTableModel(viewModel) {

    override val logObservables: LogProducerManager.LogObservables
        get() = viewModel.filteredLogObservables
}