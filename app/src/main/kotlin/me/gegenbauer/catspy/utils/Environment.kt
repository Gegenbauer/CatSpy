package me.gegenbauer.catspy.utils

import me.gegenbauer.catspy.log.GLog
import java.lang.management.ManagementFactory
import javax.swing.TransferHandler.TransferSupport

val userDir: String = System.getProperty("user.dir")

interface IPlatform {
    val adbCommand: String
        get() = "adb"

    fun getFileDropAction(transferSupport: TransferSupport): Int {
        return transferSupport.sourceDropActions
    }

}

fun isInDebugMode(): Boolean {
    val inputArguments = ManagementFactory.getRuntimeMXBean().inputArguments
    for (arg in inputArguments) {
        if (arg.contains("-agentlib:jdwp")) {
            return true
        }
    }
    return false
}

enum class Platform : IPlatform {
    WINDOWS {
        override val adbCommand: String
            get() = "adb.exe"

        override fun getFileDropAction(transferSupport: TransferSupport): Int {
            return transferSupport.dropAction
        }
    },
    LINUX {
        // default implementation
    },
    MAC {
        // default implementation
    },
    UNKNOWN {
        // default implementation
    };
}

val currentPlatform: Platform by lazy {
    val platform = _currentPlatform
    GLog.i(TAG, "[currentPlatform] $platform")
    platform
}

private inline val _currentPlatform: Platform
    get() = when (System.getProperty("os.name").lowercase()) {
        "windows" -> Platform.WINDOWS
        "linux" -> Platform.LINUX
        "mac" -> Platform.MAC
        else -> Platform.UNKNOWN
    }

private const val TAG = "Environment"