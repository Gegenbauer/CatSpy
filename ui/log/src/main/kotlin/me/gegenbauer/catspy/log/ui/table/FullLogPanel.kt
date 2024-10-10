package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.event.FullLogWindowModeChangedEvent
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.event.EventManager
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.view.button.TableBarButton
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JLabel
import javax.swing.event.ListSelectionEvent

class FullLogPanel(
    tableModel: LogTableModel,
    contexts: Contexts = Contexts.default
) : LogPanel(tableModel, contexts) {

    private val windowedModeBtn = TableBarButton(STRINGS.ui.windowedMode) applyTooltip
            STRINGS.toolTip.viewWindowedModeBtn
    private val actionHandler = ActionHandler()
    private val eventManager: EventManager
        get() = kotlin.run {
            val logMainPanel = contexts.getContext(BaseLogMainPanel::class.java)
                ?: error("No BaseLogMainPanel found in contexts")
            ServiceManager.getContextService(logMainPanel, EventManager::class.java)
        }

    var isWindowedMode = false
        set(value) {
            field = value
            windowedModeBtn.isEnabled = !value
        }

    init {
        windowedModeBtn.margin = buttonMargin
        windowedModeBtn.addActionListener(actionHandler)
        createUI()
        bind(binding)
    }

    override fun createTableBar() {
        ctrlMainPanel.add(JLabel(STRINGS.ui.fullLog))
        super.createTableBar()
        ctrlMainPanel.add(windowedModeBtn)
        ctrlMainPanel.updateUI()
    }

    override fun valueChanged(event: ListSelectionEvent) {
        super.valueChanged(event)
        if (table.isLastRowSelected()) {
            setGoToLast(true)
        }
    }

    private inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                windowedModeBtn -> {
                    eventManager.publish(FullLogWindowModeChangedEvent(!isWindowedMode))
                }
            }
        }
    }
}
