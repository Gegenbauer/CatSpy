package me.gegenbauer.catspy.java.ext

interface Event

object EmptyEvent : Event
class NormalEvent(val message: String) : Event
class ErrorEvent(val error: Throwable) : Event
class FileSaveEvent(val fileAbsolutePath: String, val title: String, val message: String) : Event