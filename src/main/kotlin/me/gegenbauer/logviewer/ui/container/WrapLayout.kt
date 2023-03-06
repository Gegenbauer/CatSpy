package me.gegenbauer.logviewer.ui.container

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel

class WrapLayout(hGap: Int = 0, vGap: Int = 0, align: Int = LEFT) : FlowLayout(align, hGap, vGap) {
    private var hasAddComposeAdapter = false
    override fun minimumLayoutSize(target: Container): Dimension {
        return Dimension(0, 0)
    }

    override fun layoutContainer(target: Container) {
        super.layoutContainer(target)
        if (target !is JPanel) return
        if (hasAddComposeAdapter.not()) {
            target.addComponentListener(WrapLayoutComponentAdapter())
        }
    }

    class WrapLayoutComponentAdapter : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
            super.componentResized(e)
            val target = e.component as? JPanel
            target ?: return
            if (target.components.isNullOrEmpty()) {
                return
            }
            target.apply {
                val lastComponent = components.last()
                val maxY = lastComponent.location.y + lastComponent.height
                if (maxY != preferredSize.height) {
                    preferredSize = Dimension(preferredSize.width, maxY)
                    updateUI()
                }
            }
        }
    }
}