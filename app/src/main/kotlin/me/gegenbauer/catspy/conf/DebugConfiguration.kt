package me.gegenbauer.catspy.conf

import com.formdev.flatlaf.extras.FlatInspector
import me.gegenbauer.catspy.configuration.SettingsManager.settings

object DebugConfiguration {

    @JvmStatic
    fun apply() {
        if (settings.debugSettings.globalDebug) {
            FlatInspector.install("ctrl shift alt X")
        }
    }
}