package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.java.ext.Bundle
import javax.swing.JPanel

abstract class BaseTabPanel(override val contexts: Contexts = Contexts.default) : JPanel(), TabPanel {

    private var tabNameController: (String) -> Unit = {}
    private var tabTooltipController: (String?) -> Unit = {}

    private var hasSetUp = false

    override fun setup(bundle: Bundle?) {
        if (hasSetUp) return
        onSetup(bundle)
        hasSetUp = true
    }

    abstract fun onSetup(bundle: Bundle?)

    override fun setTabNameController(controller: (String) -> Unit) {
        tabNameController = controller
    }

    override fun setTabTooltipController(controller: (String?) -> Unit) {
        tabTooltipController = controller
    }

    protected fun setTabName(name: String) {
        tabNameController(name)
    }

    protected fun setTabTooltip(tooltip: String?) {
        tabTooltipController(tooltip)
    }
}