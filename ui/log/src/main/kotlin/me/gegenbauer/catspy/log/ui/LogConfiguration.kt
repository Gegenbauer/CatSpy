package me.gegenbauer.catspy.log.ui

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.Observable
import me.gegenbauer.catspy.databinding.bind.ObservableProperty
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.log.filter.DefaultLogFilter
import me.gegenbauer.catspy.log.filter.ColumnFilterInfo
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.metadata.LogMetadataOwner
import me.gegenbauer.catspy.log.ui.filter.FilterPanel
import me.gegenbauer.catspy.log.ui.filter.IFavoriteFilterPanel
import me.gegenbauer.catspy.log.ui.search.ISearchFilterController
import me.gegenbauer.catspy.log.ui.search.ISearchPanel
import me.gegenbauer.catspy.log.ui.search.SearchFilterController
import me.gegenbauer.catspy.log.ui.search.SearchPanel
import me.gegenbauer.catspy.log.ui.table.ILogRenderer
import me.gegenbauer.catspy.log.ui.table.LogRenderer
import java.util.concurrent.atomic.AtomicBoolean

class LogConfiguration(
    private val filterManager: FilterManager = FilterManager(),
    private val searchFilterController: SearchFilterController = SearchFilterController(),
    private val searchPanel: SearchPanel = SearchPanel(),
    private val bufferSelectPanel: ILogcatLogBufferSelectPanel = LogcatLogBufferSelectPanel(),
    private val logRenderer: LogRenderer = LogRenderer(),
    private val observableLogMetadata: ObservableProperty<LogMetadata> = ObservableProperty(),
    override val contexts: Contexts = Contexts.default,
) : IFilterManager by filterManager,
    IFavoriteFilterPanel by filterManager,
    ILogRenderer by logRenderer,
    ILogcatLogBufferSelectPanel by bufferSelectPanel,
    Observable<LogMetadata> by observableLogMetadata,
    ISearchFilterController by searchFilterController,
    ISearchPanel by searchPanel,
    Context,
    LogMetadataOwner {

    var isPreviewMode = false

    private val logMetadataObservers = mutableListOf<Observer<LogMetadata>>()

    init {
        searchFilterController.bind(searchPanel)
    }

    val filterPanel: FilterPanel
        get() = filterManager.filterPanel

    override var logMetaData: LogMetadata
        get() = observableLogMetadata.value ?: LogMetadata.default
        set(value) {
            observableLogMetadata.updateValue(value)
        }

    val filterCreatedAfterMetadataChanged = AtomicBoolean(false)
    private var filter = DefaultLogFilter(emptyList(), emptyList())

    override fun setLogMetadata(logMetaData: LogMetadata) {
        filterCreatedAfterMetadataChanged.set(false)
        disableObservers()
        this.logMetaData = logMetaData
        reAddExistingObservers()
        filterManager.setLogMetadata(logMetaData)
        logRenderer.cancel()
        logRenderer.setColumns(logMetaData)
        this.observableLogMetadata.forceUpdateValue(logMetaData)
    }

    override fun configureContext(context: Context) {
        filterManager.setParent(this)
        searchPanel.setParent(this)
        logRenderer.setParent(this)
        filterPanel.setParent(this)
        bufferSelectPanel.setParent(this)
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
        val filters = filterableColumns.map { ColumnFilterInfo(it, getFilterItem(it.id), getFilter(it.id)) }
        val newFilter = DefaultLogFilter(filters, filterableColumns)
        if (newFilter != filter) {
            filterPanel.delayAddFilterToHistory()
            filter = newFilter
        }
        filterCreatedAfterMetadataChanged.set(true)
        return newFilter
    }

    fun getCurrentFilter(): LogFilter {
        return filter
    }

    companion object {
        val default = LogConfiguration()
    }
}