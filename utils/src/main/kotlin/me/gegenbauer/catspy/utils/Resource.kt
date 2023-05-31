package me.gegenbauer.catspy.utils

import com.github.weisj.darklaf.iconset.AllIcons
import com.github.weisj.darklaf.properties.icons.IconLoader
import me.gegenbauer.catspy.log.appendPath
import java.io.InputStream
import java.net.URL
import java.util.*
import javax.swing.Icon

fun loadResourceAsStream(resourcePath: String): InputStream {
    val contextClassLoader = Thread.currentThread().contextClassLoader
    val resource = contextClassLoader.getResourceAsStream(resourcePath)
    return requireNotNull(resource) { "Resource $resourcePath not found" }
}

fun getResource(relativePath: String): URL {
    val classLoader = Thread.currentThread().contextClassLoader
    val resource = classLoader.getResource(relativePath)
    return resource ?: throw IllegalArgumentException("Resource not found")
}

private const val IMAGE_RES_DIR = "images"

fun <T : Icon> loadThemedIcon(img: String, w: Int = iconDefaultSize, h: Int = iconDefaultSize): T {
    return IconLoader.get().getIcon(IMAGE_RES_DIR.appendPath(img), w, h, true) as T
}

fun <T : Icon> loadThemedIcon(img: String, size: Int = iconDefaultSize): T {
    return IconLoader.get().getIcon(IMAGE_RES_DIR.appendPath(img), size, size, true) as T
}

fun <T : Icon> loadDarklafThemedIcon(img: String, size: Int = iconDefaultSize): T {
    return IconLoader.get(AllIcons::class.java).getIcon(
        img,
        iconDefaultSize,
        iconDefaultSize, true
    ) as T
}

fun <T : Icon> loadIcon(img: String, w: Int = iconDefaultSize, h: Int = iconDefaultSize): T {
    return IconLoader.get().getIcon(IMAGE_RES_DIR.appendPath(img), w, h, false) as T
}

fun <T : Icon> loadIcon(img: String, size: Int = iconDefaultSize): T {
    return IconLoader.get().getIcon(IMAGE_RES_DIR.appendPath(img), size, size, false) as T
}

fun <T : Icon> loadIconWithRealSize(img: String, themed: Boolean = false): T {
    return IconLoader.get().getIcon(IMAGE_RES_DIR.appendPath(img), themed) as T
}

private const val STRING_RES_DIR = "strings"

fun getStringFile(locale: Locale): InputStream {
    val filename = when (locale) {
        Locale.KOREAN -> {
            "ko.json"
        }

        Locale.CHINA -> {
            "zh_cn.json"
        }

        Locale.ENGLISH -> {
            "en.json"
        }

        else -> {
            "zh_cn.json"
        }
    }
    return loadResourceAsStream(STRING_RES_DIR.appendPath(filename))
}

var iconDefaultSize = 15