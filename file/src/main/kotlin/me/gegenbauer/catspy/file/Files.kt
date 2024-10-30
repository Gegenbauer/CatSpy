package me.gegenbauer.catspy.file

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Append a name to the string with file separator
 */
fun String.appendPath(path: String): String {
    if (path.isEmpty()) return this
    if (isEmpty()) return path

    val buffer = StringBuilder(this)
    return buffer.appendPath(path).toString()
}

fun String.appendPathWithBuffer(path: String): StringBuilder {
    return StringBuilder(this).appendPath(path)
}

/**
 * Append a name to the string with file separator
 */
fun StringBuilder.appendPath(path: String): StringBuilder {
    if (path.isEmpty()) return this

    return if (endsWith(File.separator)) {
        append(path)
    } else {
        append(File.separator).append(path)
    }
}

/**
 * Append a name to the string with a splitter '_'
 */
fun StringBuilder.appendName(name: String, splitter: String = "_"): StringBuilder {
    return append(splitter).append(name)
}

/**
 * Append a name to the string with a splitter '_'
 */
fun String.appendName(name: String, splitter: String = "_"): String {
    val buffer = StringBuilder(this)
    return buffer.appendName(name, splitter).toString()
}

fun String.appendNameWithBuffer(name: String, splitter: String = "_"): StringBuilder {
    return StringBuilder(this).appendName(name, splitter)
}

fun StringBuilder.appendExtension(extension: String): StringBuilder {
    return append(".").append(extension)
}

fun String.appendExtensionWithBuffer(extension: String): StringBuilder {
    return StringBuilder(this).appendExtension(extension)
}

fun String.appendExtension(extension: String): String {
    val buffer = StringBuilder(this)
    return buffer.appendExtension(extension).toString()
}

fun StringBuilder.get(): String {
    return toString()
}

fun File.copy(targetFile: File) {
    Files.copy(toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

fun getFilePath(key: String): FilePath {
    val lastSeparator = key.lastIndexOf(File.separator)
    return if (lastSeparator == -1) {
        FilePath(EMPTY_STRING, key)
    } else {
        FilePath(key.substring(0, lastSeparator), key.substring(lastSeparator + 1))
    }
}

inline val String.fileName: String
    get() = substringAfterLast(File.separator)

data class FilePath(
    val parentDir: String,
    val fileName: String
)

/**
 * Check if the name is a valid file name
 * macOS, Windows, Linux
 */
fun isValidFileName(name: String): Boolean {
    return name.matches(Regex("^[^/\\\\:*?\"<>|]*$"))
}

const val FILE_NAME_INVALID_CHARS = "/\\:*?\"<>|"

val archiveExtensions: Set<String> =
    setOf("zip", "tar", "gz", "tgz", "tar.gz", "tar.bz2", "tar.xz", "7z", "rar")

val singleFileArchiveExtensions: Set<String> = setOf("gz, bz2, xz")

fun isSingleFileArchive(file: File): Boolean {
    return file.extension in singleFileArchiveExtensions && !file.nameWithoutExtension.endsWith(".tar")
}

/**
 * Get the files from a list of files, recursively
 */
fun List<File>?.getFiles(): List<File> {
    if (this == null) return emptyList()
    val files = mutableListOf<File>()
    forEach {
        if (it.isDirectory) {
            files.addAll(it.listFiles()?.toList().getFiles())
        } else if (it.isFile) {
            files.add(it)
        }
    }
    return files
}