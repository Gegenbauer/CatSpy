package me.gegenbauer.catspy.log.ui.filter

import info.clearthought.layout.TableLayout
import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.IgnoreFastCallbackScheduler
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.customProperty
import me.gegenbauer.catspy.databinding.property.support.enabledProperty
import me.gegenbauer.catspy.databinding.property.support.listProperty
import me.gegenbauer.catspy.databinding.property.support.selectedItemProperty
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.databinding.property.support.textProperty
import me.gegenbauer.catspy.log.filter.FilterProperty
import me.gegenbauer.catspy.log.filter.FilterRecord
import me.gegenbauer.catspy.log.filter.applyFilterRecord
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.utils.ui.editorComponent
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.combobox.filterComboBox
import me.gegenbauer.catspy.view.combobox.readOnlyComboBox
import me.gegenbauer.catspy.view.filter.FilterItem
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

interface IFilterPanel {
    fun configure(filterUIConfs: List<Column.CommonFilterUIConf>, properties: List<FilterProperty>)

    fun applyFilterRecord(filterRecord: FilterRecord)

    fun delayAddFilterToHistory()

    fun getFilterPanel(): JPanel
}

class FilterPanel(override val contexts: Contexts = Contexts.default) : JPanel(), IFilterPanel, Context {
    private val filterUIConfs = mutableListOf<Column.CommonFilterUIConf>()
    private val filterGroups = mutableMapOf<Column.CommonFilterUIConf, FilterGroup>()
    private val properties = mutableMapOf<Column.CommonFilterUIConf, FilterProperty>()
    private val ignoreFastCallbackScheduler = IgnoreFastCallbackScheduler(Dispatchers.UI, 3000)

    override fun configure(filterUIConfs: List<Column.CommonFilterUIConf>, properties: List<FilterProperty>) {
        require(filterUIConfs.size == properties.size) { "Columns and properties must have the same size" }
        this.filterUIConfs.clear()
        this.filterUIConfs.addAll(filterUIConfs)
        this.properties.clear()
        this.properties.putAll(filterUIConfs.zip(properties))
        processColumnsChange()
    }

    override fun applyFilterRecord(filterRecord: FilterRecord) {
        properties.values.toList().applyFilterRecord(filterRecord)
    }

    override fun delayAddFilterToHistory() {
        ignoreFastCallbackScheduler.schedule {
            properties.values.forEach { it.addCurrentContentToList() }
        }
    }

    override fun getFilterPanel(): JPanel {
        return this
    }

    private fun processColumnsChange() {
        removeAll()
        val filtersInRows = filterUIConfs
            .sortedBy { it.position.columnIndex }
            .groupBy { it.position.rowIndex }
            .values
            .sortedBy { it.first().position.rowIndex }
            .toList()

        layout = TableLayout(
            doubleArrayOf(TableLayout.FILL),
            DoubleArray(filtersInRows.size) { TableLayout.PREFERRED }
        )
        filtersInRows.forEach {
            add(createFilterRow(it), "0,${filtersInRows.indexOf(it)}")
        }
        parent?.revalidate()
        parent?.repaint()
    }

    private fun createFilterRow(filters: List<Column.CommonFilterUIConf>): JPanel {
        val rowPanel = JPanel()
        rowPanel.layout = TableLayout(arrayOf(DoubleArray(filters.size) {
            filters[it].layoutWidth
        }, doubleArrayOf(TableLayout.PREFERRED)))

        filterGroups.clear()
        filters.forEach { filterInfo ->
            val filterGroup = createFilterGroup(filterInfo).also { it.init() }
            filterGroups[filterInfo] = filterGroup
            filterGroup.bind(properties[filterInfo]!!)
            rowPanel.add(filterGroup, "${filters.indexOf(filterInfo)},0")
        }
        return rowPanel
    }

    private fun createFilterGroup(filterUIConf: Column.CommonFilterUIConf): BaseFilterGroup {
        return when (filterUIConf) {
            is Column.LevelFilterUIConf -> LevelFilterGroup(filterUIConf)
            is Column.MatchCaseFilterInfoConf -> MatchCaseFilterGroup(filterUIConf)
            else -> ContentFilterGroup(filterUIConf)
        }
    }

    override fun destroy() {
        super.destroy()
        ignoreFastCallbackScheduler.cancel()
    }

    private interface FilterGroup {

        fun init()

        fun bind(properties: FilterProperty)
    }

    abstract class BaseFilterGroup(filterUIConf: Column.CommonFilterUIConf) : JPanel(), FilterGroup {
        private val enableComponent = ColorToggleButton(filterUIConf.name)

        override fun init() {
            layout = BorderLayout()
            add(enableComponent, BorderLayout.WEST)
            getFilterComponent()?.let { add(it, BorderLayout.CENTER) }
        }

        protected abstract fun getFilterComponent(): JComponent?

        override fun bind(properties: FilterProperty) {
            selectedProperty(enableComponent) bindDual properties.enabled
        }
    }

    class LevelFilterGroup(filterInfo: Column.LevelFilterUIConf) : BaseFilterGroup(filterInfo) {
        private val levelSelector = readOnlyComboBox()
        private val levels = filterInfo.levels

        override fun getFilterComponent(): JComponent {
            return levelSelector
        }

        override fun bind(properties: FilterProperty) {
            super.bind(properties)
            properties.hasHistory = false

            enabledProperty(levelSelector) bindDual properties.enabled
            listProperty(levelSelector) bindDual properties.contentList
            textProperty(levelSelector.editorComponent) bindDual properties.content
            selectedItemProperty(levelSelector) bindDual properties.selectedItem
            properties.selectedItem.updateValue(levels.minBy { it.level.value }.level.name)

            properties.contentList.updateValue(levels.map { it.level.name })
        }

    }

    open class ContentFilterGroup(filterUIConf: Column.CommonFilterUIConf) : BaseFilterGroup(filterUIConf) {
        private val contentFilterComponent = filterComboBox()

        override fun getFilterComponent(): JComponent {
            return contentFilterComponent
        }

        override fun bind(properties: FilterProperty) {
            super.bind(properties)

            enabledProperty(contentFilterComponent) bindDual properties.enabled
            listProperty(contentFilterComponent) bindDual properties.contentList
            textProperty(contentFilterComponent.editorComponent) bindDual properties.content
            selectedItemProperty(contentFilterComponent) bindDual properties.selectedItem
            customProperty(contentFilterComponent, "filterItem", FilterItem.EMPTY_ITEM) bindDual properties.filterItem
        }
    }

    class MatchCaseFilterGroup(filterUIConf: Column.CommonFilterUIConf) : BaseFilterGroup(filterUIConf) {

        override fun getFilterComponent(): JComponent? {
            return null
        }

    }
}