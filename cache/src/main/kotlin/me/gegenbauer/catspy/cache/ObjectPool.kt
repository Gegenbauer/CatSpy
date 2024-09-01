package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.context.MemoryAware
import me.gegenbauer.catspy.context.MemoryState


abstract class ObjectPool<T: Any>(maxSize: Int): MemoryAware {
    private val cache = Array<Any?>(maxSize) { null }
    private var size = 0

    @Synchronized
    fun obtain(): T {
        if (size == 0) {
            return create()
        }
        val obj = cache[--size] as T
        cache[size] = null
        return obj
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

    companion object {
        fun <T: Any> createMemoryAwarePool(maxSize: Int, create: () -> T): ObjectPool<T> {
            val pool = object : ObjectPool<T>(maxSize) {
                override fun create(): T {
                    return create()
                }
            }
            MemoryState.register(pool)
            return pool
        }
    }
}

fun interface CacheableObject {
    fun recycle()
}

inline fun <T: CacheableObject, R> T.use(block: (T) -> R): R {
    val result: R
    try {
        result = block(this)
    } finally {
        recycle()
    }
    return result
}