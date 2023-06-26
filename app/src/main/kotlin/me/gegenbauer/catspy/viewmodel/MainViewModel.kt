package me.gegenbauer.catspy.viewmodel

import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.data.model.log.LogLevel
import me.gegenbauer.catspy.data.model.log.getLevelFromName
import me.gegenbauer.catspy.data.model.log.nameToLogLevel
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.bind.bindLeft
import me.gegenbauer.catspy.databinding.bind.bindRight
import me.gegenbauer.catspy.databinding.property.support.*
import me.gegenbauer.catspy.ui.button.ButtonDisplayMode
import me.gegenbauer.catspy.ui.combobox.HistoryComboBox
import me.gegenbauer.catspy.ui.combobox.HistoryItem
import me.gegenbauer.catspy.ui.combobox.toHistoryItemList
import me.gegenbauer.catspy.ui.log.LogMainUI
import me.gegenbauer.catspy.ui.panel.Rotation
import me.gegenbauer.catspy.utils.editorComponent
import me.gegenbauer.catspy.utils.getEnum
import javax.swing.JComponent
import javax.swing.JToggleButton

// TODO 改为类
object MainViewModel {
    private const val TAG = "MainViewModel"

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
    val logLevelFilterSelectedIndex = ObservableViewModelProperty(sortedLogLevels.indexOf(UIConfManager.uiConf.logLevel))

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

    //region Style
    val buttonDisplayMode = ObservableViewModelProperty(ButtonDisplayMode.ALL) // TODO save configuration of this
    val logFont = ObservableViewModelProperty(UIConfManager.uiConf.getLogFont())
    //endregion

    fun bind(mainUI: LogMainUI) {
        mainUI.apply {
            //region Toolbar
            //region Filter
            bindLogFilter(showLogCombo, showLogToggle, logFilterSelectedIndex, logFilterHistory, logFilterEnabled, logFilterCurrentContent, logFilterErrorMessage)
            bindLogFilter(showTagCombo, showTagToggle, tagFilterSelectedIndex, tagFilterHistory, tagFilterEnabled, tagFilterCurrentContent, tagFilterErrorMessage)
            bindLogFilter(showPidCombo, showPidToggle, pidFilterSelectedIndex, pidFilterHistory, pidFilterEnabled, pidFilterCurrentContent, pidFilterErrorMessage)
            bindLogFilter(showTidCombo, showTidToggle, tidFilterSelectedIndex, tidFilterHistory, tidFilterEnabled, tidFilterCurrentContent, tidFilterErrorMessage)
            bindLogFilter(logLevelCombo, logLevelToggle, logLevelFilterSelectedIndex, logLevelFilterHistory, logLevelFilterEnabled, logLevelFilterCurrentContent)
            bindLogFilter(boldLogCombo, boldLogToggle, boldSelectedIndex, boldHistory, boldEnabled, boldCurrentContent, boldErrorMessage)

            selectedProperty(matchCaseToggle) bindDual filterMatchCaseEnabled
            //endregion

            //region ADB
            bindNormalCombo(deviceCombo, deviceSelectedIndex, connectedDevices, currentDevice)

            selectedProperty(pauseToggle) bindDual pauseAll
            //endregion

            //region Menu
            customProperty(splitLogPane, "rotation", Rotation.ROTATION_LEFT_RIGHT) bindDual rotation
            //endregion

            //endregion

            //region SearchBar
            listProperty(searchPanel.searchCombo) bindDual searchHistory
            selectedIndexProperty(searchPanel.searchCombo) bindLeft searchSelectedIndex
            textProperty(searchPanel.searchCombo.editorComponent) bindDual searchCurrentContent
            customProperty(searchPanel.searchCombo, "errorMsg", "") bindDual searchErrorMessage

            visibilityProperty(searchPanel) bindDual searchPanelVisible

            selectedProperty(searchPanel.searchMatchCaseToggle) bindDual searchMatchCase
            //endregion

            //region LogPanel
            dividerProperty(splitLogPane) bindDual splitPanelDividerLocation
            //endregion

            //region Style
            bindWithButtonDisplayMode(
                startBtn, stopBtn, pauseToggle, saveBtn, clearViewsBtn, adbConnectBtn, adbRefreshBtn, adbDisconnectBtn
            )
            //endregion

            logLevelFilterCurrentContent.addObserver {
                logLevel.updateValue(nameToLogLevel[it] ?: LogLevel.VERBOSE)
            }
        }
    }

    private fun bindLogFilter(
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

    private fun bindNormalCombo(
        comboBox: HistoryComboBox<String>,
        selectedIndexProperty: ObservableViewModelProperty<Int>,
        listProperty: ObservableViewModelProperty<List<HistoryItem<String>>>,
        editorContentProperty: ObservableViewModelProperty<String>,
    ) {
        listProperty(comboBox) bindDual listProperty
        selectedIndexProperty(comboBox) bindLeft selectedIndexProperty
        textProperty(comboBox.editorComponent) bindDual editorContentProperty
    }

    private fun bindWithButtonDisplayMode(vararg component: JComponent) {
        component.forEach { customProperty(it, "buttonDisplayMode", ButtonDisplayMode.ALL) bindRight buttonDisplayMode }
    }
}