package me.gegenbauer.catspy.conf

import me.gegenbauer.catspy.cache.CacheLog
import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.configuration.WriteSyncValueProperty
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.databinding.BindingLog
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.ddmlib.log.DdmLog
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.filter.StorableValueProperty
import me.gegenbauer.catspy.task.TaskLog

object MainConfSync {
    val globalDebug: ObservableValueProperty<Boolean> = WriteSyncValueProperty(
        listOf(
            currentSettings.debugSettings::globalDebug,
            GLog::debug
        ),
        currentSettings.debugSettings.globalDebug
    )
    val dataBindingDebug: ObservableValueProperty<Boolean> = WriteSyncValueProperty(
        listOf(
            currentSettings.debugSettings::dataBindingDebug,
            BindingLog::debug
        ),
        currentSettings.debugSettings.dataBindingDebug
    )
    val taskDebug: ObservableValueProperty<Boolean> = WriteSyncValueProperty(
        listOf(
            currentSettings.debugSettings::taskDebug,
            TaskLog::debug
        ),
        currentSettings.debugSettings.taskDebug
    )
    val ddmDebug: ObservableValueProperty<Boolean> = WriteSyncValueProperty(
        listOf(
            currentSettings.debugSettings::ddmDebug,
            DdmLog::debug
        ),
        currentSettings.debugSettings.ddmDebug
    )
    val cacheDebug: ObservableValueProperty<Boolean> = WriteSyncValueProperty(
        listOf(
            currentSettings.debugSettings::cacheDebug,
            CacheLog::debug
        ),
        currentSettings.debugSettings.cacheDebug
    )
    val logDebug: ObservableValueProperty<Boolean> = WriteSyncValueProperty(
        listOf(
            currentSettings.debugSettings::logDebug,
            Log::debug
        ),
        currentSettings.debugSettings.logDebug
    )

    val showStatusBar: ObservableValueProperty<Boolean> = StorableValueProperty(
        GlobalStrings.FUNC_SHOW_STATUS_BAR,
        true
    )
}