package me.gegenbauer.catspy.ui.menu

import com.github.weisj.darklaf.settings.ThemeSettings
import me.gegenbauer.catspy.common.support.Menu.MENU_ITEM_ICON_SIZE
import me.gegenbauer.catspy.common.ui.menu.GMenu
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.utils.loadDarklafThemedIcon
import java.awt.Dialog
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JMenuItem

class SettingsMenu : GMenu() {
    // TODO itemFilterIncremental has no sense
    private val itemThemeSettings = JMenuItem(STRINGS.ui.theme).apply {
        icon = loadDarklafThemedIcon("menu/themeSettings.svg", MENU_ITEM_ICON_SIZE)
        addActionListener { _: ActionEvent ->
            ThemeSettings.showSettingsDialog(this, Dialog.ModalityType.APPLICATION_MODAL)
        }
    }
    private val debugMenu = JMenu(STRINGS.ui.debug)
    val bindingDebug = JCheckBoxMenuItem("DataBinding")
    val globalDebug = JCheckBoxMenuItem("Global")

    init {
        text = STRINGS.ui.setting
        mnemonic = KeyEvent.VK_S
        add(itemThemeSettings)
        addSeparator()
        debugMenu.add(globalDebug)
        debugMenu.add(bindingDebug)
        add(debugMenu)
    }
}