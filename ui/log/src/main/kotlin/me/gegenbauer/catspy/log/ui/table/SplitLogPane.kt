package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.configuration.Rotation
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.event.FullLogVisibilityChangedEvent
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.utils.event.EventManager
import me.gegenbauer.catspy.view.state.ListState
import me.gegenbauer.catspy.view.state.StatefulPanel
import javax.swing.JSplitPane
import javax.swing.SwingUtilities

class SplitLogPane(
    fullTableModel: LogTableModel,
    filteredTableModel: LogTableModel,
    override val contexts: Contexts = Contexts.default
) : JSplitPane(), Context {

    val fullLogPanel = FullLogPanel(fullTableModel)
    val filteredLogPanel = FilteredLogPanel(filteredTableModel, fullLogPanel)
    var rotation: Rotation = Rotation.ROTATION_LEFT_RIGHT
        set(value) {
            field = value
            changeRotation(value)
        }
    private val filterStatefulPanel = StatefulPanel()
    private val scope = MainScope()
    private val eventManager: EventManager
        get() = kotlin.run {
            val logMainPanel = contexts.getContext(BaseLogMainPanel::class.java)
                ?: error("No BaseLogMainPanel found in contexts")
            ServiceManager.getContextService(logMainPanel, EventManager::class.java)
        }

    private val logConf: LogConfiguration
        get() = contexts.getContext(LogConfiguration::class.java)
            ?: throw IllegalStateException("No LogConfiguration found in contexts")

    init {
        continuousLayout = true
        orientation = HORIZONTAL_SPLIT

        filterStatefulPanel.setContent(filteredLogPanel)
        filterStatefulPanel.listState = ListState.NORMAL
        observeListState(filteredTableModel)
        observeFullPanelVisibility()

        add(fullLogPanel, LEFT)
        add(filterStatefulPanel, RIGHT)
    }

    private fun observeListState(filteredTableModel: LogTableModel) {
        scope.launch {
            filteredTableModel.logObservables.listState.collect {
                filterStatefulPanel.listState =
                    if (it == ListState.LOADING) ListState.LOADING else ListState.NORMAL
            }
        }
    }

    private fun observeFullPanelVisibility() {
        scope.launch {
            eventManager.collect {
                if (it is FullLogVisibilityChangedEvent) {
                    if (it.isVisible) {
                        setDividerLocation(0.2)
                        resetWithCurrentRotation()
                    } else {
                        remove(fullLogPanel)
                    }
                }
            }
        }
    }

    fun detachFullLogPanel() {
        if (fullLogPanel.isWindowedMode) {
            return
        }
        fullLogPanel.isWindowedMode = true
        remove(fullLogPanel)
        filteredLogPanel.binding.showFullLogBtnEnabled.updateValue(false)
        SwingUtilities.updateComponentTreeUI(this)
    }

    fun attachFullLogPanel() {
        filteredLogPanel.binding.showFullLogBtnEnabled.updateValue(true)
        fullLogPanel.isWindowedMode = false
        resetWithCurrentRotation()
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

        filterStatefulPanel.isVisible = logConf.isPreviewMode.not()
    }

    private fun resetWithCurrentRotation() {
        changeRotation(rotation)
    }

    override fun destroy() {
        super.destroy()
        filteredLogPanel.destroy()
        fullLogPanel.destroy()
        scope.cancel()
    }

    companion object {
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
