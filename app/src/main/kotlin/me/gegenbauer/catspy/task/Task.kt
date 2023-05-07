package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineScope
import me.gegenbauer.catspy.log.GLog

interface Task {
    val name: String

    val scope: CoroutineScope

    fun start()

    fun pause() {
        GLog.d(name, "[pause]")
    }

    fun resume() {
        GLog.d(name, "[resume]")
    }

    fun stop()

    fun addListener(taskListener: TaskListener)

    fun removeListener(taskListener: TaskListener)
}