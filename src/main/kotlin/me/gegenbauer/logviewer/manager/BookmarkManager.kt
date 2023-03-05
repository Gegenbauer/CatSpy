package me.gegenbauer.logviewer.manager

class BookmarkEvent(change:Int) {
    val bookmarkChange = change
    companion object {
        const val ADDED = 0
        const val REMOVED = 1
    }
}

interface BookmarkEventListener {
    fun bookmarkChanged(event: BookmarkEvent)
}

class BookmarkManager private constructor(){
    companion object {
        private val instance: BookmarkManager = BookmarkManager()

        fun getInstance(): BookmarkManager {
            return instance
        }
    }

    val bookmarks = ArrayList<Int>()
    private val eventListeners = ArrayList<BookmarkEventListener>()

    fun addBookmarkEventListener(listener: BookmarkEventListener) {
        eventListeners.add(listener)
    }

    fun isBookmark(bookmark:Int): Boolean {
        return bookmarks.contains(bookmark)
    }

    fun updateBookmark(bookmark:Int) {
        if (bookmarks.contains(bookmark)) {
            removeBookmark(bookmark)
        } else {
            addBookmark(bookmark)
        }
    }

    fun addBookmark(bookmark:Int) {
        bookmarks.add(bookmark)
        bookmarks.sort()

        for (listener in eventListeners) {
            listener.bookmarkChanged(BookmarkEvent(BookmarkEvent.ADDED))
        }
    }

    fun removeBookmark(bookmark:Int) {
        bookmarks.remove(bookmark)

        for (listener in eventListeners) {
            listener.bookmarkChanged(BookmarkEvent(BookmarkEvent.REMOVED))
        }
    }

    fun clear() {
        bookmarks.clear()

        for (listener in eventListeners) {
            listener.bookmarkChanged(BookmarkEvent(BookmarkEvent.REMOVED))
        }
    }
}