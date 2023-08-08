package me.gegenbauer.catspy.common.viewmodel

import me.gegenbauer.catspy.common.configuration.UIConfManager
import me.gegenbauer.catspy.databinding.BindingLog
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.ddmlib.DdmLog
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.task.TaskLog

object GlobalViewModel {
    val globalDebug = ObservableViewModelProperty(UIConfManager.uiConf.globalDebug)
    val dataBindingDebug = ObservableViewModelProperty(UIConfManager.uiConf.dataBindingDebug)
    val taskDebug = ObservableViewModelProperty(UIConfManager.uiConf.taskDebug)
    val ddmDebug = ObservableViewModelProperty(UIConfManager.uiConf.ddmDebug)
    val versionCode = ObservableViewModelProperty(UIConfManager.uiConf.versionCode)
    val appHome = ObservableViewModelProperty(UIConfManager.uiConf.appHome)

    fun init() {
        // do nothing
    }

    init {
        globalDebug.addObserver {
            UIConfManager.uiConf.globalDebug = it ?: false
            GLog.debug = it ?: false
        }
        dataBindingDebug.addObserver {
            UIConfManager.uiConf.dataBindingDebug = it ?: false
            BindingLog.bindingDebug = it ?: false
        }
        taskDebug.addObserver {
            UIConfManager.uiConf.taskDebug = it ?: false
            TaskLog.taskDebug = it ?: false
        }
        ddmDebug.addObserver {
            UIConfManager.uiConf.ddmDebug = it ?: false
            DdmLog.ddmDebug = it ?: false
        }
        versionCode.addObserver { UIConfManager.uiConf.versionCode = it ?: 0 }
        appHome.addObserver { UIConfManager.uiConf.appHome = it ?: "" }
    }
}