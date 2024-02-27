package me.gegenbauer.catspy.java.ext

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope

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


private val _globalEvent: MutableSharedFlow<Event?> = MutableSharedFlow()

val globalEvent: SharedFlow<Event?> = _globalEvent.asSharedFlow()
val globalEventPublisher: EventPublisher = object : EventPublisher {
    override fun publish(event: Event) {
        AppScope.launch {
            _globalEvent.emit(event)
        }
    }
}