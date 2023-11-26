package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.configuration.Menu
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.GlobalStrings
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.MainViewModel
import me.gegenbauer.catspy.ui.dialog.AboutDialog
import me.gegenbauer.catspy.ui.dialog.HelpDialog
import me.gegenbauer.catspy.utils.MENU_ICON_SIZE
import me.gegenbauer.catspy.utils.findFrameFromParent
import me.gegenbauer.catspy.view.dialog.FileSaveHandler
import me.gegenbauer.catspy.view.menu.GMenu
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.JMenuItem

class HelpMenu(override val contexts: Contexts = Contexts.default) : GMenu(), Context {
    private val itemHelp = JMenuItem(STRINGS.ui.menuHelp).apply {
        icon = GIcons.Menu.Help.get(Menu.MENU_ITEM_ICON_SIZE, Menu.MENU_ITEM_ICON_SIZE)
    }
    private val itemExportLog = JMenuItem(STRINGS.ui.menuExportLog)
    private val itemCheckUpdates = JMenuItem(STRINGS.ui.menuCheckUpdates)
    private val itemAbout = JMenuItem(STRINGS.ui.menuAbout).apply {
        icon = GIcons.Menu.About.get(MENU_ICON_SIZE, MENU_ICON_SIZE)
    }
    private val actionHandler = ActionListener {
        when (it.source) {
            itemHelp -> openHelpDialog()
            itemExportLog -> exportLog()
            itemCheckUpdates -> checkUpdates()
            itemAbout -> openAboutDialog()
        }
    }

    init {
        text = STRINGS.ui.menuHelp
        mnemonic = KeyEvent.VK_H

        add(itemHelp)
        add(itemExportLog)
        addSeparator()
        add(itemCheckUpdates)
        add(itemAbout)

        itemHelp.addActionListener(actionHandler)
        itemExportLog.addActionListener(actionHandler)
        itemCheckUpdates.addActionListener(actionHandler)
        itemAbout.addActionListener(actionHandler)
    }

    private fun openHelpDialog() {
        val frame = findFrameFromParent<JFrame>()
        val helpDialog = HelpDialog(frame)
        helpDialog.setLocationRelativeTo(frame)
        helpDialog.isVisible = true
    }

    private fun exportLog() {
        val mainFrame = contexts.getContext(MainFrame::class.java)
        mainFrame?.let { frame ->
            val vm = ServiceManager.getContextService(frame, MainViewModel::class.java)
            FileSaveHandler.Builder(frame)
                .onFileSpecified(vm::exportLog)
                .setDefaultName(GlobalStrings.LOG_NAME)
                .build()
                .show()
        }
    }

    private fun openAboutDialog() {
        val frame = findFrameFromParent<JFrame>()
        val aboutDialog = AboutDialog(frame)
        aboutDialog.setLocationRelativeTo(frame)
        aboutDialog.isVisible = true
    }

    private fun checkUpdates() {
        val mainFrame = contexts.getContext(MainFrame::class.java)
        mainFrame?.let { frame ->
            val vm = ServiceManager.getContextService(frame, MainViewModel::class.java)
            vm.checkUpdate(true)
        }
    }
}

