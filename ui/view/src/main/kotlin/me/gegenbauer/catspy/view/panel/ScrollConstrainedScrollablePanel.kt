package me.gegenbauer.catspy.view.panel

import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JPanel
import javax.swing.Scrollable

open class ScrollConstrainedScrollablePanel(
    private val horizontalScrollable: Boolean = true,
    private val verticalScrollable: Boolean = true
) : JPanel(), Scrollable {
    override fun getPreferredScrollableViewportSize(): Dimension {
        return preferredSize
    }

    override fun getScrollableUnitIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int {
        return 10
    }

    override fun getScrollableBlockIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int {
        return 10
    }

    override fun getScrollableTracksViewportWidth(): Boolean {
        return !horizontalScrollable
    }

    override fun getScrollableTracksViewportHeight(): Boolean {
        return !verticalScrollable
    }
}