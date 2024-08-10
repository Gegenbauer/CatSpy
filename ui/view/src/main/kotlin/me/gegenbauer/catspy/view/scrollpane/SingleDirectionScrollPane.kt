package me.gegenbauer.catspy.view.scrollpane

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ScrollPaneLayout

class SingleDirectionScrollPane(view: Component, private val horizontal: Boolean = false) : JScrollPane(view) {
    init {
        setHorizontalScrollBarPolicy(if (horizontal) HORIZONTAL_SCROLLBAR_ALWAYS else HORIZONTAL_SCROLLBAR_NEVER)
        setVerticalScrollBarPolicy(if (horizontal) VERTICAL_SCROLLBAR_NEVER else VERTICAL_SCROLLBAR_ALWAYS)
        layout = SingleDirectionScrollPaneLayout()
    }

    private inner class SingleDirectionScrollPaneLayout : ScrollPaneLayout() {
        override fun layoutContainer(parent: Container) {
            super.layoutContainer(parent)
            val viewPreferredSize = viewport.view.preferredSize
            val size = parent.size

            if (horizontal) {
                viewport.setSize(size.width, viewPreferredSize.height)
            } else {
                viewport.setSize(viewPreferredSize.width, size.height)
            }
            viewport.view.size = viewport.size
        }

        override fun preferredLayoutSize(parent: Container): Dimension {
            val viewport = viewport
            val viewPreferredSize = viewport.view.preferredSize
            val tableHeader = (viewport.view as? JTable)?.tableHeader
            return if (horizontal) {
                Dimension(
                    parent.width,
                    viewPreferredSize.height +
                            horizontalScrollBar.preferredSize.height + viewport.insets.top +
                            viewport.insets.bottom + horizontalScrollBar.insets.top +
                            horizontalScrollBar.insets.bottom + insets.top + insets.bottom +
                            (tableHeader?.height ?: 0) + SPARE_SPACE
                )
            } else {
                Dimension(
                    viewPreferredSize.width + verticalScrollBar.preferredSize.width +
                            viewport.insets.left + viewport.insets.right + insets.left + insets.right + SPARE_SPACE,
                    parent.height
                )
            }
        }

        override fun minimumLayoutSize(parent: Container): Dimension {
            return preferredLayoutSize(parent)
        }

    }

    companion object {
        private const val SPARE_SPACE = 30
    }
}