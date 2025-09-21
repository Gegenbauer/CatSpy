package me.gegenbauer.catspy.ui.panel

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import kotlinx.coroutines.cancel
import me.gegenbauer.catspy.concurrency.UIScope
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.log.ui.tab.DeviceLogGuidancePanel
import me.gegenbauer.catspy.log.ui.tab.DeviceLogMainPanel
import me.gegenbauer.catspy.log.ui.tab.FileLogGuidancePanel
import me.gegenbauer.catspy.log.ui.tab.FileLogMainPanel
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.view.panel.StatusBar
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.tab.BaseTabPanel
import me.gegenbauer.catspy.view.tab.TabManager
import java.io.File
import javax.swing.JPanel
import javax.swing.TransferHandler

class HomePanel : BaseTabPanel() {

    override val tag: String = "HomePanel"

    private val fileLogGuidancePanel = FileLogGuidancePanel(::openFile)
    private val androidDeviceGuidePanel = DeviceLogGuidancePanel(::openDeviceLogPanel)

    private val tabManager: TabManager
        get() = contexts.getContext(MainFrame::class.java)!!
    private val statusBar = ServiceManager.getContextService(StatusPanel::class.java)
    private val scope = UIScope()

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

    override fun getTabContent(): JPanel {
        return this
    }

    override fun isDataImportSupported(info: TransferHandler.TransferSupport): Boolean {
        return true
    }

    override fun handleDataImport(info: TransferHandler.TransferSupport): Boolean {
        val fileLogMainPanel = tabManager.addTab(FileLogMainPanel::class.java)
        val droppedFiles = getDroppedFiles(info)
        fileLogMainPanel.pendingOpenFiles(droppedFiles)
        return true
    }

    private fun openFile(file: File) {
        val fileLogMainPanel = tabManager.addTab(FileLogMainPanel::class.java)
        fileLogMainPanel.pendingOpenFiles(listOf(file))
    }

    private fun openDeviceLogPanel() {
        tabManager.addTab(DeviceLogMainPanel::class.java)
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

}