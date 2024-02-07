package me.gegenbauer.catspy.ddmlib.adb

import me.gegenbauer.catspy.ddmlib.log.DdmLog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "AdbSupport"
private const val TCP_PORT_PATTERN = "tcp:%d"
private const val ADB_COMMAND_START_SERVER = "start-server"

@Throws(IOException::class)
fun startServer(adbConf: AdbConf): Boolean {
    val tcpPort = String.format(TCP_PORT_PATTERN, adbConf.port)
    val startAdbServerCommand = arrayListOf(adbConf.path, "-L", tcpPort, ADB_COMMAND_START_SERVER)
    val proc = ProcessBuilder(startAdbServerCommand)
        .redirectErrorStream(true)
        .start()
    runCatching {
        // wait for adb server to start, it takes a few seconds
        proc.waitFor(10, TimeUnit.SECONDS)
        proc.exitValue()
        DdmLog.i(TAG, "[startServer] adb server started")
    }.onFailure {
        DdmLog.e(TAG, "[startServer] failed to start adb server", it)
        proc.destroyForcibly()
        return false
    }

    val out = ByteArrayOutputStream()
    proc.inputStream.copyTo(out)
    val output = out.toString()
    return output.contains(tcpPort).also {
        if (it) {
            DdmLog.e(TAG, "[startServer] failed to start adb server, $output")
        }
    }
}

fun stopServer(adbConf: AdbConf) {
    val stopAdbServerCommand = arrayListOf(adbConf.path, "kill-server")
    val proc = ProcessBuilder(stopAdbServerCommand)
        .redirectErrorStream(true)
        .start()
    runCatching {
        proc.waitFor(3, TimeUnit.SECONDS)
        proc.exitValue()
        DdmLog.i(TAG, "[stopServer] adb server stopped")
    }.onFailure {
        DdmLog.e(TAG, "[stopServer] failed to stop adb server", it)
        proc.destroyForcibly()
    }
}

fun isServerRunning(adbConf: AdbConf): Boolean {
    return runCatching { adbConf.socket().use { true } }.getOrDefault(false)
}