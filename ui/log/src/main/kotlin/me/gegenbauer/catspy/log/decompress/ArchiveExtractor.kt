package me.gegenbauer.catspy.log.decompress

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.file.archive.CompressionType
import me.gegenbauer.catspy.file.archive.FileType
import me.gegenbauer.catspy.log.Log
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private const val TAG = "ArchiveExtractor"

interface ExtractProgressListener {
    fun onProgressUpdate(progress: Int)

    fun onFileExtracted(file: File)

    fun onExtractionComplete()

    fun onExtractionFailed(err: Throwable)
}

interface ArchiveExtractor {
    suspend fun extract()
}

abstract class BaseArchiveExtractor(
    protected val archiveFile: File,
    protected val outputDir: File,
    protected val extractedFiles: Set<String>,
    protected val listener: ExtractProgressListener? = null
) : ArchiveExtractor {


    protected fun shouldExtract(entryName: String): Boolean {
        return !extractedFiles.contains(entryName)
    }

    protected suspend fun extractChunk(
        buffer: ByteArray,
        outputStream: FileOutputStream,
        inputStream: InputStream
    ) {
        return withContext(Dispatchers.GIO) {
            var bytes = inputStream.read(buffer)
            while (bytes > 0) {
                ensureActive()
                outputStream.write(buffer, 0, bytes)
                bytes = inputStream.read(buffer)
            }
        }
    }
}

class FullyExtractor(
    archiveFile: File,
    outputDir: File,
    extractedFiles: Set<String>,
    listener: ExtractProgressListener? = null
) : BaseArchiveExtractor(archiveFile, outputDir, extractedFiles, listener) {

    private val unCompressedFiles = mutableSetOf<String>()

    override suspend fun extract() {
        withContext(Dispatchers.GIO) {
            val fileToBeDecompressed = LinkedList<FileDecompressParams>()
            fileToBeDecompressed.push(FileDecompressParams(archiveFile, outputDir))
            while (fileToBeDecompressed.isNotEmpty()) {
                val targetArchiveFile = fileToBeDecompressed.pollLast()
                if (extractedFiles.contains(targetArchiveFile.file.name)) {
                    listener?.onFileExtracted(targetArchiveFile.file)
                    continue
                }
                val header = ByteArray(300)
                val fis = FileInputStream(targetArchiveFile.file)
                fis.read(header)
                if (!FileType.isCompressedFile(header)) {
                    recordUnCompressedFile(targetArchiveFile.file)
                    listener?.onFileExtracted(targetArchiveFile.file)
                    continue
                }
                val extractor = createExtractor(
                    header,
                    targetArchiveFile.file,
                    targetArchiveFile.outputDir,
                    extractedFiles,
                    listener
                )
                Log.d(
                    TAG, "[extract] extracting ${targetArchiveFile.file} to" +
                            " ${targetArchiveFile.outputDir}, extractor: $extractor"
                )
                try {
                    targetArchiveFile.outputDir.mkdirs()
                    extractor.extract()
                } catch (e: Exception) {
                    listener?.onExtractionFailed(e)
                    continue
                }
                listener?.onFileExtracted(targetArchiveFile.file)
                if (targetArchiveFile.file.absolutePath != archiveFile.absolutePath) {
                    targetArchiveFile.file.delete()
                }
                val files = collectCompressFile(targetArchiveFile.outputDir)
                files.forEach {
                    val relativePath = it.relativeTo(targetArchiveFile.outputDir).path
                    val outputDir = File(targetArchiveFile.outputDir, relativePath.replace(".", "_"))
                    fileToBeDecompressed.push(FileDecompressParams(it, outputDir))
                }
            }
            listener?.onExtractionComplete()
        }
    }

    private fun createExtractor(
        byteArray: ByteArray,
        archiveFile: File,
        outputDir: File,
        extractedFiles: Set<String>,
        listener: ExtractProgressListener? = null
    ): ArchiveExtractor {
        when {
            CompressionType.Zip.isOfType(byteArray) -> {
                return ZipExtractor(archiveFile, outputDir, extractedFiles, listener)
            }

            CompressionType.Rar.isOfType(byteArray) -> {
                return RarExtractor(archiveFile, outputDir, extractedFiles, listener)
            }

            CompressionType.Tar.isOfType(byteArray) -> {
                return TarExtractor(archiveFile, outputDir, extractedFiles, listener)
            }

            CompressionType.SevenZip.isOfType(byteArray) -> {
                return SevenZipExtractor(archiveFile, outputDir, extractedFiles, listener)
            }

            CompressionType.Gzip.isOfType(byteArray) -> {
                return GzipExtractor(archiveFile, outputDir, extractedFiles, listener)
            }

            CompressionType.Bzip2.isOfType(byteArray) -> {
                return Bzip2Extractor(archiveFile, outputDir, extractedFiles, listener)
            }

            CompressionType.Xz.isOfType(byteArray) -> {
                return XzExtractor(archiveFile, outputDir, extractedFiles, listener)
            }

            else -> {
                throw IllegalArgumentException("Unsupported compression type")
            }
        }
    }

    private suspend fun collectCompressFile(dir: File): List<File> {
        return withContext(Dispatchers.GIO) {
            val files = mutableListOf<File>()
            dir.listFiles()?.forEach {
                if (it.isDirectory) {
                    files.addAll(collectCompressFile(it))
                } else {
                    val header = ByteArray(300)
                    val fis = FileInputStream(it)
                    fis.read(header)
                    if (!isUnCompressedFile(it) || FileType.isCompressedFile(header)) {
                        files.add(it)
                    } else {
                        recordUnCompressedFile(it)
                    }
                }
            }
            files
        }
    }

    private fun recordUnCompressedFile(file: File) {
        unCompressedFiles.add(file.name)
    }

    private fun isUnCompressedFile(file: File): Boolean {
        return unCompressedFiles.contains(file.name)
    }

    private data class FileDecompressParams(
        val file: File,
        val outputDir: File,
    )
}

