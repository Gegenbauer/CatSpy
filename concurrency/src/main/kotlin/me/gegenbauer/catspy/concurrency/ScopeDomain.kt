package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import me.gegenbauer.catspy.log.GLog
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

private const val TAG = "ScopeDomain"
private const val DISPATCHER_NAME_TRACK = "track"
private const val DISPATCHER_NAME_SINGLE_UN_BUSY = "singleUnBusy"
private const val DISPATCHER_NAME_APP_START = "app_launch"
private const val APP_LAUNCH_CORE_POOL_SIZE = 4

/**
 * 取代 [GlobalScope] 作为全局的作用域，默认使用 [Dispatchers.CPU]
 */
object AppScope : CoroutineScope {
    private const val TAG = "AppScope"
    override val coroutineContext: CoroutineContext
        get() = CoroutineName(TAG) + Dispatchers.CPU + loggingExceptionHandler + SupervisorJob()
}

/**
 * Service 层中的作用域，默认使用 [Dispatchers.CPU]，生命周期随 Service 存在而存在，每个 Service 都有一个对应的 [ServiceScope]
 */
class ServiceScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = CoroutineName(TAG) + Dispatchers.CPU + loggingExceptionHandler + SupervisorJob()

    private companion object {
        private const val TAG = "ServiceScope"
    }
}

/**
 * Model 层中的作用域，默认使用 [Dispatchers.IO]，生命周期随 Model 存在而存在，每个 Model 都有一个对应的 [ModelScope]
 */
open class ModelScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = CoroutineName(TAG) + Dispatchers.IO + loggingExceptionHandler + SupervisorJob()

    private companion object {
        private const val TAG = "ModelScope"
    }
}

/**
 * ViewModel 层中的作用域，默认使用 [Dispatchers.CPU]，生命周期随 ViewModel 存在而存在，每个 ViewModel 都有一个对应的 [ViewModelScope]
 */
class ViewModelScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = CoroutineName(TAG) + Dispatchers.CPU + loggingExceptionHandler + SupervisorJob()

    private companion object {
        private const val TAG = "ViewModelScope"
    }
}

/**
 * 为埋点建立的一个作用域，默认使用 [Dispatchers.TRACK]
 */
object TrackScope : CoroutineScope {
    private const val TAG = "AppScope"
    override val coroutineContext: CoroutineContext
        get() = CoroutineName(TAG) + Dispatchers.CPU + loggingExceptionHandler + SupervisorJob()
}

/**
 * 异常处理的 Handler 句柄， 当不使用此异常处理句柄，而协程中崩溃时，并不会捕获异常，可能导致崩溃。所以必须使用。
 */
val loggingExceptionHandler = CoroutineExceptionHandler { _, t ->
    GLog.e(TAG, "[CoroutineExceptionHandler]", t)
}

/**
 * UI 线程的调度器，取代 [Dispatchers.Main]
 */
val Dispatchers.UI by lazy { Dispatchers.Swing }

/**
 * CPU 密集型任务的调度器，取代 [Dispatchers.Default], 默认线程池数量为 CPU 的核心数
 */
val Dispatchers.CPU by lazy { Dispatchers.Default }

val Dispatchers.GIO by lazy { Dispatchers.IO }

/**
 * 埋点的调度器，启动时阻塞
 */
val Dispatchers.TRACK by lazy {
    Executors.newSingleThreadExecutor(
        PriorityThreadFactory(
            DISPATCHER_NAME_TRACK,
            Thread.MIN_PRIORITY
        )
    ).asCoroutineDispatcher()
}

/**
 * 不繁忙的单线程调度器，使用此调度器尽量不要处理 大量任务、耗时任务
 */
val Dispatchers.SINGLE_UN_BUSY by lazy {
    Executors.newSingleThreadExecutor(
        PriorityThreadFactory(
            DISPATCHER_NAME_SINGLE_UN_BUSY,
            Thread.NORM_PRIORITY
        )
    ).asCoroutineDispatcher()
}

/**
 * 进程启动耗时任务调度器
 */
val Dispatchers.APP_LAUNCH by lazy {
    Executors.newScheduledThreadPool(
        APP_LAUNCH_CORE_POOL_SIZE,
        PriorityThreadFactory(DISPATCHER_NAME_APP_START, Thread.NORM_PRIORITY)
    ).asCoroutineDispatcher()
}

fun <T> Deferred<T>.getOnBlocking() = runBlocking {
    await()
}

fun CoroutineScope.runNotOnUiThread(
    newCoroutineDispatcher: CoroutineDispatcher = Dispatchers.CPU,
    block: () -> Unit
) {
    if (isInMainThread()) {
        launch(newCoroutineDispatcher) {
            block()
        }
    } else {
        block()
    }
}