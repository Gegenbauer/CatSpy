package me.gegenbauer.catspy.database

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import me.gegenbauer.catspy.database.log.LogEntry
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.zip.ZipFile

class DatabaseManager : IDatabaseManager {
    override val name: String = "catspy.db"

    override val driver: SqlDriver by lazy {
        JdbcSqliteDriver("jdbc:sqlite:$name").apply {
            execute(null, "PRAGMA foreign_keys=ON;", 0)
        }
    }

    override val database: Database by lazy {
        Database(driver)
    }
    override val logQueries: LogQueries by lazy {
        database.logQueries
    }

    override fun create() {
        Database.Schema.create(driver)
    }
}

fun main() {
    val databaseManager = DatabaseManager()
    var num = 0
    databaseManager.create()
    val file = File("/home/yingbin/temp_log.txt")

    var fileId = 0
    databaseManager.database.transaction {
        databaseManager.database.fileQueries.insertFile(
            file.name,
            file.absolutePath,
            if (file.isArchive()) 1 else 0,
            file.calculateMD5(),
            0
        )
        fileId = databaseManager.database.fileQueries.queryIdByPath(file.absolutePath).executeAsOne().toInt()
    }
    val startTime = System.currentTimeMillis()
    databaseManager.database.transaction {
        file.forEachLine {
            val logEntry = LogEntry.from(it, num++)
            if (num % 10000 == 0) println("$num")
            databaseManager.database.logQueries.insertLogEntry(
                logEntry.num.toLong(),
                fileId.toLong(),
                logEntry.time,
                logEntry.pid,
                logEntry.tid,
                logEntry.level.logName,
                logEntry.tag,
                logEntry.message
            )
        }
    }
    val endTime = System.currentTimeMillis()
    println("insert cost ${(endTime - startTime) / 1000}s")
}

private fun File.isArchive(): Boolean {
    return runCatching {
        ZipFile(this).use { true }
    }.getOrDefault(false)
}

fun File.calculateMD5(): String {
    return runCatching {
        val digest = MessageDigest.getInstance("MD5")
        this.inputStream().use { fileIn ->
            val buffer = ByteArray(8192)
            while (true) {
                val bytesRead = fileIn.read(buffer)
                if (bytesRead <= 0) break
                digest.update(buffer, 0, bytesRead)
            }
        }
        val md5Sum = digest.digest()
        val bigInt = BigInteger(1, md5Sum)
        String.format("%032x", bigInt)
    }.getOrDefault("")
}