package me.gegenbauer.catspy.viewmodel

import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.ui.log.LogLevel
import me.gegenbauer.catspy.ui.panel.Rotation

object GlobalViewModel {
    val debug = ObservableViewModelProperty(UIConfManager.uiConf.debug)
    val versionCode = ObservableViewModelProperty(UIConfManager.uiConf.versionCode)
    val appHome = ObservableViewModelProperty(UIConfManager.uiConf.appHome)

    val logLevel = ObservableViewModelProperty(UIConfManager.uiConf.logLevel)

    fun init() {
        // do nothing
    }

    init {
        debug.addObserver {
            UIConfManager.uiConf.debug = it ?: false
            GLog.debug = it ?: false
        }
        versionCode.addObserver { UIConfManager.uiConf.versionCode = it ?: 0 }
        appHome.addObserver { UIConfManager.uiConf.appHome = it ?: "" }

        syncGlobalConfWithMainViewModel()
    }

    private fun syncGlobalConfWithMainViewModel() {
        MainViewModel.apply {
            searchHistory.addObserver {
                UIConfManager.uiConf.searchHistory.clear()
                UIConfManager.uiConf.searchHistory.addAll(it!!)
            }
            logFilterHistory.addObserver {
                UIConfManager.uiConf.logFilterHistory.clear()
                UIConfManager.uiConf.logFilterHistory.addAll(it!!)
            }
            tagFilterHistory.addObserver {
                UIConfManager.uiConf.tagFilterHistory.clear()
                UIConfManager.uiConf.tagFilterHistory.addAll(it!!)
            }
            boldHistory.addObserver {
                UIConfManager.uiConf.highlightHistory.clear()
                UIConfManager.uiConf.highlightHistory.addAll(it!!)
            }
            logCmdHistory.addObserver {
                UIConfManager.uiConf.logCmdHistory.clear()
                UIConfManager.uiConf.logCmdHistory.addAll(it!!)
            }
            fullLog.addObserver {
                UIConfManager.uiConf.logFullViewEnabled = it ?: true
            }
            rotation.addObserver {
                UIConfManager.uiConf.rotation = it?.ordinal ?: Rotation.ROTATION_LEFT_RIGHT.ordinal
            }
            retryAdb.addObserver {
                UIConfManager.uiConf.retryAdbEnabled = it ?: false
            }
            logLevel.addObserver {
                UIConfManager.uiConf.logLevel = it ?: LogLevel.WARN.logName
            }
            splitPanelDividerLocation.addObserver {
                UIConfManager.uiConf.dividerLocation = it ?: 500
            }
        }
    }
}