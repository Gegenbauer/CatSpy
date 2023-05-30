package me.gegenbauer.catspy.ui.filter

open class FilterExpression(
    open val wholeExpression: String = "",
    open val start: Int = 0,
    open val end: Int = 0,
) {

    fun getContent(): String {
        return wholeExpression.substring(start, end + 1)
    }

    fun crop(start: Int, end: Int): FilterExpression {
        return FilterExpression(wholeExpression, start, end)
    }

    override fun toString(): String {
        return getContent()
    }

    fun trim(): FilterExpression {
        val trimmedRange = trimmedRange()
        return FilterExpression(wholeExpression, trimmedRange.first, trimmedRange.last)
    }

    companion object {
        private fun from(expression: String): FilterExpression {
            val trimmedRange = expression.trimmedRange()
            if (trimmedRange.first == -1 || trimmedRange.last == -1) {
                return FilterExpression(expression, 0, expression.length - 1)
            }
            return FilterExpression(expression, trimmedRange.first, trimmedRange.last)
        }

        fun String.toFilterExpression(): FilterExpression {
            return from(this)
        }
    }
}

sealed class FilterKey(val key: String) {
    object Tag : FilterKey("tag")

    object Pid : FilterKey("pid")

    object Tid : FilterKey("tid")

    object Message : FilterKey("message")

    object Level : FilterKey("level")

    object Age : FilterKey("age")

    companion object {
        // 使用反射来实现
        private val keyToObjectMap = FilterKey::class.nestedClasses.filter { clazz ->
            FilterKey::class.java.isAssignableFrom(clazz.java) && (clazz.java != FilterKey::class.java)
        }.map {
            it.objectInstance as FilterKey
        }.associateBy {
            it.key
        }

        fun from(key: String): FilterKey {
            return keyToObjectMap[key] ?: throw IllegalArgumentException("Invalid key: $key")
        }
    }
}

open class FilterValue(
    val value: String,
)

class NormalFilterValue(
    value: String,
) : FilterValue(value)

class RegexFilterValue(
    value: String,
) : FilterValue(value)

fun String.trimmedRange(): IntRange {
    val trimmed = this.trim()
    return if (trimmed.isEmpty()) {
        IntRange(-1, -1)
    } else {
        val start = indexOf(trimmed)
        IntRange(start, start + trimmed.length - 1)
    }
}

private fun FilterExpression.trimmedRange(): IntRange {
    val trimmed = wholeExpression.substring(start, end + 1).trim()
    return if (trimmed.isEmpty()) {
        IntRange(-1, -1)
    } else {
        val start = wholeExpression.indexOf(trimmed)
        IntRange(start, start + trimmed.length - 1)
    }
}