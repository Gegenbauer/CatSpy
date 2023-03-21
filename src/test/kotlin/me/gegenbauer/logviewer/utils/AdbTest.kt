package me.gegenbauer.logviewer.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


fun main() {
    val process = Runtime.getRuntime().exec("adb shell ps | awk '{print \$2 \" \" \$9}' | sed '1d'")
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val pidToPackageName = mutableMapOf<String, String>()
    reader.forEachLine {
        val split = it.split(" ")
        if (split.size == 2) {
            pidToPackageName[split[0]] = split[1]
        }
    }
    println(pidToPackageName)
}
private fun getPackageName(pid: String): String? {
    var packageName: String? = null
    try {
        val process = Runtime.getRuntime().exec("adb shell ps")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String
        while (reader.readLine().also { line = it } != null) {
            if (line.contains(pid)) {
                val segments = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                packageName = segments[segments.size - 1]
                break
            }
        }
        reader.close()
        process.destroy()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return packageName
}