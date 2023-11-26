package me.gegenbauer.catspy.file

import java.io.File
import java.io.RandomAccessFile

class RandomFileAccessor(override val path: String) : FileAccessor {
    private val file: RandomAccessFile by lazy {
        RandomAccessFile(path, "r")
    }
    private val lines = mutableListOf<FileAccessor.Line>()
    override val linesCount: Int
        get() = lines.size

    private var canceled = false

    override fun parseFile(): List<FileAccessor.Line> {
        File(path).inputStream().use { stream ->
            val buffer = ByteArray(4096)
            var bytesRead = stream.read(buffer)
            var intChar: Int
            var offset: Long = 0
            var length = 0
            var lastCharWasCR = false
            while (bytesRead != -1) {
                for (i in 0 until bytesRead) {
                    intChar = buffer[i].toInt()
                    length++
                    if (intChar.toChar() == '\n') {
                        if (lastCharWasCR) {
                            length-- // Windows-style '\r\n', don't count the '\r' as part of the previous line
                        }
                        lines.add(FileAccessor.Line(offset, length - 1))
                        offset += length
                        length = 0
                    } else if (lastCharWasCR) { // Mac-style '\r'
                        lines.add(FileAccessor.Line(offset, length - 2)) // don't count the '\r' as part of the line
                        offset += length - 1
                        length = 1
                    }
                    lastCharWasCR = (intChar.toChar() == '\r')
                    if (canceled) {
                        return@use
                    }
                }
                bytesRead = stream.read(buffer)
            }
        }
        return lines
    }

    override fun getLine(line: Int): String {
        val chars = ByteArray(lines[line].length)
        file.seek(lines[line].offset)
        file.readFully(chars)
        return String(chars)
    }

    override fun dispose() {
        file.close()
    }

    override fun cancel() {
        canceled = true
    }
}