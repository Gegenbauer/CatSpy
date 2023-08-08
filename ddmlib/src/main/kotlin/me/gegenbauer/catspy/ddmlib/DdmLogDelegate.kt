package me.gegenbauer.catspy.ddmlib

import com.android.ddmlib.Log

class DdmLogDelegate: Log.ILogOutput {

    override fun printLog(logLevel: Log.LogLevel, tag: String, message: String) {
        printWithGLog(logLevel, tag, message)
    }

    private fun printWithGLog(logLevel: Log.LogLevel, tag: String, message: String) {
        when (logLevel) {
            Log.LogLevel.DEBUG -> DdmLog.d(tag, message)
            Log.LogLevel.ERROR -> DdmLog.e(tag, message)
            Log.LogLevel.INFO -> DdmLog.i(tag, message)
            Log.LogLevel.VERBOSE -> DdmLog.v(tag, message)
            Log.LogLevel.WARN -> DdmLog.w(tag, message)
            else -> DdmLog.d(tag, message)
        }
    }

    override fun printAndPromptLog(logLevel: Log.LogLevel, tag: String, message: String) {
        printWithGLog(logLevel, tag, message)
    }

    companion object {
        var debug = false
    }
}