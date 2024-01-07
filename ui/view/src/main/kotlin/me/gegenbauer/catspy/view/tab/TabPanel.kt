package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.hint.HintManager
import javax.swing.Icon
import javax.swing.JComponent

interface TabPanel : Context {
    val tabName: String

    val tabIcon: Icon?

    val hint: HintManager.Hint?
        get() = null

    val tabTooltip: String?
        get() = STRINGS.toolTip.tab

    val tabMnemonic: Char
        get() = ' '

    val closeable: Boolean
        get() = true

    fun setup()

    fun onTabSelected()

    fun onTabUnselected()

    fun getTabContent(): JComponent
}