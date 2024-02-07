package me.gegenbauer.catspy.view.panel

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants
import me.gegenbauer.catspy.context.ContextService
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel

class StatusPanel(
    private val backgroundTasksContainer: TaskMonitorPanel = TaskMonitorPanel()
) : JPanel(), StatusBar, ContextService, TaskMonitor by backgroundTasksContainer {

    override var logStatus: StatusBar.LogStatus = StatusBar.LogStatus.NONE
        set(value) {
            field = value
            logStatusContainer.setLogStatus(value)
            DarkUIUtil.getParentOfType(JFrame::class.java, this)?.title = value.status
        }

    override var memoryMonitorBar: JComponent = JPanel()
        set(value) {
            field = value
            memoryMonitorContainer.removeAll()
            memoryMonitorContainer.add(value)
        }

    override var statusIcons: StatusIconsBar = StatusIconsBar()

    private val logStatusContainer = LogStatusBar()
    private val memoryMonitorContainer = JPanel()

    init {
        border = BorderFactory.createEmptyBorder(2, 2, 2, 2)

        val p = TableLayoutConstants.PREFERRED
        layout = TableLayout(
            doubleArrayOf(TableLayoutConstants.FILL, p, p, p),
            doubleArrayOf(TableLayoutConstants.FILL)
        )
        add(logStatusContainer, "0,0,f,c")
        add(backgroundTasksContainer, "1,0,f,c")
        add(statusIcons, "2,0,f,c")
        add(memoryMonitorContainer, "3,0,f,c")

        //statusIcons.addStatusIcon(DeviceIcon()) // TODO complete device monitor panel
    }
}