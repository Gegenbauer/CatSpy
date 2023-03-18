package me.gegenbauer.logviewer.manager

import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.log.LogLevel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

// TODO refactor
object ConfigManager {
    private const val TAG = "ConfigManager"
    private const val CONFIG_FILE = "log_viewer.xml"
    val APP_HOME: String? = System.getenv("LOG_VIEWER_HOME")
    const val ITEM_CONFIG_VERSION = "CONFIG_VERSION"
    const val ITEM_FRAME_X = "FRAME_X"
    const val ITEM_FRAME_Y = "FRAME_Y"
    const val ITEM_FRAME_WIDTH = "FRAME_WIDTH"
    const val ITEM_FRAME_HEIGHT = "FRAME_HEIGHT"
    const val ITEM_FRAME_EXTENDED_STATE = "FRAME_EXTENDED_STATE"
    const val ITEM_ROTATION = "ROTATION"
    const val ITEM_DIVIDER_LOCATION = "DIVIDER_LOCATION"
    const val ITEM_LAST_DIVIDER_LOCATION = "LAST_DIVIDER_LOCATION"

    const val ITEM_LANG = "LANG"

    const val ITEM_SHOW_LOG = "SHOW_LOG_"
    const val COUNT_SHOW_LOG = 20
    const val ITEM_SHOW_TAG = "SHOW_TAG_"
    const val COUNT_SHOW_TAG = 10

    const val ITEM_HIGHLIGHT_LOG = "HIGHLIGHT_LOG_"
    const val COUNT_HIGHLIGHT_LOG = 10

    const val ITEM_SEARCH_LOG = "SEARCH_LOG_"
    const val COUNT_SEARCH_LOG = 10

    const val ITEM_SEARCH_MATCH_CASE = "SEARCH_MATCH_CASE"

    const val ITEM_SHOW_LOG_CHECK = "SHOW_LOG_CHECK"
    const val ITEM_SHOW_TAG_CHECK = "SHOW_TAG_CHECK"
    const val ITEM_SHOW_PID_CHECK = "SHOW_PID_CHECK"
    const val ITEM_SHOW_TID_CHECK = "SHOW_TID_CHECK"

    const val ITEM_HIGHLIGHT_LOG_CHECK = "HIGHLIGHT_LOG_CHECK"

    const val ITEM_LOG_LEVEL = "LOG_LEVEL"

    const val ITEM_LOOK_AND_FEEL = "LOOK_AND_FEEL"
    const val ITEM_UI_FONT_SIZE = "UI_FONT_SIZE"
    const val ITEM_APPEARANCE_DIVIDER_SIZE = "APPEARANCE_DIVIDER_SIZE"

    const val ITEM_ADB_DEVICE = "ADB_DEVICE"
    const val ITEM_ADB_CMD = "ADB_CMD"
    const val ITEM_ADB_LOG_CMD = "ADB_LOG_CMD"
    const val ITEM_ADB_LOG_SAVE_PATH = "ADB_LOG_SAVE_PATH"
    const val ITEM_ADB_PREFIX = "ADB_PREFIX"

    const val ITEM_FONT_NAME = "FONT_NAME"
    const val ITEM_FONT_SIZE = "FONT_SIZE"
    const val ITEM_VIEW_FULL = "VIEW_FULL"
    const val ITEM_FILTER_INCREMENTAL = "FILTER_INCREMENTAL"

    const val ITEM_SCROLL_BACK = "SCROLL_BACK"
    const val ITEM_SCROLL_BACK_SPLIT_FILE = "SCROLL_BACK_SPLIT_FILE"
    const val ITEM_MATCH_CASE = "MATCH_CASE"

    const val ITEM_FILTERS_TITLE = "FILTERS_TITLE_"
    const val ITEM_FILTERS_FILTER = "FILTERS_FILTER_"
    const val ITEM_FILTERS_TABLE_BAR = "FILTERS_TABLE_BAR"

    const val ITEM_CMD_TITLE = "CMD_TITLE_"
    const val ITEM_CMD_CMD = "CMD_CMD_"
    const val ITEM_CMD_TABLE_BAR = "CMD_TABLE_BAR"

    const val ITEM_COLOR_MANAGER = "COLOR_MANAGER_"
    const val ITEM_COLOR_FILTER_STYLE = "COLOR_FILTER_STYLE"

