package me.gegenbauer.catspy.configuration

import com.google.gson.JsonObject
import me.gegenbauer.catspy.utils.migration.JsonMigration

object SettingsMigrations {
    private val migrations = emptyList<JsonMigration>()

    // define migrations here

    fun migrate(source: JsonObject, currentVersion: Int): JsonObject? {
        return JsonMigration.migrate(source, migrations, currentVersion)
    }
}