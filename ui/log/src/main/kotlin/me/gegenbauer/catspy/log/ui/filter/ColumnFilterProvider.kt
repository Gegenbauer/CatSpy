package me.gegenbauer.catspy.log.ui.filter

import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.log.filter.ColumnFilter
import me.gegenbauer.catspy.log.filter.FilterFactory
import me.gegenbauer.catspy.log.filter.FilterProperty
import me.gegenbauer.catspy.log.filter.getFilterFactory
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.view.filter.FilterItem

interface IColumnFilterProvider {
    fun getFilter(columnId: Int): ColumnFilter

    fun getFilterItem(columnId: Int): FilterItem

    fun addFilterPropertyObserver(observer: FilterPropertyObserver)

    fun removeFilterPropertyObserver(observer: FilterPropertyObserver)
}

fun interface FilterPropertyObserver {
    fun onFilterPropertyChanged(property: ObservableValueProperty<*>)
}

class ColumnFilterProvider : IColumnFilterProvider, FilterPropertyObserver {

    private val filterProperties = mutableMapOf<Int, FilterProperty>()
    private val columns = mutableMapOf<Int, Column>()
    private val filterFactories = mutableMapOf<Int, FilterFactory>()
    private val filters = mutableMapOf<Int, ColumnFilter>()
    private val matchCaseFilterProperty: FilterProperty
        get() = filterProperties[FilterProperty.FILTER_ID_MATCH_CASE]
            ?: FilterProperty(GlobalStrings.MATCH_CASE, FilterProperty.FILTER_ID_MATCH_CASE)

    private val matchCase: Boolean
        get() = matchCaseFilterProperty.enabled.getValueNonNull()

    fun setFilterProperties(columns: List<Column>, filterProperties: List<FilterProperty>) {
        this.filterProperties.values.forEach { it.removePropertyObserver(this) }
        this.filterProperties.clear()
        this.filterProperties.putAll(filterProperties.associateBy { it.columnId })
        this.filterProperties.values.forEach { it.addPropertyObserver(this) }

        this.columns.clear()
        this.columns.putAll(columns.associateBy { it.id })

        rebuildFilterFactories()
        rebuildFilters()
    }

    private fun rebuildFilterFactories() {
        filterFactories.clear()
        columns.keys.filter {
            columns[it]?.let { column -> column.supportFilter && column.uiConf.column.isHidden.not() } ?: false
        }.forEach {
            val column = columns[it]
            if (column != null) {
                filterFactories[column.id] = column.getFilterFactory()
            }
        }
    }

    private fun rebuildFilters() {
        filters.clear()

        columns.keys.forEach { column ->
            val filterFactory = filterFactories[column]
            val filterProperties = filterProperties[column]

            if (filterFactory != null && filterProperties != null) {
                filters[column] = filterFactory.getColumnFilter(filterProperties, matchCase)
            }
        }
    }

    override fun getFilter(columnId: Int): ColumnFilter {
        return filters[columnId] ?: ColumnFilter.EMPTY
    }

    override fun getFilterItem(columnId: Int): FilterItem {
        return filterProperties[columnId]?.processToFilterItem(matchCase)
            ?: FilterItem.EMPTY_ITEM
    }

    override fun addFilterPropertyObserver(observer: FilterPropertyObserver) {
        filterProperties.values.forEach { it.addPropertyObserver(observer) }
    }

    override fun removeFilterPropertyObserver(observer: FilterPropertyObserver) {
        filterProperties.values.forEach { it.removePropertyObserver(observer) }
    }

    override fun onFilterPropertyChanged(property: ObservableValueProperty<*>) {
        rebuildFilters()
        filterProperties.values.forEach { it.processToFilterItem(matchCase) }
    }

}