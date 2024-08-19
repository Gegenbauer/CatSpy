package me.gegenbauer.catspy.file

const val KB = 1024L
const val MB = 1024 * KB
const val GB = 1024 * MB
const val TB = 1024 * GB

fun Long.toHumanReadableSize(): String {
    return when {
        this < KB -> "$this B"
        this < MB -> "${this / KB} KB"
        this < GB -> "${this / MB} MB"
        this < TB -> "${this / GB} GB"
        else -> "${this / TB} TB"
    }
}