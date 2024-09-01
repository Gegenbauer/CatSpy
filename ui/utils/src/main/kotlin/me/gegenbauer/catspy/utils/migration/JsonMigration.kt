package me.gegenbauer.catspy.utils.migration

import com.google.gson.JsonObject

interface JsonMigration {
    val versionFrom: Int

    val versionTo: Int

    fun migrate(source: JsonObject)

    companion object {
        private const val KEY_VERSION = "version"
        private const val INVALID_VERSION = -1

        fun migrate(
            source: JsonObject,
            availableMigrations: List<JsonMigration>,
            currentVersion: Int
        ): JsonObject? {
            val sourceVersion = INVALID_VERSION.takeUnless { source.has(KEY_VERSION) }
                ?: source.get(KEY_VERSION).asInt
            if (sourceVersion == currentVersion) {
                return source
            }
            val migrations = findMigrations(availableMigrations, sourceVersion, currentVersion)
            if (migrations.isEmpty()) {
                return null
            }
            for (migration in migrations) {
                migration.migrate(source)
            }
            return source
        }
    }
}

/**
 * Find migrations to apply to migrate from version [from] to version [to].
 */
fun findMigrations(migrations: List<JsonMigration>, from: Int, to: Int): List<JsonMigration> {
    val applicableMigrations = migrations.filter { it.versionFrom >= from && it.versionTo <= to }
        .sortedBy { it.versionFrom }

    var currentVersion = from
    for (migration in applicableMigrations) {
        if (migration.versionFrom != currentVersion) {
            return emptyList()
        }
        currentVersion = migration.versionTo
    }

    if (currentVersion != to) {
        return emptyList()
    }

    return applicableMigrations
}