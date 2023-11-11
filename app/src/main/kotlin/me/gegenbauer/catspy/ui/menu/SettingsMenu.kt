package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.configuration.Menu.MENU_ITEM_ICON_SIZE
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.Configuration
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.dialog.GThemeSettingsDialog
import me.gegenbauer.catspy.utils.findFrameFromParent
import me.gegenbauer.catspy.view.menu.GMenu
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JFrame
import javax.swing.JMenu
import javax.swing.JMenuItem

class SettingsMenu(override val contexts: Contexts = Contexts.default) : GMenu(), Context {
    private val itemThemeSettings = JMenuItem(STRINGS.ui.theme).apply {
        icon = GIcons.Menu.Settings.get(MENU_ITEM_ICON_SIZE, MENU_ITEM_ICON_SIZE)
        addActionListener { _: ActionEvent ->
            val frame: JFrame = this@SettingsMenu.findFrameFromParent()
            val dialog = GThemeSettingsDialog(frame)
            dialog.setLocationRelativeTo(frame)
            dialog.isVisible = true
        }
    }
    private val debugMenu = JMenu(STRINGS.ui.debug).apply {
        icon = GIcons.Menu.Debug.get(MENU_ITEM_ICON_SIZE, MENU_ITEM_ICON_SIZE)
    }
    val globalDebug = JCheckBoxMenuItem(Configuration.DEBUG_GLOBAL)
    val bindingDebug = JCheckBoxMenuItem(Configuration.DEBUG_DATA_BINDING)
    val taskDebug = JCheckBoxMenuItem(Configuration.DEBUG_TASK)
    val ddmDebug = JCheckBoxMenuItem(Configuration.DEBUG_DDM)
    val cacheDebug = JCheckBoxMenuItem(Configuration.DEBUG_CACHE)
    val logDebug = JCheckBoxMenuItem(Configuration.DEBUG_LOG)

    init {
        text = STRINGS.ui.setting
        mnemonic = KeyEvent.VK_S
        add(itemThemeSettings)
        addSeparator()
        debugMenu.add(globalDebug)
        debugMenu.add(bindingDebug)
        debugMenu.add(taskDebug)
        debugMenu.add(ddmDebug)
        debugMenu.add(cacheDebug)
        debugMenu.add(logDebug)
        add(debugMenu)
    }
}