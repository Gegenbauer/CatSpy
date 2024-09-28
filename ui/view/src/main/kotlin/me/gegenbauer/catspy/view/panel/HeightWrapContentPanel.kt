package me.gegenbauer.catspy.view.panel

import java.awt.Dimension
import javax.swing.JPanel

open class HeightWrapContentPanel: JPanel() {

    override fun getMaximumSize(): Dimension {
        val maxSuper = super.getMaximumSize()
        return Dimension(maxSuper.width, minimumSize.height)
    }

    override fun getPreferredSize(): Dimension {
        val prefSuper = super.getPreferredSize()
        return Dimension(prefSuper.width, minimumSize.height)
    }
}