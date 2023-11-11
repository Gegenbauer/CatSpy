package me.gegenbauer.catspy.log.binding

import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.log.getLevelFromName
import me.gegenbauer.catspy.log.nameToLogLevel
import me.gegenbauer.catspy.configuration.*
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.bind.bindLeft
import me.gegenbauer.catspy.databinding.property.support.*
import me.gegenbauer.catspy.java.ext.getEnum
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.editorComponent
import me.gegenbauer.catspy.view.combobox.HistoryComboBox
import me.gegenbauer.catspy.view.combobox.HistoryItem
import me.gegenbauer.catspy.view.combobox.toContentList
import me.gegenbauer.catspy.view.combobox.toHistoryItemList
import javax.swing.JToggleButton

class LogMainBinding : ContextService, GThemeChangeListener {

    //region Toolbar
    //region Filter
    val logFilterEnabled = ObservableValueProperty(UIConfManager.uiConf.logFilterEnabled)
    val logFilterHistory = ObservableValueProperty(UIConfManager.uiConf.logFilterHistory.toHistoryItemList())
    val logFilterSelectedIndex = ObservableValueProperty<Int>()
    val logFilterCurrentContent = ObservableValueProperty<String>()
    val logFilterErrorMessage = ObservableValueProperty<String>()

    val tagFilterEnabled = ObservableValueProperty(UIConfManager.uiConf.tagFilterEnabled)
    val tagFilterHistory = ObservableValueProperty(UIConfManager.uiConf.tagFilterHistory.toHistoryItemList())
    val tagFilterSelectedIndex = ObservableValueProperty<Int>()
    val tagFilterCurrentContent = ObservableValueProperty<String>()
    val tagFilterErrorMessage = ObservableValueProperty<String>()

    val pidFilterEnabled = ObservableValueProperty(UIConfManager.uiConf.pidFilterEnabled)
    val pidFilterHistory = ObservableValueProperty(arrayListOf<String>().toHistoryItemList())
    val pidFilterSelectedIndex = ObservableValueProperty<Int>()
    val pidFilterCurrentContent = ObservableValueProperty<String>()
    val pidFilterErrorMessage = ObservableValueProperty<String>()

    val tidFilterEnabled = ObservableValueProperty(UIConfManager.uiConf.tidFilterEnabled)
    val tidFilterHistory = ObservableValueProperty(arrayListOf<String>().toHistoryItemList())
    val tidFilterSelectedIndex = ObservableValueProperty<Int>()
    val tidFilterCurrentContent = ObservableValueProperty<String>()
    val tidFilterErrorMessage = ObservableValueProperty<String>()

    val logLevelFilterEnabled = ObservableValueProperty(UIConfManager.uiConf.logLevelFilterEnabled)
    private val sortedLogLevels = nameToLogLevel.toList().sortedBy { it.second.logLevel }.map { it.second.logName }
    val logLevelFilterHistory = ObservableValueProperty(sortedLogLevels.toHistoryItemList())
    val logLevelFilterCurrentContent = ObservableValueProperty(UIConfManager.uiConf.logLevel)
    val logLevelFilterSelectedIndex =
        ObservableValueProperty(sortedLogLevels.indexOf(UIConfManager.uiConf.logLevel))

    val boldEnabled = ObservableValueProperty(UIConfManager.uiConf.boldEnabled)
    val boldHistory = ObservableValueProperty(UIConfManager.uiConf.highlightHistory.toHistoryItemList())
    val boldSelectedIndex = ObservableValueProperty<Int>()
    val boldCurrentContent = ObservableValueProperty<String>()
    val boldErrorMessage = ObservableValueProperty<String>()

    val filterMatchCaseEnabled = ObservableValueProperty(UIConfManager.uiConf.filterMatchCaseEnabled)
    //endregion

    //region ADB
    val connectedDevices = ObservableValueProperty(arrayListOf<HistoryItem<String>>().toList())
    val deviceSelectedIndex = ObservableValueProperty<Int>()
    val currentDevice = ObservableValueProperty<String>()

