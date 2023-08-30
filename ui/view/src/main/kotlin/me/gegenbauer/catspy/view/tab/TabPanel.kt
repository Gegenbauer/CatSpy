package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Disposable
import javax.swing.Icon
import javax.swing.JComponent

interface TabPanel: Disposable, Context {
    val tabName: String

    val tabIcon: Icon?

    val tabTooltip: String?

    val tabMnemonic: Char

    val closeable: Boolean
        get() = true

    fun onTabSelected()

    fun onTabUnselected()

    fun getTabContent(): JComponent
}