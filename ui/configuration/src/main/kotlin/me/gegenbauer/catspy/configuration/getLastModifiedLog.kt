package me.gegenbauer.catspy.configuration

import me.gegenbauer.catspy.platform.filesDir
import java.io.File

fun getLastModifiedLog(): File? {
    val logDir = File(filesDir)
    if (logDir.exists().not() || logDir.isFile || logDir.listFiles().isNullOrEmpty()) {
        return null
    }
    return logDir.listFiles()
        ?.filter { it.name.startsWith(GlobalStrings.LOG_NAME) }
        ?.maxByOrNull { it.lastModified() }
}