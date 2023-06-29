package me.gegenbauer.catspy.script.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.common.ui.card.Card
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.script.executor.CommandExecutor
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.TaskManager
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DeviceInfoCard(
    private val scriptUIItems: List<ScriptUIItem>,
    private val taskManager: TaskManager
) : JPanel(), Card {
    private val scope = ModelScope()
    private var device = ScriptMainUI.defaultDevice
    private var period: Long = 1000L
    private val periodUpdateTask = PeriodicTask(period, "UpdateCard", ::updateContent, Dispatchers.GIO)

    init {
        layout = GridLayout(0, 2)
        scriptUIItems.forEach {
            add(JLabel(it.script.name))
            add(JLabel())
        }
    }

    override val id: Int
        get() = 1
    override val component: JComponent
        get() = this

    override fun updateContent() {
        scriptUIItems.forEachIndexed { index, item ->
            scope.async {
                val executor = CommandExecutor(taskManager, item.script, Dispatchers.GIO)
                val response = executor.execute(device)
                response.collect {
                    withContext(Dispatchers.UI) {
                        val label = getComponent(index * 2 + 1) as JLabel
                        label.text = it.output
                    }
                }
            }
        }
    }

    override fun setAutomaticallyUpdate(enabled: Boolean) {
        if (enabled) {
            taskManager.exec(periodUpdateTask)
        } else {
            periodUpdateTask.cancel()
        }
    }

    override fun stopAutomaticallyUpdate() {
        periodUpdateTask.pause()
    }

    override fun resumeAutomaticallyUpdate() {
        periodUpdateTask.resume()
    }

    override fun setPeriod(period: Long) {
        this.period = period
    }

    // fun setInfo
}