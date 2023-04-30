package me.gegenbauer.catspy.concurrency

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class PriorityThreadFactory(private val name: String, private val priority: Int): ThreadFactory {

    private val number = AtomicInteger()

    override fun newThread(r: Runnable) = object : Thread(r, "$name-$THREAD_NAME-${number.getAndIncrement()}") {
        override fun run() {
            priority = this@PriorityThreadFactory.priority
            super.run()
        }
    }

    private companion object {
        private const val THREAD_NAME = "LowPriorityThread"
    }
}