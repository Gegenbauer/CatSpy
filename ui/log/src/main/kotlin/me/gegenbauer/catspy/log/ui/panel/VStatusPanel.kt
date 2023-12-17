package me.gegenbauer.catspy.log.ui.panel

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.glog.GLog
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.plaf.ComponentUI


class VStatusPanel(override val contexts: Contexts = Contexts.default) : JPanel(), Context {

    init {
        minimumSize = Dimension(0, 0)
        preferredSize = Dimension(STATUS_PANEL_WIDTH, CURRENT_POSITION_MARK_MIN_HEIGHT)
        border = BorderFactory.createLineBorder(Color.DARK_GRAY)
        addMouseListener(MouseHandler())
    }

    override fun setUI(newUI: ComponentUI) {
        require(newUI is VStatusPanelUI) { "UI must be of type VStatusPanelUI" }
        super.setUI(newUI)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        (getUI() as? VStatusPanelUI)?.setParent(this)
    }

    override fun updateUI() {
        if (ui != null) {
            setUI(VStatusPanelUI().apply { setParent(this@VStatusPanel) })
        } else {
            setUI(VStatusPanelUI())
        }
    }

    internal inner class MouseHandler : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            val logTable = contexts.getContext(LogPanel::class.java)?.table
            logTable ?: return
            val row = (event.point.y.toLong() * logTable.tableModel.dataSize / height).toInt()
            if (row !in 0 until logTable.tableModel.dataSize) {
                GLog.w(TAG, "[mouseClicked] row is out of range: $row")
                super.mouseClicked(event)
                return
            }
            logTable.moveRowToCenter(row, false)
            super.mouseClicked(event)
        }
    }

    companion object {
        private const val TAG = "VStatusPanel"
        const val STATUS_PANEL_WIDTH = 14
        const val CURRENT_POSITION_MARK_MIN_HEIGHT = 5
    }
}
