package me.gegenbauer.logviewer.manager

class BookmarkEvent(change:Int) {
    val bookmarkChange = change
    companion object {
        const val ADDED = 0
        const val REMOVED = 1
    }
}

interface BookmarkEventListener {
    fun bookmarkChanged(event: BookmarkEvent?)
}

class BookmarkManager private constructor(){
    companion object {
        private val mInstance: BookmarkManager = BookmarkManager()

        fun getInstance(): BookmarkManager {
            return mInstance
        }
    }

    val bookmarks = ArrayList<Int>()
    private val mEventListeners = ArrayList<BookmarkEventListener>()

    fun addBookmarkEventListener(listener: BookmarkEventListener) {
        mEventListeners.add(listener)
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

        for (listener in mEventListeners) {
            listener.bookmarkChanged(BookmarkEvent(BookmarkEvent.ADDED))
        }
    }

    fun removeBookmark(bookmark:Int) {
        bookmarks.remove(bookmark)

        for (listener in mEventListeners) {
            listener.bookmarkChanged(BookmarkEvent(BookmarkEvent.REMOVED))
        }
    }

    fun clear() {
        bookmarks.clear()

        for (listener in mEventListeners) {
            listener.bookmarkChanged(BookmarkEvent(BookmarkEvent.REMOVED))
        }
    }
}