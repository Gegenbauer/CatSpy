package me.gegenbauer.catspy.file.archive

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.InputStream

class SevenZArchiveInputStream(inputStream: InputStream) : ArchiveInputStream<SevenZArchiveEntry>() {

    private val sevenZFile = SevenZFile.Builder()
        .setInputStream(inputStream)
        .setCharset(Charsets.UTF_8)
        .get()

    override fun getNextEntry(): SevenZArchiveEntry? {
        return sevenZFile.nextEntry
    }

    override fun close() {
        sevenZFile.close()
    }
}