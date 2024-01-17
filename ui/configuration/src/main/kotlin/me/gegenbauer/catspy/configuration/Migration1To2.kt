package me.gegenbauer.catspy.configuration

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.utils.toArgb

class Migration1To2 : Migration {
    override val version = 2

    override fun migrate(settings: GSettings, oldSettings: JsonObject) {
        GLog.i(TAG, "[migrate] start")
        oldSettings["globalDebug"]?.asBoolean?.let {
            settings.debugSettings.globalDebug = it
        }
        oldSettings["dataBindingDebug"]?.asBoolean?.let {
            settings.debugSettings.dataBindingDebug = it
        }
        oldSettings["taskDebug"]?.asBoolean?.let {
            settings.debugSettings.taskDebug = it
        }
        oldSettings["ddmDebug"]?.asBoolean?.let {
            settings.debugSettings.ddmDebug = it
        }
        oldSettings["cacheDebug"]?.asBoolean?.let {
            settings.debugSettings.cacheDebug = it
        }
        oldSettings["logDebug"]?.asBoolean?.let {
            settings.debugSettings.logDebug = it
        }

        oldSettings["theme"]?.asString?.let {
            settings.themeSettings.theme = it
        }
        oldSettings["fontFamily"]?.asString?.let {
            settings.themeSettings.font.family = it
        }
        oldSettings["fontStyle"]?.asInt?.let {
            settings.themeSettings.font.style = it
        }
        oldSettings["fontSize"]?.asInt?.let {
            settings.themeSettings.font.size = it
        }

        oldSettings["dividerLocation"]?.asInt?.let {
            settings.logSettings.dividerLocation = it
        }

        oldSettings["logFilterHistory"]?.asJsonArray?.let {
            settings.logSettings.filterHistory.logFilterHistory.addAll(it.map { it.asString })
        }
        oldSettings["tagFilterHistory"]?.asJsonArray?.let {
            settings.logSettings.filterHistory.tagFilterHistory.addAll(it.map { it.asString })
        }
        oldSettings["packageFilterHistory"]?.asJsonArray?.let {
            settings.logSettings.filterHistory.packageFilterHistory.addAll(it.map { it.asString })
        }
        oldSettings["highlightHistory"]?.asJsonArray?.let {
            settings.logSettings.filterHistory.highlightHistory.addAll(it.map { it.asString })
        }
        oldSettings["searchHistory"]?.asJsonArray?.let {
            settings.logSettings.search.searchHistory.addAll(it.map { it.asString })
        }
        oldSettings["logFilterEnabled"]?.asBoolean?.let {
            settings.logSettings.filterEnabledState.logFilterEnabled = it
        }
        oldSettings["tagFilterEnabled"]?.asBoolean?.let {
            settings.logSettings.filterEnabledState.tagFilterEnabled = it
        }
        oldSettings["pidFilterEnabled"]?.asBoolean?.let {
            settings.logSettings.filterEnabledState.pidFilterEnabled = it
        }
        oldSettings["packageFilterEnabled"]?.asBoolean?.let {
            settings.logSettings.filterEnabledState.packageFilterEnabled = it
        }
        oldSettings["tidFilterEnabled"]?.asBoolean?.let {
            settings.logSettings.filterEnabledState.tidFilterEnabled = it
        }
        oldSettings["logLevelFilterEnabled"]?.asBoolean?.let {
            settings.logSettings.filterEnabledState.logLevelFilterEnabled = it
        }
        oldSettings["boldEnabled"]?.asBoolean?.let {
            settings.logSettings.filterEnabledState.boldEnabled = it
        }
        oldSettings["filterMatchCaseEnabled"]?.asBoolean?.let {
            settings.logSettings.filterEnabledState.filterMatchCaseEnabled = it
        }
        oldSettings["logLevel"]?.asString?.let {
            settings.logSettings.logLevel = it
        }
        oldSettings["searchMatchCaseEnabled"]?.asBoolean?.let {
            settings.logSettings.search.searchMatchCaseEnabled = it
        }
        oldSettings["accentColor"]?.asInt?.let {
            settings.themeSettings.setAccentColor(it.toArgb())
        }
        oldSettings["locale"]?.asInt?.let {
            settings.mainUISettings.locale = it
        }
        oldSettings["lastFileSaveDir"]?.asString?.let {
            settings.lastFileSaveDir = it
        }
        oldSettings["ignoredRelease"]?.asJsonArray
            ?.map(JsonElement::getAsString)
            ?.forEach(settings.updateSettings::addIgnoredRelease)
        settings.version = version
        GLog.i(TAG, "[migrate] end")
    }

    companion object {
        private const val TAG = "Migration1To2"
    }
}