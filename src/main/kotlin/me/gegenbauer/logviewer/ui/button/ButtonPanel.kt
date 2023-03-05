package me.gegenbauer.logviewer.ui.button

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel

class ButtonPanel : JPanel() {
    internal inner class ButtonFlowLayout(align: Int, hGap: Int, vGap: Int) : FlowLayout(align, hGap, vGap) {
        override fun minimumLayoutSize(target: Container?): Dimension {
            return Dimension(0, 0)
        }
    }
    var lastComponent: Component? = null
    init {
        layout = ButtonFlowLayout(FlowLayout.LEFT, 2, 0)
        addComponentListener(
                object : ComponentAdapter() {
                    var prevPoint: Point? = null
                    override fun componentResized(e: ComponentEvent) {
                        super.componentResized(e)
                        for (item in components) {
                            if (lastComponent == null) {
                                lastComponent = item
                            } else {
                                if ((item.location.y + item.height) > (lastComponent!!.location.y + lastComponent!!.height)) {
                                    lastComponent = item
                                }
                            }
                        }
                        if (prevPoint == null || prevPoint!!.y != lastComponent!!.location.y) {
                            println("last Component moved to ${lastComponent!!.location}")
                            preferredSize = Dimension(preferredSize.width, lastComponent!!.location.y + lastComponent!!.height)
                            updateUI()
                        }
                        prevPoint = lastComponent!!.location
                    }
                })
    }
}