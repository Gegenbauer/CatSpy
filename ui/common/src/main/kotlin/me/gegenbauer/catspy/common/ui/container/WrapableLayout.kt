package me.gegenbauer.catspy.common.ui.container

import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel

class WrapableLayout(hGap: Int = 0, vGap: Int = 0, align: Int = LEFT) : FlowLayout(align, hGap, vGap) {
    private val resizeListener = WrapLayoutComponentAdapter()

    override fun minimumLayoutSize(target: Container): Dimension {
        return Dimension(0, 0)
    }

    override fun layoutContainer(target: Container) {
        super.layoutContainer(target)
        if (target !is JPanel) return
        if (target.componentListeners.contains(resizeListener).not()) {
            target.addComponentListener(resizeListener)
        }
    }

    fun resizeComponent(container: Container) {
        resizeListener.componentResized(ComponentEvent(container, ComponentEvent.COMPONENT_RESIZED))
    }

    class WrapLayoutComponentAdapter : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
            super.componentResized(e)
            val target = e.component as? JPanel
            target ?: return
            if (target.components.isNullOrEmpty()) {
                return
            }
            // TODO bug font adjustment will cause bottom border overflow
            target.apply {
                val lastComponent = components.last()
                val maxY = lastComponent.location.y + lastComponent.height
                if (maxY != location.y + height) {
                    preferredSize = Dimension(width, maxY)
                    updateUI()
                }
            }
        }
    }
}