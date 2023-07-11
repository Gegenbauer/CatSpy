package me.gegenbauer.catspy.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import me.gegenbauer.catspy.concurrency.GIO
import java.io.File

/**
 * TODO Progress Calculation is not accurate
 */
open class ReadFileTask(
    private val file: File,
) : PausableTask(Dispatchers.GIO, "ReadFileTask") {

    private var accumulateSize = 0

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
        if (file.exists().not()) {
            notifyError(IllegalArgumentException("File ${file.absolutePath} does not exist"))
            return
        }
        TaskLog.d(name, "[startInCoroutine] read file: ${file.absolutePath}")
        // read file and calculate progress
        val totalSize = file.length()
        val reader = file.bufferedReader()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (!scope.isActive) {
                break
            }
            line?.let {
                addPausePoint()
                notifyProgress(it)
                accumulateSize += it.length
                if (accumulateSize == BATCH_COUNT) {
                    TaskLog.d(name, "[startInCoroutine] progress=${accumulateSize / totalSize.toFloat()}}")
                }
            }
        }
        notifyFinalResult()
        TaskLog.d(name, "[startInCoroutine] progress=${accumulateSize / totalSize.toFloat()}}")
    }

    companion object {
        private const val BATCH_COUNT = 2000
    }
}