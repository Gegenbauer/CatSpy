package me.gegenbauer.catspy.view.panel

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager
import javax.swing.JComponent

class VerticalFlexibleWidthLayout(vGap: Int = 0) : LayoutManager {
    var vGap: Int = vGap
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
                comp as JComponent
                if (comp.isVisible) {
                    val d = comp.preferredSize
                    width = maxOf(width, d.width + comp.insets.left + comp.insets.right)
                    height += d.height + comp.insets.top + comp.insets.bottom + vGap
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
            parent as JComponent
            val insets = parent.insets
            var y = insets.top
            val width = parent.width - insets.left - insets.right
            for (comp in parent.components) {
                comp as JComponent
                if (comp.isVisible) {
                    val compInsets = comp.insets
                    val compWidth = width - compInsets.left - compInsets.right
                    comp.setSize(compWidth, comp.preferredSize.height)
                    comp.setLocation(insets.left + compInsets.left, y + compInsets.top)
                    y += comp.height + compInsets.top + compInsets.bottom + vGap
                }
            }
        }
    }
}