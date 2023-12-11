package me.gegenbauer.catspy.conf

import me.gegenbauer.catspy.cache.CacheLog
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.databinding.BindingLog
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.ddmlib.DdmLog
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.task.TaskLog

object GlobalConfSync {
    val globalDebug = ObservableValueProperty(SettingsManager.settings.globalDebug)
    val dataBindingDebug = ObservableValueProperty(SettingsManager.settings.dataBindingDebug)
    val taskDebug = ObservableValueProperty(SettingsManager.settings.taskDebug)
    val ddmDebug = ObservableValueProperty(SettingsManager.settings.ddmDebug)
    val cacheDebug = ObservableValueProperty(SettingsManager.settings.cacheDebug)
    val logDebug = ObservableValueProperty(SettingsManager.settings.logDebug)

    fun init() {
        // do nothing
    }

    // TODO configuration 模块存在循环依赖
    init {
        globalDebug.addObserver { value ->
            SettingsManager.updateSettings { globalDebug = value ?: false }
            GLog.debug = value ?: false
        }
        dataBindingDebug.addObserver { value ->
            SettingsManager.updateSettings { dataBindingDebug = value ?: false }
            BindingLog.debug = value ?: false
        }
        taskDebug.addObserver { value ->
            SettingsManager.updateSettings { taskDebug = value ?: false }
            TaskLog.debug = value ?: false
        }
        ddmDebug.addObserver { value ->
            SettingsManager.updateSettings { ddmDebug = value ?: false }
            DdmLog.debug = value ?: false
        }
        cacheDebug.addObserver { value ->
            SettingsManager.updateSettings { cacheDebug = value ?: false  }
            CacheLog.debug = value ?: false
        }
        logDebug.addObserver { value ->
            SettingsManager.updateSettings { logDebug = value ?: false }
            Log.debug = value ?: false
        }
    }
}