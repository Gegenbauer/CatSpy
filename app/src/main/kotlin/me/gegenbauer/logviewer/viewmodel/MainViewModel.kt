package me.gegenbauer.logviewer.viewmodel

import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.databinding.bind.*
import me.gegenbauer.logviewer.databinding.property.support.*
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.button.ButtonDisplayMode
import me.gegenbauer.logviewer.utils.editorComponent
import javax.swing.JComponent

object MainViewModel {
    private const val TAG = "MainViewModel"

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

    val highlightEnabled = ObservableViewModelProperty(UIConfManager.uiConf.highlightEnabled)
    val highlightHistory = ObservableViewModelProperty(UIConfManager.uiConf.highlightHistory.toList())
    val highlightSelectedIndex = ObservableViewModelProperty<Int>()
    val highlightCurrentContent = ObservableViewModelProperty<String>()

    val searchHistory = ObservableViewModelProperty(UIConfManager.uiConf.searchHistory.toList())
    val searchSelectedIndex = ObservableViewModelProperty<Int>()
    val searchCurrentContent = ObservableViewModelProperty<String>()

    val connectedDevices = ObservableViewModelProperty(arrayListOf<String>().toList())
    val deviceSelectedIndex = ObservableViewModelProperty<Int>()
    val currentDevice = ObservableViewModelProperty<String>()

    val filterMatchCaseEnabled = ObservableViewModelProperty(UIConfManager.uiConf.filterMatchCaseEnabled)

    val searchPanelVisible = ObservableViewModelProperty(false)
    val fullLog = ObservableViewModelProperty(UIConfManager.uiConf.logFullViewEnabled)
    val appLogLevel = ObservableViewModelProperty(UIConfManager.uiConf.logLevel)
    val filterIncremental = ObservableViewModelProperty(UIConfManager.uiConf.filterIncrementalEnabled)
    val debug = ObservableViewModelProperty(UIConfManager.uiConf.debug)

    val adbProcessStopped = ObservableViewModelProperty(false)
    val splitFile = ObservableViewModelProperty(UIConfManager.uiConf.logScrollBackSplitFileEnabled)
    val spiltBatchCount = ObservableViewModelProperty(0)

    val retryAdb = ObservableViewModelProperty(UIConfManager.uiConf.retryAdbEnabled)
    val buttonDisplayMode = ObservableViewModelProperty(ButtonDisplayMode.ALL) // TODO save configuration of this

