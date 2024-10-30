package me.gegenbauer.catspy.file.archive

import java.io.File
import java.util.zip.ZipInputStream

interface ArchiveFileExtractor {
    val file: File

    suspend fun extractTo(destination: File)
}

interface ArchiveFileExtractorFactory {
    suspend fun createExtractor(file: File): ArchiveFileExtractor
}

class ZipArchiveFileExtractor(override val file: File) : ArchiveFileExtractor {
    override suspend fun extractTo(destination: File) {
        ZipInputStream(file.inputStream()).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                val entryFile = File(destination, entry.name)
                entryFile.parentFile.mkdirs()
                entryFile.outputStream().use { entryFileOutputStream ->
                    zipInputStream.copyTo(entryFileOutputStream)
                }
            }
        }
    }
}