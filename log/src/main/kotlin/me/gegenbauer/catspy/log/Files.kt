package me.gegenbauer.catspy.log

import java.io.File

fun String.appendPath(path: String): String {
    return "$this${File.separator}$path"
}