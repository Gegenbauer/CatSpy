package me.gegenbauer.catspy.utils.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.file.getFilePath
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.utils.file.StringFileManager
import me.gegenbauer.catspy.utils.file.XMLFileManager
import java.io.File
import java.lang.ref.WeakReference

interface UserPreferences {

    suspend fun loadFromDisk()

    fun getString(key: String, defaultValue: String): String

    fun getString(key: String): String {
        return getString(key, "")
    }

    fun putString(key: String, value: String)

    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun getBoolean(key: String): Boolean {
        return getBoolean(key, false)
    }

    fun putBoolean(key: String, value: Boolean)

    fun getInt(key: String, defaultValue: Int): Int

    fun getInt(key: String): Int {
        return getInt(key, 0)
    }

    fun putInt(key: String, value: Int)

    fun getLong(key: String, defaultValue: Long): Long

    fun getLong(key: String): Long {
        return getLong(key, 0)
    }

    fun putLong(key: String, value: Long)

    fun getFloat(key: String, defaultValue: Float): Float

    fun getFloat(key: String): Float {
        return getFloat(key, 0f)
    }

    fun putFloat(key: String, value: Float)

    fun getStringList(key: String, defaultValue: List<String>): List<String>

    fun getStringList(key: String): List<String> {
        return getStringList(key, emptyList())
    }

    fun putStringList(key: String, value: List<String>)

    fun putStringList(key: String, value: List<String>, maxSize: Int)

    /**
     * Do not pass in an instance of an anonymous inner class created in a method,
     * it will be recycled after the method is completed.
     */
    fun addChangeListener(listener: PreferencesChangeListener)

    fun remove(key: String)

    fun clear()

    fun contains(key: String): Boolean

    interface PreferencesChangeListener {
        val key: String

        fun onPreferencesChanged()
    }
}

class XmlUserPreferences : UserPreferences {
    /**
     * xml file path -> preferences map
     */
    private val preferenceGroup = mutableMapOf<String, MutableMap<String, Any>>()

    private val preferencesChangeListeners =
        mutableMapOf<String, MutableList<WeakReference<UserPreferences.PreferencesChangeListener>>>()
    private val rootDir = filesDir.appendPath(PREFERENCES_DIR)
    private val scope = AppScope

    private val xmlManager: XMLFileManager
        get() = ServiceManager.getContextService(XMLFileManager::class.java)
    private val keySeparator = "/"
    override suspend fun loadFromDisk() {
        withContext(Dispatchers.GIO) {
            synchronized(this) {
                ensureDir()
                File(rootDir).listFiles()?.filter { it.extension == XMLFileManager.FILE_EXTENSION }?.forEach { file ->
                    val preferencesStr = xmlManager.read(file)
                    val preferences = xmlManager.parseXMLString(preferencesStr)
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
    override fun getString(key: String, defaultValue: String): String {
        val cachedKey = getCachedKey(key)
        return preferenceGroup[cachedKey.path]?.get(cachedKey.storeKey) as? String ?: defaultValue
    }

    @Synchronized
    override fun putString(key: String, value: String) {
        val cachedKey = getCachedKey(key)
        if (!preferenceGroup.containsKey(cachedKey.path)) {
            preferenceGroup[cachedKey.path] = mutableMapOf()
        }
        preferenceGroup[cachedKey.path]!![cachedKey.storeKey] = value
        notifyListeners(key)
        saveToDisk(cachedKey.path, preferenceGroup[cachedKey.path]!!)
    }

    private fun getCachedKey(key: String): Key {
        if (key.contains(keySeparator)) {
            val storeKey = key.substringAfterLast(keySeparator)
            val filePath = getFilePath(key.substringBeforeLast(keySeparator))
            return Key(filePath.parentDir, filePath.fileName, storeKey)
        }
        return Key("", DEFAULT_FILE_NAME, key)
    }

    private data class Key(
        val parentDir: String,
        val fileName: String,
        val storeKey: String
    ) {
        val path: String
            get() = parentDir.appendPath(fileName)
    }

    @Synchronized
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return getString(key, defaultValue.toString()).toBoolean()
    }

    @Synchronized
    override fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    @Synchronized
    override fun getInt(key: String, defaultValue: Int): Int {
        return getString(key, defaultValue.toString()).toInt()
    }

    @Synchronized
    override fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    @Synchronized
    override fun getLong(key: String, defaultValue: Long): Long {
        return getString(key, defaultValue.toString()).toLong()
    }

    @Synchronized
    override fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    @Synchronized
    override fun getFloat(key: String, defaultValue: Float): Float {
        return getString(key, defaultValue.toString()).toFloat()
    }

    @Synchronized
    override fun putFloat(key: String, value: Float) {
        putString(key, value.toString())
    }

    override fun getStringList(key: String, defaultValue: List<String>): List<String> {
        val cachedKey = getCachedKey(key)
        return preferenceGroup[cachedKey.path]?.get(cachedKey.storeKey) as? ArrayList<String> ?: defaultValue
    }

    override fun putStringList(key: String, value: List<String>) {
        val cachedKey = getCachedKey(key)
        if (!preferenceGroup.containsKey(cachedKey.path)) {
            preferenceGroup[cachedKey.path] = mutableMapOf()
        }
        preferenceGroup[cachedKey.path]!![cachedKey.storeKey] = value
        notifyListeners(key)
        saveToDisk(cachedKey.path, preferenceGroup[cachedKey.path]!!)
    }

    override fun putStringList(key: String, value: List<String>, maxSize: Int) {
        putStringList(key, value.take(maxSize))
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
        saveToDisk(cachedKey.path, preferenceGroup[cachedKey.path]!!)
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

    private fun saveToDisk(key: String, preferences: Map<String, Any>) {
        scope.launch {
            withContext(Dispatchers.GIO) {
                val path = getFilePath(key)
                val file = File(rootDir, path.parentDir)
                if (!file.exists()) {
                    file.mkdirs()
                }
                val xmlString = xmlManager.mapToXMLString(preferences)
                xmlManager.write(PREFERENCES_DIR.appendPath(key), xmlString)
            }
        }
    }

    companion object {
        private const val PREFERENCES_DIR = "preferences"

        private const val DEFAULT_FILE_NAME = "default"
    }
}

object Preferences : UserPreferences by XmlUserPreferences()