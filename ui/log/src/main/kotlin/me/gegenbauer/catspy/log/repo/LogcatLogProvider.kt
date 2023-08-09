package me.gegenbauer.catspy.log.repo

import me.gegenbauer.catspy.log.model.LogcatLogItem
import java.io.File

class LogcatLogProvider: LogProvider<LogcatLogItem> {
    var logTempFile: File? = null
    private val callbacks = mutableListOf<LogObservable.Observer<LogcatLogItem>>()
    private var collector: LogCollector<LogcatLogItem>? = null

    override fun startCollectLog(collector: LogCollector<LogcatLogItem>) {
        this.collector = collector
        collector.addObserver(this)
        collector.startCollecting()
        logTempFile = collector.getLogFile()
    }

    override fun stopCollectLog() {
        collector?.stopCollecting()
    }

    override fun clear() {
        collector?.clear()
    }

    override fun isCollecting(): Boolean {
        return collector?.isCollecting() ?: false
    }

    override fun destroy() {
        stopCollectLog()
        clear()
        collector?.destroy()
    }

    override fun onLogItemReceived(logItem: LogcatLogItem) {
        notifyLogItemReceived(logItem)
    }

    override fun onLogCleared() {
        notifyLogCleared()
    }

    override fun onError(error: Throwable) {
        notifyError(error)
    }

    override fun addObserver(observer: LogObservable.Observer<LogcatLogItem>) {
        callbacks.add(observer)
    }

    override fun removeObserver(observer: LogObservable.Observer<LogcatLogItem>) {
        callbacks.remove(observer)
    }

    override fun notifyLogItemReceived(logItem: LogcatLogItem) {
        callbacks.forEach { it.onLogItemReceived(logItem) }
    }

    override fun notifyError(error: Throwable) {
        callbacks.forEach { it.onError(error) }
    }

    override fun notifyLogCleared() {
        callbacks.forEach { it.onLogCleared() }
    }
}