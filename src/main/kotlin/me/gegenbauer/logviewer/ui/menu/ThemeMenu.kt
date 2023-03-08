package me.gegenbauer.logviewer.ui.menu

import com.github.weisj.darklaf.theme.DarculaTheme
import com.github.weisj.darklaf.theme.IntelliJTheme
import me.gegenbauer.logviewer.changeTheme
import me.gegenbauer.logviewer.strings.STRINGS
import java.awt.event.ActionListener
import javax.swing.JMenu
import javax.swing.JMenuItem

class ThemeMenu: JMenu() {
    private val itemDarculaTheme: JMenuItem = JMenuItem(STRINGS.ui.darcula)
    private val itemIntelliJTheme: JMenuItem = JMenuItem(STRINGS.ui.intelliJ)

    private val actionHandler = ActionListener { e ->
        when (e.source) {
            itemDarculaTheme -> {
                changeTheme(DarculaTheme())
            }
            itemIntelliJTheme -> {
                changeTheme(IntelliJTheme())
            }
        }
    }

    init {
        text = STRINGS.ui.theme
        add(itemDarculaTheme)
        add(itemIntelliJTheme)
        itemDarculaTheme.addActionListener(actionHandler)
        itemIntelliJTheme.addActionListener(actionHandler)
    }
}