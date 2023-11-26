package me.gegenbauer.catspy

import me.gegenbauer.catspy.file.RandomFileAccessor
import me.gegenbauer.catspy.log.model.LogcatItem
import java.awt.BorderLayout
import javax.swing.*
import kotlin.system.measureTimeMillis

private const val PAGE_SIZE = 20000

fun createAndShowGUI() {
    val frame = JFrame("File Viewer")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val textArea = JTextArea(20, 400)
    val scrollPane = JScrollPane(textArea)
    var startLine = 0
    var endLine = PAGE_SIZE

    val statusLabel = JLabel()
    frame.add(statusLabel, BorderLayout.SOUTH)

    val prevButton = JButton("Previous Page")
    val fileAccessor = RandomFileAccessor("/home/yingbin/temp_log.txt")
    prevButton.addActionListener {
        if (startLine > 0) {
            startLine -= PAGE_SIZE
            endLine -= PAGE_SIZE
            statusLabel.text = "Loading..."
            object : SwingWorker<String, Void>() {
                override fun doInBackground(): String {
                    return fileAccessor.getLines(startLine, endLine).joinToString("\n")
                }

                override fun done() {
                    textArea.text = get()
                    statusLabel.text = ""
                }
            }.execute()
        }
    }

    val nextButton = JButton("Next Page")
    nextButton.addActionListener {
        if (endLine < fileAccessor.linesCount - 1) {
            startLine += PAGE_SIZE
            endLine += PAGE_SIZE
            statusLabel.text = "Loading..."
            object : SwingWorker<String, Void>() {
                override fun doInBackground(): String {
                    return fileAccessor.getLines(startLine, endLine).joinToString("\n")
                }

                override fun done() {
                    textArea.text = get()
                    statusLabel.text = ""
                }
            }.execute()
        }
    }

    frame.add(prevButton, BorderLayout.WEST)
    frame.add(nextButton, BorderLayout.EAST)
    frame.add(scrollPane, BorderLayout.CENTER)

    // Load the first page of content
    statusLabel.text = "Loading..."
    object : SwingWorker<String, Void>() {
        override fun doInBackground(): String {
            fileAccessor.parseFile()
            return fileAccessor.getLines(0, PAGE_SIZE).joinToString("\n")
        }

        override fun done() {
            textArea.text = get()
            statusLabel.text = ""
        }
    }.execute()

    frame.pack()
    frame.isVisible = true
}

fun main() {
    //SwingUtilities.invokeLater(::createAndShowGUI)
    val fileAccessor = RandomFileAccessor("/home/yingbin/temp_log.txt")
    measureTimeMillis {
        fileAccessor.parseFile()
    }.apply {
        println("parseFile() took $this ms")
    }
    measureTimeMillis {
        fileAccessor.getLines(0, fileAccessor.linesCount).mapIndexed { index, s ->
            LogcatItem.from(s, index)
        }
    }.apply {
        println("getLines() took $this ms")
    }
    println("end")
}