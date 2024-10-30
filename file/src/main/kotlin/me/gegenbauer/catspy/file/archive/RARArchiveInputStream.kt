package me.gegenbauer.catspy.file.archive

import com.github.junrar.Archive
import com.github.junrar.exception.RarException
import org.apache.commons.compress.archivers.ArchiveInputStream
import java.io.IOException
import java.io.InputStream

class RARArchiveInputStream(inputStream: InputStream) : ArchiveInputStream<RARArchiveEntry>() {

    private var archive = readContent(inputStream)

    override fun getNextEntry(): RARArchiveEntry? {
        val fileHeader = archive.nextFileHeader() ?: return null
        return RARArchiveEntry(fileHeader)
    }

    private fun readContent(inputStream: InputStream): Archive {
        return try {
            Archive(inputStream)
        } catch (e: RarException) {
            throw IOException("Failed to read RAR archive", e)
        }
    }
}
