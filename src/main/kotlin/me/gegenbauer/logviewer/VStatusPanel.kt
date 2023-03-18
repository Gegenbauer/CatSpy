package me.gegenbauer.logviewer

import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.manager.BookmarkManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.log.LogTable
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel


class VStatusPanel(private val logTable: LogTable) : JPanel() {

    companion object {
        private const val TAG = "VStatusPanel"
        const val VIEW_RECT_WIDTH = 20
        const val VIEW_RECT_HEIGHT = 5
    }
    init {
        preferredSize = Dimension(VIEW_RECT_WIDTH, VIEW_RECT_HEIGHT)
        background = if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
            Color(0x46494B)
        } else {
            Color.WHITE
        }
        border = BorderFactory.createLineBorder(Color.DARK_GRAY)
        addMouseListener(MouseHandler())
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
            g.color = Color(0xFFFFFF)
        } else {
            g.color = Color(0x000000)
        }
        for (row in 0 until logTable.rowCount) {
            val num = logTable.getValueAt(row, 0).toString().trim().toInt()
            if (BookmarkManager.bookmarks.contains(num)) {
                g.fillRect(0, row * height / logTable.rowCount, width, 1)
            }
        }

        val visibleY:Long = (logTable.visibleRect.y).toLong()
        val totalHeight:Long = (logTable.rowHeight * logTable.rowCount).toLong()
        if (logTable.rowCount != 0 && height != 0) {
            if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
                g.color = Color(0xC0, 0xC0, 0xC0, 0x50)
            } else {
                g.color = Color(0xA0, 0xA0, 0xA0, 0x50)
            }
            var viewHeight = logTable.visibleRect.height * height / totalHeight
            if (viewHeight < VIEW_RECT_HEIGHT) {
                viewHeight = VIEW_RECT_HEIGHT.toLong()
            }

            var viewY = visibleY * height / totalHeight
            if (viewY + viewHeight > height) {
                viewY = height - viewHeight
            }
            g.fillRect(0, viewY.toInt(), width, viewHeight.toInt())
        }
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
