package me.gegenbauer.catspy.render

import java.awt.Color

/**
 * Transforms a color to an HTML color string with alpha.
 */
fun Color.toHtml(): String {
    return "rgba(%d, %d, %d, %f)".format(red, green, blue, alpha.toFloat() / 255.0)
}