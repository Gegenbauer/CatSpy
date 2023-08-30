package me.gegenbauer.catspy.script.ui

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.script.executor.CommandExecutor
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.TaskManager
import me.gegenbauer.catspy.view.card.Card
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DeviceInfoCard(
    private val scriptUIItems: List<ScriptUIItem>,
    private val taskManager: TaskManager,
    override val contexts: Contexts = Contexts.default
) : JPanel(), Card {
    private val scope = ModelScope()
    private var device = ScriptTabPanel.defaultDevice
    private var period: Long = 1000L
    private val periodUpdateTask = PeriodicTask(period, "UpdateCard", ::updateContent, Dispatchers.GIO)

    init {
        val rows = DoubleArray(scriptUIItems.size) { TableLayoutConstants.PREFERRED }
        layout = TableLayout(
            doubleArrayOf(0.2, 0.8),
            rows
        )
        scriptUIItems.forEachIndexed { index, it ->
            add(JLabel(it.script.name), getTableConstraint(index, 0))
            add(JLabel(), getTableConstraint(index, 1))
        }
    }

    private fun getTableConstraint(row: Int, column: Int): String {
        return "$column, $row"
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
                    val parsedResult = item.parseRule.parse(it.output)
                    withContext(Dispatchers.UI) {
                        val label = getComponent(index * 2 + 1) as JLabel
                        label.text = parsedResult.firstOrNull() ?: ""
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