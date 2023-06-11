package me.gegenbauer.catspy.data.repo.log

import me.gegenbauer.catspy.data.model.log.LogcatLogItem
import me.gegenbauer.catspy.task.PeriodicTask

class FullLogcatRepository(
    updateUITask: PeriodicTask
) : BaseLogcatRepository(updateUITask) {
    override fun filterRule(item: LogcatLogItem): Boolean {
        return true
    }
}