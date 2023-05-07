package me.gegenbauer.catspy.task

interface TaskListener {
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

    fun onFinalResult(task: Task, data: Any) {
        // empty implementation
    }
}