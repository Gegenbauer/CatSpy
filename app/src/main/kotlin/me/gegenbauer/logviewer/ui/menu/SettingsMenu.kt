package me.gegenbauer.logviewer.ui.menu

import com.github.weisj.darklaf.settings.ThemeSettings
import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.dialog.AppearanceSettingsDialog
import me.gegenbauer.logviewer.ui.dialog.LogCmdSettingsDialog
import me.gegenbauer.logviewer.ui.log.LogLevel
import me.gegenbauer.logviewer.ui.log.getLevelFromName
import me.gegenbauer.logviewer.utils.findFrameFromParent
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ItemListener
import java.awt.event.KeyEvent
import javax.swing.*

class SettingsMenu : JMenu() {
    private val itemLogCommand = JMenuItem("${STRINGS.ui.logCmd}(${STRINGS.ui.adb})")
    private val itemLogFile = JMenuItem(STRINGS.ui.logFile)
    private val itemFilterIncremental = JCheckBoxMenuItem("${STRINGS.ui.filter}-${STRINGS.ui.incremental}")
    private val itemDebug = JCheckBoxMenuItem(STRINGS.ui.debug)
    private val menuLogLevel = JMenu(STRINGS.ui.logLevel)
    private val logLevelGroup = ButtonGroup()

    private val itemAppearance = JMenuItem(STRINGS.ui.appearance)
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
        icon = ThemeSettings.getIcon()
        addActionListener { _: ActionEvent ->
            ThemeSettings.showSettingsDialog(this)
        }
    }
    private val itemClickListener = ActionListener {
        when (it.source) {
            itemLogCommand -> openLogCommandConfigurationDialog()
            itemLogFile -> openLogCommandConfigurationDialog()
            itemFilterIncremental -> UIConfManager.uiConf.filterIncrementalEnabled = itemFilterIncremental.state
            itemDebug -> {
                UIConfManager.uiConf.debug = itemDebug.state
                GLog.DEBUG = itemDebug.state
            }
            itemAppearance -> openAppearanceConfigurationDialog()
        }
    }
    var onLogLevelChangedListener: (LogLevel) -> Unit = {}
    var logLevel: String = ""
    val filterIncremental: Boolean
        get() = itemFilterIncremental.state

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

        itemFilterIncremental.state = UIConfManager.uiConf.filterIncrementalEnabled
        itemDebug.state = UIConfManager.uiConf.debug

        itemLogFile.addActionListener(itemClickListener)
        itemLogCommand.addActionListener(itemClickListener)
        itemFilterIncremental.addActionListener(itemClickListener)
        itemDebug.addActionListener(itemClickListener)
        itemAppearance.addActionListener(itemClickListener)

        add(itemLogCommand)
        add(itemLogFile)
        addSeparator()
        add(menuLogLevel)
        addSeparator()
        add(itemFilterIncremental)
        addSeparator()
        add(itemAppearance)
        add(itemThemeSettings)
        addSeparator()
        add(itemDebug)
    }

    private fun openLogCommandConfigurationDialog() {
        val frame = findFrameFromParent(this)
        val settingsDialog = LogCmdSettingsDialog(frame as MainUI)
        settingsDialog.setLocationRelativeTo(frame)
        settingsDialog.isVisible = true
    }

    private fun openAppearanceConfigurationDialog() {
        val frame = findFrameFromParent(this)
        val appearanceSettingsDialog = AppearanceSettingsDialog(frame as MainUI)
        appearanceSettingsDialog.setLocationRelativeTo(frame)
        appearanceSettingsDialog.isVisible = true
    }
}