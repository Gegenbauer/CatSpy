package me.gegenbauer.catspy.view.menu

import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.BorderFactory
import javax.swing.JMenu

open class GMenu: JMenu() {

    init {
        //font = ThemeSettings.getInstance().theme.toFont().newFont(size = DEFAULT_FONT_SIZE)
        border = BorderFactory.createEmptyBorder(4, 6, 4, 6)

        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                isPopupMenuVisible = false
            }
        })
    }
}