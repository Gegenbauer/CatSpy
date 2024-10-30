package me.gegenbauer.catspy.log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.log.decompress.ExtractProgressListener
import me.gegenbauer.catspy.log.decompress.FullyExtractor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val ui = ZipExtractorUI()
        ui.isVisible = true
    }
}

// 使用 Swing 组件创建 GUI
class ZipExtractorUI : JFrame("Zip Extractor") {
    private val progressLabel = JLabel("拖拽文件以解压")
    private val startButton = JButton("开始解压")
    private val stopButton = JButton("停止")
    private var zipFile: File? = null
    private var extractionJob: Job? = null

    init {
        layout = BorderLayout()
        size = Dimension(400, 200)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        isVisible = true

        val panel = JPanel().apply {
            layout = BorderLayout()
            background = Color.LIGHT_GRAY
            add(progressLabel, BorderLayout.CENTER)
        }

        // 设置拖拽文件功能
        panel.dropTarget = DropTarget().apply {
            addDropTargetListener(object : DropTargetAdapter() {
                override fun drop(dtde: DropTargetDropEvent) {
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY)
                        val droppedFiles = dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        zipFile = droppedFiles[0] as File
                        progressLabel.text = "文件路径：${zipFile?.absolutePath}"
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            })
        }

        // 设置开始和停止按钮
        startButton.addActionListener {
            if (zipFile != null) {
                startExtraction()
            } else {
                JOptionPane.showMessageDialog(this, "请先拖拽一个文件")
            }
        }

        stopButton.addActionListener {
            stopExtraction()
        }

        val buttonPanel = JPanel().apply {
            add(startButton)
            add(stopButton)
        }

        add(panel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun startExtraction() {
        extractionJob = CoroutineScope(Dispatchers.IO).launch {
            FullyExtractor(zipFile!!, File("${zipFile!!.parentFile.path}/output"), emptySet(), object : ExtractProgressListener {
                override fun onProgressUpdate(progress: Int) {
                }

                override fun onFileExtracted(file: File) {
                    println("解压文件：${file.absolutePath}")
                    SwingUtilities.invokeLater {
                        progressLabel.text = "解压文件：${file.name}"
                    }
                }

                override fun onExtractionComplete() {
                    println("解压完成")
                    SwingUtilities.invokeLater {
                        progressLabel.text = "解压完成, 目标文件夹：${zipFile!!.parentFile.path}/output"
                    }
                }

                override fun onExtractionFailed(err: Throwable) {
                    println("解压失败：${err.message}")
                    SwingUtilities.invokeLater {
                        progressLabel.text = "解压失败：${err.message}"
                    }
                }

            }).extract()
        }
    }

    private fun stopExtraction() {
        extractionJob?.cancel()
        progressLabel.text = "解压已停止"
    }
}