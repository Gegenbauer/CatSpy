package me.gegenbauer.logviewer.manager

import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.log.GLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

// TODO refactor
object ConfigManager {
    private const val TAG = "ConfigManager"
    private const val CONFIG_FILE = "log_viewer.xml"

    const val ITEM_ADB_LOG_CMD = "ADB_LOG_CMD"

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
        if (UIConfManager.uiConf.appHome.isNotEmpty()) {
            configPath = "${UIConfManager.uiConf.appHome}${File.separator}$CONFIG_FILE"
        }
        GLog.d(TAG, "Config Path : $configPath")
        manageVersion()
    }

    fun loadConfig() {
        kotlin.runCatching {
            FileInputStream(configPath).use {
                properties.loadFromXML(it)
            }
        }.onFailure {
            GLog.e(TAG, "[loadConfig] ${it.message}")
        }
    }

    fun saveConfig() {
        kotlin.runCatching {
            FileOutputStream(configPath).use {
                properties.storeToXML(it, "")
            }
        }.onFailure {
            GLog.e(TAG, "[loadConfig] ${it.message}")
        }
    }

    fun saveItem(key: String, value: String) {
        loadConfig()
        setItem(key, value)
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

    fun saveFontColors(family: String, size: Int) {
        loadConfig()

        UIConfManager.uiConf.logFontName = family
        UIConfManager.uiConf.logFontSize = size
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

    private fun manageVersion() {
        loadConfig()
        if (UIConfManager.uiConf.versionCode == 0) {
            updateConfigFromV0ToV1()
            GLog.d(TAG, "manageVersion : ${UIConfManager.uiConf.versionCode} applied")
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
        UIConfManager.uiConf.versionCode = 1
        GLog.d(TAG, "updateConfigFromV0ToV1 : --")
    }
}

