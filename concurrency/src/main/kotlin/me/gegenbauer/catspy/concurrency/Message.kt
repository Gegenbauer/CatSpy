package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import me.gegenbauer.catspy.java.ext.EMPTY_STRING

sealed class Message {
    abstract val message: String

    data class Info(override val message: String) : Message()
    data class Error(override val message: String) : Message()
    data class Warning(override val message: String) : Message()
    data class Empty(override val message: String = EMPTY_STRING) : Message()
}

fun interface MessagePublisher {
    fun publish(message: Message)
}

fun interface MessageObservable {
    suspend fun collect(action: suspend (Message) -> Unit)
}

object GlobalMessageManager : MessagePublisher, MessageObservable {
    private val _globalMessage: MutableSharedFlow<Message?> = MutableSharedFlow(
        extraBufferCapacity = 20,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun publish(message: Message) {
        _globalMessage.tryEmit(message)
    }

    override suspend fun collect(action: suspend (Message) -> Unit) {
        _globalMessage.collect {
            action(it ?: Message.Empty())
        }
    }
}
