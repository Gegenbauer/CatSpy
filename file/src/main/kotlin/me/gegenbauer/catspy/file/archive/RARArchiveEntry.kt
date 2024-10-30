package me.gegenbauer.catspy.file.archive

import com.github.junrar.rarfile.FileHeader
import org.apache.commons.compress.archivers.ArchiveEntry
import java.util.Date

class RARArchiveEntry(private val fileHeader: FileHeader) : ArchiveEntry {

    override fun getName(): String {
        return fileHeader.fileName
    }

    override fun getSize(): Long {
        return fileHeader.fullUnpackSize
    }

    override fun isDirectory(): Boolean {
        return fileHeader.isDirectory
    }

    override fun getLastModifiedDate(): Date {
        return fileHeader.mTime
    }
}
