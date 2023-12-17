package me.gegenbauer.catspy.strings

fun String.get(vararg params: Any): String {
    return replace("\\\"", "\"").format(*params)
}