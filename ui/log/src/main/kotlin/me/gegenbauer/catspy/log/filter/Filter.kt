package me.gegenbauer.catspy.log.filter

import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.log.filter.FilterProperty.Companion.FILTER_ID_MATCH_CASE
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.ui.filter.FilterPropertyObserver
import me.gegenbauer.catspy.utils.persistence.appendKeySeparator
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.getOrCreateFilterItem
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class FilterProperty(
    val name: String,
    val columnId: Int = -1,
    val storeKeyPrefix: String = "",
    initialEnabled: Boolean = true
) {
    val enabled: ObservableValueProperty<Boolean> = StorableValueProperty(getComposedKey("enabled"), initialEnabled)
    val content: ObservableValueProperty<String> = ObservableValueProperty("")
    val contentList: ObservableValueProperty<List<String>> =
        StorableValueProperty(getComposedKey("contentList"), emptyList())
    val selectedItem: ObservableValueProperty<String> = ObservableValueProperty(null)
    val filterItem: ObservableValueProperty<FilterItem> = ObservableValueProperty(FilterItem.EMPTY_ITEM)

    private val observers = mutableListOf<FilterPropertyObserver>()
    private val observerLock = ReentrantReadWriteLock()

    private val enableStateObserver = Observer<Boolean> {
        notifyObservers(enabled)
    }

    private fun getComposedKey(key: String): String {
        val prefix = if (storeKeyPrefix.isEmpty()) "" else "${storeKeyPrefix}_"
        return STORE_KEY_PREFIX.appendKeySeparator("$prefix${name}_$key")
    }

    private val contentObserver = Observer<String> {
        notifyObservers(content)
    }

    init {
        enabled.addObserver(enableStateObserver)
        content.addObserver(contentObserver)
    }

    fun addCurrentContentToList() {
        // wrong filter item should not be added to the list
        if (filterItem.getValueNonNull().errorMessage.isNotEmpty()) return

        val currentContent = content.getValueNonNull()
        if (currentContent.isEmpty()) return
        val currentList = contentList.getValueNonNull().toMutableList()
        if (currentList.contains(currentContent)) {
            currentList.removeIf { it == currentContent }
        }
        currentList.add(0, currentContent)
        contentList.updateValue(currentList)
        selectedItem.updateValue(currentContent)
    }

    /**
     * pre-process the filter item
     */
    fun processToFilterItem(matchCase: Boolean): FilterItem {
        val filterItem = content.getValueNonNull().getOrCreateFilterItem(matchCase).takeIf { enabled.getValueNonNull() }
            ?: FilterItem.EMPTY_ITEM
        this.filterItem.updateValue(filterItem)
        return filterItem
    }

    fun addPropertyObserver(observer: FilterPropertyObserver) {
        observerLock.write {
            observers.add(observer)
        }
    }

    fun removePropertyObserver(observer: FilterPropertyObserver) {
        observerLock.write {
            observers.remove(observer)
        }
    }

    private fun notifyObservers(property: ObservableValueProperty<*>) {
        observerLock.read {
            observers.forEach { it.onFilterPropertyChanged(property) }
        }
    }

    companion object {
        const val FILTER_ID_MATCH_CASE = -1

        private const val STORE_KEY_PREFIX = "filter"
    }
}

fun LogMetadata.generateFilterProperties(): List<FilterProperty> {
    val properties = columns
        .filter { it.supportFilter && it.uiConf.column.isHidden.not() }
        .map { FilterProperty(it.name, it.id) }
        .toMutableList()

    properties.add(FilterProperty(GlobalStrings.MATCH_CASE, FILTER_ID_MATCH_CASE))
    return properties
}

fun Column.getFilterFactory(): FilterFactory {
    return if (supportFilter.not() || uiConf.column.isHidden) {
        EmptyColumnFilterFactory(this)
    } else if (this is Column.LevelColumn) {
        LevelColumnFilterFactory(this)
    } else {
        ContentColumnFilterFactory(this)
    }
}

interface FilterFactory {
    val column: Column

    fun getColumnFilter(properties: FilterProperty, matchCase: Boolean): ColumnFilter
}

class LevelColumnFilterFactory(override val column: Column) : FilterFactory {
    private val levelTagToLevelMap = (column as Column.LevelColumn).levels.associate { it.level.tag to it.level.value }
    private val levelNameToLevelMap =
        (column as Column.LevelColumn).levels.associate { it.level.name to it.level.value }

    override fun getColumnFilter(properties: FilterProperty, matchCase: Boolean): ColumnFilter {
        return ColumnFilter { text ->
            if (properties.enabled.getValueNonNull().not()) {
                return@ColumnFilter true
            }
            val minLevel = levelNameToLevelMap[properties.content.getValueNonNull()] ?: 0
            (levelTagToLevelMap[text] ?: Int.MAX_VALUE) >= minLevel
        }
    }
}

class ContentColumnFilterFactory(override val column: Column) : FilterFactory {

    override fun getColumnFilter(properties: FilterProperty, matchCase: Boolean): ColumnFilter {
        if (properties.content.getValueNonNull().isEmpty()) return ColumnFilter.EMPTY
        return ColumnFilter { text ->
            if (properties.enabled.getValueNonNull().not()) {
                return@ColumnFilter true
            }
            val filterItem = properties.processToFilterItem(matchCase)

            filterItem.match(text)
        }
    }
}

class EmptyColumnFilterFactory(override val column: Column) : FilterFactory {
    override fun getColumnFilter(properties: FilterProperty, matchCase: Boolean): ColumnFilter {
        return ColumnFilter.EMPTY
    }
}

fun interface ColumnFilter {
    fun filter(text: String): Boolean

    companion object {
        val EMPTY = ColumnFilter { true }

        inline val ColumnFilter.isEmpty: Boolean
            get() = this == EMPTY
    }
}