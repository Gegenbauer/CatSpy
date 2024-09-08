package me.gegenbauer.catspy.java.ext

const val EMPTY_STRING: String = ""

fun String.capitalize() = this.replaceFirstChar { it.uppercase() }

private val stringBuilder = ThreadLocal.withInitial { StringBuilder() }

private const val ELLIPSIS = "..."

enum class EllipsisPosition {
    START, END, MIDDLE
}

fun String.truncate(maxLength: Int, ellipsisPosition: EllipsisPosition = EllipsisPosition.END): String {
    if (this.length <= maxLength || maxLength <= ELLIPSIS.length) {
        return this
    }
    val lengthWithoutEllipsis = maxLength - ELLIPSIS.length
    return when (ellipsisPosition) {
        EllipsisPosition.START -> ELLIPSIS + this.substring(this.length - lengthWithoutEllipsis)
        EllipsisPosition.END -> this.substring(0, lengthWithoutEllipsis) + ELLIPSIS
        EllipsisPosition.MIDDLE -> {
            val half = maxLength / 2
            val leftLength = half - ELLIPSIS.length / 2
            val rightLength = maxLength - ELLIPSIS.length - leftLength
            val sb = stringBuilder.get()
            sb.clear()
            sb.append(this.substring(0, leftLength))
            sb.append(ELLIPSIS)
            sb.append(this.substring(this.length - rightLength))
            sb.toString()
        }
    }
}

fun getUniqueName(name: String, existingNames: Set<String>): String {
    if (!existingNames.contains(name)) {
        return name
    }
    val baseName = name.substringBeforeLast('_')
    val number = name.substringAfterLast('_').toIntOrNull() ?: 0
    var newName = name
    var i = number
    while (existingNames.contains(newName)) {
        newName = "${baseName}_${++i}"
    }
    return newName
}

/**
 * 只允许有字母与数字还有下划线
 */
fun isValidName(name: String): Boolean {
    return name.matches(Regex("^[a-zA-Z0-9_]*$"))
}