package me.gegenbauer.catspy.utils.file

import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.MemoryAware
import me.gegenbauer.catspy.file.appendExtension
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
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

    /**
     * Read a string from the file corresponding to the key.
     * If the key contains a file separator, it will be treated as a directory structure, or it will be treated as a file name.
     */
    fun read(filename: String, parentDir: String = ""): String {
        val key = parentDir.appendPath(filename)
        val lock = getLock(key)
        synchronized(lock) {
            val json = jsonCache[key]
            if (json != null) {
                return json
            }
            val jsonFile = getJsonFile(filename, parentDir)
            jsonFile.parentFile.mkdirs()
            val backupFile = getBackupFile(filename, parentDir)
            if (backupFile.exists()) {
                GLog.w(TAG, "[read] backup file $backupFile exists. Last write may not be completed, recovery it.")
                jsonFile.delete()
                backupFile.renameTo(jsonFile)
            }
            if (jsonFile.exists() && jsonFile.canRead().not()) {
                GLog.w(TAG, "[read] Attempt to read json file $jsonFile without read permission")
            }

            if (jsonFile.exists().not()) {
                jsonCache[key] = EMPTY_STRING
                return EMPTY_STRING
            }

            val content = kotlin.runCatching {
                jsonFile.readText()
            }.onFailure {
                GLog.e(TAG, "[read] Failed to read json file $jsonFile", it)
            }.getOrNull() ?: EMPTY_STRING

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
        return read(filename.removePrefix(JSON_PREFIX), dir)
    }

    private fun getJsonFile(filename: String, parentDir: String): File {
        val absolutePath = filesDir
            .appendPath(parentDir)
            .appendPath("$JSON_PREFIX${filename}")
            .appendExtension(fileExtension)
        return File(absolutePath)
    }

    private fun getBackupFile(filename: String, parentDir: String): File {
        val absolutePath = filesDir
            .appendPath(parentDir)
            .appendPath("$JSON_PREFIX${filename}$BACKUP_FILE_SUFFIX")
        return File(absolutePath)
    }

    @Synchronized
    private fun getLock(key: String): Any {
        return locks.getOrPut(key) { Any() }
    }

    fun write(filename: String, parentDir: String = "", content: String) {
        val key = parentDir.appendPath(filename)
        val lock = getLock(key)
        synchronized(lock) {
            val jsonFile = getJsonFile(filename, parentDir)
            jsonFile.parentFile.mkdirs()
            val backupFile = getBackupFile(filename, parentDir)
            if (backupFile.exists()) {
                GLog.w(TAG, "[write] backup file $backupFile exists. Last write may not be completed, deleting it.")
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
                GLog.e(TAG, "[write] Failed to write json file $jsonFile", it)
            }
            backupFile.delete()
            GLog.i(TAG, "[write] Wrote json file $jsonFile")
            jsonCache[key] = content
        }
    }

    fun delete(filename: String, parentDir: String = "") {
        require(filename.contains(File.separator)) {
            "Filename must contain a file separator to avoid conflicts with directory structure"
        }
        val key = parentDir.appendPath(filename)
        val lock = getLock(key)
        synchronized(lock) {
            val jsonFile = getJsonFile(filename, parentDir)
            val backupFile = getBackupFile(filename, parentDir)
            if (jsonFile.exists()) {
                GLog.i(TAG, "[delete] Deleting json file $jsonFile")
                jsonFile.delete()
            } else {
                GLog.w(TAG, "[delete] json file $jsonFile does not exist")
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