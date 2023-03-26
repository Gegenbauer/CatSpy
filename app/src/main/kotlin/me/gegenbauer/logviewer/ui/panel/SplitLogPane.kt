package me.gegenbauer.logviewer.ui.panel

import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.log.LogPanel
import me.gegenbauer.logviewer.ui.log.LogTableModel
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JSplitPane

class SplitLogPane(
    mainUI: MainUI,
    fullTableModel: LogTableModel,
    filteredTableModel: LogTableModel
    ) : JSplitPane(), FocusListener {

    var onFocusGained: (Boolean) -> Unit = {}

    val fullLogPanel = LogPanel(mainUI, fullTableModel, null, this)
    val filteredLogPanel = LogPanel(mainUI, filteredTableModel, fullLogPanel, this)
    var rotation: Orientation = Orientation.ROTATION_LEFT_RIGHT
        set(value) {
            if (field == value) return
            field = value
            forceRotate(value)
        }

    init {
        continuousLayout = false
        orientation = HORIZONTAL_SPLIT
        add(fullLogPanel, LEFT)
        add(filteredLogPanel, RIGHT)
    }

    fun forceRotate(orientation: Orientation = rotation) {
        remove(filteredLogPanel)
        remove(fullLogPanel)
        when (orientation) {
            Orientation.ROTATION_LEFT_RIGHT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, LEFT)
                add(filteredLogPanel, RIGHT)
                resizeWeight = SPLIT_WEIGHT
            }

            Orientation.ROTATION_TOP_BOTTOM -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, TOP)
                add(filteredLogPanel, BOTTOM)
                resizeWeight = SPLIT_WEIGHT
            }

            Orientation.ROTATION_RIGHT_LEFT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, RIGHT)
                add(filteredLogPanel, LEFT)
                resizeWeight = 1 - SPLIT_WEIGHT
            }

            Orientation.ROTATION_BOTTOM_TOP -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, BOTTOM)
                add(filteredLogPanel, TOP)
                resizeWeight = 1 - SPLIT_WEIGHT
            }
        }
    }

    fun rotate(orientation: Orientation = rotation.next()) {
        rotation = orientation
    }

    override fun focusGained(e: FocusEvent) {
        onFocusGained.invoke(e.source == filteredLogPanel)
    }

    override fun focusLost(e: FocusEvent) {
        // do nothing
    }

    companion object {
        private const val SPLIT_WEIGHT = 0.7
    }

}

enum class Orientation {
    ROTATION_LEFT_RIGHT,
    ROTATION_TOP_BOTTOM,
    ROTATION_RIGHT_LEFT,
    ROTATION_BOTTOM_TOP,
}

fun Orientation.next(): Orientation {
    return when (this) {
        Orientation.ROTATION_LEFT_RIGHT -> {
            Orientation.ROTATION_TOP_BOTTOM
        }
        Orientation.ROTATION_TOP_BOTTOM -> {
            Orientation.ROTATION_RIGHT_LEFT
        }
        Orientation.ROTATION_RIGHT_LEFT -> {
            Orientation.ROTATION_BOTTOM_TOP
        }
        Orientation.ROTATION_BOTTOM_TOP -> {
            Orientation.ROTATION_LEFT_RIGHT
        }
    }
}