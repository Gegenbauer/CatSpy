package me.gegenbauer.logviewer.ui.panel

import com.github.weisj.darklaf.ui.panel.DarkPanelUI
import me.gegenbauer.logviewer.manager.BookmarkManager
import me.gegenbauer.logviewer.ui.log.LogTable
import java.awt.Color
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.UIManager

open class VStatusPanelUI: DarkPanelUI() {
    private var bookmarkColor = Color(0x000000)
    private var currentPositionColor = Color(0x000000)
    private lateinit var panel: JPanel
    private lateinit var logTable: LogTable

    override fun installDefaults(p: JPanel) {
        super.installDefaults(p)
        installBackgroundAndBookMarkColor(p)
    }

    override fun installUI(c: JComponent?) {
        super.installUI(c)
        panel = c as VStatusPanel
    }

    protected fun installBackgroundAndBookMarkColor(p: JPanel) {
        p.background = UIManager.getColor("VStatusPanel.background") ?: Color.GRAY
        bookmarkColor = UIManager.getColor("VStatusPanel.bookmark") ?: Color.GRAY
        currentPositionColor = UIManager.getColor("VStatusPanel.currentPosition") ?: Color.GRAY
    }

    override fun paint(g: Graphics, c: JComponent) {
        super.paint(g, c)
        logTable = (panel as VStatusPanel).logTable
        paintBookmarks(g, c)
        paintCurrentPosition(g, c)
    }

    private fun paintBookmarks(g: Graphics, c: JComponent) {
        g.color = bookmarkColor
        for (row in 0 until logTable.rowCount) {
            val num = logTable.getValueAt(row, 0).toString().trim().toInt()
            if (BookmarkManager.bookmarks.contains(num)) {
                g.fillRect(0, row * c.height / logTable.rowCount, c.width, 1)
            }
        }
    }

    private fun paintCurrentPosition(g: Graphics, c: JComponent) {
        val visibleY:Long = (logTable.visibleRect.y).toLong()
        val totalHeight:Long = (logTable.rowHeight * logTable.rowCount).toLong()
        if (logTable.rowCount != 0 && c.height != 0) {
            g.color = currentPositionColor
            var viewHeight = logTable.visibleRect.height * c.height / totalHeight
            if (viewHeight < VStatusPanel.VIEW_RECT_HEIGHT) {
                viewHeight = VStatusPanel.VIEW_RECT_HEIGHT.toLong()
            }

            var viewY = visibleY * c.height / totalHeight
            if (viewY + viewHeight > c.height) {
                viewY = c.height - viewHeight
            }
            g.fillRect(0, viewY.toInt(), c.width, viewHeight.toInt())
        }
    }
}