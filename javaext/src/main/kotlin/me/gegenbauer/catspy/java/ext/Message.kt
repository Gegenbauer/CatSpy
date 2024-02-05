package me.gegenbauer.catspy.java.ext

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope


sealed class Message {
    abstract val message: String

    data class Info(override val message: String) : Message()
    data class Error(override val message: String) : Message()
    data class Warning(override val message: String) : Message()
    data class Empty(override val message: String = "") : Message()
}

interface MessagePublisher {
    fun publish(message: Message)
}


private val _globalMessage: MutableSharedFlow<Message?> = MutableSharedFlow()

val globalMessage: SharedFlow<Message?> = _globalMessage.asSharedFlow()
val globalMessagePublisher: MessagePublisher = object : MessagePublisher {
    override fun publish(message: Message) {
        AppScope.launch {
            _globalMessage.emit(message)
        }
    }
}