    const val ITEM_RETRY_ADB = "RETRY_ADB"

    const val ITEM_SHOW_LOG_STYLE = "SHOW_LOG_STYLE"
    const val ITEM_SHOW_TAG_STYLE = "SHOW_TAG_STYLE"
    const val ITEM_SHOW_PID_STYLE = "SHOW_PID_STYLE"
    const val ITEM_SHOW_TID_STYLE = "SHOW_TID_STYLE"
    const val ITEM_BOLD_LOG_STYLE = "BOLD_LOG_STYLE"

    const val ITEM_ICON_TEXT = "ICON_TEXT"
    const val VALUE_ICON_TEXT_I_T = "IconText"
    const val VALUE_ICON_TEXT_I = "Icon"
    const val VALUE_ICON_TEXT_T = "Text"

    var LaF = ""
    private val properties = Properties()
    private var configPath = CONFIG_FILE

    init {
        if (APP_HOME != null) {
            configPath = "$APP_HOME${File.separator}$CONFIG_FILE"
        }
        GLog.d(TAG, "Config Path : $configPath")
        manageVersion()
    }

    private fun setDefaultConfig() {
        properties[ITEM_LOG_LEVEL] = LogLevel.VERBOSE.logName
        properties[ITEM_SHOW_LOG_CHECK] = "true"
        properties[ITEM_SHOW_TAG_CHECK] = "true"
        properties[ITEM_SHOW_PID_CHECK] = "true"
        properties[ITEM_SHOW_TID_CHECK] = "true"
        properties[ITEM_HIGHLIGHT_LOG_CHECK] = "true"
    }

