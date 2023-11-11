package me.gegenbauer.catspy.cache

class LruResourceCache(size: Long): LruCache<Key, Resource<*>>(size), MemoryCache {
    private var listener: MemoryCache.ResourceRemovedListener? = null

    override fun setResourceRemovedListener(listener: MemoryCache.ResourceRemovedListener) {
        this.listener = listener
    }

    override fun onItemEvicted(key: Key, value: Resource<*>?) {
        if (value != null) {
            listener?.onResourceRemoved(value)
        }
    }

    override fun getSize(item: Resource<*>?): Int {
        if (item == null) {
            return super.getSize(null)
        } else {
            return item.size
        }
    }

    override fun trimMemory(level: Int) {

    }

}