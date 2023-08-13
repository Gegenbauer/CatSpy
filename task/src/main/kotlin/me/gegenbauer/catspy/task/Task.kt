package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineScope

interface Task {
    val name: String

    val scope: CoroutineScope

    val isCanceled: Boolean

    val isRunning: Boolean

    fun start()

    fun pause()

    fun resume()

    fun cancel()

    fun addListener(taskListener: TaskListener)

    fun removeListener(taskListener: TaskListener)
}