package me.gegenbauer.catspy.ddmlib

import com.android.ddmlib.Log
import me.gegenbauer.catspy.log.GLog

class DdmLogDelegate: Log.ILogOutput {
    override fun printLog(logLevel: Log.LogLevel, tag: String, message: String) {
        printWithGLog(logLevel, tag, message)
    }

    private fun printWithGLog(logLevel: Log.LogLevel, tag: String, message: String) {
        when (logLevel) {
            Log.LogLevel.DEBUG -> GLog.d(tag, message)
            Log.LogLevel.ERROR -> GLog.e(tag, message)
            Log.LogLevel.INFO -> GLog.i(tag, message)
            Log.LogLevel.VERBOSE -> GLog.v(tag, message)
            Log.LogLevel.WARN -> GLog.w(tag, message)
            else -> GLog.d(tag, message)
        }
    }

    override fun printAndPromptLog(logLevel: Log.LogLevel, tag: String, message: String) {
        printWithGLog(logLevel, tag, message)
    }
}