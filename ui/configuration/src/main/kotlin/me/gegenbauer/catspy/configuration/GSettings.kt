package me.gegenbauer.catspy.configuration

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.strings.Locale
import me.gegenbauer.catspy.utils.toArgb
import java.awt.Color
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import javax.swing.UIManager

data class GSettings(
    var version: Int = 0,
    val debugSettings: Debug = Debug(),

    val logSettings: Log = Log(),
    val themeSettings: Theme = Theme(),
    val mainUISettings: Main = Main(),
    val updateSettings: Update = Update(),
    val windowSettings: Window = Window(),

    var lastFileSaveDir: String = "",
    private val shownHints: MutableSet<String> = mutableSetOf()
): ISettings {

    override fun init() {
        themeSettings.init()
        windowSettings.init()
    }

    data class Debug(
        var globalDebug: Boolean = false,
        var dataBindingDebug: Boolean = false,
        var taskDebug: Boolean = false,
        var ddmDebug: Boolean = false,
        var cacheDebug: Boolean = false,
        var logDebug: Boolean = false
    ) : ISettings

    data class Theme(
        var theme: String = DEFAULT_THEME,
        var font: Font = Font(),
        private var accentColor: Int = 0
    ) : ISettings {
        fun getAccentColor(): Color {
            if (accentColor == 0) {
                return UIManager.getColor("CatSpy.accent.default") ?: Color.lightGray
            }
            return accentColor.toArgb()
        }

        fun setAccentColor(color: Color) {
            accentColor = color.toArgb()
        }
    }

    data class Log(
        var logLevel: String = LogLevel.VERBOSE.logName,
        var dividerLocation: Int = DEFAULT_DIVIDER_LOCATION,
        var rotation: Int = Rotation.ROTATION_LEFT_RIGHT.ordinal,
        var font: Font = Font(),
        val filterHistory: FilterHistory = FilterHistory(),
        val filterEnabledState: FilterEnabledState = FilterEnabledState(),
        val search: Search = Search(),
    ) : ISettings {

        data class FilterHistory(
            val logFilterHistory: MutableList<String> = mutableListOf(),
            val tagFilterHistory: MutableList<String> = mutableListOf(),
            val packageFilterHistory: MutableList<String> = mutableListOf(),
            val highlightHistory: MutableList<String> = mutableListOf(),
        )

        data class FilterEnabledState(
            var logFilterEnabled: Boolean = true,
            var tagFilterEnabled: Boolean = true,
            var pidFilterEnabled: Boolean = true,
            var packageFilterEnabled: Boolean = true,
            var tidFilterEnabled: Boolean = true,
            var logLevelFilterEnabled: Boolean = true,
            var boldEnabled: Boolean = true,
            var filterMatchCaseEnabled: Boolean = false
        )

        data class Search(
            var searchMatchCaseEnabled: Boolean = false,
            val searchHistory: MutableList<String> = mutableListOf()
        )

        companion object {
            private const val DEFAULT_DIVIDER_LOCATION = 500
        }
    }

    data class Main(
        var locale: Int = Locale.EN.ordinal,
        private val shownHints: MutableSet<String> = mutableSetOf()
    ) : ISettings

    data class Font(
        var family: String = DEFAULT_FONT_FAMILY,
        var style: Int = DEFAULT_FONT_STYLE,
        var size: Int = DEFAULT_FONT_SIZE
    ) {

        fun toNativeFont(): java.awt.Font {
            return java.awt.Font(family, style, size)
        }

        fun update(font: java.awt.Font) {
            family = font.family
            style = font.style
            size = font.size
        }
    }

    data class Window(
        val configurations: MutableSet<Configuration> = mutableSetOf()
    ) : ISettings {

        @Transient
        private val configurationCache = mutableMapOf<String, Configuration>()

        override fun init() {
            configurations.forEach { configurationCache[it.id] = it }
        }

        fun loadWindowSettings(window: java.awt.Window, extendedState: Int = Frame.NORMAL) {
            loadWindowSettings(window, null, extendedState)
        }

        fun loadWindowSettings(window: java.awt.Window, defaultBounds: Rectangle? = null, extendedState: Int = Frame.NORMAL) {
            configurationCache[window.javaClass.name]?.takeIf { isAccessibleInAnyScreen(it) }?.let { windowSettings ->
                window.bounds = windowSettings.bounds.toNativeBounds()
                (window as? Frame)?.extendedState = windowSettings.extendedState
            } ?: run {
                (window as? Frame)?.extendedState = extendedState
                window.bounds = defaultBounds ?: Bounds().toNativeBounds()
            }
        }

        fun saveWindowSettings(window: java.awt.Window) {
            val setting = Configuration(
                window.javaClass.name,
                (window as? Frame)?.extendedState ?: Frame.NORMAL,
                Bounds(
                    window.x,
                    window.y,
                    window.width,
                    window.height
                )
            )
            configurationCache[setting.id] = setting
            configurations.remove(setting)
            configurations.add(setting)
        }

        data class Configuration(
            val id: String,
            val extendedState: Int,
            val bounds: Bounds
        ) {
            override fun hashCode(): Int {
                return id.hashCode()
            }

            override fun equals(other: Any?): Boolean {
                return other is Configuration && other.id == id
            }
        }

        data class Bounds(
            val x: Int = 0,
            val y: Int = 0,
            val width: Int = 1000,
            val height: Int = 1000
        ) {
            fun toNativeBounds(): Rectangle {
                return Rectangle(x, y, width, height)
            }
        }

        private fun isAccessibleInAnyScreen(pos: Configuration): Boolean {
            val windowBounds: Rectangle = pos.bounds.toNativeBounds()
            for (gd in GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices) {
                val screenBounds = gd.defaultConfiguration.bounds
                if (screenBounds.intersects(windowBounds)) {
                    return true
                }
            }
            GLog.e(TAG, "Window is not accessible in any screen: $pos")
            return false
        }
    }

    data class Update(
        private val ignoredRelease: MutableList<String> = mutableListOf(),
    ) {
        fun isIgnored(release: String): Boolean {
            return ignoredRelease.contains(release)
        }

        fun addIgnoredRelease(release: String) {
            if (ignoredRelease.size >= MAX_IGNORED_RELEASE) {
                ignoredRelease.removeAt(0)
            }
            ignoredRelease.add(release)
        }

        companion object {
            private const val MAX_IGNORED_RELEASE = 10
        }
    }

    fun isHintShown(hintId: String): Boolean {
        return shownHints.contains(hintId)
    }

    fun setHintShown(hintId: String) {
        shownHints.add(hintId)
    }

    companion object {
        const val DEFAULT_FONT_SIZE = 14
        const val DEFAULT_LOG_FONT_SIZE = 13
        const val DEFAULT_FONT_STYLE = 0
        const val DEFAULT_FONT_FAMILY = "Dialog"
        const val DEFAULT_THEME = "One Dark"
        const val SETTINGS_VERSION = 2


        private const val TAG = "GSettings"
    }
}

interface ISettings {
    fun init() {}
}