class ZipExtractor(
    archiveFile: File,
    outputDir: File,
    extractedFiles: Set<String>,
    listener: ExtractProgressListener? = null
) : BaseArchiveExtractor(archiveFile, outputDir, extractedFiles, listener) {

    override suspend fun extract() {
        withContext(Dispatchers.GIO) {
            val buffer = ByteArray(8192)
            ZipInputStream(FileInputStream(archiveFile)).use { inputStream ->
                var entry: ZipEntry? = inputStream.nextEntry
                while (entry != null) {
                    ensureActive()
                    val entryName = entry.name
                    val outputFile = File(outputDir, entryName)
                    outputFile.delete()
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        if (shouldExtract(entryName)) {
                            FileOutputStream(outputFile).use { fos ->
                                extractChunk(buffer, fos, inputStream)
                            }
                            listener?.onFileExtracted(outputFile)
                        }
                    }

                    inputStream.closeEntry()
                    entry = inputStream.nextEntry
                }
            }
        }
    }
}

class RarExtractor(
    archiveFile: File,
    outputDir: File,
    extractedFiles: Set<String>,
    listener: ExtractProgressListener? = null
) : BaseArchiveExtractor(archiveFile, outputDir, extractedFiles, listener) {

    override suspend fun extract() {
        withContext(Dispatchers.GIO) {
            Archive(archiveFile).use { archive ->
                var fileHeader: FileHeader? = archive.nextFileHeader()

                while (fileHeader != null) {
                    ensureActive()
                    val entryName = fileHeader.fileName.trim()
                    val outputFile = File(outputDir, entryName)

                    if (fileHeader.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile.mkdirs()

                        CancellableOutputStream(
                            FileOutputStream(outputFile),
                            currentCoroutineContext().job
                        ).use { fos ->
                            archive.extractFile(fileHeader, fos)
                        }
                    }
                    listener?.onFileExtracted(outputFile)
                    fileHeader = archive.nextFileHeader()
                }
            }
        }
    }
}

class TarExtractor(
    archiveFile: File,
    outputDir: File,
    extractedFiles: Set<String>,
    listener: ExtractProgressListener? = null
) : BaseArchiveExtractor(archiveFile, outputDir, extractedFiles, listener) {

    override suspend fun extract() {
        withContext(Dispatchers.GIO) {
            val buffer = ByteArray(8192)
            TarArchiveInputStream(FileInputStream(archiveFile)).use { inputStream ->
                var entry = inputStream.nextEntry
                while (entry != null) {
                    ensureActive()
                    val entryName = entry.name
                    val outputFile = File(outputDir, entryName)
                    outputFile.delete()
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        if (shouldExtract(entryName)) {
                            FileOutputStream(outputFile).use { fos ->
                                extractChunk(buffer, fos, inputStream)
                            }
                            listener?.onFileExtracted(outputFile)
                        }
                    }

                    entry = inputStream.nextEntry
                }
            }
        }
    }
}

