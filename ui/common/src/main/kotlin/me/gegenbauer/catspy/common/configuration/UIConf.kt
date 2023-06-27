package me.gegenbauer.catspy.common.configuration

import me.gegenbauer.catspy.common.log.LogLevel
import me.gegenbauer.catspy.utils.filesDir
import java.awt.Font
import java.awt.Frame

// TODO 日志过滤器无法添加；
data class UIConf(
    /** 应用配置 start **/
    var versionCode: Int = 10000,
    var versionName: String = "1.0.0",
    var appHome: String = filesDir, // 应用程序主目录, 用于存放配置文件, 以及日志等临时文件
    var globalDebug: Boolean = false,
    var dataBindingDebug: Boolean = false,
    /** 应用配置 end **/

    /** 主窗口配置 start **/
    var frameX: Int = 0,
    var frameY: Int = 0,
    var frameWidth: Int = 1000,
    var frameHeight: Int = 500,
    var frameExtendedState: Int = Frame.MAXIMIZED_BOTH,
    var rotation: Int = Rotation.ROTATION_LEFT_RIGHT.ordinal,
    var dividerLocation: Int = 500,
    /** 主窗口配置 end **/

    /** 输入历史 start **/
    val logFilterHistory: MutableList<String> = mutableListOf(),
    val tagFilterHistory: MutableList<String> = mutableListOf(),
    val highlightHistory: MutableList<String> = mutableListOf(),
    val searchHistory: MutableList<String> = mutableListOf(),
    val logCmdHistory: MutableList<String> = mutableListOf("logcat -v threadtime"),
    /** 输入历史 end **/

    /** 日志过滤项启用配置 start **/
    var logFilterEnabled: Boolean = true,
    var tagFilterEnabled: Boolean = true,
    var pidFilterEnabled: Boolean = true,
    var tidFilterEnabled: Boolean = true,
    var logLevelFilterEnabled: Boolean = true,
    var boldEnabled: Boolean = true,
    var filterMatchCaseEnabled: Boolean = false,
    /** 日志过滤项启用配置 end **/

    /** 界面主题 start **/
    var uiFontScale: Int = 100,
    var dividerSize: Int = 10,
    var logFontName: String = "DialogInput",
    var logFontSize: Int = 14,
    var logFontStyle: Int = 0,
    val strColorList: MutableList<String> = mutableListOf(),
    /** 界面主题 end **/

    /** 日志命令配置 start **/
    var logLevel: String = LogLevel.VERBOSE.logName,
    var adbLogSavePath: String = "",
    var adbPrefix: String = "",
    var logScrollBackCount: Int = 0,
    var logScrollBackSplitFileEnabled: Boolean = false,
    /** 日志命令配置 end **/

    /** 搜索框配置 start **/
    var searchMatchCaseEnabled: Boolean = false,
    /** 搜索框配置 end **/
) {
    fun getLogFont(): Font {
        return Font(logFontName, logFontStyle, logFontSize)
    }
}