package me.gegenbauer.catspy.view.panel

import info.clearthought.layout.TableLayout
import me.gegenbauer.catspy.network.DownloadListener
import me.gegenbauer.catspy.view.button.CloseButton
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.UUID
import javax.swing.*

interface ITask {
    val title: String

    val id: String
    
    var progress: Float

    val taskHandle: TaskHandle

    val taskObservers: Set<TaskObserver>

    fun addTaskObserver(taskObserver: TaskObserver)

    fun removeTaskObserver(taskObserver: TaskObserver)

    fun notifyTaskStarted() {
        taskObservers.toList().forEach { it.onTaskStarted() }
    }

    fun notifyTaskPaused() {
        taskObservers.toList().forEach { it.onTaskPaused() }
    }

    fun notifyTaskResumed() {
        taskObservers.toList().forEach { it.onTaskResumed() }
    }

    fun notifyTaskCancelled() {
        taskObservers.toList().forEach { it.onTaskCancelled() }
    }

    fun notifyTaskFinished() {
        taskObservers.toList().forEach { it.onTaskFinished() }
    }

    fun notifyTaskFailed(e: Throwable) {
        taskObservers.toList().forEach { it.onTaskFailed(e) }
    }

    fun notifyProgressChanged(newProgress: Float) {
        taskObservers.toList().forEach { it.onProgressChanged(newProgress) }
    }
}

open class DownloadListenerTaskWrapper(private val task: ITask) : DownloadListener {
    override fun onDownloadStart() {
        task.notifyTaskStarted()
    }

    override fun onProgressChanged(bytesRead: Long, contentLength: Long) {
        task.notifyProgressChanged(bytesRead.toFloat() / contentLength)
    }

    override fun onDownloadComplete(file: File) {
        task.notifyTaskFinished()
    }

    override fun onDownloadCanceled() {
        task.notifyTaskCancelled()
    }

    override fun onDownloadFailed(e: Throwable) {
        task.notifyTaskFinished()
    }
}

open class Task(
    override val title: String,
    override val taskHandle: TaskHandle,
    override val id: String = UUID.randomUUID().toString(),
) : ITask {
    override var progress: Float = 0f

    override val taskObservers: MutableSet<TaskObserver> = mutableSetOf()

    @Synchronized
    override fun addTaskObserver(taskObserver: TaskObserver) {
        taskObservers.add(taskObserver)
    }

    @Synchronized
    override fun removeTaskObserver(taskObserver: TaskObserver) {
        taskObservers.remove(taskObserver)
    }

    override fun notifyProgressChanged(newProgress: Float) {
        super.notifyProgressChanged(newProgress)
        progress = newProgress
    }

    override fun toString(): String {
        return "Task(name='$title', progress=$progress, taskHandle=$taskHandle, taskObservers=$taskObservers)"
    }
}

interface TaskHandle {
    fun pause() {}

    fun resume() {}

    fun cancel() {}
}

interface TaskObserver {
    fun onTaskStarted() {}

    fun onTaskPaused() {}

    fun onTaskResumed() {}

    fun onTaskCancelled() {}

    fun onTaskFinished() {}

    fun onTaskFailed(e: Throwable) {}

    fun onProgressChanged(newProgress: Float) {}
}

interface TaskMonitor {
    fun addTask(task: ITask)

    fun removeTask(task: ITask)
}

class TaskMonitorPanel : JPanel(), TaskMonitor {
    private val allTasksDialog = JDialog().apply {
        isResizable = true
        defaultCloseOperation = JDialog.HIDE_ON_CLOSE
    }
    private val taskListPanel = TaskListPanel(allTasksDialog)
    private val taskPanels = LinkedHashMap<String, JPanel>()
    private val latestTaskPanel = JPanel(BorderLayout())

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(latestTaskPanel)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                showAllTasks()
            }
        })
    }

    override fun addTask(task: ITask) {
        taskListPanel.addTask(task)
        val taskPanel = createTaskPanel(task)
        taskPanels[task.id] = taskPanel
        updateLatestTaskPanel(taskPanel)
    }

    override fun removeTask(task: ITask) {
        taskListPanel.removeTask(task)
        val removed = taskPanels.remove(task.id)
        removed?.let {
            remove(it)
            updateLatestTaskPanel(taskPanels.values.lastOrNull() ?: JPanel())
        }
    }

    private fun createTaskPanel(task: ITask): JPanel {
        val taskPanel = JPanel(BorderLayout())
        taskPanel.border = BorderFactory.createEmptyBorder(4, 10, 4, 10)
        val nameLabel = JLabel(task.title)
        nameLabel.horizontalAlignment = SwingConstants.RIGHT
        nameLabel.border = BorderFactory.createEmptyBorder(0, 0, 0, 10)
        val progressBar = JProgressBar(0, PROGRESS_BAR_MAX)
        progressBar.value = (PROGRESS_BAR_MAX * task.progress).toInt()
        if (task.progress == 0F) {
            progressBar.isIndeterminate = true
        }
        val cancelButton = CloseButton(task.taskHandle::cancel)

        val layout = TableLayout(
            doubleArrayOf(TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED),
            doubleArrayOf(TableLayout.PREFERRED)
        )
        taskPanel.layout = layout

        taskPanel.add(nameLabel, "0, 0")
        taskPanel.add(progressBar, "1, 0")
        taskPanel.add(cancelButton, "2, 0")

        progressBar.isIndeterminate = true
        task.addTaskObserver(object : TaskObserver {
            override fun onTaskFinished() {
                removeTask(task)
            }

            override fun onTaskCancelled() {
                removeTask(task)
            }

            override fun onTaskFailed(e: Throwable) {
                removeTask(task)
            }

            override fun onProgressChanged(newProgress: Float) {
                if (newProgress > 0) {
                    progressBar.isIndeterminate = false
                }
                progressBar.value = (PROGRESS_BAR_MAX * task.progress).toInt()
            }
        })

        return taskPanel
    }

    private fun updateLatestTaskPanel(taskPanel: JPanel) {
        latestTaskPanel.removeAll()
        latestTaskPanel.add(taskPanel)
        revalidate()
        repaint()
    }

    private fun showAllTasks() {
        allTasksDialog.contentPane = taskListPanel
        allTasksDialog.pack()
        val location = this.locationOnScreen
        location.y -= allTasksDialog.height
        allTasksDialog.location = location
        allTasksDialog.isVisible = true
    }

    companion object {
        private const val PROGRESS_BAR_MAX = 1000
    }
}