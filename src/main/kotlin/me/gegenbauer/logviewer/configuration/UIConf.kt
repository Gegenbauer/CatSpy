package me.gegenbauer.logviewer.configuration

import me.gegenbauer.logviewer.manager.CustomListManager
import me.gegenbauer.logviewer.ui.combobox.FilterComboBox
import me.gegenbauer.logviewer.ui.log.LogLevel

// TODO 日志过滤器无法添加；
data class UIConf(
    /** 应用配置 start **/
    var versionCode: Int = 0,
    var appHome: String = "", // 应用程序主目录, 用于存放配置文件, 以及日志等临时文件
    /** 应用配置 end **/

    /** 主窗口配置 start **/
    var frameX: Int = 0,
    var frameY: Int = 0,
    var frameWidth: Int = 0,
    var frameHeight: Int = 0,
    var frameExtendedState: Int = 0,
    var rotation: Int = 0,
    var dividerLocation: Int = 0,
    var lastDividerLocation: Int = 0,
    /** 主窗口配置 end **/

    /** 输入历史 start **/
    val logFilterHistory: MutableList<String> = mutableListOf(),
    val tagFilterHistory: MutableList<String> = mutableListOf(),
    val highlightHistory: MutableList<String> = mutableListOf(),
    val searchHistory: MutableList<String> = mutableListOf(),
    /** 输入历史 end **/

    /** 日志过滤项启用配置 start **/
    var logFilterEnabled: Boolean = true,
    var tagFilterEnabled: Boolean = true,
    var pidFilterEnabled: Boolean = true,
    var tidFilterEnabled: Boolean = true,
    var highlightEnabled: Boolean = true,
    var filterMatchCaseEnabled: Boolean = false,
    var logFilterComboStyle: FilterComboBox.Mode = FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT,
    var highlightComboStyle: FilterComboBox.Mode = FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT,
    var tagFilterComboStyle: FilterComboBox.Mode = FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT,
    var pidFilterComboStyle: FilterComboBox.Mode = FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT,
    var tidFilterComboStyle: FilterComboBox.Mode = FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT,
    /** 日志过滤项启用配置 end **/

    /** 界面主题 start **/
    var laf: String = "",
    var uiFontScale: Int = 0,
    var dividerSize: Int = 0,
    var logFontName: String = "",
    var logFontSize: Int = 0,
    var logFullViewEnabled: Boolean = false,
    var filterIncrementalEnabled: Boolean = false,
    /** 界面主题 end **/

    /** 日志命令配置 start **/
    var logLevel: String = LogLevel.VERBOSE.logName,
    var adbDevice: String = "",
    var adbCommand: String = "",
    var adbLogCommand: String = "",
    var adbLogSavePath: String = "",
    var adbPrefix: String = "",
    var logScrollBackCount: Int = 0,
    var logScrollBackSplitFileEnabled: Boolean = false,
    /** 日志命令配置 end **/

    /** 搜索框配置 start **/
    var searchMatchCaseEnabled: Boolean = false,
    /** 搜索框配置 end **/

    /** 过滤器列表 start **/
    val filters: MutableList<CustomListManager.CustomElement> = mutableListOf(),
    /** 过滤器列表 end **/

    /** 命令列表 start **/
    val commands: MutableList<CustomListManager.CustomElement> = mutableListOf(),
    /** 命令列表 end **/
)