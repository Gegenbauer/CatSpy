package me.gegenbauer.catspy.command

import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.task.*
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.utils.currentPlatform
import java.io.IOException
import java.util.*
import javax.swing.JOptionPane


object LogCmdManager {
    private const val TAG = "LogCmdManager"
    private val taskManager = TaskManager()

    const val EVENT_NONE = 0
    private const val EVENT_SUCCESS = 1
    private const val EVENT_FAIL = 2

    const val CMD_CONNECT = 1
    const val CMD_GET_DEVICES = 2
    private const val CMD_LOGCAT = 3
    const val CMD_DISCONNECT = 4

    const val DEFAULT_LOGCAT = "logcat -v threadtime"
    const val LOG_CMD_MAX = 10

    const val TYPE_CMD_PREFIX = "CMD:"
    const val TYPE_CMD_PREFIX_LEN = 4
    const val TYPE_LOGCAT = 0
    const val TYPE_CMD = 1

    var prefix: String = UIConfManager.uiConf.adbPrefix.ifEmpty { STRINGS.ui.app }
    var adbCmd = UIConfManager.uiConf.adbCommand.ifEmpty { currentPlatform.adbCommand }
    var logSavePath: String = UIConfManager.uiConf.adbLogSavePath.ifEmpty { "." }
    var targetDevice: String = ""
    var logCmd: String = UIConfManager.uiConf.adbLogCommand.ifEmpty { DEFAULT_LOGCAT }
    var devices = ArrayList<String>()
    private val eventListeners = ArrayList<AdbEventListener>()
    private var mainUI: MainUI? = null

    fun setMainUI(mainUI: MainUI) {
        this.mainUI = mainUI
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
        currentExecutor?.interrupt()
        currentExecutor = null
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
    private fun execute(cmd: Runnable?) {
        cmd?.run()
    }

    private fun makeExecutor(cmdNum: Int): Runnable? {
        var executor: Runnable? = null
        when (cmdNum) {
            CMD_CONNECT -> executor = Runnable {
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
                    return@Runnable
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

            CMD_GET_DEVICES -> executor = Runnable {
                val getDeviceTask = GetDeviceTask()
                getDeviceTask.addListener(object : TaskListener {
                    override fun onFinalResult(task: Task, data: Any) {
                        devices.clear()
                        devices.addAll(data as List<String>)
                        val adbEvent = AdbEvent(CMD_GET_DEVICES, EVENT_SUCCESS)
                        sendEvent(adbEvent)
                    }
                })
                taskManager.exec(getDeviceTask)
            }

            CMD_LOGCAT -> executor = Runnable {
            }

            CMD_DISCONNECT -> executor = Runnable {
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
                    return@Runnable
                }

                val adbEvent = AdbEvent(CMD_DISCONNECT, EVENT_SUCCESS)
                sendEvent(adbEvent)
            }
        }

        return executor
    }

    fun interface AdbEventListener {
        fun changedStatus(event: AdbEvent)
    }

    class AdbEvent(c: Int, e: Int) {
        val cmd = c
        val event = e
    }

    fun interface ProcessListener : EventListener {
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
