package me.gegenbauer.catspy.log

import com.google.gson.JsonObject
import me.gegenbauer.catspy.utils.migration.JsonMigration
import me.gegenbauer.catspy.utils.migration.findMigrations
import kotlin.test.Test
import kotlin.test.assertEquals

class LogMetadataJsonMigrationTest {

    class Migration1To2 : JsonMigration {
        override val versionFrom: Int = 1
        override val versionTo: Int = 2

        override fun migrate(source: JsonObject) {

        }

    }

    private fun createMigration(from: Int, to: Int): JsonMigration {
        return object : JsonMigration {
            override val versionFrom: Int = from
            override val versionTo: Int = to

            override fun migrate(source: JsonObject) {

            }
        }
    }

    @Test
    fun `should find applicable migrations when 1 migration can be found`() {
        val migrations = listOf<JsonMigration>(
            Migration1To2()
        )
        val foundMigrations = findMigrations(migrations, 1, 2)
        assertEquals(1, foundMigrations.size)
        assertEquals(1, foundMigrations[0].versionFrom)
        assertEquals(2, foundMigrations[0].versionTo)
    }

    @Test
    fun `should return empty list when no applicable migrations can be found due to missing migration to version`() {
        val migrations = listOf<JsonMigration>(
            Migration1To2()
        )
        val foundMigrations = findMigrations(migrations, 1, 3)
        assertEquals(0, foundMigrations.size)
    }

    @Test
    fun `should return empty list when no applicable migrations can be found due to missing migration from version`() {
        val migrations = listOf<JsonMigration>(
            Migration1To2()
        )
        val foundMigrations = findMigrations(migrations, 2, 3)
        assertEquals(0, foundMigrations.size)
    }

    @Test
    fun `should return empty list when version range is not covered by migrations`() {
        val migrations = listOf(
            createMigration(1, 2),
            createMigration(3, 4)
        )
        val foundMigrations = findMigrations(migrations, 1, 4)
        assertEquals(0, foundMigrations.size)
    }

    @Test
    fun `should return correct migrations when version range is covered by migrations`() {
        val migrations = listOf(
            createMigration(1, 2),
            createMigration(2, 3),
            createMigration(3, 4)
        )
        val foundMigrations = findMigrations(migrations, 1, 4)
        assertEquals(3, foundMigrations.size)
        assertEquals(1, foundMigrations[0].versionFrom)
        assertEquals(2, foundMigrations[0].versionTo)
        assertEquals(2, foundMigrations[1].versionFrom)
        assertEquals(3, foundMigrations[1].versionTo)
        assertEquals(3, foundMigrations[2].versionFrom)
        assertEquals(4, foundMigrations[2].versionTo)
    }
}