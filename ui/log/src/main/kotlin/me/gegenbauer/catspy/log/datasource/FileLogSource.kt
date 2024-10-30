import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.log.datasource.LogItem
import okio.buffer
import okio.source
import okio.use
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

interface FileLogSource {
    val file: File

    fun read(): Flow<LogItem>
}

class RegularFileLogSource(override val file: File) : FileLogSource {
    override fun read(): Flow<LogItem> = flow {
        file.source().buffer().use { bufferedSource ->
            bufferedSource.readUtf8()
                .lineSequence()
                .forEach { line -> emit(LogItem(file.absolutePath, line)) }
        }
    }
}

class DirectoryLogSource(override val file: File) : FileLogSource {

    override fun read(): Flow<LogItem> = flow {
        val factory = FileLogSourceFactory()
        file.listFiles()?.forEach { innerFile ->
            emitAll(factory.createSource(innerFile).read())
        }
    }.flowOn(Dispatchers.GIO)
}

class ZipFileLogSource(override val file: File) : FileLogSource {
    override fun read(): Flow<LogItem> = flow {
        ZipInputStream(file.source().buffer().inputStream()).use { zipStream ->
            var entry: ZipEntry?
            while (zipStream.nextEntry.also { entry = it } != null) {
                entry?.let { zipEntry ->
                    if (!zipEntry.isDirectory) {
                        zipStream.source().buffer()
                            .use {
                                val tag = "${file.absolutePath}:${zipEntry.name}"
                                it.readUtf8().lineSequence().forEach { line -> emit(LogItem(tag, line)) }
                            }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.GIO)
}

class GzipFileLogSource(override val file: File) : FileLogSource {
    override fun read(): Flow<LogItem> = flow {
        GZIPInputStream(file.source().buffer().inputStream()).source().buffer().use { bufferedSource ->
            bufferedSource.readUtf8().lineSequence().forEach {
                line -> emit(LogItem(file.absolutePath, line))
            }
        }
    }.flowOn(Dispatchers.GIO)
}

class TarFileLogSource(override val file: File) : FileLogSource {
    override fun read(): Flow<LogItem> {
        return flow {
            GZIPInputStream(FileInputStream(file)).use { gzipInputStream ->
                TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
                    emitAll(readAll(tarInputStream))
                }
            }
        }.flowOn(Dispatchers.GIO)
    }

    private fun readAll(tarInputStream: TarArchiveInputStream): Flow<LogItem> {
        return flow {
            var entry: TarArchiveEntry?
            while (tarInputStream.nextEntry.also { entry = it } != null) {
                entry?.let { tarEntry ->
                    if (!tarEntry.isDirectory) {
                        tarInputStream
                            .source()
                            .buffer()
                            .use { it.readUtf8() }
                            .lineSequence()
                            .forEach { line ->
                                val tag = "${file.absolutePath}:${tarEntry.name}"
                                emit(LogItem(tag, line))
                            }
                    }
                }
            }
        }
    }
}

class FileLogSourceFactory {
    fun createSource(file: File): FileLogSource {
        return when {
            file.isDirectory -> DirectoryLogSource(file)
            file.extension == "zip" -> ZipFileLogSource(file)
            file.extension == "gz" -> GzipFileLogSource(file)
            file.name.endsWith(".tar.gz") -> TarFileLogSource(file)
            else -> RegularFileLogSource(file)
        }
    }
}

class FileReader {

    private val logSourceFactory = FileLogSourceFactory()

    fun readFileAsFlow(file: File): Flow<LogItem> {
        val logSource = logSourceFactory.createSource(file)
        return logSource.read()
    }
}

fun main() = runBlocking {
    val fileReader = FileReader()

    val fileToRead = File("/home/yingbin/CacheFiles/logs/dir_0_13.log.gz")

    val flowOfStrings = fileReader.readFileAsFlow(fileToRead)

    flowOfStrings.collect { content ->
        println("读取到的文件内容: $content")
    }
}
