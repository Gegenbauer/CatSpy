package me.gegenbauer.catspy.cache

import kotlin.jvm.Throws

interface Cacheable<T, K> {
    val size: Int

    fun clear()

    fun clear(key: K)

    @Throws(Exception::class)
    fun get(key: K): T
}