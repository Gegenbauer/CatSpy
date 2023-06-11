package me.gegenbauer.catspy.data.repo.log

import me.gegenbauer.catspy.data.model.log.LogcatLogItem
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
        collector?.destroy()
    }

    override fun onLogItemReceived(logItem: LogcatLogItem) {
        callbacks.forEach { it.onLogItemReceived(logItem) }
    }

    override fun onError(error: Throwable) {
        callbacks.forEach { it.onError(error) }
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
}