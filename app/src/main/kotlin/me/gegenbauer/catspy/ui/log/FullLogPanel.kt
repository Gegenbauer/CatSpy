package me.gegenbauer.catspy.ui.log

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.command.CmdManager
import me.gegenbauer.catspy.manager.CustomListManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.ui.button.TableBarButton
import me.gegenbauer.catspy.utils.addVSeparator1
import me.gegenbauer.catspy.utils.applyTooltip
import me.gegenbauer.catspy.utils.loadIcon
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.event.ListSelectionEvent

class FullLogPanel(
    mainUI: MainUI,
    tableModel: LogTableModel,
) : LogPanel(mainUI, tableModel) {

    private val windowedModeBtn = GButton(STRINGS.ui.windowedMode) applyTooltip STRINGS.toolTip.viewWindowedModeBtn
    private val cmdManager = CmdManager(mainUI, this)
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

    private fun updateTableBarCommands(customArray: ArrayList<CustomListManager.CustomElement>) {

        ctrlMainPanel.add(getCustomActionButton(customArray))
        val icon = loadIcon<DerivableImageIcon>("filterscmdsitem.png")
        customArray.filter { it.tableBar }.forEach { item ->
            val button = TableBarButton(item.title) applyTooltip "${item.title} : ${item.value}"
            button.icon = icon
            button.value = item.value
            button.margin = Insets(0, 3, 0, 3)
            button.addActionListener(customBtActionListener)
            ctrlMainPanel.add(button)
        }
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
        ctrlMainPanel.addVSeparator1(10)
        updateTableBarCommands(customArray)
        ctrlMainPanel.updateUI()
    }

    override fun getCustomActionButton(customArray: ArrayList<CustomListManager.CustomElement>): TableBarButton {
        val cmdsBtn = TableBarButton(STRINGS.ui.commands) applyTooltip STRINGS.toolTip.addCmdBtn
        cmdsBtn.icon = loadIcon("filterscmds.png")
        cmdsBtn.margin = Insets(0, 3, 0, 3)
        cmdsBtn.addActionListener {
            cmdManager.showDialog()
        }
        return cmdsBtn
    }

    override fun onListSelectionChanged(event: ListSelectionEvent) {
        super.onListSelectionChanged(event)
        if (table.selectedRow == table.rowCount - 1) {
            setGoToLast(true)
        }
    }

    private inner class ActionHandler: ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                windowedModeBtn -> {
                    mainUI.windowedModeLogPanel(this@FullLogPanel)
                }
            }
        }
    }
}