package me.gegenbauer.logviewer.viewmodel

import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.databinding.Bindings
import me.gegenbauer.logviewer.databinding.ObservableViewModelProperty
import me.gegenbauer.logviewer.databinding.adapter.enableProperty
import me.gegenbauer.logviewer.databinding.adapter.selectedProperty
import me.gegenbauer.logviewer.ui.MainUI

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

    fun bind(mainUI: MainUI) {
        Bindings.bind(selectedProperty(mainUI.showLogToggle), logFilterEnabled)
        Bindings.bind(enableProperty(mainUI.showLogCombo), logFilterEnabled)

        Bindings.bind(selectedProperty(mainUI.showTagToggle), tagFilterEnabled)
        Bindings.bind(enableProperty(mainUI.showTagCombo), tagFilterEnabled)

        Bindings.bind(selectedProperty(mainUI.showPidToggle), pidFilterEnabled)
        Bindings.bind(enableProperty(mainUI.showPidCombo), pidFilterEnabled)

        Bindings.bind(selectedProperty(mainUI.showTagToggle), tidFilterEnabled)
        Bindings.bind(enableProperty(mainUI.showTagCombo), tidFilterEnabled)

        Bindings.bind(selectedProperty(mainUI.boldLogToggle), highlightEnabled)
        Bindings.bind(enableProperty(mainUI.highlightLogCombo), highlightEnabled)
        Bindings.bind(selectedProperty(mainUI.matchCaseToggle), filterMatchCaseEnabled)
    }
}