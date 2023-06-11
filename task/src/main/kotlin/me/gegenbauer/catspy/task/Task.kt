package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineScope

interface Task {
    val name: String

    val scope: CoroutineScope

    fun start()

    fun pause()

    fun resume()

    fun cancel()

    fun isRunning(): Boolean

    fun addListener(taskListener: TaskListener)

    fun removeListener(taskListener: TaskListener)
}