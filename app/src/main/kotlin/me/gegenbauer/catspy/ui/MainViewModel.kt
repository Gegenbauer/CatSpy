package me.gegenbauer.catspy.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.file.copy
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.strings.Configuration
import me.gegenbauer.catspy.strings.STRINGS
import java.io.File

class MainViewModel(override val contexts: Contexts = Contexts.default) : Context, ContextService {

    val memoryInfo: StateFlow<Memory>
        get() = _memoryInfo

    val messageDialog: StateFlow<String>
        get() = _messageDialog

    private val _memoryInfo = MutableStateFlow(Memory.EMPTY)
    private val scope = ViewModelScope()
    private val memoryMonitor: IMemoryMonitor = MemoryMonitor()

    private val _messageDialog = MutableStateFlow("")

    private var memoryMonitorJob: Job? = null

    fun refreshMemoryInfo() {
        scope.launch {
            _memoryInfo.value = memoryMonitor.calculateMemoryUsage()
        }
    }

    fun startMemoryMonitor() {
        GLog.d(TAG, "[startMemoryMonitor]")
        memoryMonitorJob = scope.launch {
           val memory = memoryMonitor.startMonitor()
            memory.collect {
                _memoryInfo.value = it
            }
        }
    }

    /**
     * 增加文件存在询问是否覆盖的弹框
     * 增加报错弹出消息的弹框
     */
    fun exportLog(targetFile: File) {
        scope.launch(Dispatchers.GIO) {
            val logFile = getLastModifiedLog()
            GLog.d(TAG, "[exportLog] targetLogFile=${targetFile.absolutePath}, sourceLogFile=${logFile?.absolutePath}")
            logFile?.copy(targetFile)
            _messageDialog.emit(STRINGS.ui.exportFileSuccess)
        }
    }

    private fun getLastModifiedLog(): File? {
        val logDir = File(filesDir)
        if (logDir.exists().not() || logDir.isFile || logDir.listFiles().isNullOrEmpty()) {
            return null
        }
        return logDir.listFiles()
            ?.filter { it.name.startsWith(Configuration.LOG_NAME) }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun stopMemoryMonitor() {
        memoryMonitorJob?.cancel()
    }

    override fun destroy() {
        super.destroy()
        stopMemoryMonitor()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}