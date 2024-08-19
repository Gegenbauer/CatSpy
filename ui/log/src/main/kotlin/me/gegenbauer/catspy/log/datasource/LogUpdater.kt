package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.log.Log
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Determine whether to execute the periodic log update task based on the number of triggers that cause log data changes.
 * The log source may come from the producer or an ongoing filtering task.
 * If the number of triggers is 0, pause the log update. If the number of triggers is greater than 0, continue the log update task.
 */
internal class LogUpdater(
    private val fullLogRepo: FullLogRepo,
    private val filteredLogRepo: FilteredLogRepo,
    private val scope: CoroutineScope
) {
    private val lock = ReentrantLock()
    private val updateLogTaskSuspender = CoroutineSuspender("LogUpdater")
    private var updatingFullLogTriggerCount = 0
    private var updatingFilteredLogTriggerCount = 0
    private var notifyDisplayedLogTask: Job? = null

    fun start() {
        if (notifyDisplayedLogTask.isActive) {
            return
        }
        notifyDisplayedLogTask = scope.launch {
            while (isActive) {
                delay(UPDATE_LOG_ITEMS_DELAY)
                updateLogTaskSuspender.checkSuspend()
                fullLogRepo.submitLogItems(true)
                filteredLogRepo.submitLogItems(true)
            }
        }
    }

    fun updateFullLogTriggerCount(increase: Boolean) {
        lock.withLock {
            updatingFullLogTriggerCount += if (increase) 1 else -1
            Log.d(TAG, "[updateFullLogTriggerCount] count=${updatingFullLogTriggerCount}")
            ensureSuspenderState()
            if (updatingFullLogTriggerCount == 0) {
                fullLogRepo.submitLogItems()
            }
        }
    }

    fun updateFilteredLogTriggerCount(increase: Boolean) {
        lock.withLock {
            updatingFilteredLogTriggerCount += if (increase) 1 else -1
            Log.d(TAG, "[updateFilteredLogTriggerCount] count=${updatingFilteredLogTriggerCount}")
            ensureSuspenderState()
            if (updatingFilteredLogTriggerCount == 0) {
                filteredLogRepo.submitLogItems()
            }
        }
    }

    private fun ensureSuspenderState() {
        lock.withLock {
            if (shouldPause()) {
                updateLogTaskSuspender.enable()
            } else {
                updateLogTaskSuspender.disable()
            }
        }
    }

    private fun shouldPause(): Boolean {
        return lock.withLock {
            updatingFilteredLogTriggerCount == 0 && updatingFullLogTriggerCount == 0
        }
    }

    fun pause() {
        updateLogTaskSuspender.enable()
    }

    fun resume() {
        ensureSuspenderState()
    }

    fun cancel() {
        notifyDisplayedLogTask?.cancel()
    }

    companion object {
        private const val UPDATE_LOG_ITEMS_DELAY = 100L

        private const val TAG = "LogUpdater"

        private val Job?.isActive: Boolean
            get() = this?.isActive == true
    }
}