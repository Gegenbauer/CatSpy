package me.gegenbauer.catspy.cache

interface Resource<T> {

    val resourceClazz: Class<T>

    val size: Int

    fun get(): T

    fun recycle()
}