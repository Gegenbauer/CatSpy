package me.gegenbauer.catspy.task

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.CancellablePause
import me.gegenbauer.catspy.log.GLog
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class LoadProcessPackageTask : BaseObservableTask() {
    override val name: String = "LoadProcessPackageTask"
    private val packageToPidMapper = ConcurrentHashMap<String, String>()
    private val cancellablePause = CancellablePause()

    override fun start() {
        scope.launch {
            repeat(Int.MAX_VALUE) {
                delay(1000)
                fetchPackageToPidMap()
                cancellablePause.addPausePoint()
            }
        }
    }

    private fun fetchPackageToPidMap() {
        packageToPidMapper.clear()
        val process = Runtime.getRuntime().exec("adb shell ps | awk '{print \$2 \"$KEY_VALUE_SEPARATOR\" \$9}' | sed '1d'")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        reader.forEachLine {
            val split = it.split(KEY_VALUE_SEPARATOR)
            if (split.size == 2) {
                packageToPidMapper[split[0]] = split[1]
            }
        }
        notifyProgress(HashMap(packageToPidMapper))
        GLog.d(name, "[fetchPackageToPidMap] result=${packageToPidMapper}")
    }

    override fun pause() {
        cancellablePause.pause()
    }

    override fun resume() {
        cancellablePause.resume()
    }

    override fun stop() {
        scope.cancel()
    }

    companion object {
        private const val KEY_VALUE_SEPARATOR = ","
    }
}