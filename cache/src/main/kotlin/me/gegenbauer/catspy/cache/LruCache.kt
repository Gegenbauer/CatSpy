package me.gegenbauer.catspy.cache

import kotlin.math.roundToLong

open class LruCache<T, K>(private val initialMaxSize: Long): MemoryAware {
    val count: Int
        get() = cache.size
    var currentSize: Long = 0L
    var maxSize: Long = initialMaxSize
    var missedCount: Int = 0

    private val cache: MutableMap<T, Entry<K>> = LinkedHashMap(100, 0.75f, true)


    @Synchronized
    fun setSizeMultiplier(multiplier: Float) {
        require(multiplier >= 0) { "Multiplier must be >= 0" }
        maxSize = (initialMaxSize * multiplier).roundToLong()
        ensureSize()
    }

    protected open fun getSize(item: K?): Int {
        return 1
    }

    @Synchronized
    fun contains(key: T): Boolean {
        return cache.containsKey(key)
    }

    @Synchronized
    open operator fun get(key: T): K? {
        val entry = cache[key]
        val value = entry?.value
        if (value == null) {
            missedCount++
        }
        return value
    }

    @Synchronized
    fun put(key: T, item: K?): K? {
        if (item == null) {
            remove(key)
            return null
        }

        val itemSize = getSize(item)
        if (itemSize >= maxSize) {
            onItemEvicted(key, item)
            return null
        }

        currentSize += itemSize

        val old = cache.put(key, Entry(item, itemSize))
        old?.let {
            currentSize -= it.size
            if (old.value != item) {
                onItemEvicted(key, old.value)
            }
        }
        ensureSize()

        return old?.value
    }

    @Synchronized
    fun remove(key: T): K? {
        val value = cache.remove(key)
        if (value != null) {
            currentSize -= value.size
        }
        return value?.value
    }

    fun clearMemory() {
        CacheLog.d(TAG, "[clearMemory]")
        trimToSize(0)
    }

    protected open fun onItemEvicted(key: T, value: K?) {
        // optional override
    }

    @Synchronized
    protected fun trimToSize(size: Long) {
        CacheLog.d(TAG, "[trimToSize] currentSize: $currentSize, maxSize: $maxSize, size: $size")
        val cacheIterator = cache.iterator()
        while (currentSize > size) {
            val last = cacheIterator.next()
            val toRemove = last.value
            currentSize -= toRemove.size
            val key = last.key
            cacheIterator.remove()
            onItemEvicted(key, toRemove.value)
        }
    }

    private fun ensureSize() {
        trimToSize(maxSize)
    }

    override fun toString(): String {
        return "LruCache(currentSize=$currentSize, maxSize=$maxSize, count=$count, missedCount=$missedCount)"
    }

    data class Entry<K>(
        val value: K,
        val size: Int
    )

    companion object {
        private const val TAG = "LruCache"
    }

    override fun onTrimMemory(level: Int) {
        CacheLog.d(TAG, "[onTrimMemory] level: $level")
        // TODO
    }
}