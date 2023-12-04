package me.gegenbauer.catspy.task

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class TaskManagerImpl: TaskManager {
    private val taskList = CopyOnWriteArrayList<Task>()
    private var paused = false
    private val taskListeners = ConcurrentHashMap<TaskListener, (Task) -> Boolean>()

    override fun onStart(task: Task) {
        TaskLog.d(TAG, "[onStart] $task")
    }

    override fun onStop(task: Task) {
        TaskLog.d(TAG, "[onStop] $task")
        taskList.remove(task)
    }

    override fun onCancel(task: Task) {
        TaskLog.d(TAG, "[onCancel] $task")
    }

    override fun onResume(task: Task) {
        TaskLog.d(TAG, "[onResume] $task")
    }

    override fun onPause(task: Task) {
        TaskLog.d(TAG, "[onPause] $task")
    }

    override fun onError(task: Task, t: Throwable) {
        TaskLog.e(TAG, "[onError] $task", t)
    }

    override fun exec(task: Task) {
        taskList.add(task)
        taskListeners.toList().filter { it.second(task) }.forEach { task.addListener(it.first) }
        updateTaskPauseState(task, paused)
        task.addListener(this)
        task.start()
    }

    override fun cancelAll(taskMatcher: (Task) -> Boolean) {
        taskList.filter(taskMatcher).forEach(Task::cancel)
    }

    override fun cancelAll() {
        taskList.forEach(Task::cancel)
    }

    override fun addListener(taskListener: TaskListener, taskMatcher: (Task) -> Boolean) {
        taskListeners[taskListener] = taskMatcher
        taskList.filter(taskMatcher).forEach { it.addListener(taskListener) }
    }

    override fun addListener(taskListener: TaskListener) {
        addListener(taskListener) { true }
    }

    override fun removeListener(taskListener: TaskListener, taskMatcher: (Task) -> Boolean) {
        taskListeners.remove(taskListener)
        taskList.filter(taskMatcher).forEach { it.removeListener(taskListener) }
    }

    override fun removeListener(taskListener: TaskListener) {
        removeListener(taskListener) { true }
    }

    override fun updatePauseState(paused: Boolean) {
        this.paused = paused
        taskList.forEach { updateTaskPauseState(it, paused) }
    }

    override fun isIdle(): Boolean {
        return taskList.all { it.isRunning.not() }
    }

    override fun isPaused(): Boolean {
        return paused
    }

    override fun isRunning(): Boolean {
        return taskList.any { it.isRunning }
    }

    override fun isAnyTaskRunning(taskMatcher: (Task) -> Boolean): Boolean {
        return taskList.filter(taskMatcher).any { it.isRunning }
    }

    private fun updateTaskPauseState(task: Task, paused: Boolean) {
        if (paused) {
            task.pause()
        } else {
            task.resume()
        }
    }

    companion object {
        private const val TAG = "TaskManager"
    }
}