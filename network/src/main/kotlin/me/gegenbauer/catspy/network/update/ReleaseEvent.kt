package me.gegenbauer.catspy.network.update

import me.gegenbauer.catspy.java.ext.Event
import me.gegenbauer.catspy.network.update.data.Release

sealed class ReleaseEvent: Event {
    class NewReleaseEvent(val release: Release): ReleaseEvent()

    object NoNewReleaseEvent : ReleaseEvent()

    class ErrorEvent(val error: Throwable?): ReleaseEvent()
}