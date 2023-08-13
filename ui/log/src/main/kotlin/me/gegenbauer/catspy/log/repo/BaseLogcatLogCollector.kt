package me.gegenbauer.catspy.log.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.log.model.LogcatLogItem
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskListener
import me.gegenbauer.catspy.task.TaskManager
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseLogcatLogCollector(
    override val taskManager: TaskManager,
) : LogCollector<LogcatLogItem>, TaskListener {

    protected var logTempFile: File? = null
    private val logItems = mutableListOf<LogcatLogItem>()
    private val logCount = AtomicInteger(0)
    private val scope = ModelScope()
    private val callbacks = mutableListOf<LogObservable.Observer<LogcatLogItem>>()

    private fun observeCollectorTask() {
        collectorTask.addListener(this)
    }

    override fun startCollecting() {
        observeCollectorTask()
        taskManager.exec(collectorTask)
    }

    override fun onProgress(task: Task, data: Any) {
        super.onProgress(task, data)
        processLine(data as String)
    }

    private fun processLine(line: String) {
        val item = LogcatLogItem.from(line, logCount.getAndIncrement())
        notifyLogItemReceived(item)
    }

    override fun notifyError(error: Throwable) {
        callbacks.forEach { it.onError(error) }
    }

    override fun notifyLogItemReceived(logItem: LogcatLogItem) {
        callbacks.forEach { it.onLogItemReceived(logItem) }
    }

    override fun notifyLogCleared() {
        callbacks.forEach { it.onLogCleared() }
    }

    override fun pauseCollecting() {
        collectorTask.pause()
    }

    override fun resumeCollecting() {
        collectorTask.resume()
    }

    override fun stopCollecting() {
        collectorTask.cancel()
        collectorTask.removeListener(this)
    }

    override fun isCollecting(): Boolean {
        return collectorTask.isRunning
    }

    override fun getLogFile(): File? {
        return logTempFile
    }

    override fun clear() {
        logItems.clear()
        logCount.set(0)
        notifyLogCleared()
    }

    override fun onError(task: Task, t: Throwable) {
        super.onError(task, t)
        scope.launch(Dispatchers.UI) {
            notifyError(t)
        }
    }

    override fun addObserver(observer: LogObservable.Observer<LogcatLogItem>) {
        callbacks.add(observer)
    }

    override fun removeObserver(observer: LogObservable.Observer<LogcatLogItem>) {
        callbacks.remove(observer)
    }

    override fun destroy() {
        scope.cancel()
    }
}