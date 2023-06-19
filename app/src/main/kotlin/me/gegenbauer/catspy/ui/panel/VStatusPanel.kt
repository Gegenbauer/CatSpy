package me.gegenbauer.catspy.ui.panel

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextScope
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.ui.log.LogTable
import java.awt.Color
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.plaf.ComponentUI


class VStatusPanel(override val contexts: Contexts = Contexts.default) : JPanel(), Context {
    override val scope: ContextScope = ContextScope.COMPONENT

    init {
        preferredSize = Dimension(VIEW_RECT_WIDTH, VIEW_RECT_HEIGHT)
        border = BorderFactory.createLineBorder(Color.DARK_GRAY)
        addMouseListener(MouseHandler())
    }

    override fun setUI(newUI: ComponentUI) {
        require(newUI is VStatusPanelUI) { "UI must be of type VStatusPanelUI" }
        super.setUI(newUI)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        (getUI() as? VStatusPanelUI)?.setContexts(contexts)
    }

    override fun updateUI() {
        setUI(VStatusPanelUI())
    }

    internal inner class MouseHandler : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            val logTable = contexts.getContext(LogTable::class.java)
            logTable ?: return
            val row = event.point.y * logTable.rowCount / height
            try {
                logTable.scrollRectToVisible(Rectangle(logTable.getCellRect(row, 0, true)))
            } catch (e: IllegalArgumentException) {
                GLog.e(TAG, "", e)
            }
            super.mouseClicked(event)
        }
    }

    companion object {
        private const val TAG = "VStatusPanel"
        const val VIEW_RECT_WIDTH = 20
        const val VIEW_RECT_HEIGHT = 5
    }
}
