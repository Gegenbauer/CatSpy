package me.gegenbauer.catspy.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.gegenbauer.catspy.log.GLog
import java.io.File

/**
 * TODO Progress Calculation is not accurate
 */
open class ReadFileTask(
    private val file: File,
    private val cacheAllContent: Boolean = false,
    private val delay: Long = 500
) :
    PausableTask(Dispatchers.IO, "ReadFileTask") {
    private val contentCache = StringBuilder()
    private val batchStr = mutableListOf<String>()
    private val batchCount = 20000
    private var currentCount = 0

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
        if (file.exists().not()) {
            notifyError("File: ${file.absolutePath} not exist")
            return
        }
        GLog.d(name, "[startInCoroutine] read file: ${file.absolutePath}")
        // read file and calculate progress
        val totalSize = file.length()
        val reader = file.bufferedReader()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (!scope.isActive) {
                break
            }
            addPausePoint()
            currentCount += line!!.length
            batchStr.add(line!!)
            if (batchStr.size == batchCount) {
                notifyProgress(batchStr)
                batchStr.clear()
                GLog.d(name, "[startInCoroutine] progress=${currentCount / totalSize.toFloat()}}")
                delay(delay)
            }
            if (cacheAllContent) {
                contentCache.appendLine(line)
            }
        }
        notifyProgress(batchStr)
        batchStr.clear()
        GLog.d(name, "[startInCoroutine] progress=${currentCount / totalSize.toFloat()}}")
        notifyFinalResult(if (cacheAllContent) contentCache.toString() else "")
    }
}