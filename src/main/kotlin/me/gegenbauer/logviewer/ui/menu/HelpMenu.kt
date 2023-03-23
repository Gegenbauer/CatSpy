package me.gegenbauer.logviewer.ui.menu

import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.ui.dialog.AboutDialog
import me.gegenbauer.logviewer.ui.dialog.HelpDialog
import me.gegenbauer.logviewer.utils.findFrameFromParent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.JMenu
import javax.swing.JMenuItem

class HelpMenu : JMenu() {
    private val itemHelp = JMenuItem(STRINGS.ui.help)
    private val itemAbout = JMenuItem(STRINGS.ui.about)
    private val actionHandler = ActionListener {
        when (it.source) {
            itemHelp -> openHelpDialog()
            itemAbout -> openAboutDialog()
        }
    }

    init {
        text = STRINGS.ui.help
        mnemonic = KeyEvent.VK_H

        itemHelp.addActionListener(actionHandler)
        itemAbout.addActionListener(actionHandler)

        add(itemHelp)
        addSeparator()
        add(itemAbout)
    }

    private fun openHelpDialog() {
        val frame = findFrameFromParent(this)
        val helpDialog = HelpDialog(frame)
        helpDialog.setLocationRelativeTo(frame)
        helpDialog.isVisible = true
    }

    private fun openAboutDialog() {
        val frame = findFrameFromParent(this)
        val aboutDialog = AboutDialog(frame)
        aboutDialog.setLocationRelativeTo(frame)
        aboutDialog.isVisible = true
    }
}

