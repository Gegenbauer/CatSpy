package me.gegenbauer.catspy.log.event

import me.gegenbauer.catspy.concurrency.Event
import java.awt.Component

class FullLogWindowModeChangedEvent(val enabled: Boolean) : Event

class FullLogVisibilityChangedEvent(val isVisible: Boolean) : Event

class CancelLoadingTaskEvent(val source: Component? = null) : Event