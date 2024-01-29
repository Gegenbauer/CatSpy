package me.gegenbauer.catspy.log.ui.panel

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.configuration.Rotation
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.view.state.ListState
import me.gegenbauer.catspy.view.state.StatefulPanel
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JSplitPane
import javax.swing.SwingUtilities

class SplitLogPane(
    fullTableModel: LogTableModel,
    filteredTableModel: LogTableModel,
    val onFocusGained: (Boolean) -> Unit = {},
    override val contexts: Contexts = Contexts.default
) : JSplitPane(), FocusListener, Context {

    val fullLogPanel = FullLogPanel(fullTableModel, this)
    val filteredLogPanel = FilteredLogPanel(filteredTableModel, this, fullLogPanel)
    private val filterStatefulPanel = StatefulPanel()
    private var rotation: Rotation = Rotation.ROTATION_LEFT_RIGHT
        set(value) {
            field = value
            changeRotation(value)
        }
    private val scope = MainScope()

    init {
        continuousLayout = true
        orientation = HORIZONTAL_SPLIT

        filterStatefulPanel.setContent(filteredLogPanel)
        filterStatefulPanel.listState = ListState.NORMAL
        scope.launch {
            filteredTableModel.viewModel.filteredLogListState.collect {
                filterStatefulPanel.listState =
                    if (it == ListState.LOADING) ListState.LOADING else ListState.NORMAL
            }
        }

        add(fullLogPanel, LEFT)
        add(filterStatefulPanel, RIGHT)
    }

    private fun changeRotation(rotation: Rotation) {
        remove(filterStatefulPanel)
        remove(fullLogPanel)
        when (rotation) {
            Rotation.ROTATION_LEFT_RIGHT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, LEFT)
                add(filterStatefulPanel, RIGHT)
                resizeWeight = SPLIT_WEIGHT
            }

            Rotation.ROTATION_TOP_BOTTOM -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, TOP)
                add(filterStatefulPanel, BOTTOM)
                resizeWeight = SPLIT_WEIGHT
            }

            Rotation.ROTATION_RIGHT_LEFT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, RIGHT)
                add(filterStatefulPanel, LEFT)
                resizeWeight = 1 - SPLIT_WEIGHT
            }

            Rotation.ROTATION_BOTTOM_TOP -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, BOTTOM)
                add(filterStatefulPanel, TOP)
                resizeWeight = 1 - SPLIT_WEIGHT
            }
        }
        SwingUtilities.updateComponentTreeUI(this)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        filteredLogPanel.setParent(this)
        fullLogPanel.setParent(this)
    }

    fun resetWithCurrentRotation() {
        changeRotation(rotation)
    }

    override fun destroy() {
        super.destroy()
        filteredLogPanel.destroy()
        fullLogPanel.destroy()
        scope.cancel()
    }

    override fun focusGained(e: FocusEvent) {
        onFocusGained.invoke(e.source == filteredLogPanel.table)
    }

    override fun focusLost(e: FocusEvent) {
        // do nothing
    }

    companion object {
        private const val TAG = "SplitLogPane"
        private const val SPLIT_WEIGHT = 0.7
    }

}

fun Rotation.nextRotation(): Rotation {
    return when (this) {
        Rotation.ROTATION_LEFT_RIGHT -> {
            Rotation.ROTATION_TOP_BOTTOM
        }

        Rotation.ROTATION_TOP_BOTTOM -> {
            Rotation.ROTATION_RIGHT_LEFT
        }

        Rotation.ROTATION_RIGHT_LEFT -> {
            Rotation.ROTATION_BOTTOM_TOP
        }

        Rotation.ROTATION_BOTTOM_TOP -> {
            Rotation.ROTATION_LEFT_RIGHT
        }
    }
}