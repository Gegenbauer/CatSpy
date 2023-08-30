package me.gegenbauer.catspy.java.ext

fun String.capitalize() = this.replaceFirstChar { it.uppercase() }