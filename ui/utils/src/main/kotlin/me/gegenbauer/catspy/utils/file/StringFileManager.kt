package me.gegenbauer.catspy.utils.file

import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.MemoryAware
import me.gegenbauer.catspy.file.appendExtension
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.file.getFilePath
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.filesDir
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manage the reading and writing of files storing strings
 *
 * 1. Before writing to a file, a temporary file will be created.
 * After writing is completed, the temporary file will be renamed to the target file.
 * When reading, the temporary file will be read first, and after reading is completed, the temporary file will be deleted.
 *
 * 2. Manage files in the form of keys, with one key corresponding to one file.
 */
abstract class StringFileManager : ContextService {
    private val locks = ConcurrentHashMap<String, Any>()
    private val jsonCache = ConcurrentHashMap<String, String>()

    abstract val fileExtension: String

    fun read(key: String): String {
        val lock = getLock(key)
        synchronized(lock) {
            val json = jsonCache[key]
            if (json != null) {
                return json
            }
            val jsonFile = getJsonFile(key)
            jsonFile.parentFile.mkdirs()
            val backupFile = getBackupFile(key)
            if (backupFile.exists()) {
                GLog.w(TAG, "backup file $backupFile exists. Last write may not be completed, recovery it.")
                jsonFile.delete()
                backupFile.renameTo(jsonFile)
            }
            if (jsonFile.exists() && jsonFile.canRead().not()) {
                GLog.w(TAG, "Attempt to read json file $jsonFile without read permission")
            }

            if (jsonFile.exists().not()) {
                jsonCache[key] = ""
                return ""
            }

            val content = kotlin.runCatching {
                jsonFile.readText()
            }.onFailure {
                GLog.e(TAG, "Failed to read json file $jsonFile", it)
            }.getOrNull() ?: ""

            jsonCache[key] = content
            return content
        }
    }

    fun read(file: File): String {
        val key = file.absolutePath
            .removePrefix(filesDir)
            .removePrefix(File.separator)
            .removeSuffix(".${fileExtension}")
        val filename = key.substringAfterLast(File.separator)
        val dir = key.substringBeforeLast(File.separator)
        return read(dir.appendPath(filename.removePrefix(JSON_PREFIX)))
    }

    private fun getJsonFile(key: String): File {
        val filePath = getFilePath(key)
        val absolutePath = filesDir
            .appendPath(filePath.parentDir)
            .appendPath("$JSON_PREFIX${filePath.fileName}")
            .appendExtension(fileExtension)
        return File(absolutePath)
    }

    private fun getBackupFile(key: String): File {
        val filePath = getFilePath(key)
        val absolutePath = filesDir
            .appendPath(filePath.parentDir)
            .appendPath("$JSON_PREFIX${filePath.fileName}$BACKUP_FILE_SUFFIX")
        return File(absolutePath)
    }

    @Synchronized
    private fun getLock(key: String): Any {
        return locks.getOrPut(key) { Any() }
    }

    fun write(key: String, content: String) {
        val lock = getLock(key)
        synchronized(lock) {
            val jsonFile = getJsonFile(key)
            jsonFile.parentFile.mkdirs()
            val backupFile = getBackupFile(key)
            if (backupFile.exists()) {
                GLog.w(TAG, "backup file $backupFile exists. Last write may not be completed, deleting it.")
                backupFile.delete()
            }
            if (jsonFile.exists()) {
                jsonFile.renameTo(backupFile)
            }
            runCatching {
                if (jsonFile.exists().not()) {
                    jsonFile.createNewFile()
                }
                jsonFile.writeText(content)
            }.onFailure {
                GLog.e(TAG, "Failed to write json file $jsonFile", it)
            }
            backupFile.delete()
            jsonCache[key] = content
        }
    }

    fun delete(key: String) {
        val lock = getLock(key)
        synchronized(lock) {
            val jsonFile = getJsonFile(key)
            val backupFile = getBackupFile(key)
            if (jsonFile.exists()) {
                jsonFile.delete()
            }
            if (backupFile.exists()) {
                backupFile.delete()
            }
            jsonCache.remove(key)
        }
    }

    override fun onTrimMemory(level: MemoryAware.Level) {
        if (level == MemoryAware.Level.HIGH) {
            jsonCache.clear()
        }
    }

    companion object {
        private const val TAG = "StringFileManager"

        private const val BACKUP_FILE_SUFFIX = ".bak"
        const val JSON_PREFIX = "catspy_"
    }

}