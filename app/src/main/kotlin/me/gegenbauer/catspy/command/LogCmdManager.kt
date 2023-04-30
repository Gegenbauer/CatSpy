package me.gegenbauer.catspy.command

import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.utils.currentPlatform
import java.io.IOException
import java.util.*
import javax.swing.JOptionPane


object LogCmdManager {
    private const val TAG = "LogCmdManager"

    const val EVENT_NONE = 0
    const val EVENT_SUCCESS = 1
    const val EVENT_FAIL = 2

    const val CMD_CONNECT = 1
    const val CMD_GET_DEVICES = 2
    const val CMD_LOGCAT = 3
    const val CMD_DISCONNECT = 4

    const val DEFAULT_LOGCAT = "logcat -v threadtime"
    const val LOG_CMD_MAX = 10

    const val TYPE_CMD_PREFIX = "CMD:"
    const val TYPE_CMD_PREFIX_LEN = 4
    const val TYPE_LOGCAT = 0
    const val TYPE_CMD = 1

    var prefix: String = STRINGS.ui.app
    var adbCmd = "adb"
    var logSavePath: String = "."
    var targetDevice: String = ""
    var logCmd: String = ""
    var devices = ArrayList<String>()
    private val eventListeners = ArrayList<AdbEventListener>()
    private var mainUI: MainUI? = null

    init {
        val cmd = UIConfManager.uiConf.adbCommand
        adbCmd = cmd.ifEmpty { currentPlatform.adbCommand }

        logSavePath = UIConfManager.uiConf.adbLogSavePath.ifEmpty { "." }
        logCmd = UIConfManager.uiConf.adbLogCommand.ifEmpty { DEFAULT_LOGCAT }
        prefix = UIConfManager.uiConf.adbPrefix.ifEmpty { STRINGS.ui.app }
    }

    fun setMainUI(mainUI: MainUI) {
        LogCmdManager.mainUI = mainUI
    }

    fun getDevices() {
        execute(makeExecutor(CMD_GET_DEVICES))
    }

    fun getType(): Int {
        return if (logCmd.startsWith(TYPE_CMD_PREFIX)) {
            TYPE_CMD
        } else {
            TYPE_LOGCAT
        }
    }

    fun connect() {
        if (targetDevice.isEmpty()) {
            GLog.w(TAG, "Target device is not selected")
            return
        }

        execute(makeExecutor(CMD_CONNECT))
    }

    fun disconnect() {
        execute(makeExecutor(CMD_DISCONNECT))
    }

    fun startLogcat() {
        execute(makeExecutor(CMD_LOGCAT))
    }

    fun stop() {
        GLog.d(TAG, "Stop all processes ++")
        processLogcat?.destroy()
        processLogcat = null
        currentExecutor?.interrupt()
        currentExecutor = null
        GLog.d(TAG, "Stop all processes --")
    }

    fun addEventListener(eventListener: AdbEventListener) {
        eventListeners.add(eventListener)
    }

    private fun sendEvent(event: AdbEvent) {
        for (listener in eventListeners) {
            listener.changedStatus(event)
        }
    }

    private var currentExecutor: Thread? = null
    var processLogcat: Process? = null
    private fun execute(cmd: Runnable?) {
        cmd?.run()
    }

