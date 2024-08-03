package me.gegenbauer.catspy.conf

import com.formdev.flatlaf.extras.FlatInspector
import me.gegenbauer.catspy.platform.isInDebugMode

object DebugConfiguration {

    @JvmStatic
    fun apply() {
        if (isInDebugMode) {
            FlatInspector.install("ctrl shift alt X")
        }
    }
}
