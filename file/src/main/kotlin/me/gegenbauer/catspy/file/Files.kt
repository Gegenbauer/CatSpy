package me.gegenbauer.catspy.file

import java.io.File

fun String.appendPath(path: String): String {
    return "$this/$path"
}

fun File.ensureDir() {
    if (!exists()) {
        mkdirs()
    }
}