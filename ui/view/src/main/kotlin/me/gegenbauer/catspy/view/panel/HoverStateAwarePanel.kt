package me.gegenbauer.catspy.view.panel

import java.awt.Component
import java.awt.FlowLayout
import java.awt.LayoutManager
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import javax.swing.JPanel

open class HoverStateAwarePanel(layoutManager: LayoutManager = FlowLayout()) : JPanel(layoutManager), MouseMotionListener {

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
        if (!contains(e.point)) {
            hoverListener.mouseExited(e)
        } else {
            hoverListener.mouseEntered(e)
        }
    }

    protected open fun onHoverStateChanged(isHover: Boolean) {
        // no-op
    }

    class HoverListener(private val panel: HoverStateAwarePanel) : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
            panel.onHoverStateChanged(true)
        }

        override fun mouseExited(e: MouseEvent) {
            if (!panel.contains(e.point)) {
                panel.onHoverStateChanged(false)
            }
        }
    }
}