package me.gegenbauer.logviewer.ui.menu

import com.github.weisj.darklaf.settings.ThemeSettings
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.log.LogCmdSettingsDialog
import me.gegenbauer.logviewer.ui.log.LogLevel
import me.gegenbauer.logviewer.ui.log.getLevelFromName
import me.gegenbauer.logviewer.ui.settings.AppearanceSettingsDialog
import me.gegenbauer.logviewer.utils.findFrameFromParent
import java.awt.event.ActionListener
import java.awt.event.ItemListener
import java.awt.event.KeyEvent
import javax.swing.*

class SettingsMenu : JMenu() {
    private val itemLogCommand = JMenuItem("${STRINGS.ui.logCmd}(${STRINGS.ui.adb})")
    private val itemLogFile = JMenuItem(STRINGS.ui.logFile)
    private val itemFilterIncremental = JCheckBoxMenuItem("${STRINGS.ui.filter}-${STRINGS.ui.incremental}")
    private val menuLogLevel = JMenu(STRINGS.ui.logLevel)
    private val logLevelGroup = ButtonGroup()

    private val itemAppearance = JMenuItem(STRINGS.ui.appearance);
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
    private val itemThemeSettings = JMenuItem(STRINGS.ui.theme)
    private val itemClickListener = ActionListener {
        when (it.source) {
            itemLogCommand -> openLogCommandConfigurationDialog()
            itemLogFile -> openLogCommandConfigurationDialog()
            itemFilterIncremental -> ConfigManager.getInstance()
                .saveItem(ConfigManager.ITEM_FILTER_INCREMENTAL, itemFilterIncremental.state.toString())
            itemAppearance -> openAppearanceConfigurationDialog()
            itemThemeSettings -> openThemeConfigurationDialog()
        }
    }
    private var onLogLevelChangedListener: (LogLevel) -> Unit = {}
    var logLevel: String = ""
    val filterIncremental: Boolean
        get() = itemFilterIncremental.state

    init {
        text = STRINGS.ui.setting
        mnemonic = KeyEvent.VK_S

        logLevel = ConfigManager.getInstance().getItem(ConfigManager.ITEM_LOG_LEVEL) ?: ""
        logLevelGroup.apply {
            add(JRadioButtonMenuItem(MainUI.VERBOSE).apply {
                isSelected = logLevel == MainUI.VERBOSE
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(MainUI.DEBUG).apply {
                isSelected = logLevel == MainUI.DEBUG
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(MainUI.INFO).apply {
                isSelected = logLevel == MainUI.INFO
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(MainUI.WARNING).apply {
                isSelected = logLevel == MainUI.WARNING
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(MainUI.ERROR).apply {
                isSelected = logLevel == MainUI.ERROR
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
            add(JRadioButtonMenuItem(MainUI.FATAL).apply {
                isSelected = logLevel == MainUI.FATAL
                menuLogLevel.add(this)
                addItemListener(levelItemHandler)
            })
        }

        val check = ConfigManager.getInstance().getItem(ConfigManager.ITEM_FILTER_INCREMENTAL)
        if (!check.isNullOrEmpty()) {
            itemFilterIncremental.state = check.toBoolean()
        } else {
            itemFilterIncremental.state = false
        }

        itemLogFile.addActionListener(itemClickListener)
        itemLogCommand.addActionListener(itemClickListener)
        itemFilterIncremental.addActionListener(itemClickListener)
        itemAppearance.addActionListener(itemClickListener)
        itemThemeSettings.addActionListener(itemClickListener)

        add(itemLogCommand)
        add(itemLogFile)
        addSeparator()
        add(menuLogLevel)
        addSeparator()
        add(itemFilterIncremental)
        addSeparator()
        add(itemAppearance)
        add(itemThemeSettings)
    }

    fun setLogLevelChangedListener(listener: (LogLevel) -> Unit) {
        onLogLevelChangedListener = listener
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

    private fun openThemeConfigurationDialog() {
        val frame = findFrameFromParent(this)
        ThemeSettings.showSettingsDialog(frame as MainUI)
    }
}