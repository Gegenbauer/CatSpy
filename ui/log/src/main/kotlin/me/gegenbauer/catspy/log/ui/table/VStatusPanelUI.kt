package me.gegenbauer.catspy.log.ui.table

import com.formdev.flatlaf.ui.FlatPanelUI
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.BookmarkManager
import java.awt.Color
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.UIManager

open class VStatusPanelUI(override val contexts: Contexts = Contexts.default) : FlatPanelUI(true), Context {

    private var bookmarkColor = Color(0x000000)
    private var currentPositionColor = Color(0x000000)
    private val logTable by lazy { contexts.getContext(LogPanel::class.java)?.table }
    private val bookmarkManager by lazy { ServiceManager.getContextService(this, BookmarkManager::class.java) }

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
        val table = logTable ?: return
        paintBookmarks(table, g, c)
        paintCurrentPosition(table, g, c)
    }

    /**
     * Calculate the position of the log corresponding to the bookmark among all logs in the table,
     * and then convert it to its position in the VStatusPanel
     */
    private fun paintBookmarks(logTable: LogTable, g: Graphics, c: JComponent) {
        g.color = bookmarkColor
        val tableModel = logTable.tableModel
        val dataCount = tableModel.dataSize
        if (dataCount <= 0) {
            return
        }
        bookmarkManager.getAllBookmarks().forEach {
            val row = tableModel.getRowIndexInAllPages(it)
            if (row > 0) {
                g.fillRect(0, row * c.height / dataCount, c.width, 1)
            }
        }
    }

    private fun paintCurrentPosition(logTable: LogTable, g: Graphics, c: JComponent) {
        g.color = currentPositionColor
        val tableModel = logTable.tableModel

        val tableVisibleY = (logTable.visibleRect.y).toLong()
        val tableTotalHeight = (logTable.rowHeight * tableModel.dataSize).toLong()
        if (tableTotalHeight <= 0L) {
            return
        }
        var positionMarkHeight = logTable.visibleRect.height * c.height / tableTotalHeight
        if (positionMarkHeight < VStatusPanel.CURRENT_POSITION_MARK_MIN_HEIGHT) {
            positionMarkHeight = VStatusPanel.CURRENT_POSITION_MARK_MIN_HEIGHT.toLong()
        }
        val currentPage = tableModel.currentPage
        val positionPercentage = (tableVisibleY.toDouble() + currentPage * tableModel.pageSize * logTable.rowHeight) / tableTotalHeight
        var currentPositionInPanel = (positionPercentage * c.height).toLong()
        if (currentPositionInPanel + positionMarkHeight > c.height) {
            currentPositionInPanel = c.height - positionMarkHeight
        }
        g.fillRect(0, currentPositionInPanel.toInt(), c.width, positionMarkHeight.toInt())
    }
}