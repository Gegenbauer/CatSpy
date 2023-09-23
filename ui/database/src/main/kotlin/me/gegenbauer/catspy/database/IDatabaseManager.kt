package me.gegenbauer.catspy.database

import com.squareup.sqldelight.db.SqlDriver

interface IDatabaseManager {
    val name: String

    val driver: SqlDriver

    val database: Database

    val logQueries: LogQueries

    fun create()
}