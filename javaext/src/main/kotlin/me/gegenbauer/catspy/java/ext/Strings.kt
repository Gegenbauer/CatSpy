package me.gegenbauer.catspy.java.ext

const val EMPTY_STRING: String = ""

const val SPACE_STRING: String = " "

val WORD_REGEX = "\\s+".toRegex()

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
 * Parse a command line string into an array of arguments, properly handling quoted strings.
 * This handles paths with spaces by supporting both single and double quotes.
 */
fun String.toCommandArray(): Array<String> {
    return parseCommandLine(this)
}

/**
 * Parse a command line string into an array of arguments, properly handling quoted strings.
 * This handles paths with spaces by supporting both single and double quotes.
 */
private fun parseCommandLine(command: String): Array<String> {
    val args = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    var quoteChar = ' '
    var i = 0
    
    while (i < command.length) {
        val char = command[i]
        
        when {
            // Handle quote characters
            (char == '"' || char == '\'') && !inQuotes -> {
                inQuotes = true
                quoteChar = char
            }
            char == quoteChar && inQuotes -> {
                inQuotes = false
                quoteChar = ' '
            }
            // Handle spaces
            char == ' ' && !inQuotes -> {
                if (current.isNotEmpty()) {
                    args.add(current.toString())
                    current = StringBuilder()
                }
                // Skip multiple spaces
                while (i + 1 < command.length && command[i + 1] == ' ') {
                    i++
                }
            }
            // Handle escape sequences
            char == '\\' && i + 1 < command.length -> {
                val nextChar = command[i + 1]
                if (nextChar == '"' || nextChar == '\'' || nextChar == '\\') {
                    current.append(nextChar)
                    i++ // Skip the next character
                } else {
                    current.append(char)
                }
            }
            // Regular character
            else -> {
                current.append(char)
            }
        }
        i++
    }
    
    // Add the last argument if any
    if (current.isNotEmpty()) {
        args.add(current.toString())
    }
    
    return args.toTypedArray()
}