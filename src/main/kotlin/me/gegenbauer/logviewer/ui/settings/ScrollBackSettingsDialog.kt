package me.gegenbauer.logviewer.ui.settings

import me.gegenbauer.logviewer.strings.Strings
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.button.ColorButton
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel

class ScrollBackSettingsDialog(parent: MainUI) : JDialog(parent, "ScrollBack settings", true), ActionListener {
    private var okBtn: ColorButton = ColorButton(Strings.OK)
    private var cancelBtn: ColorButton
    private var scrollBackLabel: JLabel

    init {
        okBtn.addActionListener(this)
        cancelBtn = ColorButton(Strings.CANCEL)
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