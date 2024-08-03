package me.gegenbauer.catspy.log.ui

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.Observable
import me.gegenbauer.catspy.databinding.bind.ObservableProperty
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.log.filter.DefaultLogFilter
import me.gegenbauer.catspy.log.filter.FilterInfo
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.metadata.LogMetadataOwner
import me.gegenbauer.catspy.log.ui.filter.FilterPanel
import me.gegenbauer.catspy.log.ui.search.ISearchFilterController
import me.gegenbauer.catspy.log.ui.search.ISearchPanel
import me.gegenbauer.catspy.log.ui.search.SearchFilterController
import me.gegenbauer.catspy.log.ui.search.SearchPanel
import me.gegenbauer.catspy.log.ui.table.ILogRenderer
import me.gegenbauer.catspy.log.ui.table.LogRenderer

class LogConfiguration(
    private val columnManager: ColumnManager = ColumnManager(),
    private val searchFilterController: SearchFilterController = SearchFilterController(),
    private val searchPanel: SearchPanel = SearchPanel(),
    private val logRenderer: LogRenderer = LogRenderer(),
    override val contexts: Contexts = Contexts.default,
    private val observableLogMetadata: ObservableProperty<LogMetadata> = ObservableProperty()
) : IColumnManager by columnManager,
    ILogRenderer by logRenderer,
    Observable<LogMetadata> by observableLogMetadata,
    ISearchFilterController by searchFilterController,
    ISearchPanel by searchPanel,
    Context,
    LogMetadataOwner {

    private val logMetadataObservers = mutableListOf<Observer<LogMetadata>>()

    init {
        searchFilterController.bind(searchPanel)
    }

    override val filterPanel: FilterPanel
        get() = columnManager.filterPanel

    override var logMetaData: LogMetadata
        get() = observableLogMetadata.value ?: LogMetadata.default
        set(value) {
            observableLogMetadata.updateValue(value)
        }

    private var lastFilter = DefaultLogFilter(emptyList(), emptyList())

    override fun setLogMetadata(logMetaData: LogMetadata) {
        disableObservers()
        this.logMetaData = logMetaData
        reAddExistingObservers()
        columnManager.setLogMetadata(logMetaData)
        logRenderer.setColumns(logMetaData)
        this.observableLogMetadata.forceUpdateValue(logMetaData)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        columnManager.setParent(this)
        searchPanel.setParent(this)
        logRenderer.setParent(this)
        filterPanel.setParent(this)
    }

    override fun addObserver(observer: Observer<LogMetadata>) {
        observableLogMetadata.addObserver(observer)
        logMetadataObservers.add(observer)
    }

    override fun removeObserver(observer: Observer<LogMetadata>?) {
        observableLogMetadata.removeObserver(observer)
        logMetadataObservers.remove(observer)
    }

    private fun disableObservers() {
        logMetadataObservers.forEach { observer -> observableLogMetadata.removeObserver(observer) }
    }

    private fun reAddExistingObservers() {
        logMetadataObservers.forEach { observer -> observableLogMetadata.addObserver(observer) }
    }

    fun generateLogFilter(): LogFilter {
        val filterableColumns = logMetaData.columns.filter { it.supportFilter && it.uiConf.column.isHidden.not() }
        val filters = filterableColumns.map { FilterInfo(it, getFilterItem(it.id), getFilter(it.id)) }
        val newFilter = DefaultLogFilter(filters, filterableColumns)
        if (newFilter != lastFilter) {
            filterPanel.onNewFilterGenerated()
            lastFilter = newFilter
        }
        return newFilter
    }

    companion object {
        val default = LogConfiguration()
    }
}