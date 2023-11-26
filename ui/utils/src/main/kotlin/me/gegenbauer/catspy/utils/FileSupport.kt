package me.gegenbauer.catspy.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


suspend fun copyWithProgress(sourceFile: File, targetFile: File, progressListener: (Float) -> Unit) {
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