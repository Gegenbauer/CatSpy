package me.gegenbauer.catspy.task

import java.util.concurrent.CopyOnWriteArrayList

class TaskManager : TaskListener {
    private val taskList = CopyOnWriteArrayList<Task>()
    private var paused = false

    override fun onStop(task: Task) {
        taskList.remove(task)
    }

    fun exec(task: Task) {
        taskList.add(task)
        updateTaskPauseState(task, paused)
        task.addListener(this)
        task.start()
    }

    fun cancelAll() {
        taskList.forEach(Task::cancel)
    }

    fun addListener(taskListener: TaskListener, taskMatch: (Task) -> Boolean = { _ -> true }) {
        taskList.filter(taskMatch).forEach { it.addListener(taskListener) }
    }

    fun removeListener(taskListener: TaskListener, taskMatch: (Task) -> Boolean = { _ -> true }) {
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