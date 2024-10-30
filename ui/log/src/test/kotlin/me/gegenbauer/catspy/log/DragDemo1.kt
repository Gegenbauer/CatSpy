import java.awt.BorderLayout
import java.awt.Color
import java.awt.dnd.*
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater { FileDragAndDropWithParent() }
}

class FileDragAndDropWithParent : JFrame() {

    private val parentPanel: JPanel = JPanel(BorderLayout()) // 父布局
    private val childPanel: JPanel = JPanel() // 子布局

    init {
        title = "父布局接管拖拽事件"
        setSize(500, 400)
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)

        // 设置父布局的 UI 和边框
        parentPanel.border = BorderFactory.createTitledBorder("父布局（接管拖拽）")
        parentPanel.background = Color.LIGHT_GRAY

        // 设置子布局的 UI 和边框
        childPanel.border = BorderFactory.createTitledBorder("子布局（实际接收拖拽）")
        childPanel.background = Color.CYAN
        childPanel.isOpaque = true

        // 添加文件拖拽支持到父布局
        DropTarget(parentPanel, object : DropTargetListener {
            override fun dragEnter(dtde: DropTargetDragEvent) {
                if (isDragAcceptable(dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY)
                    parentPanel.background = Color.GREEN // 父布局显示绿色，表示接受拖拽
                    if (isWithinChild(dtde)) {
                        childPanel.background = Color.YELLOW // 子布局变为黄色，表示文件拖拽到子布局
                    }
                } else {
                    dtde.rejectDrag()
                    parentPanel.background = Color.RED // 拒绝时变为红色
                }
            }

            override fun dragOver(dtde: DropTargetDragEvent) {
                // 拖拽经过父布局时
                if (isWithinChild(dtde)) {
                    childPanel.background = Color.YELLOW
                } else {
                    childPanel.background = Color.CYAN
                }
            }

            override fun dropActionChanged(dtde: DropTargetDragEvent) {}

            override fun dragExit(dte: DropTargetEvent) {
                // 恢复原始颜色
                resetLayoutColors()
            }

            override fun drop(dtde: DropTargetDropEvent) {
                // 先由父布局接管，决定是否处理或交给子布局
                if (isDropAcceptable(dtde)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    if (isWithinChild(dtde)) {
                        // 交由子布局处理
                        processDropInChild(dtde)
                    } else {
                        // 父布局处理
                        processDropInParent(dtde)
                    }
                } else {
                    dtde.rejectDrop()
                }
            }

            // 判断拖拽事件是否在子布局内
            private fun isWithinChild(dtde: DropTargetDragEvent): Boolean {
                val point = SwingUtilities.convertPoint(parentPanel, dtde.location, childPanel)
                return point.x >= 0 && point.y >= 0 && point.x <= childPanel.width && point.y <= childPanel.height
            }

            private fun isWithinChild(dtde: DropTargetDropEvent): Boolean {
                val point = SwingUtilities.convertPoint(parentPanel, dtde.location, childPanel)
                return point.x >= 0 && point.y >= 0 && point.x <= childPanel.width && point.y <= childPanel.height
            }

            // 判断拖拽的文件是否接受（文件数目和后缀名）
            private fun isDragAcceptable(dtde: DropTargetDragEvent): Boolean {
                return try {
                    val transferable = dtde.transferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        true
                    } else {
                        false
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    false
                }
            }

            private fun isDropAcceptable(dtde: DropTargetDropEvent): Boolean {
                return isDragAcceptable(DropTargetDragEvent(dtde.dropTargetContext, dtde.location, dtde.dropAction, dtde.sourceActions))
            }

            // 父布局处理文件放下
            private fun processDropInParent(dtde: DropTargetDropEvent) {
                val transferable = dtde.transferable
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                println("父布局处理文件: ${files.map { it.name }}")
                parentPanel.background = Color.BLUE // 成功时显示蓝色
                dtde.dropComplete(true)
            }

            // 子布局处理文件放下
            private fun processDropInChild(dtde: DropTargetDropEvent) {
                val transferable = dtde.transferable
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                println("子布局处理文件: ${files.map {
                    Files.probeContentType(Paths.get(it.absolutePath))
                }}")
                childPanel.background = Color.BLUE // 成功时子布局显示蓝色
                dtde.dropComplete(true)
            }

            // 恢复颜色
            private fun resetLayoutColors() {
                parentPanel.background = Color.LIGHT_GRAY
                childPanel.background = Color.CYAN
            }
        })

        parentPanel.add(childPanel, BorderLayout.CENTER)
        add(parentPanel)
        isVisible = true
    }
}
