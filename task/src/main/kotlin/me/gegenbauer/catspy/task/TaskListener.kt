package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineDispatcher

interface TaskListener {
    val dispatcher: CoroutineDispatcher?
        get() = null
    
    fun onStart(task: Task) {
        // empty implementation
    }

    fun onStop(task: Task) {
        // empty implementation
    }

    fun onPause(task: Task) {
        // empty implementation
    }

    fun onResume(task: Task) {
        // empty implementation
    }

    fun onCancel(task: Task) {
        // empty implementation
    }

    fun onProgress(task: Task, data: Any) {
        // empty implementation
    }

    fun onRepeat(task: Task) {
        // empty implementation
    }

    fun onFinalResult(task: Task, data: Any) {
        // empty implementation
    }

    fun onError(task: Task, t: Throwable) {
        // empty implementation
    }
}