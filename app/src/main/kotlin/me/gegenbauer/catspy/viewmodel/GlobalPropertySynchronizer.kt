package me.gegenbauer.catspy.viewmodel

import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.databinding.bind.ObservableProperty

object GlobalPropertySynchronizer {
    val debug = ObservableProperty(UIConfManager.uiConf.debug)
    val versionCode = ObservableProperty(UIConfManager.uiConf.versionCode)
    val appHome = ObservableProperty(UIConfManager.uiConf.appHome)

    fun init() {
        // do nothing
    }

    init {
        debug.addObserver { UIConfManager.uiConf.debug = it ?: false }
        versionCode.addObserver { UIConfManager.uiConf.versionCode = it ?: 0 }
        appHome.addObserver { UIConfManager.uiConf.appHome = it ?: "" }

        MainViewModel.searchHistory.addObserver {
            UIConfManager.uiConf.searchHistory.clear()
            UIConfManager.uiConf.searchHistory.addAll(it!!)
        }
        MainViewModel.logFilterHistory.addObserver {
            UIConfManager.uiConf.logFilterHistory.clear()
            UIConfManager.uiConf.logFilterHistory.addAll(it!!)
        }

        MainViewModel.tagFilterHistory.addObserver {
            UIConfManager.uiConf.tagFilterHistory.clear()
            UIConfManager.uiConf.tagFilterHistory.addAll(it!!)
        }
        MainViewModel.highlightHistory.addObserver {
            UIConfManager.uiConf.highlightHistory.clear()
            UIConfManager.uiConf.highlightHistory.addAll(it!!)
        }
    }
}