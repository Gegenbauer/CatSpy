package me.gegenbauer.catspy.common.ui.menu

import com.github.weisj.darklaf.settings.ThemeSettings
import me.gegenbauer.catspy.common.configuration.newFont
import me.gegenbauer.catspy.common.configuration.toFont
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.BorderFactory
import javax.swing.JMenu

open class GMenu: JMenu() {

    init {
        font = ThemeSettings.getInstance().theme.toFont().newFont(size = 14)
        border = BorderFactory.createEmptyBorder(4, 6, 4, 6)

        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                isPopupMenuVisible = false
            }
        })
    }
}