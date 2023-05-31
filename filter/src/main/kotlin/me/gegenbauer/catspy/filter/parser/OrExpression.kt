package me.gegenbauer.catspy.filter.parser

class OrExpression(
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