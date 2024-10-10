package me.gegenbauer.catspy.log.event

import me.gegenbauer.catspy.concurrency.Event

class FullLogWindowModeChangedEvent(val enabled: Boolean) : Event

class FullLogVisibilityChangedEvent(val isVisible: Boolean) : Event