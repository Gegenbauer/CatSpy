package me.gegenbauer.catspy.log.binding

import com.formdev.flatlaf.FlatLaf
import me.gegenbauer.catspy.configuration.GThemeChangeListener
import me.gegenbauer.catspy.configuration.Rotation
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.bind.bindLeft
import me.gegenbauer.catspy.databinding.property.support.*
import me.gegenbauer.catspy.file.clone
import me.gegenbauer.catspy.glog.LogLevel
import me.gegenbauer.catspy.java.ext.getEnum
import me.gegenbauer.catspy.glog.nameToLogLevel
import me.gegenbauer.catspy.utils.editorComponent
import me.gegenbauer.catspy.view.combobox.HistoryComboBox
import me.gegenbauer.catspy.view.combobox.HistoryItem
import me.gegenbauer.catspy.view.combobox.toStrContentList
import me.gegenbauer.catspy.view.combobox.toStrHistoryList
import javax.swing.JToggleButton

class LogMainBinding : ContextService, GThemeChangeListener {
    private val logSettings = currentSettings.logSettings

    //region Toolbar
    //region Filter
    val logFilterEnabled = ObservableValueProperty(logSettings.filterEnabledState.logFilterEnabled)
    val logFilterHistory = ObservableValueProperty(logSettings.filterHistory.logFilterHistory.toStrHistoryList())
    val logFilterSelectedIndex = ObservableValueProperty<Int>()
    val logFilterCurrentContent = ObservableValueProperty<String>()
    val logFilterErrorMessage = ObservableValueProperty<String>()

    val tagFilterEnabled = ObservableValueProperty(logSettings.filterEnabledState.tagFilterEnabled)
    val tagFilterHistory = ObservableValueProperty(logSettings.filterHistory.tagFilterHistory.toStrHistoryList())
    val tagFilterSelectedIndex = ObservableValueProperty<Int>()
    val tagFilterCurrentContent = ObservableValueProperty<String>()
    val tagFilterErrorMessage = ObservableValueProperty<String>()

    val pidFilterEnabled = ObservableValueProperty(logSettings.filterEnabledState.pidFilterEnabled)
    val pidFilterHistory = ObservableValueProperty(arrayListOf<String>().toStrHistoryList())
    val pidFilterSelectedIndex = ObservableValueProperty<Int>()
    val pidFilterCurrentContent = ObservableValueProperty<String>()
    val pidFilterErrorMessage = ObservableValueProperty<String>()

    val packageFilterEnabled = ObservableValueProperty(logSettings.filterEnabledState.packageFilterEnabled)
    val packageFilterHistory = ObservableValueProperty(logSettings.filterHistory.packageFilterHistory.toStrHistoryList())
    val packageFilterSelectedIndex = ObservableValueProperty<Int>()
    val packageFilterCurrentContent = ObservableValueProperty<String>()
    val packageFilterErrorMessage = ObservableValueProperty<String>()

    val tidFilterEnabled = ObservableValueProperty(logSettings.filterEnabledState.tidFilterEnabled)
    val tidFilterHistory = ObservableValueProperty(arrayListOf<String>().toStrHistoryList())
    val tidFilterSelectedIndex = ObservableValueProperty<Int>()
    val tidFilterCurrentContent = ObservableValueProperty<String>()
    val tidFilterErrorMessage = ObservableValueProperty<String>()

    val logLevelFilterEnabled = ObservableValueProperty(logSettings.filterEnabledState.logLevelFilterEnabled)
    private val sortedLogLevels = nameToLogLevel.toList().sortedBy { it.second.intValue }.map { it.second.logName }
    val logLevelFilterHistory = ObservableValueProperty(sortedLogLevels.toStrHistoryList())
    val logLevelFilterCurrentContent = ObservableValueProperty(logSettings.logLevel)
    val logLevelFilterSelectedIndex =
        ObservableValueProperty(sortedLogLevels.indexOf(logSettings.logLevel))

    val boldEnabled = ObservableValueProperty(logSettings.filterEnabledState.boldEnabled)
    val boldHistory = ObservableValueProperty(logSettings.filterHistory.highlightHistory.toStrHistoryList())
    val boldSelectedIndex = ObservableValueProperty<Int>()
    val boldCurrentContent = ObservableValueProperty<String>()
    val boldErrorMessage = ObservableValueProperty<String>()

    val filterMatchCaseEnabled = ObservableValueProperty(logSettings.filterEnabledState.filterMatchCaseEnabled)
    //endregion

    //region ADB
    val adbServerStatusWarningVisibility = ObservableValueProperty(false)
    val connectedDevices = ObservableValueProperty(arrayListOf<HistoryItem<String>>().toList())
    val deviceSelectedIndex = ObservableValueProperty<Int>()
    val currentDevice = ObservableValueProperty<String>()

    val pauseAll = ObservableValueProperty(false)

    val searchPanelVisible = ObservableValueProperty(false)
    //endregion

