package me.gegenbauer.catspy.log.metadata

import com.google.gson.JsonObject
import me.gegenbauer.catspy.utils.migration.JsonMigration

object LogMetadataMigrations {
    private val migrations = listOf(
        Migration0To1()
    )

    class Migration0To1 : JsonMigration {
        override val versionFrom: Int = 0

        override val versionTo: Int = 1

        override fun migrate(source: JsonObject) {
            // not implemented
        }
    }

    fun migrate(source: JsonObject, currentVersion: Int): JsonObject? {
        return JsonMigration.migrate(source, migrations, currentVersion)
    }
}