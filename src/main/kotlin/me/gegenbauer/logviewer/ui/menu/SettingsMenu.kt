package me.gegenbauer.logviewer.ui.menu

import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.log.LogCmdSettingsDialog
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_DEBUG
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_ERROR
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_FATAL
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_INFO
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_NONE
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_VERBOSE
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_WARNING
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
                    onLogLevelChangedListener(parseLogLevel(item.text))
                }
            }
        }
    }
    private val itemClickListener = ActionListener {
        when (it.source) {
            itemLogCommand -> openLogCommandConfigurationDialog()
            itemLogFile -> openLogCommandConfigurationDialog()
            itemFilterIncremental -> ConfigManager.getInstance()
                .saveItem(ConfigManager.ITEM_FILTER_INCREMENTAL, itemFilterIncremental.state.toString())

            itemAppearance -> openAppearanceConfigurationDialog()
        }
    }
    private var onLogLevelChangedListener: (Int) -> Unit = {}
    var logLevel: String? = null
    val filterIncremental: Boolean
        get() = itemFilterIncremental.state

    init {
        text = STRINGS.ui.setting
        mnemonic = KeyEvent.VK_S

        logLevel = ConfigManager.getInstance().getItem(ConfigManager.ITEM_LOG_LEVEL)
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

        add(itemLogCommand)
        add(itemLogFile)
        addSeparator()
        add(menuLogLevel)
        addSeparator()
        add(itemFilterIncremental)
        addSeparator()
        add(itemAppearance)
    }

    fun setLogLevelChangedListener(listener: (Int) -> Unit) {
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

    companion object {
        fun parseLogLevel(level: String): Int = when (level) {
            MainUI.VERBOSE -> LEVEL_VERBOSE
            MainUI.DEBUG -> LEVEL_DEBUG
            MainUI.INFO -> LEVEL_INFO
            MainUI.WARNING -> LEVEL_WARNING
            MainUI.ERROR -> LEVEL_ERROR
            MainUI.FATAL -> LEVEL_FATAL
            else -> LEVEL_NONE
        }

        fun parseLogLevel(level: Int): String = when (level) {
            LEVEL_VERBOSE -> MainUI.VERBOSE
            LEVEL_DEBUG -> MainUI.DEBUG
            LEVEL_INFO -> MainUI.INFO
            LEVEL_WARNING -> MainUI.WARNING
            LEVEL_ERROR -> MainUI.ERROR
            LEVEL_FATAL -> MainUI.FATAL
            else -> throw IllegalArgumentException("Unknown log level: $level")
        }
    }
}