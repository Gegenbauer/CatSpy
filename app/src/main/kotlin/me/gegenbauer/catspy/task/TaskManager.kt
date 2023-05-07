package me.gegenbauer.catspy.task

class TaskManager : TaskListener {
    private val taskList = mutableListOf<Task>()

    override fun onStop(task: Task) {
        taskList.remove(task)
    }

    fun exec(task: Task) {
        taskList.add(task)
        task.addListener(this)
        task.start()
    }

    fun cancelAll() {
        taskList.forEach(Task::stop)
    }

    fun addListener(taskListener: TaskListener, taskMatch: (Task) -> Boolean = { _ -> true }) {
        taskList.filter(taskMatch).forEach { it.addListener(taskListener) }
    }

    fun removeListener(taskListener: TaskListener, taskMatch: (Task) -> Boolean = { _ -> true }) {
        taskList.filter(taskMatch).forEach { it.removeListener(taskListener) }
    }
}