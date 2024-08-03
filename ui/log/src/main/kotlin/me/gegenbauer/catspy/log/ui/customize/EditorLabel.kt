package me.gegenbauer.catspy.log.ui.customize

import java.awt.Graphics
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

class EditorLabel(text: String) : JLabel(text) {

    private val scrollPane: JScrollPane? by lazy { findScrollPane() }

    override fun paint(g: Graphics) {
        if (scrollPane == null) {
            super.paint(g)
            return
        }

        val scrollPane = findScrollPane()
        if (scrollPane != null) {
            val viewport = scrollPane.viewport

            val clipBounds = viewport.bounds

            val viewportPosition = SwingUtilities.convertPoint(viewport, viewport.location, this)
            clipBounds.location = viewportPosition
            g.clip = clipBounds
        }
        super.paint(g)
    }

    private fun findScrollPane(): JScrollPane? {
        var parent = parent
        while (parent != null) {
            if (parent is JScrollPane) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }
}