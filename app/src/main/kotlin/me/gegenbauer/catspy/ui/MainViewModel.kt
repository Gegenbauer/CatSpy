package me.gegenbauer.catspy.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.Event
import me.gegenbauer.catspy.concurrency.FileSaveEvent
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.configuration.getLastModifiedLog
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.Memory
import me.gegenbauer.catspy.context.MemoryState
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.parse.ParserManager
import me.gegenbauer.catspy.network.update.GithubUpdateServiceFactory
import me.gegenbauer.catspy.network.update.ReleaseEvent
import me.gegenbauer.catspy.network.update.data.Release
import me.gegenbauer.catspy.platform.GlobalProperties.*
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.file.copyFileWithProgress
import me.gegenbauer.catspy.utils.persistence.Preferences
import me.gegenbauer.catspy.view.panel.DownloadListenerTaskWrapper
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.panel.Task
import me.gegenbauer.catspy.view.panel.TaskHandle
import java.io.File

class MainViewModel(override val contexts: Contexts = Contexts.default) : Context, ContextService {
    val eventFlow: SharedFlow<Event>
        get() = _eventFlow

    private val _eventFlow = MutableSharedFlow<Event>(
        extraBufferCapacity = 30,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val scope = ViewModelScope()
    private val memoryMonitor: IMemoryMonitor = MemoryMonitor()
    private val updateService = GithubUpdateServiceFactory.create(APP_AUTHOR, APP_REPO)
    private val globalStatus = ServiceManager.getContextService(StatusPanel::class.java)

    private var memoryMonitorJob: Job? = null

    fun refreshMemoryInfo() {
        scope.launch {
            _eventFlow.emit(memoryMonitor.calculateMemoryUsage())
        }
    }

    fun startMemoryMonitor() {
        GLog.d(TAG, "[startMemoryMonitor]")
        memoryMonitorJob = scope.launch {
            val memory = memoryMonitor.startMonitor()
            memory.collect {
                _eventFlow.emit(it)
                checkMemoryState(it)
            }
        }
    }

    private fun checkMemoryState(memory: Memory) {
        MemoryState.onMemoryChanged(memory)
    }

    fun checkUpdate(force: Boolean = false) {
        val lastCheckTime = Preferences.getLong(CHECK_UPDATE_TIME_KEY, 0)
        if (!force && System.currentTimeMillis() - lastCheckTime < CHECK_UPDATE_INTERVAL) return
        scope.launch {
            val latestReleaseResult = updateService.getLatestRelease()
            if (latestReleaseResult.isFailure) {
                GLog.w(
                    TAG,
                    "[checkUpdate] failed to get latest release, error=${latestReleaseResult.exceptionOrNull()}"
                )
                if (force) {
                    _eventFlow.emit(ReleaseEvent.ErrorEvent(latestReleaseResult.exceptionOrNull()))
                }
                return@launch
            }
            val latestRelease = latestReleaseResult.getOrThrow()
            Preferences.putLong(CHECK_UPDATE_TIME_KEY, System.currentTimeMillis())
            GLog.d(TAG, "[checkUpdate] latestRelease=$latestRelease, currentRelease=${Release(APP_VERSION_NAME)}")
            if (updateService.checkForUpdate(latestRelease, Release(APP_VERSION_NAME))) {
                val releaseIgnored = Preferences.getStringList(IGNORED_RELEASES_KEY).contains(latestRelease.name)
                GLog.i(TAG, "[checkUpdate] releaseIgnored=$releaseIgnored")
                if (force || releaseIgnored.not()) {
                    _eventFlow.emit(ReleaseEvent.NewReleaseEvent(latestRelease))
                }
            } else if (force) {
                GLog.i(TAG, "[checkUpdate] no new release")
                _eventFlow.emit(ReleaseEvent.NoNewReleaseEvent)
            }
        }
    }

    fun startDownloadRelease(release: Release) {
        scope.launch {
            val asset = release.assets.firstOrNull { it.name.contains(ARTIFACT_TYPE) }
            asset?.let {
                val downloadFileName = it.name
                val downloadPath = filesDir.appendPath(DOWNLOAD_DIR).appendPath(downloadFileName)
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
                        _eventFlow.tryEmit(
                            FileSaveEvent.FileSaveSuccess(
                                file.absolutePath,
                                STRINGS.ui.downloadReleaseCompleteDialogTitle,
                                STRINGS.ui.downloadReleaseCompleteDialogMessage
                            )
                        )
                    }

                    override fun onDownloadFailed(e: Throwable) {
                        super.onDownloadFailed(e)
                        _eventFlow.tryEmit(
                            FileSaveEvent.FileSaveError(
                                STRINGS.ui.downloadReleaseTitle,
                                STRINGS.ui.downloadReleaseFailedMessage,
                                e
                            )
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
                GLog.d(
                    TAG,
                    "[exportLog] targetLogFile=${targetFile.absolutePath}, sourceLogFile=${sourceFile.absolutePath}"
                )

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

    fun loadData() {
        scope.launch {
            listOf(
                async {
                    val logMetadataManager = ServiceManager.getContextService(LogMetadataManager::class.java)
                    logMetadataManager.loadAllMetadata()
                },
                async {
                    val parserManager = ServiceManager.getContextService(ParserManager::class.java)
                    parserManager.loadParsers()
                }
            ).awaitAll()
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

        private const val DOWNLOAD_DIR = "downloads"
        private const val CHECK_UPDATE_TIME_KEY = "update/checkUpdateTime"
        const val IGNORED_RELEASES_KEY = "update/ignoredReleases"
        private const val CHECK_UPDATE_INTERVAL = 1000L * 60L * 60 * 24
    }
}