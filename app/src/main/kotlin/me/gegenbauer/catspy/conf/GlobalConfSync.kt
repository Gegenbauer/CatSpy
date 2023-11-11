package me.gegenbauer.catspy.conf

import me.gegenbauer.catspy.cache.CacheLog
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.databinding.BindingLog
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.ddmlib.DdmLog
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.task.TaskLog

object GlobalConfSync {
    val globalDebug = ObservableValueProperty(UIConfManager.uiConf.globalDebug)
    val dataBindingDebug = ObservableValueProperty(UIConfManager.uiConf.dataBindingDebug)
    val taskDebug = ObservableValueProperty(UIConfManager.uiConf.taskDebug)
    val ddmDebug = ObservableValueProperty(UIConfManager.uiConf.ddmDebug)
    val cacheDebug = ObservableValueProperty(UIConfManager.uiConf.cacheDebug)
    val logDebug = ObservableValueProperty(UIConfManager.uiConf.logDebug)
    val versionCode = ObservableValueProperty(UIConfManager.uiConf.versionCode)
    val appHome = ObservableValueProperty(UIConfManager.uiConf.appHome)

    fun init() {
        // do nothing
    }

    // TODO configuration 模块存在循环依赖
    init {
        globalDebug.addObserver {
            UIConfManager.uiConf.globalDebug = it ?: false
            GLog.debug = it ?: false
        }
        dataBindingDebug.addObserver {
            UIConfManager.uiConf.dataBindingDebug = it ?: false
            BindingLog.debug = it ?: false
        }
        taskDebug.addObserver {
            UIConfManager.uiConf.taskDebug = it ?: false
            TaskLog.debug = it ?: false
        }
        ddmDebug.addObserver {
            UIConfManager.uiConf.ddmDebug = it ?: false
            DdmLog.debug = it ?: false
        }
        cacheDebug.addObserver {
            UIConfManager.uiConf.cacheDebug = it ?: false
            CacheLog.debug = it ?: false
        }
        logDebug.addObserver {
            UIConfManager.uiConf.logDebug = it ?: false
            Log.debug = it ?: false
        }
        versionCode.addObserver { UIConfManager.uiConf.versionCode = it ?: 0 }
        appHome.addObserver { UIConfManager.uiConf.appHome = it ?: "" }
    }
}