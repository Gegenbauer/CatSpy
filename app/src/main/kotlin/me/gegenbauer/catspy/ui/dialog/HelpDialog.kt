package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.helpText
import me.gegenbauer.catspy.utils.KeyUtils
import me.gegenbauer.catspy.view.button.GButton
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

class HelpDialog(parent: JFrame) : JDialog(parent, STRINGS.ui.help, true), ActionListener {
    private var helpTextPane: JTextPane
    private var closeBtn = GButton(STRINGS.ui.close)

    init {
        closeBtn.addActionListener(this)

        helpTextPane = JTextPane()
        helpTextPane.contentType = "text/html"

        helpTextPane.text = STRINGS.ui.helpText

        helpTextPane.caretPosition = 0
        val scrollPane = JScrollPane(helpTextPane)
        val aboutPanel = JPanel()
        scrollPane.preferredSize = Dimension(850, 800)
        aboutPanel.add(scrollPane)

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