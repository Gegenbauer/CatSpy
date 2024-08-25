package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.context.MemoryAware

abstract class ObjectPool<T: Any>(maxSize: Int): MemoryAware {
    private val cache = Array<Any?>(maxSize) { this.create() }
    private var size = 0

    @Synchronized
    fun obtain(): T {
        if (size == 0) {
            return create()
        }
        return cache[--size] as T
    }

    @Synchronized
    fun recycle(obj: T) {
        if (size < cache.size) {
            cache[size++] = obj
        }
    }

    @Synchronized
    fun clear() {
        for (i in 0 until size) {
            cache[i] = null
        }
        size = 0
    }

    abstract fun create(): T

    override fun onTrimMemory(level: MemoryAware.Level) {
        clear()
    }
}

fun interface CacheableObject {
    fun recycle()
}

inline fun <T: CacheableObject, R> T.with(block: (T) -> R): R {
    val result: R
    try {
        result = block(this)
    } finally {
        recycle()
    }
    return result
}