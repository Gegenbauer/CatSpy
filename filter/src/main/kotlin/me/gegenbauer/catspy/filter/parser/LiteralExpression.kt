package me.gegenbauer.catspy.filter.parser

class LiteralExpression(
    val key: FilterKey,
    val value: FilterValue,
    wholeExpression: String,
    start: Int,
    end: Int,
    val isExclude: Boolean = false,
): FilterExpression(wholeExpression, start, end) {
    companion object {
        const val EXCLUDE_FLAG = "-"
        const val REGEX_FLAG = "~"
        const val SPLITTER = ":"
    }
}