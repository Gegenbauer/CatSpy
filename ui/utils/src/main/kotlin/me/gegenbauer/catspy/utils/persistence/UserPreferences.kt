package me.gegenbauer.catspy.utils.persistence

import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.utils.file.JsonFileManager
import me.gegenbauer.catspy.utils.file.KeyValuesFileManager
import me.gegenbauer.catspy.utils.file.XMLFileManager

interface UserPreferences {

    suspend fun loadFromDisk()

    fun <T> get(key: String, defaultValue: T): T

    fun <T> put(key: String, value: T?)

    fun getString(key: String, defaultValue: String): String

    fun getString(key: String): String {
        return getString(key, EMPTY_STRING)
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

class XmlUserPreferences : BaseUserPreferences() {
    override val keyValuesFileManager: KeyValuesFileManager =
        ServiceManager.getContextService(XMLFileManager::class.java)
}

class JsonUserPreferences : BaseUserPreferences() {
    override val keyValuesFileManager: KeyValuesFileManager =
        ServiceManager.getContextService(JsonFileManager::class.java)
}

object Preferences : UserPreferences by JsonUserPreferences()