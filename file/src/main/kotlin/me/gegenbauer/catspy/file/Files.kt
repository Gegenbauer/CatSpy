package me.gegenbauer.catspy.file

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun String.appendPath(path: String): String {
    return "$this${File.separator}$path"
}

fun String.checkAndAppendPath(path: String): String {
    return if (endsWith(File.separator)) {
        "$this$path"
    } else {
        appendPath(path)
    }
}

fun String.appendExtension(extension: String): String {
    return "$this.$extension"
}

fun File.ensureDir() {
    if (!exists()) {
        mkdirs()
    }
}

fun File.copy(targetFile: File) {
    Files.copy(toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

fun String.getFileName(): String {
    val lastSeparator = lastIndexOf(File.separator)
    return if (lastSeparator == -1) {
        this
    } else {
        substring(lastSeparator + 1)
    }
}