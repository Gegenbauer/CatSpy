package me.gegenbauer.catspy.task

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

open class TaskManager : TaskListener {
    private val taskList = CopyOnWriteArrayList<Task>()
    private var paused = false
    private val taskListeners = ConcurrentHashMap<TaskListener, (Task) -> Boolean>()

    override fun onStop(task: Task) {
        taskList.remove(task)
    }

    fun exec(task: Task) {
        taskList.add(task)
        taskListeners.toList().filter { it.second(task) }.forEach { task.addListener(it.first) }
        updateTaskPauseState(task, paused)
        task.addListener(this)
        task.start()
    }

    fun cancelAll() {
        taskList.forEach(Task::cancel)
    }

    fun addListener(taskListener: TaskListener, taskMatcher: (Task) -> Boolean = { _ -> true }) {
        taskListeners[taskListener] = taskMatcher
        taskList.filter(taskMatcher).forEach { it.addListener(taskListener) }
    }

    fun removeListener(taskListener: TaskListener, taskMatch: (Task) -> Boolean = { _ -> true }) {
        taskListeners.remove(taskListener)
        taskList.filter(taskMatch).forEach { it.removeListener(taskListener) }
    }

    fun updatePauseState(paused: Boolean) {
        taskList.forEach { updateTaskPauseState(it, paused) }
        this.paused = paused
    }

    private fun updateTaskPauseState(task: Task, paused: Boolean) {
        if (paused) {
            task.pause()
        } else {
            task.resume()
        }
    }
}