package me.gegenbauer.catspy.log.binding

import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.log.getLevelFromName
import me.gegenbauer.catspy.log.nameToLogLevel
import me.gegenbauer.catspy.configuration.*
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
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
    val logFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.logFilterEnabled)
    val logFilterHistory = ObservableViewModelProperty(UIConfManager.uiConf.logFilterHistory.toHistoryItemList())
    val logFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val logFilterCurrentContent = ObservableViewModelProperty<String>()
    val logFilterErrorMessage = ObservableViewModelProperty<String>()

    val tagFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.tagFilterEnabled)
    val tagFilterHistory = ObservableViewModelProperty(UIConfManager.uiConf.tagFilterHistory.toHistoryItemList())
    val tagFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val tagFilterCurrentContent = ObservableViewModelProperty<String>()
    val tagFilterErrorMessage = ObservableViewModelProperty<String>()

    val pidFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.pidFilterEnabled)
    val pidFilterHistory = ObservableViewModelProperty(arrayListOf<String>().toHistoryItemList())
    val pidFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val pidFilterCurrentContent = ObservableViewModelProperty<String>()
    val pidFilterErrorMessage = ObservableViewModelProperty<String>()

    val tidFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.tidFilterEnabled)
    val tidFilterHistory = ObservableViewModelProperty(arrayListOf<String>().toHistoryItemList())
    val tidFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val tidFilterCurrentContent = ObservableViewModelProperty<String>()
    val tidFilterErrorMessage = ObservableViewModelProperty<String>()

    val logLevelFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.logLevelFilterEnabled)
    private val sortedLogLevels = nameToLogLevel.toList().sortedBy { it.second.logLevel }.map { it.second.logName }
    val logLevelFilterHistory = ObservableViewModelProperty(sortedLogLevels.toHistoryItemList())
    val logLevelFilterCurrentContent = ObservableViewModelProperty(UIConfManager.uiConf.logLevel)
    val logLevelFilterSelectedIndex =
        ObservableViewModelProperty(sortedLogLevels.indexOf(UIConfManager.uiConf.logLevel))

    val boldEnabled = ObservableViewModelProperty(UIConfManager.uiConf.boldEnabled)
    val boldHistory = ObservableViewModelProperty(UIConfManager.uiConf.highlightHistory.toHistoryItemList())
    val boldSelectedIndex = ObservableViewModelProperty<Int>()
    val boldCurrentContent = ObservableViewModelProperty<String>()
    val boldErrorMessage = ObservableViewModelProperty<String>()

    val filterMatchCaseEnabled = ObservableViewModelProperty(UIConfManager.uiConf.filterMatchCaseEnabled)
    //endregion

    //region ADB
    val connectedDevices = ObservableViewModelProperty(arrayListOf<HistoryItem<String>>().toList())
    val deviceSelectedIndex = ObservableViewModelProperty<Int>()
    val currentDevice = ObservableViewModelProperty<String>()

    val pauseAll = ObservableViewModelProperty(false)

    val searchPanelVisible = ObservableViewModelProperty(false)
    //endregion

    //region Menu
    val rotation = ObservableViewModelProperty(getEnum<Rotation>(UIConfManager.uiConf.rotation))
    val logLevel = ObservableViewModelProperty(getLevelFromName(UIConfManager.uiConf.logLevel))
    //endregion

    //endregion

    //region SearchBar
    val searchHistory = ObservableViewModelProperty(UIConfManager.uiConf.searchHistory.toHistoryItemList())
    val searchSelectedIndex = ObservableViewModelProperty<Int>()
    val searchCurrentContent = ObservableViewModelProperty<String>()
    val searchMatchCase = ObservableViewModelProperty(UIConfManager.uiConf.searchMatchCaseEnabled)
    val searchErrorMessage = ObservableViewModelProperty<String>()
    //endregion

    //region LogPanel
    val splitPanelDividerLocation = ObservableViewModelProperty(UIConfManager.uiConf.dividerLocation)
    //endregion

    //region status bar
    val status = ObservableViewModelProperty("")
    val filePath = ObservableViewModelProperty(STRINGS.ui.none)
    //endregion

    //region Style
    val logFont = ObservableViewModelProperty(UIConfManager.uiConf.getLogFont())
    //endregion

    fun bindLogFilter(
        comboBox: HistoryComboBox<String>,
        toggle: JToggleButton,
        selectedIndexProperty: ObservableViewModelProperty<Int>,
        listProperty: ObservableViewModelProperty<List<HistoryItem<String>>>,
        enabledProperty: ObservableViewModelProperty<Boolean>,
        editorContentProperty: ObservableViewModelProperty<String>,
        errorMessageProperty: ObservableViewModelProperty<String>? = null,
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
        selectedIndexProperty: ObservableViewModelProperty<Int>,
        listProperty: ObservableViewModelProperty<List<HistoryItem<String>>>,
        editorContentProperty: ObservableViewModelProperty<String>,
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
            UIConfManager.uiConf.logFontSize = it?.size ?: 14
            UIConfManager.uiConf.logFontStyle = it?.style ?: 0
        }
    }

    override fun onThemeChange(theme: Theme) {
        logFont.value?.let {
            logFont.updateValue(it.newFont(theme, DEFAULT_LOG_FONT_SIZE))
        }
    }
}