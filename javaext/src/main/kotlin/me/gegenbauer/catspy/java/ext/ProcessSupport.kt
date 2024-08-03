package me.gegenbauer.catspy.java.ext

import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.glog.GLog

private const val TAG = "ProcessSupport"

fun String.runCommandIgnoreResult() {
    split(" ").runCommandIgnoreResult()
}

fun List<String>.runCommandIgnoreResult() {
    val process = ProcessBuilder(this.toList()).start()

    AppScope.launch {
        process.errorStream.bufferedReader().use {
            val error = it.readText()
            if (error.isNotEmpty()) {
                GLog.e(TAG, "[runCommand] executing command[${this@runCommandIgnoreResult}] failed with error: $error")
                GlobalMessageManager.publish(Message.Error(error))
            }
        }
    }
}