package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.launch
import me.gegenbauer.catspy.java.ext.SPACE_STRING

fun String.runCommandIgnoreResult() {
    split(SPACE_STRING).runCommandIgnoreResult()
}

fun List<String>.runCommandIgnoreResult() {
    val process = ProcessBuilder(this.toList()).start()

    AppScope.launch {
        process.errorStream.bufferedReader().use {
            val error = it.readText()
            if (error.isNotEmpty()) {
                GlobalMessageManager.publish(Message.Error(error))
                throw RuntimeException("[runCommand] executing command[${this@runCommandIgnoreResult}] failed with error: $error")
            }
        }
    }
}