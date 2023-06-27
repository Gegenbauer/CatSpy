package me.gegenbauer.catspy.script.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.common.ui.card.Card
import me.gegenbauer.catspy.common.ui.card.RoundedCard
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.script.ScriptExecutor
import me.gegenbauer.catspy.script.model.Script
import me.gegenbauer.catspy.script.parser.Rule
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.TaskManager
import java.awt.GridLayout
import javax.swing.JLabel

class ScriptCard(
    private val taskManager: TaskManager,
    private val script: Script,
    private val parseRule: Rule
) : RoundedCard(), Card {
    override val id: Int = 1 // TODO generate id
    override val component: RoundedCard = this

    private val scope = ModelScope()
    private val titleLabel = JLabel() withName "titleLabel"
    private val contentLabel = BorderedTextArea()
    private val actionBar = ScriptActionBar()


    private var period: Long = 1000L
    private val periodUpdateTask = PeriodicTask(period, "UpdateCard", ::updateContent, Dispatchers.IO)

    /**
     * titleLabel 和 contentLabel 分两行
     */
    init {
        withName("ScriptCard")
        layout = GridLayout(3, 0)
        titleLabel.text = script.name
        contentLabel.text = "No output yet"
        add(actionBar)
        add(titleLabel)
        add(contentLabel)
    }

    override fun updateContent() {
        scope.launch {
            updateContentInternal()
        }
    }

    private suspend fun updateContentInternal() {
        val scriptExecutor = ScriptExecutor(taskManager, script.sourceCode)
        val output = scriptExecutor.executeAndGetResult()
        val parsedOutput = parseRule.parse(output)
        withContext(Dispatchers.UI) {
            contentLabel.text = parsedOutput.joinToString("\n")
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