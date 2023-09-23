package me.gegenbauer.catspy.demo

import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import java.awt.BorderLayout
import java.io.RandomAccessFile
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JTextArea

class LogEntry(val start: Long, val end: Long)

class LogViewer(private val filename: String) {
    private var currentLine = 0
    private val logEntries = ArrayList<LogEntry>()

    private var logTextArea: JTextArea? = null
    private var file: RandomAccessFile? = null

    private fun loadNextPage() {
        val textArea = logTextArea ?: return
        val file = file ?: return

        for (i in 0 until 50) {
            if (currentLine >= logEntries.size) {
                break
            }

            val logEntry = logEntries[currentLine]
            val lineBytes = ByteArray((logEntry.end - logEntry.start).toInt())
            file.seek(logEntry.start)
            file.read(lineBytes)
            textArea.append(String(lineBytes))
            textArea.append("\n")

            currentLine++
        }
    }

    fun prepare() {
        file = RandomAccessFile(filename, "r")

        var offset = file!!.filePointer
        while (true) {
            val line = file!!.readLine() ?: break
            val lineEnd = file!!.filePointer
            logEntries.add(LogEntry(offset, lineEnd))
            offset = lineEnd
        }
    }

    fun createAndShowGui() {
        val frame = JFrame("Log Viewer")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(500, 500)

        logTextArea = JTextArea()
        frame.add(logTextArea, BorderLayout.CENTER)

        val nextPageButton = JButton("Load Next Page")
        nextPageButton.addActionListener { loadNextPage() }
        frame.add(nextPageButton, BorderLayout.SOUTH)

        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}

fun main() {
    AppScope.launch {  }
}