package me.gegenbauer.catspy.platform

import me.gegenbauer.catspy.file.appendPath
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

    fun showFileInExplorer(file: File) {}
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
            return userHome.appendPath("AppData").appendPath(GlobalProperties.APP_NAME)
        }

        override fun showFileInExplorer(file: File) {
            Runtime.getRuntime().exec("explorer.exe /select,${file.absolutePath}")
        }
    },
    LINUX {
        override fun getFilesDir(): String {
            return userHome.appendPath(".config").appendPath(GlobalProperties.APP_NAME)
        }

        override fun showFileInExplorer(file: File) {
            Runtime.getRuntime().exec("nautilus ${file.absolutePath}")
        }
    },
    MAC {
        override fun getFilesDir(): String {
            return userHome.appendPath("Library").appendPath("Application Support").appendPath(GlobalProperties.APP_NAME)
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

const val LOG_DIR = "applog"

private inline val _currentPlatform: Platform
    get() = when (System.getProperty("os.name").lowercase()) {
        "windows", "windows 11" -> Platform.WINDOWS
        "linux" -> Platform.LINUX
        "mac" -> Platform.MAC
        else -> Platform.UNKNOWN
    }