package me.gegenbauer.catspy.ui.menu

import com.github.weisj.darklaf.settings.ThemeSettings
import me.gegenbauer.catspy.configuration.Menu.MENU_ITEM_ICON_SIZE
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.menu.GMenu
import java.awt.Dialog
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JMenuItem

class SettingsMenu : GMenu() {
    private val itemThemeSettings = JMenuItem(STRINGS.ui.theme).apply {
        icon = GIcons.Menu.Settings.get(MENU_ITEM_ICON_SIZE, MENU_ITEM_ICON_SIZE)
        addActionListener { _: ActionEvent ->
            ThemeSettings.showSettingsDialog(this, Dialog.ModalityType.APPLICATION_MODAL)
        }
    }
    private val debugMenu = JMenu(STRINGS.ui.debug).apply {
        icon = GIcons.Menu.Debug.get(MENU_ITEM_ICON_SIZE, MENU_ITEM_ICON_SIZE)
    }
    val globalDebug = JCheckBoxMenuItem("Global")
    val bindingDebug = JCheckBoxMenuItem("DataBinding")
    val taskDebug = JCheckBoxMenuItem("Task")
    val ddmDebug = JCheckBoxMenuItem("Ddm")

    init {
        text = STRINGS.ui.setting
        mnemonic = KeyEvent.VK_S
        add(itemThemeSettings)
        addSeparator()
        debugMenu.add(globalDebug)
        debugMenu.add(bindingDebug)
        debugMenu.add(taskDebug)
        debugMenu.add(ddmDebug)
        add(debugMenu)
    }
}