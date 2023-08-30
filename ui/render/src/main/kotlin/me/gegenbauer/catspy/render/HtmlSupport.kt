package me.gegenbauer.catspy.render

import java.awt.Color

fun Color.toHtml(): String {
    return "#%02x%02x%02x".format(red, green, blue)
}