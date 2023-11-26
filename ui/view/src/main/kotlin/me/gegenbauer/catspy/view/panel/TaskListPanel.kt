package me.gegenbauer.catspy.view.panel

import me.gegenbauer.catspy.view.button.CloseButton
import me.gegenbauer.catspy.view.list.ListNoneSelectionModel
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.*

class TaskListPanel(private val dialog: JDialog) : JPanel(), TaskObserver, TaskListAccess {
    private val taskListModel = DefaultListModel<ITask>()
    private val hoveredIconTasks = mutableSetOf<ITask>()
    private val renderer = TaskCellRenderer(this)

    private val taskList = object : JList<ITask>(taskListModel) {
        init {
            selectionModel = ListNoneSelectionModel()
        }
    }
    private val cancelButtons = mutableMapOf<ITask, CloseButton>()

    init {
        layout = BorderLayout()
        taskList.cellRenderer = renderer

        val scrollPane = JScrollPane(taskList)
        scrollPane.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        add(scrollPane, BorderLayout.CENTER)

        taskList.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val index = taskList.locationToIndex(e.point)
                if (index != -1) {
                    val task = taskListModel.getElementAt(index)
                    val offsetButtonBounds = calculateOffsetButtonBounds(index, task)
                    if (offsetButtonBounds?.contains(e.point) == true) {
                        hoveredIconTasks.add(task)
                    } else {
                        hoveredIconTasks.remove(task)
                    }
                    taskList.repaint()
                }
            }
        })
        taskList.addMouseListener(object : MouseAdapter() {
            override fun mouseExited(e: MouseEvent) {
                hoveredIconTasks.clear()
                taskList.repaint()
            }

            override fun mouseClicked(e: MouseEvent) {
                val index = taskList.locationToIndex(e.point)
                if (index != -1) {
                    val task = taskListModel.getElementAt(index)
                    val offsetButtonBounds = calculateOffsetButtonBounds(index, task)
                    if (offsetButtonBounds?.contains(e.point) == true) {
                        task.taskHandle.cancel()
                    }
                }
            }
        })
    }

    override fun getHoveredTasks(): Set<ITask> {
        return hoveredIconTasks
    }

    override fun getCancelButtons(): MutableMap<ITask, CloseButton> {
        return cancelButtons
    }

    override fun calculateOffsetButtonBounds(index: Int, task: ITask): Rectangle? {
        val buttonBounds = cancelButtons[task]?.bounds
        val cellBounds = taskList.getCellBounds(index, index)
        return buttonBounds?.let {
            it.x += cellBounds.x
            it.y += cellBounds.y
            it
        }
    }

    override fun onProgressChanged(newProgress: Float) {
        taskList.repaint()
    }

    override fun onTaskPaused() {
        taskList.repaint()
    }

    override fun onTaskResumed() {
        taskList.repaint()
    }

    fun addTask(task: ITask) {
        taskListModel.addElement(task)
        task.addTaskObserver(this)
        dialog.pack()
    }

    fun removeTask(task: ITask) {
        taskListModel.removeElement(task)
        cancelButtons.remove(task)
        renderer.removeTask(task)
        if (taskListModel.isEmpty) {
            dialog.isVisible = false
        } else {
            dialog.pack()
        }
    }
}

interface TaskListAccess {
    fun getHoveredTasks(): Set<ITask>
    fun getCancelButtons(): MutableMap<ITask, CloseButton>
    fun calculateOffsetButtonBounds(index: Int, task: ITask): Rectangle?
}