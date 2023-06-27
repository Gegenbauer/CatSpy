package me.gegenbauer.catspy.log.repo

import me.gegenbauer.catspy.log.model.LogItem

interface LogObservable<T: LogItem> {

    fun addObserver(observer: Observer<T>)

    fun removeObserver(observer: Observer<T>)

    fun notifyLogItemReceived(logItem: T)

    fun notifyLogCleared()

    fun notifyError(error: Throwable)

    interface Observer<T: LogItem> {
        fun onLogItemReceived(logItem: T) {}

        fun onLogCleared() {}

        fun onError(error: Throwable) {}
    }
}