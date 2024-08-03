package me.gegenbauer.catspy.view.filter

import me.gegenbauer.catspy.cache.LruCache
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.MemoryAware

class FilterCache : LruCache<FilterKey, FilterItem>(2000), ContextService {

    override fun create(key: FilterKey): FilterItem? {
        return key.filter.toFilterItem(key.matchCase)
    }

    override fun onTrimMemory(level: MemoryAware.Level) {
        setSizeMultiplier(0.8f)
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