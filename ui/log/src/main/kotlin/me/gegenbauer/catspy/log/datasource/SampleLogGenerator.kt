package me.gegenbauer.catspy.log.datasource

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

// 设置日志级别、TAG 和其他参数
val LOG_LEVELS = listOf("V", "D", "I", "W", "E", "F")
val TAGS = listOf("MyApp", "Network", "UI", "Database", "Security", "System")
val SAMPLE_MESSAGES = listOf(
    "Initialized the application",
    "User logged in successfully",
    "Network request completed",
    "Error connecting to database",
    "Data saved successfully",
    "User logged out"
)

// 生成单条日志记录
fun generateLogEntry(pid: Int, tid: Int): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
    val level = LOG_LEVELS.random()
    val tag = TAGS.random()
    val message = SAMPLE_MESSAGES.random() + " - " + Random.nextInt(1, 1000)
    return "$timestamp $pid $tid $level $tag: $message"
}

// 生成日志文件
fun generateLogFile(filePath: String, logCount: Int) {
    File(filePath).parentFile.mkdirs()
    BufferedWriter(FileWriter(filePath)).use { logFile ->
        val pid = Random.nextInt(1000, 2000)  // 随机进程ID
        val tid = Random.nextInt(1000, 2000)  // 随机线程ID

        repeat(logCount) {
            logFile.write(generateLogEntry(pid, tid) + "\n")
        }
    }
}

// 生成 Gzip 文件
fun createGzipFile(sourcePath: String, gzipPath: String) {
    FileInputStream(sourcePath).use { fis ->
        GZIPOutputStream(FileOutputStream(gzipPath).buffered()).use { gos ->
            fis.copyTo(gos)
        }
    }
}

// 生成 Zip 文件
fun createZipFile(dirPath: String, zipPath: String) {
    ZipOutputStream(FileOutputStream(zipPath).buffered()).use { zipOutputStream ->
        File(dirPath).listFiles()?.forEach { file ->
            if (file.isFile) {
                zipOutputStream.putNextEntry(ZipEntry(file.name))
                file.inputStream().copyTo(zipOutputStream)
                zipOutputStream.closeEntry()
            }
        }
    }
}

// 生成 Tar.gz 文件
fun createTarGzFile(sourcePath: String, tarGzPath: String) {
    TarArchiveOutputStream(FileOutputStream(tarGzPath).buffered()).use { tarOutputStream ->
        val entry = TarArchiveEntry(File(sourcePath), File(sourcePath).name)
        tarOutputStream.putArchiveEntry(entry)
        FileInputStream(sourcePath).use { fis ->
            fis.copyTo(tarOutputStream)
        }
        tarOutputStream.closeArchiveEntry()
    }
}

// 创建包含日志文件的目录
fun createLogDirectory(dirPath: String, numFiles: Int, logCount: Int) {
    File(dirPath).mkdirs()
    val executor = Executors.newFixedThreadPool(4) // 创建固定大小的线程池

    for (i in 1..numFiles) {
        executor.submit {
            val logFilePath = "$dirPath/log_$i.txt"
            println("Generating log file: $logFilePath")
            generateLogFile(logFilePath, logCount)
        }
    }

    executor.shutdown() // 关闭线程池，等待所有任务完成
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS) // 等待完成
}

// 递归生成日志文件和目录
fun generateLogStructure(basePath: String, depth: Int = 0) {
    if (depth > 3) return  // 最大深度限制

    val isDirectory = Random.nextBoolean()
    val numFiles = Random.nextInt(5, 11)  // 5到10个文件或目录

    if (isDirectory) {
        val dirPath = "$basePath/dir_${depth}_${Random.nextInt(1, 100)}"
        createLogDirectory(dirPath, numFiles, 500_000)  // 每个文件500,000条日志
        if (Random.nextBoolean()) createZipFile(dirPath, "$dirPath.zip")
        if (Random.nextBoolean()) createGzipFile("$dirPath/log_1.txt", "$dirPath.log.gz")
        if (Random.nextBoolean()) createTarGzFile("$dirPath/log_1.txt", "$dirPath.tar.gz")
        generateLogStructure(dirPath, depth + 1)
    } else {
        val logFilePath = "$basePath/log_${Random.nextInt(1, 100)}.txt"
        println("Generating log file: $logFilePath")
        generateLogFile(logFilePath, 500_000)  // 500,000条日志
    }
}

// 主函数
fun main() {
    val baseDir = "/home/yingbin/CacheFiles/logs"
    println("Generating log structure...")
    generateLogFile("$baseDir/log.txt", 1_000_000)  // 1,000,000条日志
    generateLogStructure(baseDir)
    println("Log generation completed.")
}
