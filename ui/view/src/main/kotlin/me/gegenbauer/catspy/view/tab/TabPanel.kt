package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Disposable
import me.gegenbauer.catspy.strings.STRINGS
import javax.swing.Icon
import javax.swing.JComponent

interface TabPanel: Disposable, Context {
    val tabName: String

    val tabIcon: Icon?

    val tabTooltip: String?
        get() = STRINGS.toolTip.tab

    val tabMnemonic: Char
        get() = ' '

    val closeable: Boolean
        get() = true

    fun onTabSelected()

    fun onTabUnselected()

    fun getTabContent(): JComponent
}