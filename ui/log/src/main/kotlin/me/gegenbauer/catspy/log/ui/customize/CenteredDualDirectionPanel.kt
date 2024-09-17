package me.gegenbauer.catspy.log.ui.customize

import java.awt.Component
import java.awt.Dimension
import javax.swing.JPanel

open class CenteredDualDirectionPanel(hGap: Int = 0) : JPanel() {

    var hGap: Int = hGap
        set(value) {
            field = value
            revalidate()
            repaint()
        }

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
        val insets = insets
        var maxHeight = 0
        for (comp in this.components.filter { it.isVisible }) {
            val compSize = comp.preferredSize
            maxHeight = maxOf(maxHeight, compSize.height)
        }

        var leftOffset = insets.left
        for (comp in leftComponents.filter { it.isVisible }) {
            val compSize = comp.preferredSize
            val y = insets.top + (maxHeight - compSize.height) / 2
            comp.setBounds(leftOffset, y, compSize.width, compSize.height)
            leftOffset += compSize.width + hGap
        }

        var rightOffset = width - insets.right
        for (comp in rightComponents.filter { it.isVisible }) {
            val compSize = comp.preferredSize
            rightOffset -= compSize.width
            val y = insets.top + (maxHeight - compSize.height) / 2
            comp.setBounds(rightOffset, y, compSize.width, compSize.height)
            rightOffset -= hGap
        }
    }

    override fun getPreferredSize(): Dimension {
        val insets = insets
        var totalWidth = insets.left + insets.right
        var maxHeight = insets.top + insets.bottom
        for (comp in this.components.filter { it.isVisible }) {
            val compSize = comp.preferredSize
            maxHeight = maxOf(maxHeight, compSize.height + insets.top + insets.bottom)
            totalWidth += compSize.width + hGap
        }
        return Dimension(totalWidth, maxHeight)
    }
}