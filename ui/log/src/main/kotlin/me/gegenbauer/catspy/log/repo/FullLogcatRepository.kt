package me.gegenbauer.catspy.log.repo

import me.gegenbauer.catspy.log.model.LogcatLogItem
import me.gegenbauer.catspy.task.PeriodicTask

class FullLogcatRepository(
    updateUITask: PeriodicTask
) : BaseLogcatRepository(updateUITask) {
    override fun filterRule(item: LogcatLogItem): Boolean {
        return true
    }
}