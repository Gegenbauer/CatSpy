package me.gegenbauer.catspy.ui.log

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.button.TableBarButton
import me.gegenbauer.catspy.utils.applyTooltip
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
    }

    override fun updateTableBar() {
        super.updateTableBar()
        ctrlMainPanel.add(windowedModeBtn)
        ctrlMainPanel.updateUI()
    }

    override fun onListSelectionChanged(event: ListSelectionEvent) {
        super.onListSelectionChanged(event)
        if (table.selectedRow == table.rowCount - 1) {
            setGoToLast(true)
        }
    }

    private inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            val mainUI = DarkUIUtil.getParentOfType(this@FullLogPanel, LogMainUI::class.java)
            when (event.source) {
                windowedModeBtn -> {
                    mainUI.windowedModeLogPanel(this@FullLogPanel)
                }
            }
        }
    }
}