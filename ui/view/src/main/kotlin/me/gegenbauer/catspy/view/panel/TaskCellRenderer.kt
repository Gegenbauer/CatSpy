package me.gegenbauer.catspy.view.panel

import me.gegenbauer.catspy.view.button.CloseButton
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

class TaskCellRenderer(private val taskListAccess: TaskListAccess) : ListCellRenderer<ITask> {
    private val panelCache = mutableMapOf<ITask, JPanel>()

    override fun getListCellRendererComponent(
        list: JList<out ITask>?,
        value: ITask?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        return panelCache.getOrPut(value!!) {
            val panel = JPanel(BorderLayout())
            setupPanel(panel, value)
            panel
        }
    }

    fun removeTask(task: ITask) {
        panelCache.remove(task)
    }

    private fun setupPanel(panel: JPanel, value: ITask?) {
        panel.layout = BorderLayout()

        val nameLabel = createNameLabel(value)
        panel.add(nameLabel, BorderLayout.CENTER)

        val rightPanel = createRightPanel(value)
        panel.add(rightPanel, BorderLayout.EAST)

        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    }

    private fun createNameLabel(value: ITask?): JLabel {
        val nameLabel = JLabel(value?.title)
        nameLabel.horizontalAlignment = SwingConstants.LEFT
        return nameLabel
    }

    private fun createRightPanel(value: ITask?): JPanel {
        val rightPanel = JPanel()
        rightPanel.border = BorderFactory.createEmptyBorder(0, 10, 0, 0)
        rightPanel.layout = BoxLayout(rightPanel, BoxLayout.X_AXIS)

        val progressBar = createProgressBar(value)
        rightPanel.add(progressBar)

        val cancelButton = createCloseButton(value)
        rightPanel.add(cancelButton)

        return rightPanel
    }

    private fun createProgressBar(value: ITask?): JProgressBar {
        val progressBar = JProgressBar(0, PROGRESS_BAR_MAX)
        progressBar.value = (PROGRESS_BAR_MAX * (value?.progress ?: 0f)).toInt()
        if (value?.progress == 0F) {
            progressBar.isIndeterminate = true
        }
        return progressBar
    }

    private fun createCloseButton(value: ITask?): CloseButton {
        val cancelButton = CloseButton()
        if (taskListAccess.getHoveredTasks().contains(value)) {
            cancelButton.setHover()
        } else {
            cancelButton.setNormal()
        }
        taskListAccess.getCancelButtons()[value!!] = cancelButton
        return cancelButton
    }

    companion object {
        private const val PROGRESS_BAR_MAX = 1000
    }
}