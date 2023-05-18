package me.gegenbauer.catspy.task

import me.gegenbauer.catspy.log.GLog

class GetDeviceTask : CommandTask(arrayOf("adb", "devices"), name = "GetDeviceTask") {
    private val output = StringBuilder()

    override suspend fun onReceiveOutput(line: String) {
        super.onReceiveOutput(line)
        output.appendLine(line)
    }

    override fun onProcessEnd() {
        super.onProcessEnd()
        val devices = arrayListOf<String>().apply {
            filterOutput(output.toString()).forEach { line ->
                val textSplit = line.trim().split("\t")
                if (textSplit.size >= 2) {
                    GLog.d(name, "device : ${textSplit[0]}")
                    add(textSplit[0])
                }
            }
        }
        output.clear()
        GLog.d(name, "[onProcessEnd] devices : $devices")
        notifyFinalResult(devices)
    }

    private fun filterOutput(output: String): List<String> {
        val lines = output.trim().lines()
        if (lines.size < 2) {
            return emptyList()
        }
        return if (lines[0].contains("List of devices attached")) {
            lines.subList(1, lines.size)
        } else {
            emptyList()
        }
    }
}