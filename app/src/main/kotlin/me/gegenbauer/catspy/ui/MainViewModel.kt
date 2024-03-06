package me.gegenbauer.catspy.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.configuration.getLastModifiedLog
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
import me.gegenbauer.catspy.network.update.ReleaseEvent
import me.gegenbauer.catspy.network.update.data.Release
import me.gegenbauer.catspy.platform.GlobalProperties.*
import me.gegenbauer.catspy.platform.Platform
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.copyFileWithProgress
import me.gegenbauer.catspy.view.panel.DownloadListenerTaskWrapper
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.panel.Task
import me.gegenbauer.catspy.view.panel.TaskHandle
import java.io.File

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
            val latestReleaseResult = updateService.getLatestRelease()
            if (latestReleaseResult.isFailure) {
                GLog.w(TAG, "[checkUpdate] failed to get latest release, error=${latestReleaseResult.exceptionOrNull()}")
                if (force) {
                    _eventFlow.value = ReleaseEvent.ErrorEvent(latestReleaseResult.exceptionOrNull())
                }
                return@launch
            }
            val latestRelease = latestReleaseResult.getOrThrow()
            GLog.d(TAG, "[checkUpdate] latestRelease=$latestRelease, currentRelease=${Release(APP_VERSION_NAME)}")
            if (updateService.checkForUpdate(latestRelease, Release(APP_VERSION_NAME))) {
                val releaseIgnored = currentSettings.updateSettings.isIgnored(latestRelease.name)
                GLog.i(TAG, "[checkUpdate] releaseIgnored=$releaseIgnored")
                if (force || releaseIgnored.not()) {
                    _eventFlow.value = ReleaseEvent.NewReleaseEvent(latestRelease)
                }
            } else if (force) {
                GLog.i(TAG, "[checkUpdate] no new release")
                _eventFlow.value = ReleaseEvent.NoNewReleaseEvent
            }
        }
    }

    fun startDownloadRelease(release: Release) {
        scope.launch {
            val asset = release.assets.firstOrNull { it.name.contains(currentPlatform.assetKeyword) }
            asset?.let {
                val downloadFileName = it.name
                val downloadPath = filesDir.appendPath(downloadFileName)
                val taskName = STRINGS.ui.downloadTaskTitle.get(release.name)
                val task = Task(taskName, object : TaskHandle {
                    override fun cancel() {
                        updateService.cancelDownload()
                    }
                })
                globalStatus.addTask(task)
                updateService.downloadAsset(it, downloadPath, object : DownloadListenerTaskWrapper(task) {
                    override fun onDownloadComplete(file: File) {
                        super.onDownloadComplete(file)
                        _eventFlow.value = FileSaveEvent.FileSaveSuccess(
                            file.absolutePath,
                            STRINGS.ui.downloadReleaseCompleteDialogTitle,
                            STRINGS.ui.downloadReleaseCompleteDialogMessage
                        )
                    }

                    override fun onDownloadFailed(e: Throwable) {
                        super.onDownloadFailed(e)
                        _eventFlow.value = FileSaveEvent.FileSaveError(
                            STRINGS.ui.downloadReleaseTitle,
                            STRINGS.ui.downloadReleaseFailedMessage,
                            e
                        )
                    }
                })
            }
        }
    }

    suspend fun exportLog(targetFile: File): Result<File?> {
        return withContext(Dispatchers.GIO) {
            val logFile = getLastModifiedLog()
            logFile?.let { sourceFile ->
                GLog.d(TAG, "[exportLog] targetLogFile=${targetFile.absolutePath}, sourceLogFile=${sourceFile.absolutePath}")

                val taskName = STRINGS.ui.exportFileTaskTitle.get(targetFile.absolutePath)
                val task = Task(taskName, object : TaskHandle {
                    override fun cancel() {
                        coroutineContext.job.cancel()
                    }
                })

                globalStatus.addTask(task)

                task.notifyTaskStarted()
                runCatching {
                    copyFileWithProgress(sourceFile, targetFile) { progress ->
                        task.notifyProgressChanged(progress)
                    }
                    GLog.d(TAG, "[exportLog] copyFileWithProgress success")
                    task.notifyTaskFinished()
                    Result.success(targetFile)
                }.onFailure {
                    if (it is CancellationException) {
                        task.notifyTaskCancelled()
                    } else {
                        task.notifyTaskFailed(it)
                    }
                    Result.failure<String>(it)
                }.getOrDefault(Result.failure(Exception(STRINGS.ui.unknownError)))
            } ?: Result.success(null)
        }
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