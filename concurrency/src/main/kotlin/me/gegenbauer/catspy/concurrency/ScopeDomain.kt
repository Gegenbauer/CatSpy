package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import me.gegenbauer.catspy.glog.GLog
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

private const val TAG = "ScopeDomain"
private const val DISPATCHER_NAME_TRACK = "track"
private const val DISPATCHER_NAME_SINGLE_UN_BUSY = "singleUnBusy"
private const val DISPATCHER_NAME_APP_START = "app_launch"
private const val DISPATCHER_NAME_SERIAL = "serial"
private const val APP_LAUNCH_CORE_POOL_SIZE = 4

/**
 * replace [GlobalScope] as global scope. Use [Dispatchers.CPU] by default
 */
object AppScope : CoroutineScope {
    private const val TAG = "AppScope"
    override val coroutineContext: CoroutineContext
        = CoroutineName(TAG) + Dispatchers.CPU + loggingExceptionHandler + SupervisorJob()
}

/**
 * Scope of Service. Use [Dispatchers.CPU] as default. Its lifecycle is as long as service.
 */
class ServiceScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        = CoroutineName(TAG) + Dispatchers.CPU + loggingExceptionHandler + SupervisorJob()

    private companion object {
        private const val TAG = "ServiceScope"
    }
}

/**
 * Scope of Model. Use [Dispatchers.GIO] as default. Its lifecycle is as long as Model.
 */
open class ModelScope : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        CoroutineName(TAG) + Dispatchers.GIO + loggingExceptionHandler + SupervisorJob()

    private companion object {
        private const val TAG = "ModelScope"
    }
}

/**
 * Scope of ViewModel. Use [Dispatchers.UI] as default. Its lifecycle is as long as ViewModel.
 */
class ViewModelScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        = CoroutineName(TAG) + Dispatchers.UI + loggingExceptionHandler + SupervisorJob()

    private companion object {
        private const val TAG = "ViewModelScope"
    }
}

/**
 * A scope created for the statics, default [Dispatchers.TRACK]
 */
object TrackScope : CoroutineScope {
    private const val TAG = "AppScope"
    override val coroutineContext: CoroutineContext
        = CoroutineName(TAG) + Dispatchers.CPU + loggingExceptionHandler + SupervisorJob()
}

/**
 * Exception handling handler. When this exception handling handler is not used, and a coroutine crashes,
 * the exception will not be caught, which may lead to a crash. Therefore, it must be used.
 */
val loggingExceptionHandler = CoroutineExceptionHandler { _, t ->
    GLog.e(TAG, "[CoroutineExceptionHandler]", t)
}

/**
 * UI thread dispatcher, replacing [Dispatchers.Main]
 */
val Dispatchers.UI by lazy { Dispatchers.Swing }

/**
 * CPU-intensive task dispatcher, replacing [Dispatchers.Default],
 * with the default thread pool size equal to the number of CPU cores.
 */
val Dispatchers.CPU by lazy { Dispatchers.Default }

val Dispatchers.GIO by lazy { Dispatchers.IO }

/**
 * Blocking dispatcher for tracking points during startup.
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
 * Non-busy single-threaded dispatcher, avoid using this dispatcher for handling large or time-consuming tasks.
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
 * Dispatcher for time-consuming tasks during process startup.
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

fun getSerialDispatcher(name: String = DISPATCHER_NAME_SERIAL): CoroutineDispatcher {
    return Executors.newSingleThreadExecutor(
        PriorityThreadFactory(
            name,
            Thread.NORM_PRIORITY
        )
    ).asCoroutineDispatcher()
}