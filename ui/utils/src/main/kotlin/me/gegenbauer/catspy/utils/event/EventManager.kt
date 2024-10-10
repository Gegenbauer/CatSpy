package me.gegenbauer.catspy.utils.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import me.gegenbauer.catspy.concurrency.EmptyEvent
import me.gegenbauer.catspy.concurrency.Event
import me.gegenbauer.catspy.concurrency.EventObservable
import me.gegenbauer.catspy.concurrency.EventPublisher
import me.gegenbauer.catspy.context.ContextService

class EventManager : ContextService, EventPublisher, EventObservable {
    private val _event: MutableSharedFlow<Event?> = MutableSharedFlow(
        extraBufferCapacity = 20,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun publish(event: Event) {
        _event.tryEmit(event)
    }

    override suspend fun collect(action: suspend (Event) -> Unit) {
        _event.collect {
            action(it ?: EmptyEvent)
        }
    }

}