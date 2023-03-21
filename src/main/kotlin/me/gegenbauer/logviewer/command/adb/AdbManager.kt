package me.gegenbauer.logviewer.command.adb

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.coroutines.CoroutineContext

object AdbManager: CoroutineScope {
    private val packageToPidMapper = mutableMapOf<String, String>()
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main

    init {
        launch {
            repeat(Int.MAX_VALUE) {
                delay(1000)
                fetchPackageToPidMap()
            }
        }
    }

    private suspend fun fetchPackageToPidMap() {
        withContext(Dispatchers.IO) {
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
}