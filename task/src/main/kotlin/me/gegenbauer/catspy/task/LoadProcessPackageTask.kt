package me.gegenbauer.catspy.task

import kotlinx.coroutines.delay
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class LoadProcessPackageTask : BaseObservableTask(name = "LoadProcessPackageTask") {
    private var pidToNameMapper = ConcurrentHashMap<String, String>()
    private val coroutineSuspender = CoroutineSuspender("LoadProcessPackageTask")

    override suspend fun startInCoroutine() {
        repeat(Int.MAX_VALUE) {
            updatePidToNameMap()
            delay(3000)
            coroutineSuspender.checkSuspend()
        }
    }

    private fun updatePidToNameMap() {
        pidToNameMapper = ConcurrentHashMap()
        val process = Runtime.getRuntime().exec("adb shell ps | awk '{print \$2 \"$KEY_VALUE_SEPARATOR\" \$9}' | sed '1d'")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        reader.forEachLine {
            val split = it.split(KEY_VALUE_SEPARATOR)
            if (split.size == 2) {
                pidToNameMapper[split[0]] = split[1]
            }
        }
        notifyProgress(HashMap(pidToNameMapper))
        TaskLog.d(name, "[fetchPackageToPidMap] result=${pidToNameMapper}")
    }

    fun getPidToNameMap(): Map<String, String> {
        return pidToNameMapper
    }

    override fun pause() {
        coroutineSuspender.enable()
    }

    override fun resume() {
        coroutineSuspender.disable()
    }

    companion object {
        private const val KEY_VALUE_SEPARATOR = ","
    }
}