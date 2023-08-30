package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.configuration.Menu
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.dialog.AboutDialog
import me.gegenbauer.catspy.ui.dialog.HelpDialog
import me.gegenbauer.catspy.utils.findFrameFromParent
import me.gegenbauer.catspy.view.menu.GMenu
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.JMenuItem

class HelpMenu : GMenu() {
    private val itemHelp = JMenuItem(STRINGS.ui.help).apply {
        icon = GIcons.Menu.Help.get(Menu.MENU_ITEM_ICON_SIZE, Menu.MENU_ITEM_ICON_SIZE)
    }
    private val itemAbout = JMenuItem(STRINGS.ui.about).apply {
        icon = GIcons.Menu.About.get(14, 14)
    }
    private val actionHandler = ActionListener {
        when (it.source) {
            itemHelp -> openHelpDialog()
            itemAbout -> openAboutDialog()
        }
    }

    init {
        text = STRINGS.ui.help
        mnemonic = KeyEvent.VK_H

        add(itemHelp)
        addSeparator()
        add(itemAbout)

        itemHelp.addActionListener(actionHandler)
        itemAbout.addActionListener(actionHandler)
    }

    private fun openHelpDialog() {
        val frame = findFrameFromParent<JFrame>()
        val helpDialog = HelpDialog(frame)
        helpDialog.setLocationRelativeTo(frame)
        helpDialog.isVisible = true
    }

    private fun openAboutDialog() {
        val frame = findFrameFromParent<JFrame>()
        val aboutDialog = AboutDialog(frame)
        aboutDialog.setLocationRelativeTo(frame)
        aboutDialog.isVisible = true
    }
}

