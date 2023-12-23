package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.log.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class AndroidProcessFetcher(var device: String) {
    private var pidToPackageMap = HashMap<String, String>()
    private val suspender = CoroutineSuspender()
    private val scope = ModelScope()
    private val splitRegex by lazy { "\\s+".toRegex() }

    private var fetchTask: Job? = null

    fun start() {
        val lastTask = fetchTask
        fetchTask = scope.launch {
            withContext(Dispatchers.GIO) {
                lastTask?.cancelAndJoin()
                while (isActive) {
                    updatePidToPackageMap()
                    delay(UPDATE_INTERVAL)
                }
            }
        }
    }

    private fun updatePidToPackageMap() {
        kotlin.runCatching {
            if (device.isEmpty()) {
                return
            }
            val map = HashMap<String, String>()
            val process = Runtime.getRuntime().exec("adb -s $device shell ps")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.forEachLine {
                parseLineToPidPackage(it)?.let { (pid, packageName) ->
                    map[pid] = packageName
                }
            }
            pidToPackageMap = map
        }.onFailure {
            Log.e(TAG, "[updatePidToPackageMap] failed", it)
        }
    }

    private fun parseLineToPidPackage(line: String): Pair<String, String>? {
        val columns = line.split(splitRegex)
        if (columns.size != 9) {
            return null
        }
        return Pair(columns[1], columns[columns.size - 1])
    }

    fun getPidToPackageMap(): Map<String, String> {
        return pidToPackageMap
    }

    fun pause() {
        Log.d(TAG, "[pause]")
        suspender.enable()
    }

    fun resume() {
        Log.d(TAG, "[resume]")
        suspender.disable()
    }

    fun cancel() {
        Log.d(TAG, "[cancel]")
        fetchTask?.cancel()
    }

    fun destroy() {
        Log.d(TAG, "[destroy]")
        scope.cancel()
    }

    companion object {
        private const val TAG = "AndroidProcessFetcher"
        private const val UPDATE_INTERVAL = 1000L
    }
}