package me.gegenbauer.catspy.view.panel

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

class HorizontalFlexibleHeightLayout(hGap: Int = 0) : LayoutManager {

    var hGap: Int = hGap
        set(value) {
            field = value
            parent.revalidate()
            parent.repaint()
        }

    private lateinit var parent: Container

    override fun addLayoutComponent(name: String?, comp: Component?) {
        // 不需要实现
    }

    override fun removeLayoutComponent(comp: Component?) {
        // 不需要实现
    }

    override fun preferredLayoutSize(parent: Container): Dimension {
        synchronized(parent.treeLock) {
            this.parent = parent
            var width = 0
            var height = 0
            for (comp in parent.components) {
                if (comp.isVisible) {
                    val d = comp.preferredSize
                    width += d.width + hGap
                    height = maxOf(height, d.height)
                }
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
            var x = insets.left
            val height = parent.height - insets.top - insets.bottom
            for (comp in parent.components) {
                if (comp.isVisible) {
                    comp.setSize(comp.preferredSize.width, height)
                    comp.setLocation(x, insets.top)
                    x += comp.width + hGap
                }
            }
        }
    }
}