package me.gegenbauer.catspy.view.filter

import me.gegenbauer.catspy.cache.LruCache
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService

class FilterCache: LruCache<FilterKey, FilterItem>(2000), ContextService {

    override fun get(key: FilterKey): FilterItem {
        return super.get(key) ?: key.filter.toFilterItem(key.matchCase).apply {
            put(key, this)
        }
    }

    override fun onContextDestroyed(context: Context) {
        clearMemory()
    }
}

data class FilterKey(
    val filter: String,
    val matchCase: Boolean = false
)

fun String.toFilterKey(matchCase: Boolean = false): FilterKey {
    return FilterKey(this, matchCase)
}