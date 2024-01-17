package me.gegenbauer.catspy.configuration

import com.google.gson.JsonObject

interface Migration {
    val version: Int

    fun migrate(settings: GSettings, oldSettings: JsonObject)
}