package me.gegenbauer.logviewer.ui.help

import me.gegenbauer.logviewer.strings.Strings
import me.gegenbauer.logviewer.Utils
import me.gegenbauer.logviewer.ui.button.ColorButton
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

class HelpDialog(parent: JFrame) : JDialog(parent, Strings.HELP, true), ActionListener {
    private var helpTextPane: JTextPane
    private var closeBtn : ColorButton = ColorButton(Strings.CLOSE)

    init {
        closeBtn.addActionListener(this)

        helpTextPane = JTextPane()
        helpTextPane.contentType = "text/html"

        if (Strings.lang == Strings.KO) {
            helpTextPane.text = HelpText.textKo
        }
        else {
            helpTextPane.text = HelpText.textEn
        }

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

    override fun actionPerformed(e: ActionEvent?) {
        if (e?.source == closeBtn) {
            dispose()
        }
    }
}