class SevenZipExtractor(
    archiveFile: File,
    outputDir: File,
    extractedFiles: Set<String>,
    listener: ExtractProgressListener? = null
) : BaseArchiveExtractor(archiveFile, outputDir, extractedFiles, listener) {

    override suspend fun extract() {
        withContext(Dispatchers.GIO) {
            val buffer = ByteArray(8192)
            val sevenZFile = SevenZFile.Builder()
                .setFile(archiveFile)
                .setCharset(Charsets.UTF_8)
                .get()
            sevenZFile.use { zFile ->
                var entry = zFile.nextEntry
                while (entry != null) {
                    ensureActive()
                    val entryName = entry.name
                    val outputFile = File(outputDir, entryName)
                    outputFile.delete()
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        if (shouldExtract(entryName)) {
                            FileOutputStream(outputFile).use { fos ->
                                var bytes = zFile.read(buffer)
                                while (bytes > 0) {
                                    ensureActive()
                                    fos.write(buffer, 0, bytes)
                                    bytes = zFile.read(buffer)
                                }
                            }
                            listener?.onFileExtracted(outputFile)
                        }
                    }
                    entry = zFile.nextEntry
                }
            }
        }
    }
}

class GzipExtractor(
    archiveFile: File,
    outputDir: File,
    extractedFiles: Set<String>,
    listener: ExtractProgressListener? = null
) : BaseArchiveExtractor(archiveFile, outputDir, extractedFiles, listener) {

    override suspend fun extract() {
        withContext(Dispatchers.GIO) {
            if (extractedFiles.contains(archiveFile.name)) {
                listener?.onFileExtracted(archiveFile)
                return@withContext
            }
            val buffer = ByteArray(8192)
            FileInputStream(archiveFile).use { fis ->
                val gzipInputStream = GZIPInputStream(fis)
                val entryName = archiveFile.nameWithoutExtension
                val outputFile = File(outputDir, entryName)
                outputFile.delete()

                FileOutputStream(outputFile).use { fos ->
                    var bytes = gzipInputStream.read(buffer)
                    while (bytes > 0) {
                        ensureActive()
                        fos.write(buffer, 0, bytes)
                        bytes = gzipInputStream.read(buffer)
                    }
                }
                listener?.onFileExtracted(outputFile)
            }
        }
    }
}

class Bzip2Extractor(
    archiveFile: File,
    outputDir: File,
    extractedFiles: Set<String>,
    listener: ExtractProgressListener? = null
) : BaseArchiveExtractor(archiveFile, outputDir, extractedFiles, listener) {

    override suspend fun extract() {
        withContext(Dispatchers.GIO) {
            if (extractedFiles.contains(archiveFile.name)) {
                listener?.onFileExtracted(archiveFile)
                return@withContext
            }
            val buffer = ByteArray(8192)
            FileInputStream(archiveFile).use { fis ->
                val bzip2InputStream = BZip2CompressorInputStream(fis)
                val entryName = archiveFile.nameWithoutExtension
                val outputFile = File(outputDir, entryName)
                outputFile.delete()

                FileOutputStream(outputFile).use { fos ->
                    var bytes = bzip2InputStream.read(buffer)
                    while (bytes > 0) {
                        ensureActive()
                        fos.write(buffer, 0, bytes)
                        bytes = bzip2InputStream.read(buffer)
                    }
                }
                listener?.onFileExtracted(outputFile)
            }
        }
    }
}

class XzExtractor(
    archiveFile: File,
    outputDir: File,
    extractedFiles: Set<String>,
    listener: ExtractProgressListener? = null
) : BaseArchiveExtractor(archiveFile, outputDir, extractedFiles, listener) {

    override suspend fun extract() {
        withContext(Dispatchers.GIO) {
            if (extractedFiles.contains(archiveFile.name)) {
                listener?.onFileExtracted(archiveFile)
                return@withContext
            }
            val buffer = ByteArray(8192)
            FileInputStream(archiveFile).use { fis ->
                val xzInputStream = XZInputStream(fis)
                val entryName = archiveFile.nameWithoutExtension
                val outputFile = File(outputDir, entryName)
                outputFile.delete()

                FileOutputStream(outputFile).use { fos ->
                    var bytes = xzInputStream.read(buffer)
                    while (bytes > 0) {
                        ensureActive()
                        fos.write(buffer, 0, bytes)
                        bytes = xzInputStream.read(buffer)
                    }
                }
                listener?.onFileExtracted(outputFile)
            }
        }
    }
}

class CancellableOutputStream(
    private val outputStream: OutputStream,
    private val coroutineJob: Job
) : OutputStream() {
    override fun write(b: Int) {
        checkCancellation()
        outputStream.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        checkCancellation()
        outputStream.write(b, off, len)
    }

    override fun close() {
        outputStream.close()
        super.close()
    }

    private fun checkCancellation() {
        if (!coroutineJob.isActive) {
            throw CancellationException("Extraction cancelled")
        }
    }
}