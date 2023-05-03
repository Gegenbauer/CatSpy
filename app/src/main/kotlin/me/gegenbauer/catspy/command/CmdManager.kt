package me.gegenbauer.catspy.command

import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.CustomListManager
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.log.LogPanel
import me.gegenbauer.catspy.utils.isDoubleClick
import java.awt.event.*
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

class CmdManager(mainUI: MainUI, logPanel: LogPanel) : CustomListManager(mainUI, logPanel) {
    private val listSelectionHandler = ListSelectionHandler()
    private val mouseHandler = MouseHandler()
    private val keyHandler = KeyHandler()

    override val dialogTitle: String = "Cmd Manager"

    override fun loadList(): ArrayList<CustomElement> {
        return ArrayList(UIConfManager.uiConf.commands)
    }

    override fun saveList(list: ArrayList<CustomElement>) {
        UIConfManager.uiConf.commands.clear()
        UIConfManager.uiConf.commands.addAll(list)
        UIConfManager.saveUI()
    }

    override fun getFirstElement(): CustomElement {
        return CustomElement("Example", "adb shell input key event POWER", false)
    }

    override fun getListSelectionListener(): ListSelectionListener {
        return listSelectionHandler
    }

    override fun getListMouseListener(): MouseListener {
        return mouseHandler
    }

    override fun getListKeyListener(): KeyListener {
        return keyHandler
    }

    private fun runCmd(list: JList<CustomElement>) {
        val selection = list.selectedValue
        val cmd = replaceAdbCmdWithTargetDevice(selection.value)
        if (cmd.isNotEmpty()) {
            val ret = JOptionPane.showConfirmDialog(
                list,
                "Run : $cmd",
                "Run command",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            )
            if (ret == JOptionPane.OK_OPTION) {
                val runtime = Runtime.getRuntime()
                runtime.exec(cmd)
            }
        }
    }

    internal inner class ListSelectionHandler : ListSelectionListener {
        override fun valueChanged(e: ListSelectionEvent) {
            GLog.d(TAG, "Not implemented")
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal inner class MouseHandler : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            super.mouseClicked(event)
            if (event.isDoubleClick) {
                val list = event.source as JList<CustomElement>
                runCmd(list)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal inner class KeyHandler : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            if (event.keyCode == KeyEvent.VK_ENTER) {
                val list = event.source as JList<CustomElement>
                runCmd(list)
            }
        }
    }

    companion object {
        private const val TAG = "CmdManager"
        const val MAX_CMD_COUNT = 20

        fun replaceAdbCmdWithTargetDevice(cmd: String): String {
            return if (cmd.startsWith("adb ")) {
                cmd.replaceFirst("adb ", "${LogCmdManager.adbCmd} -s ${LogCmdManager.targetDevice} ")
            } else if (cmd.startsWith("adb.exe ")) {
                cmd.replaceFirst("adb.exe ", "${LogCmdManager.adbCmd} -s ${LogCmdManager.targetDevice} ")
            } else {
                cmd
            }
        }
    }
}
