package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.view.hint.HintManager
import me.gegenbauer.catspy.view.panel.FileDropHandler
import java.io.File
import javax.swing.JPanel

interface TabPanel : Context, FileDropHandler {

    val tag: String

    val hint: HintManager.Hint?
        get() = null

    var isTabSelected: Boolean

    fun setup(bundle: Bundle?)

    fun setTabNameController(controller: (String) -> Unit)

    fun setTabTooltipController(controller: (String?) -> Unit)

    fun getTabContent(): JPanel {
        require(this is JPanel) { "TabPanel must be a JPanel" }
        return this
    }

    fun onOpenFileRequested(files: List<File>) {
        // no-op
    }
}