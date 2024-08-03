package me.gegenbauer.catspy.log.recent

import me.gegenbauer.catspy.concurrency.assertInMainThread
import me.gegenbauer.catspy.file.gson
import me.gegenbauer.catspy.utils.persistence.Preferences
import me.gegenbauer.catspy.utils.persistence.UserPreferences
import java.io.File
import java.lang.ref.WeakReference

/**
 * Manages the list of recently opened log files.
 * The list is stored in the [UserPreferences].
 * Methods are not thread-safe and should be called from the main thread.
 */
object RecentLogFiles: UserPreferences.PreferencesChangeListener {
    private const val RECENT_FILE_STORE_KEY = "recent/recent_files"
    private const val MAX_RECENT_FILES = 30
    private const val LAST_OPEN_DIR_STORE_KEY = "last_open_dir/log_files"

    private val recentFilesChangeListeners = mutableListOf<WeakReference<(List<RecentFile>) -> Unit>>()

    override val key: String
        get() = RECENT_FILE_STORE_KEY

    init {
        Preferences.addChangeListener(this)
    }

    fun getLastOpenDir(): String {
        return Preferences.getString(LAST_OPEN_DIR_STORE_KEY, "")
    }

    fun onNewFileOpen(file: String): List<RecentFile> {
        assertInMainThread()
        Preferences.putString(LAST_OPEN_DIR_STORE_KEY, file.substringBeforeLast(File.separator))
        val recentFiles = getRecentFiles().toMutableList()
        val existingFile = recentFiles.find { it.path == file }
        recentFiles.remove(existingFile)
        val updatedFile = existingFile?.copy(lastOpenTime = System.currentTimeMillis()) ?: RecentFile(file)
        recentFiles.add(0, updatedFile)
        if (recentFiles.size > MAX_RECENT_FILES) {
            recentFiles.removeAt(recentFiles.size - 1)
        }
        saveRecentFiles(recentFiles)
        return recentFiles
    }

    fun getRecentFiles(): List<RecentFile> {
        assertInMainThread()
        return Preferences.getStringList(RECENT_FILE_STORE_KEY)
            .map { gson.fromJson(it, RecentFile::class.java) }
    }

    fun saveRecentFiles(files: List<RecentFile>) {
        assertInMainThread()
        Preferences.putStringList(RECENT_FILE_STORE_KEY, files.map { gson.toJson(it) })
    }

    override fun onPreferencesChanged() {
        recentFilesChangeListeners.forEach { listenerRef ->
            listenerRef.get()?.invoke(getRecentFiles())
        }
    }

    /**
     * Do not pass in an instance of an anonymous inner class created in a method,
     * it will be recycled after the method is completed.
     */
    fun registerRecentFileChangeListener(listener: (List<RecentFile>) -> Unit) {
        assertInMainThread()
        recentFilesChangeListeners.add(WeakReference(listener))
    }

}