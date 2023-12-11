package me.gegenbauer.catspy.log.ui.panel

import com.formdev.flatlaf.ui.FlatPanelUI
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.log.ui.table.LogTable
import java.awt.Color
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.UIManager

open class VStatusPanelUI(override val contexts: Contexts = Contexts.default) : FlatPanelUI(true), Context {

    private var bookmarkColor = Color(0x000000)
    private var currentPositionColor = Color(0x000000)

    override fun installDefaults(p: JPanel) {
        super.installDefaults(p)
        installBackgroundAndBookMarkColor(p)
    }

    private fun installBackgroundAndBookMarkColor(p: JPanel) {
        p.background = UIManager.getColor("VStatusPanel.background") ?: Color.GRAY
        bookmarkColor = UIManager.getColor("VStatusPanel.bookmark") ?: Color.GRAY
        currentPositionColor = UIManager.getColor("VStatusPanel.currentPosition") ?: Color.GRAY
    }

    override fun paint(g: Graphics, c: JComponent) {
        super.paint(g, c)
        val logTable = contexts.getContext(LogPanel::class.java)?.table ?: return
        paintBookmarks(logTable, g, c)
        paintCurrentPosition(logTable, g, c)
    }

    private fun paintBookmarks(logTable: LogTable, g: Graphics, c: JComponent) {
        g.color = bookmarkColor
        for (row in 0 until logTable.rowCount) {
            val num = logTable.getValueAt(row, 0).toString().trim().toInt()
            contexts.getContext(LogTabPanel::class.java)?.apply {
                val bookmarkManager = ServiceManager.getContextService(this, BookmarkManager::class.java)
                if (bookmarkManager.isBookmark(num)) {
                    g.fillRect(0, row * c.height / logTable.rowCount, c.width, 1)
                }
            }
        }
    }

    private fun paintCurrentPosition(logTable: LogTable, g: Graphics, c: JComponent) {
        val visibleY: Long = (logTable.visibleRect.y).toLong()
        val totalHeight: Long = (logTable.rowHeight * logTable.tableModel.dataSize).toLong()
        if (logTable.tableModel.dataSize != 0 && c.height != 0) {
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