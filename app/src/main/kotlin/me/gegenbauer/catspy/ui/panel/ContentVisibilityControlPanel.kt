package me.gegenbauer.catspy.ui.panel

import me.gegenbauer.catspy.databinding.bind.*
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.databinding.property.support.visibilityProperty
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.filter.StorableValueProperty
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.IconBarToggleButton
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToggleButton

interface ContentVisibilityController {
    /**
     * Bind the controller to a new panel with a specific tag.
     * Tag should be unique for each panel to avoid conflicts.
     * Tag will be used to save the state of the panel in the controller.
     */
    fun bindToPanel(panel: JPanel, tag: String)

    fun unbindFromPanel(panel: JPanel)
}

class ContentVisibilityControlPanel : JPanel(), ContentVisibilityController {
    private val controllers = mutableMapOf<JPanel, ObservableValueProperty<Boolean>>()
    private val componentProperties = mutableMapOf<JPanel, ObservableComponentProperty<Boolean>>()
    private val toggleButtons = mutableMapOf<JPanel, JToggleButton>()

    override fun bindToPanel(panel: JPanel, tag: String) {
        require(controllers.containsKey(panel).not()) {
            "Panel is already bound to this controller"
        }
        val visibilityProperty = StorableValueProperty(tag, true)
        visibilityProperty(panel) bindLeft visibilityProperty
        controllers[panel] = visibilityProperty
        val button = createToggleButton(panel)
        toggleButtons[panel] = button
    }

    override fun unbindFromPanel(panel: JPanel) {
        require(componentProperties.containsKey(panel)) {
            "Panel is not bound to this controller"
        }
        val componentProperty = componentProperties[panel]!!
        Bindings.unBind(componentProperty)
        controllers.remove(panel)
        componentProperties.remove(panel)
    }

    fun createToggleButton(panel: JPanel): JToggleButton {
        require(controllers.containsKey(panel)) {
            "Panel must be bound to this controller before creating a toggle button"
        }
        val visibilityProperty = controllers[panel]!!
        val toggleButton = IconBarToggleButton(
            GIcons.State.ScrollEnd.get(20, 20),
            GIcons.State.ScrollEnd.selected(20, 20),
        )
        selectedProperty(toggleButton) bindDual visibilityProperty
        //
        return toggleButton
    }

    companion object {
        private const val TAG = "ContentVisibilityControlPanel"
    }
}