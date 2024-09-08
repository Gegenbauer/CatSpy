package me.gegenbauer.catspy.platform

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.util.SystemInfo
import me.gegenbauer.catspy.concurrency.runCommandIgnoreResult
import me.gegenbauer.catspy.file.appendPath
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.io.File
import java.lang.management.ManagementFactory
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JRootPane
import javax.swing.TransferHandler.TransferSupport

interface IPlatform {
    val adbExecutable: String
        get() = "adb"

    val osName: String
        get() = System.getProperty("os.name")

    val vkMask: Int
        get() = KeyEvent.CTRL_DOWN_MASK

    val wifiAdbIpPortSeparator: String
        get() = "/"

    fun getFileDropAction(transferSupport: TransferSupport): Int {
        return transferSupport.sourceDropActions
    }

    fun getFilesDir() = userDir

    fun ensureFilesDirExists() {
        val filesDir = getFilesDir()
        val file = File(filesDir)
        file.mkdirs()
    }

    fun showFileInExplorer(file: File) {}

    fun configureUIProperties() {
        System.setProperty("awt.useSystemAAFontSettings", "on")
        System.setProperty("swing.aatext", "true")
    }

    fun setFrameTitleFullscreen(frame: JFrame) {}
}

val isInDebugMode: Boolean by lazy {
    val inputArguments = ManagementFactory.getRuntimeMXBean().inputArguments
    for (arg in inputArguments) {
        if (arg.contains("-agentlib:jdwp")) {
            return@lazy true
        }
    }
    false
}

enum class Platform : IPlatform {
    WINDOWS {
        override val adbExecutable: String
            get() = "adb.exe"

        override val wifiAdbIpPortSeparator: String
            get() = ":"

        override fun getFileDropAction(transferSupport: TransferSupport): Int {
            return transferSupport.dropAction
        }

        override fun getFilesDir(): String {
            return userHome.appendPath("AppData").appendPath(GlobalProperties.APP_NAME)
        }

        override fun showFileInExplorer(file: File) {
            listOf("explorer.exe", "/select,${file.absolutePath}").runCommandIgnoreResult()
        }
    },
    LINUX {
        override fun getFilesDir(): String {
            return userHome.appendPath(".config").appendPath(GlobalProperties.APP_NAME)
        }

        override fun showFileInExplorer(file: File) {
            when {
                osName.uppercase().contains("DEBIAN") -> {
                    listOf("dolphin", "--select", file.absolutePath).runCommandIgnoreResult()
                }

                osName.uppercase().contains("THUNAR") -> {
                    listOf("thunar", file.absolutePath).runCommandIgnoreResult()
                }

                else -> {
                    listOf("nautilus", "--select", file.absolutePath).runCommandIgnoreResult()
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
        override val vkMask: Int
            get() = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx

        override fun getFilesDir(): String {
            return userHome.appendPath("Library").appendPath("Application Support")
                .appendPath(GlobalProperties.APP_NAME)
        }

        override fun showFileInExplorer(file: File) {
            listOf("/bin/bash", "-c", "open -R \"${file.absolutePath}\"").runCommandIgnoreResult()
        }

        override fun configureUIProperties() {
            super.configureUIProperties()
            // enable screen menu bar
            // (moves menu bar from JFrame window to top of screen)
            System.setProperty("apple.laf.useScreenMenuBar", "true")

            // application name used in screen menu bar
            // (in first menu after the "apple" menu)
            System.setProperty("apple.awt.application.name", GlobalProperties.APP_NAME)

            // appearance of window title bars
            // possible values:
            //   - "system": use current macOS appearance (light or dark)
            //   - "NSAppearanceNameAqua": use light appearance
            //   - "NSAppearanceNameDarkAqua": use dark appearance
            // (must be set on main thread and before AWT/Swing is initialized;
            //  setting it on AWT thread does not work)
            System.setProperty("apple.awt.application.appearance", "system")
        }

        override fun setFrameTitleFullscreen(frame: JFrame) {
            super.setFrameTitleFullscreen(frame)

            val rootPane: JRootPane = frame.rootPane
            if (SystemInfo.isMacFullWindowContentSupported) {
                // expand window content into window title bar and make title bar transparent
                rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                rootPane.putClientProperty(
                    FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING,
                    FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING_LARGE
                )

                // hide window title
                rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            }
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

private val userDir: String = System.getProperty("user.dir")
val userHome: String = System.getProperty("user.home")

val filesDir = run {
    currentPlatform.ensureFilesDirExists()
    currentPlatform.getFilesDir()
}

const val LOG_DIR = "applog"

private inline val _currentPlatform: Platform
    get() = when {
        SystemInfo.isWindows -> Platform.WINDOWS
        SystemInfo.isLinux -> Platform.LINUX
        SystemInfo.isMacOS -> Platform.MAC
        else -> Platform.UNKNOWN
    }
