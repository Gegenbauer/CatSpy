package me.gegenbauer.catspy.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


suspend fun copyFileWithProgress(sourceFile: File, targetFile: File, progressListener: (Float) -> Unit) {
    val totalSize = sourceFile.length()
    var copiedBytes = 0L

    withContext(Dispatchers.GIO) {
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                ensureActive()
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    ensureActive()
                    output.write(buffer, 0, bytesRead)
                    copiedBytes += bytesRead
                    progressListener(copiedBytes.toFloat() / totalSize)
                    bytesRead = input.read(buffer)
                }
            }
        }
    }
}

suspend fun writeLinesWithProgress(targetFile: File, count: Int, lineFetcher: (Int) -> String, progressListener: (Float) -> Unit) {
    withContext(Dispatchers.GIO) {
        val outputStream = BufferedOutputStream(targetFile.outputStream())
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bufferIndex = 0
        var linesWritten = 0

        for (index in 0 until count) {
            val bytes = (lineFetcher(index) + "\n").toByteArray()
            if (bufferIndex + bytes.size > buffer.size) {
                ensureActive()
                outputStream.write(buffer, 0, bufferIndex)
                bufferIndex = 0
                linesWritten += index - linesWritten
                progressListener(linesWritten.toFloat() / count)
            }
            bytes.forEach { byte ->
                buffer[bufferIndex] = byte
                bufferIndex++
            }
        }

        ensureActive()
        if (bufferIndex > 0) {
            outputStream.write(buffer, 0, bufferIndex)
            linesWritten += count - linesWritten
            progressListener(linesWritten.toFloat() / count)
        }

        outputStream.close()
    }
}