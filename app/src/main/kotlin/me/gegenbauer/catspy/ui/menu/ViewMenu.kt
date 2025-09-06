package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.view.menu.GMenu
import java.awt.event.KeyEvent
import javax.swing.JCheckBoxMenuItem

class ViewMenu(override val contexts: Contexts = Contexts.default) : GMenu(), Context {
    val showLogToolbar = JCheckBoxMenuItem(STRINGS.ui.showLogToolbar).applyTooltip(STRINGS.toolTip.showLogToolbar)
    val showFilterPanel = JCheckBoxMenuItem(STRINGS.ui.showFilterPanel).applyTooltip(STRINGS.toolTip.showFilterPanel)
    val showStatusBar = JCheckBoxMenuItem(STRINGS.ui.showStatusBar).applyTooltip(STRINGS.toolTip.showStatusBar)
    val showLogTableColumnNames = JCheckBoxMenuItem(STRINGS.ui.showLogTableColumnNames).applyTooltip(STRINGS.toolTip.showLogTableColumnNames)
    val showLogPanelToolbar = JCheckBoxMenuItem(STRINGS.ui.showLogPanelToolbar).applyTooltip(STRINGS.toolTip.showLogPanelToolbar)

    init {
        text = STRINGS.ui.menuView
        mnemonic = KeyEvent.VK_V

        add(showLogToolbar)
        add(showFilterPanel)
        add(showLogPanelToolbar)
        add(showLogTableColumnNames)
        add(showStatusBar)
    }
}