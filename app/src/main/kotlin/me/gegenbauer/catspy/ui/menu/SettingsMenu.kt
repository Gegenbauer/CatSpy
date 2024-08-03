package me.gegenbauer.catspy.ui.menu

import com.formdev.flatlaf.extras.FlatUIDefaultsInspector
import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.configuration.Menu.MENU_ITEM_ICON_SIZE
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.ui.customize.LogMetadataDialog
import me.gegenbauer.catspy.platform.isInDebugMode
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.dialog.GThemeSettingsDialog
import me.gegenbauer.catspy.utils.ui.findFrameFromParent
import me.gegenbauer.catspy.view.menu.GMenu
import java.awt.event.KeyEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JFrame
import javax.swing.JMenu
import javax.swing.JMenuItem

class SettingsMenu(override val contexts: Contexts = Contexts.default) : GMenu(), Context {
    private val itemThemeSettings = JMenuItem(STRINGS.ui.menuTheme).apply {
        icon = GIcons.Menu.Settings.get(MENU_ITEM_ICON_SIZE, MENU_ITEM_ICON_SIZE)
        addActionListener {
            val frame: JFrame = this@SettingsMenu.findFrameFromParent()
            val dialog = GThemeSettingsDialog(frame)
            dialog.setLocationRelativeTo(frame)
            dialog.isVisible = true
        }
    }
    private val debugMenu = JMenu(STRINGS.ui.menuDebug).apply {
        icon = GIcons.Menu.Debug.get(MENU_ITEM_ICON_SIZE, MENU_ITEM_ICON_SIZE)
    }
    val globalDebug = JCheckBoxMenuItem(GlobalStrings.DEBUG_GLOBAL)
    val bindingDebug = JCheckBoxMenuItem(GlobalStrings.DEBUG_DATA_BINDING)
    val taskDebug = JCheckBoxMenuItem(GlobalStrings.DEBUG_TASK)
    val ddmDebug = JCheckBoxMenuItem(GlobalStrings.DEBUG_DDM)
    val cacheDebug = JCheckBoxMenuItem(GlobalStrings.DEBUG_CACHE)
    val logDebug = JCheckBoxMenuItem(GlobalStrings.DEBUG_LOG)

    private val uiDefaultsInspector = JMenuItem(GlobalStrings.DEBUG_UI_DEFAULTS_INSPECTOR).apply {
        icon = GIcons.Menu.Debug.get(MENU_ITEM_ICON_SIZE, MENU_ITEM_ICON_SIZE)
    }

    private val logFormatCustomizationMenu = JMenuItem(STRINGS.ui.logCustomization).apply {
        icon = GIcons.Menu.Debug.get(MENU_ITEM_ICON_SIZE, MENU_ITEM_ICON_SIZE)
        addActionListener {
            val frame: JFrame = this@SettingsMenu.findFrameFromParent()
            val dialog = LogMetadataDialog(frame)
            dialog.setLocationRelativeTo(frame)
            dialog.isVisible = true
        }
    }

    init {
        text = STRINGS.ui.menuSettings
        mnemonic = KeyEvent.VK_S
        add(itemThemeSettings)
        add(logFormatCustomizationMenu)
        addSeparator()
        debugMenu.add(globalDebug)
        debugMenu.add(bindingDebug)
        debugMenu.add(taskDebug)
        debugMenu.add(ddmDebug)
        debugMenu.add(cacheDebug)
        debugMenu.add(logDebug)
        add(debugMenu)
        if (isInDebugMode) {
            add(uiDefaultsInspector)
        }

        uiDefaultsInspector.addActionListener {
            FlatUIDefaultsInspector.show()
        }
    }
}
