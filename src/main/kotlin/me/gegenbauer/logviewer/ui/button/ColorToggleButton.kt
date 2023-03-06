package me.gegenbauer.logviewer.ui.button

import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JToggleButton


class ColorToggleButton(title: String) : JToggleButton(title) {
    init {
        icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
        selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on.png"))
    }

    override fun paintComponent(graphics: Graphics) {
        drawIconAndText(icon, graphics as Graphics2D)
        super.paintComponent(graphics)
    }

    private fun drawIconAndText(icon: Icon?, graphics2D: Graphics2D) {
        if (icon != null) {
            icon.paintIcon(this, graphics2D, margin.left + insets.left, (height - icon.iconHeight) / 2)
            graphics2D.drawString(
                text,
                margin.left + insets.left + icon.iconWidth + 2,
                (height + graphics2D.fontMetrics.ascent) / 2 - 2
            )
        } else {
            graphics2D.drawString(
                text,
                (width - graphics2D.fontMetrics.stringWidth(text)) / 2,
                (height + graphics2D.fontMetrics.ascent) / 2 - 2
            )
        }
    }
}
