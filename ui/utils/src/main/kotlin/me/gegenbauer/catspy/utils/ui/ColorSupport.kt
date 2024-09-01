package me.gegenbauer.catspy.utils.ui

import me.gegenbauer.catspy.cache.use
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.utils.string.CacheableStringBuilder
import java.awt.Color

private const val TAG = "ColorSupport"

/**
 * Transforms a color to an HTML color string with alpha.
 */
fun Color.toHtml(): String {
    return CacheableStringBuilder.obtain().use {
        it.append("rgba(")
        it.append(red.toString())
        it.append(", ")
        it.append(green.toString())
        it.append(", ")
        it.append(blue.toString())
        it.append(", ")
        it.append((alpha.toFloat() / 255.0).toString())
        it.append(")")
        it.build()
    }
}

fun Color.toHex(): String {
    return "#%02x%02x%02x%02x".format(alpha, red, green, blue)
}

fun String.hexToColor(): Color {
    return kotlin.runCatching {
        val hex = this.removePrefix("#")
        if (hex.length == 6) {
            return Color.decode("#$hex")
        }
        val alpha = hex.substring(0, 2).toInt(16)
        val red = hex.substring(2, 4).toInt(16)
        val green = hex.substring(4, 6).toInt(16)
        val blue = hex.substring(6, 8).toInt(16)
        return Color(red, green, blue, alpha)
    }.onFailure {
        GLog.w(TAG, "[hexToColor] Failed to parse color from hex: $this")
    }.getOrDefault(Color.BLACK)
}

fun Color.isDark(): Boolean {
    return (red * 0.299 + green * 0.587 + blue * 0.114) < 186
}