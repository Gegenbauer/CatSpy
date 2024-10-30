package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import me.gegenbauer.catspy.concurrency.Event
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.view.state.ListState
import java.io.File

class FileGroupSearchViewModel(override val contexts: Contexts = Contexts.default) : ILogViewModel {
    override val fullLogObservables: LogProducerManager.LogObservables
        get() = throw UnsupportedOperationException("No need to implement")
    override val filteredLogObservables: LogProducerManager.LogObservables
        get() = LogProducerManager.LogObservables(_logItemsFlow, _listState, _logRepaintEventFlow)
    override val logFilter: LogFilter
        get() = logConf.getCurrentFilter()

    private val logConf by lazy { contexts.getContext(LogConfiguration::class.java)!! }
    private val _logItemsFlow = MutableStateFlow(emptyList<LogItem>())
    private val _listState = MutableStateFlow<ListState>(ListState.Empty)
    private val _logRepaintEventFlow = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    private val scope = ModelScope()

    fun startSearch(file: File) {
        scope
    }

    override fun updateFilter(filter: LogFilter) {

    }
}