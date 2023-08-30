package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.version
import me.gegenbauer.catspy.utils.KeyUtils
import me.gegenbauer.catspy.view.button.GButton
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

class AboutDialog(parent: JFrame) :JDialog(parent, STRINGS.ui.about, true), ActionListener {
    private var aboutLabel: JLabel
    private var closeBtn = GButton(STRINGS.ui.close)

    init {
        closeBtn.addActionListener(this)

        aboutLabel = JLabel("<html><center><h1>CatSpy ${STRINGS.ui.version}</h1><br>https://github.com/Gegenbauer/CatSpy</center></html>")

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

        KeyUtils.installKeyStrokeEscClosing(this)
    }

    override fun actionPerformed(event: ActionEvent) {
        if (event.source == closeBtn) {
            dispose()
        }
    }
}