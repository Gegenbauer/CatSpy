package me.gegenbauer.catspy.task

interface TaskManager : TaskListener {
    fun exec(task: Task)

    fun cancelAll(taskMatcher: (Task) -> Boolean)

    fun cancelAll()

    fun addListener(taskListener: TaskListener, taskMatcher: (Task) -> Boolean)

    fun addListener(taskListener: TaskListener)

    fun removeListener(taskListener: TaskListener, taskMatcher: (Task) -> Boolean)

    fun removeListener(taskListener: TaskListener)

    fun updatePauseState(paused: Boolean)

    fun isPaused(): Boolean

    fun isIdle(): Boolean

    fun isRunning(): Boolean

    fun isAnyTaskRunning(taskMatcher: (Task) -> Boolean): Boolean
}