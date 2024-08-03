package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.view.tab.FunctionTab
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class TabSelectorPopupMenu : JPopupMenu() {

    var onTabSelected: ((FunctionTab) -> Unit)? = null

    fun show(tabs: List<FunctionTab>, anchor: JComponent) {
        removeAll()
        tabs.forEach { tab ->
            val item = JMenuItem(tab.name, tab.icon)
            item.toolTipText = tab.tooltip
            item.addActionListener {
                onTabSelected?.invoke(tab)
            }
            add(item)
        }

        show(anchor, 0, anchor.height)
    }
}