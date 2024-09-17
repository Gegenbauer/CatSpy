package me.gegenbauer.catspy.utils.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.file.getFilePath
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.utils.file.KeyValuesFileManager
import me.gegenbauer.catspy.utils.file.StringFileManager
import java.io.File
import java.lang.ref.WeakReference

abstract class BaseUserPreferences : UserPreferences {
    /**
     * xml file path -> preferences map
     */
    private val preferenceGroup = mutableMapOf<String, MutableMap<String, Any?>>()

    private val preferencesChangeListeners =
        mutableMapOf<String, MutableList<WeakReference<UserPreferences.PreferencesChangeListener>>>()
    private val rootDir = filesDir.appendPath(PREFERENCES_DIR)
    private val scope = AppScope

    abstract val keyValuesFileManager: KeyValuesFileManager
    
    override suspend fun loadFromDisk() {
        withContext(Dispatchers.GIO) {
            synchronized(this) {
                ensureDir()
                File(rootDir).listFiles()
                    ?.filter { it.extension == keyValuesFileManager.fileExtension }
                    ?.forEach { file ->
                        val preferencesStr = keyValuesFileManager.read(file)
                        val preferences = keyValuesFileManager.deserialize(preferencesStr)
                        val key = file.nameWithoutExtension.removePrefix(StringFileManager.JSON_PREFIX)
                        preferenceGroup[key] = preferences.toMutableMap()
                    }
            }
        }
    }

    private fun ensureDir() {
        File(rootDir).mkdirs()
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String, defaultValue: T): T {
        val cachedKey = getCachedKey(key)
        return preferenceGroup[cachedKey.path]?.get(cachedKey.storeKey) as? T ?: defaultValue
    }

    @Synchronized
    override fun <T> put(key: String, value: T?) {
        val cachedKey = getCachedKey(key)
        if (!preferenceGroup.containsKey(cachedKey.path)) {
            preferenceGroup[cachedKey.path] = mutableMapOf()
        }
        preferenceGroup[cachedKey.path]?.put(cachedKey.storeKey, value)
        notifyListeners(key)
        saveToDisk(cachedKey.path, preferenceGroup[cachedKey.path])
    }

    @Synchronized
    override fun getString(key: String, defaultValue: String): String {
        return get(key, defaultValue)
    }

    @Synchronized
    override fun putString(key: String, value: String) {
        put(key, value)
    }

    private fun getCachedKey(key: String): Key {
        if (key.contains(KEY_SEPARATOR)) {
            val storeKey = key.substringAfterLast(KEY_SEPARATOR)
            val filePath = getFilePath(key.substringBeforeLast(KEY_SEPARATOR))
            return Key(filePath.parentDir, filePath.fileName, storeKey)
        }
        return Key(EMPTY_STRING, DEFAULT_FILE_NAME, key)
    }

    @Synchronized
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return get(key, defaultValue)
    }

    @Synchronized
    override fun putBoolean(key: String, value: Boolean) {
        put(key, value)
    }

    @Synchronized
    override fun getInt(key: String, defaultValue: Int): Int {
        return get(key, defaultValue)
    }

    @Synchronized
    override fun putInt(key: String, value: Int) {
        put(key, value)
    }

    @Synchronized
    override fun getLong(key: String, defaultValue: Long): Long {
        return get(key, defaultValue)
    }

    @Synchronized
    override fun putLong(key: String, value: Long) {
        put(key, value)
    }

    @Synchronized
    override fun getFloat(key: String, defaultValue: Float): Float {
        return get(key, defaultValue)
    }

    @Synchronized
    override fun putFloat(key: String, value: Float) {
        put(key, value)
    }

    override fun getStringList(key: String, defaultValue: List<String>): List<String> {
        return get(key, defaultValue)
    }

    override fun putStringList(key: String, value: List<String>) {
        put(key, value)
    }

    override fun putStringList(key: String, value: List<String>, maxSize: Int) {
        put(key, value.takeLast(maxSize))
    }

    override fun addChangeListener(listener: UserPreferences.PreferencesChangeListener) {
        synchronized(preferencesChangeListeners) {
            val key = listener.key
            if (!preferencesChangeListeners.containsKey(key)) {
                preferencesChangeListeners[key] = mutableListOf()
            }
            preferencesChangeListeners[key]?.add(WeakReference(listener))
        }
    }

    private fun notifyListeners(key: String) {
        synchronized(preferencesChangeListeners) {
            preferencesChangeListeners[key]?.forEach { listenerRef ->
                listenerRef.get()?.onPreferencesChanged()
            }
        }
    }

    @Synchronized
    override fun remove(key: String) {
        val cachedKey = getCachedKey(key)
        preferenceGroup[cachedKey.path]?.remove(cachedKey.storeKey)
        notifyListeners(key)
        saveToDisk(cachedKey.path, preferenceGroup[cachedKey.path])
    }

    @Synchronized
    override fun clear() {
        preferenceGroup.clear()
        File(rootDir).listFiles()?.forEach { it.delete() }
    }

    @Synchronized
    override fun contains(key: String): Boolean {
        val cachedKey = getCachedKey(key)
        return preferenceGroup[cachedKey.path]?.containsKey(cachedKey.storeKey) ?: false
    }

    private fun saveToDisk(key: String, preferences: Map<String, Any?>?) {
        scope.launch {
            withContext(Dispatchers.GIO) {
                val path = getFilePath(key)
                val file = File(rootDir, path.parentDir)
                if (!file.exists()) {
                    file.mkdirs()
                }
                val saveKey = PREFERENCES_DIR.appendPath(key)
                if (preferences == null) {
                    keyValuesFileManager.delete(saveKey)
                } else {
                    val xmlString = keyValuesFileManager.serialize(preferences)
                    keyValuesFileManager.write(saveKey, xmlString)
                }
            }
        }
    }

    private data class Key(
        val parentDir: String,
        val fileName: String,
        val storeKey: String
    ) {
        val path: String
            get() = parentDir.appendPath(fileName)
    }

    companion object {
        private const val PREFERENCES_DIR = "preferences"
        private const val DEFAULT_FILE_NAME = "default"
        const val KEY_SEPARATOR = "/"
    }
}

fun String.appendKeySeparator(key: String): String {
    return this + BaseUserPreferences.KEY_SEPARATOR + key
}