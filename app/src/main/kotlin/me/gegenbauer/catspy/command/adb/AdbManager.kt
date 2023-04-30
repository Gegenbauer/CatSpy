package me.gegenbauer.catspy.command.adb

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.ModelScope
import java.io.BufferedReader
import java.io.InputStreamReader

object AdbManager {
    private val packageToPidMapper = mutableMapOf<String, String>()
    private val scope = ModelScope()

    init {
        scope.launch {
            repeat(Int.MAX_VALUE) {
                delay(1000)
                fetchPackageToPidMap()
            }
        }
    }

    private suspend fun fetchPackageToPidMap() {
        withContext(scope.coroutineContext) {
            val process = Runtime.getRuntime().exec("adb shell ps | awk '{print \$2 \" \" \$9}' | sed '1d'")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.forEachLine {
                val split = it.split(" ")
                if (split.size == 2) {
                    packageToPidMapper[split[0]] = split[1]
                }
            }
        }
    }

    fun destroy() {
        scope.cancel()
    }
}