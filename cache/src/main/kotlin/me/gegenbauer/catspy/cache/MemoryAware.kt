package me.gegenbauer.catspy.cache

interface MemoryAware {
    fun onTrimMemory(level: Int)
}