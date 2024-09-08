package me.gegenbauer.catspy.context

import me.gegenbauer.catspy.concurrency.Event
import me.gegenbauer.catspy.file.GB

data class Memory(
    val jvm: JvmMemory,
    val device: DeviceMemory,
): Event {

    val jvmFreePercentage: Float
        get() = jvm.freePercentage

    fun isEmpty(): Boolean = this == EMPTY

    companion object {
        val EMPTY = Memory(
            JvmMemory(),
            DeviceMemory()
        )
    }
}

data class JvmMemory(
    val allocated: Float = 0f,
    val free: Float = 0f,
    val total: Float = 0f,
    val max: Float = 0f,
) {
    val freePercentage: Float
        get() = free / max

    private fun toGB(): JvmMemory  = JvmMemory(
        allocated = allocated / GB,
        free = free / GB,
        total = total / GB,
        max = max / GB
    )

    fun readable(): String {
        val readableMemory = toGB()
        return "%.2f GB / %.2f GB".format(readableMemory.allocated, readableMemory.max)
    }

    fun isNotEmpty(): Boolean = this != EMPTY

    companion object {
        val EMPTY = JvmMemory()
    }
}

data class DeviceMemory(
    val rss: Float = 0f,
    val vss: Float = 0f,
) {

    private fun toGB(): DeviceMemory  = DeviceMemory(
        rss = rss / GB,
        vss = vss / GB,
    )

    fun readable(): String {
        val readableMemory = toGB()
        return "rss: %.2f GB / vss: %.2f GB".format(readableMemory.rss, readableMemory.vss)
    }

    fun isNotEmpty(): Boolean = this != EMPTY

    companion object {
        val EMPTY = DeviceMemory()
    }
}