    fun loadConfig() {
        var fileInput: FileInputStream? = null

        try {
            fileInput = FileInputStream(configPath)
            properties.loadFromXML(fileInput)
        } catch (ex: Exception) {
            ex.printStackTrace()
            setDefaultConfig()
        } finally {
            if (null != fileInput) {
                try {
                    fileInput.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    fun saveConfig() {
        var fileOutput: FileOutputStream? = null
        try {
            fileOutput = FileOutputStream(configPath)
            properties.storeToXML(fileOutput, "")
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            if (null != fileOutput) {
                try {
                    fileOutput.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    fun saveItem(key: String, value: String) {
        loadConfig()
        setItem(key, value)
        saveConfig()
    }

    fun saveItems(keys: Array<String>, values: Array<String>) {
        loadConfig()
        setItems(keys, values)
        saveConfig()
    }

    fun getItem(key: String): String? {
        return properties[key] as String?
    }

    fun setItem(key: String, value: String) {
        properties[key] = value
    }

    private fun setItems(keys: Array<String>, values: Array<String>) {
        if (keys.size != values.size) {
            GLog.d(TAG, "saveItem : size not match ${keys.size}, ${values.size}")
            return
        }
        for (idx in keys.indices) {
            properties[keys[idx]] = values[idx]
        }
    }

    fun removeConfigItem(key: String) {
        properties.remove(key)
    }

    fun saveFontColors(family: String, size: Int) {
        loadConfig()

        properties[ITEM_FONT_NAME] = family
        properties[ITEM_FONT_SIZE] = size.toString()
        ColorManager.fullTableColor.putConfig()
        ColorManager.filterTableColor.putConfig()

        saveConfig()
    }

    fun saveFilterStyle(keys: Array<String>, values: Array<String>) {
        loadConfig()
        setItems(keys, values)
        ColorManager.putConfigFilterStyle()
        saveConfig()
    }

    fun loadFilters() : ArrayList<CustomListManager.CustomElement> {
        val filters = ArrayList<CustomListManager.CustomElement>()

        var title: String?
        var filter: String?
        var check: String?
        var tableBar: Boolean
        for (i in 0 until FiltersManager.MAX_FILTERS) {
            title = properties[ITEM_FILTERS_TITLE + i] as? String
            if (title == null) {
                break
            }
            filter = properties[ITEM_FILTERS_FILTER + i] as? String
            if (filter == null) {
                filter = "null"
            }

            check = properties[ITEM_FILTERS_TABLE_BAR + i] as? String
            tableBar = if (!check.isNullOrEmpty()) {
                check.toBoolean()
            } else {
                false
            }
            filters.add(CustomListManager.CustomElement(title, filter, tableBar))
        }

        return filters
    }

    fun saveFilters(filters : ArrayList<CustomListManager.CustomElement>) {
        loadConfig()

        var nCount = filters.size
        if (nCount > FiltersManager.MAX_FILTERS) {
            nCount = FiltersManager.MAX_FILTERS
        }

        for (i in 0 until FiltersManager.MAX_FILTERS) {
            val title = properties[ITEM_FILTERS_TITLE + i] as? String ?: break
            properties.remove(ITEM_FILTERS_TITLE + i)
            properties.remove(ITEM_FILTERS_FILTER + i)
            properties.remove(ITEM_FILTERS_TABLE_BAR + i)
        }

        for (i in 0 until nCount) {
            properties[ITEM_FILTERS_TITLE + i] = filters[i].title
            properties[ITEM_FILTERS_FILTER + i] = filters[i].value
            properties[ITEM_FILTERS_TABLE_BAR + i] = filters[i].tableBar.toString()
        }

        saveConfig()
        return
    }

    fun loadCmd() : ArrayList<CustomListManager.CustomElement> {
        val commands = ArrayList<CustomListManager.CustomElement>()

        var title: String?
        var cmd: String?
        var check: String?
        var tableBar: Boolean
        for (i in 0 until CmdManager.MAX_CMD_COUNT) {
            title = properties[ITEM_CMD_TITLE + i] as? String
            if (title == null) {
                break
            }
            cmd = properties[ITEM_CMD_CMD + i] as? String
            if (cmd == null) {
                cmd = "null"
            }

            check = properties[ITEM_CMD_TABLE_BAR + i] as? String
            tableBar = if (!check.isNullOrEmpty()) {
                check.toBoolean()
            } else {
                false
            }
            commands.add(CustomListManager.CustomElement(title, cmd, tableBar))
        }

        return commands
    }

    fun saveCommands(commands : ArrayList<CustomListManager.CustomElement>) {
        loadConfig()

        var nCount = commands.size
        if (nCount > CmdManager.MAX_CMD_COUNT) {
            nCount = CmdManager.MAX_CMD_COUNT
        }

        for (i in 0 until CmdManager.MAX_CMD_COUNT) {
            val title = properties[ITEM_CMD_TITLE + i] as? String ?: break
            properties.remove(ITEM_CMD_TITLE + i)
            properties.remove(ITEM_CMD_CMD + i)
            properties.remove(ITEM_CMD_TABLE_BAR + i)
        }

        for (i in 0 until nCount) {
            properties[ITEM_CMD_TITLE + i] = commands[i].title
            properties[ITEM_CMD_CMD + i] = commands[i].value
            properties[ITEM_CMD_TABLE_BAR + i] = commands[i].tableBar.toString()
        }

        saveConfig()
        return
    }

    private fun manageVersion() {
        loadConfig()
        var confVer = properties[ITEM_CONFIG_VERSION] as String?
        if (confVer == null) {
            updateConfigFromV0ToV1()
            confVer = properties[ITEM_CONFIG_VERSION] as String?
            GLog.d(TAG, "manageVersion : $confVer applied")
        }

        saveConfig()
    }

    private fun updateConfigFromV0ToV1() {
        GLog.d(TAG, "updateConfigFromV0ToV1 : change color manager properties ++")
        for (idx: Int in 0..22) {
            val item = properties["$ITEM_COLOR_MANAGER$idx"] as String?
            if (item != null) {
                when (idx) {
                    2 -> {
                        properties["$ITEM_COLOR_MANAGER${ColorManager.TableColorType.FULL_LOG_TABLE}_${ColorManager.TableColorIdx.LOG_BG.value}"] = item
                    }
                    3 -> {
                        properties["$ITEM_COLOR_MANAGER${ColorManager.TableColorType.FILTER_LOG_TABLE}_${ColorManager.TableColorIdx.LOG_BG.value}"] = item
                    }
                    else -> {
                        properties["$ITEM_COLOR_MANAGER${ColorManager.TableColorType.FULL_LOG_TABLE}_$idx"] = item
                        properties["$ITEM_COLOR_MANAGER${ColorManager.TableColorType.FILTER_LOG_TABLE}_$idx"] = item
                    }
                }

                properties.remove("$ITEM_COLOR_MANAGER$idx")
            }
        }
        properties[ITEM_CONFIG_VERSION] = "1"
        GLog.d(TAG, "updateConfigFromV0ToV1 : --")
    }
}

