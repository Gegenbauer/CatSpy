package me.gegenbauer.catspy.data.repo.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.data.model.log.LogcatLogItem
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.task.OneTimeTask
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.TaskManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class FilteredLogcatRepository(
    private val taskManager: TaskManager,
    updateUITask: PeriodicTask,
    private val bookmarkManager: BookmarkManager,
) : BaseLogcatRepository(updateUITask) {
    // a copy of all log items used when filter is updated
    private val fullLogItems = mutableListOf<LogcatLogItem>()
    private val fullLogLock = ReentrantReadWriteLock()
    private val updatingFilter = AtomicBoolean(false)
    private var updateFilterTask: UpdateFilterTask? = null

    override fun onItemInsertFromFilterUpdate(logItem: LogcatLogItem) {
        addLogItem(logItem)
    }

    override fun onFilterUpdate() {
        if (fullLogItems.isEmpty()) {
            return
        }
        GLog.d(TAG, "[onFilterUpdate]")
        updatingFilter.set(true)
        cancelFilterUpdate()
        taskManager.exec(UpdateFilterTask().apply {
            updateFilterTask = this
        })
    }

    override fun addLogItem(logItem: LogcatLogItem) {
        super.addLogItem(logItem)
        fullLogLock.write { fullLogItems.add(logItem) }
    }

    override fun cancelFilterUpdate() {
        GLog.d(TAG, "[cancelFilterUpdate]")
        updateFilterTask?.cancel()
        updatingFilter.set(false)
    }

    override fun processCacheForUIUpdate() {
        if (updatingFilter.get()) {
            GLog.d(TAG, "[processCacheForUIUpdate] filter is updating, skip")
            return
        }
        super.processCacheForUIUpdate()
    }

    override fun filterRule(item: LogcatLogItem): Boolean {
        return when {
            fullMode -> true
            bookmarkMode -> bookmarkManager.isBookmark(item.num)
            logFilter.filter(item) -> true
            else -> false
        }
    }

    private fun <R> accessFullLogItems(visitor: (MutableList<LogcatLogItem>) -> R): R {
        return fullLogLock.write { visitor(fullLogItems) }
    }

    inner class UpdateFilterTask : OneTimeTask() {
        override suspend fun startInCoroutine() {
            setRunning(true)
            accessFullLogItems { fullLogItems ->
                accessLogItems { logItems ->
                    logItems.clear()
                    fullLogItems.forEach {
                        if (!isRunning()) {
                            GLog.d(TAG, "[UpdateFilterTask] cancelled")
                            return@forEach
                        }
                        if (filterRule(it)) {
                            logItems.add(it)
                        }
                    }
                    scope.launch(Dispatchers.UI) {
                        notifyLogDataSetChange()
                        updatingFilter.set(false)
                    }
                }
            }
        }

        override fun cancel() {
            super.cancel()
            setRunning(false)
        }
    }

    companion object {
        private const val TAG = "FilteredLogcatRepository"
    }
}