package me.gegenbauer.logviewer.viewmodel

import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.databinding.Bindings
import me.gegenbauer.logviewer.databinding.ObservableViewModelProperty
import me.gegenbauer.logviewer.databinding.adapter.customProperty
import me.gegenbauer.logviewer.databinding.adapter.enableProperty
import me.gegenbauer.logviewer.databinding.adapter.selectedProperty
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.button.ButtonDisplayMode

class MainViewModel {
    val logFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.logFilterEnabled)
    val logFilterHistory = ObservableViewModelProperty(UIConfManager.uiConf.logFilterHistory)
    val logFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val logFilterCurrentContent = ObservableViewModelProperty<String>()

    val tagFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.tagFilterEnabled)
    val tagFilterHistory = ObservableViewModelProperty(UIConfManager.uiConf.tagFilterHistory)
    val tagFilterSelectedIndex = ObservableViewModelProperty<Int>()
    val tagFilterCurrentContent = ObservableViewModelProperty<String>()

    val pidFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.pidFilterEnabled)
    val pidFilterCurrentContent = ObservableViewModelProperty<String>()

    val tidFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.tidFilterEnabled)
    val tidFilterCurrentContent = ObservableViewModelProperty<String>()

    val highlightEnabled = ObservableViewModelProperty(UIConfManager.uiConf.highlightEnabled)
    val highlightHistory = ObservableViewModelProperty(UIConfManager.uiConf.highlightHistory)
    val highlightSelectedIndex = ObservableViewModelProperty<Int>()
    val highlightCurrentContent = ObservableViewModelProperty<String>()

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
        Bindings.bind(selectedProperty(mainUI.showLogToggle), logFilterEnabled)
        Bindings.bind(enableProperty(mainUI.showLogCombo), logFilterEnabled)

        Bindings.bind(selectedProperty(mainUI.showTagToggle), tagFilterEnabled)
        Bindings.bind(enableProperty(mainUI.showTagCombo), tagFilterEnabled)

        Bindings.bind(selectedProperty(mainUI.showPidToggle), pidFilterEnabled)
        Bindings.bind(enableProperty(mainUI.showPidCombo), pidFilterEnabled)

        Bindings.bind(selectedProperty(mainUI.showTidToggle), tidFilterEnabled)
        Bindings.bind(enableProperty(mainUI.showTidCombo), tidFilterEnabled)

        Bindings.bind(selectedProperty(mainUI.boldLogToggle), highlightEnabled)
        Bindings.bind(enableProperty(mainUI.highlightLogCombo), highlightEnabled)
        Bindings.bind(selectedProperty(mainUI.matchCaseToggle), filterMatchCaseEnabled)

        Bindings.bind(customProperty(mainUI.startBtn, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.stopBtn, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.saveBtn, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.clearViewsBtn, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.adbConnectBtn, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.adbRefreshBtn, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.adbDisconnectBtn, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.scrollBackApplyBtn, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)

        Bindings.bind(customProperty(mainUI.retryAdbToggle, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.pauseToggle, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.scrollBackSplitFileToggle, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)
        Bindings.bind(customProperty(mainUI.scrollBackKeepToggle, "buttonDisplayMode", ButtonDisplayMode.ALL), buttonDisplayMode)

        Bindings.bind(selectedProperty(mainUI.retryAdbToggle), retryAdb)


        logFilterEnabled.addObserver {
            mainUI.showLogCombo.setEnabledFilter(it ?: false)
        }
        tagFilterEnabled.addObserver {
            mainUI.showTagCombo.setEnabledFilter(it ?: false)
        }
        pidFilterEnabled.addObserver {
            mainUI.showPidCombo.setEnabledFilter(it ?: false)
        }
        tidFilterEnabled.addObserver {
            mainUI.showTidCombo.setEnabledFilter(it ?: false)
        }
        highlightEnabled.addObserver {
            mainUI.highlightLogCombo.setEnabledFilter(it ?: false)
        }
    }
}