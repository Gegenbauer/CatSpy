package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.java.ext.capitalize
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.icon.IconComponent
import me.gegenbauer.catspy.view.icon.ScaledIcon
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*

class AboutDialog(parent: JFrame) : JDialog(parent, STRINGS.ui.menuAbout, true) {
    init {
        val aboutLabel = JLabel("<html><center><h1>${GlobalProperties.APP_NAME} ${GlobalProperties.APP_VERSION_NAME}</h1><br><font color='blue'><a href=''>${GlobalProperties.APP_REPO_LINK}</a></font></center></html>")
        aboutLabel.font = aboutLabel.font.deriveFont(Font.BOLD, 16f)
        aboutLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        aboutLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(GlobalProperties.APP_REPO_LINK))
                }
            }
        })

        val authorLabel = JLabel("<html><h2>${STRINGS.ui.author}: ${GlobalProperties.APP_AUTHOR.capitalize()}</h2></html>")
        authorLabel.alignmentX = Component.LEFT_ALIGNMENT

        val copyrightLabel = JLabel("<html><font color='gray'><h2>Copyright &copy; 2023 ${GlobalProperties.APP_AUTHOR.capitalize()}</h2></font></html>")
        copyrightLabel.alignmentX = Component.LEFT_ALIGNMENT

        val logoLabel = IconComponent(ScaledIcon(GIcons.Logo.get(), 0.5))
        logoLabel.maximumSize = Dimension(logoLabel.preferredSize)

        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val constraints = GridBagConstraints()
        constraints.gridx = 0
        constraints.gridy = GridBagConstraints.RELATIVE
        constraints.fill = GridBagConstraints.NONE  // Do not resize the component
        constraints.anchor = GridBagConstraints.CENTER  // Center the component

        panel.add(logoLabel, constraints)
        constraints.fill = GridBagConstraints.HORIZONTAL  // Reset to default
        constraints.anchor = GridBagConstraints.WEST  // Reset to default
        panel.add(aboutLabel, constraints)
        panel.add(Box.createRigidArea(Dimension(0, 20)), constraints)
        panel.add(authorLabel, constraints)
        panel.add(Box.createRigidArea(Dimension(0, 10)), constraints)
        panel.add(copyrightLabel, constraints)
        panel.add(Box.createRigidArea(Dimension(0, 20)), constraints)

        contentPane.add(panel)
        pack()
        setLocationRelativeTo(parent)
    }
}