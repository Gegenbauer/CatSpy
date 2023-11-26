package me.gegenbauer.catspy.view.icon

import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.Icon

class ScaledIcon(private val original: Icon, private val scale: Double = 1.0) : Icon {
    override fun getIconWidth(): Int {
        return (original.iconWidth * scale).toInt()
    }

    override fun getIconHeight(): Int {
        return (original.iconHeight * scale).toInt()
    }

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2d = g.create() as Graphics2D
        g2d.scale(scale, scale)
        original.paintIcon(c, g2d, x, y)
        g2d.dispose()
    }
}