package me.gegenbauer.catspy.view.panel

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

class VerticalFlexibleWidthLayout : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {
        // 不需要实现
    }

    override fun removeLayoutComponent(comp: Component?) {
        // 不需要实现
    }

    override fun preferredLayoutSize(parent: Container): Dimension {
        synchronized(parent.treeLock) {
            var width = 0
            var height = 0
            for (comp in parent.components) {
                val d = comp.preferredSize
                width = maxOf(width, d.width)
                height += d.height
            }
            width += parent.insets.left + parent.insets.right
            height += parent.insets.top + parent.insets.bottom
            return Dimension(width, height)
        }
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        return preferredLayoutSize(parent)
    }

    override fun layoutContainer(parent: Container) {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            var y = insets.top
            val width = parent.width - insets.left - insets.right
            for (comp in parent.components) {
                comp.setSize(width, comp.preferredSize.height)
                comp.setLocation(insets.left, y)
                y += comp.height
            }
        }
    }
}