package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.ui.panel.TabInfo
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class TabSelectorPopupMenu : JPopupMenu() {

    var onTabSelected: ((TabInfo) -> Unit)? = null

    fun show(tabs: List<TabInfo>, anchor: JComponent) {
        removeAll()
        tabs.forEach { tab ->
            val item = JMenuItem(tab.tabName, tab.tabIcon)
            item.toolTipText = tab.tooltip
            item.addActionListener {
                onTabSelected?.invoke(tab)
            }
            add(item)
        }

        show(anchor, 0, anchor.height)
    }
}