package me.gegenbauer.catspy.utils

import me.gegenbauer.catspy.log.appendPath
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import java.io.File
import java.lang.management.ManagementFactory
import javax.swing.TransferHandler.TransferSupport

interface IPlatform {
    val adbCommand: String
        get() = "adb"

    fun getFileDropAction(transferSupport: TransferSupport): Int {
        return transferSupport.sourceDropActions
    }

    fun getFilesDir() = userDir

    fun ensureFilesDirExists() {
        val filesDir = getFilesDir()
        val file = File(filesDir)
        if (!file.exists()) {
            file.mkdirs()
        }
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

        override fun getFilesDir(): String {
            return userHome.appendPath("AppData").appendPath(STRINGS.ui.app)
        }
    },
    LINUX {
        override fun getFilesDir(): String {
            return userHome.appendPath(".config").appendPath(STRINGS.ui.app)
        }
    },
    MAC {
        override fun getFilesDir(): String {
            return userHome.appendPath("Library").appendPath("Application Support").appendPath(STRINGS.ui.app)
        }
    },
    UNKNOWN {
        // default implementation
    };
}

val currentPlatform: Platform by lazy {
    val platform = _currentPlatform
    platform
}

val userDir: String = System.getProperty("user.dir")
val userHome: String = System.getProperty("user.home")

val filesDir = run {
    currentPlatform.ensureFilesDirExists()
    currentPlatform.getFilesDir()
}

private inline val _currentPlatform: Platform
    get() = when (System.getProperty("os.name").lowercase()) {
        "windows" -> Platform.WINDOWS
        "linux" -> Platform.LINUX
        "mac" -> Platform.MAC
        else -> Platform.UNKNOWN
    }