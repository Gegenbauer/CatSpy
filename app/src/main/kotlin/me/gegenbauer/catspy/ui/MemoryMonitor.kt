package me.gegenbauer.catspy.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.java.ext.Event
import me.gegenbauer.catspy.file.GB
import kotlin.math.min

interface IMemoryMonitor {
    fun startMonitor(): Flow<Memory>

    fun calculateMemoryUsage(): Memory
}

data class Memory(
    val allocated: Float,
    val free: Float,
    val total: Float,
    val max: Float,
): Event {

    fun isEmpty(): Boolean = this == EMPTY

    private fun toGB(): Memory  = Memory(
        allocated = allocated / GB,
        free = free / GB,
        total = total / GB,
        max = max / GB
    )

    fun readable(): String {
        val readableMemory = toGB()
        return "%.2f GB / %.2f GB".format(readableMemory.allocated, readableMemory.max)
    }

    companion object {
        val EMPTY = Memory(0f, 0f, 0f, 0f)
    }
}

class MemoryMonitor(private val dispatcher: CoroutineDispatcher = Dispatchers.GIO) : IMemoryMonitor {

    override fun startMonitor(): Flow<Memory> {
        return channelFlow {
            while (true) {
                send(calculateMemoryUsage())
                delay(MEMORY_INFO_REFRESH_INTERVAL)
            }
        }.flowOn(dispatcher)
    }

    override fun calculateMemoryUsage(): Memory {
        val runtime = Runtime.getRuntime()
        return Memory(
            (runtime.totalMemory() - runtime.freeMemory()).toFloat(),
            runtime.freeMemory().toFloat(),
            runtime.totalMemory().toFloat(),
            runtime.maxMemory().toFloat()
        )
    }

    companion object {

        fun isFreeMemoryAvailable(): Boolean {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalFree = runtime.freeMemory() + (maxMemory - runtime.totalMemory())
            return totalFree > minFreeMemory
        }

        val minFreeMemory: Double = run {
            val runtime = Runtime.getRuntime()
            val minFree = (runtime.maxMemory() * MIN_FREE_MEMORY_PERCENTAGE).toLong()
            min(minFree.toDouble(), MIN_FREE_MEMORY.toDouble())
        }

        private const val MIN_FREE_MEMORY = 512 * 1024L * 1024L
        private const val MIN_FREE_MEMORY_PERCENTAGE = 0.2
        private const val MEMORY_INFO_REFRESH_INTERVAL = 1000L
    }
}