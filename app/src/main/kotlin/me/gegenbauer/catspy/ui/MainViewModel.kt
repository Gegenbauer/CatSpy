package me.gegenbauer.catspy.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.EmptyEvent
import me.gegenbauer.catspy.java.ext.Event
import me.gegenbauer.catspy.java.ext.FileSaveEvent
import me.gegenbauer.catspy.network.update.GithubUpdateServiceFactory
import me.gegenbauer.catspy.network.update.data.Release
import me.gegenbauer.catspy.platform.GlobalProperties.*
import me.gegenbauer.catspy.platform.Platform
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.strings.GlobalStrings
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.copyWithProgress
import me.gegenbauer.catspy.view.panel.DownloadListenerTaskWrapper
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.panel.Task
import me.gegenbauer.catspy.view.panel.TaskHandle
import java.io.File
import java.util.*

class MainViewModel(override val contexts: Contexts = Contexts.default) : Context, ContextService {
    val eventFlow: StateFlow<Event>
        get() = _eventFlow

    private val _eventFlow = MutableStateFlow<Event>(EmptyEvent)
    private val scope = ViewModelScope()
    private val memoryMonitor: IMemoryMonitor = MemoryMonitor()
    private val updateService = GithubUpdateServiceFactory.create(APP_AUTHOR, APP_REPO)
    private val globalStatus = ServiceManager.getContextService(StatusPanel::class.java)

    private var memoryMonitorJob: Job? = null

    fun refreshMemoryInfo() {
        scope.launch {
            _eventFlow.value = memoryMonitor.calculateMemoryUsage()
        }
    }

    fun startMemoryMonitor() {
        GLog.d(TAG, "[startMemoryMonitor]")
        memoryMonitorJob = scope.launch {
           val memory = memoryMonitor.startMonitor()
            memory.collect {
                _eventFlow.value = it
            }
        }
    }

    fun checkUpdate(force: Boolean = false) {
        scope.launch {
            val latestRelease = updateService.getLatestRelease()
            if (updateService.checkForUpdate(latestRelease, Release(APP_VERSION_NAME))) {
                if (force || SettingsManager.settings.ignoredRelease.contains(latestRelease.name).not()) {
                    _eventFlow.value = latestRelease
                }
            }
        }
    }

    fun startDownloadRelease(release: Release) {
        scope.launch {
            val asset = release.assets.firstOrNull { it.name.contains(currentPlatform.assetKeyword) }
            asset?.let {
                val downloadFileName = "${APP_NAME.lowercase(Locale.getDefault())}_${release.name}"
                val downloadPath = filesDir.appendPath(downloadFileName)
                val taskName = String.format(STRINGS.ui.downloadTaskTitle, release.name)
                val task = Task(taskName, object : TaskHandle {
                    override fun cancel() {
                        updateService.cancelDownload()
                    }
                })
                globalStatus.addTask(task)
                updateService.downloadAsset(it, downloadPath, object : DownloadListenerTaskWrapper(task) {
                    override fun onDownloadComplete(file: File) {
                        super.onDownloadComplete(file)
                        _eventFlow.value = FileSaveEvent(
                            file.absolutePath,
                            STRINGS.ui.downloadReleaseCompleteDialogTitle,
                            STRINGS.ui.downloadReleaseCompleteDialogMessage
                        )
                    }
                })
            }
        }
    }

    suspend fun exportLog(targetFile: File) {
        withContext(Dispatchers.GIO) {
            val logFile = getLastModifiedLog()
            logFile?.let { sourceFile ->
                GLog.d(TAG, "[exportLog] targetLogFile=${targetFile.absolutePath}, sourceLogFile=${sourceFile.absolutePath}")

                val taskName = String.format(STRINGS.ui.exportFileTaskTitle, targetFile.absolutePath)
                val task = Task(taskName, object : TaskHandle {
                    override fun cancel() {
                        coroutineContext.job.cancel()
                    }
                })

                globalStatus.addTask(task)

                task.notifyTaskStarted()
                runCatching {
                    copyWithProgress(sourceFile, targetFile) { progress ->
                        task.notifyProgressChanged(progress)
                    }
                }.onSuccess {
                    task.notifyTaskFinished()
                }.onFailure {
                    if (it is CancellationException) {
                        task.notifyTaskCancelled()
                    } else {
                        task.notifyTaskFailed(it)
                    }
                }
            }
        }
    }

    private fun getLastModifiedLog(): File? {
        val logDir = File(filesDir)
        if (logDir.exists().not() || logDir.isFile || logDir.listFiles().isNullOrEmpty()) {
            return null
        }
        return logDir.listFiles()
            ?.filter { it.name.startsWith(GlobalStrings.LOG_NAME) }
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

val Platform.assetKeyword: String
    get() = when (this) {
        Platform.WINDOWS -> "msi"
        Platform.LINUX -> "deb"
        Platform.MAC -> "dmg"
        else -> throw IllegalArgumentException("Unsupported platform: $this")
    }