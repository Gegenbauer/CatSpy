package me.gegenbauer.catspy.log.ui

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.ui.filter.FilterPanelProvider
import me.gegenbauer.catspy.log.filter.FilterProperty
import me.gegenbauer.catspy.log.filter.generateFilterProperties
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.metadata.getFilterUIConfs
import me.gegenbauer.catspy.log.ui.filter.ColumnFilterProvider
import me.gegenbauer.catspy.log.ui.filter.IColumnFilterProvider
import me.gegenbauer.catspy.log.ui.filter.FilterPanel
import me.gegenbauer.catspy.log.ui.filter.FilterPropertyObserver
import me.gegenbauer.catspy.strings.STRINGS

interface IColumnManager: IColumnFilterProvider, FilterPanelProvider {
    fun setLogMetadata(logMetaData: LogMetadata)

    fun getMessageFilterProperty(): FilterProperty

    fun getFilterProperty(column: Column): FilterProperty
}

class ColumnManager(
    private val filterProvider: ColumnFilterProvider = ColumnFilterProvider(),
    override val contexts: Contexts = Contexts.default
) : IColumnFilterProvider by filterProvider, FilterPanelProvider, IColumnManager, Context {

    override val filterPanel = FilterPanel()

    private val filterProperties = mutableMapOf<Int, FilterProperty>()
    private val idToColumns = mutableMapOf<Int, Column>()
    private val indexToColumn = mutableMapOf<Int, Column>()
    private val filterPropertyObservers = mutableListOf<FilterPropertyObserver>()

    override fun setLogMetadata(logMetaData: LogMetadata) {
        val filterInfos = logMetaData.getFilterUIConfs()

        setColumns(logMetaData.columns)

        disableObservers(filterProperties.values.toList())

        val filterProperties = logMetaData.generateFilterProperties()
        setFilterProperties(filterProperties)

        filterPanel.setFilters(filterInfos, filterProperties)
        filterProvider.setFilterProperties(logMetaData.columns, filterProperties)

        reAddExistingObservers(filterProperties)
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
}