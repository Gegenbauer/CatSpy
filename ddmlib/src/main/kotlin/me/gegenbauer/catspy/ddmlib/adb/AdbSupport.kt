package me.gegenbauer.catspy.ddmlib.adb

import me.gegenbauer.catspy.ddmlib.log.DdmLog
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.currentPlatform
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "AdbSupport"
private const val SOCKET_ADDRESS_LOCALHOST = "localhost"
private const val SOCKET_ADDRESS_DEFAULT_PORT = 5037
private const val WAIT_FOR_SERVER_START_RESULT_TIMEOUT = 10L
private const val TCP_PORT_PATTERN = "tcp:%d"
private const val ADB_COMMAND_START_SERVER = "start-server"
private const val ADB_COMMAND_KILL_SERVER = "kill-server"

enum class AdbServerStartResult {
    SUCCESS,
    FAILURE_WRONG_EXECUTABLE,
    FAILURE_FILE_NOT_EXIST,
    FAILURE_ADB_PATH_UNSPECIFIED,
    FAILURE_ADB_UNKNOWN,
}

inline val AdbServerStartResult.isSuccess: Boolean
    get() = this == AdbServerStartResult.SUCCESS

@Throws(IOException::class)
fun startServer(adbConf: AdbConf): AdbServerStartResult {
    val adbExecutable = File(adbConf.path)
    if (adbExecutable.exists().not() || adbExecutable.isDirectory) {
        DdmLog.e(TAG, "[startServer] adb executable does not exist")
        return AdbServerStartResult.FAILURE_FILE_NOT_EXIST
    }
    if (adbConf.path.isEmpty()) {
        DdmLog.e(TAG, "[startServer] adb path is not specified")
        return AdbServerStartResult.FAILURE_ADB_PATH_UNSPECIFIED
    }
    val tcpPort = String.format(TCP_PORT_PATTERN, adbConf.port)
    val startAdbServerCommand = arrayListOf(adbConf.path, "-L", tcpPort, ADB_COMMAND_START_SERVER)

    fun logError(e: Exception) {
        DdmLog.e(TAG, "[startServer] failed", e)
    }

    try {
        val proc = ProcessBuilder(startAdbServerCommand)
            .redirectErrorStream(true)
            .start()
        try {
            // wait for adb server to start, it takes a few seconds
            proc.waitFor(WAIT_FOR_SERVER_START_RESULT_TIMEOUT, TimeUnit.SECONDS)
            proc.exitValue()
            DdmLog.i(TAG, "[startServer] adb server started")
        } catch (e: IOException) {
            logError(e)
            proc.destroyForcibly()
            return AdbServerStartResult.FAILURE_WRONG_EXECUTABLE
        }

        val out = ByteArrayOutputStream()
        proc.inputStream.copyTo(out)
        val output = out.toString()
        val success = output.isEmpty() || output.contains(tcpPort)
        if (!success) {
            DdmLog.e(TAG, "[startServer] failed to start adb server, $output")
            return AdbServerStartResult.FAILURE_ADB_UNKNOWN
        }
        return AdbServerStartResult.SUCCESS
    } catch (e: IOException) {
        logError(e)
        return AdbServerStartResult.FAILURE_WRONG_EXECUTABLE
    } catch (e: Exception) {
        logError(e)
        return AdbServerStartResult.FAILURE_ADB_UNKNOWN
    }
}

fun stopServer(adbConf: AdbConf) {
    val stopAdbServerCommand = arrayListOf(adbConf.path, ADB_COMMAND_KILL_SERVER)
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

fun isServerRunning(port: Int = SOCKET_ADDRESS_DEFAULT_PORT): Boolean {
    return runCatching { Socket(SOCKET_ADDRESS_LOCALHOST, port).use { true } }.getOrDefault(false)
}

fun detectAdbPath(): String {
    val androidSdkPath = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    androidSdkPath?.let {
        val targetPath = it.appendPath("platform-tools").appendPath(currentPlatform.adbExecutable)
        if (File(targetPath).exists()) {
            return targetPath
        }
    }
    GLog.w(TAG, "[detectAdbPath] failed to detect adb path from ANDROID_HOME or ANDROID_SDK_ROOT")


    val envPath = System.getenv("PATH")
    val paths = envPath.split(File.pathSeparator)
    for (path in paths) {
        val targetPath = path.appendPath(currentPlatform.adbExecutable)
        if (File(targetPath).exists()) {
            return targetPath
        }
    }
    GLog.w(TAG, "[detectAdbPath] failed to detect adb path from PATH")
    return ""
}