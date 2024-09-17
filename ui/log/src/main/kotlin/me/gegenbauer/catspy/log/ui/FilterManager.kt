package me.gegenbauer.catspy.log.ui

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.filter.FilterProperty
import me.gegenbauer.catspy.log.filter.FilterRecord
import me.gegenbauer.catspy.log.filter.generateFilterProperties
import me.gegenbauer.catspy.log.filter.toFilterRecord
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.metadata.getFilterUIConfs
import me.gegenbauer.catspy.log.ui.filter.FavoriteFilterPanel
import me.gegenbauer.catspy.log.ui.filter.FilterPanel
import me.gegenbauer.catspy.log.ui.filter.FilterPropertyObserver
import me.gegenbauer.catspy.log.ui.filter.FilterProvider
import me.gegenbauer.catspy.log.ui.filter.IFavoriteFilterPanel
import me.gegenbauer.catspy.log.ui.filter.IFilterPanel
import me.gegenbauer.catspy.log.ui.filter.IFilterProvider
import me.gegenbauer.catspy.strings.STRINGS

interface IFilterManager: IFilterProvider {
    fun setLogMetadata(logMetaData: LogMetadata)

    fun getMessageFilterProperty(): FilterProperty

    fun getFilterProperty(column: Column): FilterProperty

    fun applyFilterRecord(filterRecord: FilterRecord)
}

class FilterManager(
    val filterPanel: FilterPanel = FilterPanel(),
    private val filterProvider: FilterProvider = FilterProvider(),
    private val favoriteFilterPanel: FavoriteFilterPanel = FavoriteFilterPanel(),
    override val contexts: Contexts = Contexts.default
) : IFilterProvider by filterProvider,
    IFilterPanel by filterPanel,
    IFavoriteFilterPanel by favoriteFilterPanel,
    IFilterManager, Context {

    private val filterProperties = mutableMapOf<Int, FilterProperty>()
    private val idToColumns = mutableMapOf<Int, Column>()
    private val indexToColumn = mutableMapOf<Int, Column>()
    private val filterPropertyObservers = mutableListOf<FilterPropertyObserver>()

    init {
        favoriteFilterPanel.setCurrentFilterRecordProvider(::buildFilterRecord)
        favoriteFilterPanel.setOnFilterRecordSelectedListener(::applyFilterRecord)
    }

    override fun setLogMetadata(logMetaData: LogMetadata) {
        val filterInfos = logMetaData.getFilterUIConfs()

        setColumns(logMetaData.columns)

        disableObservers(filterProperties.values.toList())

        val filterProperties = logMetaData.generateFilterProperties()
        setFilterProperties(filterProperties)

        filterPanel.configure(filterInfos, filterProperties)
        filterProvider.setFilterProperties(logMetaData.columns, filterProperties)

        reAddExistingObservers(filterProperties)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        filterPanel.setParent(context)
        favoriteFilterPanel.setParent(context)
    }

    private fun setColumns(columns: List<Column>) {
        this.idToColumns.clear()
        this.idToColumns.putAll(columns.associateBy { it.id })

        this.indexToColumn.clear()
        this.indexToColumn.putAll(columns.associateBy { it.uiConf.column.index })
    }

    private fun setFilterProperties(filterProperties: List<FilterProperty>) {
        this.filterProperties.clear()
        this.filterProperties.putAll(filterProperties.associateBy { it.columnId })
    }

    private fun disableObservers(properties: List<FilterProperty>) {
        filterPropertyObservers.forEach { observer -> properties.forEach { it.removePropertyObserver(observer) } }
    }

    private fun reAddExistingObservers(properties: List<FilterProperty>) {
        filterPropertyObservers.forEach { observer -> properties.forEach { it.addPropertyObserver(observer) } }
    }

    override fun addFilterPropertyObserver(observer: FilterPropertyObserver) {
        filterProvider.addFilterPropertyObserver(observer)
        filterPropertyObservers.add(observer)
    }

    override fun removeFilterPropertyObserver(observer: FilterPropertyObserver) {
        filterProvider.removeFilterPropertyObserver(observer)
        filterPropertyObservers.remove(observer)
    }

    override fun getMessageFilterProperty(): FilterProperty {
        val messageColumn = idToColumns.values.firstOrNull { it is Column.MessageColumn }
        return filterProperties[messageColumn?.id] ?: FilterProperty(STRINGS.ui.log)
    }

    override fun getFilterProperty(column: Column): FilterProperty {
        return filterProperties[column.id]!!
    }

    override fun applyFilterRecord(filterRecord: FilterRecord) {
        filterPanel.applyFilterRecord(filterRecord)
    }

    private fun buildFilterRecord(name: String): FilterRecord {
        return filterProperties.values.toList().toFilterRecord(name)
    }
}