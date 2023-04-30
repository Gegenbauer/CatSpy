package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.version
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.utils.Utils
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

class AboutDialog(parent: JFrame) :JDialog(parent, STRINGS.ui.about, true), ActionListener {
    private var aboutLabel: JLabel
    private var closeBtn : JButton = GButton(STRINGS.ui.close)
    private var mainUI: MainUI

    init {
        closeBtn.addActionListener(this)
        mainUI = parent as MainUI

        aboutLabel = JLabel("<html><center><h1>LogViewer ${STRINGS.ui.version}</h1><br>865815634@qq.com</center></html>")

        val aboutPanel = JPanel()
        aboutPanel.add(aboutLabel)

        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.add(aboutPanel, BorderLayout.CENTER)

        val btnPanel = JPanel()
        btnPanel.add(closeBtn)
        panel.add(btnPanel, BorderLayout.SOUTH)

        contentPane.add(panel)
        pack()

        Utils.installKeyStrokeEscClosing(this)
    }

    override fun actionPerformed(event: ActionEvent) {
        if (event.source == closeBtn) {
            dispose()
        }
    }
}