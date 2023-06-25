package me.gegenbauer.catspy.ui.script

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.script.model.Script
import me.gegenbauer.catspy.script.model.ScriptType
import me.gegenbauer.catspy.script.parse.DirectRule
import me.gegenbauer.catspy.script.parse.RegexRule
import me.gegenbauer.catspy.task.TaskManager
import me.gegenbauer.catspy.ui.card.ScriptCard
import me.gegenbauer.catspy.ui.tab.OnTabChangeListener
import java.awt.BorderLayout
import javax.swing.JPanel

class ScriptMainUI(override val contexts: Contexts = Contexts.default) : JPanel(), Context, OnTabChangeListener {

    private val taskManager = TaskManager()
    private val cardContainer = ScriptCardContainer()

    private val focusedActivityParseRule = RegexRule("mCurrentFocus=Window\\{[0-9a-z]+ [0-9a-z]+ (.*)\\}", DirectRule())
    private val focusedActivityParseRule2 = RegexRule("mCurrentFocus=Window\\{[0-9a-z]+ [0-9a-z]+ (.*)\\}", DirectRule())
    private val getFocusedActivityScript = Script(
"GetFocusedActivity",
        ScriptType.adb,
        "adb shell dumpsys activity activities"
    )
    private val focusedActivityCard = ScriptCard(taskManager, getFocusedActivityScript, focusedActivityParseRule)
    private val focusedActivityCard2 = ScriptCard(taskManager, getFocusedActivityScript, focusedActivityParseRule2)

    init {
        componentName = "ScriptMainUI"
        layout = BorderLayout()
        add(cardContainer.container, BorderLayout.NORTH)

        cardContainer.addCard(focusedActivityCard)
        cardContainer.addCard(focusedActivityCard2)

        //cardContainer.setAutomaticallyUpdate(true)
    }

    private fun updateCardContent() {
        focusedActivityCard.updateContent()
    }

    override fun onTabFocusChanged(focused: Boolean) {
        taskManager.updatePauseState(!focused)
    }

}