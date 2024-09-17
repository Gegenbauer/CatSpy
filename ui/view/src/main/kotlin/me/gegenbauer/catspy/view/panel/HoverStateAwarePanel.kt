package me.gegenbauer.catspy.view.panel

import java.awt.Component
import java.awt.FlowLayout
import java.awt.LayoutManager
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import javax.swing.JPanel

open class HoverStateAwarePanel(layoutManager: LayoutManager = FlowLayout()) : JPanel(layoutManager),
    MouseMotionListener {

    private val hoverListener = HoverListener(this)

    init {
        addMouseMotionListener(this)
        addMouseListener(hoverListener)
    }

    override fun addImpl(comp: Component?, constraints: Any?, index: Int) {
        super.addImpl(comp, constraints, index)
        comp?.addMouseMotionListener(this)
        comp?.addMouseListener(hoverListener)
    }

    override fun mouseDragged(e: MouseEvent) {
        // no-op
    }

    override fun mouseMoved(e: MouseEvent) {
        if (e.isInComponent(this)) {
            hoverListener.mouseEntered(e)
        } else {
            hoverListener.mouseExited(e)
        }
    }

    protected open fun onHoverStateChanged(isHover: Boolean, e: MouseEvent) {
        // no-op
    }

    class HoverListener(private val panel: HoverStateAwarePanel) : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
            if (e.isInComponent(panel)) {
                panel.onHoverStateChanged(true, e)
            }
        }

        override fun mouseExited(e: MouseEvent) {
            if (!e.isInComponent(panel)) {
                panel.onHoverStateChanged(false, e)
            }
        }
    }

    companion object {

        private fun MouseEvent.isInComponent(component: Component): Boolean {
            return locationOnScreen.isInComponent(component)
        }

        /**
         * use screen coordinate to check if the point is in the component
         */
        private fun Point.isInComponent(component: Component): Boolean {
            val screenPoint = component.locationOnScreen
            val screenRect = component.bounds
            screenRect.location = screenPoint
            return screenRect.contains(this)
        }
    }
}