    //region Menu
    val rotation = ObservableValueProperty(getEnum<Rotation>(logSettings.rotation))
    //endregion

    //endregion

    //region SearchBar
    val searchHistory = ObservableValueProperty(logSettings.search.searchHistory.toStrHistoryList())
    val searchSelectedIndex = ObservableValueProperty<Int>()
    val searchCurrentContent = ObservableValueProperty<String>()
    val searchMatchCase = ObservableValueProperty(logSettings.search.searchMatchCaseEnabled)
    val searchErrorMessage = ObservableValueProperty<String>()
    //endregion

    //region LogPanel
    val splitPanelDividerLocation = ObservableValueProperty(logSettings.dividerLocation)
    //endregion

    //region Style
    val logFont = ObservableValueProperty(logSettings.font)
    //endregion

    fun bindLogFilter(
        comboBox: HistoryComboBox<String>,
        toggle: JToggleButton,
        selectedIndexProperty: ObservableValueProperty<Int>,
        listProperty: ObservableValueProperty<List<HistoryItem<String>>>,
        enabledProperty: ObservableValueProperty<Boolean>,
        editorContentProperty: ObservableValueProperty<String>,
        errorMessageProperty: ObservableValueProperty<String>? = null,
    ) {
        selectedProperty(toggle) bindDual enabledProperty
        enabledProperty(comboBox) bindDual enabledProperty
        listProperty(comboBox) bindDual listProperty
        selectedIndexProperty(comboBox) bindDual selectedIndexProperty
        textProperty(comboBox.editorComponent) bindDual editorContentProperty
        customProperty(comboBox, "errorMsg", "") bindLeft errorMessageProperty
    }

    fun bindNormalCombo(
        comboBox: HistoryComboBox<String>,
        selectedIndexProperty: ObservableValueProperty<Int>,
        listProperty: ObservableValueProperty<List<HistoryItem<String>>>,
        editorContentProperty: ObservableValueProperty<String>,
    ) {
        listProperty(comboBox) bindDual listProperty
        selectedIndexProperty(comboBox) bindLeft selectedIndexProperty
        textProperty(comboBox.editorComponent) bindDual editorContentProperty
    }

    fun syncGlobalConfWithMainBindings() {
        searchHistory.addObserver {
            SettingsManager.updateSettings {
                logSettings.search.searchHistory.clear()
                logSettings.search.searchHistory.addAll(it!!.toStrContentList())
            }
        }
        logFilterHistory.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterHistory.logFilterHistory.clear()
                logSettings.filterHistory.logFilterHistory.addAll(it!!.toStrContentList())
            }
        }
        packageFilterHistory.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterHistory.packageFilterHistory.clear()
                logSettings.filterHistory.packageFilterHistory.addAll(it!!.toStrContentList())
            }
        }
        tagFilterHistory.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterHistory.tagFilterHistory.clear()
                logSettings.filterHistory.tagFilterHistory.addAll(it!!.toStrContentList())
            }
        }
        boldHistory.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterHistory.highlightHistory.clear()
                logSettings.filterHistory.highlightHistory.addAll(it!!.toStrContentList())
            }
        }
        rotation.addObserver {
            SettingsManager.updateSettings {
                logSettings.rotation = it?.ordinal ?: Rotation.ROTATION_LEFT_RIGHT.ordinal
            }
        }
        logLevelFilterCurrentContent.addObserver {
            SettingsManager.updateSettings {
                logSettings.logLevel = it ?: LogLevel.VERBOSE.logName
            }
        }
        splitPanelDividerLocation.addObserver {
            SettingsManager.updateSettings {
                logSettings.dividerLocation = it ?: 500
            }
        }
        logFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterEnabledState.logFilterEnabled = it ?: false
            }
        }
        tagFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterEnabledState.tagFilterEnabled = it ?: false
            }
        }
        pidFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterEnabledState.pidFilterEnabled = it ?: false
            }
        }
        packageFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterEnabledState.packageFilterEnabled = it ?: false
            }
        }
        tidFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterEnabledState.tidFilterEnabled = it ?: false
            }
        }
        logLevelFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterEnabledState.logLevelFilterEnabled = it ?: false
            }
        }
        boldEnabled.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterEnabledState.boldEnabled = it ?: false
            }
        }
        filterMatchCaseEnabled.addObserver {
            SettingsManager.updateSettings {
                logSettings.filterEnabledState.filterMatchCaseEnabled = it ?: false
            }
        }
        searchMatchCase.addObserver {
            SettingsManager.updateSettings {
                logSettings.search.searchMatchCaseEnabled = it ?: false
            }
        }
        logFont.addObserver {
            SettingsManager.updateSettings {
                logSettings.font = it?.run { clone(this) } ?: logSettings.font
            }
        }
    }

    // TODO customize log font
    override fun onThemeChange(theme: FlatLaf) {
        logFont.value?.let {
            //logFont.updateValue(it.newFont(theme, DEFAULT_LOG_FONT_SIZE))
        }
    }
}