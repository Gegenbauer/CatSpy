package me.gegenbauer.catspy.platform

import me.gegenbauer.catspy.file.appendPath
import java.io.File
import java.lang.management.ManagementFactory
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.TransferHandler.TransferSupport

interface IPlatform {
    val adbCommand: String
        get() = "adb"

    val osName: String
        get() = System.getProperty("os.name")

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

    fun configureUIProperties() {
        // 启用系统抗锯齿，极大提升字体渲染速度
        System.setProperty("awt.useSystemAAFontSettings", "on")
        System.setProperty("swing.aatext", "true")
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
            when {
                osName.uppercase().contains("DEBIAN") -> {
                    Runtime.getRuntime().exec("dolphin --select ${file.absolutePath}")
                }
                osName.uppercase().contains("THUNAR") -> {
                    Runtime.getRuntime().exec("thunar ${file.absolutePath}")
                }
                else -> {
                    Runtime.getRuntime().exec("nautilus --select ${file.absolutePath}")
                }
            }
        }

        override fun configureUIProperties() {
            super.configureUIProperties()
            // enable custom window decorations
            JFrame.setDefaultLookAndFeelDecorated(true)
            JDialog.setDefaultLookAndFeelDecorated(true)
        }
    },
    MAC {
        override fun getFilesDir(): String {
            return userHome.appendPath("Library").appendPath("Application Support").appendPath(GlobalProperties.APP_NAME)
        }

        override fun showFileInExplorer(file: File) {
            Runtime.getRuntime().exec("osascript -e 'tell application \"Finder\" to reveal POSIX file \"${file.absolutePath}\"' -e 'tell application \"Finder\" to activate'")
        }

        override fun configureUIProperties() {
            super.configureUIProperties()
            // enable screen menu bar
            // (moves menu bar from JFrame window to top of screen)
            System.setProperty("apple.laf.useScreenMenuBar", "true")

            // application name used in screen menu bar
            // (in first menu after the "apple" menu)
            System.setProperty("apple.awt.application.name", "FlatLaf Demo")

            // appearance of window title bars
            // possible values:
            //   - "system": use current macOS appearance (light or dark)
            //   - "NSAppearanceNameAqua": use light appearance
            //   - "NSAppearanceNameDarkAqua": use dark appearance
            // (must be set on main thread and before AWT/Swing is initialized;
            //  setting it on AWT thread does not work)
            System.setProperty("apple.awt.application.appearance", "system")
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