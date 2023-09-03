package me.gegenbauer.catspy.script.task

import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.task.TaskManager
import me.gegenbauer.catspy.task.TaskManagerImpl

class ScriptTaskManager: TaskManager by TaskManagerImpl(), ContextService