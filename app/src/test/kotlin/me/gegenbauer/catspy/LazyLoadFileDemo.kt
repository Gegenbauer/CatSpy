package me.gegenbauer.catspy

import java.awt.BorderLayout
import java.io.RandomAccessFile
import javax.swing.*
import kotlin.math.min

data class LineInfo(val offset: Long, val length: Int)

val lineInfos = mutableListOf<LineInfo>()
var file: RandomAccessFile? = null

fun parseFile(filePath: String) {
    file = RandomAccessFile(filePath, "r")
    var offset: Long = 0
    var length: Int = 0
    var byte = file?.read()
    while (byte != null) {
        length++
        if (byte.toChar() == '\n') {
            lineInfos.add(LineInfo(offset, length))
            offset += length
            length = 0
        }
        byte = file?.read()
    }
    // Add the last line if it doesn't end with a newline
    if (length > 0) {
        lineInfos.add(LineInfo(offset, length))
    }
}

fun getLineContent(lineNumber: Int): String {
    val lineInfo = lineInfos[lineNumber]
    val bytes = ByteArray(lineInfo.length)
    file?.seek(lineInfo.offset)
    file?.read(bytes)
    return String(bytes)
}

fun closeFile() {
    file?.close()
}

fun getLineContent(startLineNumber: Int, endLineNumber: Int): String {
    val content = StringBuilder()
    val safeEndLineNumber = min(endLineNumber, lineInfos.size - 1)
    for (lineNumber in startLineNumber..safeEndLineNumber) {
        val lineInfo = lineInfos[lineNumber]
        val bytes = ByteArray(lineInfo.length)
        file?.seek(lineInfo.offset)
        file?.read(bytes, 0, lineInfo.length)
        content.append(String(bytes)).append("\n")
    }
    return content.toString()
}

fun createAndShowGUI() {
    val frame = JFrame("File Viewer")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val textArea = JTextArea(20, 400)
    val scrollPane = JScrollPane(textArea)
    var startLine = 0
    var endLine = 99999

    val statusLabel = JLabel()
    frame.add(statusLabel, BorderLayout.SOUTH)

    val prevButton = JButton("Previous Page")
    prevButton.addActionListener {
        if (startLine > 0) {
            startLine -= 100000
            endLine -= 100000
            statusLabel.text = "Loading..."
            object : SwingWorker<String, Void>() {
                override fun doInBackground(): String {
                    return getLineContent(startLine, endLine)
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
        if (endLine < lineInfos.size - 1) {
            startLine += 100000
            endLine += 100000
            statusLabel.text = "Loading..."
            object : SwingWorker<String, Void>() {
                override fun doInBackground(): String {
                    return getLineContent(startLine, endLine)
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
            parseFile("/home/yingbin/temp_log.txt")
            return getLineContent(startLine, endLine)
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

    // 使用示例
    val (offsets, lengths) = getLineOffsets("/home/yingbin/temp_log.txt")
    val lineContent = readLineAt("your_file.txt", offsets[5], lengths[5])  // 读取第6行的内容
    println(lineContent)
}

fun getLineOffsets(filePath: String): Pair<List<Long>, List<Int>> {
    val offsets = mutableListOf<Long>()
    val lengths = mutableListOf<Int>()
    RandomAccessFile(filePath, "r").use { file ->
        var offset: Long = 0
        var length = 0
        var intChar: Int
        while (file.read().also { intChar = it } != -1) {
            length++
            if (intChar.toChar() == '\n') {
                offsets.add(offset)
                lengths.add(length)
                offset += length
                length = 0
            }
        }
    }
    return Pair(offsets, lengths)
}

fun readLineAt(filePath: String, offset: Long, length: Int): String {
    RandomAccessFile(filePath, "r").use { file ->
        val chars = CharArray(length)
        file.seek(offset)
        for (i in 0 until length) {
            chars[i] = file.read().toChar()
        }
        return String(chars)
    }
}