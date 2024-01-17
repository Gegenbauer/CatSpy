package me.gegenbauer.catspy.conf

import me.gegenbauer.catspy.cache.CacheLog
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.databinding.BindingLog
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.ddmlib.DdmLog
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.task.TaskLog

object GlobalConfSync {
    val globalDebug = ObservableValueProperty(currentSettings.debugSettings.globalDebug)
    val dataBindingDebug = ObservableValueProperty(currentSettings.debugSettings.dataBindingDebug)
    val taskDebug = ObservableValueProperty(currentSettings.debugSettings.taskDebug)
    val ddmDebug = ObservableValueProperty(currentSettings.debugSettings.ddmDebug)
    val cacheDebug = ObservableValueProperty(currentSettings.debugSettings.cacheDebug)
    val logDebug = ObservableValueProperty(currentSettings.debugSettings.logDebug)

    fun init() {
        // do nothing
    }

    // TODO configuration 模块存在循环依赖
    init {
        globalDebug.addObserver { value ->
            SettingsManager.updateSettings { debugSettings.globalDebug = value ?: false }
            GLog.debug = value ?: false
        }
        dataBindingDebug.addObserver { value ->
            SettingsManager.updateSettings { debugSettings.dataBindingDebug = value ?: false }
            BindingLog.debug = value ?: false
        }
        taskDebug.addObserver { value ->
            SettingsManager.updateSettings { debugSettings.taskDebug = value ?: false }
            TaskLog.debug = value ?: false
        }
        ddmDebug.addObserver { value ->
            SettingsManager.updateSettings { debugSettings.ddmDebug = value ?: false }
            DdmLog.debug = value ?: false
        }
        cacheDebug.addObserver { value ->
            SettingsManager.updateSettings { debugSettings.cacheDebug = value ?: false  }
            CacheLog.debug = value ?: false
        }
        logDebug.addObserver { value ->
            SettingsManager.updateSettings { debugSettings.logDebug = value ?: false }
            Log.debug = value ?: false
        }
    }
}