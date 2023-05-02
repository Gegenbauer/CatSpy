package me.gegenbauer.catspy.viewmodel

import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.databinding.bind.*
import me.gegenbauer.catspy.databinding.property.support.*
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.button.ButtonDisplayMode
import me.gegenbauer.catspy.ui.combobox.FilterComboBox
import me.gegenbauer.catspy.ui.panel.Rotation
import me.gegenbauer.catspy.utils.editorComponent
import me.gegenbauer.catspy.utils.getEnum
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JToggleButton

object MainViewModel {
    private const val TAG = "MainViewModel"

    //region Toolbar
    //region Filter
    val logFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.logFilterEnabled)
    val logFilterHistory = ObservableViewModelProperty(UIConfManager.uiConf.logFilterHistory.toList())
    val logFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val logFilterCurrentContent = ObservableViewModelProperty<String>()

    val tagFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.tagFilterEnabled)
    val tagFilterHistory = ObservableViewModelProperty(UIConfManager.uiConf.tagFilterHistory.toList())
    val tagFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val tagFilterCurrentContent = ObservableViewModelProperty<String>()

    val pidFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.pidFilterEnabled)
    val pidFilterHistory = ObservableViewModelProperty(arrayListOf<String>().toList())
    val pidFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val pidFilterCurrentContent = ObservableViewModelProperty<String>()

    val tidFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.tidFilterEnabled)
    val tidFilterHistory = ObservableViewModelProperty(arrayListOf<String>().toList())
    val tidFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val tidFilterCurrentContent = ObservableViewModelProperty<String>()

    val boldEnabled = ObservableViewModelProperty(UIConfManager.uiConf.boldEnabled)
    val boldHistory = ObservableViewModelProperty(UIConfManager.uiConf.highlightHistory.toList())
    val boldSelectedIndex = ObservableViewModelProperty<Int>()
    val boldCurrentContent = ObservableViewModelProperty<String>()

    val filterMatchCaseEnabled = ObservableViewModelProperty(UIConfManager.uiConf.filterMatchCaseEnabled)
    //endregion

    //region ADB
    val connectedDevices = ObservableViewModelProperty(arrayListOf<String>().toList())
    val deviceSelectedIndex = ObservableViewModelProperty<Int>()
    val currentDevice = ObservableViewModelProperty<String>()

    val logCmdHistory = ObservableViewModelProperty(UIConfManager.uiConf.logCmdHistory.toList())
    val logCmdSelectedIndex = ObservableViewModelProperty<Int>()
    val logCmdCurrentContent = ObservableViewModelProperty<String>()

    val retryAdb = ObservableViewModelProperty(UIConfManager.uiConf.retryAdbEnabled)
    val adbProcessStopped = ObservableViewModelProperty(false)

    val splitFile = ObservableViewModelProperty(UIConfManager.uiConf.logScrollBackSplitFileEnabled)
    val spiltBatchCount = ObservableViewModelProperty(0)
    val searchPanelVisible = ObservableViewModelProperty(false)
    //endregion

    //region Menu
    val filterIncremental = ObservableViewModelProperty(UIConfManager.uiConf.filterIncrementalEnabled)
    val fullLog = ObservableViewModelProperty(UIConfManager.uiConf.logFullViewEnabled)
    val rotation = ObservableViewModelProperty(getEnum<Rotation>(UIConfManager.uiConf.rotation))
    val logLevel = ObservableViewModelProperty(UIConfManager.uiConf.logLevel)
    //endregion

    //endregion

    //region SearchBar
    val searchHistory = ObservableViewModelProperty(UIConfManager.uiConf.searchHistory.toList())
    val searchSelectedIndex = ObservableViewModelProperty<Int>()
    val searchCurrentContent = ObservableViewModelProperty<String>()
    //endregion

    //region LogPanel
    val splitPanelDividerLocation = ObservableViewModelProperty(UIConfManager.uiConf.dividerLocation)
    //endregion

    //region Style
    val buttonDisplayMode = ObservableViewModelProperty(ButtonDisplayMode.ALL) // TODO save configuration of this
    //endregion

    fun bind(mainUI: MainUI) {
        mainUI.apply {
            //region Toolbar
            //region Filter
            bindLogFilter(showLogCombo, showLogToggle, logFilterSelectedIndex, logFilterHistory, logFilterEnabled, logFilterCurrentContent)
            bindLogFilter(showTagCombo, showTagToggle, tagFilterSelectedIndex, tagFilterHistory, tagFilterEnabled, tagFilterCurrentContent)
            bindLogFilter(showPidCombo, showPidToggle, pidFilterSelectedIndex, pidFilterHistory, pidFilterEnabled, pidFilterCurrentContent)
            bindLogFilter(showTidCombo, showTidToggle, tidFilterSelectedIndex, tidFilterHistory, tidFilterEnabled, tidFilterCurrentContent)
            bindLogFilter(boldLogCombo, boldLogToggle, boldSelectedIndex, boldHistory, boldEnabled, boldCurrentContent)

            selectedProperty(matchCaseToggle) bindDual filterMatchCaseEnabled
            //endregion

            //region ADB
            bindNormalCombo(deviceCombo, deviceSelectedIndex, connectedDevices, currentDevice)
            bindNormalCombo(logCmdCombo, logCmdSelectedIndex, logCmdHistory, logCmdCurrentContent)

            selectedProperty(retryAdbToggle) bindDual retryAdb
            //endregion

            //region Menu
            selectedProperty(settingsMenu.itemDebug) bindDual GlobalViewModel.debug
            selectedProperty(settingsMenu.itemFilterIncremental) bindDual filterIncremental
            selectedProperty(viewMenu.itemFull) bindDual fullLog
            fullLog.addObserver {
                if (it != false) {
                    attachLogPanel(splitLogPane.fullLogPanel)
                } else {
                    detachLogPanel(splitLogPane.fullLogPanel)
                }
            }
            customProperty(splitLogPane, "rotation", Rotation.ROTATION_LEFT_RIGHT) bindDual rotation
            //endregion

            //endregion

            //region SearchBar
            listProperty(searchPanel.searchCombo) bindDual searchHistory
            selectedIndexProperty(searchPanel.searchCombo) bindLeft searchSelectedIndex
            textProperty(searchPanel.searchCombo.editorComponent) bindDual searchCurrentContent

            visibilityProperty(searchPanel) bindDual searchPanelVisible
            selectedProperty(viewMenu.itemSearch) bindDual searchPanelVisible

            reorderAfterByLRU(searchSelectedIndex, searchHistory)
            //endregion

            //region LogPanel
            dividerProperty(splitLogPane) bindDual splitPanelDividerLocation
            //endregion

            //region Style
            bindWithButtonDisplayMode(
                startBtn, stopBtn, pauseToggle, saveBtn, clearViewsBtn, adbConnectBtn, adbRefreshBtn, adbDisconnectBtn,
                scrollBackApplyBtn, retryAdbToggle, retryAdbToggle, scrollBackSplitFileToggle, scrollBackKeepToggle,
                scrollBackLabel
            )
            //endregion
        }
    }

    private fun bindLogFilter(
        comboBox: FilterComboBox,
        toggle: JToggleButton,
        selectedIndexProperty: ObservableViewModelProperty<Int>,
        listProperty: ObservableViewModelProperty<List<String>>,
        enabledProperty: ObservableViewModelProperty<Boolean>,
        editorContentProperty: ObservableViewModelProperty<String>,
    ) {
        selectedProperty(toggle) bindDual enabledProperty
        enabledProperty(comboBox) bindDual enabledProperty
        visibilityProperty(comboBox) bindDual enabledProperty
        listProperty(comboBox) bindDual listProperty
        selectedIndexProperty(comboBox) bindLeft selectedIndexProperty
        textProperty(comboBox.editorComponent) bindDual editorContentProperty
        reorderAfterByLRU(selectedIndexProperty, listProperty)
    }

    private fun bindNormalCombo(
        comboBox: JComboBox<String>,
        selectedIndexProperty: ObservableViewModelProperty<Int>,
        listProperty: ObservableViewModelProperty<List<String>>,
        editorContentProperty: ObservableViewModelProperty<String>,
    ) {
        listProperty(comboBox) bindDual listProperty
        selectedIndexProperty(comboBox) bindLeft selectedIndexProperty
        textProperty(comboBox.editorComponent) bindDual editorContentProperty
        reorderAfterByLRU(selectedIndexProperty, listProperty)
    }

    /**
     * 按下 Enter 进行搜索后，将该项提至最前
     */
    private fun reorderAfterByLRU(
        selectedIndexProperty: ObservableViewModelProperty<Int>,
        listProperty: ObservableViewModelProperty<List<String>>
    ) {
        selectedIndexProperty.addObserver { selectedIndex ->
            selectedIndex ?: return@addObserver
            if (selectedIndex < 0) {
                return@addObserver
            }
            listProperty.value?.let {
                listProperty.updateValue(it.updateListByLRU(it[selectedIndex]))
            }
        }
    }

    private fun bindWithButtonDisplayMode(vararg component: JComponent) {
        component.forEach { customProperty(it, "buttonDisplayMode", ButtonDisplayMode.ALL) bindRight buttonDisplayMode }
    }
}