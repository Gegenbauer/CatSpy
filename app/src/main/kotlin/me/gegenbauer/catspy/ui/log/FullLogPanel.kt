package me.gegenbauer.catspy.ui.log

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.command.CmdManager
import me.gegenbauer.catspy.manager.CustomListManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.ui.button.TableBarButton
import me.gegenbauer.catspy.utils.applyTooltip
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.event.ListSelectionEvent

class FullLogPanel(tableModel: LogTableModel, ) : LogPanel(tableModel) {

    private val windowedModeBtn = GButton(STRINGS.ui.windowedMode) applyTooltip STRINGS.toolTip.viewWindowedModeBtn
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

    private val customBtActionListener = ActionListener { event ->
        val cmd = CmdManager.replaceAdbCmdWithTargetDevice((event.source as TableBarButton).value)

        if (cmd.isNotEmpty()) {
            val runtime = Runtime.getRuntime()
            runtime.exec(cmd)
        }
    }

    override fun updateTableBar(customArray: ArrayList<CustomListManager.CustomElement>) {
        super.updateTableBar(customArray)
        ctrlMainPanel.add(windowedModeBtn)
        ctrlMainPanel.updateUI()
    }

    override fun onListSelectionChanged(event: ListSelectionEvent) {
        super.onListSelectionChanged(event)
        if (table.selectedRow == table.rowCount - 1) {
            setGoToLast(true)
        }
    }

    private inner class ActionHandler: ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            val mainUI = DarkUIUtil.getParentOfType(this@FullLogPanel, MainUI::class.java)
            when (event.source) {
                windowedModeBtn -> {
                    mainUI.windowedModeLogPanel(this@FullLogPanel)
                }
            }
        }
    }
}