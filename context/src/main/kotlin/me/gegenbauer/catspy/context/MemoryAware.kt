package me.gegenbauer.catspy.context

import me.gegenbauer.catspy.glog.GLog
import java.lang.ref.WeakReference

interface MemoryAware {
    fun onTrimMemory(level: Level) {}

    enum class Level(val freeMemoryPercentage: Float, val changeBuffer: Float) {
        LOW(
            0.6f,
            0.2f
        ),
        MEDIUM(
            0.3f,
            0.1f
        ),
        HIGH(
            0.1f,
            0.03f
        ),
        CRITICAL(
            0.05f,
            0.01f
        )
    }
}

object MemoryState {
    private const val TAG = "MemoryState"

    private val memoryAwareComponents = mutableListOf<WeakReference<MemoryAware>>()
    private var lastMemory = Memory.EMPTY
    private val lock = Any()

    fun register(component: MemoryAware) {
        synchronized(lock) {
            memoryAwareComponents.add(WeakReference(component))
        }
    }

    fun onMemoryChanged(memory: Memory) {
        synchronized(lock) {
            if (memory.isEmpty()) {
                return
            }
            checkAndNotifyMemoryAwareComponents(lastMemory, memory)
            lastMemory = memory
        }
    }

    fun forceTrimMemory() {
        synchronized(lock) {
            notifyMemoryAwareComponents(MemoryAware.Level.HIGH)
        }
    }

    private fun checkAndNotifyMemoryAwareComponents(oldMemory: Memory, newMemory: Memory) {
        val oldMemoryLevel = getMemoryLevel(oldMemory)
        val newMemoryLevel = getMemoryLevel(newMemory)
        if (oldMemoryLevel >= newMemoryLevel) {
            return
        }
        if (newMemory.freePercentage - newMemoryLevel.freeMemoryPercentage < newMemoryLevel.changeBuffer) {
            return
        }
        notifyMemoryAwareComponents(newMemoryLevel)
    }

    private fun notifyMemoryAwareComponents(level: MemoryAware.Level) {
        GLog.i(TAG, "[notifyMemoryAwareComponents] Notify memory aware components of level $level")
        memoryAwareComponents.forEach { it.get()?.onTrimMemory(level) }
    }

    private fun getMemoryLevel(memory: Memory): MemoryAware.Level {
        val freePercentage = memory.free / memory.total
        return when {
            freePercentage < MemoryAware.Level.CRITICAL.freeMemoryPercentage -> MemoryAware.Level.CRITICAL
            freePercentage < MemoryAware.Level.HIGH.freeMemoryPercentage -> MemoryAware.Level.HIGH
            freePercentage < MemoryAware.Level.MEDIUM.freeMemoryPercentage -> MemoryAware.Level.MEDIUM
            freePercentage < MemoryAware.Level.LOW.freeMemoryPercentage -> MemoryAware.Level.LOW
            else -> MemoryAware.Level.LOW
        }
    }
}