package me.gegenbauer.catspy.view.icon

import java.awt.Dimension
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.JComponent

class IconComponent(private val icon: Icon) : JComponent() {
    init {
        preferredSize = Dimension(icon.iconWidth, icon.iconHeight)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        icon.paintIcon(this, g, 0, 0)
    }
}