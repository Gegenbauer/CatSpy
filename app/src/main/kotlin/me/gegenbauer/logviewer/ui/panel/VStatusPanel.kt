package me.gegenbauer.logviewer.ui.panel

import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.ui.log.LogTable
import java.awt.Color
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.plaf.ComponentUI


class VStatusPanel(val logTable: LogTable) : JPanel() {

    companion object {
        private const val TAG = "VStatusPanel"
        const val VIEW_RECT_WIDTH = 20
        const val VIEW_RECT_HEIGHT = 5
    }
    init {
        preferredSize = Dimension(VIEW_RECT_WIDTH, VIEW_RECT_HEIGHT)
        border = BorderFactory.createLineBorder(Color.DARK_GRAY)
        addMouseListener(MouseHandler())
    }

    override fun setUI(newUI: ComponentUI) {
        if (newUI !is VStatusPanelUI) {
            throw IllegalArgumentException("UI must be of type VStatusPanelUI")
        }
        super.setUI(newUI)
    }

    override fun updateUI() {
        setUI(VStatusPanelUI())
    }

    internal inner class MouseHandler : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            val row = event.point.y * logTable.rowCount / height
            try {
                logTable.scrollRectToVisible(Rectangle(logTable.getCellRect(row, 0, true)))
            } catch (e: IllegalArgumentException) {
                GLog.d(TAG, "e : $e")
            }
            super.mouseClicked(event)
        }
    }
}
