package me.gegenbauer.catspy.log.task

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.task.TaskManager
import me.gegenbauer.catspy.task.TaskManagerImpl

class LogTaskManager: TaskManager by TaskManagerImpl(), ContextService {

    override fun onContextDestroyed(context: Context) {
        cancelAll()
    }
}