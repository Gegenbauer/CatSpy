package me.gegenbauer.logviewer.ui.dialog

import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.button.GButton
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel

class ScrollBackSettingsDialog(parent: MainUI) : JDialog(parent, "ScrollBack settings", true), ActionListener {
    private var okBtn: JButton = GButton(STRINGS.ui.ok)
    private var cancelBtn: JButton
    private var scrollBackLabel: JLabel

    init {
        okBtn.addActionListener(this)
        cancelBtn = GButton(STRINGS.ui.cancel)
        cancelBtn.addActionListener(this)

        scrollBackLabel = JLabel("ScrollBack(lines)")

        val scrollBackPanel = JPanel()
        scrollBackPanel.add(scrollBackLabel)
        scrollBackPanel.add(okBtn)
        scrollBackPanel.add(cancelBtn)

        contentPane.add(scrollBackPanel)
        pack()
    }

    override fun actionPerformed(e: ActionEvent) {
        if (e.source == okBtn) {
            dispose()
        } else if (e.source == cancelBtn) {
            dispose()
        }
    }
}