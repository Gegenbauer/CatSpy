package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.java.ext.WORD_REGEX
import me.gegenbauer.catspy.log.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class AndroidProcessFetcher(private val device: String) {
    private val packageNameCache = ConcurrentHashMap<String, String>()
    private val pidMissingCache = ConcurrentHashMap<String, Boolean>()
    private var timeOnDevice = INVALID_TIME
    private var lastLogTime = INVALID_TIME

    suspend fun init() {
        updatePidToPackageMap("init")
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

        if (isPidMissing(pid)) {
            Log.d(TAG, "[queryPackageName] pid=$pid is missing, ignore.")
            return EMPTY_STRING
        }

        val cachedPackageName = findProcessNameInCache(pid)
        if (cachedPackageName.isNotEmpty()) {
            return cachedPackageName
        }

        if (lastLogTime > time) {
            Log.d(TAG, "[queryPackageName] system time adjusted backwards, invalidateTimeOnDevice.")
            invalidateTimeOnDevice()
        }
        lastLogTime = time

        timeOnDevice = ensureTimeOnDevice()
        if (time < timeOnDevice) {
            Log.d(TAG, "[queryPackageName] time for log is earlier than time on device, ignore.")
            return EMPTY_STRING
        }

        updatePidToPackageMap("cache missed for pid=$pid")
        var packageName = findProcessNameInCache(pid)
        if (INTERMEDIATE_PROCESS_NAMES.contains(packageName)) {
            Log.d(TAG, "[queryPackageName] intermediate process name: $packageName, wait for update.")
            delay(DELAY_FOR_PROCESS_NAME_UPDATE)
            updatePidToPackageMap("intermediate process name")
            packageName = findProcessNameInCache(pid)
        }
        if (packageName.isEmpty()) {
            recordMissingPid(pid)
        }
        return packageName
    }

    private fun findProcessNameInCache(pid: String): String {
        val specialName = SPECIAL_PROCESS_NAMES_MAP[pid]
        if (specialName != null) {
            return specialName
        }
        return packageNameCache[pid] ?: EMPTY_STRING
    }

    private fun recordMissingPid(pid: String) {
        pidMissingCache[pid] = true
    }

    private fun isPidMissing(pid: String): Boolean {
        return pidMissingCache[pid] ?: false
    }

    private suspend fun updatePidToPackageMap(reason: String) {
        withContext(Dispatchers.GIO) {
            kotlin.runCatching {
                if (device.isEmpty()) {
                    return@withContext
                }
                Log.d(TAG, "[updatePidToPackageMap] $reason, query package info from device.")
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
        val columns = line.split(WORD_REGEX)
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
                        " $device shell date +%s%3N"
            )

            val dateTimeReader = InputStreamReader(dateTimeProcess.inputStream)
            val millisecondsReader = InputStreamReader(millisecondsProcess.inputStream)

            val dateTime = dateTimeReader.readLines().first { it.isNotEmpty() }
            val milliseconds = millisecondsReader.readLines().first { it.isNotEmpty() }

            "$dateTime.${(milliseconds.toLongOrNull() ?: 0) % 1000}"
        }.onFailure {
            Log.e(TAG, "[fetchTimeOnDevice] failed", it)
        }.getOrDefault(EMPTY_STRING)
    }

    companion object {
        private const val TAG = "AndroidProcessFetcher"
        private const val DELAY_FOR_PROCESS_NAME_UPDATE = 100L
        private const val INVALID_TIME = "99-99 99:99:99.999"

        private val INTERMEDIATE_PROCESS_NAMES = setOf(
            "<pre-initialized>",
            "zygote",
            "zygote64",
        )
        private val SPECIAL_PROCESS_NAMES_MAP = mapOf(
            "0" to "swapper",
            "1" to "init",
            "2" to "kthreadd",
        )
    }
}