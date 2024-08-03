package me.gegenbauer.catspy.utils

import java.util.concurrent.atomic.AtomicInteger

object IdGenerator {
    private val id = AtomicInteger(0)

    fun generateId(): Int = id.incrementAndGet()
}