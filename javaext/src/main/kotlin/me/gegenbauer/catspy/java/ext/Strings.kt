package me.gegenbauer.catspy.java.ext

fun String.capitalize() = this.replaceFirstChar { it.uppercase() }

fun String.maxLength(maxLength: Int, ellipsisEndOrStart: Boolean = true, ellipsis: String = "..."): String {
    if (this.length <= maxLength) return this
    return if (ellipsisEndOrStart) {
        this.substring(0, maxLength - ellipsis.length) + ellipsis
    } else {
        ellipsis + this.substring(this.length - maxLength + ellipsis.length)
    }
}