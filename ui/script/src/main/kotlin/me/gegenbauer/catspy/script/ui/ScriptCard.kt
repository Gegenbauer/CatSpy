package me.gegenbauer.catspy.script.ui

import com.malinskiy.adam.request.device.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.script.executor.CommandExecutor
import me.gegenbauer.catspy.script.ui.ScriptTabPanel.Companion.defaultDevice
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.TaskManager
import me.gegenbauer.catspy.view.card.Card
import me.gegenbauer.catspy.view.card.RoundedCard
import java.awt.GridLayout
import javax.swing.JLabel

class ScriptCard(
    private val taskManager: TaskManager,
    private val scriptUIItem: ScriptUIItem,
    override val contexts: Contexts = Contexts.default
) : RoundedCard(), Card {
    override val id: Int = 1 // TODO generate id
    override val component: RoundedCard = this

    private val scope = ModelScope()
    private val titleLabel = JLabel() withName "titleLabel"
    private val contentLabel = BorderedTextArea()
    private val actionBar = ScriptActionBar()

    private var period: Long = 1000L
    private val periodUpdateTask = PeriodicTask(period, "UpdateCard", ::updateContent, Dispatchers.GIO)

    private val executor = CommandExecutor(taskManager, scriptUIItem.script, Dispatchers.GIO)

    var device: Device = defaultDevice

    /**
     * titleLabel 和 contentLabel 分两行
     */
    init {
        withName("ScriptCard")
        layout = GridLayout(3, 0)
        titleLabel.text = scriptUIItem.script.name
        contentLabel.text = "No output yet"
        add(actionBar)
        add(titleLabel)
        add(contentLabel)
    }

    override fun updateContent() {
        val deviceManager = ServiceManager.getContextService(AdamDeviceManager::class.java)
        if (deviceManager.getDevices().isNotEmpty()) {
            scope.launch {
                updateContentInternal()
            }
        }
    }

    private suspend fun updateContentInternal() {
        val scriptTabPanel = contexts.getContext(ScriptTabPanel::class.java)
        scriptTabPanel ?: return
        val response = executor.execute(device)
        response.collect {
            val parsedOutput = scriptUIItem.parseRule.parse(it.output)
            withContext(Dispatchers.UI) {
                contentLabel.text = parsedOutput.joinToString("\n")
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
}