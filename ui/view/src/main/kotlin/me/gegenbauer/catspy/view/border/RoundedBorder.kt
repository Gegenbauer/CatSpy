package me.gegenbauer.catspy.view.border

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.border.Border

class RoundedBorder(radius: Int = 10, color: Color) : Border {

    var radius: Int = radius
    var fillColor: Color = color
    var strokeColor: Color? = null

    override fun getBorderInsets(c: Component) = Insets(2, radius / 2 - 4, 2, radius / 2 - 4)
    override fun isBorderOpaque() = true
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val g2 = g as Graphics2D
        g2.color = fillColor
        g2.setRenderingHints(mapOf(RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON))
        g2.fillRoundRect(x, y, width - 1, height - 1, radius, radius)
        strokeColor?.let {
            g2.color = it
            g2.stroke = BasicStroke(4f)
        }
        g2.drawRoundRect(x, y, width - 2, height - 2, radius, radius)
    }
}