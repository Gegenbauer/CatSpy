package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

interface Event

object EmptyEvent : Event
class NormalEvent(val message: String) : Event
class ErrorEvent(val error: Throwable) : Event

sealed class FileSaveEvent : Event {
    class FileSaveSuccess(val fileAbsolutePath: String, val title: String, val message: String) : FileSaveEvent()

    class FileSaveError(val title: String, val message: String, val error: Throwable) : FileSaveEvent()
}

object OpenAdbPathSettingsEvent : Event

fun interface EventPublisher {
    fun publish(event: Event)
}

fun interface EventObservable {
    suspend fun collect(action: suspend (Event) -> Unit)
}

object GlobalEventManager: EventPublisher, EventObservable {
    private val _globalEvent: MutableSharedFlow<Event?> = MutableSharedFlow(
        extraBufferCapacity = 20,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun publish(event: Event) {
        _globalEvent.tryEmit(event)
    }

    override suspend fun collect(action: suspend (Event) -> Unit) {
        _globalEvent.collect {
            action(it ?: EmptyEvent)
        }
    }

}