package me.gegenbauer.logviewer.manager

import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.ui.MainUI
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

    var LaF = MainUI.FLAT_LIGHT_LAF
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

    private fun manageVersion() {
        loadConfig()
        saveConfig()
    }
}

