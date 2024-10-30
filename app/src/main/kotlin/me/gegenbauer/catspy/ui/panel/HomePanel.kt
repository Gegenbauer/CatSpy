package me.gegenbauer.catspy.ui.panel

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.log.ui.tab.DeviceLogGuidancePanel
import me.gegenbauer.catspy.log.ui.tab.DeviceLogMainPanel
import me.gegenbauer.catspy.log.ui.tab.FileLogCommonPanel
import me.gegenbauer.catspy.log.ui.tab.FileLogGuidancePanel
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.view.panel.StatusBar
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.tab.BaseTabPanel
import me.gegenbauer.catspy.view.tab.TabManager
import java.io.File

class HomePanel : BaseTabPanel() {

    override val tag: String = "HomePanel"

    private val fileLogGuidancePanel = FileLogGuidancePanel(::openFile)
    private val androidDeviceGuidePanel = DeviceLogGuidancePanel(::openDeviceLogPanel)

    private val tabManager: TabManager
        get() = contexts.getContext(MainFrame::class.java)!!
    private val statusBar = ServiceManager.getContextService(StatusPanel::class.java)
    private val scope = MainScope()

    override fun onSetup(bundle: Bundle?) {
        layout = TableLayout(
            doubleArrayOf(0.25, FILL, 0.25),
            doubleArrayOf(0.4, PREFERRED, PREFERRED, 0.2)
        )
        add(fileLogGuidancePanel, "1,1")
        add(androidDeviceGuidePanel, "1,2")
    }

    override fun onTabSelected() {
        statusBar.logStatus = StatusBar.LogStatus.NONE
    }

    override fun onTabUnselected() {
        // no-op
    }

    override fun isFileAcceptable(files: List<File>): Boolean {
        return FileLogCommonPanel.isFileAcceptable(files)
    }

    override fun handleFileDrop(files: List<File>) {
        val fileLogCommonPanel = tabManager.addTab(FileLogCommonPanel::class.java)
        fileLogCommonPanel.onOpenFileRequested(files)
    }

    private fun openFile(file: File) {
        val fileLogCommonPanel = tabManager.addTab(FileLogCommonPanel::class.java)
        fileLogCommonPanel.onOpenFileRequested(listOf(file))
    }

    private fun openDeviceLogPanel() {
        tabManager.addTab(DeviceLogMainPanel::class.java)
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

}