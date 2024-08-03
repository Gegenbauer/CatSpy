package me.gegenbauer.catspy.log.ui.table

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.view.button.TableBarButton
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.event.ListSelectionEvent

class FullLogPanel(
    tableModel: LogTableModel,
    contexts: Contexts = Contexts.default
) : LogPanel(tableModel, contexts) {

    private val windowedModeBtn =
        TableBarButton(STRINGS.ui.windowedMode) applyTooltip STRINGS.toolTip.viewWindowedModeBtn
    private val actionHandler = ActionHandler()

    var isWindowedMode = false
        set(value) {
            field = value
            windowedModeBtn.isEnabled = !value
        }

    init {
        windowedModeBtn.margin = Insets(0, 3, 0, 3)
        windowedModeBtn.addActionListener(actionHandler)
        createUI()
        bind(binding)
    }

    override fun updateTableBar() {
        super.updateTableBar()
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
            val mainUI = DarkUIUtil.getParentOfType(this@FullLogPanel, BaseLogMainPanel::class.java)
            when (event.source) {
                windowedModeBtn -> {
                    mainUI.showLogPanelInWindow(this@FullLogPanel)
                }
            }
        }
    }
}
