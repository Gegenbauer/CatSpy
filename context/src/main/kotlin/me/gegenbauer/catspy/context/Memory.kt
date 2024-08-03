package me.gegenbauer.catspy.context

import me.gegenbauer.catspy.file.GB
import me.gegenbauer.catspy.java.ext.Event

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