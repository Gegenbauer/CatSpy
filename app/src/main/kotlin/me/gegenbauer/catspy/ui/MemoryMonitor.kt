package me.gegenbauer.catspy.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.context.Memory
import kotlin.math.min

interface IMemoryMonitor {
    fun startMonitor(): Flow<Memory>

    fun calculateMemoryUsage(): Memory
}

/**
 * todo 内存监控，根据内存情况，清理内存中的日志，优先清理文件日志，再清理设备实时日志。
 */
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
        private const val MEMORY_INFO_REFRESH_INTERVAL = 300L
    }
}