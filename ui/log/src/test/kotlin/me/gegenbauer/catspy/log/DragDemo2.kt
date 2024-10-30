import java.awt.BorderLayout
import java.awt.Color
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater { FileDragAndDropWithUIFeedback() }
}

// 扩展接口，增加文件是否可接受的判断
interface FileDropHandler {
    fun handleFileDrop(files: List<File>)
    fun isFileAcceptable(files: List<File>): Boolean
}

// 父布局负责接管拖拽，并分发事件到实现了 FileDropHandler 的子布局
class FileDragAndDropWithUIFeedback : JFrame() {

    private val parentPanel: JPanel = JPanel(BorderLayout()) // 父布局
    private val childPanel: FileDropPanel = FileDropPanel() // 子布局，实现了 FileDropHandler

    init {
        title = "拖拽反馈示例"
        setSize(500, 400)
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)

        // 父布局的 UI
        parentPanel.border = BorderFactory.createTitledBorder("父布局")
        parentPanel.background = Color.LIGHT_GRAY

        // 子布局的 UI
        childPanel.border = BorderFactory.createTitledBorder("子布局")
        childPanel.background = Color.CYAN

        // 父布局设置 TransferHandler 来处理拖拽
        parentPanel.transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferHandler.TransferSupport): Boolean {
                // 仅接受文件拖拽
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val transferable = support.transferable
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>

                    // 判断拖拽是否在子布局范围内，并交由子布局判断是否接受文件
                    if (isWithinChild(support.component.location)) {
                        return if (childPanel.isFileAcceptable(files)) {
                            childPanel.onDragAccept()  // 文件可接受，改变子布局UI
                            true
                        } else {
                            childPanel.onDragReject()  // 文件不可接受，改变子布局UI
                            false
                        }
                    }
                }
                resetChildPanelUI()
                return false
            }

            override fun importData(support: TransferHandler.TransferSupport): Boolean {
                // 父布局统一处理拖拽事件
                return if (support.isDrop) {
                    try {
                        val transferable = support.transferable
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>

                        // 判断拖拽是否在子布局范围内，并分发到子布局
                        if (isWithinChild(support.component.location)) {
                            if (childPanel.isFileAcceptable(files)) {
                                childPanel.handleFileDrop(files)
                            } else {
                                showRejectedFileMessage()
                            }
                        } else {
                            handleFileDropInParent(files)
                        }
                        resetChildPanelUI()
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                } else {
                    false
                }
            }

            // 判断是否在子布局范围内
            private fun isWithinChild(dropLocation: java.awt.Point): Boolean {
                val convertedPoint = SwingUtilities.convertPoint(parentPanel, dropLocation, childPanel)
                return convertedPoint.x >= 0 && convertedPoint.y >= 0 &&
                        convertedPoint.x <= childPanel.width && convertedPoint.y <= childPanel.height
            }

            // 父布局处理文件
            private fun handleFileDropInParent(files: List<File>) {
                println("父布局处理文件: ${files.map { it.name }}")
                parentPanel.background = Color.BLUE // 拖拽成功，变为蓝色
            }

            // 拒绝文件时显示提示信息
            private fun showRejectedFileMessage() {
                JOptionPane.showMessageDialog(this@FileDragAndDropWithUIFeedback,
                    "文件类型不符合要求", "文件不被接受", JOptionPane.ERROR_MESSAGE)
            }

            // 重置子布局的UI
            private fun resetChildPanelUI() {
                childPanel.resetUI()
            }
        }

        parentPanel.add(childPanel, BorderLayout.CENTER)
        add(parentPanel)
        isVisible = true
    }
}

// 子布局，实现 FileDropHandler 接口，处理文件拖拽，并判断文件是否接受
class FileDropPanel : JPanel(), FileDropHandler {

    override fun handleFileDrop(files: List<File>) {
        // 处理文件拖拽的逻辑
        println("子布局处理文件: ${files.map { it.name }}")
        this.background = Color.YELLOW // 拖拽成功，变为黄色
    }

    override fun isFileAcceptable(files: List<File>): Boolean {
        // 判断文件是否可接受，例如只接受 .txt 和 .jpg 文件，且文件数量不超过 3
        return files.size <= 3 && files.all { file ->
            file.name.lowercase().endsWith(".txt") || file.name.lowercase().endsWith(".jpg")
        }
    }

    // 当文件可接受时更新 UI
    fun onDragAccept() {
        this.border = BorderFactory.createLineBorder(Color.GREEN, 3) // 绿色边框表示接受
    }

    // 当文件不可接受时更新 UI
    fun onDragReject() {
        this.border = BorderFactory.createLineBorder(Color.RED, 3) // 红色边框表示拒绝
    }

    // 重置 UI 到初始状态
    fun resetUI() {
        this.border = BorderFactory.createTitledBorder("子布局") // 恢复默认边框
        this.background = Color.CYAN // 恢复默认背景颜色
    }
}
