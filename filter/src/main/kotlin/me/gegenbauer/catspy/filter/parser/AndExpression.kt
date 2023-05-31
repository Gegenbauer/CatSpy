package me.gegenbauer.catspy.filter.parser

class AndExpression(
    val left: FilterExpression,
    val right: FilterExpression,
    wholeExpression: String,
    start: Int,
    end: Int,
): FilterExpression(wholeExpression, start, end) {
    override fun toString(): String {
        return "left=$left, right=$right"
    }
}