package me.gegenbauer.catspy.view.panel

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants
import me.gegenbauer.catspy.context.ContextService
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel

class StatusPanel : JPanel(), StatusBar, ContextService, TaskMonitor {

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

    override var statusIcons: List<JComponent> = mutableListOf()
        set(value) {
            field = value
            statusIconsContainer.removeAll()
            for (icon in value) {
                statusIconsContainer.add(icon)
            }
        }

    private val logStatusContainer = LogStatusBar()
    private val backgroundTasksContainer = TaskMonitorPanel()
    private val statusIconsContainer = JPanel()
    private val memoryMonitorContainer = JPanel()

    init {
        val p = TableLayoutConstants.PREFERRED
        layout = TableLayout(
            doubleArrayOf(TableLayoutConstants.FILL, p, p, p),
            doubleArrayOf(p)
        )
        add(logStatusContainer, "0,0")
        add(backgroundTasksContainer, "1,0")
        add(statusIconsContainer, "2,0")
        add(memoryMonitorContainer, "3,0")
    }

    override fun addTask(task: ITask) {
        backgroundTasksContainer.addTask(task)
    }

    override fun removeTask(task: ITask) {
        backgroundTasksContainer.removeTask(task)
    }
}