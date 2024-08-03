package me.gegenbauer.catspy.log.ui.customize

import java.awt.Component
import java.awt.Dimension
import javax.swing.JPanel

open class CenteredDualDirectionPanel : JPanel() {
    private val leftComponents = arrayListOf<Component>()
    private val rightComponents = arrayListOf<Component>()

    fun addLeft(component: Component, index: Int = leftComponents.size) {
        leftComponents.add(index, component)
        add(component)
        revalidate()
        repaint()
    }

    fun addRight(component: Component, index: Int = rightComponents.size) {
        rightComponents.add(index, component)
        add(component)
        revalidate()
        repaint()
    }

    override fun doLayout() {
        super.doLayout()
        var maxHeight = 0
        for (comp in this.components.filter { it.isVisible }) {
            val compSize = comp.preferredSize
            maxHeight = maxOf(maxHeight, compSize.height)
        }

        var leftOffset = 0
        for (comp in leftComponents.filter { it.isVisible }) {
            val compSize = comp.preferredSize
            val y = (maxHeight - compSize.height) / 2
            comp.setBounds(leftOffset, y, compSize.width, compSize.height)
            leftOffset += compSize.width
        }

        var rightOffset = width
        for (comp in rightComponents.filter { it.isVisible }) {
            val compSize = comp.preferredSize
            rightOffset -= compSize.width
            val y = (maxHeight - compSize.height) / 2
            comp.setBounds(rightOffset, y, compSize.width, compSize.height)
        }
    }

    override fun getPreferredSize(): Dimension {
        var totalWidth = 0
        var maxHeight = 0
        for (comp in this.components) {
            val compSize = comp.preferredSize
            maxHeight = maxOf(maxHeight, compSize.height)
            totalWidth += compSize.width
        }
        return Dimension(totalWidth, maxHeight)
    }
}