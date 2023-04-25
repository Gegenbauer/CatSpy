package me.gegenbauer.logviewer.databinding.property.support

import java.awt.event.FocusEvent
import java.awt.event.FocusListener

open class DefaultFocusListener : FocusListener {

    override fun focusGained(e: FocusEvent) {
        focusChanged(e)
    }

    override fun focusLost(e: FocusEvent) {
        focusChanged(e)
    }

    open fun focusChanged(e: FocusEvent) {
        // no-op
    }
}