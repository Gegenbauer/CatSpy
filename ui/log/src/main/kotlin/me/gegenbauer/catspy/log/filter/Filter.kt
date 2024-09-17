package me.gegenbauer.catspy.log.filter

import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.filter.FilterProperty.Companion.FILTER_ID_MATCH_CASE
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.Level
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
    val keyPrefix: String = EMPTY_STRING,
    var hasHistory: Boolean = true,
    initialEnabled: Boolean = true
) {
    val enabled: ObservableValueProperty<Boolean> = ObservableValueProperty(initialEnabled)
    val content: ObservableValueProperty<String> = ObservableValueProperty(EMPTY_STRING)
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
        val prefix = if (keyPrefix.isEmpty()) EMPTY_STRING else "${keyPrefix}_"
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
        if (!hasHistory) return
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
        .map { FilterProperty(it.uiConf.filter.name, it.id) }
        .toMutableList()

    properties.add(FilterProperty(GlobalStrings.MATCH_CASE, FILTER_ID_MATCH_CASE))
    return properties
}

fun Column.getFilterFactory(): FilterFactory {
    return if (supportFilter.not() || uiConf.column.isHidden) {
        EmptyColumnFilterFactory()
    } else if (this is Column.LevelColumn) {
        LevelColumnFilterFactory(levels.map { it.level })
    } else {
        ContentColumnFilterFactory()
    }
}

fun interface FilterFactory {
    fun getColumnFilter(properties: FilterProperty, matchCase: Boolean): ColumnFilter
}

class LevelColumnFilterFactory(levels: List<Level>) : FilterFactory {
    private val levelKeywordToLevelMap = levels.associate { it.keyword to it.value }
    private val levelNameToLevelMap = levels.associate { it.name to it.value }

    override fun getColumnFilter(properties: FilterProperty, matchCase: Boolean): ColumnFilter {
        if (properties.enabled.getValueNonNull().not()) {
            return ColumnFilter.EMPTY
        }
        val filterContent = properties.content.getValueNonNull()
        return LevelFilter(
            minLevel = levelNameToLevelMap[filterContent] ?: 0,
            levelGetter = { text ->
                levelKeywordToLevelMap[text] ?: Int.MAX_VALUE
            }
        )
    }
}

class ContentColumnFilterFactory : FilterFactory {

    override fun getColumnFilter(properties: FilterProperty, matchCase: Boolean): ColumnFilter {
        return ContentFilter(properties.processToFilterItem(matchCase))
    }
}

class EmptyColumnFilterFactory : FilterFactory {
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

class ContentFilter(private val filterItem: FilterItem) : ColumnFilter {
    override fun filter(text: String): Boolean {
        return filterItem.match(text)
    }

    override fun toString(): String {
        return "ContentFilter(filterItem=$filterItem)"
    }
}

class LevelFilter(private val minLevel: Int, private val levelGetter: (String) -> Int) : ColumnFilter {
    override fun filter(text: String): Boolean {
        return levelGetter(text) >= minLevel
    }

    override fun toString(): String {
        return "LevelFilter(minLevel=$minLevel)"
    }
}

