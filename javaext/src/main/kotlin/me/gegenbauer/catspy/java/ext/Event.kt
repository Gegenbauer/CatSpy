package me.gegenbauer.catspy.java.ext

interface Event

object EmptyEvent : Event
class NormalEvent(val message: String) : Event
class ErrorEvent(val error: Throwable) : Event

sealed class FileSaveEvent : Event {
    class FileSaveSuccess(val fileAbsolutePath: String, val title: String, val message: String) : FileSaveEvent()

    class FileSaveError(val title: String, val message: String, val error: Throwable) : FileSaveEvent()
}