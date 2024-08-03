package me.gegenbauer.catspy.view.combobox

import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JList
import javax.swing.SwingUtilities

class CellButtonsMouseListener : MouseAdapter() {

    override fun mouseMoved(e: MouseEvent) {
        val list = e.component as JList<*>
        val pt = e.point
        val index = list.locationToIndex(pt)
        val renderer = list.cellRenderer as PopupListRenderer<*>
        renderer.rolloverIndex = if (getButton(list, pt, index) != null) index else -1
        list.repaint() // repaint all cells
    }

    override fun mousePressed(e: MouseEvent) {
        e.component.repaint() // repaint all cells
    }

    override fun mouseReleased(e: MouseEvent) {
        val list = e.component as JList<*>
        val pt = e.point
        val index = list.locationToIndex(pt)
        if (index >= 0) {
            val button = getButton(list, pt, index)
            button?.doClick()
        }
        (list.cellRenderer as PopupListRenderer<*>).rolloverIndex = -1
        list.repaint() // repaint all cells
    }

    override fun mouseExited(e: MouseEvent) {
        val list = e.component as JList<*>
        (list.cellRenderer as PopupListRenderer<*>).rolloverIndex = -1
    }

    private fun <E> getButton(list: JList<E>, pt: Point, index: Int): JButton? {
        val proto = list.prototypeCellValue
        val renderer = list.cellRenderer
        val c = renderer.getListCellRendererComponent(list, proto, index, false, false)
        val r = list.getCellBounds(index, index)
        c.bounds = r
        pt.translate(-r.x, -r.y)
        return SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y) as? JButton
    }
}