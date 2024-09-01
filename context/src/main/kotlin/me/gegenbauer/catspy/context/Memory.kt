package me.gegenbauer.catspy.context

import me.gegenbauer.catspy.concurrency.Event
import me.gegenbauer.catspy.file.GB

data class Memory(
    val allocated: Float,
    val free: Float,
    val total: Float,
    val max: Float,
): Event {

    val freePercentage: Float
        get() = free / max

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