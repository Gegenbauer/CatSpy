package me.gegenbauer.catspy.cache

/**
 * Refer To Glide
 */
interface MemoryCache {
    val currentSize: Long

    val maxSize: Long

    fun interface ResourceRemovedListener {
        fun onResourceRemoved(removed: Resource<*>)
    }

    fun setSizeMultiplier(multiplier: Float)

    fun remove(key: Key): Resource<*>?

    fun put(key: Key, resource: Resource<*>?): Resource<*>?

    fun get(key: Key): Resource<*>?

    fun setResourceRemovedListener(listener: ResourceRemovedListener)

    fun clearMemory()

    fun trimMemory(level: Int)
}