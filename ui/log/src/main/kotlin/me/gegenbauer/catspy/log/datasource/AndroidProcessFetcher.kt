package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.log.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class AndroidProcessFetcher(private val  device: String) {
    private val pidToPackageMap = HashMap<String, String>()

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
                        pidToPackageMap[pid] = packageName
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

    suspend fun queryPackageName(pid: String): String {
        if (pidToPackageMap.contains(pid)) {
            return pidToPackageMap[pid]!!
        }
        updatePidToPackageMap()
        val packageName = pidToPackageMap[pid] ?: ""
        pidToPackageMap.getOrPut(pid) { packageName }
        return packageName
    }

    companion object {
        private const val TAG = "AndroidProcessFetcher"

        private val splitRegex by lazy { "\\s+".toRegex() }
    }
}