    private fun makeExecutor(cmdNum: Int): Runnable? {
        var executor: Runnable? = null
        when (cmdNum) {
            CMD_CONNECT -> executor = Runnable {
                run {
                    val cmd = "$adbCmd connect $targetDevice"
                    val runtime = Runtime.getRuntime()
                    val scanner = try {
                        val process = runtime.exec(cmd)
                        Scanner(process.inputStream)
                    } catch (e: IOException) {
                        GLog.w(TAG, "Failed run $cmd")
                        e.printStackTrace()
                        JOptionPane.showMessageDialog(mainUI, e.message, "Error", JOptionPane.ERROR_MESSAGE)
                        val adbEvent = AdbEvent(CMD_CONNECT, EVENT_FAIL)
                        sendEvent(adbEvent)
                        return@run
                    }

                    var line: String
                    var isSuccess = false
                    while (scanner.hasNextLine()) {
                        line = scanner.nextLine()
                        if (line.contains("connected to")) {
                            GLog.i(TAG, "Success connect to $targetDevice")
                            val adbEvent = AdbEvent(CMD_CONNECT, EVENT_SUCCESS)
                            sendEvent(adbEvent)
                            isSuccess = true
                            break
                        }
                    }

                    if (!isSuccess) {
                        GLog.w(TAG, "Failed connect to $targetDevice")
                        val adbEvent = AdbEvent(CMD_CONNECT, EVENT_FAIL)
                        sendEvent(adbEvent)
                    }
                }
            }

            CMD_GET_DEVICES -> executor = Runnable {
                run {
                    devices.clear()

                    val cmd = "$adbCmd devices"
                    val runtime = Runtime.getRuntime()
                    val scanner = try {
                        val process = runtime.exec(cmd)
                        Scanner(process.inputStream)
                    } catch (e: IOException) {
                        GLog.w(TAG, "Failed run $cmd")
                        e.printStackTrace()
                        val adbEvent = AdbEvent(CMD_GET_DEVICES, EVENT_FAIL)
                        sendEvent(adbEvent)
                        return@run
                    }

                    var line: String
                    while (scanner.hasNextLine()) {
                        line = scanner.nextLine()
                        if (line.contains("List of devices")) {
                            continue
                        }
                        val textSplit = line.trim().split(Regex("\\s+"))
                        if (textSplit.size >= 2) {
                            GLog.d(TAG, "device : ${textSplit[0]}")
                            devices.add(textSplit[0])
                        }
                    }
                    val adbEvent = AdbEvent(CMD_GET_DEVICES, EVENT_SUCCESS)
                    sendEvent(adbEvent)
                }
            }

            CMD_LOGCAT -> executor = Runnable {
                run {
                    processLogcat?.destroy()

                    val cmd = if (targetDevice.isNotBlank()) {
                        if (getType() == TYPE_CMD) {
                            "${logCmd.substring(TYPE_CMD_PREFIX_LEN)} $targetDevice"
                        } else {
                            "$adbCmd -s $targetDevice $logCmd"
                        }
                    } else {
                        if (getType() == TYPE_CMD) {
                            logCmd.substring(TYPE_CMD_PREFIX_LEN)
                        } else {
                            "$adbCmd $logCmd"
                        }
                    }
                    GLog.d(TAG, "Start : $cmd")
                    val runtime = Runtime.getRuntime()
                    try {
                        processLogcat = runtime.exec(cmd)
                        val processExitDetector = ProcessExitDetector(processLogcat!!)
                        processExitDetector.addProcessListener(object : ProcessListener {
                            override fun processFinished(process: Process?) {
                                GLog.d(TAG, "The subprocess has finished")
                            }
                        })
                        processExitDetector.start()
                    } catch (e: IOException) {
                        GLog.e(TAG, "Failed run $cmd")
                        e.printStackTrace()
                        JOptionPane.showMessageDialog(mainUI, e.message, "Error", JOptionPane.ERROR_MESSAGE)
                        processLogcat = null
                        return@run
                    }
                    GLog.d(TAG, "End : $cmd")
                }
            }

            CMD_DISCONNECT -> executor = Runnable {
                run {
                    val cmd = "$adbCmd disconnect"
                    val runtime = Runtime.getRuntime()
                    try {
                        runtime.exec(cmd)
                    } catch (e: IOException) {
                        GLog.d(TAG, "Failed run $cmd")
                        e.printStackTrace()
                        JOptionPane.showMessageDialog(mainUI, e.message, "Error", JOptionPane.ERROR_MESSAGE)
                        val adbEvent = AdbEvent(CMD_DISCONNECT, EVENT_FAIL)
                        sendEvent(adbEvent)
                        return@run
                    }

                    val adbEvent = AdbEvent(CMD_DISCONNECT, EVENT_SUCCESS)
                    sendEvent(adbEvent)
                }
            }
        }

        return executor
    }

    interface AdbEventListener {
        fun changedStatus(event: AdbEvent)
    }

    class AdbEvent(c: Int, e: Int) {
        val cmd = c
        val event = e
    }

    interface ProcessListener : EventListener {
        fun processFinished(process: Process?)
    }

    class ProcessExitDetector(process: Process) : Thread() {
        private var process: Process
        private val listeners: MutableList<ProcessListener> = ArrayList<ProcessListener>()
        override fun run() {
            try {
                process.waitFor()
                for (listener in listeners) {
                    listener.processFinished(process)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        fun addProcessListener(listener: ProcessListener) {
            listeners.add(listener)
        }

        fun removeProcessListener(listener: ProcessListener) {
            listeners.remove(listener)
        }

        init {
            this.process = process
        }
    }
}
