package me.gegenbauer.logviewer.viewmodel

import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.databinding.ObservableViewModelProperty

class MainViewModel {
    val logFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.logFilterEnabled)
    val tagFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.tagFilterEnabled)
    val pidFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.pidFilterEnabled)
    val tidFilterEnabled = ObservableViewModelProperty(UIConfManager.uiConf.tidFilterEnabled)
    val highlightEnabled = ObservableViewModelProperty(UIConfManager.uiConf.highlightEnabled)
    val filterMatchCaseEnabled = ObservableViewModelProperty(UIConfManager.uiConf.filterMatchCaseEnabled)
}