    val pauseAll = ObservableValueProperty(false)

    val searchPanelVisible = ObservableValueProperty(false)
    //endregion

    //region Menu
    val rotation = ObservableValueProperty(getEnum<Rotation>(UIConfManager.uiConf.rotation))
    val logLevel = ObservableValueProperty(getLevelFromName(UIConfManager.uiConf.logLevel))
    //endregion

    //endregion

    //region SearchBar
    val searchHistory = ObservableValueProperty(UIConfManager.uiConf.searchHistory.toHistoryItemList())
    val searchSelectedIndex = ObservableValueProperty<Int>()
    val searchCurrentContent = ObservableValueProperty<String>()
    val searchMatchCase = ObservableValueProperty(UIConfManager.uiConf.searchMatchCaseEnabled)
    val searchErrorMessage = ObservableValueProperty<String>()
    //endregion

    //region LogPanel
    val splitPanelDividerLocation = ObservableValueProperty(UIConfManager.uiConf.dividerLocation)
    //endregion

    //region status bar
    val status = ObservableValueProperty("")
    val filePath = ObservableValueProperty(STRINGS.ui.none)
    //endregion

    //region Style
    val logFont = ObservableValueProperty(UIConfManager.uiConf.getLogFont())
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
            UIConfManager.uiConf.searchHistory.clear()
            UIConfManager.uiConf.searchHistory.addAll(it!!.toContentList())
        }
        logFilterHistory.addObserver {
            UIConfManager.uiConf.logFilterHistory.clear()
            UIConfManager.uiConf.logFilterHistory.addAll(it!!.toContentList())
        }
        tagFilterHistory.addObserver {
            UIConfManager.uiConf.tagFilterHistory.clear()
            UIConfManager.uiConf.tagFilterHistory.addAll(it!!.toContentList())
        }
        boldHistory.addObserver {
            UIConfManager.uiConf.highlightHistory.clear()
            UIConfManager.uiConf.highlightHistory.addAll(it!!.toContentList())
        }
        rotation.addObserver {
            UIConfManager.uiConf.rotation = it?.ordinal ?: Rotation.ROTATION_LEFT_RIGHT.ordinal
        }
        logLevel.addObserver {
            UIConfManager.uiConf.logLevel = it?.logName ?: LogLevel.WARN.logName
        }
        splitPanelDividerLocation.addObserver {
            UIConfManager.uiConf.dividerLocation = it ?: 500
        }
        logFilterEnabled.addObserver {
            UIConfManager.uiConf.logFilterEnabled = it ?: false
        }
        tagFilterEnabled.addObserver {
            UIConfManager.uiConf.tagFilterEnabled = it ?: false
        }
        pidFilterEnabled.addObserver {
            UIConfManager.uiConf.pidFilterEnabled = it ?: false
        }
        tidFilterEnabled.addObserver {
            UIConfManager.uiConf.tidFilterEnabled = it ?: false
        }
        logLevelFilterEnabled.addObserver {
            UIConfManager.uiConf.logLevelFilterEnabled = it ?: false
        }
        boldEnabled.addObserver {
            UIConfManager.uiConf.boldEnabled = it ?: false
        }
        filterMatchCaseEnabled.addObserver {
            UIConfManager.uiConf.filterMatchCaseEnabled = it ?: false
        }
        searchMatchCase.addObserver {
            UIConfManager.uiConf.searchMatchCaseEnabled = it ?: false
        }
        logFont.addObserver {
            UIConfManager.uiConf.logFontName = it?.fontName ?: "DialogInput"
            UIConfManager.uiConf.logFontSize = it?.size ?: DEFAULT_FONT_SIZE
            UIConfManager.uiConf.logFontStyle = it?.style ?: 0
        }
    }

    override fun onThemeChange(theme: Theme) {
        logFont.value?.let {
            logFont.updateValue(it.newFont(theme, DEFAULT_LOG_FONT_SIZE))
        }
    }
}