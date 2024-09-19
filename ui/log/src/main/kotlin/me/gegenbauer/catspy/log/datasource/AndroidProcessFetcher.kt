package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class AndroidProcessFetcher(private val device: String) {
    private val packageNameCache = ConcurrentHashMap<String, String>()
    private var timeOnDevice = INVALID_TIME

    suspend fun init() {
        timeOnDevice = ensureTimeOnDevice()
        updatePidToPackageMap()
    }

    fun invalidateTimeOnDevice() {
        timeOnDevice = INVALID_TIME
    }

    private fun isTimeOnDeviceValid(): Boolean {
        return timeOnDevice != INVALID_TIME
    }

    /**
     * Query package name by pid.
     * @param pid process id
     * @param time time of the log, used to determine whether the cache is valid
     */
    suspend fun queryPackageName(pid: String, time: String): String {
        if (pid.isEmpty()) {
            return EMPTY_STRING
        }
        val cachedPackageName = packageNameCache[pid]
        if (cachedPackageName != null) {
            return cachedPackageName
        }
        timeOnDevice = ensureTimeOnDevice()
        if (time < timeOnDevice) {
            return EMPTY_STRING
        }
        updatePidToPackageMap()
        var packageName = packageNameCache[pid] ?: EMPTY_STRING
        if (INTERMEDIATE_PROCESS_NAMES.contains(packageName)) {
            Log.d(TAG, "[queryPackageName] intermediate process name: $packageName, wait for update.")
            delay(DELAY_FOR_PROCESS_NAME_UPDATE)
            updatePidToPackageMap()
            packageName = packageNameCache[pid] ?: EMPTY_STRING
        }
        return packageName
    }

    private suspend fun updatePidToPackageMap() {
        withContext(Dispatchers.GIO) {
            kotlin.runCatching {
                if (device.isEmpty()) {
                    return@withContext
                }
                Log.d(TAG, "[updatePidToPackageMap] cache missed, query package info from device.")
                val process = Runtime.getRuntime().exec("${SettingsManager.adbPath} -s $device shell ps")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.forEachLine {
                    parseLineToPidPackage(it)?.let { (pid, packageName) ->
                        packageNameCache[pid] = packageName
                    }
                }
            }.onFailure {
                Log.e(TAG, "[updatePidToPackageMap] failed", it)
            }
        }
    }

    private fun parseLineToPidPackage(line: String): Pair<String, String>? {
        val columns = line.split(splitRegex)
        if (columns.size != 9) {
            return null
        }
        return Pair(columns[1], columns[columns.size - 1])
    }

    private suspend fun ensureTimeOnDevice(): String {
        if (isTimeOnDeviceValid()) {
            return timeOnDevice
        }
        return withContext(Dispatchers.GIO) {
            fetchTimeOnDevice().also {
                Log.d(TAG, "[ensureTimeOnDevice] time on device: $it")
            }
        }
    }

    /**
     * adb shell "date +'%m-%d %H:%M:%S' | awk '{printf \"%s.\", \$0}'; date +'%N' | cut -c1-3"
     */
    private fun fetchTimeOnDevice(): String {
        return kotlin.runCatching {
            val dateTimeProcess = Runtime.getRuntime().exec(
                "${SettingsManager.adbPath} -s " +
                        "$device shell date +'%m-%d %H:%M:%S'"
            )
            val millisecondsProcess = Runtime.getRuntime().exec(
                "${SettingsManager.adbPath} -s" +
                        " $device shell date +'%N' | cut -c1-3"
            )

            val dateTimeReader = BufferedReader(InputStreamReader(dateTimeProcess.inputStream))
            val millisecondsReader = BufferedReader(InputStreamReader(millisecondsProcess.inputStream))

            val dateTime = dateTimeReader.readLine()
            val milliseconds = millisecondsReader.readLine()

            "$dateTime.$milliseconds"
        }.onFailure {
            Log.e(TAG, "[fetchTimeOnDevice] failed", it)
        }.getOrDefault(EMPTY_STRING)
    }

    companion object {
        private const val TAG = "AndroidProcessFetcher"
        private const val DELAY_FOR_PROCESS_NAME_UPDATE = 100L
        private const val INVALID_TIME = "INVALID"

        private val splitRegex by lazy { "\\s+".toRegex() }
        private val INTERMEDIATE_PROCESS_NAMES = setOf(
            "<pre-initialized>",
            "zygote",
            "zygote64",
        )
    }
}