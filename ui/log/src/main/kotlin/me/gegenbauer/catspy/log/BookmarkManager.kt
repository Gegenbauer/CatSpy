package me.gegenbauer.catspy.log

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

fun interface BookmarkChangeListener {
    fun bookmarkChanged()
}

class BookmarkManager: ContextService {

    private val bookmarks = HashSet<Int>()
    private val eventListeners = mutableSetOf<BookmarkChangeListener>()
    private val listenerLock = ReentrantReadWriteLock()
    private val bookmarksLock = ReentrantReadWriteLock()

    fun addBookmarkEventListener(listener: BookmarkChangeListener) {
        listenerLock.write { eventListeners.add(listener) }
    }

    fun removeBookmarkEventListener(listener: BookmarkChangeListener) {
        listenerLock.write { eventListeners.remove(listener) }
    }

    fun isBookmark(bookmark: Int): Boolean {
        return bookmarksLock.read { bookmarks.contains(bookmark) }
    }

    fun updateBookmark(bookmark: Int) {
        bookmarksLock.read {
            if (bookmarks.contains(bookmark)) {
                removeBookmark(bookmark)
            } else {
                addBookmark(bookmark)
            }
        }
    }

    fun getAllBookmarks(): List<Int> {
        return bookmarksLock.read { bookmarks.toList() }
    }

    fun checkNewRow(rows: List<Int>): Boolean {
        return bookmarksLock.read { rows.any { !bookmarks.contains(it) } }
    }

    fun addBookmark(bookmark: Int) {
        bookmarksLock.write { bookmarks.add(bookmark) }
        notifyChanged()
    }

    fun removeBookmark(bookmark: Int) {
        bookmarksLock.write { bookmarks.remove(bookmark) }
        notifyChanged()
    }

    fun clear() {
        bookmarksLock.write { bookmarks.clear() }
        notifyChanged()
    }

    private fun notifyChanged() {
        eventListeners.toList().forEach(BookmarkChangeListener::bookmarkChanged)
    }

    override fun onContextDestroyed(context: Context) {
        clear()
    }
}