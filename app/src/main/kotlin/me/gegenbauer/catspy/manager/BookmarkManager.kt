package me.gegenbauer.catspy.manager

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService

fun interface BookmarkChangeListener {
    fun bookmarkChanged()
}

class BookmarkManager: ContextService {

    private val bookmarks = HashSet<Int>()
    private val eventListeners = ArrayList<BookmarkChangeListener>()

    fun addBookmarkEventListener(listener: BookmarkChangeListener) {
        eventListeners.add(listener)
    }

    fun isBookmark(bookmark: Int): Boolean {
        return bookmarks.contains(bookmark)
    }

    fun updateBookmark(bookmark: Int) {
        if (bookmarks.contains(bookmark)) {
            removeBookmark(bookmark)
        } else {
            addBookmark(bookmark)
        }
    }

    fun checkNewRow(rows: IntArray): Boolean {
        return rows.any { !bookmarks.contains(it) }
    }

    fun addBookmark(bookmark: Int) {
        bookmarks.add(bookmark)
        notifyChanged()
    }

    fun removeBookmark(bookmark: Int) {
        bookmarks.remove(bookmark)
        notifyChanged()
    }

    fun clear() {
        bookmarks.clear()
        notifyChanged()
    }

    private fun notifyChanged() {
        for (listener in eventListeners) {
            listener.bookmarkChanged()
        }
    }

    override fun onContextDestroyed(context: Context) {
        clear()
    }
}