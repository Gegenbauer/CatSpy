package me.gegenbauer.catspy.java.ext

fun String.capitalize() = this.replaceFirstChar { it.uppercase() }

private val stringBuilder = ThreadLocal.withInitial { StringBuilder() }

fun String.maxLength(maxLength: Int, ellipsisEndOrStart: Boolean = true, ellipsis: String = "..."): String {
    if (this.length <= maxLength) return this
    val result = stringBuilder.get()
    result.setLength(0) // Reset the StringBuilder
    if (ellipsisEndOrStart) {
        result.append(this, 0, maxLength - ellipsis.length).append(ellipsis)
    } else {
        result.append(ellipsis).append(this, this.length - maxLength + ellipsis.length, this.length)
    }
    return result.toString()
}