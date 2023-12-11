package me.gegenbauer.catspy.log.binding

import com.formdev.flatlaf.FlatLaf
import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.catspy.configuration.*
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.bind.bindLeft
import me.gegenbauer.catspy.databinding.property.support.*
import me.gegenbauer.catspy.java.ext.getEnum
import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.log.getLevelFromName
import me.gegenbauer.catspy.log.nameToLogLevel
import me.gegenbauer.catspy.utils.editorComponent
import me.gegenbauer.catspy.view.combobox.HistoryComboBox
import me.gegenbauer.catspy.view.combobox.HistoryItem
import me.gegenbauer.catspy.view.combobox.toStrContentList
import me.gegenbauer.catspy.view.combobox.toStrHistoryList
import javax.swing.JToggleButton

class LogMainBinding : ContextService, GThemeChangeListener {

    //region Toolbar
    //region Filter
    val logFilterEnabled = ObservableValueProperty(SettingsManager.settings.logFilterEnabled)
    val logFilterHistory = ObservableValueProperty(SettingsManager.settings.logFilterHistory.toStrHistoryList())
    val logFilterSelectedIndex = ObservableValueProperty<Int>()
    val logFilterCurrentContent = ObservableValueProperty<String>()
    val logFilterErrorMessage = ObservableValueProperty<String>()

    val tagFilterEnabled = ObservableValueProperty(SettingsManager.settings.tagFilterEnabled)
    val tagFilterHistory = ObservableValueProperty(SettingsManager.settings.tagFilterHistory.toStrHistoryList())
    val tagFilterSelectedIndex = ObservableValueProperty<Int>()
    val tagFilterCurrentContent = ObservableValueProperty<String>()
    val tagFilterErrorMessage = ObservableValueProperty<String>()

    val pidFilterEnabled = ObservableValueProperty(SettingsManager.settings.pidFilterEnabled)
    val pidFilterHistory = ObservableValueProperty(arrayListOf<String>().toStrHistoryList())
    val pidFilterSelectedIndex = ObservableValueProperty<Int>()
    val pidFilterCurrentContent = ObservableValueProperty<String>()
    val pidFilterErrorMessage = ObservableValueProperty<String>()

    val tidFilterEnabled = ObservableValueProperty(SettingsManager.settings.tidFilterEnabled)
    val tidFilterHistory = ObservableValueProperty(arrayListOf<String>().toStrHistoryList())
    val tidFilterSelectedIndex = ObservableValueProperty<Int>()
    val tidFilterCurrentContent = ObservableValueProperty<String>()
    val tidFilterErrorMessage = ObservableValueProperty<String>()

    val logLevelFilterEnabled = ObservableValueProperty(SettingsManager.settings.logLevelFilterEnabled)
    private val sortedLogLevels = nameToLogLevel.toList().sortedBy { it.second.logLevel }.map { it.second.logName }
    val logLevelFilterHistory = ObservableValueProperty(sortedLogLevels.toStrHistoryList())
    val logLevelFilterCurrentContent = ObservableValueProperty(SettingsManager.settings.logLevel)
    val logLevelFilterSelectedIndex =
        ObservableValueProperty(sortedLogLevels.indexOf(SettingsManager.settings.logLevel))

    val boldEnabled = ObservableValueProperty(SettingsManager.settings.boldEnabled)
    val boldHistory = ObservableValueProperty(SettingsManager.settings.highlightHistory.toStrHistoryList())
    val boldSelectedIndex = ObservableValueProperty<Int>()
    val boldCurrentContent = ObservableValueProperty<String>()
    val boldErrorMessage = ObservableValueProperty<String>()

    val filterMatchCaseEnabled = ObservableValueProperty(SettingsManager.settings.filterMatchCaseEnabled)
    //endregion

    //region ADB
    val connectedDevices = ObservableValueProperty(arrayListOf<HistoryItem<String>>().toList())
    val deviceSelectedIndex = ObservableValueProperty<Int>()
    val currentDevice = ObservableValueProperty<String>()

    val pauseAll = ObservableValueProperty(false)

    val searchPanelVisible = ObservableValueProperty(false)
    //endregion

    //region Menu
    val rotation = ObservableValueProperty(getEnum<Rotation>(SettingsManager.settings.rotation))
    val logLevel = ObservableValueProperty(getLevelFromName(SettingsManager.settings.logLevel))
    //endregion

    //endregion

    //region SearchBar
    val searchHistory = ObservableValueProperty(SettingsManager.settings.searchHistory.toStrHistoryList())
    val searchSelectedIndex = ObservableValueProperty<Int>()
    val searchCurrentContent = ObservableValueProperty<String>()
    val searchMatchCase = ObservableValueProperty(SettingsManager.settings.searchMatchCaseEnabled)
    val searchErrorMessage = ObservableValueProperty<String>()
    //endregion

    //region LogPanel
    val splitPanelDividerLocation = ObservableValueProperty(SettingsManager.settings.dividerLocation)
    //endregion

    //region Style
    val logFont = ObservableValueProperty(SettingsManager.settings.logFont)
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
                searchHistory.clear()
                searchHistory.addAll(it!!.toStrContentList())
            }
        }
        logFilterHistory.addObserver {
            SettingsManager.updateSettings {
                logFilterHistory.clear()
                logFilterHistory.addAll(it!!.toStrContentList())
            }
        }
        tagFilterHistory.addObserver {
            SettingsManager.updateSettings {
                tagFilterHistory.clear()
                tagFilterHistory.addAll(it!!.toStrContentList())
            }
        }
        boldHistory.addObserver {
            SettingsManager.updateSettings {
                highlightHistory.clear()
                highlightHistory.addAll(it!!.toStrContentList())
            }
        }
        rotation.addObserver {
            SettingsManager.updateSettings {
                rotation = it?.ordinal ?: Rotation.ROTATION_LEFT_RIGHT.ordinal
            }
        }
        logLevel.addObserver {
            SettingsManager.updateSettings {
                logLevel = it?.logName ?: LogLevel.WARN.logName
            }
        }
        splitPanelDividerLocation.addObserver {
            SettingsManager.updateSettings {
                dividerLocation = it ?: 500
            }
        }
        logFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                logFilterEnabled = it ?: false
            }
        }
        tagFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                tagFilterEnabled = it ?: false
            }
        }
        pidFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                pidFilterEnabled = it ?: false
            }
        }
        tidFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                tidFilterEnabled = it ?: false
            }
        }
        logLevelFilterEnabled.addObserver {
            SettingsManager.updateSettings {
                logLevelFilterEnabled = it ?: false
            }
        }
        boldEnabled.addObserver {
            SettingsManager.updateSettings {
                boldEnabled = it ?: false
            }
        }
        filterMatchCaseEnabled.addObserver {
            SettingsManager.updateSettings {
                filterMatchCaseEnabled = it ?: false
            }
        }
        searchMatchCase.addObserver {
            SettingsManager.updateSettings {
                searchMatchCaseEnabled = it ?: false
            }
        }
        logFont.addObserver {
            SettingsManager.updateSettings {
                logFontName = it?.fontName ?: "DialogInput"
                logFontSize = it?.size ?: DEFAULT_FONT_SIZE
                logFontStyle = it?.style ?: 0
            }
        }
    }

    override fun onThemeChange(theme: FlatLaf) {
        logFont.value?.let {
            //logFont.updateValue(it.newFont(theme, DEFAULT_LOG_FONT_SIZE))
        }
    }
}