    fun bind(mainUI: MainUI) {
        mainUI.apply {
            selectedProperty(showLogToggle) bindDual logFilterEnabled
            enabledProperty(showLogCombo) bindDual logFilterEnabled
            visibilityProperty(showLogCombo) bindDual logFilterEnabled
            listProperty(showLogCombo) bindDual logFilterHistory
            selectedIndexProperty(showLogCombo) bindLeft logFilterSelectedIndex
            textProperty(showLogCombo.editorComponent) bindDual logFilterCurrentContent

            selectedProperty(showTagToggle) bindDual tagFilterEnabled
            enabledProperty(showTagCombo) bindDual tagFilterEnabled
            visibilityProperty(showTagCombo) bindDual tagFilterEnabled
            listProperty(showTagCombo) bindDual tagFilterHistory
            selectedIndexProperty(showTagCombo) bindDual tagFilterSelectedIndex
            textProperty(showTagCombo.editorComponent) bindDual tagFilterCurrentContent

            selectedProperty(showPidToggle) bindDual pidFilterEnabled
            enabledProperty(showPidCombo) bindDual pidFilterEnabled
            visibilityProperty(showPidCombo) bindDual pidFilterEnabled
            listProperty(showPidCombo) bindDual pidFilterHistory
            selectedIndexProperty(showPidCombo) bindLeft pidFilterSelectedIndex
            textProperty(showPidCombo.editorComponent) bindDual pidFilterCurrentContent

            selectedProperty(showTidToggle) bindDual tidFilterEnabled
            enabledProperty(showTidCombo) bindDual tidFilterEnabled
            visibilityProperty(showTidCombo) bindDual tidFilterEnabled
            listProperty(showTidCombo) bindDual tidFilterHistory
            selectedIndexProperty(showTidCombo) bindLeft tidFilterSelectedIndex
            textProperty(showTidCombo.editorComponent) bindDual tidFilterCurrentContent

            selectedProperty(boldLogToggle) bindDual highlightEnabled
            enabledProperty(highlightLogCombo) bindDual highlightEnabled
            visibilityProperty(highlightLogCombo) bindDual highlightEnabled
            listProperty(highlightLogCombo) bindDual highlightHistory
            selectedIndexProperty(highlightLogCombo) bindLeft highlightSelectedIndex
            textProperty(highlightLogCombo.editorComponent) bindDual highlightCurrentContent

            listProperty(searchPanel.searchCombo) bindDual searchHistory
            selectedIndexProperty(searchPanel.searchCombo) bindLeft searchSelectedIndex
            textProperty(searchPanel.searchCombo.editorComponent) bindDual searchCurrentContent

            listProperty(deviceCombo) bindDual connectedDevices
            selectedIndexProperty(deviceCombo) bindLeft deviceSelectedIndex
            textProperty(deviceCombo.editorComponent) bindDual currentDevice

            selectedProperty(matchCaseToggle) bindDual filterMatchCaseEnabled

            selectedProperty(retryAdbToggle) bindDual retryAdb

            bindWithButtonDisplayMode(
                startBtn, stopBtn, pauseToggle, saveBtn, clearViewsBtn, adbConnectBtn, adbRefreshBtn, adbDisconnectBtn,
                scrollBackApplyBtn, retryAdbToggle, retryAdbToggle, scrollBackSplitFileToggle, scrollBackKeepToggle,
                scrollBackLabel
            )

            filteredTableModel.filterLog

            // 更改为按下 Enter 进行搜索后，将该项提至最前
            logFilterSelectedIndex.addObserver {selectedIndex ->
                selectedIndex ?: return@addObserver
                if (selectedIndex < 0) {
                    return@addObserver
                }
                logFilterHistory.value?.let {
                    logFilterHistory.updateValue(it.updateListByLRU(it[selectedIndex]))
                }
            }

            tagFilterSelectedIndex.addObserver {selectedIndex ->
                selectedIndex ?: return@addObserver
                if (selectedIndex < 0) {
                    return@addObserver
                }
                tagFilterHistory.value?.let {
                    tagFilterHistory.updateValue(it.updateListByLRU(it[selectedIndex]))
                }
            }

            tidFilterSelectedIndex.addObserver {selectedIndex ->
                selectedIndex ?: return@addObserver
                if (selectedIndex < 0) {
                    return@addObserver
                }
                tidFilterHistory.value?.let {
                    tidFilterHistory.updateValue(it.updateListByLRU(it[selectedIndex]))
                }
            }

            pidFilterSelectedIndex.addObserver {selectedIndex ->
                selectedIndex ?: return@addObserver
                if (selectedIndex < 0) {
                    return@addObserver
                }
                pidFilterHistory.value?.let {
                    pidFilterHistory.updateValue(it.updateListByLRU(it[selectedIndex]))
                }
            }

            highlightSelectedIndex.addObserver {selectedIndex ->
                selectedIndex ?: return@addObserver
                if (selectedIndex < 0) {
                    return@addObserver
                }
                highlightHistory.value?.let {
                    highlightHistory.updateValue(it.updateListByLRU(it[selectedIndex]))
                }
            }

            searchSelectedIndex.addObserver {selectedIndex ->
                selectedIndex ?: return@addObserver
                if (selectedIndex < 0) {
                    return@addObserver
                }
                searchHistory.value?.let {
                    searchHistory.updateValue(it.updateListByLRU(it[selectedIndex]))
                }
            }
        }
    }

    private fun bindWithButtonDisplayMode(vararg component: JComponent) {
        component.forEach { customProperty(it, "buttonDisplayMode", ButtonDisplayMode.ALL) bindRight buttonDisplayMode }
    }
}