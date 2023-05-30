package me.gegenbauer.catspy.ui.menu

import com.github.weisj.darklaf.settings.ThemeSettings
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.Menu.MENU_ITEM_ICON_SIZE
import me.gegenbauer.catspy.ui.dialog.LogCmdSettingsDialog
import me.gegenbauer.catspy.ui.log.LogLevel
import me.gegenbauer.catspy.ui.log.getLevelFromName
import me.gegenbauer.catspy.utils.findFrameFromParent
import me.gegenbauer.catspy.utils.loadDarklafThemedIcon
import java.awt.Dialog
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ItemListener
import java.awt.event.KeyEvent
import javax.swing.*

class SettingsMenu : JMenu() {
    private val itemLogCommand = JMenuItem("${STRINGS.ui.logCmd}(${STRINGS.ui.adb})")
    private val itemLogFile = JMenuItem(STRINGS.ui.logFile)
    // TODO itemFilterIncremental has no sense
    val itemDebug = JCheckBoxMenuItem(STRINGS.ui.debug)
    private val menuLogLevel = JMenu(STRINGS.ui.logLevel)
    private val logLevelGroup = ButtonGroup()

    private val levelItemHandler = ItemListener {
        when (it.source) {
            is JRadioButtonMenuItem -> {
                val item = it.source as JRadioButtonMenuItem
                if (item.isSelected) {
                    logLevel = item.text
                    onLogLevelChangedListener(getLevelFromName(item.text.first().toString()))
                }
            }
        }
    }
    private val itemThemeSettings = JMenuItem(STRINGS.ui.theme).apply {
        icon = loadDarklafThemedIcon("menu/themeSettings.svg", MENU_ITEM_ICON_SIZE)
        addActionListener { _: ActionEvent ->
            ThemeSettings.showSettingsDialog(this, Dialog.ModalityType.APPLICATION_MODAL)
        }
    }
    private val itemClickListener = ActionListener {
        when (it.source) {
            itemLogCommand -> openLogCommandConfigurationDialog()
            itemLogFile -> openLogCommandConfigurationDialog()
        }
    }
    var onLogLevelChangedListener: (LogLevel) -> Unit = {}
    private var logLevel: String = ""

    init {
        text = STRINGS.ui.setting
        mnemonic = KeyEvent.VK_S

        logLevel = UIConfManager.uiConf.logLevel
        logLevelGroup.apply {
            add(JRadioButtonMenuItem(LogLevel.VERBOSE.logName).apply {
                isSelected = logLevel == LogLevel.VERBOSE.logName
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(LogLevel.DEBUG.logName).apply {
                isSelected = logLevel == LogLevel.DEBUG.logName
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(LogLevel.INFO.logName).apply {
                isSelected = logLevel == LogLevel.INFO.logName
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(LogLevel.WARN.logName).apply {
                isSelected = logLevel == LogLevel.WARN.logName
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(LogLevel.ERROR.logName).apply {
                isSelected = logLevel == LogLevel.ERROR.logName
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(LogLevel.FATAL.logName).apply {
                isSelected = logLevel == LogLevel.FATAL.logName
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
        }

        itemLogFile.addActionListener(itemClickListener)
        itemLogCommand.addActionListener(itemClickListener)

        add(itemLogCommand)
        add(itemLogFile)
        addSeparator()
        add(menuLogLevel)
        addSeparator()
        add(itemThemeSettings)
        addSeparator()
        add(itemDebug)
    }

    private fun openLogCommandConfigurationDialog() {
        val frame = findFrameFromParent<JFrame>()
        val settingsDialog = LogCmdSettingsDialog(frame)
        settingsDialog.setLocationRelativeTo(frame)
        settingsDialog.isVisible = true
    }
}