package me.gegenbauer.catspy.ui.dialog

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.installKeyStrokeEscClosing
import me.gegenbauer.catspy.view.button.GButton
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class AboutDialog(parent: JFrame) :JDialog(parent, STRINGS.ui.menuAbout, true), ActionListener {
    private var aboutLabel: JLabel
    private var closeBtn = GButton(STRINGS.ui.close)

    init {
        closeBtn.addActionListener(this)

        aboutLabel = JLabel("<html><center><h1>${GlobalProperties.APP_NAME} ${GlobalProperties.APP_VERSION_NAME}</h1><br>https://github.com/Gegenbauer/CatSpy</center></html>")

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
        iconImages = listOf((GIcons.Logo.get(200, 200) as DerivableImageIcon).image)

        installKeyStrokeEscClosing(this)
    }

    override fun actionPerformed(event: ActionEvent) {
        if (event.source == closeBtn) {
            dispose()
        }
    }
}