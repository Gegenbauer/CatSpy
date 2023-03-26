package me.gegenbauer.logviewer.ui.dialog

import me.gegenbauer.logviewer.utils.Utils
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.resource.strings.helpText
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

class HelpDialog(parent: JFrame) : JDialog(parent, STRINGS.ui.help, true), ActionListener {
    private var helpTextPane: JTextPane
    private var closeBtn: JButton = JButton(STRINGS.ui.close)

    init {
        closeBtn.addActionListener(this)

        helpTextPane = JTextPane()
        helpTextPane.contentType = "text/html"

        helpTextPane.text = STRINGS.ui.helpText

        helpTextPane.caretPosition = 0
        val scrollPane = JScrollPane(helpTextPane)
        val aboutPanel = JPanel()
        scrollPane.preferredSize = Dimension(850, 800)
        scrollPane.verticalScrollBar.setUI(BasicScrollBarUI())
        scrollPane.horizontalScrollBar.setUI(BasicScrollBarUI())
        aboutPanel.add(scrollPane)

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